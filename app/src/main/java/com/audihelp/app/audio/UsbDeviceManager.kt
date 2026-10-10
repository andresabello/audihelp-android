package com.audihelp.app.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class UsbAudioDeviceState(
    val connected: Boolean = false,
    val deviceName: String? = null,
    val deviceId: Int = -1,
)

/**
 * Monitors USB device attach/detach and reports whether a USB audio INPUT device
 * (e.g. DJI Mic Mini via USB-C) is currently connected.
 *
 * Audio capture itself is handled by Oboe — it automatically routes to the USB
 * audio device when present. This class only provides the UI connection status.
 */
@Singleton
class UsbDeviceManager @Inject constructor(
    private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _deviceState = MutableStateFlow(UsbAudioDeviceState())
    val deviceState: StateFlow<UsbAudioDeviceState> = _deviceState

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshState()
        }
    }

    fun register() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        registered = true
        refreshState()
    }

    fun unregister() {
        if (registered) {
            context.unregisterReceiver(receiver)
            registered = false
        }
    }

    private fun refreshState() {
        val usbMic = audioManager
            .getDevices(AudioManager.GET_DEVICES_INPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                           it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                           it.type == AudioDeviceInfo.TYPE_USB_HEADSET }

        _deviceState.value = if (usbMic != null) {
            UsbAudioDeviceState(connected = true, deviceName = usbMic.productName?.toString() ?: "USB mic", deviceId = usbMic.id)
        } else {
            UsbAudioDeviceState()
        }
    }
}
