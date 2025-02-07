package com.example.fishplatform

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.*

// 定義目標點位
val targets = listOf(
    Triple(1, 24.424646, 118.259475), // 羅厝
    Triple(4, 25.957714, 119.965942), // 猛澳
    Triple(220, 25.142463, 121.791643), // 八斗子
)

@SuppressLint("MissingPermission")
@Composable
fun LocationUpdatesScreen() {
    val permissions = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    CustomPermissionBox(
        permissions = permissions,
        requiredPermissions = listOf(permissions.first())
    ) { grantedPermissions ->
        LocationUpdatesContent(
            usePreciseLocation = grantedPermissions.contains(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }
/*
    PermissionBox(
        permissions = permissions,
        requiredPermissions = listOf(permissions.first()),
    ) {
        LocationUpdatesContent(
            usePreciseLocation = it.contains(Manifest.permission.ACCESS_FINE_LOCATION),
        )
    }*/
}

@RequiresPermission(
    anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION],
)
@Composable
fun LocationUpdatesContent(usePreciseLocation: Boolean) {
    var locationRequest by remember { mutableStateOf<LocationRequest?>(null) }
    var locationUpdates by remember { mutableStateOf("") }
    var vesselId by remember { mutableStateOf("") }
    var totalDistance by remember { mutableStateOf(0f) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }

    if (locationRequest != null) {
        LocationUpdatesEffect(
            locationRequest = locationRequest!!,
            vesselId = vesselId.toIntOrNull() ?: 0,
            serverUrl = "https://padt.toff.best:4431/api/accept_data/"
        ) { result ->
            for (currentLocation in result.locations) {
                lastLocation?.let { last ->
                    val distance = last.distanceTo(currentLocation)
                    totalDistance += distance
                }

                // 找到最近的目標點
                val nearestTarget = findNearestTarget(listOf(currentLocation), targets)
                val targetInfo = nearestTarget?.let {
                    "\n- Nearest Target: ID ${it.second.first} " +
                            "\n- Target Distance: ${String.format("%.2f", it.third)} km"
                } ?: "\n- No nearby target"

                locationUpdates = "${System.currentTimeMillis()}:\n" +
                        "- @lat: ${currentLocation.latitude}\n" +
                        "- @lng: ${currentLocation.longitude}\n" +
                        "- Accuracy: ${currentLocation.accuracy}\n" +
                        "- Total Distance: ${String.format("%.2f", totalDistance)} meters" +
                        targetInfo + "\n\n" +
                        locationUpdates

                lastLocation = currentLocation
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Vessel ID 輸入欄位
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Vessel ID: ")
            TextField(
                value = vesselId,
                onValueChange = { vesselId = it },
                modifier = Modifier.width(120.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
        }

        // 開關位置更新
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable location updates")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = locationRequest != null,
                onCheckedChange = { checked ->
                    locationRequest = if (checked) {
                        val priority = if (usePreciseLocation) {
                            Priority.PRIORITY_HIGH_ACCURACY
                        } else {
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY
                        }
                        LocationRequest.Builder(priority, 3000).build()
                    } else {
                        null
                    }
                }
            )
        }

        // 總距離顯示
        Text(
            text = "Total Distance: ${String.format("%.2f", totalDistance)} meters",
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // 位置更新資訊
        Text(text = locationUpdates)
    }
}

// 保留原有的 LocationUpdatesEffect 和其他輔助函數...
@RequiresPermission(
    anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION],
)
@Composable
fun LocationUpdatesEffect(
    locationRequest: LocationRequest,
    vesselId: Int,
    serverUrl: String,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onUpdate: (result: LocationResult) -> Unit,
) {
    val context = LocalContext.current
    val currentOnUpdate by rememberUpdatedState(newValue = onUpdate)

    DisposableEffect(locationRequest, lifecycleOwner) {
        val locationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationCallback: LocationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(result: LocationResult) {
                currentOnUpdate(result)

                val nearestLocation = findNearestTarget(result.locations, targets)
                if (nearestLocation != null) {
                    sendLocationToServer(result, vesselId, serverUrl)
                } else {
                    Log.d("LocationUpdatesEffect", "No nearby locations found")
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                locationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper()
                )
            } else if (event == Lifecycle.Event.ON_STOP) {
                locationClient.removeLocationUpdates(locationCallback)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            locationClient.removeLocationUpdates(locationCallback)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

// Haversine 距離計算
private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371 // 地球半徑（公里）
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

// 尋找最近的目標點
private fun findNearestTarget(
    locations: List<Location>,
    targets: List<Triple<Int, Double, Double>>
): Triple<Location, Triple<Int, Double, Double>, Double>? {
    return locations.minByOrNull { location ->
        targets.minOf { target ->
            haversine(location.latitude, location.longitude, target.second, target.third)
        }
    }?.let { nearestLocation ->
        val nearestTarget = targets.minByOrNull { target ->
            haversine(nearestLocation.latitude, nearestLocation.longitude, target.second, target.third)
        }
        nearestTarget?.let {
            val distance = haversine(nearestLocation.latitude, nearestLocation.longitude, it.second, it.third)
            Triple(nearestLocation, it, distance)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun sendLocationToServer(locationResult: LocationResult, vesselId: Int, serverUrl: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val logTime = Instant.now().atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT)
            val json = buildJsonFromLocation(locationResult, vesselId, logTime)

            val client = OkHttpClient()
            val body = RequestBody.create(
                "application/json; charset=utf-8".toMediaType(),
                json
            )
            val request = Request.Builder()
                .url(serverUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("LocationUpdatesEffect", "Failed to send data: ${response.code}")
                } else {
                    Log.i("LocationUpdatesEffect", "Data sent successfully!")
                }
            }
        } catch (e: Exception) {
            Log.e("LocationUpdatesEffect", "Error sending location data: ${e.message}")
        }
    }
}

private fun buildJsonFromLocation(
    locationResult: LocationResult,
    vesselId: Int,
    logTime: String
): String {
    val location = locationResult.locations[0]
    return JSONObject(
        mapOf(
            "vessel_id" to vesselId,
            "log_time" to logTime,
            "lon" to location.longitude,
            "lat" to location.latitude
        )
    ).toString()
}

@Composable
fun CustomPermissionBox(
    permissions: List<String>,
    requiredPermissions: List<String>,
    onPermissionGranted: @Composable (List<String>) -> Unit
) {
    val context = LocalContext.current
    var grantedPermissions by remember {
        mutableStateOf(
            permissions.filter {
                ContextCompat.checkSelfPermission(
                    context,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        grantedPermissions = permissions.filter { permission ->
            permissionsMap[permission] == true
        }
    }

    LaunchedEffect(key1 = permissions) {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            launcher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val requiredGranted = requiredPermissions.all {
        grantedPermissions.contains(it)
    }

    if (requiredGranted) {
        onPermissionGranted(grantedPermissions)
    }
}

// permission
