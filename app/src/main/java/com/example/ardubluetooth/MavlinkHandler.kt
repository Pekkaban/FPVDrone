package com.example.ardubluetooth

import io.dronefleet.mavlink.MavlinkConnection
import io.dronefleet.mavlink.common.Heartbeat
import io.dronefleet.mavlink.common.SysStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import java.io.PipedInputStream
import java.io.PipedOutputStream

class MavlinkHandler(private val ble: BleManager, private val onTelemetry: (String, Any)->Unit) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var connection: MavlinkConnection
    private val psIn = PipedInputStream(64 * 1024)
    private val psOut = PipedOutputStream(psIn)

    fun start() {
        connection = MavlinkConnection.builder(psIn, psOut).build()

        scope.launch {
            try {
                val it = connection.iterator()
                while (it.hasNext()) {
                    val msg = it.next()
                    when (msg) {
                        is Heartbeat -> onTelemetry("heartbeat", msg)
                        is SysStatus -> onTelemetry("sys_status", msg)
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        scope.launch {
            ble.incoming.consumeEach { bytes ->
                try {
                    psOut.write(bytes)
                    psOut.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun close() {
        scope.cancel()
        try { psOut.close(); psIn.close() } catch (_:Exception) {}
    }
}
