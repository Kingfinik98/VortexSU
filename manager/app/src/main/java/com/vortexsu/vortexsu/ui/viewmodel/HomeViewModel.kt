package com.vortexsu.vortexsu.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.system.Os
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortexsu.vortexsu.KernelVersion
import com.vortexsu.vortexsu.Natives
import com.vortexsu.vortexsu.getKernelVersion
import com.vortexsu.vortexsu.ksuApp
import com.vortexsu.vortexsu.ui.util.*
import com.vortexsu.vortexsu.ui.util.module.LatestVersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream

class HomeViewModel : ViewModel() {

    data class SystemStatus(
        val isManager: Boolean = false,
        val ksuVersion: Int? = null,
        val ksuFullVersion : String? = null,
        val lkmMode: Boolean? = null,
        val kernelVersion: KernelVersion = getKernelVersion(),
        val isRootAvailable: Boolean = false,
        val isKpmConfigured: Boolean = false,
        val requireNewKernel: Boolean = false
    )

    data class SystemInfo(
        val kernelRelease: String = "",
        val androidVersion: String = "",
        val deviceModel: String = "",
        val managerVersion: Pair<String, Long> = Pair("", 0L),
        val seLinuxStatus: String = "",
        val kpmVersion: String = "",
        val suSFSStatus: String = "",
        val suSFSVersion: String = "",
        val suSFSVariant: String = "", 
        val suSFSFeatures: String = "",
        val superuserCount: Int = 0,
        val moduleCount: Int = 0,
        val kpmModuleCount: Int = 0,
        val managersList: Natives.ManagersList? = null,
        val isDynamicSignEnabled: Boolean = false,
        val zygiskImplement: String = "",
        val metaModuleImplement: String = ""
    )

    var systemStatus by mutableStateOf(SystemStatus())
        private set

    var systemInfo by mutableStateOf(SystemInfo())
        private set

    var latestVersionInfo by mutableStateOf(LatestVersionInfo())
        private set

    var customBannerUri by mutableStateOf<Uri?>(null)
        private set

    private val bannerFileName = "custom_banner"

