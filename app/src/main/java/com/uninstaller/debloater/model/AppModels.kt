package com.uninstaller.debloater.model

import android.graphics.drawable.Drawable

enum class SafetyLevel {
    SAFE,
    CAUTION,
    CRITICAL;

    fun labelIt(): String = when (this) {
        SAFE -> "Sicura"
        CAUTION -> "Cautela"
        CRITICAL -> "Critica (Protetta)"
    }
}

enum class ExecutionMode {
    EMBEDDED_ADB_READY,
    SHIZUKU_READY,
    SHIZUKU_NEEDS_PERMISSION,
    ROOT_READY,
    STANDARD;

    val canRemoveSystemAppsDirectly: Boolean
        get() = this == EMBEDDED_ADB_READY || this == SHIZUKU_READY || this == ROOT_READY
}

enum class AppCategoryFilter {
    ALL,
    USER_ONLY,
    SYSTEM_ONLY,
    SAFE_BLOATWARE
}

data class AppEntry(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val icon: Drawable?,
    val apkSizeBytes: Long,
    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean,
    val isInstalledForCurrentUser: Boolean,
    val safetyLevel: SafetyLevel,
    val safetyReason: String
) {
    fun formattedSize(): String {
        if (apkSizeBytes <= 0L) return ""
        val mb = apkSizeBytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format("%.1f MB", mb)
        } else {
            val kb = apkSizeBytes.toDouble() / 1024.0
            String.format("%.0f KB", kb)
        }
    }
}
