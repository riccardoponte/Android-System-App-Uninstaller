package com.uninstaller.debloater.engine

import android.content.Context
import android.content.pm.PackageManager
import com.uninstaller.debloater.model.ExecutionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandResult(
    val success: Boolean,
    val message: String
)

class UninstallEngine(private val context: Context) {

    private val embeddedAdb = EmbeddedAdbManager.getInstance(context.applicationContext)

    fun detectExecutionMode(): ExecutionMode {
        // 1. Priorità assoluta: Motore ADB Interno integrato nell'app (Zero app esterne!)
        if (runCatching { embeddedAdb.isConnected }.getOrDefault(false)) {
            return ExecutionMode.EMBEDDED_ADB_READY
        }

        // 2. Verifica se il dispositivo ha permessi di Root (su)
        if (isRootAvailable()) {
            return ExecutionMode.ROOT_READY
        }

        // 3. Verifica se Shizuku è attivo sul dispositivo (opzionale)
        val shizukuAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (shizukuAlive) {
            val granted = runCatching {
                !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)

            return if (granted) {
                ExecutionMode.SHIZUKU_READY
            } else {
                ExecutionMode.SHIZUKU_NEEDS_PERMISSION
            }
        }

        // 4. Modalità Standard Android
        return ExecutionMode.STANDARD
    }

    suspend fun tryAutoConnectEmbeddedAdb(): Boolean {
        return embeddedAdb.tryAutoConnect(context)
    }

    suspend fun connectEmbeddedAdbPort(port: Int): Boolean {
        return embeddedAdb.connectToPort(port)
    }

    suspend fun pairAndConnectEmbeddedAdb(
        pairingPort: Int,
        pairingCode: String,
        connectPort: Int? = null
    ): CommandResult {
        return embeddedAdb.pairAndConnect(context, pairingPort, pairingCode, connectPort)
    }

    fun requestShizukuPermission(requestCode: Int = 2001): Boolean {
        return runCatching {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                Shizuku.requestPermission(requestCode)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Disinstalla qualsiasi app (utente o di sistema) per l'utente corrente (user 0)
     * usando il Motore ADB Interno integrato, Root o Shizuku.
     */
    suspend fun uninstallPackagePrivileged(
        packageName: String,
        keepData: Boolean = false
    ): CommandResult = withContext(Dispatchers.IO) {
        val flags = if (keepData) "-k --user 0" else "--user 0"
        val primaryCmd = "pm uninstall $flags $packageName"
        val res = executePrivilegedShell(primaryCmd)
        if (res.success) {
            return@withContext CommandResult(true, "Disinstallata con successo ($packageName)")
        }

        // Fallback se l'app è un pacchetto utente standard
        val fallbackRes = executePrivilegedShell("pm uninstall $packageName")
        if (fallbackRes.success) {
            return@withContext CommandResult(true, "Disinstallata con successo ($packageName)")
        }

        // Ultimo fallback: disattiva per l'utente 0
        val disableRes = executePrivilegedShell("pm disable-user --user 0 $packageName")
        if (disableRes.success) {
            return@withContext CommandResult(true, "App disattivata per l'utente ($packageName)")
        }

        res
    }

    /**
     * Ripristina un'app di sistema precedentemente disinstallata per l'utente 0.
     */
    suspend fun restoreSystemPackagePrivileged(packageName: String): CommandResult =
        withContext(Dispatchers.IO) {
            val res1 = executePrivilegedShell("cmd package install-existing $packageName")
            if (res1.success) {
                executePrivilegedShell("pm enable $packageName")
                return@withContext CommandResult(true, "App ripristinata: $packageName")
            }

            val res2 = executePrivilegedShell("pm install-existing --user 0 $packageName")
            if (res2.success) {
                executePrivilegedShell("pm enable $packageName")
                return@withContext CommandResult(true, "App ripristinata: $packageName")
            }

            res1
        }

    private suspend fun executePrivilegedShell(command: String): CommandResult {
        if (!embeddedAdb.isConnected) {
            embeddedAdb.tryAutoConnect(context)
        }
        return when (detectExecutionMode()) {
            ExecutionMode.EMBEDDED_ADB_READY -> embeddedAdb.executeShellCommand(command)
            ExecutionMode.ROOT_READY -> runViaRoot(command)
            ExecutionMode.SHIZUKU_READY -> runViaShizuku(command)
            else -> CommandResult(
                success = false,
                message = "Attiva prima il Motore ADB Interno toccando 'Attiva Motore Interno' in alto."
            )
        }
    }

    private fun runViaShizuku(command: String): CommandResult {
        return runCatching {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply {
                isAccessible = true
            }

            val process = method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            val combined = "$stdout\n$stderr".trim()

            val isSuccess = exitCode == 0 &&
                (combined.contains("Success", ignoreCase = true) ||
                    combined.contains("installed for user", ignoreCase = true) ||
                    combined.contains("new state", ignoreCase = true))

            CommandResult(
                success = isSuccess,
                message = combined.ifEmpty { "Exit code: $exitCode" }
            )
        }.getOrElse { e ->
            CommandResult(false, "Errore: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    private fun runViaRoot(command: String): CommandResult {
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            val combined = "$stdout\n$stderr".trim()

            val isSuccess = exitCode == 0 &&
                (combined.contains("Success", ignoreCase = true) ||
                    combined.contains("installed for user", ignoreCase = true) ||
                    combined.contains("new state", ignoreCase = true))

            CommandResult(
                success = isSuccess,
                message = combined.ifEmpty { "Exit code: $exitCode" }
            )
        }.getOrElse { e ->
            CommandResult(false, "Errore Root: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    private fun isRootAvailable(): Boolean {
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor() == 0 && output.contains("uid=0")
        }.getOrDefault(false)
    }
}
