package com.vortexsu.vortexsu.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.system.Os
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuSFSConfigScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.vortexsu.vortexsu.KernelVersion
import com.vortexsu.vortexsu.Natives
import com.vortexsu.vortexsu.R
import com.vortexsu.vortexsu.ui.component.KsuIsValid
import com.vortexsu.vortexsu.ui.component.rememberConfirmDialog
import com.vortexsu.vortexsu.ui.theme.CardConfig
import com.vortexsu.vortexsu.ui.theme.CardConfig.cardAlpha
import com.vortexsu.vortexsu.ui.theme.getCardColors
import com.vortexsu.vortexsu.ui.theme.getCardElevation
import com.vortexsu.vortexsu.ui.susfs.util.SuSFSManager
import com.vortexsu.vortexsu.ui.util.checkNewVersion
import com.vortexsu.vortexsu.ui.util.getSuSFSStatus
import com.vortexsu.vortexsu.ui.util.module.LatestVersionInfo
import com.vortexsu.vortexsu.ui.util.reboot
import com.vortexsu.vortexsu.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * @author ShirkNeko
 * @date 2025/9/29.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val viewModel = viewModel<HomeViewModel>()
    val coroutineScope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewModel.isRefreshing,
        onRefresh = {
            viewModel.onPullRefresh(context)
        }
    )

    LaunchedEffect(key1 = navigator) {
        viewModel.loadUserSettings(context)
        coroutineScope.launch {
            viewModel.loadCoreData()
            delay(100)
            viewModel.loadExtendedData(context)
        }

        // Startup data monitor
        coroutineScope.launch {
            while (true) {
                delay(5000)
                viewModel.autoRefreshIfNeeded(context)
            }
        }
    }

    // Listen to refresh trigger
    LaunchedEffect(viewModel.dataRefreshTrigger) {
        viewModel.dataRefreshTrigger.collect { _ -> }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior,
                navigator = navigator,
                isDataLoaded = viewModel.isCoreDataLoaded,
                customBannerUri = viewModel.customBannerUri
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = 12.dp,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status cards
                if (viewModel.isCoreDataLoaded) {
                    HybridStatusCard(
                        systemStatus = viewModel.systemStatus,
                        onClickInstall = {
                            navigator.navigate(InstallScreenDestination(preselectedKernelUri = null))
                        }
                    )

                    // Warnings
                    if (viewModel.systemStatus.requireNewKernel) {
                        WarningCard(
                            stringResource(id = R.string.require_kernel_version).format(
                                Natives.getSimpleVersionFull(),
                                Natives.MINIMAL_SUPPORTED_KERNEL_FULL
                            )
                        )
                    }

                    if (viewModel.systemStatus.ksuVersion != null && !viewModel.systemStatus.isRootAvailable) {
                        WarningCard(stringResource(id = R.string.grant_root_failed))
                    }

                    val shouldShowWarnings = viewModel.systemStatus.requireNewKernel ||
                            (viewModel.systemStatus.ksuVersion != null && !viewModel.systemStatus.isRootAvailable)

                    if (Natives.version <= Natives.MINIMAL_NEW_IOCTL_KERNEL && !shouldShowWarnings && viewModel.systemStatus.ksuVersion != null) {
                        IncompatibleKernelCard()
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Extended Data
                if (viewModel.isExtendedDataLoaded) {
                    val checkUpdate = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .getBoolean("check_update", true)
                    if (checkUpdate) {
                        UpdateCard()
                    }

                    HybridInfoCard(
                        systemInfo = viewModel.systemInfo,
                        isSimpleMode = viewModel.isSimpleMode,
                        isHideSusfsStatus = viewModel.isHideSusfsStatus,
                        isHideZygiskImplement = viewModel.isHideZygiskImplement,
                        isHideMetaModuleImplement = viewModel.isHideMetaModuleImplement,
                        showKpmInfo = viewModel.showKpmInfo,
                        lkmMode = viewModel.systemStatus.lkmMode,
                    )

                    // Link Cards
                    if (!viewModel.isSimpleMode && !viewModel.isHideLinkCard) {
                        ContributionCard()
                        DonateCard()
                        LearnMoreCard()
                    }
                }

                if (!viewModel.isExtendedDataLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val latestVersionInfo = LatestVersionInfo()
    val newVersion by produceState(initialValue = latestVersionInfo) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion()
        }
    }

    val currentVersionCode = getManagerVersion(context).second
    val newVersionCode = newVersion.versionCode
    val newVersionUrl = newVersion.downloadUrl
    val changelog = newVersion.changelog

    val uriHandler = LocalUriHandler.current
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        WarningCard(
            message = stringResource(id = R.string.new_version_available).format(newVersionCode),
            color = MaterialTheme.colorScheme.outlineVariant,
            onClick = {
                if (changelog.isEmpty()) {
                    uriHandler.openUri(newVersionUrl)
                } else {
                    updateDialog.showConfirm(
                        title = title,
                        content = changelog,
                        markdown = true,
                        confirm = updateText
                    )
                }
            }
        )
    }
}

