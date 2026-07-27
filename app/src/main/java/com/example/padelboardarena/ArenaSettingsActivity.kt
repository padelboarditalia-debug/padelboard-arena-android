package com.example.padelboardarena

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.padelboardarena.arena.AndroidKeystoreArenaSessionStore
import com.example.padelboardarena.arena.ArenaAuthClient
import com.example.padelboardarena.arena.ArenaBuildConfig
import com.example.padelboardarena.arena.ArenaCourt
import com.example.padelboardarena.arena.ArenaCourtSelection
import com.example.padelboardarena.arena.ArenaCourtSelectionStore
import com.example.padelboardarena.arena.ArenaCourtsClient
import com.example.padelboardarena.arena.ArenaManualUiMessages
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ArenaSettingsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_SETTINGS_REQUEST =
            "com.example.padelboardarena.EXTRA_SETTINGS_REQUEST"
        const val REQUEST_ASSIGN_SIDE_A = "ASSIGN_SIDE_A"
        const val REQUEST_ASSIGN_SIDE_B = "ASSIGN_SIDE_B"
        const val REQUEST_COURT_CHANGED = "COURT_CHANGED"

        private const val PREFS_NAME = "padelboard_arena"
        private const val PREF_DEVICE_A = "device_a"
        private const val PREF_DEVICE_B = "device_b"
        private const val ARENA_SETTINGS_LOG_TAG =
            "PadelBoardArena"
    }

    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var statusText: TextView
    private lateinit var courtStatusText: TextView
    private lateinit var selectCourtButton: Button
    private lateinit var deviceAStatusText: TextView
    private lateinit var deviceBStatusText: TextView
    private lateinit var assignAButton: Button
    private lateinit var assignBButton: Button
    private lateinit var backButton: Button

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val arenaConfig by lazy {
        ArenaBuildConfig.load()
    }

    private val arenaAuthClient by lazy {
        ArenaAuthClient(
            config = arenaConfig,
            sessionStore =
                AndroidKeystoreArenaSessionStore(
                    this
                )
        )
    }

    private val arenaCourtsClient by lazy {
        ArenaCourtsClient(
            config = arenaConfig,
            tokenProvider = arenaAuthClient
        )
    }

    private val arenaCourtSelectionStore by lazy {
        ArenaCourtSelectionStore(
            this
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arena_settings)

        bindViews()
        updateCourtSelectionUi()
        updateShellyAssignmentUi()

        loginButton.setOnClickListener {
            runArenaLogin()
        }

        selectCourtButton.setOnClickListener {
            loadArenaCourtsForSelection()
        }

        assignAButton.setOnClickListener {
            returnAssignmentRequest(
                REQUEST_ASSIGN_SIDE_A
            )
        }

        assignBButton.setOnClickListener {
            returnAssignmentRequest(
                REQUEST_ASSIGN_SIDE_B
            )
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun bindViews() {
        passwordEditText =
            findViewById(R.id.arenaSettingsPasswordEditText)

        loginButton =
            findViewById(R.id.arenaSettingsLoginButton)

        statusText =
            findViewById(R.id.arenaSettingsStatusText)

        courtStatusText =
            findViewById(R.id.arenaSettingsCourtStatusText)

        selectCourtButton =
            findViewById(R.id.arenaSettingsSelectCourtButton)

        deviceAStatusText =
            findViewById(R.id.arenaSettingsDeviceAStatusText)

        deviceBStatusText =
            findViewById(R.id.arenaSettingsDeviceBStatusText)

        assignAButton =
            findViewById(R.id.arenaSettingsAssignAButton)

        assignBButton =
            findViewById(R.id.arenaSettingsAssignBButton)

        backButton =
            findViewById(R.id.arenaSettingsBackButton)
    }

    private fun updateCourtSelectionUi() {
        val selection =
            arenaCourtSelectionStore.readSelection()
                ?: fallbackArenaCourtSelection()

        courtStatusText.text =
            if (selection == null) {
                "Seleziona campo Arena"
            } else {
                buildString {
                    append(selection.selectedCenterName)
                    append("\n")
                    append(selection.selectedCourtLabel)
                    append("\n")

                    val tournamentCourtName =
                        selection.selectedTournamentCourtName

                    if (tournamentCourtName.isNullOrBlank()) {
                        append("Non collegato a un campo torneo")
                    } else {
                        append("Collegato a: ")
                        append(tournamentCourtName)
                    }
                }
            }
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

    private fun loadArenaCourtsForSelection() {
        selectCourtButton.isEnabled = false
        courtStatusText.text =
            "Caricamento campi Arena"

        executor.execute {
            val restoreResult =
                arenaAuthClient.restorePersistedSession()

            if (arenaAuthClient.currentAccessToken() == null) {
                runOnUiThread {
                    statusText.text =
                        "Login Arena richiesto"
                    selectCourtButton.isEnabled = true
                    updateCourtSelectionUi()
                }
                return@execute
            }

            val result =
                arenaCourtsClient.fetchCourtsBlocking()

            runOnUiThread {
                selectCourtButton.isEnabled = true

                if (!result.success) {
                    courtStatusText.text =
                        "Impossibile caricare i campi Arena"
                    return@runOnUiThread
                }

                statusText.text =
                    if (restoreResult.name == "RESTORED") {
                        "Arena connessa"
                    } else {
                        statusText.text
                    }

                showCourtSelectionDialog(
                    result.courts
                )
            }
        }
    }

    private fun showCourtSelectionDialog(
        courts: List<ArenaCourt>
    ) {
        if (courts.isEmpty()) {
            courtStatusText.text =
                "Nessun campo Arena disponibile"
            return
        }

        val labels =
            courts.map { court ->
                formatCourtSelectionLabel(
                    court
                )
            }.toTypedArray()

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Seleziona campo Arena"
            )
            .setItems(
                labels
            ) { _, which ->
                val court =
                    courts[which]

                if (!court.isActive) {
                    courtStatusText.text =
                        "${court.centerName}\n${court.label}\nCampo non attivo"
                    return@setItems
                }

                arenaCourtSelectionStore.saveSelection(
                    court
                )
                updateCourtSelectionUi()
                setResult(
                    RESULT_OK,
                    Intent().putExtra(
                        EXTRA_SETTINGS_REQUEST,
                        REQUEST_COURT_CHANGED
                    )
                )
            }
            .show()
    }

    private fun formatCourtSelectionLabel(
        court: ArenaCourt
    ): String {
        val mapping =
            if (court.tournamentCourtName.isNullOrBlank()) {
                "non pronto"
            } else {
                "collegato a ${court.tournamentCourtName}"
            }

        val activity =
            if (court.isActive) {
                ""
            } else {
                " - non attivo"
            }

        return "${court.centerName} - ${court.label} ($mapping)$activity"
    }

    private fun updateShellyAssignmentUi() {
        val preferences =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

        val deviceA =
            preferences.getString(
                PREF_DEVICE_A,
                null
            )

        val deviceB =
            preferences.getString(
                PREF_DEVICE_B,
                null
            )

        updateShellySideUi(
            associated = deviceA != null,
            statusText = deviceAStatusText,
            assignButton = assignAButton,
            statusLabel = "Pulsante A",
            assignLabel = "pulsante A"
        )

        updateShellySideUi(
            associated = deviceB != null,
            statusText = deviceBStatusText,
            assignButton = assignBButton,
            statusLabel = "Pulsante B",
            assignLabel = "pulsante B"
        )
    }

    private fun updateShellySideUi(
        associated: Boolean,
        statusText: TextView,
        assignButton: Button,
        statusLabel: String,
        assignLabel: String
    ) {
        statusText.text =
            if (associated) {
                "$statusLabel: associato"
            } else {
                "$statusLabel: non associato"
            }

        assignButton.text =
            if (associated) {
                "Sostituisci $assignLabel"
            } else {
                "Associa $assignLabel"
            }
    }

    private fun returnAssignmentRequest(
        request: String
    ) {
        setResult(
            RESULT_OK,
            Intent().putExtra(
                EXTRA_SETTINGS_REQUEST,
                request
            )
        )

        finish()
    }

    private fun runArenaLogin() {
        val password =
            passwordEditText.text
                ?.toString()
                .orEmpty()

        if (password.isBlank()) {
            statusText.text =
                "Arena: inserisci la password E2E"
            return
        }

        loginButton.isEnabled = false
        statusText.text =
            "Arena: login in corso"

        Log.i(
            ARENA_SETTINGS_LOG_TAG,
            "Arena login start"
        )

        executor.execute {
            try {
                arenaAuthClient.login(
                    password = password
                )

                Log.i(
                    ARENA_SETTINGS_LOG_TAG,
                    "Arena login success"
                )

                runOnUiThread {
                    passwordEditText.text?.clear()
                    statusText.text =
                        "Arena connessa"
                    loginButton.isEnabled = true
                }
            } catch (error: Exception) {
                Log.w(
                    ARENA_SETTINGS_LOG_TAG,
                    "Arena login failed: ${error.javaClass.simpleName} - " +
                            ArenaManualUiMessages.safeLoginFailureCause(
                                error
                            )
                )

                runOnUiThread {
                    statusText.text =
                        ArenaManualUiMessages.loginFailed(
                            error
                        )
                    loginButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        arenaCourtsClient.shutdown()
        super.onDestroy()
    }
}
