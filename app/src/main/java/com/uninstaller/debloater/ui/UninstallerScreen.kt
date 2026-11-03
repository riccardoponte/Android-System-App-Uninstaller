package com.uninstaller.debloater.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uninstaller.debloater.model.AppCategoryFilter
import com.uninstaller.debloater.model.AppEntry
import com.uninstaller.debloater.model.ExecutionMode
import com.uninstaller.debloater.model.SafetyLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UninstallerScreen(
    uiState: UninstallerUiState,
    onRefresh: () -> Unit,
    onAutoConnectEmbedded: () -> Unit,
    onConnectByPort: (Int) -> Unit,
    onPairAndConnect: (Int, String, Int?) -> Unit,
    onStartNotificationPairingAndOpenDevOptions: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onTabChange: (ActiveTab) -> Unit,
    onCategoryFilterChange: (AppCategoryFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSelectApp: (AppEntry) -> Unit,
    onSelectAllSafeBloatware: () -> Unit,
    onClearSelection: () -> Unit,
    onUninstallApps: (List<AppEntry>, Boolean) -> Unit,
    onRestoreSystemApp: (AppEntry) -> Unit,
    onOpenAppSettings: (String) -> Unit,
    onDismissStatusBanner: () -> Unit
) {
    var showEmbeddedEngineDialog by remember { mutableStateOf(false) }
    var appsPendingConfirm by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var keepDataOnUninstall by remember { mutableStateOf(false) }
    var systemAppWithoutEngineTarget by remember { mutableStateOf<AppEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "App Uninstaller Pro",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEmbeddedEngineDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Motore ADB Interno"
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Aggiorna lista"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.activeTab == ActiveTab.INSTALLED && uiState.selectedPackages.isNotEmpty()) {
                val selectedEntries = remember(uiState.installedApps, uiState.selectedPackages) {
                    uiState.installedApps.filter { it.packageName in uiState.selectedPackages }
                }
                val totalSizeMb = remember(selectedEntries) {
                    selectedEntries.sumOf { it.apkSizeBytes }.toDouble() / (1024.0 * 1024.0)
                }

                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedEntries.size} app selezionate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format("Spazio APK stimato: %.1f MB", totalSizeMb),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = onClearSelection) {
                            Text("Annulla")
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = { appsPendingConfirm = selectedEntries },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Disinstalla (${selectedEntries.size})")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Card Stato Motore Integrato
            ExecutionModeCard(
                mode = uiState.executionMode,
                onAutoConnect = onAutoConnectEmbedded,
                onRequestShizukuPermission = onRequestShizukuPermission,
                onOpenEngineDialog = { showEmbeddedEngineDialog = true }
            )

            // Messaggio di conferma o stato operazione
            if (uiState.statusBannerMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.statusBannerMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissStatusBanner,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Chiudi",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selettore Tab: Installate vs Ripristina Rimosse
            TabRow(
                selectedTabIndex = if (uiState.activeTab == ActiveTab.INSTALLED) 0 else 1,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = uiState.activeTab == ActiveTab.INSTALLED,
                    onClick = { onTabChange(ActiveTab.INSTALLED) },
                    text = {
                        Text(
                            text = "Installate (${uiState.totalInstalledCount})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
                Tab(
                    selected = uiState.activeTab == ActiveTab.RESTORE,
                    onClick = { onTabChange(ActiveTab.RESTORE) },
                    text = {
                        Text(
                            text = "Ripristina (${uiState.removedSystemApps.size})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra di ricerca
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cerca per nome app o pacchetto (es. bixby, facebook)...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Cancella")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Filtri per Categoria (visibili nel Tab Installate)
            if (uiState.activeTab == ActiveTab.INSTALLED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.categoryFilter == AppCategoryFilter.ALL,
                        onClick = { onCategoryFilterChange(AppCategoryFilter.ALL) },
                        label = { Text("Tutte (${uiState.totalInstalledCount})") }
                    )
                    FilterChip(
                        selected = uiState.categoryFilter == AppCategoryFilter.USER_ONLY,
                        onClick = { onCategoryFilterChange(AppCategoryFilter.USER_ONLY) },
                        label = { Text("Utente (${uiState.userAppsCount})") }
                    )
                    FilterChip(
                        selected = uiState.categoryFilter == AppCategoryFilter.SYSTEM_ONLY,
                        onClick = { onCategoryFilterChange(AppCategoryFilter.SYSTEM_ONLY) },
                        label = { Text("Sistema (${uiState.systemAppsCount})") }
                    )
                    FilterChip(
                        selected = uiState.categoryFilter == AppCategoryFilter.SAFE_BLOATWARE,
                        onClick = { onCategoryFilterChange(AppCategoryFilter.SAFE_BLOATWARE) },
                        label = { Text("Bloatware Sicuro (${uiState.safeBloatwareCount})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                if (uiState.categoryFilter == AppCategoryFilter.SAFE_BLOATWARE && uiState.safeBloatwareCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onSelectAllSafeBloatware) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Seleziona tutto il Bloatware sicuro")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Contenuto Principale
            if (uiState.isLoading || uiState.isBusyBatch) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.isBusyBatch) {
                                uiState.batchProgressText
                            } else {
                                "Scansione di tutte le app utente e di sistema..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.activeTab == ActiveTab.INSTALLED) {
                if (uiState.installedApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessuna applicazione corrisponde ai filtri.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.installedApps,
                            key = { it.packageName }
                        ) { app ->
                            InstalledAppCard(
                                app = app,
                                isSelected = app.packageName in uiState.selectedPackages,
                                onToggleSelect = { onToggleSelectApp(app) },
                                onUninstallClick = {
                                    if (app.isSystemApp && !uiState.executionMode.canRemoveSystemAppsDirectly) {
                                        systemAppWithoutEngineTarget = app
                                    } else {
                                        appsPendingConfirm = listOf(app)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // TAB RIPRISTINA APP RIMOSSE
                if (uiState.removedSystemApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nessuna app di sistema rimossa o disattivata.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.removedSystemApps,
                            key = { it.packageName }
                        ) { app ->
                            RemovedAppCard(
                                app = app,
                                onRestoreClick = { onRestoreSystemApp(app) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog Conferma Disinstallazione
    if (appsPendingConfirm.isNotEmpty()) {
        val hasCritical = appsPendingConfirm.any { it.safetyLevel == SafetyLevel.CRITICAL }
        val hasSystem = appsPendingConfirm.any { it.isSystemApp }

        AlertDialog(
            onDismissRequest = { appsPendingConfirm = emptyList() },
            icon = {
                Icon(
                    imageVector = if (hasCritical) Icons.Default.Warning else Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = if (appsPendingConfirm.size == 1) {
                        "Disinstallare ${appsPendingConfirm.first().appName}?"
                    } else {
                        "Disinstallare ${appsPendingConfirm.size} applicazioni?"
                    }
                )
            },
            text = {
                Column {
                    if (hasCritical) {
                        Text(
                            text = "ATTENZIONE: Hai selezionato un componente CRITICO di sistema. La rimozione può causare malfunzionamenti gravi del telefono.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (hasSystem) {
                        Text(
                            text = "Le app di sistema selezionate verranno rimosse per il tuo utente (potrai ripristinarle in qualsiasi momento dalla scheda 'Ripristina')."
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text("Le applicazioni selezionate verranno rimosse dal dispositivo.")
                    }

                    if (uiState.executionMode.canRemoveSystemAppsDirectly) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { keepDataOnUninstall = !keepDataOnUninstall }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = keepDataOnUninstall,
                                onCheckedChange = { keepDataOnUninstall = it }
                            )
                            Text(
                                text = "Mantieni dati e cache dell'app (-k)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = appsPendingConfirm
                        appsPendingConfirm = emptyList()
                        onUninstallApps(toRemove, keepDataOnUninstall)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Conferma Rimozione")
                }
            },
            dismissButton = {
                TextButton(onClick = { appsPendingConfirm = emptyList() }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Dialog quando l'utente tocca Elimina su un'app di sistema e il motore interno non è ancora connesso
    systemAppWithoutEngineTarget?.let { targetApp ->
        AlertDialog(
            onDismissRequest = { systemAppWithoutEngineTarget = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Rimozione App di Sistema: ${targetApp.appName}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Per disinstallare '${targetApp.appName}' con un tocco (senza installare app esterne), attiva il Motore ADB Interno integrato nell'app."
                    )
                    Text(
                        text = "In alternativa puoi aprire subito la scheda di sistema per disattivarla."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        systemAppWithoutEngineTarget = null
                        showEmbeddedEngineDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Attiva Motore Interno")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val pkg = targetApp.packageName
                        systemAppWithoutEngineTarget = null
                        onOpenAppSettings(pkg)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scheda Sistema")
                }
            }
        )
    }

    // Pannello Completo Motore ADB Interno (Senza App Esterne!)
    if (showEmbeddedEngineDialog) {
        var manualConnectPort by remember { mutableStateOf("5555") }
        var pairingPortText by remember { mutableStateOf("") }
        var pairingCodeText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEmbeddedEngineDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Motore ADB Interno (Zero App Esterne)")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Il motore di rimozione di sistema è integrato direttamente in questa app, senza bisogno di installare Shizuku o altri programmi.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Opzione 1: Riconnessione automatica 1-click
                    Button(
                        onClick = {
                            showEmbeddedEngineDialog = false
                            onAutoConnectEmbedded()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1. Connetti Automaticamente (1-Click)")
                    }

                    HorizontalDivider()

                    // Opzione 2: Accoppiamento Rapido da Notifica con Debug Wireless
                    Text(
                        text = "Prima configurazione dal telefono (con Debug Wireless):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Premi il pulsante qui sotto: apparirà una notifica in alto e si apriranno le Opzioni Sviluppatore. Entra in 'Debug Wireless' -> 'Accoppia con codice' e scrivi PORTA:CODICE direttamente nella notifica!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            showEmbeddedEngineDialog = false
                            onStartNotificationPairingAndOpenDevOptions()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Avvia Notifica + Apri Opzioni Sviluppatore")
                    }

                    HorizontalDivider()

                    // Opzione 3: Inserimento diretto porta / codice
                    Text(
                        text = "Oppure inserisci porta manuale:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualConnectPort,
                            onValueChange = { manualConnectPort = it },
                            label = { Text("Porta ADB") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val p = manualConnectPort.trim().toIntOrNull()
                                if (p != null) {
                                    showEmbeddedEngineDialog = false
                                    onConnectByPort(p)
                                }
                            }
                        ) {
                            Text("Connetti")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = pairingPortText,
                            onValueChange = { pairingPortText = it },
                            label = { Text("Porta Pair") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pairingCodeText,
                            onValueChange = { pairingCodeText = it },
                            label = { Text("Codice 6 cifre") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val pPort = pairingPortText.trim().toIntOrNull()
                                val cPort = manualConnectPort.trim().toIntOrNull()
                                if (pPort != null && pairingCodeText.isNotBlank()) {
                                    showEmbeddedEngineDialog = false
                                    onPairAndConnect(pPort, pairingCodeText.trim(), cPort)
                                }
                            }
                        ) {
                            Text("Pair")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmbeddedEngineDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }
}

@Composable
private fun ExecutionModeCard(
    mode: ExecutionMode,
    onAutoConnect: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onOpenEngineDialog: () -> Unit
) {
    val containerColor = when (mode) {
        ExecutionMode.EMBEDDED_ADB_READY,
        ExecutionMode.SHIZUKU_READY,
        ExecutionMode.ROOT_READY ->
            MaterialTheme.colorScheme.primaryContainer
        ExecutionMode.SHIZUKU_NEEDS_PERMISSION ->
            MaterialTheme.colorScheme.tertiaryContainer
        ExecutionMode.STANDARD ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when (mode) {
                        ExecutionMode.EMBEDDED_ADB_READY,
                        ExecutionMode.SHIZUKU_READY,
                        ExecutionMode.ROOT_READY -> Icons.Default.Bolt
                        ExecutionMode.SHIZUKU_NEEDS_PERMISSION -> Icons.Default.Warning
                        ExecutionMode.STANDARD -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = when (mode) {
                            ExecutionMode.EMBEDDED_ADB_READY -> "Motore ADB Interno ATTIVO (Integrato)"
                            ExecutionMode.ROOT_READY -> "Modalità Completa Attiva (Root)"
                            ExecutionMode.SHIZUKU_READY -> "Modalità Completa Attiva (ADB)"
                            ExecutionMode.SHIZUKU_NEEDS_PERMISSION -> "Servizio ADB rilevato!"
                            ExecutionMode.STANDARD -> "Motore ADB Interno in attesa"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (mode) {
                            ExecutionMode.EMBEDDED_ADB_READY,
                            ExecutionMode.SHIZUKU_READY,
                            ExecutionMode.ROOT_READY ->
                                "Rimozione e ripristino in 1 tocco attivi anche per le app di sistema"
                            ExecutionMode.SHIZUKU_NEEDS_PERMISSION ->
                                "Tocca 'Autorizza' per attivare la rimozione di sistema"
                            ExecutionMode.STANDARD ->
                                "Tocca 'Attiva' per connettere il motore interno senza app esterne"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (mode) {
                ExecutionMode.SHIZUKU_NEEDS_PERMISSION -> {
                    Button(onClick = onRequestShizukuPermission) {
                        Text("Autorizza")
                    }
                }
                ExecutionMode.STANDARD -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = onAutoConnect) {
                            Text("Connetti")
                        }
                        OutlinedButton(onClick = onOpenEngineDialog) {
                            Text("Configura")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun InstalledAppCard(
    app: AppEntry,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onUninstallClick: () -> Unit
) {
    val imageBitmap = remember(app.packageName, app.icon) {
        app.icon?.toImageBitmap(sizePx = 128)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = app.safetyLevel != SafetyLevel.CRITICAL,
                onClick = onToggleSelect
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.safetyLevel == SafetyLevel.CRITICAL) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Protetta",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SafetyBadge(safetyLevel = app.safetyLevel, isSystem = app.isSystemApp)
                }

                Text(
                    text = buildString {
                        append(app.packageName)
                        val sizeStr = app.formattedSize()
                        if (sizeStr.isNotEmpty()) {
                            append(" • ")
                            append(sizeStr)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = app.safetyReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (app.safetyLevel) {
                        SafetyLevel.SAFE -> Color(0xFF2E7D32)
                        SafetyLevel.CAUTION -> Color(0xFFE65100)
                        SafetyLevel.CRITICAL -> MaterialTheme.colorScheme.error
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onUninstallClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Disinstalla",
                    tint = if (app.safetyLevel == SafetyLevel.CRITICAL) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun RemovedAppCard(
    app: AppEntry,
    onRestoreClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(onClick = onRestoreClick) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ripristina")
            }
        }
    }
}

@Composable
private fun SafetyBadge(safetyLevel: SafetyLevel, isSystem: Boolean) {
    val (bg, fg, text) = when {
        !isSystem -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF1B5E20),
            "Utente"
        )
        safetyLevel == SafetyLevel.SAFE -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF1B5E20),
            "Bloatware Sicuro"
        )
        safetyLevel == SafetyLevel.CAUTION -> Triple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            "Sistema"
        )
        else -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFB71C1C),
            "Critica"
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun Drawable.toImageBitmap(sizePx: Int = 128): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap.asImageBitmap()
    }
    val w = if (intrinsicWidth > 0) intrinsicWidth.coerceAtMost(sizePx) else sizePx
    val h = if (intrinsicHeight > 0) intrinsicHeight.coerceAtMost(sizePx) else sizePx
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}
