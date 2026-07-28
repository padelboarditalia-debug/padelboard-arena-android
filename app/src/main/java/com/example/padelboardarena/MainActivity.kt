package com.example.padelboardarena

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.padelboardarena.arena.ArenaApiClient
import com.example.padelboardarena.arena.ArenaApiResult
import com.example.padelboardarena.arena.ArenaApiScoreSnapshotSender
import com.example.padelboardarena.arena.ArenaAuthClient
import com.example.padelboardarena.arena.AndroidKeystoreArenaSessionStore
import com.example.padelboardarena.arena.ArenaBuildConfig
import com.example.padelboardarena.arena.ArenaCourtSelection
import com.example.padelboardarena.arena.ArenaCourtSelectionStore
import com.example.padelboardarena.arena.ArenaLiveMatch
import com.example.padelboardarena.arena.ArenaLiveMatchClient
import com.example.padelboardarena.arena.ArenaLiveMatchResponse
import com.example.padelboardarena.arena.ArenaLiveScoreAdoptionClient
import com.example.padelboardarena.arena.ArenaLiveScoreAdoptionRequest
import com.example.padelboardarena.arena.ArenaLastClosedMatch
import com.example.padelboardarena.arena.ArenaLastClosedMatchStore
import com.example.padelboardarena.arena.SharedPreferencesArenaLifecycleSequenceStore
import com.example.padelboardarena.arena.ArenaManualUiMessages
import com.example.padelboardarena.arena.ArenaMatchLifecycleClient
import com.example.padelboardarena.arena.ArenaMatchLifecycleResult
import com.example.padelboardarena.arena.ArenaMatchLifecycleScoreSnapshot
import com.example.padelboardarena.arena.ArenaMatchReopenRequest
import com.example.padelboardarena.arena.ArenaOperationModeStore
import com.example.padelboardarena.arena.ArenaRealScoreSnapshotFactory
import com.example.padelboardarena.arena.ArenaRealScoreState
import com.example.padelboardarena.arena.ArenaRealScoreSync
import com.example.padelboardarena.arena.ArenaScoreSnapshot
import com.example.padelboardarena.arena.ArenaSessionRestoreResult
import com.example.padelboardarena.arena.SharedPreferencesArenaManualSequenceStore
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "padelboard_arena"

        private const val PREF_DEVICE_A = "device_a"
        private const val PREF_DEVICE_B = "device_b"

        private const val PREF_POINTS_A = "points_a"
        private const val PREF_POINTS_B = "points_b"

        private const val PREF_GAMES_A = "games_a"
        private const val PREF_GAMES_B = "games_b"
        private const val PREF_SETS_A = "sets_a"
        private const val PREF_SETS_B = "sets_b"
        private const val PREF_TIE_BREAK_ACTIVE = "tie_break_active"
        private const val PREF_TIE_BREAK_POINTS_A = "tie_break_points_a"
        private const val PREF_TIE_BREAK_POINTS_B = "tie_break_points_b"
        private const val PREF_LOCAL_MATCH_FINISHED = "local_match_finished"
        private const val PREF_MATCH_LIFECYCLE_STATE = "match_lifecycle_state"
        private const val PREF_LAST_FINISHED_COURT_ID = "last_finished_court_id"
        private const val PREF_LAST_FINISHED_MATCH_ID = "last_finished_match_id"
        private const val PREF_LAST_FINISH_EVENT_ID = "last_finish_event_id"
        private const val PREF_LAST_FINISH_EVENT_SEQUENCE = "last_finish_event_sequence"
        private const val PREF_LAST_FINISHED_POINTS_A = "last_finished_points_a"
        private const val PREF_LAST_FINISHED_POINTS_B = "last_finished_points_b"
        private const val PREF_LAST_FINISHED_GAMES_A = "last_finished_games_a"
        private const val PREF_LAST_FINISHED_GAMES_B = "last_finished_games_b"
        private const val PREF_LAST_FINISHED_SETS_A = "last_finished_sets_a"
        private const val PREF_LAST_FINISHED_SETS_B = "last_finished_sets_b"
        private const val PREF_LAST_FINISHED_ADVANTAGE_SIDE = "last_finished_advantage_side"
        private const val PREF_LAST_FINISHED_KILLER_MODE = "last_finished_killer_mode"
        private const val PREF_LAST_FINISHED_TIE_BREAK_ACTIVE = "last_finished_tie_break_active"
        private const val PREF_LAST_FINISHED_TIE_BREAK_POINTS_A = "last_finished_tie_break_points_a"
        private const val PREF_LAST_FINISHED_TIE_BREAK_POINTS_B = "last_finished_tie_break_points_b"
        private const val PREF_LAST_FINISHED_LOCAL_MATCH_FINISHED = "last_finished_local_match_finished"
        private const val PREF_CURRENT_LIVE_MATCH_ID = "current_live_match_id"
        private const val PREF_LAST_ADOPTED_LIVE_SCORE_KEY = "last_adopted_live_score_key"

        private const val PREF_ADVANTAGE_SIDE = "advantage_side"
        private const val PREF_KILLER_MODE = "killer_mode"

        private const val BUTTON_OBJECT_ID = 0x3A
        private const val PACKET_ID_OBJECT_ID = 0x00
        private const val BATTERY_OBJECT_ID = 0x01

        /*
         * Manufacturer ID ufficiale Shelly / Allterco.
         */
        private const val SHELLY_MANUFACTURER_ID = 0x0BA9

        /*
         * Se manca il packet ID, usiamo questa finestra
         * temporale come protezione di riserva.
         */
        private const val FALLBACK_DUPLICATE_WINDOW_MS = 2_000L

        /*
         * Dopo aver premuto "Associa", aspettiamo un istante
         * per evitare di catturare la coda del pacchetto precedente.
         */
        private const val ASSIGNMENT_ARM_DELAY_MS = 700L

        private const val ARENA_LOG_TAG =
            "PadelBoardArena"

        private const val SCORE_FLASH_DURATION_MS = 5_000L
        private const val SCORE_FLASH_PULSE_COUNT = 5
        private const val SCORE_FLASH_RISE_MS = 350L
        private const val SCORE_FLASH_HOLD_MS = 200L
        private const val SCORE_FLASH_FALL_MS = 350L
        private const val SCORE_FLASH_PAUSE_MS = 100L

        private const val NUMERIC_SCORE_TEXT_SIZE_SP = 360
        private const val NUMERIC_SCORE_AUTO_MIN_SP = 140
        private const val NUMERIC_SCORE_AUTO_MAX_SP = 430
        private const val ADV_SCORE_TEXT_SIZE_SP = 200
        private const val ADV_SCORE_AUTO_MIN_SP = 96
        private const val ADV_SCORE_AUTO_MAX_SP = 220
        private const val SCORE_TEXT_AUTOSIZE_STEP_SP = 4
        private const val LIVE_MATCH_POLLING_INTERVAL_MS = 15_000L
        private const val BLE_WATCHDOG_INTERVAL_MS = 30_000L
        private const val BLE_INACTIVITY_RESTART_MS = 180_000L
        private const val BLE_MIN_SCAN_AGE_BEFORE_RESTART_MS = 180_000L
        private const val BLE_RESTART_DELAY_MS = 1_000L
        private const val BLE_SCAN_FAILURE_RETRY_DELAY_MS = 5_000L
        private const val DEFAULT_TEAM_A_LABEL = "Squadra A"
        private const val DEFAULT_TEAM_B_LABEL = "Squadra B"
    }

    private lateinit var statusText: TextView
    private lateinit var phaseText: TextView
    private lateinit var eventText: TextView

    private lateinit var scoreAText: TextView
    private lateinit var scoreBText: TextView
    private lateinit var teamAText: TextView
    private lateinit var teamBText: TextView

    private lateinit var gamesAText: TextView
    private lateinit var gamesBText: TextView
    private lateinit var setsAText: TextView
    private lateinit var setsBText: TextView

    private lateinit var deviceAText: TextView
    private lateinit var deviceBText: TextView

    private lateinit var startButton: Button
    private lateinit var arenaSettingsButton: ImageButton
    private lateinit var helpText: TextView
    private lateinit var arenaConnectionStatusText: TextView
    private lateinit var arenaLastCallText: TextView
    private lateinit var scoreAnnouncer: ArenaScoreAnnouncer

    private var scoreAnimationA: AnimatorSet? = null
    private var scoreAnimationB: AnimatorSet? = null
    private var gameAnimationA: AnimatorSet? = null
    private var gameAnimationB: AnimatorSet? = null
    private var teamALabel =
        DEFAULT_TEAM_A_LABEL
    private var teamBLabel =
        DEFAULT_TEAM_B_LABEL
    private var liveMatchRequestInFlight = false
    private var liveMatchPollingActive = false
    private var liveMatchClientGeneration = 0
    private var activityVisible = false
    private var bleWatchdogActive = false
    private var bleScanRestartPending = false
    private var bluetoothReceiverRegistered = false
    private var arenaCourtSelection: ArenaCourtSelection? = null
    private var arenaApiClient: ArenaApiClient? = null
    private var arenaLiveMatchClient: ArenaLiveMatchClient? = null
    private var arenaLiveScoreAdoptionClient: ArenaLiveScoreAdoptionClient? = null
    private var arenaRealScoreSync: ArenaRealScoreSync? = null
    private var arenaMatchLifecycleClient: ArenaMatchLifecycleClient? = null
    private var currentLiveMatchId: String? = null
    private var lastAdoptedLiveScoreKey: String? = null
    private var matchLifecycleState = MatchLifecycleState.ACTIVE
    private var lastFinishedCourtId: String? = null
    private var lastFinishedMatchId: String? = null
    private var lastFinishedSnapshot: ScoreSnapshot? = null
    private var lastFinishEventId: String? = null
    private var lastFinishEventSequence: Int = 0
    private var reopenedClosedMatchForCorrection = false

    private val liveMatchPollingHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val liveMatchPollingRunnable =
        object : Runnable {
            override fun run() {
                refreshLiveMatchIfNeeded()
                scheduleNextLiveMatchPoll()
            }
        }

    private val bleWatchdogHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val bleWatchdogRunnable =
        object : Runnable {
            override fun run() {
                runBleWatchdogCheck()
                scheduleNextBleWatchdogCheck()
            }
        }

    private val bluetoothStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (
                    intent?.action !=
                    BluetoothAdapter.ACTION_STATE_CHANGED
                ) {
                    return
                }

                when (
                    intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                ) {
                    BluetoothAdapter.STATE_OFF -> {
                        scanning = false
                        bleScanStartedAt = 0L
                        statusText.text =
                            "Bluetooth non disponibile"
                        logBleDebug(
                            "Bluetooth off"
                        )
                    }

                    BluetoothAdapter.STATE_ON -> {
                        logBleDebug(
                            "Bluetooth on"
                        )
                        if (
                            activityVisible &&
                            hasAnyShellyAssociation()
                        ) {
                            restartBleScanSafely(
                                reason = "bluetooth_on",
                                delayMs = BLE_RESTART_DELAY_MS
                            )
                        }
                    }
                }
            }
        }

    private val pointFlashColor =
        Color.rgb(70, 255, 120)
    private val undoFlashColor =
        Color.rgb(255, 80, 80)

    private var scanning = false
    private var lastBlePacketReceivedAt = 0L
    private var lastBleButtonEventAt = 0L
    private var bleScanStartedAt = 0L

    /*
     * Qui salviamo l'identificativo stabile ottenuto
     * dal MAC Shelly nei manufacturer data.
     */
    private var deviceA: String? = null
    private var deviceB: String? = null

    /*
     * Punti interni:
     *
     * 0 = 0
     * 1 = 15
     * 2 = 30
     * 3 = 40
     */
    private var pointsA = 0
    private var pointsB = 0

    private var gamesA = 0
    private var gamesB = 0
    private var setsA = 0
    private var setsB = 0

    private var advantageSide: Side? = null
    private var killerMode = false
    private var tieBreakActive = false
    private var tieBreakPointsA = 0
    private var tieBreakPointsB = 0
    private var localMatchFinished = false
    private var standaloneClassicMode = false

    private var assignmentMode: AssignmentMode? = null
    private var assignmentArmedAt = 0L

    /*
     * Ultimo packet ID ricevuto per ciascun pulsante.
     * È la protezione principale contro i punti multipli.
     */
    private val lastPacketIdByDevice =
        mutableMapOf<String, Int>()

    /*
     * Protezione di riserva nel caso il packet ID non sia presente.
     */
    private val lastFallbackEventByDevice =
        mutableMapOf<String, FallbackEvent>()

    private val scoreHistory =
        mutableListOf<ScoreSnapshot>()

    private val arenaManualExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val preferences by lazy {
        getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    private val arenaConfig by lazy {
        ArenaBuildConfig.load()
    }

    private val arenaSessionStore by lazy {
        AndroidKeystoreArenaSessionStore(
            this
        )
    }

    private val arenaCourtSelectionStore by lazy {
        ArenaCourtSelectionStore(
            preferences
        )
    }

    private val arenaOperationModeStore by lazy {
        ArenaOperationModeStore(
            preferences
        )
    }

    private val arenaSequenceStore by lazy {
        SharedPreferencesArenaManualSequenceStore(
            preferences
        )
    }

    private val arenaLifecycleSequenceStore by lazy {
        SharedPreferencesArenaLifecycleSequenceStore(
            preferences
        )
    }

    private val lastClosedMatchStore by lazy {
        ArenaLastClosedMatchStore(
            preferences
        )
    }

    private val arenaAuthClient by lazy {
        ArenaAuthClient(
            config = arenaConfig,
            sessionStore = arenaSessionStore
        )
    }

    private val bluetoothManager by lazy {
        getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager
    }

    private val bthomeUuid = ParcelUuid(
        UUID.fromString(
            "0000fcd2-0000-1000-8000-00805f9b34fb"
        )
    )

    private val arenaSettingsLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val settingsRequest =
                result.data?.getStringExtra(
                    ArenaSettingsActivity.EXTRA_SETTINGS_REQUEST
                )

            reloadArenaOperationMode()

            when (settingsRequest) {
                ArenaSettingsActivity.REQUEST_ASSIGN_SIDE_A ->
                    handleSettingsAssignmentRequest(
                        AssignmentMode.SIDE_A
                    )

                ArenaSettingsActivity.REQUEST_ASSIGN_SIDE_B ->
                    handleSettingsAssignmentRequest(
                        AssignmentMode.SIDE_B
                    )

                ArenaSettingsActivity.REQUEST_RESET_SCORE ->
                    if (standaloneClassicMode) {
                        resetMatch()
                    } else {
                        resetLocalScoreWithoutArenaSync(
                            statusMessage =
                                "Tabellone azzerato - riallineamento PadelBoard",
                            eventMessage =
                                "In attesa del punteggio ufficiale"
                        )
                        reloadArenaCourtSelection()
                        bootstrapArenaSession()
                        refreshLiveMatchIfNeeded()
                    }

                ArenaSettingsActivity.REQUEST_REOPEN_LAST_FINISHED_MATCH ->
                    reopenLastClosedMatchForCorrection()

                else -> {
                    reloadArenaCourtSelection()
                    bootstrapArenaSession()
                }
            }
        }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val allGranted =
                permissions.values.all { granted ->
                    granted
                }

            if (allGranted) {
                startBleScan()
            } else {
                statusText.text =
                    "Permessi Bluetooth o posizione negati"
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setContentView(R.layout.activity_main)

        bindViews()
        scoreAnnouncer =
            ArenaScoreAnnouncer(
                this
            )
        reloadArenaOperationMode()
        loadSavedData()
        updateScreen()
        reloadArenaCourtSelection()
        startBleScanIfDevicesAssigned()
        bootstrapArenaSession()
        registerBluetoothStateReceiver()


        arenaSettingsButton.setOnClickListener {
            openArenaSettings()
        }
    }

    override fun onStart() {
        super.onStart()
        activityVisible = true
        startBleScanIfDevicesAssigned()
        startBleWatchdogIfNeeded()
        if (!standaloneClassicMode) {
            startLiveMatchPolling()
        }
    }

    override fun onResume() {
        super.onResume()
        startBleScanIfDevicesAssigned()
        startBleWatchdogIfNeeded()
    }

    override fun onStop() {
        activityVisible = false
        stopBleWatchdog()
        stopLiveMatchPolling()
        super.onStop()
    }

    private fun bindViews() {
        statusText =
            findViewById(R.id.statusText)

        phaseText =
            findViewById(R.id.phaseText)

        eventText =
            findViewById(R.id.eventText)

        scoreAText =
            findViewById(R.id.scoreAText)

        scoreBText =
            findViewById(R.id.scoreBText)

        teamAText =
            findViewById(R.id.teamAText)

        teamBText =
            findViewById(R.id.teamBText)

        gamesAText =
            findViewById(R.id.gamesAText)

        gamesBText =
            findViewById(R.id.gamesBText)

        setsAText =
            findViewById(R.id.setsAText)

        setsBText =
            findViewById(R.id.setsBText)

        deviceAText =
            findViewById(R.id.deviceAText)

        deviceBText =
            findViewById(R.id.deviceBText)

        startButton =
            findViewById(R.id.startButton)

        arenaSettingsButton =
            findViewById(R.id.arenaSettingsButton)

        helpText =
            findViewById(R.id.helpText)

        arenaConnectionStatusText =
            findViewById(R.id.arenaConnectionStatusText)

        arenaLastCallText =
            findViewById(R.id.arenaLastCallText)
    }

    private fun bootstrapArenaSession() {
        if (standaloneClassicMode) {
            arenaConnectionStatusText.text =
                "Modalità autonoma"
            arenaLastCallText.text =
                "Modalità autonoma"
            return
        }

        arenaConnectionStatusText.text =
            "Arena: ripristino sessione"

        arenaManualExecutor.execute {
            val result =
                arenaAuthClient.restorePersistedSession()

            runOnUiThread {
                when (result) {
                    ArenaSessionRestoreResult.RESTORED -> {
                        arenaConnectionStatusText.text =
                            "Arena connessa"
                        if (arenaCourtSelection == null) {
                            arenaLastCallText.text =
                                "Seleziona campo Arena"
                        } else {
                            refreshLiveMatchIfNeeded()
                        }
                    }

                    ArenaSessionRestoreResult.MISSING -> {
                        arenaConnectionStatusText.text =
                            "Login Arena richiesto"
                    }

                    ArenaSessionRestoreResult.FAILED -> {
                        arenaConnectionStatusText.text =
                            "Login Arena richiesto"
                    }
                }
            }
        }
    }

    private fun startLiveMatchPolling() {
        if (standaloneClassicMode) {
            liveMatchPollingActive = false
            arenaLastCallText.text =
                "Modalità autonoma"
            return
        }

        if (arenaCourtSelection == null || arenaLiveMatchClient == null) {
            liveMatchPollingActive = false
            arenaLastCallText.text =
                "Seleziona campo Arena"
            return
        }

        liveMatchPollingActive = true
        refreshLiveMatchIfNeeded()
        scheduleNextLiveMatchPoll()
    }

    private fun stopLiveMatchPolling() {
        liveMatchPollingActive = false
        liveMatchPollingHandler.removeCallbacks(
            liveMatchPollingRunnable
        )
    }

    private fun scheduleNextLiveMatchPoll() {
        liveMatchPollingHandler.removeCallbacks(
            liveMatchPollingRunnable
        )

        if (liveMatchPollingActive) {
            liveMatchPollingHandler.postDelayed(
                liveMatchPollingRunnable,
                LIVE_MATCH_POLLING_INTERVAL_MS
            )
        }
    }

    private fun refreshLiveMatchIfNeeded() {
        if (standaloneClassicMode) {
            return
        }

        if (liveMatchRequestInFlight) {
            return
        }

        val liveMatchClient =
            arenaLiveMatchClient ?: return

        val requestGeneration =
            liveMatchClientGeneration

        if (arenaAuthClient.currentAccessToken() == null) {
            return
        }

        liveMatchRequestInFlight = true

        liveMatchClient.fetchLiveMatch { result ->
            runOnUiThread {
                if (requestGeneration != liveMatchClientGeneration) {
                    return@runOnUiThread
                }

                liveMatchRequestInFlight = false

                val response =
                    result.response

                if (result.success && response != null) {
                    applyLiveMatchResponse(
                        response
                    )
                } else {
                    arenaLastCallText.text =
                        "Partita live non disponibile"

                    if (BuildConfig.DEBUG) {
                        Log.i(
                            ARENA_LOG_TAG,
                            "Arena live match failed: HTTP ${result.statusCode}"
                        )
                    }
                }
            }
        }
    }

    private fun reloadArenaCourtSelection() {
        if (standaloneClassicMode) {
            configureArenaCourtClients(
                null
            )
            return
        }

        val selection =
            arenaCourtSelectionStore.readSelection()
                ?: fallbackArenaCourtSelection()

        configureArenaCourtClients(
            selection
        )
    }

    private fun reloadArenaOperationMode() {
        standaloneClassicMode =
            arenaOperationModeStore.isStandaloneClassicMode()
    }

    private fun fallbackArenaCourtSelection(): ArenaCourtSelection? {
        val courtId =
            arenaConfig.courtId.trim()

        if (courtId.isBlank()) {
            return null
        }

        return ArenaCourtSelection(
            selectedCourtId = courtId,
            selectedCourtLabel = "Campo Arena PoC",
            selectedCenterName = "Centro",
            selectedTournamentCourtName = null
        )
    }

    private fun configureArenaCourtClients(
        selection: ArenaCourtSelection?
    ) {
        val previousSelectedCourtId =
            arenaCourtSelection?.selectedCourtId

        stopLiveMatchPolling()
        liveMatchClientGeneration += 1
        liveMatchRequestInFlight = false

        arenaApiClient?.shutdown()
        arenaLiveMatchClient?.shutdown()
        arenaLiveScoreAdoptionClient?.shutdown()
        arenaMatchLifecycleClient?.shutdown()

        if (standaloneClassicMode) {
            arenaCourtSelection = null
            arenaApiClient = null
            arenaLiveMatchClient = null
            arenaLiveScoreAdoptionClient = null
            arenaRealScoreSync = null
            arenaMatchLifecycleClient = null
            currentLiveMatchId = null
            lastAdoptedLiveScoreKey = null
            saveCurrentLiveMatchId()
            clearMatchLifecycleState()
            saveMatchLifecycleState()
            updateTeamLabels(
                DEFAULT_TEAM_A_LABEL,
                DEFAULT_TEAM_B_LABEL
            )
            arenaConnectionStatusText.text =
                "Modalità autonoma"
            arenaLastCallText.text =
                "Modalità autonoma"
            return
        }

        arenaCourtSelection =
            selection

        if (selection == null) {
            arenaApiClient = null
            arenaLiveMatchClient = null
            arenaLiveScoreAdoptionClient = null
            arenaRealScoreSync = null
            arenaMatchLifecycleClient = null
            currentLiveMatchId = null
            lastAdoptedLiveScoreKey = null
            saveCurrentLiveMatchId()
            updateTeamLabels(
                DEFAULT_TEAM_A_LABEL,
                DEFAULT_TEAM_B_LABEL
            )
            arenaLastCallText.text =
                "Seleziona campo Arena"
            return
        }

        if (
            previousSelectedCourtId != null &&
            previousSelectedCourtId != selection.selectedCourtId
        ) {
            currentLiveMatchId = null
            lastAdoptedLiveScoreKey = null
            saveCurrentLiveMatchId()
        }

        val selectedConfig =
            arenaConfig.copy(
                courtId = selection.selectedCourtId
            )

        val apiClient =
            ArenaApiClient(
                config = selectedConfig,
                tokenProvider = arenaAuthClient
            )

        val liveMatchClient =
            ArenaLiveMatchClient(
                config = selectedConfig,
                tokenProvider = arenaAuthClient
            )

        val liveScoreAdoptionClient =
            ArenaLiveScoreAdoptionClient(
                config = selectedConfig,
                tokenProvider = arenaAuthClient
            )

        val matchLifecycleClient =
            ArenaMatchLifecycleClient(
                config = selectedConfig,
                tokenProvider = arenaAuthClient
            )

        arenaApiClient =
            apiClient
        arenaLiveMatchClient =
            liveMatchClient
        arenaLiveScoreAdoptionClient =
            liveScoreAdoptionClient
        arenaMatchLifecycleClient =
            matchLifecycleClient
        arenaRealScoreSync =
            ArenaRealScoreSync(
                snapshotFactory =
                    ArenaRealScoreSnapshotFactory(
                        sequenceStore =
                            arenaSequenceStore
                    ),
                sender =
                    ArenaApiScoreSnapshotSender(
                        apiClient
                    ),
                onResult = { snapshot, result ->
                    runOnUiThread {
                        showArenaApiDiagnostic(
                            label = "Arena reale",
                            snapshot = snapshot,
                            result = result
                        )
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        arenaLastCallText.text =
                            ArenaManualUiMessages.apiFailed(
                                error
                            )
                    }
                }
            )

        arenaLastCallText.text =
            "${selection.selectedCenterName} - ${selection.selectedCourtLabel}"

        clearFinishedLifecycleIfCourtChanged(
            selection.selectedCourtId
        )

        if (activityVisible && arenaAuthClient.currentAccessToken() != null) {
            startLiveMatchPolling()
        }
    }

    private fun applyLiveMatchResponse(
        response: ArenaLiveMatchResponse
    ) {
        if (standaloneClassicMode) {
            return
        }

        val match =
            response.match

        if (match == null) {
            updateTeamLabels(
                DEFAULT_TEAM_A_LABEL,
                DEFAULT_TEAM_B_LABEL
            )

            arenaLastCallText.text =
                when (response.reason) {
                    "court_not_mapped" ->
                        "Campo Arena non collegato"

                    else ->
                        "Nessuna partita live"
                }

            return
        }

        arenaLifecycleSequenceStore.advanceToAtLeast(
            match.lastLifecycleEventSequence
        )

        val previousLiveMatchId =
            currentLiveMatchId
        val matchChanged =
            previousLiveMatchId != null &&
                    previousLiveMatchId != match.matchId
        val replacesFinishedMatch =
            matchLifecycleState == MatchLifecycleState.FINISHED_BY_ARENA &&
                    lastFinishedMatchId != null &&
                    lastFinishedMatchId != match.matchId

        if (matchChanged || replacesFinishedMatch) {
            activateNewLiveMatch(
                match.matchId
            )
        }

        val shouldBootstrapScore =
            matchChanged ||
                    replacesFinishedMatch ||
                    isLocalScoreEmptyForLiveMatchBootstrap()

        currentLiveMatchId =
            match.matchId

        updateTeamLabels(
            match.sideA.label,
            match.sideB.label
        )

        if (shouldBootstrapScore) {
            bootstrapLocalScoreFromLiveMatch(
                match = match,
                clearPreviousHistory = matchChanged
            )
            adoptBootstrappedLiveScoreIfNeeded(
                match
            )
            statusText.text =
                "Tabellone riallineato"
        } else {
            saveCurrentLiveMatchId()
        }

        arenaLastCallText.text =
            "Partita live aggiornata"
    }

    private fun activateNewLiveMatch(
        matchId: String
    ) {
        if (
            currentLiveMatchId == matchId &&
            matchLifecycleState == MatchLifecycleState.ACTIVE
        ) {
            return
        }

        clearMatchLifecycleState()
        localMatchFinished = false
        scoreHistory.clear()
    }

    private fun updateTeamLabels(
        sideA: String,
        sideB: String
    ) {
        teamALabel =
            sideA.trim().ifEmpty {
                DEFAULT_TEAM_A_LABEL
            }

        teamBLabel =
            sideB.trim().ifEmpty {
                DEFAULT_TEAM_B_LABEL
            }

        teamAText.text =
            teamALabel

        teamBText.text =
            teamBLabel

        if (::scoreAnnouncer.isInitialized) {
            scoreAnnouncer.updateTeamLabels(
                teamALabel = teamALabel,
                teamBLabel = teamBLabel
            )
        }
    }

    private fun isLocalScoreEmptyForLiveMatchBootstrap(): Boolean {
        return pointsA == 0 &&
                pointsB == 0 &&
                gamesA == 0 &&
                gamesB == 0 &&
                setsA == 0 &&
                setsB == 0 &&
                scoreHistory.isEmpty() &&
                matchLifecycleState == MatchLifecycleState.ACTIVE
    }

    private fun bootstrapLocalScoreFromLiveMatch(
        match: ArenaLiveMatch,
        clearPreviousHistory: Boolean
    ) {
        gamesA =
            match.scoreA.coerceAtLeast(
                0
            )
        gamesB =
            match.scoreB.coerceAtLeast(
                0
            )
        setsA =
            match.setsA.coerceAtLeast(
                0
            )
        setsB =
            match.setsB.coerceAtLeast(
                0
            )
        pointsA = 0
        pointsB = 0
        advantageSide = null
        killerMode = false
        tieBreakActive = false
        tieBreakPointsA = 0
        tieBreakPointsB = 0

        if (clearPreviousHistory) {
            scoreHistory.clear()
        }

        saveState()
        updateScreen()
    }

    private fun adoptBootstrappedLiveScoreIfNeeded(
        match: ArenaLiveMatch
    ) {
        if (standaloneClassicMode) {
            return
        }

        if (matchLifecycleState != MatchLifecycleState.ACTIVE) {
            return
        }

        val adoptionClient =
            arenaLiveScoreAdoptionClient ?: return

        val adoptionKey =
            liveScoreAdoptionKey(
                match
            )

        if (lastAdoptedLiveScoreKey == adoptionKey) {
            return
        }

        val request =
            ArenaLiveScoreAdoptionRequest(
                eventId = UUID.randomUUID().toString(),
                eventSequence = arenaSequenceStore.nextSequence(),
                matchId = match.matchId,
                gamesA = match.scoreA.coerceAtLeast(
                    0
                ),
                gamesB = match.scoreB.coerceAtLeast(
                    0
                ),
                setsA = match.setsA.coerceAtLeast(
                    0
                ),
                setsB = match.setsB.coerceAtLeast(
                    0
                )
            )

        adoptionClient.adoptLiveScore(
            request
        ) { result ->
            runOnUiThread {
                if (isLiveScoreAdoptionSuccess(result.status)) {
                    lastAdoptedLiveScoreKey =
                        adoptionKey
                    saveCurrentLiveMatchId()
                    arenaLastCallText.text =
                        "Baseline Arena aggiornata"
                } else {
                    arenaLastCallText.text =
                        "Baseline Arena non aggiornata"
                }
            }
        }
    }

    private fun liveScoreAdoptionKey(
        match: ArenaLiveMatch
    ): String {
        return "${match.matchId}:${match.scoreA}-${match.scoreB}"
    }

    private fun isLiveScoreAdoptionSuccess(
        status: String?
    ): Boolean {
        return status == "adopted" ||
                status == "already_adopted" ||
                status == "duplicate_event"
    }

    private fun openArenaSettings() {
        arenaSettingsLauncher.launch(
            Intent(
                this,
                ArenaSettingsActivity::class.java
            )
        )
    }

    private fun handleSettingsAssignmentRequest(
        mode: AssignmentMode
    ) {
        reloadArenaCourtSelection()
        bootstrapArenaSession()
        requestShellyAssignment(
            mode
        )
    }

    private fun requestShellyAssignment(
        mode: AssignmentMode
    ) {
        val sideName =
            when (mode) {
                AssignmentMode.SIDE_A -> "A"
                AssignmentMode.SIDE_B -> "B"
            }

        helpText.text =
            "Attendi un secondo, poi premi il pulsante Shelly lato $sideName"

        beginAssignment(
            mode
        )
    }

    private fun showArenaApiDiagnostic(
        label: String,
        snapshot: ArenaScoreSnapshot,
        result: ArenaApiResult
    ) {
        val diagnostic =
            ArenaManualUiMessages.apiDiagnostic(
                label = label,
                snapshot = snapshot,
                result = result
            )

        arenaLastCallText.text =
            ArenaManualUiMessages.apiResult(
                result
            )

        if (BuildConfig.DEBUG) {
            Log.i(
                ARENA_LOG_TAG,
                diagnostic
            )
        }
    }

    private fun loadSavedData() {
        deviceA = preferences.getString(
            PREF_DEVICE_A,
            null
        )

        deviceB = preferences.getString(
            PREF_DEVICE_B,
            null
        )

        pointsA = preferences.getInt(
            PREF_POINTS_A,
            0
        )

        pointsB = preferences.getInt(
            PREF_POINTS_B,
            0
        )

        gamesA = preferences.getInt(
            PREF_GAMES_A,
            0
        )

        gamesB = preferences.getInt(
            PREF_GAMES_B,
            0
        )

        setsA = preferences.getInt(
            PREF_SETS_A,
            0
        )

        setsB = preferences.getInt(
            PREF_SETS_B,
            0
        )

        advantageSide =
            when (
                preferences.getString(
                    PREF_ADVANTAGE_SIDE,
                    null
                )
            ) {
                "A" -> Side.A
                "B" -> Side.B
                else -> null
            }

        killerMode =
            preferences.getBoolean(
                PREF_KILLER_MODE,
                false
            )

        tieBreakActive =
            preferences.getBoolean(
                PREF_TIE_BREAK_ACTIVE,
                false
            )

        tieBreakPointsA =
            preferences.getInt(
                PREF_TIE_BREAK_POINTS_A,
                0
            )

        tieBreakPointsB =
            preferences.getInt(
                PREF_TIE_BREAK_POINTS_B,
                0
            )

        localMatchFinished =
            preferences.getBoolean(
                PREF_LOCAL_MATCH_FINISHED,
                false
            )

        currentLiveMatchId =
            preferences.getString(
                PREF_CURRENT_LIVE_MATCH_ID,
                null
            )
        lastAdoptedLiveScoreKey =
            preferences.getString(
                PREF_LAST_ADOPTED_LIVE_SCORE_KEY,
                null
            )

        loadMatchLifecycleState()
    }

    private fun saveState() {
        preferences.edit()
            .putString(
                PREF_DEVICE_A,
                deviceA
            )
            .putString(
                PREF_DEVICE_B,
                deviceB
            )
            .putInt(
                PREF_POINTS_A,
                pointsA
            )
            .putInt(
                PREF_POINTS_B,
                pointsB
            )
            .putInt(
                PREF_GAMES_A,
                gamesA
            )
            .putInt(
                PREF_GAMES_B,
                gamesB
            )
            .putInt(
                PREF_SETS_A,
                setsA
            )
            .putInt(
                PREF_SETS_B,
                setsB
            )
            .putString(
                PREF_ADVANTAGE_SIDE,
                advantageSide?.name
            )
            .putBoolean(
                PREF_KILLER_MODE,
                killerMode
            )
            .putBoolean(
                PREF_TIE_BREAK_ACTIVE,
                tieBreakActive
            )
            .putInt(
                PREF_TIE_BREAK_POINTS_A,
                tieBreakPointsA
            )
            .putInt(
                PREF_TIE_BREAK_POINTS_B,
                tieBreakPointsB
            )
            .putBoolean(
                PREF_LOCAL_MATCH_FINISHED,
                localMatchFinished
            )
            .putString(
                PREF_CURRENT_LIVE_MATCH_ID,
                currentLiveMatchId
            )
            .putString(
                PREF_LAST_ADOPTED_LIVE_SCORE_KEY,
                lastAdoptedLiveScoreKey
            )
            .apply()

        saveMatchLifecycleState()
    }

    private fun saveCurrentLiveMatchId() {
        preferences.edit()
            .putString(
                PREF_CURRENT_LIVE_MATCH_ID,
                currentLiveMatchId
            )
            .putString(
                PREF_LAST_ADOPTED_LIVE_SCORE_KEY,
                lastAdoptedLiveScoreKey
            )
            .apply()
    }

    private fun loadMatchLifecycleState() {
        matchLifecycleState =
            MatchLifecycleState.fromStoredValue(
                preferences.getString(
                    PREF_MATCH_LIFECYCLE_STATE,
                    null
                )
            )

        lastFinishedCourtId =
            preferences.getString(
                PREF_LAST_FINISHED_COURT_ID,
                null
            )

        lastFinishedMatchId =
            preferences.getString(
                PREF_LAST_FINISHED_MATCH_ID,
                null
            )

        lastFinishEventId =
            preferences.getString(
                PREF_LAST_FINISH_EVENT_ID,
                null
            )

        lastFinishEventSequence =
            preferences.getInt(
                PREF_LAST_FINISH_EVENT_SEQUENCE,
                0
            )

        lastFinishedSnapshot =
            if (
                matchLifecycleState ==
                MatchLifecycleState.FINISHED_BY_ARENA
            ) {
                ScoreSnapshot(
                    pointsA = preferences.getInt(
                        PREF_LAST_FINISHED_POINTS_A,
                        0
                    ),
                    pointsB = preferences.getInt(
                        PREF_LAST_FINISHED_POINTS_B,
                        0
                    ),
                    gamesA = preferences.getInt(
                        PREF_LAST_FINISHED_GAMES_A,
                        0
                    ),
                    gamesB = preferences.getInt(
                        PREF_LAST_FINISHED_GAMES_B,
                        0
                    ),
                    setsA = preferences.getInt(
                        PREF_LAST_FINISHED_SETS_A,
                        0
                    ),
                    setsB = preferences.getInt(
                        PREF_LAST_FINISHED_SETS_B,
                        0
                    ),
                    advantageSide =
                        when (
                            preferences.getString(
                                PREF_LAST_FINISHED_ADVANTAGE_SIDE,
                                null
                            )
                        ) {
                            "A" -> Side.A
                            "B" -> Side.B
                            else -> null
                        },
                    killerMode = preferences.getBoolean(
                        PREF_LAST_FINISHED_KILLER_MODE,
                        false
                    ),
                    tieBreakActive = preferences.getBoolean(
                        PREF_LAST_FINISHED_TIE_BREAK_ACTIVE,
                        false
                    ),
                    tieBreakPointsA = preferences.getInt(
                        PREF_LAST_FINISHED_TIE_BREAK_POINTS_A,
                        0
                    ),
                    tieBreakPointsB = preferences.getInt(
                        PREF_LAST_FINISHED_TIE_BREAK_POINTS_B,
                        0
                    ),
                    localMatchFinished = preferences.getBoolean(
                        PREF_LAST_FINISHED_LOCAL_MATCH_FINISHED,
                        false
                    ),
                    scoringSide = Side.A
                )
            } else {
                null
            }
    }

    private fun saveMatchLifecycleState() {
        preferences.edit()
            .putString(
                PREF_MATCH_LIFECYCLE_STATE,
                matchLifecycleState.name
            )
            .putString(
                PREF_LAST_FINISHED_COURT_ID,
                lastFinishedCourtId
            )
            .putString(
                PREF_LAST_FINISHED_MATCH_ID,
                lastFinishedMatchId
            )
            .putString(
                PREF_LAST_FINISH_EVENT_ID,
                lastFinishEventId
            )
            .putInt(
                PREF_LAST_FINISH_EVENT_SEQUENCE,
                lastFinishEventSequence
            )
            .apply()

        saveLastFinishedSnapshot()
    }

    private fun saveLastFinishedSnapshot() {
        val snapshot =
            lastFinishedSnapshot

        val editor =
            preferences.edit()

        if (snapshot == null) {
            editor
                .remove(PREF_LAST_FINISHED_POINTS_A)
                .remove(PREF_LAST_FINISHED_POINTS_B)
                .remove(PREF_LAST_FINISHED_GAMES_A)
                .remove(PREF_LAST_FINISHED_GAMES_B)
                .remove(PREF_LAST_FINISHED_SETS_A)
                .remove(PREF_LAST_FINISHED_SETS_B)
                .remove(PREF_LAST_FINISHED_ADVANTAGE_SIDE)
                .remove(PREF_LAST_FINISHED_KILLER_MODE)
                .remove(PREF_LAST_FINISHED_TIE_BREAK_ACTIVE)
                .remove(PREF_LAST_FINISHED_TIE_BREAK_POINTS_A)
                .remove(PREF_LAST_FINISHED_TIE_BREAK_POINTS_B)
                .remove(PREF_LAST_FINISHED_LOCAL_MATCH_FINISHED)
                .apply()
            return
        }

        editor
            .putInt(
                PREF_LAST_FINISHED_POINTS_A,
                snapshot.pointsA
            )
            .putInt(
                PREF_LAST_FINISHED_POINTS_B,
                snapshot.pointsB
            )
            .putInt(
                PREF_LAST_FINISHED_GAMES_A,
                snapshot.gamesA
            )
            .putInt(
                PREF_LAST_FINISHED_GAMES_B,
                snapshot.gamesB
            )
            .putInt(
                PREF_LAST_FINISHED_SETS_A,
                snapshot.setsA
            )
            .putInt(
                PREF_LAST_FINISHED_SETS_B,
                snapshot.setsB
            )
            .putString(
                PREF_LAST_FINISHED_ADVANTAGE_SIDE,
                snapshot.advantageSide?.name
            )
            .putBoolean(
                PREF_LAST_FINISHED_KILLER_MODE,
                snapshot.killerMode
            )
            .putBoolean(
                PREF_LAST_FINISHED_TIE_BREAK_ACTIVE,
                snapshot.tieBreakActive
            )
            .putInt(
                PREF_LAST_FINISHED_TIE_BREAK_POINTS_A,
                snapshot.tieBreakPointsA
            )
            .putInt(
                PREF_LAST_FINISHED_TIE_BREAK_POINTS_B,
                snapshot.tieBreakPointsB
            )
            .putBoolean(
                PREF_LAST_FINISHED_LOCAL_MATCH_FINISHED,
                snapshot.localMatchFinished
            )
            .apply()
    }

    private fun updateScreen() {
        val displayedScoreA =
            displayPointForSide(
                Side.A
            )

        val displayedScoreB =
            displayPointForSide(
                Side.B
            )

        scoreAText.text =
            displayedScoreA
        applyScoreTextSizing(
            textView = scoreAText,
            displayedValue = displayedScoreA
        )

        scoreBText.text =
            displayedScoreB
        applyScoreTextSizing(
            textView = scoreBText,
            displayedValue = displayedScoreB
        )

        gamesAText.text =
            gamesA.toString()

        gamesBText.text =
            gamesB.toString()

        setsAText.text =
            setsA.toString()

        setsBText.text =
            setsB.toString()

        deviceAText.text =
            deviceA?.let { identifier ->
                "Associato:\n$identifier"
            } ?: "Non associato"

        deviceBText.text =
            deviceB?.let { identifier ->
                "Associato:\n$identifier"
            } ?: "Non associato"

        phaseText.text =
            when {
                standaloneClassicMode ->
                    when {
                        localMatchFinished ->
                            "Partita conclusa"

                        tieBreakActive ->
                            "TIE-BREAK"

                        else ->
                            "Modalità autonoma"
                    }

                matchLifecycleState ==
                        MatchLifecycleState.FINISHED_BY_ARENA ->
                    "Partita conclusa - doppio tap per correggere"

                matchLifecycleState ==
                        MatchLifecycleState.FINISHING ->
                    "Chiusura partita..."

                matchLifecycleState ==
                        MatchLifecycleState.REOPENING ->
                    "Riapertura partita..."

                killerMode ->
                    "KILLER: il prossimo punto vince il game"

                advantageSide == Side.A ->
                    "Vantaggio lato A"

                advantageSide == Side.B ->
                    "Vantaggio lato B"

                pointsA == 3 &&
                        pointsB == 3 ->
                    "Parità 40-40"

                else ->
                    "Game in corso"
            }
    }

    private fun applyScoreTextSizing(
        textView: TextView,
        displayedValue: String
    ) {
        val isAdvantage =
            displayedValue == "ADV"

        val minTextSize =
            if (isAdvantage) {
                ADV_SCORE_AUTO_MIN_SP
            } else {
                NUMERIC_SCORE_AUTO_MIN_SP
            }

        val maxTextSize =
            if (isAdvantage) {
                ADV_SCORE_AUTO_MAX_SP
            } else {
                NUMERIC_SCORE_AUTO_MAX_SP
            }

        val textSize =
            if (isAdvantage) {
                ADV_SCORE_TEXT_SIZE_SP
            } else {
                NUMERIC_SCORE_TEXT_SIZE_SP
            }

        textView.setAutoSizeTextTypeUniformWithConfiguration(
            minTextSize,
            maxTextSize,
            SCORE_TEXT_AUTOSIZE_STEP_SP,
            TypedValue.COMPLEX_UNIT_SP
        )

        textView.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            textSize.toFloat()
        )
    }

    private fun displayPointForSide(
        side: Side
    ): String {
        if (tieBreakActive) {
            return when (side) {
                Side.A -> tieBreakPointsA
                Side.B -> tieBreakPointsB
            }.toString()
        }

        if (advantageSide == side) {
            return "ADV"
        }

        if (advantageSide != null) {
            return "40"
        }

        val value =
            when (side) {
                Side.A -> pointsA
                Side.B -> pointsB
            }

        return when (value) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            else -> "40"
        }
    }

    private fun saveStateUpdateScreenAndEnqueueArenaSnapshot() {
        saveState()
        updateScreen()
        enqueueArenaSnapshot()
    }

    private fun enqueueArenaSnapshot() {
        if (standaloneClassicMode) {
            arenaLastCallText.text =
                "Modalità autonoma"
            return
        }

        val sync =
            arenaRealScoreSync

        if (sync == null) {
            arenaLastCallText.text =
                "Seleziona campo Arena"
            return
        }

        sync.enqueue(
            ArenaRealScoreState(
                pointsA = displayPointForSide(
                    Side.A
                ),
                pointsB = displayPointForSide(
                    Side.B
                ),
                gamesA = gamesA,
                gamesB = gamesB
            )
        )
    }

    private fun hasAnyShellyAssociation(): Boolean {
        return deviceA != null || deviceB != null
    }

    private fun startBleScanIfDevicesAssigned() {
        if (!hasAnyShellyAssociation()) {
            return
        }

        if (scanning) {
            return
        }

        checkPermissionsAndStart()
    }

    private fun startBleWatchdogIfNeeded() {
        if (!activityVisible) {
            return
        }

        if (!hasAnyShellyAssociation()) {
            return
        }

        if (bleWatchdogActive) {
            return
        }

        bleWatchdogActive = true
        scheduleNextBleWatchdogCheck()
    }

    private fun stopBleWatchdog() {
        bleWatchdogActive = false
        bleWatchdogHandler.removeCallbacks(
            bleWatchdogRunnable
        )
    }

    private fun scheduleNextBleWatchdogCheck() {
        if (!bleWatchdogActive) {
            return
        }

        bleWatchdogHandler.removeCallbacks(
            bleWatchdogRunnable
        )
        bleWatchdogHandler.postDelayed(
            bleWatchdogRunnable,
            BLE_WATCHDOG_INTERVAL_MS
        )
    }

    private fun runBleWatchdogCheck() {
        if (
            !activityVisible ||
            !hasAnyShellyAssociation() ||
            assignmentMode != null ||
            bleScanRestartPending ||
            !isBluetoothReadyForScan() ||
            !hasBleScanPermissions()
        ) {
            return
        }

        if (!scanning) {
            checkPermissionsAndStart()
            return
        }

        val now =
            SystemClock.elapsedRealtime()
        val lastPacketAt =
            lastBlePacketReceivedAt.takeIf { value ->
                value > 0L
            } ?: bleScanStartedAt
        val scanAge =
            now - bleScanStartedAt
        val inactiveFor =
            now - lastPacketAt

        if (
            scanAge >= BLE_MIN_SCAN_AGE_BEFORE_RESTART_MS &&
            inactiveFor >= BLE_INACTIVITY_RESTART_MS
        ) {
            logBleDebug(
                "BLE watchdog detected inactivity"
            )
            restartBleScanSafely(
                reason = "watchdog_inactivity",
                delayMs = BLE_RESTART_DELAY_MS
            )
        }
    }

    private fun restartBleScanSafely(
        reason: String,
        delayMs: Long = BLE_RESTART_DELAY_MS
    ) {
        if (
            bleScanRestartPending ||
            !activityVisible ||
            !hasAnyShellyAssociation() ||
            assignmentMode != null
        ) {
            return
        }

        bleScanRestartPending = true
        logBleDebug(
            "BLE scan restart: $reason"
        )
        statusText.text =
            "Riconnessione Shelly..."

        stopBleScanInternal(
            clearAssignment = false,
            updateUi = false
        )

        bleWatchdogHandler.postDelayed(
            {
                bleScanRestartPending = false

                if (
                    activityVisible &&
                    hasAnyShellyAssociation() &&
                    assignmentMode == null
                ) {
                    checkPermissionsAndStart()
                }
            },
            delayMs
        )
    }

    private fun beginAssignment(
        mode: AssignmentMode
    ) {
        assignmentMode = mode

        /*
         * Impedisce che il nuovo lato venga associato
         * usando una copia ritardata della pressione precedente.
         */
        assignmentArmedAt =
            System.currentTimeMillis() +
                    ASSIGNMENT_ARM_DELAY_MS

        val sideName =
            when (mode) {
                AssignmentMode.SIDE_A -> "A"
                AssignmentMode.SIDE_B -> "B"
            }

        statusText.text =
            "Attendi un secondo, poi premi il pulsante $sideName"

        eventText.text =
            "Modalità associazione lato $sideName"

        if (!scanning) {
            checkPermissionsAndStart()
        }
    }

    private fun checkPermissionsAndStart() {
        val requiredPermissions =
            mutableListOf<String>()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            requiredPermissions.add(
                Manifest.permission.BLUETOOTH_SCAN
            )

            requiredPermissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }

        requiredPermissions.add(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions =
            requiredPermissions.filter { permission ->

                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (missingPermissions.isEmpty()) {
            startBleScan()
        } else {
            permissionLauncher.launch(
                missingPermissions.toTypedArray()
            )
        }
    }

    private fun hasBleScanPermissions(): Boolean {
        val requiredPermissions =
            mutableListOf<String>()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            requiredPermissions.add(
                Manifest.permission.BLUETOOTH_SCAN
            )

            requiredPermissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }

        requiredPermissions.add(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isBluetoothReadyForScan(): Boolean {
        val adapter =
            bluetoothManager.adapter ?: return false

        return try {
            adapter.isEnabled
        } catch (_: SecurityException) {
            false
        }
    }

    private fun registerBluetoothStateReceiver() {
        if (bluetoothReceiverRegistered) {
            return
        }

        val filter =
            IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            registerReceiver(
                bluetoothStateReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                bluetoothStateReceiver,
                filter
            )
        }

        bluetoothReceiverRegistered = true
    }

    private fun unregisterBluetoothStateReceiver() {
        if (!bluetoothReceiverRegistered) {
            return
        }

        unregisterReceiver(
            bluetoothStateReceiver
        )
        bluetoothReceiverRegistered = false
    }

    private fun logBleDebug(
        message: String
    ) {
        if (BuildConfig.DEBUG) {
            Log.i(
                ARENA_LOG_TAG,
                message
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        val bluetoothAdapter =
            bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            statusText.text =
                "Bluetooth non disponibile"
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            statusText.text =
                "Attiva il Bluetooth del telefono"
            scanning = false
            return
        }

        val scanner =
            bluetoothAdapter.bluetoothLeScanner

        if (scanner == null) {
            statusText.text =
                "Scanner BLE non disponibile"
            scanning = false
            return
        }

        val settings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .setReportDelay(0)
                .build()

        try {
            scanner.startScan(
                null,
                settings,
                scanCallback
            )
        } catch (exception: RuntimeException) {
            scanning = false
            statusText.text =
                "Bluetooth non disponibile"
            logBleDebug(
                "BLE scan start failed: " +
                        exception.javaClass.simpleName
            )
            return
        }

        scanning = true
        bleScanStartedAt =
            SystemClock.elapsedRealtime()

        startButton.text =
            "Ferma rilevamento"

        if (assignmentMode == null) {
            statusText.text =
                "Rilevamento Shelly attivo"
        }

        logBleDebug(
            "BLE scan started"
        )
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        stopBleScanInternal(
            clearAssignment = true,
            updateUi = true
        )
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScanInternal(
        clearAssignment: Boolean,
        updateUi: Boolean
    ) {
        try {
            bluetoothManager.adapter
                ?.bluetoothLeScanner
                ?.stopScan(scanCallback)
        } catch (exception: RuntimeException) {
            logBleDebug(
                "BLE scan stop failed: " +
                        exception.javaClass.simpleName
            )
        }

        scanning = false
        bleScanStartedAt = 0L

        if (clearAssignment) {
            assignmentMode = null
        }

        if (updateUi) {
            statusText.text =
                "Scanner fermo"

            startButton.text =
                "Avvia rilevamento Shelly"
        }

        logBleDebug(
            "BLE scan stopped"
        )
    }

    private val scanCallback =
        object : ScanCallback() {

            @SuppressLint("MissingPermission")
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {
                processScanResult(result)
            }

            @SuppressLint("MissingPermission")
            override fun onBatchScanResults(
                results: MutableList<ScanResult>
            ) {
                results.forEach { result ->
                    processScanResult(result)
                }
            }

            override fun onScanFailed(
                errorCode: Int
            ) {
                runOnUiThread {
                    scanning = false
                    bleScanStartedAt = 0L

                    statusText.text =
                        "Riconnessione Shelly..."

                    startButton.text =
                        "Avvia rilevamento Shelly"

                    logBleDebug(
                        "BLE scan failed: $errorCode"
                    )

                    restartBleScanSafely(
                        reason = "scan_failed_$errorCode",
                        delayMs = BLE_SCAN_FAILURE_RETRY_DELAY_MS
                    )
                }
            }
        }

    @SuppressLint("MissingPermission")
    private fun processScanResult(
        result: ScanResult
    ) {
        val scanRecord =
            result.scanRecord ?: return

        val serviceData =
            scanRecord.getServiceData(
                bthomeUuid
            ) ?: return

        if (serviceData.isEmpty()) {
            return
        }

        val deviceInfo =
            serviceData[0].toInt() and 0xFF

        val encrypted =
            deviceInfo and 0x01 != 0

        if (encrypted) {
            runOnUiThread {
                statusText.text =
                    "Shelly rilevato ma dati cifrati"
            }

            return
        }

        /*
         * Usa prima il MAC stabile contenuto nei dati Shelly.
         * Solo se non disponibile usa l'indirizzo visto da Android.
         */
        val stableDeviceId =
            extractShellyMac(scanRecord)
                ?: try {
                    result.device.address
                } catch (_: SecurityException) {
                    return
                }

        lastBlePacketReceivedAt =
            SystemClock.elapsedRealtime()

        val parsedPacket =
            parseShellyPacket(
                serviceData
            )

        val buttonEvent =
            parsedPacket.buttonEvent
                ?: return

        lastBleButtonEventAt =
            SystemClock.elapsedRealtime()

        val currentAssignment =
            assignmentMode

        if (
            currentAssignment != null &&
            System.currentTimeMillis() < assignmentArmedAt
        ) {
            return
        }

        /*
         * Deduplicazione principale tramite packet ID.
         */
        val packetId =
            parsedPacket.packetId

        if (packetId != null) {
            val previousPacketId =
                lastPacketIdByDevice[
                    stableDeviceId
                ]

            if (previousPacketId == packetId) {
                return
            }

            lastPacketIdByDevice[
                stableDeviceId
            ] = packetId
        } else {
            /*
             * Protezione di riserva per eventuali pacchetti
             * senza packet ID.
             */
            val now =
                System.currentTimeMillis()

            val packetHex =
                serviceData.toHexString()

            val previous =
                lastFallbackEventByDevice[
                    stableDeviceId
                ]

            if (
                previous != null &&
                previous.packetHex == packetHex &&
                now - previous.timestamp <
                FALLBACK_DUPLICATE_WINDOW_MS
            ) {
                return
            }

            lastFallbackEventByDevice[
                stableDeviceId
            ] = FallbackEvent(
                packetHex = packetHex,
                timestamp = now
            )
        }

        runOnUiThread {
            handleButtonEvent(
                stableDeviceId,
                buttonEvent
            )
        }
    }

    /*
     * Estrae il MAC stabile inserito da Shelly
     * nei manufacturer data con ID 0x0BA9.
     */
    private fun extractShellyMac(
        scanRecord: ScanRecord
    ): String? {
        val manufacturerData =
            scanRecord.getManufacturerSpecificData(
                SHELLY_MANUFACTURER_ID
            ) ?: return null

        var index = 0

        while (index < manufacturerData.size) {
            val blockType =
                manufacturerData[index]
                    .toInt() and 0xFF

            index += 1

            when (blockType) {
                /*
                 * Flags Shelly: 2 byte.
                 */
                0x01 -> {
                    if (
                        index + 2 >
                        manufacturerData.size
                    ) {
                        return null
                    }

                    index += 2
                }

                /*
                 * MAC Shelly: 6 byte.
                 */
                0x0A -> {
                    if (
                        index + 6 >
                        manufacturerData.size
                    ) {
                        return null
                    }

                    val macBytes =
                        manufacturerData.copyOfRange(
                            index,
                            index + 6
                        )

                    /*
                     * Lo rendiamo una stringa stabile.
                     * Anche se l'ordine fosse invertito,
                     * l'identificativo rimane univoco e coerente.
                     */
                    return macBytes
                        .joinToString(":") { byte ->
                            "%02X".format(
                                byte.toInt() and 0xFF
                            )
                        }
                }

                /*
                 * Identificativo modello Shelly: 2 byte.
                 */
                0x0B -> {
                    if (
                        index + 2 >
                        manufacturerData.size
                    ) {
                        return null
                    }

                    index += 2
                }

                else -> {
                    /*
                     * Blocco non conosciuto:
                     * non possiamo determinarne la lunghezza.
                     */
                    return null
                }
            }
        }

        return null
    }

    /*
     * Parser mirato per Shelly BLU Button.
     *
     * Oggetti previsti:
     * 0x00 = packet ID, 1 byte
     * 0x01 = batteria, 1 byte
     * 0x3A = evento pulsante, 1 byte
     */
    private fun parseShellyPacket(
        serviceData: ByteArray
    ): ParsedShellyPacket {
        var index = 1

        var packetId: Int? = null
        var buttonEvent: ButtonEvent? = null

        while (index < serviceData.size) {
            val objectId =
                serviceData[index]
                    .toInt() and 0xFF

            when (objectId) {
                PACKET_ID_OBJECT_ID -> {
                    if (
                        index + 1 >=
                        serviceData.size
                    ) {
                        break
                    }

                    packetId =
                        serviceData[index + 1]
                            .toInt() and 0xFF

                    index += 2
                }

                BATTERY_OBJECT_ID -> {
                    if (
                        index + 1 >=
                        serviceData.size
                    ) {
                        break
                    }

                    index += 2
                }

                BUTTON_OBJECT_ID -> {
                    if (
                        index + 1 >=
                        serviceData.size
                    ) {
                        break
                    }

                    val eventCode =
                        serviceData[index + 1]
                            .toInt() and 0xFF

                    buttonEvent =
                        ButtonEvent.fromCode(
                            eventCode
                        )

                    index += 2
                }

                /*
                 * Tipo dispositivo Shelly, uint16.
                 */
                0xF0 -> {
                    if (
                        index + 2 >=
                        serviceData.size
                    ) {
                        break
                    }

                    index += 3
                }

                /*
                 * Versione firmware, uint32.
                 */
                0xF1 -> {
                    if (
                        index + 4 >=
                        serviceData.size
                    ) {
                        break
                    }

                    index += 5
                }

                /*
                 * Versione firmware alternativa, uint24.
                 */
                0xF2 -> {
                    if (
                        index + 3 >=
                        serviceData.size
                    ) {
                        break
                    }

                    index += 4
                }

                else -> {
                    /*
                     * Oggetto sconosciuto:
                     * interrompiamo per evitare falsi eventi.
                     */
                    break
                }
            }
        }

        return ParsedShellyPacket(
            packetId = packetId,
            buttonEvent = buttonEvent
        )
    }

    private fun handleButtonEvent(
        deviceIdentifier: String,
        buttonEvent: ButtonEvent
    ) {
        val currentAssignment =
            assignmentMode

        if (currentAssignment != null) {
            /*
             * Ignora eventuali pacchetti arrivati immediatamente
             * dopo l'attivazione della modalità associazione.
             */
            if (
                System.currentTimeMillis() <
                assignmentArmedAt
            ) {
                return
            }

            assignDevice(
                currentAssignment,
                deviceIdentifier
            )

            return
        }

        val side =
            when (deviceIdentifier) {
                deviceA -> Side.A
                deviceB -> Side.B
                else -> null
            }

        if (side == null) {
            statusText.text =
                "Shelly non associato"

            eventText.text =
                "Identificativo: $deviceIdentifier"

            return
        }

        when (buttonEvent) {
            ButtonEvent.SINGLE_PRESS ->
                registerPoint(side)

            ButtonEvent.DOUBLE_PRESS ->
                handleDoublePress()

            ButtonEvent.TRIPLE_PRESS ->
                correctGameForSide(side)

            ButtonEvent.LONG_PRESS,
            ButtonEvent.HOLD_PRESS ->
                handleLongPress()
        }
    }

    private fun correctGameForSide(
        side: Side
    ) {
        if (standaloneClassicMode) {
            if (localMatchFinished) {
                statusText.text =
                    "Partita conclusa"

                eventText.text =
                    "Correzione non disponibile"
                return
            }
        } else if (
            matchLifecycleState !=
            MatchLifecycleState.ACTIVE
        ) {
            statusText.text =
                "Operazione in corso"

            eventText.text =
                if (
                    matchLifecycleState ==
                    MatchLifecycleState.FINISHED_BY_ARENA
                ) {
                    "Doppio tap per correggere"
                } else {
                    "Attendi il completamento"
                }
            return
        }

        val currentGames =
            when (side) {
                Side.A -> gamesA
                Side.B -> gamesB
            }

        if (currentGames <= 0) {
            statusText.text =
                "Nessun game da correggere"

            eventText.text =
                "Tripla pressione"
            return
        }

        saveSnapshot(
            side
        )

        when (side) {
            Side.A ->
                gamesA =
                    (gamesA - 1).coerceAtLeast(
                        0
                    )

            Side.B ->
                gamesB =
                    (gamesB - 1).coerceAtLeast(
                        0
                    )
        }

        statusText.text =
            "Game ${genericTeamLabelForSide(side)} corretto"

        eventText.text =
            "Tripla pressione"

        if (standaloneClassicMode) {
            saveState()
            updateScreen()
        } else {
            saveStateUpdateScreenAndEnqueueArenaSnapshot()
        }

        animateGameCorrectionChange(
            side
        )
        announceGameCorrection(
            side
        )
    }

    private fun handleDoublePress() {
        if (standaloneClassicMode) {
            undoLastAction()
            return
        }

        when (matchLifecycleState) {
            MatchLifecycleState.ACTIVE ->
                undoLastAction()

            MatchLifecycleState.FINISHED_BY_ARENA ->
                reopenFinishedMatch()

            MatchLifecycleState.FINISHING,
            MatchLifecycleState.REOPENING -> {
                statusText.text =
                    "Operazione in corso"

                eventText.text =
                    "Attendi il completamento"
            }
        }
    }

    private fun handleLongPress() {
        if (standaloneClassicMode) {
            statusText.text =
                "Pressione lunga rilevata"

            eventText.text =
                "Nessuna azione configurata"
            return
        }

        finishCurrentMatch()
    }

    private fun assignDevice(
        mode: AssignmentMode,
        deviceIdentifier: String
    ) {
        when (mode) {
            AssignmentMode.SIDE_A -> {
                /*
                 * Lo stesso pulsante non può appartenere
                 * contemporaneamente ai due lati.
                 */
                if (
                    deviceIdentifier ==
                    deviceB
                ) {
                    statusText.text =
                        "Questo pulsante è già associato al lato B"

                    eventText.text =
                        "Usa un pulsante diverso"

                    assignmentMode = null
                    return
                }

                deviceA =
                    deviceIdentifier

                statusText.text =
                    "Pulsante Lato A associato"
            }

            AssignmentMode.SIDE_B -> {
                if (
                    deviceIdentifier ==
                    deviceA
                ) {
                    statusText.text =
                        "Questo pulsante è già associato al lato A"

                    eventText.text =
                        "Usa un pulsante diverso"

                    assignmentMode = null
                    return
                }

                deviceB =
                    deviceIdentifier

                statusText.text =
                    "Pulsante Lato B associato"
            }
        }

        assignmentMode = null

        eventText.text =
            "Dispositivo: $deviceIdentifier"

        helpText.text =
            "Tap: +1 punto    Doppio tap: annulla"

        saveState()
        updateScreen()

        Toast.makeText(
            this,
            "Associazione completata",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun animatePointChange(
        side: Side
    ) {
        animateScoreChange(
            side = side,
            flashColor = pointFlashColor
        )
    }

    private fun animateUndoChange(
        side: Side
    ) {
        animateScoreChange(
            side = side,
            flashColor = undoFlashColor
        )
    }

    private fun animateGameCorrectionChange(
        side: Side
    ) {
        val target =
            when (side) {
                Side.A -> gamesAText
                Side.B -> gamesBText
            }
        val previousAnimation =
            when (side) {
                Side.A -> gameAnimationA
                Side.B -> gameAnimationB
            }
        val standardColor =
            Color.rgb(
                183,
                242,
                75
            )

        previousAnimation?.cancel()
        target.setTextColor(
            standardColor
        )

        val animation =
            AnimatorSet().apply {
                playSequentially(
                    ObjectAnimator.ofArgb(
                        target,
                        "textColor",
                        standardColor,
                        undoFlashColor
                    ).apply {
                        duration = SCORE_FLASH_RISE_MS
                    },
                    ObjectAnimator.ofArgb(
                        target,
                        "textColor",
                        undoFlashColor,
                        standardColor
                    ).apply {
                        duration = SCORE_FLASH_FALL_MS
                    }
                )
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(
                            animation: Animator
                        ) {
                            target.setTextColor(
                                standardColor
                            )
                        }

                        override fun onAnimationCancel(
                            animation: Animator
                        ) {
                            target.setTextColor(
                                standardColor
                            )
                        }
                    }
                )
            }

        when (side) {
            Side.A -> gameAnimationA = animation
            Side.B -> gameAnimationB = animation
        }

        animation.start()
    }

    private fun animateScoreChange(
        side: Side,
        flashColor: Int
    ) {
        val target =
            when (side) {
                Side.A -> scoreAText
                Side.B -> scoreBText
            }

        val previousAnimation =
            when (side) {
                Side.A -> scoreAnimationA
                Side.B -> scoreAnimationB
            }

        previousAnimation?.cancel()
        target.setTextColor(
            Color.WHITE
        )

        val pulseAnimations =
            mutableListOf<Animator>()

        repeat(SCORE_FLASH_PULSE_COUNT) {
            pulseAnimations.add(
                ObjectAnimator.ofArgb(
                    target,
                    "textColor",
                    Color.WHITE,
                    flashColor
                ).apply {
                    duration = SCORE_FLASH_RISE_MS
                }
            )
            pulseAnimations.add(
                ObjectAnimator.ofArgb(
                    target,
                    "textColor",
                    flashColor,
                    flashColor
                ).apply {
                    duration = SCORE_FLASH_HOLD_MS
                }
            )
            pulseAnimations.add(
                ObjectAnimator.ofArgb(
                    target,
                    "textColor",
                    flashColor,
                    Color.WHITE
                ).apply {
                    duration = SCORE_FLASH_FALL_MS
                }
            )
            pulseAnimations.add(
                ObjectAnimator.ofArgb(
                    target,
                    "textColor",
                    Color.WHITE,
                    Color.WHITE
                ).apply {
                    duration = SCORE_FLASH_PAUSE_MS
                }
            )
        }

        val animation =
            AnimatorSet().apply {
                playSequentially(
                    pulseAnimations
                )
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(
                            animation: Animator
                        ) {
                            target.setTextColor(
                                Color.WHITE
                            )
                        }

                        override fun onAnimationCancel(
                            animation: Animator
                        ) {
                            target.setTextColor(
                                Color.WHITE
                            )
                        }
                    }
                )
            }

        when (side) {
            Side.A -> scoreAnimationA = animation
            Side.B -> scoreAnimationB = animation
        }

        animation.start()
    }

    private fun registerPoint(
        scoringSide: Side
    ) {
        if (standaloneClassicMode) {
            registerStandaloneClassicPoint(
                scoringSide
            )
            return
        }

        if (
            matchLifecycleState !=
            MatchLifecycleState.ACTIVE
        ) {
            statusText.text =
                "Operazione in corso"

            eventText.text =
                if (
                    matchLifecycleState ==
                    MatchLifecycleState.FINISHED_BY_ARENA
                ) {
                    "Doppio tap per correggere"
                } else {
                    "Attendi il completamento"
                }
            return
        }

        saveSnapshot(
            scoringSide
        )

        val previousGamesA =
            gamesA

        val previousGamesB =
            gamesB

        /*
         * KILLER:
         * il punto assegna direttamente il game.
         */
        if (killerMode) {
            winGame(scoringSide)

            statusText.text =
                "GAME LATO ${scoringSide.name}"

            eventText.text =
                "Punto killer"

            saveStateUpdateScreenAndEnqueueArenaSnapshot()
            animatePointChange(
                scoringSide
            )
            announceValidPoint(
                scoringSide = scoringSide,
                previousGamesA = previousGamesA,
                previousGamesB = previousGamesB
            )
            return
        }

        /*
         * Gestione vantaggio.
         */
        val currentAdvantage =
            advantageSide

        if (currentAdvantage != null) {
            if (
                scoringSide ==
                currentAdvantage
            ) {
                winGame(scoringSide)

                statusText.text =
                    "GAME LATO ${scoringSide.name}"

                eventText.text =
                    "Game vinto dopo vantaggio"
            } else {
                advantageSide = null
                killerMode = true

                pointsA = 3
                pointsB = 3

                statusText.text =
                    "PARITÀ: KILLER ATTIVO"

                eventText.text =
                    "Il prossimo punto vince il game"
            }

            saveStateUpdateScreenAndEnqueueArenaSnapshot()
            animatePointChange(
                scoringSide
            )
            announceValidPoint(
                scoringSide = scoringSide,
                previousGamesA = previousGamesA,
                previousGamesB = previousGamesB
            )
            return
        }

        val scorerPoints =
            when (scoringSide) {
                Side.A -> pointsA
                Side.B -> pointsB
            }

        val opponentPoints =
            when (scoringSide) {
                Side.A -> pointsB
                Side.B -> pointsA
            }

        /*
         * Sul 40-40 il punto assegna ADV.
         */
        if (
            scorerPoints == 3 &&
            opponentPoints == 3
        ) {
            advantageSide =
                scoringSide

            statusText.text =
                "VANTAGGIO LATO ${scoringSide.name}"

            eventText.text =
                "Punto di vantaggio"

            saveStateUpdateScreenAndEnqueueArenaSnapshot()
            animatePointChange(
                scoringSide
            )
            announceValidPoint(
                scoringSide = scoringSide,
                previousGamesA = previousGamesA,
                previousGamesB = previousGamesB
            )
            return
        }

        /*
         * 40 contro punteggio inferiore:
         * il lato vince il game.
         */
        if (
            scorerPoints == 3 &&
            opponentPoints < 3
        ) {
            winGame(scoringSide)

            statusText.text =
                "GAME LATO ${scoringSide.name}"

            eventText.text =
                "Game vinto"

            saveStateUpdateScreenAndEnqueueArenaSnapshot()
            animatePointChange(
                scoringSide
            )
            announceValidPoint(
                scoringSide = scoringSide,
                previousGamesA = previousGamesA,
                previousGamesB = previousGamesB
            )
            return
        }

        /*
         * Incremento normale:
         * 0 → 15 → 30 → 40.
         */
        when (scoringSide) {
            Side.A ->
                pointsA += 1

            Side.B ->
                pointsB += 1
        }

        statusText.text =
            "PUNTO LATO ${scoringSide.name}"

        eventText.text =
            "Pressione singola"

        saveStateUpdateScreenAndEnqueueArenaSnapshot()
        animatePointChange(
            scoringSide
        )
        announceValidPoint(
            scoringSide = scoringSide,
            previousGamesA = previousGamesA,
            previousGamesB = previousGamesB
        )
    }

    private fun announceValidPoint(
        scoringSide: Side,
        previousGamesA: Int,
        previousGamesB: Int
    ) {
        scoreAnnouncer.announcePoint(
            state = currentSpeechState(),
            scoringSide = scoringSide,
            previousGamesA = previousGamesA,
            previousGamesB = previousGamesB
        )
    }

    private fun announceValidUndo(
        previousGamesA: Int,
        previousGamesB: Int
    ) {
        scoreAnnouncer.announceUndo(
            state = currentSpeechState(),
            previousGamesA = previousGamesA,
            previousGamesB = previousGamesB
        )
    }

    private fun announceGameCorrection(
        side: Side
    ) {
        scoreAnnouncer.announceGameCorrection(
            state = currentSpeechState(),
            correctedSide = side
        )
    }

    private fun currentSpeechState(): ArenaScoreSpeechState {
        return ArenaScoreSpeechState(
            pointsA = if (tieBreakActive) tieBreakPointsA else pointsA,
            pointsB = if (tieBreakActive) tieBreakPointsB else pointsB,
            gamesA = gamesA,
            gamesB = gamesB,
            advantageSide = advantageSide,
            tieBreakActive = tieBreakActive
        )
    }

    private fun registerStandaloneClassicPoint(
        scoringSide: Side
    ) {
        if (localMatchFinished) {
            statusText.text =
                "Partita conclusa"

            eventText.text =
                "Match locale concluso"

            return
        }

        saveSnapshot(
            scoringSide
        )

        val previousGamesA =
            gamesA

        val previousGamesB =
            gamesB

        val result =
            ArenaClassicScoring.registerPoint(
                state = currentClassicScoreState(),
                scoringSide = scoringSide
            )

        applyClassicScoreState(
            result.state
        )

        statusText.text =
            when {
                result.matchWinner != null ->
                    "Partita conclusa"

                result.setWinner != null ->
                    "SET LATO ${result.setWinner.name}"

                gamesA != previousGamesA ||
                        gamesB != previousGamesB ->
                    "GAME LATO ${scoringSide.name}"

                tieBreakActive ->
                    "TIE-BREAK"

                else ->
                    "PUNTO LATO ${scoringSide.name}"
            }

        eventText.text =
            "Modalità autonoma"

        saveState()
        updateScreen()
        animatePointChange(
            scoringSide
        )

        when {
            result.matchWinner != null ->
                scoreAnnouncer.announceMessage(
                    "Set ${teamLabelForSide(result.matchWinner)}. Partita conclusa"
                )

            result.setWinner != null ->
                scoreAnnouncer.announceMessage(
                    "Set ${teamLabelForSide(result.setWinner)}"
                )

            else ->
                announceValidPoint(
                    scoringSide = scoringSide,
                    previousGamesA = previousGamesA,
                    previousGamesB = previousGamesB
                )
        }
    }

    private fun teamLabelForSide(
        side: Side
    ): String {
        return when (side) {
            Side.A -> teamALabel
            Side.B -> teamBLabel
        }.ifBlank {
            when (side) {
                Side.A -> DEFAULT_TEAM_A_LABEL
                Side.B -> DEFAULT_TEAM_B_LABEL
            }
        }
    }

    private fun genericTeamLabelForSide(
        side: Side
    ): String {
        return when (side) {
            Side.A -> DEFAULT_TEAM_A_LABEL
            Side.B -> DEFAULT_TEAM_B_LABEL
        }
    }

    private fun currentClassicScoreState(): ArenaClassicScoreState {
        return ArenaClassicScoreState(
            pointsA = pointsA,
            pointsB = pointsB,
            gamesA = gamesA,
            gamesB = gamesB,
            setsA = setsA,
            setsB = setsB,
            advantageSide = advantageSide,
            killerMode = killerMode,
            tieBreakActive = tieBreakActive,
            tieBreakPointsA = tieBreakPointsA,
            tieBreakPointsB = tieBreakPointsB,
            localMatchFinished = localMatchFinished
        )
    }

    private fun applyClassicScoreState(
        state: ArenaClassicScoreState
    ) {
        pointsA = state.pointsA
        pointsB = state.pointsB
        gamesA = state.gamesA
        gamesB = state.gamesB
        setsA = state.setsA
        setsB = state.setsB
        advantageSide = state.advantageSide
        killerMode = state.killerMode
        tieBreakActive = state.tieBreakActive
        tieBreakPointsA = state.tieBreakPointsA
        tieBreakPointsB = state.tieBreakPointsB
        localMatchFinished = state.localMatchFinished
    }

    private fun winGame(
        winningSide: Side
    ) {
        when (winningSide) {
            Side.A ->
                gamesA += 1

            Side.B ->
                gamesB += 1
        }

        pointsA = 0
        pointsB = 0

        advantageSide = null
        killerMode = false
    }

    private fun saveSnapshot(
        scoringSide: Side
    ) {
        scoreHistory.add(
            ScoreSnapshot(
                pointsA = pointsA,
                pointsB = pointsB,
                gamesA = gamesA,
                gamesB = gamesB,
                setsA = setsA,
                setsB = setsB,
                advantageSide = advantageSide,
                killerMode = killerMode,
                tieBreakActive = tieBreakActive,
                tieBreakPointsA = tieBreakPointsA,
                tieBreakPointsB = tieBreakPointsB,
                localMatchFinished = localMatchFinished,
                scoringSide = scoringSide
            )
        )
    }

    private fun undoLastAction() {
        val snapshot =
            scoreHistory.removeLastOrNull()

        if (snapshot == null) {
            statusText.text =
                "Nessuna azione da annullare"

            eventText.text =
                "Doppia pressione"

            return
        }

        val previousGamesA =
            gamesA

        val previousGamesB =
            gamesB

        pointsA =
            snapshot.pointsA

        pointsB =
            snapshot.pointsB

        gamesA =
            snapshot.gamesA

        gamesB =
            snapshot.gamesB

        setsA =
            snapshot.setsA

        setsB =
            snapshot.setsB

        advantageSide =
            snapshot.advantageSide

        killerMode =
            snapshot.killerMode

        tieBreakActive =
            snapshot.tieBreakActive

        tieBreakPointsA =
            snapshot.tieBreakPointsA

        tieBreakPointsB =
            snapshot.tieBreakPointsB

        localMatchFinished =
            snapshot.localMatchFinished

        statusText.text =
            "ULTIMA AZIONE ANNULLATA"

        eventText.text =
            "Doppia pressione"

        if (standaloneClassicMode) {
            saveState()
            updateScreen()
        } else {
            saveStateUpdateScreenAndEnqueueArenaSnapshot()
        }
        animateUndoChange(
            snapshot.scoringSide
        )
        announceValidUndo(
            previousGamesA = previousGamesA,
            previousGamesB = previousGamesB
        )
    }

    private fun finishCurrentMatch() {
        if (
            matchLifecycleState ==
            MatchLifecycleState.FINISHING ||
            matchLifecycleState ==
            MatchLifecycleState.REOPENING
        ) {
            statusText.text =
                "Operazione in corso"

            eventText.text =
                "Attendi il completamento"
            return
        }

        if (
            matchLifecycleState ==
            MatchLifecycleState.FINISHED_BY_ARENA
        ) {
            statusText.text =
                "Partita conclusa"

            eventText.text =
                "Doppio tap per correggere"
            return
        }

        val lifecycleClient =
            arenaMatchLifecycleClient

        if (lifecycleClient == null) {
            statusText.text =
                "Chiusura partita fallita"

            eventText.text =
                "Seleziona campo Arena"
            return
        }

        val selection =
            arenaCourtSelection

        if (selection == null) {
            statusText.text =
                "Chiusura partita fallita"

            eventText.text =
                "Seleziona campo Arena"
            return
        }

        val matchId =
            currentLiveMatchId

        if (matchId.isNullOrBlank()) {
            statusText.text =
                "Chiusura partita fallita"

            eventText.text =
                "Nessuna partita live"
            return
        }

        if (!isCurrentScoreValidForLifecycle()) {
            statusText.text =
                "Chiusura partita fallita"

            eventText.text =
                "Punteggio non valido"
            return
        }

        val finishedSnapshot =
            captureCurrentScoreSnapshot()
        val finishEventId =
            UUID.randomUUID().toString()
        val finishEventSequence =
            arenaLifecycleSequenceStore.nextSequence()
        val payload =
            ArenaMatchLifecycleScoreSnapshot(
                eventId = finishEventId,
                eventSequence = finishEventSequence,
                gamesA = gamesA,
                gamesB = gamesB,
                setsA = setsA,
                setsB = setsB
            )

        matchLifecycleState =
            MatchLifecycleState.FINISHING
        statusText.text =
            "Chiusura partita..."
        eventText.text =
            "Arena"
        saveMatchLifecycleState()
        updateScreen()

        lifecycleClient.finishMatch(
            payload
        ) { result ->
            runOnUiThread {
                logArenaLifecycleResult(
                    operation = "finish",
                    result = result,
                    eventId = payload.eventId,
                    eventSequence = payload.eventSequence,
                    matchId = matchId,
                    gamesA = payload.gamesA,
                    gamesB = payload.gamesB,
                    setsA = payload.setsA,
                    setsB = payload.setsB
                )

                if (isFinishSuccess(result.status)) {
                    val resolvedMatchId =
                        result.match?.matchId
                            ?.takeIf { value ->
                                value.isNotBlank()
                            }
                            ?: matchId

                    lastFinishedCourtId =
                        selection.selectedCourtId
                    lastFinishedMatchId =
                        resolvedMatchId
                    lastFinishedSnapshot =
                        finishedSnapshot
                    lastFinishEventId =
                        finishEventId
                    lastFinishEventSequence =
                        finishEventSequence
                    lastClosedMatchStore.save(
                        ArenaLastClosedMatch(
                            matchId = resolvedMatchId,
                            courtId = selection.selectedCourtId,
                            teamLabelA = teamALabel,
                            teamLabelB = teamBLabel,
                            pointsA = finishedSnapshot.pointsA,
                            pointsB = finishedSnapshot.pointsB,
                            gamesA = finishedSnapshot.gamesA,
                            gamesB = finishedSnapshot.gamesB,
                            setsA = finishedSnapshot.setsA,
                            setsB = finishedSnapshot.setsB,
                            advantageSide =
                                finishedSnapshot.advantageSide?.name,
                            killerMode = finishedSnapshot.killerMode,
                            tieBreakActive =
                                finishedSnapshot.tieBreakActive,
                            tieBreakPointsA =
                                finishedSnapshot.tieBreakPointsA,
                            tieBreakPointsB =
                                finishedSnapshot.tieBreakPointsB,
                            finishEventId = finishEventId,
                            finishEventSequence = finishEventSequence,
                            finishedAt = Instant.now().toString(),
                            reopenAvailable = true
                        )
                    )
                    matchLifecycleState =
                        MatchLifecycleState.FINISHED_BY_ARENA

                    resetLocalScoreSilently()
                    saveState()
                    updateScreen()

                    statusText.text =
                        "Partita conclusa"
                    eventText.text =
                        "Doppio tap per correggere"
                    scoreAnnouncer.announceMessage(
                        "Partita conclusa"
                    )
                    if (reopenedClosedMatchForCorrection) {
                        reopenedClosedMatchForCorrection = false
                        if (activityVisible) {
                            startLiveMatchPolling()
                        }
                    }
                    return@runOnUiThread
                }

                matchLifecycleState =
                    MatchLifecycleState.ACTIVE
                saveMatchLifecycleState()
                updateScreen()
                statusText.text =
                    "Chiusura partita fallita"
                eventText.text =
                    "Punteggio mantenuto"
            }
        }
    }

    private fun reopenFinishedMatch() {
        val lifecycleClient =
            arenaMatchLifecycleClient

        val matchId =
            lastFinishedMatchId

        val finishedSnapshot =
            lastFinishedSnapshot

        val selection =
            arenaCourtSelection

        if (
            lifecycleClient == null ||
            selection == null ||
            matchId.isNullOrBlank() ||
            finishedSnapshot == null ||
            selection.selectedCourtId != lastFinishedCourtId
        ) {
            statusText.text =
                "Riapertura partita fallita"

            eventText.text =
                "Dati partita non disponibili"
            return
        }

        val reopenEventId =
            UUID.randomUUID().toString()
        val reopenEventSequence =
            arenaLifecycleSequenceStore.nextSequence()

        matchLifecycleState =
            MatchLifecycleState.REOPENING
        statusText.text =
            "Riapertura partita..."
        eventText.text =
            "Arena"
        saveMatchLifecycleState()
        updateScreen()

        val reopenRequest =
            ArenaMatchReopenRequest(
                eventId = reopenEventId,
                eventSequence = reopenEventSequence,
                matchId = matchId
            )

        lifecycleClient.reopenMatch(
            reopenRequest
        ) { result ->
            runOnUiThread {
                logArenaLifecycleResult(
                    operation = "reopen",
                    result = result,
                    eventId = reopenRequest.eventId,
                    eventSequence = reopenRequest.eventSequence,
                    matchId = reopenRequest.matchId,
                    gamesA = finishedSnapshot.gamesA,
                    gamesB = finishedSnapshot.gamesB,
                    setsA = finishedSnapshot.setsA,
                    setsB = finishedSnapshot.setsB
                )

                if (isReopenSuccess(result.status)) {
                    restoreScoreSnapshot(
                        finishedSnapshot
                    )
                    scoreHistory.clear()
                    clearMatchLifecycleState()
                    saveState()
                    updateScreen()

                    statusText.text =
                        "Partita riaperta"
                    eventText.text =
                        "Undo riparte da questo stato"
                    scoreAnnouncer.announceMessage(
                        "Partita riaperta"
                    )
                    return@runOnUiThread
                }

                matchLifecycleState =
                    MatchLifecycleState.FINISHED_BY_ARENA
                saveMatchLifecycleState()
                updateScreen()
                statusText.text =
                    "Riapertura partita fallita"
                eventText.text =
                    "Partita conclusa"
            }
        }
    }

    private fun reopenLastClosedMatchForCorrection() {
        val lifecycleClient =
            arenaMatchLifecycleClient

        val selection =
            arenaCourtSelection

        val lastClosedMatch =
            lastClosedMatchStore.read()

        if (
            standaloneClassicMode ||
            lifecycleClient == null ||
            selection == null ||
            lastClosedMatch == null ||
            selection.selectedCourtId != lastClosedMatch.courtId
        ) {
            statusText.text =
                "Riapertura partita fallita"

            eventText.text =
                "Ultimo match non disponibile"
            return
        }

        if (
            matchLifecycleState == MatchLifecycleState.FINISHING ||
            matchLifecycleState == MatchLifecycleState.REOPENING
        ) {
            statusText.text =
                "Operazione in corso"

            eventText.text =
                "Attendi il completamento"
            return
        }

        val suspendedCurrentMatch =
            captureSuspendedCurrentMatch()
        val reopenEventId =
            UUID.randomUUID().toString()
        val reopenEventSequence =
            arenaLifecycleSequenceStore.nextSequence()
        val reopenRequest =
            ArenaMatchReopenRequest(
                eventId = reopenEventId,
                eventSequence = reopenEventSequence,
                matchId = lastClosedMatch.matchId
            )

        stopLiveMatchPolling()
        matchLifecycleState =
            MatchLifecycleState.REOPENING
        statusText.text =
            "Riapertura partita..."
        eventText.text =
            "Correzione ultimo match"
        saveMatchLifecycleState()
        updateScreen()

        lifecycleClient.reopenMatch(
            reopenRequest
        ) { result ->
            runOnUiThread {
                logArenaLifecycleResult(
                    operation = "reopen-last-closed",
                    result = result,
                    eventId = reopenRequest.eventId,
                    eventSequence = reopenRequest.eventSequence,
                    matchId = reopenRequest.matchId,
                    gamesA = lastClosedMatch.gamesA,
                    gamesB = lastClosedMatch.gamesB,
                    setsA = lastClosedMatch.setsA,
                    setsB = lastClosedMatch.setsB
                )

                if (isReopenSuccess(result.status)) {
                    currentLiveMatchId =
                        lastClosedMatch.matchId
                    updateTeamLabels(
                        lastClosedMatch.teamLabelA,
                        lastClosedMatch.teamLabelB
                    )
                    restoreScoreSnapshot(
                        lastClosedMatch.toScoreSnapshot()
                    )
                    scoreHistory.clear()
                    clearMatchLifecycleState()
                    lastClosedMatchStore.markReopenUnavailable()
                    reopenedClosedMatchForCorrection = true
                    saveState()
                    updateScreen()

                    statusText.text =
                        "Partita riaperta per correzione"
                    eventText.text =
                        "Undo normale attivo"
                    scoreAnnouncer.announceMessage(
                        "Partita riaperta"
                    )
                    return@runOnUiThread
                }

                restoreSuspendedCurrentMatch(
                    suspendedCurrentMatch
                )
                saveState()
                updateScreen()
                statusText.text =
                    "Riapertura partita fallita"
                eventText.text =
                    "Match corrente mantenuto"

                if (activityVisible) {
                    startLiveMatchPolling()
                }
            }
        }
    }

    private fun captureSuspendedCurrentMatch(): SuspendedCurrentMatch {
        return SuspendedCurrentMatch(
            currentLiveMatchId = currentLiveMatchId,
            teamLabelA = teamALabel,
            teamLabelB = teamBLabel,
            snapshot = captureCurrentScoreSnapshot(),
            matchLifecycleState = matchLifecycleState,
            scoreHistory = scoreHistory.toList()
        )
    }

    private fun restoreSuspendedCurrentMatch(
        suspendedCurrentMatch: SuspendedCurrentMatch
    ) {
        currentLiveMatchId =
            suspendedCurrentMatch.currentLiveMatchId
        updateTeamLabels(
            suspendedCurrentMatch.teamLabelA,
            suspendedCurrentMatch.teamLabelB
        )
        restoreScoreSnapshot(
            suspendedCurrentMatch.snapshot
        )
        matchLifecycleState =
            suspendedCurrentMatch.matchLifecycleState
        scoreHistory.clear()
        scoreHistory.addAll(
            suspendedCurrentMatch.scoreHistory
        )
    }

    private fun ArenaLastClosedMatch.toScoreSnapshot(): ScoreSnapshot {
        return ScoreSnapshot(
            pointsA = pointsA,
            pointsB = pointsB,
            gamesA = gamesA,
            gamesB = gamesB,
            setsA = setsA,
            setsB = setsB,
            advantageSide =
                when (advantageSide) {
                    Side.A.name -> Side.A
                    Side.B.name -> Side.B
                    else -> null
                },
            killerMode = killerMode,
            tieBreakActive = tieBreakActive,
            tieBreakPointsA = tieBreakPointsA,
            tieBreakPointsB = tieBreakPointsB,
            localMatchFinished = false,
            scoringSide = Side.A
        )
    }

    private fun logArenaLifecycleResult(
        operation: String,
        result: ArenaMatchLifecycleResult,
        eventId: String,
        eventSequence: Int,
        matchId: String?,
        gamesA: Int,
        gamesB: Int,
        setsA: Int,
        setsB: Int
    ) {
        val resolvedStatus =
            result.status
                ?: if (result.body == "network_error") {
                    "network_error"
                } else {
                    "unknown"
                }
        val resolvedMatchId =
            result.match?.matchId
                ?.takeIf { value ->
                    value.isNotBlank()
                }
                ?: matchId.orEmpty()

        Log.i(
            ARENA_LOG_TAG,
            "Arena lifecycle $operation result: " +
                    "status=$resolvedStatus, " +
                    "http=${result.statusCode}, " +
                    "eventId=$eventId, " +
                    "eventSequence=$eventSequence, " +
                    "matchId=$resolvedMatchId, " +
                    "gamesA=$gamesA, " +
                    "gamesB=$gamesB, " +
                    "setsA=$setsA, " +
                    "setsB=$setsB, " +
                    "body=${result.body}"
        )
    }

    private fun resetLocalScoreSilently() {
        pointsA = 0
        pointsB = 0
        gamesA = 0
        gamesB = 0
        setsA = 0
        setsB = 0
        advantageSide = null
        killerMode = false
        tieBreakActive = false
        tieBreakPointsA = 0
        tieBreakPointsB = 0
        localMatchFinished = false
        scoreHistory.clear()
    }

    private fun resetLocalScoreWithoutArenaSync(
        statusMessage: String,
        eventMessage: String
    ) {
        resetLocalScoreSilently()

        statusText.text =
            statusMessage

        eventText.text =
            eventMessage

        saveState()
        updateScreen()
    }

    private fun captureCurrentScoreSnapshot(): ScoreSnapshot {
        return ScoreSnapshot(
            pointsA = pointsA,
            pointsB = pointsB,
            gamesA = gamesA,
            gamesB = gamesB,
            setsA = setsA,
            setsB = setsB,
            advantageSide = advantageSide,
            killerMode = killerMode,
            tieBreakActive = tieBreakActive,
            tieBreakPointsA = tieBreakPointsA,
            tieBreakPointsB = tieBreakPointsB,
            localMatchFinished = localMatchFinished,
            scoringSide = Side.A
        )
    }

    private fun restoreScoreSnapshot(
        snapshot: ScoreSnapshot
    ) {
        pointsA = snapshot.pointsA
        pointsB = snapshot.pointsB
        gamesA = snapshot.gamesA
        gamesB = snapshot.gamesB
        setsA = snapshot.setsA
        setsB = snapshot.setsB
        advantageSide = snapshot.advantageSide
        killerMode = snapshot.killerMode
        tieBreakActive = snapshot.tieBreakActive
        tieBreakPointsA = snapshot.tieBreakPointsA
        tieBreakPointsB = snapshot.tieBreakPointsB
        localMatchFinished = snapshot.localMatchFinished
    }

    private fun clearFinishedLifecycleIfCourtChanged(
        selectedCourtId: String
    ) {
        val finishedCourtId =
            lastFinishedCourtId

        if (
            finishedCourtId != null &&
            finishedCourtId != selectedCourtId
        ) {
            clearMatchLifecycleState()
            saveState()
        }
    }

    private fun clearMatchLifecycleState() {
        matchLifecycleState =
            MatchLifecycleState.ACTIVE
        lastFinishedCourtId = null
        lastFinishedMatchId = null
        lastFinishedSnapshot = null
        lastFinishEventId = null
        lastFinishEventSequence = 0
    }

    private fun isCurrentScoreValidForLifecycle(): Boolean {
        return listOf(
            gamesA,
            gamesB,
            setsA,
            setsB
        ).all { value ->
            value in 0..99
        }
    }

    private fun isFinishSuccess(
        status: String?
    ): Boolean {
        return status == "finished" ||
                status == "already_finished" ||
                status == "duplicate_event"
    }

    private fun isReopenSuccess(
        status: String?
    ): Boolean {
        return status == "reopened" ||
                status == "already_reopened" ||
                status == "duplicate_event"
    }

    private fun resetMatch() {
        resetLocalScoreWithoutArenaSync(
            statusMessage =
                "Tabellone azzerato",
            eventMessage =
                "Ultimo evento: -"
        )
    }

    private fun resetDeviceAssignments() {
        deviceA = null
        deviceB = null

        assignmentMode = null

        lastPacketIdByDevice.clear()
        lastFallbackEventByDevice.clear()

        statusText.text =
            "Associazioni cancellate"

        eventText.text =
            "Ora riassocia entrambi i pulsanti"

        saveState()
        updateScreen()
    }

    override fun onDestroy() {
        stopBleWatchdog()
        unregisterBluetoothStateReceiver()

        stopLiveMatchPolling()

        if (scanning) {
            stopBleScan()
        }

        arenaManualExecutor.shutdownNow()
        arenaApiClient?.shutdown()
        arenaLiveMatchClient?.shutdown()
        arenaLiveScoreAdoptionClient?.shutdown()
        arenaMatchLifecycleClient?.shutdown()
        scoreAnnouncer.shutdown()

        super.onDestroy()
    }
}

