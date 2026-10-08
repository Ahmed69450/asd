package com.ovos.arabicassistant.presentation.ui

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ovos.arabicassistant.R
import com.ovos.arabicassistant.data.local.preferences.SettingsDataStore
import com.ovos.arabicassistant.databinding.ActivitySettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * شاشة إعدادات المساعد الصوتي
 * تتيح اختيار نموذج GGUF من ذاكرة التخزين ديناميكياً
 * وضبط عدد أنوية المعالج (2 أو 3 خيوط) لحماية نظام السيارة.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var dataStore: SettingsDataStore

    private val selectModelLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedModel(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStore = SettingsDataStore(this)

        // تحميل الإعدادات الحالية
        lifecycleScope.launch {
            val currentPath = dataStore.modelPathFlow.first()
            if (!currentPath.isNullOrBlank()) {
                binding.tvSelectedModelPath.text = currentPath
            } else {
                binding.tvSelectedModelPath.text = getString(R.string.pref_no_model_selected)
            }

            val threads = dataStore.cpuThreadsFlow.first()
            if (threads == 3) {
                binding.rbThreads3.isChecked = true
            } else {
                binding.rbThreads2.isChecked = true
            }
        }

        // اختيار نموذج جديد
        binding.btnSelectModel.setOnClickListener {
            selectModelLauncher.launch("*/*")
        }

        // تبديل عدد الأنوية
        binding.rgCpuThreads.setOnCheckedChangeListener { _, checkedId ->
            val threads = if (checkedId == R.id.rbThreads3) 3 else 2
            lifecycleScope.launch {
                dataStore.setCpuThreads(threads)
                Toast.makeText(this@SettingsActivity, "تم ضبط الأنوية على: $threads", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSelectedModel(uri: Uri) {
        val path = uri.path ?: uri.toString()
        lifecycleScope.launch {
            dataStore.setModelPath(path)
            binding.tvSelectedModelPath.text = path
            Toast.makeText(this@SettingsActivity, "تم حفظ نموذج الذكاء الاصطناعي بنجاح", Toast.LENGTH_SHORT).show()
        }
    }
}
