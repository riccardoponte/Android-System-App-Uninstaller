package com.uninstaller.debloater.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uninstaller.debloater.data.AppRepository
import com.uninstaller.debloater.data.RemovedAppsStore
import com.uninstaller.debloater.engine.UninstallEngine
import com.uninstaller.debloater.model.AppCategoryFilter
import com.uninstaller.debloater.model.AppEntry
import com.uninstaller.debloater.model.ExecutionMode
import com.uninstaller.debloater.model.SafetyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ActiveTab {
    INSTALLED,
    RESTORE
}

data class UninstallerUiState(
    val isLoading: Boolean = true,
    val isBusyBatch: Boolean = false,
    val batchProgressText: String = "",
    val executionMode: ExecutionMode = ExecutionMode.STANDARD,
    val activeTab: ActiveTab = ActiveTab.INSTALLED,
    val categoryFilter: AppCategoryFilter = AppCategoryFilter.ALL,
    val searchQuery: String = "",
    val installedApps: List<AppEntry> = emptyList(),
    val removedSystemApps: List<AppEntry> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val totalInstalledCount: Int = 0,
    val userAppsCount: Int = 0,
    val systemAppsCount: Int = 0,
    val safeBloatwareCount: Int = 0,
    val statusBannerMessage: String? = null
)

class UninstallerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val removedStore = RemovedAppsStore(application)
    val engine = UninstallEngine(application)

    private val rawInstalledFlow = MutableStateFlow<List<AppEntry>>(emptyList())
    private val rawRemovedFlow = MutableStateFlow<List<AppEntry>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(true)
    private val isBusyBatchFlow = MutableStateFlow(false)
    private val batchProgressFlow = MutableStateFlow("")
    private val executionModeFlow = MutableStateFlow(engine.detectExecutionMode())
    private val activeTabFlow = MutableStateFlow(ActiveTab.INSTALLED)
    private val categoryFilterFlow = MutableStateFlow(AppCategoryFilter.ALL)
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedPackagesFlow = MutableStateFlow<Set<String>>(emptySet())
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    private val pendingStandardUninstallFlow = MutableStateFlow<List<AppEntry>>(emptyList())
    val pendingStandardUninstall: StateFlow<List<AppEntry>> = pendingStandardUninstallFlow

    val uiState: StateFlow<UninstallerUiState> = combine(
        combine(rawInstalledFlow, rawRemovedFlow, isLoadingFlow) { a, b, c -> Triple(a, b, c) },
        combine(isBusyBatchFlow, batchProgressFlow, executionModeFlow) { a, b, c -> Triple(a, b, c) },
        combine(activeTabFlow, categoryFilterFlow, searchQueryFlow) { a, b, c -> Triple(a, b, c) },
        selectedPackagesFlow,
        statusMessageFlow
    ) { (installed, removed, isLoading),
        (isBusy, progressText, execMode),
        (tab, catFilter, query),
        selected,
        statusMsg ->

        val q = query.trim().lowercase()

        val filteredInstalled = installed.filter { app ->
            val matchesCategory = when (catFilter) {
                AppCategoryFilter.ALL -> true
                AppCategoryFilter.USER_ONLY -> !app.isSystemApp
                AppCategoryFilter.SYSTEM_ONLY -> app.isSystemApp
                AppCategoryFilter.SAFE_BLOATWARE -> app.isSystemApp && app.safetyLevel == SafetyLevel.SAFE
            }
            val matchesQuery = q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)

            matchesCategory && matchesQuery
        }

        val filteredRemoved = removed.filter { app ->
            q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
        }

        UninstallerUiState(
            isLoading = isLoading,
            isBusyBatch = isBusy,
            batchProgressText = progressText,
            executionMode = execMode,
            activeTab = tab,
            categoryFilter = catFilter,
            searchQuery = query,
            installedApps = filteredInstalled,
            removedSystemApps = filteredRemoved,
            selectedPackages = selected,
            totalInstalledCount = installed.size,
            userAppsCount = installed.count { !it.isSystemApp },
            systemAppsCount = installed.count { it.isSystemApp },
            safeBloatwareCount = installed.count { it.isSystemApp && it.safetyLevel == SafetyLevel.SAFE },
            statusBannerMessage = statusMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UninstallerUiState()
    )

    init {
        refreshAll()
        tryBackgroundEmbeddedConnect()
    }

    private fun tryBackgroundEmbeddedConnect() {
        viewModelScope.launch {
            val connected = engine.tryAutoConnectEmbeddedAdb()
            executionModeFlow.value = engine.detectExecutionMode()
            if (connected) {
                statusMessageFlow.value = "Motore ADB Interno connesso automaticamente!"
            }
        }
    }

    fun refreshExecutionMode() {
        executionModeFlow.value = engine.detectExecutionMode()
    }

    fun autoConnectEmbeddedEngine() {
        viewModelScope.launch {
            isBusyBatchFlow.value = true
            batchProgressFlow.value = "Connessione al Motore ADB Interno in corso..."
            val connected = engine.tryAutoConnectEmbeddedAdb()
            isBusyBatchFlow.value = false
            batchProgressFlow.value = ""
            executionModeFlow.value = engine.detectExecutionMode()
            statusMessageFlow.value = if (connected) {
                "Motore ADB Interno attivo! Ora puoi rimuovere qualsiasi app di sistema in 1 tocco."
            } else {
                "Nessuna porta ADB aperta rilevata. Usa l'accoppiamento Debug Wireless integrato qui sopra."
            }
        }
    }

    fun connectEmbeddedByPort(port: Int) {
        viewModelScope.launch {
            isBusyBatchFlow.value = true
            batchProgressFlow.value = "Connessione a 127.0.0.1:$port in corso..."
            val connected = engine.connectEmbeddedAdbPort(port)
            isBusyBatchFlow.value = false
            batchProgressFlow.value = ""
            executionModeFlow.value = engine.detectExecutionMode()
            statusMessageFlow.value = if (connected) {
                "Connesso alla porta $port! Motore ADB Interno attivo."
            } else {
                "Impossibile connettersi alla porta $port. Verifica che il Debug Wireless sia attivo."
            }
        }
    }

    fun pairAndConnectEmbedded(pairingPort: Int, pairingCode: String, connectPort: Int?) {
        viewModelScope.launch {
            isBusyBatchFlow.value = true
            batchProgressFlow.value = "Accoppiamento con Debug Wireless sulla porta $pairingPort..."
            val res = engine.pairAndConnectEmbeddedAdb(pairingPort, pairingCode, connectPort)
            isBusyBatchFlow.value = false
            batchProgressFlow.value = ""
            executionModeFlow.value = engine.detectExecutionMode()
            statusMessageFlow.value = res.message
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            isLoadingFlow.value = true
            executionModeFlow.value = engine.detectExecutionMode()
            val savedRemoved = removedStore.removedAppsFlow.first()
            val result = repository.loadAllApps(savedRemoved)
            rawInstalledFlow.value = result.installedApps
            rawRemovedFlow.value = result.removedSystemApps
            val installedSet = result.installedApps.map { it.packageName }.toSet()
            selectedPackagesFlow.value = selectedPackagesFlow.value.intersect(installedSet)
            isLoadingFlow.value = false
        }
    }

    fun setActiveTab(tab: ActiveTab) {
        activeTabFlow.value = tab
    }

    fun setCategoryFilter(filter: AppCategoryFilter) {
        categoryFilterFlow.value = filter
    }

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    fun togglePackageSelection(app: AppEntry) {
        if (app.safetyLevel == SafetyLevel.CRITICAL) return
        val current = selectedPackagesFlow.value
        selectedPackagesFlow.value = if (app.packageName in current) {
            current - app.packageName
        } else {
            current + app.packageName
        }
    }

    fun selectAllSafeBloatware() {
        val bloatwarePkgs = rawInstalledFlow.value
            .filter { it.isSystemApp && it.safetyLevel == SafetyLevel.SAFE }
            .map { it.packageName }
            .toSet()
        selectedPackagesFlow.value = selectedPackagesFlow.value + bloatwarePkgs
    }

    fun clearSelection() {
        selectedPackagesFlow.value = emptySet()
    }

    fun dismissStatusMessage() {
        statusMessageFlow.value = null
    }

    fun uninstallApps(apps: List<AppEntry>, keepData: Boolean = false) {
        if (apps.isEmpty()) return
        refreshExecutionMode()
        val mode = executionModeFlow.value

        if (mode.canRemoveSystemAppsDirectly) {
            viewModelScope.launch {
                isBusyBatchFlow.value = true
                var successCount = 0
                var failCount = 0
                var lastError = ""

                apps.forEachIndexed { index, app ->
                    batchProgressFlow.value = "Disinstallazione ${index + 1}/${apps.size}: ${app.appName}..."
                    val res = engine.uninstallPackagePrivileged(app.packageName, keepData)
                    if (res.success) {
                        successCount++
                        if (app.isSystemApp) {
                            removedStore.recordRemovedApp(app.packageName, app.appName)
                        }
                    } else {
                        failCount++
                        lastError = res.message
                    }
                }

                isBusyBatchFlow.value = false
                batchProgressFlow.value = ""
                selectedPackagesFlow.value = selectedPackagesFlow.value - apps.map { it.packageName }.toSet()

                statusMessageFlow.value = when {
                    failCount == 0 && successCount == 1 -> "App disinstallata con successo!"
                    failCount == 0 -> "$successCount app disinstallate con successo!"
                    successCount > 0 -> "$successCount rimosse, $failCount non riuscite ($lastError)"
                    else -> "Impossibile disinstallare: $lastError"
                }

                refreshAll()
            }
        } else {
            pendingStandardUninstallFlow.value = apps
        }
    }

    fun consumeNextStandardUninstall(): AppEntry? {
        val current = pendingStandardUninstallFlow.value
        if (current.isEmpty()) return null
        val first = current.first()
        pendingStandardUninstallFlow.value = current.drop(1)
        selectedPackagesFlow.value = selectedPackagesFlow.value - first.packageName
        return first
    }

    fun restoreSystemApp(app: AppEntry) {
        viewModelScope.launch {
            isBusyBatchFlow.value = true
            batchProgressFlow.value = "Ripristino di ${app.appName} in corso..."
            val res = engine.restoreSystemPackagePrivileged(app.packageName)
            if (res.success) {
                removedStore.removeRestoredApp(app.packageName)
                statusMessageFlow.value = "'${app.appName}' è stata ripristinata sul telefono!"
            } else {
                statusMessageFlow.value = "Errore ripristino: ${res.message}"
            }
            isBusyBatchFlow.value = false
            batchProgressFlow.value = ""
            refreshAll()
        }
    }
}