enum class AssignmentMode {
    SIDE_A,
    SIDE_B
}

enum class Side {
    A,
    B
}

enum class MatchLifecycleState {
    ACTIVE,
    FINISHING,
    FINISHED_BY_ARENA,
    REOPENING;

    companion object {
        fun fromStoredValue(
            value: String?
        ): MatchLifecycleState {
            return entries.firstOrNull { state ->
                state.name == value
            } ?: ACTIVE
        }
    }
}

data class ParsedShellyPacket(
    val packetId: Int?,
    val buttonEvent: ButtonEvent?
)

data class FallbackEvent(
    val packetHex: String,
    val timestamp: Long
)

data class ScoreSnapshot(
    val pointsA: Int,
    val pointsB: Int,
    val gamesA: Int,
    val gamesB: Int,
    val setsA: Int,
    val setsB: Int,
    val advantageSide: Side?,
    val killerMode: Boolean,
    val tieBreakActive: Boolean,
    val tieBreakPointsA: Int,
    val tieBreakPointsB: Int,
    val localMatchFinished: Boolean,
    val scoringSide: Side
)

data class SuspendedCurrentMatch(
    val currentLiveMatchId: String?,
    val teamLabelA: String,
    val teamLabelB: String,
    val snapshot: ScoreSnapshot,
    val matchLifecycleState: MatchLifecycleState,
    val scoreHistory: List<ScoreSnapshot>
)

enum class ButtonEvent(
    val code: Int
) {
    SINGLE_PRESS(0x01),
    DOUBLE_PRESS(0x02),
    TRIPLE_PRESS(0x03),
    LONG_PRESS(0x04),
    HOLD_PRESS(0x80);

    companion object {
        fun fromCode(
            code: Int
        ): ButtonEvent? {
            return entries.firstOrNull {
                it.code == code
            }
        }
    }
}

private fun ByteArray.toHexString(): String {
    return joinToString(
        separator = " "
    ) { byte ->
        "%02X".format(
            byte.toInt() and 0xFF
        )
    }
}
