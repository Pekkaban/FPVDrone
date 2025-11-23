    package com.example.ardubluetooth

    import android.Manifest
    import android.os.Bundle
    import android.widget.Button
    import android.widget.TextView
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AppCompatActivity
    import kotlinx.coroutines.MainScope
    import kotlinx.coroutines.launch

    class MainActivity: AppCompatActivity() {
        private lateinit var bleManager: BleManager
        private var mavlinkHandler: MavlinkHandler? = null
        private val mainScope = MainScope()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            bleManager = BleManager(this)

            val btnScan = findViewById<Button>(R.id.btnScan)
            val tvStatus = findViewById<TextView>(R.id.tvStatus)
            val tvTelemetry = findViewById<TextView>(R.id.tvTelemetry)

             val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            }
            permLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION))

            btnScan.setOnClickListener {
                tvStatus.text = "Scanning..."
                bleManager.startScan { device ->
                    runOnUiThread { tvStatus.text = "Found ${'$'}{device.name ?: device.address}, connecting..." }
                    bleManager.connect(device) { ok ->
                        runOnUiThread {
                            tvStatus.text = if (ok) "Connected to ${'$'}{device.name ?: device.address}" else "Service not found / connect failed"
                            if (ok) {
                                mavlinkHandler = MavlinkHandler(bleManager) { key, payload ->
                                    mainScope.launch {
                                        when (key) {
                                            "heartbeat" -> tvTelemetry.text = "HEARTBEAT: ${'$'}{payload}
" + tvTelemetry.text
                                            "sys_status" -> tvTelemetry.text = "SYS_STATUS: ${'$'}{payload}
" + tvTelemetry.text
                                            else -> {}
                                        }
                                    }
                                }
                                mavlinkHandler?.start()
                            }
                        }
                    }
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            mavlinkHandler?.close()
            bleManager.disconnect()
        }
    }
