package com.ovos.arabicassistant.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.ovos.arabicassistant.domain.model.DynamicContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * موفر السياق الزمني والمكاني اللحظي لشاشة سيارة BYD DiLink
 * يدمج إحداثيات GPS الدقيقة واسم المدينة والوقت الحالي للنظام
 * لتغذية الـ LLM وأسئلة الطقس والملاحة.
 */
class LocationTimeProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    companion object {
        private const val TAG = "LocationTimeProvider"
    }

    /**
     * جلب السياق اللحظي الحالي
     */
    suspend fun getCurrentContext(activeScreenSummary: String? = null): DynamicContext = withContext(Dispatchers.IO) {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentTime = timeFormat.format(now)
        val currentDate = dateFormat.format(now)

        val location = fetchLastLocation()
        var city: String? = null

        if (location != null) {
            city = resolveCityFromCoordinates(location.latitude, location.longitude)
        }

        DynamicContext(
            time = currentTime,
            date = currentDate,
            latitude = location?.latitude,
            longitude = location?.longitude,
            city = city,
            activeScreenSummary = activeScreenSummary
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    private fun fetchLastLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.d(TAG, "لم يتم منح إذن الموقع الجغرافي، سيتم استخدام التوقيت فقط.")
            return null
        }

        return try {
            val task = fusedLocationClient.lastLocation
            Tasks.await(task, 2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w(TAG, "تعذر جلب موقع GPS الأخير: ${e.message}")
            null
        }
    }

    private fun resolveCityFromCoordinates(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale("ar"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: address.adminArea
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "فشل Geocoder في تحديد اسم المدينة: ${e.message}")
            null
        }
    }
}
