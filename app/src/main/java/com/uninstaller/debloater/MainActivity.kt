package com.uninstaller.debloater

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uninstaller.debloater.engine.WirelessPairingReceiver
import com.uninstaller.debloater.ui.UninstallerScreen
import com.uninstaller.debloater.ui.UninstallerViewModel
import com.uninstaller.debloater.ui.theme.AppUninstallerTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: UninstallerViewModel by viewModels()

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, _ ->
            viewModel.refreshExecutionMode()
        }

    private val shizukuBinderReceivedListener =
        Shizuku.OnBinderReceivedListener {
            viewModel.refreshExecutionMode()
        }

    private val shizukuBinderDeadListener =
        Shizuku.OnBinderDeadListener {
            viewModel.refreshExecutionMode()
        }

    private val notificationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        WirelessPairingReceiver.showPairingNotification(this)
        openDeveloperOptions()
    }

    private val standardUninstallLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val next = viewModel.consumeNextStandardUninstall()
        if (next != null) {
            launchSystemDeleteIntent(next.packageName)
        } else {
            viewModel.refreshAll()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        runCatching {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
            Shizuku.addBinderReceivedListenerSticky(shizukuBinderReceivedListener)
            Shizuku.addBinderDeadListener(shizukuBinderDeadListener)
        }

        setContent {
            AppUninstallerTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val pendingStandardQueue by viewModel.pendingStandardUninstall.collectAsStateWithLifecycle()

                LaunchedEffect(pendingStandardQueue) {
                    if (pendingStandardQueue.isNotEmpty()) {
                        val next = viewModel.consumeNextStandardUninstall()
                        if (next != null) {
                            launchSystemDeleteIntent(next.packageName)
                        }
                    }
                }

                UninstallerScreen(
                    uiState = uiState,
                    onRefresh = viewModel::refreshAll,
                    onAutoConnectEmbedded = viewModel::autoConnectEmbeddedEngine,
                    onConnectByPort = viewModel::connectEmbeddedByPort,
                    onPairAndConnect = viewModel::pairAndConnectEmbedded,
                    onStartNotificationPairingAndOpenDevOptions = {
                        startNotificationPairingFlow()
                    },
                    onRequestShizukuPermission = {
                        val requested = viewModel.engine.requestShizukuPermission()
                        if (!requested) {
                            Toast.makeText(
                                this,
                                "Usa il pulsante 'Connetti' per attivare il Motore Interno.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onTabChange = viewModel::setActiveTab,
                    onCategoryFilterChange = viewModel::setCategoryFilter,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onToggleSelectApp = viewModel::togglePackageSelection,
                    onSelectAllSafeBloatware = viewModel::selectAllSafeBloatware,
                    onClearSelection = viewModel::clearSelection,
                    onUninstallApps = viewModel::uninstallApps,
                    onRestoreSystemApp = viewModel::restoreSystemApp,
                    onOpenAppSettings = { pkg ->
                        openAppDetailsSettings(pkg)
                    },
                    onDismissStatusBanner = viewModel::dismissStatusMessage
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshExecutionMode()
    }

    override fun onDestroy() {
        runCatching {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
            Shizuku.removeBinderReceivedListener(shizukuBinderReceivedListener)
            Shizuku.removeBinderDeadListener(shizukuBinderDeadListener)
        }
        super.onDestroy()
    }

    private fun startNotificationPairingFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        WirelessPairingReceiver.showPairingNotification(this)
        openDeveloperOptions()
    }

    private fun openDeveloperOptions() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        runCatching {
            startActivity(intent)
        }.onFailure {
            runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
    }

    private fun launchSystemDeleteIntent(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        runCatching {
            standardUninstallLauncher.launch(intent)
        }.onFailure {
            openAppDetailsSettings(packageName)
        }
    }

    private fun openAppDetailsSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        runCatching { startActivity(intent) }
    }
}
