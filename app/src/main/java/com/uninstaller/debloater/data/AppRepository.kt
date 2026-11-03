package com.uninstaller.debloater.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.uninstaller.debloater.model.AppEntry
import com.uninstaller.debloater.model.SafetyLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class LoadedAppsResult(
    val installedApps: List<AppEntry>,
    val removedSystemApps: List<AppEntry>
)

class AppRepository(private val context: Context) {

    suspend fun loadAllApps(recordedRemoved: Map<String, String>): LoadedAppsResult =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val ownPackage = context.packageName

            val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS
            val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(flags)
            }

            val installedList = mutableListOf<AppEntry>()
            val removedList = mutableListOf<AppEntry>()
            val seenPackages = mutableSetOf<String>()

            for (pkgInfo in packages) {
                val pkgName = pkgInfo.packageName
                if (pkgName == ownPackage) continue
                val appInfo = pkgInfo.applicationInfo ?: continue

                seenPackages.add(pkgName)

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val isInstalledForUser = (appInfo.flags and ApplicationInfo.FLAG_INSTALLED) != 0 &&
                    appInfo.enabled

                val label = runCatching {
                    appInfo.loadLabel(pm).toString().ifBlank { pkgName }
                }.getOrDefault(recordedRemoved[pkgName] ?: pkgName)

                val icon = runCatching {
                    appInfo.loadIcon(pm)
                }.getOrNull()

                val sizeBytes = runCatching {
                    appInfo.sourceDir?.let { File(it).length() } ?: 0L
                }.getOrDefault(0L)

                val (safety, reason) = SafetyClassifier.classify(
                    packageName = pkgName,
                    appName = label,
                    isSystemApp = isSystem
                )

                val entry = AppEntry(
                    packageName = pkgName,
                    appName = label,
                    versionName = pkgInfo.versionName ?: "",
                    icon = icon,
                    apkSizeBytes = sizeBytes,
                    isSystemApp = isSystem,
                    isUpdatedSystemApp = isUpdatedSystem,
                    isInstalledForCurrentUser = isInstalledForUser,
                    safetyLevel = safety,
                    safetyReason = reason
                )

                if (isInstalledForUser) {
                    installedList.add(entry)
                } else if (isSystem || pkgName in recordedRemoved) {
                    removedList.add(entry)
                }
            }

            // Aggiungi eventuali app registrate nello storico che non compaiono più in getInstalledPackages
            for ((pkgName, savedName) in recordedRemoved) {
                if (pkgName !in seenPackages) {
                    removedList.add(
                        AppEntry(
                            packageName = pkgName,
                            appName = savedName,
                            versionName = "",
                            icon = null,
                            apkSizeBytes = 0L,
                            isSystemApp = true,
                            isUpdatedSystemApp = false,
                            isInstalledForCurrentUser = false,
                            safetyLevel = SafetyLevel.SAFE,
                            safetyReason = "Disinstallata tramite App Uninstaller Pro • Pronta per il ripristino"
                        )
                    )
                }
            }

            LoadedAppsResult(
                installedApps = installedList.sortedWith(
                    compareBy<AppEntry> { it.isSystemApp }
                        .thenBy { it.appName.lowercase() }
                ),
                removedSystemApps = removedList.sortedBy { it.appName.lowercase() }
            )
        }
}
