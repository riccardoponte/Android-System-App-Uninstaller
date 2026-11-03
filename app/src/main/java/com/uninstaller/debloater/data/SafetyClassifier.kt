package com.uninstaller.debloater.data

import com.uninstaller.debloater.model.SafetyLevel

object SafetyClassifier {

    // Pacchetti vitali del sistema Android / Samsung / Google che non devono mai essere rimossi
    private val criticalExactPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.providers.media",
        "com.android.providers.downloads",
        "com.android.providers.contacts",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.se",
        "com.android.shell",
        "com.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.samsung.android.incallui",
        "com.samsung.android.dialer",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
        "moe.shizuku.privileged.api"
    )

    private val criticalPrefixes = listOf(
        "com.android.internal.",
        "com.sec.ims",
        "com.samsung.android.ims",
        "com.qualcomm.qti.telephony"
    )

    // App preinstallate / Bloatware comuni che sono completamente sicure da rimuovere
    private val safePreinstalledKeywords = listOf(
        "facebook",
        "instagram",
        "tiktok",
        "netflix",
        "spotify",
        "linkedin",
        "onedrive",
        "microsoft.office",
        "outlook",
        "bixby",
        "arzone",
        "aremoji",
        "samsungfree",
        "sree",
        "spage",
        "gamehome",
        "gamelauncher",
        "game optimizing",
        "gameplugins",
        "kidsinstaller",
        "samsungpass",
        "samsungpay",
        "tips",
        "members",
        "globalgoals",
        "google.android.youtube",
        "google.android.apps.youtube.music",
        "google.android.videos",
        "google.android.apps.tachyon", // Google Meet / Duo
        "google.android.apps.docs",
        "google.android.apps.maps",
        "google.android.apps.photos",
        "google.android.apps.magazines",
        "google.android.apps.podcasts",
        "google.android.keep",
        "android.chrome",
        "sec.android.app.sbrowser"
    )

    fun classify(
        packageName: String,
        appName: String,
        isSystemApp: Boolean
    ): Pair<SafetyLevel, String> {
        val pkgLower = packageName.lowercase()
        val nameLower = appName.lowercase()

        if (!isSystemApp) {
            if (pkgLower == "moe.shizuku.privileged.api") {
                return SafetyLevel.CAUTION to "Necessaria per disinstallare le app di sistema senza Root"
            }
            return SafetyLevel.SAFE to "App installata dall'utente • Rimozione sicura"
        }

        if (pkgLower in criticalExactPackages || criticalPrefixes.any { pkgLower.startsWith(it) }) {
            return SafetyLevel.CRITICAL to "Componente essenziale del sistema operativo (Non rimuovere)"
        }

        if (safePreinstalledKeywords.any { kw -> pkgLower.contains(kw) || nameLower.contains(kw) }) {
            return SafetyLevel.SAFE to "App preinstallata / Bloatware • Rimozione sicura (ripristinabile)"
        }

        return SafetyLevel.CAUTION to "App di sistema • Rimuovi solo se non usi questa funzione"
    }
}
