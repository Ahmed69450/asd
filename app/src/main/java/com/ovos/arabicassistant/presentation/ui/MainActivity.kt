package com.ovos.arabicassistant.presentation.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ovos.arabicassistant.R
import com.ovos.arabicassistant.accessibility.ScreenContextHolder
import com.ovos.arabicassistant.data.ai.engine_a.OnnxSemanticRouter
import com.ovos.arabicassistant.data.ai.engine_b.LlamaCppEngine
import com.ovos.arabicassistant.data.car.BydCarIntentDispatcher
import com.ovos.arabicassistant.data.car.CarControlManager
import com.ovos.arabicassistant.data.car.CarControlRepositoryImpl
import com.ovos.arabicassistant.data.local.db.AppDatabase
import com.ovos.arabicassistant.data.local.preferences.SettingsDataStore
import com.ovos.arabicassistant.data.local.repository.MemoryRepositoryImpl
import com.ovos.arabicassistant.data.local.repository.SettingsRepositoryImpl
import com.ovos.arabicassistant.data.location.LocationTimeProvider
import com.ovos.arabicassistant.data.mcp.McpClient
import com.ovos.arabicassistant.data.mcp.McpToolRegistry
import com.ovos.arabicassistant.data.mcp.tools.ScreenAwarenessTool
import com.ovos.arabicassistant.data.mcp.tools.WebSearchTool
import com.ovos.arabicassistant.data.mcp.transport.InAppMcpTransport
import com.ovos.arabicassistant.databinding.ActivityMainBinding
import com.ovos.arabicassistant.domain.usecase.GetDynamicContextUseCase
import com.ovos.arabicassistant.domain.usecase.ProcessVoiceInputUseCase
import com.ovos.arabicassistant.presentation.state.AssistantUiState
import com.ovos.arabicassistant.presentation.viewmodel.MainViewModel
import com.ovos.arabicassistant.service.VoiceAssistantForegroundService
import com.ovos.arabicassistant.voice.PiperTtsManager
import com.ovos.arabicassistant.voice.VoskSpeechState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * شاشة المساعد الصوتي الرئيسية لشاشة سيارة BYD DiLink
 * تدير التفاعل الصوتي الحي، ربط الخدمة الخلفية الأمامية، والمحركات الهجينة.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var piperTts: PiperTtsManager
    private lateinit var locationTimeProvider: LocationTimeProvider
    private lateinit var getDynamicContextUseCase: GetDynamicContextUseCase

    private var assistantService: VoiceAssistantForegroundService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? VoiceAssistantForegroundService.LocalBinder
            assistantService = binder?.getService()
            isServiceBound = true

            assistantService?.onWakeWordListener = {
                runOnUiThread {
                    viewModel.onMicActivated()
                    piperTts.speak("نعم، أنا أسمعك.")
                }
            }

            assistantService?.onCommandCapturedListener = { utterance ->
                runOnUiThread {
                    handleVoiceUtterance(utterance)
                }
            }

            assistantService?.onStateChangedListener = { state ->
                runOnUiThread {
                    updateServiceStateUi(state)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            assistantService = null
            isServiceBound = false
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            startAssistantService()
        } else {
            Toast.makeText(this, "يلزم إذن الميكروفون لتفعيل المساعد الصوتي في السيارة", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDependenciesAndViewModel()
        setupViews()
        observeUiState()
        checkPermissionsAndStartService()
    }

    private fun setupDependenciesAndViewModel() {
        val database = AppDatabase.getInstance(this)
        val memoryRepo = MemoryRepositoryImpl(database.memoryDao())
        val settingsDataStore = SettingsDataStore(this)
        val settingsRepo = SettingsRepositoryImpl(settingsDataStore)

        locationTimeProvider = LocationTimeProvider(this)
        getDynamicContextUseCase = GetDynamicContextUseCase(locationTimeProvider, memoryRepo)

        // 1. تهيئة مدير النطق الصوتي Piper
        piperTts = PiperTtsManager(this)

        // 2. المحرك A (موجه العتاد الدلالي السريع)
        val onnxRouter = OnnxSemanticRouter(this)

        // 3. طبقة عتاد السيارة
        val carDispatcher = BydCarIntentDispatcher(this)
        val carManager = CarControlManager(carDispatcher)
        val carRepo = CarControlRepositoryImpl(carManager)

        // 4. طبقة أدوات MCP
        val mcpRegistry = McpToolRegistry().apply {
            registerTool(WebSearchTool())
            registerTool(ScreenAwarenessTool { ScreenContextHolder.getCurrentSummary() })
        }
        val mcpClient = McpClient(InAppMcpTransport(mcpRegistry))

        // 5. المحرك B (النموذج اللغوي الاحتياطي)
        val llamaEngine = LlamaCppEngine()
        lifecycleScope.launch {
            val modelPath = settingsRepo.modelPathFlow.first()
            val cpuThreads = settingsRepo.cpuThreadsFlow.first()
            if (!modelPath.isNullOrBlank()) {
                llamaEngine.loadModel(modelPath, cpuThreads)
            }
        }

        // 6. ربط حالة الاستخدام الرئيسية
        val processVoiceInputUseCase = ProcessVoiceInputUseCase(
            router = { utterance -> onnxRouter.route(utterance) },
            carRepo = carRepo,
            memoryRepo = memoryRepo,
            llmGenerator = { prompt, context -> llamaEngine.generateStream(prompt, context) }
        )

        viewModel = MainViewModel(processVoiceInputUseCase)
    }

    private fun setupViews() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnMic.setOnClickListener {
            viewModel.onMicActivated()
            assistantService?.startListening()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etUserPrompt.text.toString().trim()
            if (text.isNotEmpty()) {
                handleVoiceUtterance(text)
                binding.etUserPrompt.text.clear()
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AssistantUiState.Idle -> {
                        binding.tvStatus.text = getString(R.string.status_idle)
                        binding.viewStatusDot.backgroundTintList =
                            ContextCompat.getColorStateList(this@MainActivity, R.color.status_green)
                    }
                    is AssistantUiState.Listening -> {
                        binding.tvStatus.text = getString(R.string.status_listening)
                        binding.viewStatusDot.backgroundTintList =
                            ContextCompat.getColorStateList(this@MainActivity, R.color.status_blue)
                    }
                    is AssistantUiState.Processing -> {
                        binding.tvStatus.text = "جاري المعالجة: \"${state.utterance}\"..."
                        binding.viewStatusDot.backgroundTintList =
                            ContextCompat.getColorStateList(this@MainActivity, R.color.status_orange)
                    }
                    is AssistantUiState.Speaking -> {
                        binding.tvStatus.text = getString(R.string.status_speaking)
                        binding.tvTranscript.text = state.text
                        binding.viewStatusDot.backgroundTintList =
                            ContextCompat.getColorStateList(this@MainActivity, R.color.status_green)
                    }
                    is AssistantUiState.Error -> {
                        binding.tvStatus.text = state.message
                        binding.viewStatusDot.backgroundTintList =
                            ContextCompat.getColorStateList(this@MainActivity, R.color.error_red)
                    }
                }
            }
        }
    }

    private fun handleVoiceUtterance(utterance: String) {
        lifecycleScope.launch {
            val screenSummary = ScreenContextHolder.getCurrentSummary()
            val dynamicContext = getDynamicContextUseCase(utterance, screenSummary)
            viewModel.onUserUtterance(utterance, dynamicContext) { spokenReply ->
                piperTts.speak(spokenReply) {
                    runOnUiThread { viewModel.onSpeechCompleted() }
                }
            }
        }
    }

    private fun updateServiceStateUi(state: VoskSpeechState) {
        when (state) {
            VoskSpeechState.WAKE_WORD_LISTENING -> {
                binding.tvStatus.text = "المساعد يستمع في خلفية السيارة (نادي: يا سيارة)"
            }
            VoskSpeechState.ACTIVE_DICTATION -> {
                binding.tvStatus.text = getString(R.string.status_listening)
            }
            VoskSpeechState.PROCESSING -> {
                binding.tvStatus.text = getString(R.string.status_processing)
            }
            VoskSpeechState.IDLE -> {
                binding.tvStatus.text = getString(R.string.status_idle)
            }
        }
    }

    private fun checkPermissionsAndStartService() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startAssistantService()
        } else {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startAssistantService() {
        val serviceIntent = Intent(this, VoiceAssistantForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        piperTts.shutdown()
    }
}
