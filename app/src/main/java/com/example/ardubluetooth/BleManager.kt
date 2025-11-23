package com.example.ardubluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.channels.Channel
import java.util.*

class BleManager(private val ctx: Context) {
    companion object {
        private val TAG = "BleManager"
        val NUS_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS_TX_CHAR = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS_RX_CHAR = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bm.adapter
    }

    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var rxChar: BluetoothGattCharacteristic? = null
    private var txChar: BluetoothGattCharacteristic? = null

    val incoming = Channel<ByteArray>(Channel.UNLIMITED)

    @SuppressLint("MissingPermission")
    fun startScan(onDeviceFound: (BluetoothDevice)->Unit) {
        val scanCallback = object: ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                result.device?.let { onDeviceFound(it) }
            }
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner?.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, onConnected: (Boolean)->Unit) {
        gatt = device.connectGatt(ctx, false, object: BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "connected -> discover services")
                    g.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "disconnected")
                    onConnected(false)
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                val svc = g.getService(NUS_SERVICE)
                if (svc != null) {
                    txChar = svc.getCharacteristic(NUS_TX_CHAR)
                    rxChar = svc.getCharacteristic(NUS_RX_CHAR)
                    g.setCharacteristicNotification(rxChar, true)
                    val descriptor = rxChar?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    descriptor?.let { g.writeDescriptor(it) }
                    onConnected(true)
                } else {
                    onConnected(false)
                }
            }

            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val data = characteristic.value
                incoming.trySend(data)
            }

            override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun write(bytes: ByteArray) {
        txChar?.let {
            it.value = bytes
            gatt?.writeCharacteristic(it)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.close()
        gatt = null
    }
}
