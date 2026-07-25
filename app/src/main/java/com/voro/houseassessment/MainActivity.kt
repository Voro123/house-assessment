package com.voro.houseassessment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.voro.houseassessment.data.RoomRecord
import com.voro.houseassessment.data.RoomRepository
import com.voro.houseassessment.data.UserPreferencesRepository
import com.voro.houseassessment.ui.EditRoomScreen
import com.voro.houseassessment.ui.HomeScreen
import com.voro.houseassessment.ui.HouseAssessmentTheme
import com.voro.houseassessment.ui.MapPickerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HouseAssessmentTheme {
                Surface {
                    val repository = remember { RoomRepository(applicationContext) }
                    val preferencesRepository = remember { UserPreferencesRepository(applicationContext) }
                    var rooms by remember { mutableStateOf(repository.getAll()) }
                    var userDefaults by remember { mutableStateOf(preferencesRepository.getDefaults()) }
                    var editingRoom by remember { mutableStateOf<RoomRecord?>(null) }
                    var showingMap by remember { mutableStateOf(false) }
                    var locationBusy by remember { mutableStateOf(false) }
                    var pendingCameraPath by remember { mutableStateOf<String?>(null) }

                    fun applyCoordinates(latitude: Double, longitude: Double) {
                        editingRoom = editingRoom?.copy(latitude = latitude, longitude = longitude)
                        lifecycleScope.launch {
                            val address = reverseGeocode(latitude, longitude)
                            if (!address.isNullOrBlank()) {
                                val current = editingRoom
                                if (current?.latitude == latitude && current.longitude == longitude) {
                                    editingRoom = current.copy(address = address)
                                }
                            }
                        }
                    }

                    fun locateNow() {
                        locationBusy = true
                        requestDeviceLocation(
                            onSuccess = { latitude, longitude ->
                                locationBusy = false
                                applyCoordinates(latitude, longitude)
                            },
                            onFailure = { message ->
                                locationBusy = false
                                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        ) {
                            locateNow()
                        } else {
                            Toast.makeText(this@MainActivity, "未获得定位权限，可手输地址或使用地图选点", Toast.LENGTH_LONG).show()
                        }
                    }

                    val photoPicker = rememberLauncherForActivityResult(
                        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
                    ) { uris ->
                        val available = 8 - (editingRoom?.photos?.size ?: 0)
                        if (available <= 0 || uris.isEmpty()) return@rememberLauncherForActivityResult
                        lifecycleScope.launch {
                            val newPaths = withContext(Dispatchers.IO) {
                                uris.take(available).mapNotNull { copyPhotoToInternal(it) }
                            }
                            val current = editingRoom
                            if (current != null && newPaths.isNotEmpty()) {
                                editingRoom = current.copy(photos = (current.photos + newPaths).take(8))
                            }
                        }
                    }

                    val cameraLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.TakePicture()
                    ) { success ->
                        val path = pendingCameraPath
                        pendingCameraPath = null
                        if (success && path != null) {
                            editingRoom = editingRoom?.let { current ->
                                current.copy(photos = (current.photos + path).take(8))
                            }
                        } else if (path != null) {
                            runCatching { File(path).delete() }
                        }
                    }

                    when {
                        showingMap -> {
                            val current = editingRoom ?: RoomRecord()
                            MapPickerScreen(
                                initialLatitude = current.latitude,
                                initialLongitude = current.longitude,
                                onCancel = { showingMap = false },
                                onConfirm = { latitude, longitude ->
                                    showingMap = false
                                    applyCoordinates(latitude, longitude)
                                }
                            )
                        }

                        editingRoom != null -> {
                            val current = requireNotNull(editingRoom)
                            EditRoomScreen(
                                room = current,
                                cachedBudget = userDefaults.targetBudget,
                                recentContacts = userDefaults.recentContacts,
                                onChange = { editingRoom = it },
                                onBack = { editingRoom = null },
                                onSave = {
                                    editingRoom?.let { roomToSave ->
                                        lifecycleScope.launch {
                                            val (refreshedRooms, refreshedDefaults) = withContext(Dispatchers.IO) {
                                                repository.save(roomToSave.copy(updatedAt = System.currentTimeMillis()))
                                                preferencesRepository.remember(roomToSave)
                                                repository.getAll() to preferencesRepository.getDefaults()
                                            }
                                            rooms = refreshedRooms
                                            userDefaults = refreshedDefaults
                                            editingRoom = null
                                            Toast.makeText(this@MainActivity, "房源与常用信息已保存", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onPickPhotos = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                onTakePhoto = {
                                    val file = createCameraFile()
                                    pendingCameraPath = file.absolutePath
                                    val uri = FileProvider.getUriForFile(
                                        this@MainActivity,
                                        "$packageName.fileprovider",
                                        file
                                    )
                                    cameraLauncher.launch(uri)
                                },
                                onRemovePhoto = { path ->
                                    editingRoom = editingRoom?.let { it.copy(photos = it.photos - path) }
                                },
                                onApplyCachedBudget = {
                                    val budget = userDefaults.targetBudget
                                    if (budget != null) {
                                        editingRoom = editingRoom?.copy(targetBudget = budget)
                                    }
                                },
                                onApplyContact = { contact ->
                                    editingRoom = editingRoom?.let { preferencesRepository.applyContact(it, contact) }
                                },
                                onUseCurrentLocation = {
                                    val fine = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                                    val coarse = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                                        locateNow()
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                onOpenMap = { showingMap = true },
                                locationBusy = locationBusy
                            )
                        }

                        else -> {
                            HomeScreen(
                                rooms = rooms,
                                onAdd = {
                                    val now = System.currentTimeMillis()
                                    editingRoom = RoomRecord(
                                        targetBudget = userDefaults.targetBudget,
                                        createdAt = now,
                                        updatedAt = now
                                    )
                                },
                                onEdit = { room -> editingRoom = room },
                                onDelete = { room ->
                                    lifecycleScope.launch {
                                        val refreshed = withContext(Dispatchers.IO) {
                                            repository.delete(room.id)
                                            room.photos.forEach { runCatching { File(it).delete() } }
                                            repository.getAll()
                                        }
                                        rooms = refreshed
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestDeviceLocation(
        onSuccess: (Double, Double) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val client = LocationServices.getFusedLocationProviderClient(this)
        val token = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onSuccess(location.latitude, location.longitude)
                } else {
                    client.lastLocation
                        .addOnSuccessListener { last ->
                            if (last != null) onSuccess(last.latitude, last.longitude)
                            else onFailure("暂时无法获取位置，请到窗边重试或使用地图选点")
                        }
                        .addOnFailureListener { onFailure("定位失败：${it.localizedMessage ?: "未知错误"}") }
                }
            }
            .addOnFailureListener { onFailure("定位失败：${it.localizedMessage ?: "未知错误"}") }
    }

    private fun createCameraFile(): File {
        val directory = File(filesDir, "room_photos").apply { mkdirs() }
        return File(directory, "camera_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    }

    private fun copyPhotoToInternal(uri: Uri): String? {
        return runCatching {
            val directory = File(filesDir, "room_photos").apply { mkdirs() }
            val extension = when (contentResolver.getType(uri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val destination = File(directory, "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")
            contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            destination.absolutePath
        }.getOrNull()
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            Geocoder(this@MainActivity, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
        }.getOrNull()
    }
}
