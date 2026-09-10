package com.spoofer.command

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.spoofer.service.MockLocationService

class CommandActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runCatching {
            CommandArguments.parse(
                action = intent.action,
                extras = intent.extras?.keySet()?.associateWith { intent.getStringExtra(it).orEmpty() }.orEmpty(),
            )
        }.onSuccess { command ->
            execute(command)
            setResult(RESULT_OK)
        }.onFailure { error ->
            Log.e(TAG, error.message, error)
            setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, error.message))
        }

        finish()
    }

    private fun execute(command: SpooferCommand) {
        val serviceIntent = Intent(this, MockLocationService::class.java)
        when (command) {
            is SpooferCommand.SetLocation -> {
                serviceIntent.action = MockLocationService.ACTION_SET_STATIC
                serviceIntent.putExtra(MockLocationService.EXTRA_LATITUDE, command.location.latitude)
                serviceIntent.putExtra(MockLocationService.EXTRA_LONGITUDE, command.location.longitude)
                ContextCompat.startForegroundService(this, serviceIntent)
            }
            is SpooferCommand.StartRoute -> {
                serviceIntent.action = MockLocationService.ACTION_START_MOVEMENT
                serviceIntent.putExtra(MockLocationService.EXTRA_LATITUDE, command.origin.latitude)
                serviceIntent.putExtra(MockLocationService.EXTRA_LONGITUDE, command.origin.longitude)
                serviceIntent.putExtra(MockLocationService.EXTRA_DEST_LATITUDE, command.destination.latitude)
                serviceIntent.putExtra(MockLocationService.EXTRA_DEST_LONGITUDE, command.destination.longitude)
                command.speedKmh?.let {
                    serviceIntent.putExtra(MockLocationService.EXTRA_SPEED, it / KILOMETERS_PER_HOUR_PER_METER_PER_SECOND)
                }
                command.durationSeconds?.let {
                    serviceIntent.putExtra(MockLocationService.EXTRA_DURATION_SECONDS, it)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            }
            SpooferCommand.Stop -> startService(serviceIntent.apply { action = MockLocationService.ACTION_STOP })
        }
    }

    companion object {
        private const val TAG = "SpooferCommand"
        private const val EXTRA_ERROR = "error"
        private const val KILOMETERS_PER_HOUR_PER_METER_PER_SECOND = 3.6f
    }
}
