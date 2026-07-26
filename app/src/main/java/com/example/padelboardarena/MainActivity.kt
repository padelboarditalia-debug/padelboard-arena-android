package com.example.padelboardarena

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.padelboardarena.arena.ArenaApiClient
import com.example.padelboardarena.arena.ArenaAuthClient
import com.example.padelboardarena.arena.ArenaBuildConfig
import com.example.padelboardarena.arena.ArenaManualSnapshotFactory
import com.example.padelboardarena.arena.ArenaManualUiMessages
import com.example.padelboardarena.arena.SharedPreferencesArenaManualSequenceStore
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
    }

    private lateinit var statusText: TextView
    private lateinit var phaseText: TextView
    private lateinit var eventText: TextView

    private lateinit var scoreAText: TextView
    private lateinit var scoreBText: TextView

    private lateinit var gamesAText: TextView
    private lateinit var gamesBText: TextView

    private lateinit var deviceAText: TextView
    private lateinit var deviceBText: TextView

    private lateinit var startButton: Button
    private lateinit var assignAButton: Button
    private lateinit var assignBButton: Button
    private lateinit var resetScoreButton: Button
    private lateinit var resetDevicesButton: Button
    private lateinit var arenaPasswordEditText: EditText
    private lateinit var arenaLoginButton: Button
    private lateinit var arenaSendTestButton: Button
    private lateinit var arenaConnectionStatusText: TextView
    private lateinit var arenaLastCallText: TextView

    private var scanning = false

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

    private var advantageSide: Side? = null
    private var killerMode = false

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

    private val arenaAuthClient by lazy {
        ArenaAuthClient(
            config = arenaConfig
        )
    }

    private val arenaApiClient by lazy {
        ArenaApiClient(
            config = arenaConfig,
            tokenProvider = arenaAuthClient
        )
    }

    private val arenaManualSnapshotFactory by lazy {
        ArenaManualSnapshotFactory(
            sequenceStore =
                SharedPreferencesArenaManualSequenceStore(
                    preferences
                )
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
        setContentView(R.layout.activity_main)

        bindViews()
        loadSavedData()
        updateScreen()
        setupArenaManualTest()

        startButton.setOnClickListener {
            if (scanning) {
                stopBleScan()
            } else {
                checkPermissionsAndStart()
            }
        }

        assignAButton.setOnClickListener {
            beginAssignment(
                AssignmentMode.SIDE_A
            )
        }

        assignBButton.setOnClickListener {
            beginAssignment(
                AssignmentMode.SIDE_B
            )
        }

        resetScoreButton.setOnClickListener {
            resetMatch()
        }

        resetDevicesButton.setOnClickListener {
            resetDeviceAssignments()
        }
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

        gamesAText =
            findViewById(R.id.gamesAText)

        gamesBText =
            findViewById(R.id.gamesBText)

        deviceAText =
            findViewById(R.id.deviceAText)

        deviceBText =
            findViewById(R.id.deviceBText)

        startButton =
            findViewById(R.id.startButton)

        assignAButton =
            findViewById(R.id.assignAButton)

        assignBButton =
            findViewById(R.id.assignBButton)

        resetScoreButton =
            findViewById(R.id.resetScoreButton)

        resetDevicesButton =
            findViewById(R.id.resetDevicesButton)

        arenaPasswordEditText =
            findViewById(R.id.arenaPasswordEditText)

        arenaLoginButton =
            findViewById(R.id.arenaLoginButton)

        arenaSendTestButton =
            findViewById(R.id.arenaSendTestButton)

        arenaConnectionStatusText =
            findViewById(R.id.arenaConnectionStatusText)

        arenaLastCallText =
            findViewById(R.id.arenaLastCallText)
    }

    private fun setupArenaManualTest() {
        arenaSendTestButton.isEnabled = false

        arenaLoginButton.setOnClickListener {
            runArenaLogin()
        }

        arenaSendTestButton.setOnClickListener {
            runArenaSendTest()
        }
    }

    private fun runArenaLogin() {
        val password =
            arenaPasswordEditText.text
                ?.toString()
                .orEmpty()

        if (password.isBlank()) {
            arenaConnectionStatusText.text =
                "Arena: inserisci la password E2E"
            return
        }

        arenaLoginButton.isEnabled = false
        arenaSendTestButton.isEnabled = false
        arenaConnectionStatusText.text =
            "Arena: login in corso"

        arenaManualExecutor.execute {
            try {
                arenaAuthClient.login(
                    password = password
                )

                runOnUiThread {
                    arenaPasswordEditText.text?.clear()
                    arenaConnectionStatusText.text =
                        "Arena: login riuscito"
                    arenaSendTestButton.isEnabled = true
                    arenaLoginButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    arenaConnectionStatusText.text =
                        ArenaManualUiMessages.loginFailed(
                            error
                        )
                    arenaSendTestButton.isEnabled = false
                    arenaLoginButton.isEnabled = true
                }
            }
        }
    }

    private fun runArenaSendTest() {
        arenaSendTestButton.isEnabled = false
        arenaLastCallText.text =
            "Ultima chiamata Arena: invio in corso"

        arenaManualExecutor.execute {
            try {
                val snapshot =
                    arenaManualSnapshotFactory
                        .createSnapshot()

                val result =
                    arenaApiClient.sendStateBlocking(
                        snapshot
                    )

                runOnUiThread {
                    arenaLastCallText.text =
                        ArenaManualUiMessages.apiResult(
                            result
                        )
                    arenaSendTestButton.isEnabled =
                        arenaAuthClient.currentAccessToken() != null
                }
            } catch (error: Exception) {
                runOnUiThread {
                    arenaLastCallText.text =
                        ArenaManualUiMessages.apiFailed(
                            error
                        )
                    arenaSendTestButton.isEnabled =
                        arenaAuthClient.currentAccessToken() != null
                }
            }
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
            .putString(
                PREF_ADVANTAGE_SIDE,
                advantageSide?.name
            )
            .putBoolean(
                PREF_KILLER_MODE,
                killerMode
            )
            .apply()
    }

    private fun updateScreen() {
        scoreAText.text =
            displayPointForSide(
                Side.A
            )

        scoreBText.text =
            displayPointForSide(
                Side.B
            )

        gamesAText.text =
            gamesA.toString()

        gamesBText.text =
            gamesB.toString()

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

    private fun displayPointForSide(
        side: Side
    ): String {
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
            return
        }

        val scanner =
            bluetoothAdapter.bluetoothLeScanner

        if (scanner == null) {
            statusText.text =
                "Scanner BLE non disponibile"
            return
        }

        val settings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .setReportDelay(0)
                .build()

        scanner.startScan(
            null,
            settings,
            scanCallback
        )

        scanning = true

        startButton.text =
            "Ferma rilevamento"

        if (assignmentMode == null) {
            statusText.text =
                "Scanner attivo: premi uno Shelly"
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        bluetoothManager.adapter
            ?.bluetoothLeScanner
            ?.stopScan(scanCallback)

        scanning = false
        assignmentMode = null

        statusText.text =
            "Scanner fermo"

        startButton.text =
            "Avvia rilevamento Shelly"
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

                    statusText.text =
                        "Errore scansione BLE: $errorCode"

                    startButton.text =
                        "Avvia rilevamento Shelly"
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

        val parsedPacket =
            parseShellyPacket(
                serviceData
            )

        val buttonEvent =
            parsedPacket.buttonEvent
                ?: return

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
                undoLastAction()

            ButtonEvent.TRIPLE_PRESS -> {
                statusText.text =
                    "Tripla pressione rilevata"

                eventText.text =
                    "Nessuna azione configurata"
            }

            ButtonEvent.LONG_PRESS,
            ButtonEvent.HOLD_PRESS -> {
                statusText.text =
                    "Pressione lunga rilevata"

                eventText.text =
                    "Nessuna azione configurata"
            }
        }
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

        saveState()
        updateScreen()

        Toast.makeText(
            this,
            "Associazione completata",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun registerPoint(
        scoringSide: Side
    ) {
        saveSnapshot()

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

            saveState()
            updateScreen()
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

            saveState()
            updateScreen()
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

            saveState()
            updateScreen()
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

            saveState()
            updateScreen()
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

        saveState()
        updateScreen()
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

    private fun saveSnapshot() {
        scoreHistory.add(
            ScoreSnapshot(
                pointsA = pointsA,
                pointsB = pointsB,
                gamesA = gamesA,
                gamesB = gamesB,
                advantageSide = advantageSide,
                killerMode = killerMode
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

        pointsA =
            snapshot.pointsA

        pointsB =
            snapshot.pointsB

        gamesA =
            snapshot.gamesA

        gamesB =
            snapshot.gamesB

        advantageSide =
            snapshot.advantageSide

        killerMode =
            snapshot.killerMode

        statusText.text =
            "ULTIMA AZIONE ANNULLATA"

        eventText.text =
            "Doppia pressione"

        saveState()
        updateScreen()
    }

    private fun resetMatch() {
        pointsA = 0
        pointsB = 0

        gamesA = 0
        gamesB = 0

        advantageSide = null
        killerMode = false

        scoreHistory.clear()

        statusText.text =
            "Partita azzerata"

        eventText.text =
            "Ultimo evento: —"

        saveState()
        updateScreen()
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
        if (scanning) {
            stopBleScan()
        }

        arenaManualExecutor.shutdownNow()
        arenaApiClient.shutdown()

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
    val advantageSide: Side?,
    val killerMode: Boolean
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
