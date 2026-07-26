package com.example.padelboardarena

import android.os.Bundle
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
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var statusText: TextView
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

        loginButton.setOnClickListener {
            runArenaLogin()
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

        backButton =
            findViewById(R.id.arenaSettingsBackButton)
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

        executor.execute {
            try {
                arenaAuthClient.login(
                    password = password
                )

                runOnUiThread {
                    passwordEditText.text?.clear()
                    statusText.text =
                        "Arena connessa"
                    loginButton.isEnabled = true
                }
            } catch (error: Exception) {
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
