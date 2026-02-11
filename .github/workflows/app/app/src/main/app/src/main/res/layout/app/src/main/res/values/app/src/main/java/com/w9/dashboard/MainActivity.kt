package com.w9.dashboard

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var dataText: TextView

    private val serviceUUID = UUID.fromString("0000ff60-0000-1000-8000-00805f9b34fb")
    private val notifyUUID = UUID.fromString("0000ff61-0000-1000-8000-00805f9b34fb")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        dataText = findViewById(R.id.dataText)

        startScan()
    }

    private fun startScan() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val scanner = adapter.bluetoothLeScanner

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        statusText.text = "Skanowanie..."
        scanner.startScan(null, ScanSettings.Builder().build(), scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.name?.contains("W9", true) == true) {
                statusText.text = "Łączenie..."
                result.device.connectGatt(this@MainActivity, false, gattCallback)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread { statusText.text = "Połączono" }
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(serviceUUID)
            val characteristic = service?.getCharacteristic(notifyUUID)

            if (characteristic != null) {
                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            val hex = data.joinToString(" ") { "%02X".format(it) }

            runOnUiThread {
                dataText.text = "FF61:\n$hex"
            }
        }
    }
}