    fun checkCustomBanner(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val bannerFile = File(context.filesDir, bannerFileName)
            if (bannerFile.exists()) {
                customBannerUri = Uri.fromFile(bannerFile)
            } else {
                customBannerUri = null
            }
        }
    }

    fun saveCustomBanner(context: Context, uri: Uri, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputFile = File(context.filesDir, bannerFileName)
                
                if (inputStream != null) {
                    FileOutputStream(outputFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()
                    
                    customBannerUri = Uri.fromFile(outputFile)
                    
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                     withContext(Dispatchers.Main) {
                        onError()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError()
                }
            }
        }
    }

    fun resetBanner(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val bannerFile = File(context.filesDir, bannerFileName)
            if (bannerFile.exists()) {
                bannerFile.delete()
            }
            customBannerUri = null
        }
    }

    var isSimpleMode by mutableStateOf(false)
        private set
    var isKernelSimpleMode by mutableStateOf(false)
        private set
    var isHideVersion by mutableStateOf(false)
        private set
    var isHideOtherInfo by mutableStateOf(false)
        private set
    var isHideSusfsStatus by mutableStateOf(false)
        private set
    var isHideZygiskImplement by mutableStateOf(false)
        private set
    var isHideMetaModuleImplement by mutableStateOf(false)
        private set
    var isHideLinkCard by mutableStateOf(false)
        private set
    var showKpmInfo by mutableStateOf(false)
        private set

    var isCoreDataLoaded by mutableStateOf(false)
        private set
    var isExtendedDataLoaded by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    private val _dataRefreshTrigger = MutableStateFlow(0L)
    val dataRefreshTrigger: StateFlow<Long> = _dataRefreshTrigger

    private var loadingJobs = mutableListOf<Job>()
    private var lastRefreshTime = 0L
    private val refreshCooldown = 2000L

    fun loadUserSettings(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val settingsPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            isSimpleMode = settingsPrefs.getBoolean("is_simple_mode", false)
            isKernelSimpleMode = settingsPrefs.getBoolean("is_kernel_simple_mode", false)
            isHideVersion = settingsPrefs.getBoolean("is_hide_version", false)
            isHideOtherInfo = settingsPrefs.getBoolean("is_hide_other_info", false)
            isHideSusfsStatus = settingsPrefs.getBoolean("is_hide_susfs_status", false)
            isHideLinkCard = settingsPrefs.getBoolean("is_hide_link_card", false)
            isHideZygiskImplement = settingsPrefs.getBoolean("is_hide_zygisk_Implement", false)
            isHideMetaModuleImplement = settingsPrefs.getBoolean("is_hide_meta_module_Implement", false)
            showKpmInfo = settingsPrefs.getBoolean("show_kpm_info", false)
            
            checkCustomBanner(context)
        }
    }

    fun loadCoreData() {
        if (isCoreDataLoaded) return

        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val kernelVersion = getKernelVersion()
                val isManager = try {
                    Natives.isManager
                } catch (_: Exception) {
                    false
                }

                val ksuVersion = if (isManager) Natives.version else null

                val fullVersion = try {
                    Natives.getFullVersion()
                } catch (_: Exception) {
                    "Unknown"
                }

                val ksuFullVersion = if (isKernelSimpleMode) {
                    try {
                        val startIndex = fullVersion.indexOf('v')
                        if (startIndex >= 0) {
                            val endIndex = fullVersion.indexOf('-', startIndex)
                            val versionStr = if (endIndex > startIndex) {
                                fullVersion.substring(startIndex, endIndex)
                            } else {
                                fullVersion.substring(startIndex)
                            }
                            val numericVersion = "v" + (Regex("""\d+(\.\d+)*""").find(versionStr)?.value ?: versionStr)
                            numericVersion
                        } else {
                            fullVersion
                        }
                    } catch (_: Exception) {
                        fullVersion
                    }
                } else {
                    fullVersion
                }

                val lkmMode = ksuVersion?.let {
                    if (kernelVersion.isGKI()) Natives.isLkmMode else null
                }

                val isRootAvailable = try {
                    rootAvailable()
                } catch (_: Exception) {
                    false
                }

                val isKpmConfigured = try {
                    Natives.isKPMEnabled()
                } catch (_: Exception) {
                    false
                }

                val requireNewKernel = try {
                    isManager && Natives.requireNewKernel()
                } catch (_: Exception) {
                    false
                }

                systemStatus = SystemStatus(
                    isManager = isManager,
                    ksuVersion = ksuVersion,
                    ksuFullVersion = ksuFullVersion,
                    lkmMode = lkmMode,
                    kernelVersion = kernelVersion,
                    isRootAvailable = isRootAvailable,
                    isKpmConfigured = isKpmConfigured,
                    requireNewKernel = requireNewKernel
                )

                isCoreDataLoaded = true
            } catch (_: Exception) {
            }
        }
        loadingJobs.add(job)
    }

    fun loadExtendedData(context: Context) {
        if (isExtendedDataLoaded) return

        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                delay(50)

                val basicInfo = loadBasicSystemInfo(context)
                systemInfo = systemInfo.copy(
                    kernelRelease = basicInfo.first,
                    androidVersion = basicInfo.second,
                    deviceModel = basicInfo.third,
                    managerVersion = basicInfo.fourth,
                    seLinuxStatus = basicInfo.fifth
                )

                delay(100)

                if (!isSimpleMode) {
                    val moduleInfo = loadModuleInfo()
                    systemInfo = systemInfo.copy(
                        kpmVersion = moduleInfo.first,
                        superuserCount = moduleInfo.second,
                        moduleCount = moduleInfo.third,
                        kpmModuleCount = moduleInfo.fourth,
                        zygiskImplement = moduleInfo.fifth,
                        metaModuleImplement = moduleInfo.sixth
                    )
                }

                delay(100)

                if (!isHideSusfsStatus) {
                    val suSFSInfo = loadSuSFSInfo()
                    systemInfo = systemInfo.copy(
                        suSFSStatus = suSFSInfo.first,
                        suSFSVersion = suSFSInfo.second,
                        suSFSVariant = suSFSInfo.third,
                        suSFSFeatures = suSFSInfo.fourth,
                    )
                }

                delay(100)

                val managerInfo = loadManagerInfo()
                systemInfo = systemInfo.copy(
                    managersList = managerInfo.first,
                    isDynamicSignEnabled = managerInfo.second
                )

                isExtendedDataLoaded = true
            } catch (_: Exception) {
            }
        }
        loadingJobs.add(job)
    }

    fun refreshData(context: Context, forceRefresh: Boolean = false) {
        val currentTime = System.currentTimeMillis()

        if (!forceRefresh && currentTime - lastRefreshTime < refreshCooldown) {
            return
        }

        lastRefreshTime = currentTime

        viewModelScope.launch {
            isRefreshing = true

            try {