@Composable
fun RebootDropdownItem(@StringRes id: Int, reason: String = "") {
    DropdownMenuItem(
        text = { Text(stringResource(id)) },
        onClick = { reboot(reason) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigator: DestinationsNavigator,
    isDataLoaded: Boolean = false,
    customBannerUri: Uri? = null
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // GIF Support Loader
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    // PERBAIKAN: Banner di dalam Card, ada jarak dari pinggir (padding), gambar FULL di dalam card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp) // Jarak dari pinggir layar
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(16.dp),
            colors = getCardColors(colorScheme.surfaceContainerHigh),
            elevation = getCardElevation()
        ) {
            // Box ini untuk menampung gambar dan tombol
            Box(modifier = Modifier.fillMaxSize()) {
                // Banner Image - Full Size di dalam Card
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(customBannerUri ?: R.drawable.header_bg)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    contentScale = ContentScale.Crop // Gambar penuh, tidak terpotong aneh
                )

                // Overlay Gelap transparan agar tombol kelihatan
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )

                // Action Buttons - Hanya tombol aksi
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDataLoaded) {
                        if (getSuSFSStatus().equals("true", ignoreCase = true) && SuSFSManager.isBinaryAvailable(context)) {
                            IconButton(onClick = {
                                navigator.navigate(SuSFSConfigScreenDestination)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = stringResource(R.string.susfs_config_setting_title),
                                    tint = Color.White
                                )
                            }
                        }

                        var showDropdown by remember { mutableStateOf(false) }
                        KsuIsValid {
                            IconButton(onClick = {
                                showDropdown = true
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.PowerSettingsNew,
                                    contentDescription = stringResource(id = R.string.reboot),
                                    tint = Color.White
                                )

                                DropdownMenu(expanded = showDropdown, onDismissRequest = {
                                    showDropdown = false
                                }) {
                                    RebootDropdownItem(id = R.string.reboot)

                                    val pm =
                                        LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?
                                    @Suppress("DEPRECATION")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
                                        RebootDropdownItem(id = R.string.reboot_userspace, reason = "userspace")
                                    }
                                    RebootDropdownItem(id = R.string.reboot_recovery, reason = "recovery")
                                    RebootDropdownItem(id = R.string.reboot_bootloader, reason = "bootloader")
                                    RebootDropdownItem(id = R.string.reboot_download, reason = "download")
                                    RebootDropdownItem(id = R.string.reboot_edl, reason = "edl")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= HYBRID UI COMPONENTS =================

@Composable
private fun HybridStatusCard(
    systemStatus: HomeViewModel.SystemStatus,
    onClickInstall: () -> Unit = {}
) {
    val successColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    val bgColor = if (systemStatus.ksuVersion != null) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
    }

    ElevatedCard(
        colors = getCardColors(bgColor),
        elevation = getCardElevation(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = systemStatus.isRootAvailable || systemStatus.kernelVersion.isGKI()) {
                    onClickInstall()
                }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (systemStatus.ksuVersion != null) successColor.copy(alpha = 0.15f) else errorColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (systemStatus.ksuVersion != null) Icons.Outlined.TaskAlt else Icons.Outlined.Block,
                    contentDescription = null,
                    tint = if (systemStatus.ksuVersion != null) successColor else errorColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (systemStatus.ksuVersion != null) stringResource(R.string.home_working) else stringResource(R.string.home_unsupported),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (systemStatus.ksuVersion != null) successColor else errorColor
                    )

                    Spacer(Modifier.width(8.dp))

                    // Chips for Info
                    if (systemStatus.ksuVersion != null) {
                        HybridChip(
                            text = if (systemStatus.lkmMode == true) "LKM" else "BUILTIN",
                            bgColor = successColor.copy(alpha = 0.2f),
                            textColor = successColor
                        )

                        if (Os.uname().machine != "aarch64") {
                            Spacer(Modifier.width(6.dp))
                            HybridChip(
                                text = Os.uname().machine,
                                bgColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                textColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                val isHideVersion = LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getBoolean("is_hide_version", false)

                if (!isHideVersion && systemStatus.ksuFullVersion != null) {
                    Text(
                        text = systemStatus.ksuFullVersion,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HybridChip(text: String, bgColor: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        modifier = Modifier.height(20.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HybridInfoCard(
    systemInfo: HomeViewModel.SystemInfo,
    isSimpleMode: Boolean,
    isHideSusfsStatus: Boolean,
    isHideZygiskImplement: Boolean,
    isHideMetaModuleImplement: Boolean,
    showKpmInfo: Boolean,
    lkmMode: Boolean?
) {
    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = getCardElevation(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Text(
                text = "SYSTEM SPECS",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dense Rows - PERBAIKAN: Layout Modern Tanpa Titik Dua
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridInfoRow(Icons.Default.Memory, stringResource(R.string.home_kernel), systemInfo.kernelRelease)
                
                if (!isSimpleMode) {
                    HybridInfoRow(Icons.Default.Android, stringResource(R.string.home_android_version), systemInfo.androidVersion)
                }

                HybridInfoRow(Icons.Default.PhoneAndroid, stringResource(R.string.home_device_model), systemInfo.deviceModel)

                HybridInfoRow(
                    Icons.Default.SettingsSuggest, 
                    stringResource(R.string.home_manager_version), 
                    "${systemInfo.managerVersion.first} (${systemInfo.managerVersion.second.toInt()})"
                )

                HybridInfoRow(
                    Icons.Default.Security, 
                    stringResource(R.string.home_selinux_status), 
                    systemInfo.seLinuxStatus,
                    valueColor = if (systemInfo.seLinuxStatus.equals("Enforcing", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                // Logic for other infos (KPM, SuSFS, etc) - keep logic intact
                if (!isSimpleMode && !isHideZygiskImplement && systemInfo.zygiskImplement != "None") {
                     HybridInfoRow(Icons.Default.Adb, stringResource(R.string.home_zygisk_implement), systemInfo.zygiskImplement)
                }
                
                if (!isSimpleMode && !isHideMetaModuleImplement && systemInfo.metaModuleImplement != "None") {
                     HybridInfoRow(Icons.Default.Extension, stringResource(R.string.home_meta_module_implement), systemInfo.metaModuleImplement)
                }

                if (!isSimpleMode && !isHideSusfsStatus && systemInfo.suSFSStatus == "Supported" && systemInfo.suSFSVersion.isNotEmpty()) {
                     val infoText = buildString {
                        append(systemInfo.suSFSVersion)
                        append(" (${Natives.getHookType()})")
                    }
                    HybridInfoRow(Icons.Default.Storage, stringResource(R.string.home_susfs_version), infoText)
                }
            }
        }
    }
}

@Composable
fun HybridInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label, // Hanya Label, TANPA TITIK DUA
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 1.dp)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = valueColor,
                maxLines = 2
            )
        }
    }
}

// ================= END HYBRID UI =================

@Composable
fun WarningCard(
    message: String,
    color: Color = MaterialTheme.colorScheme.error,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = getCardColors(color),
        elevation = getCardElevation(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun ContributionCard() {
    val uriHandler = LocalUriHandler.current
    val links = listOf("https://github.com/ShirkNeko", "https://github.com/udochina")

    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainer),
        elevation = getCardElevation(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val randomIndex = Random.nextInt(links.size)
                    uriHandler.openUri(links[randomIndex])
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_ContributionCard_kernelsu),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_click_to_ContributionCard_kernelsu),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun LearnMoreCard() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.home_learn_kernelsu_url)

    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler.openUri(url)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_learn_kernelsu),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_click_to_learn_kernelsu),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun DonateCard() {
    val uriHandler = LocalUriHandler.current

    ElevatedCard(
        colors = getCardColors(MaterialTheme.colorScheme.surfaceContainer),
        elevation = getCardElevation(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler.openUri("https://patreon.com/weishu")
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_support_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_support_content),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun IncompatibleKernelCard() {
    val currentKver = remember { Natives.version }
    val threshold   = Natives.MINIMAL_NEW_IOCTL_KERNEL

    val msg = stringResource(
        id = R.string.incompatible_kernel_msg,
        currentKver,
        threshold
    )

    WarningCard(
        message = msg,
        color = MaterialTheme.colorScheme.error
    )
}

fun getManagerVersion(context: Context): Pair<String, Long> {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return Pair(packageInfo.versionName!!, versionCode)
}

@Preview
@Composable
private fun StatusCardPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HybridStatusCard(
            HomeViewModel.SystemStatus(
                isManager = true,
                ksuVersion = 1,
                lkmMode = null,
                kernelVersion = KernelVersion(5, 10, 101),
                isRootAvailable = true
            )
        )
    }
}

@Preview
@Composable
private fun WarningCardPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WarningCard(message = "Warning message")
        WarningCard(
            message = "Warning message ",
            MaterialTheme.colorScheme.outlineVariant,
            onClick = {})
    }
}
