package com.example.padelboardarena

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.padelboardarena.arena.AndroidKeystoreArenaSessionStore
import com.example.padelboardarena.arena.ArenaAuthClient
import com.example.padelboardarena.arena.ArenaBuildConfig
import com.example.padelboardarena.arena.ArenaManualUiMessages
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ArenaSettingsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_SETTINGS_REQUEST =
            "com.example.padelboardarena.EXTRA_SETTINGS_REQUEST"
        const val REQUEST_ASSIGN_SIDE_A = "ASSIGN_SIDE_A"
        const val REQUEST_ASSIGN_SIDE_B = "ASSIGN_SIDE_B"

        private const val PREFS_NAME = "padelboard_arena"
        private const val PREF_DEVICE_A = "device_a"
        private const val PREF_DEVICE_B = "device_b"
        private const val ARENA_SETTINGS_LOG_TAG =
            "PadelBoardArena"
    }

    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var statusText: TextView
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arena_settings)

        bindViews()
        updateShellyAssignmentUi()

        loginButton.setOnClickListener {
            runArenaLogin()
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
        super.onDestroy()
    }
}
