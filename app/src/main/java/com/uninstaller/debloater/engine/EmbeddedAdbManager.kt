package com.uninstaller.debloater.engine

import android.content.Context
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.security.cert.Certificate

class EmbeddedAdbManager private constructor(context: Context) : AbsAdbConnectionManager() {

    private val privateKey: PrivateKey
    private val certificate: Certificate

    init {
        api = Build.VERSION.SDK_INT
        val (pk, cert) = SelfSignedCertGenerator.loadOrGenerate(context.applicationContext)
        privateKey = pk
        certificate = cert
    }

    override fun getPrivateKey(): PrivateKey = privateKey

    override fun getCertificate(): Certificate = certificate

    override fun getDeviceName(): String = "AppUninstallerPro"

    suspend fun tryAutoConnect(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isConnected) return@withContext true

        // 1. Prova prima la porta standard TCP locale 5555 (se abilitata via USB o ADB TCP)
        val connected5555 = runCatching {
            connect("127.0.0.1", 5555)
        }.getOrDefault(false)
        if (connected5555 && isConnected) return@withContext true

        // 2. Prova auto-discovery mDNS (Debug Wireless TLS / TCP su Android 11+)
        val autoRes = runCatching {
            autoConnect(context.applicationContext, 3500)
        }.getOrDefault(false)
        if (autoRes && isConnected) return@withContext true

        false
    }

    suspend fun connectToPort(port: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (isConnected) disconnect()
            connect("127.0.0.1", port)
        }.getOrDefault(false)
    }

    suspend fun pairAndConnect(
        context: Context,
        pairingPort: Int,
        pairingCode: String,
        connectPort: Int? = null
    ): CommandResult = withContext(Dispatchers.IO) {
        val paired = runCatching {
            pair("127.0.0.1", pairingPort, pairingCode.trim())
        }.getOrElse { e ->
            return@withContext CommandResult(
                false,
                "Errore accoppiamento: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }

        if (!paired) {
            return@withContext CommandResult(
                false,
                "Accoppiamento rifiutato. Verifica che la porta e il codice a 6 cifre siano corretti."
            )
        }

        // Dopo il pairing, connettiti automaticamente tramite mDNS o porta specificata
        val connected = if (connectPort != null && connectPort > 0) {
            runCatching { connect("127.0.0.1", connectPort) }.getOrDefault(false)
        } else {
            runCatching { autoConnect(context.applicationContext, 6000) }.getOrDefault(false)
        }

        if (connected && isConnected) {
            CommandResult(true, "Motore ADB Interno accoppiato e attivo!")
        } else {
            CommandResult(
                true,
                "Accoppiamento riuscito! Ora tocca 'Riconnetti' o inserisci la porta principale del Debug Wireless."
            )
        }
    }

    suspend fun executeShellCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        runCatching {
            if (!isConnected) {
                return@withContext CommandResult(false, "Motore ADB interno non connesso.")
            }

            openStream("shell:$command").use { stream ->
                val output = BufferedReader(
                    InputStreamReader(stream.openInputStream(), StandardCharsets.UTF_8)
                ).use { reader ->
                    val sb = StringBuilder()
                    val buf = CharArray(1024)
                    var n: Int
                    while (reader.read(buf).also { n = it } != -1) {
                        sb.append(buf, 0, n)
                    }
                    sb.toString().trim()
                }

                val isSuccess = output.contains("Success", ignoreCase = true) ||
                    output.contains("installed for user", ignoreCase = true) ||
                    output.contains("new state", ignoreCase = true)

                CommandResult(
                    success = isSuccess,
                    message = output.ifEmpty { "Comando eseguito" }
                )
            }
        }.getOrElse { e ->
            CommandResult(
                false,
                "Errore ADB interno: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: EmbeddedAdbManager? = null

        fun getInstance(context: Context): EmbeddedAdbManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmbeddedAdbManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
