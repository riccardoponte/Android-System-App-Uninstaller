package com.uninstaller.debloater.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WirelessPairingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PAIR_INPUT) return

        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val rawInput = results.getCharSequence(KEY_PAIR_TEXT)?.toString()?.trim().orEmpty()

        // Formato accettato: "PORTA:CODICE" o "PORTA CODICE" (es. "37192:482910" o "37192 482910")
        val parts = rawInput.split(":", " ", "-").map { it.trim() }.filter { it.isNotEmpty() }
        val port = parts.getOrNull(0)?.toIntOrNull()
        val code = parts.getOrNull(1)

        if (port == null || code.isNullOrBlank()) {
            showPairingNotification(
                context,
                statusText = "Formato non valido! Scrivi PORTA:CODICE (es. 38123:123456)"
            )
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = EmbeddedAdbManager.getInstance(context)
                val res = manager.pairAndConnect(context, port, code)
                showResultNotification(context, res.success, res.message)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAIR_INPUT = "com.uninstaller.debloater.ACTION_PAIR_INPUT"
        const val KEY_PAIR_TEXT = "key_pair_text"
        private const val CHANNEL_ID = "wireless_adb_pairing_channel"
        private const val NOTIFICATION_ID = 2002

        fun showPairingNotification(
            context: Context,
            statusText: String = "Apri Debug Wireless -> 'Accoppia con codice' e scrivi qui PORTA:CODICE (es. 39123:654321)"
        ) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Accoppiamento ADB Interno",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)

            val remoteInput = RemoteInput.Builder(KEY_PAIR_TEXT)
                .setLabel("Scrivi PORTA:CODICE (es. 39123:654321)")
                .build()

            val replyIntent = Intent(context, WirelessPairingReceiver::class.java).apply {
                action = ACTION_PAIR_INPUT
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                201,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val action = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                "Inserisci Porta:Codice",
                replyPendingIntent
            )
                .addRemoteInput(remoteInput)
                .build()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Accoppiamento Motore Interno (Senza App Esterne)")
                .setContentText(statusText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .addAction(action)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
        }

        private fun showResultNotification(context: Context, success: Boolean, message: String) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(
                    if (success) android.R.drawable.checkbox_on_background
                    else android.R.drawable.ic_dialog_alert
                )
                .setContentTitle(
                    if (success) "Motore Interno Attivo!" else "Accoppiamento Non Riuscito"
                )
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIFICATION_ID, notification)
        }
    }
}
