/*
 * Copyright (C) 2020 The exTHmUI Open Source Project
 * Copyright (C) 2021 AOSP-Krypton Project
 * Copyright (C) 2022 Nameless-AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.chaldeaprjkt.gamespace.gamebar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioSystem
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log

import androidx.core.app.ActivityCompat

import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import javax.inject.Inject

import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings

@ServiceScoped
class CallListener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings
) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)
    private val telecomManager = context.getSystemService(TelecomManager::class.java)

    private val callsMode = appSettings.callsMode

    private val telephonyCallback = Callback()

    private var executor: ExecutorService? = null

    private var callStatus: Int = TelephonyManager.CALL_STATE_OFFHOOK

    fun init() {
        executor = Executors.newSingleThreadExecutor()
        telephonyManager?.registerTelephonyCallback(executor!!, telephonyCallback)
    }

    fun destory() {
        telephonyManager?.unregisterTelephonyCallback(telephonyCallback)
        executor?.shutdownNow()
    }

    private fun isHeadsetPluggedIn(): Boolean {
        val audioDeviceInfoArr: Array<AudioDeviceInfo> =
            audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
        return audioDeviceInfoArr.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "App does not have required permission ANSWER_PHONE_CALLS")
            return false
        }
        return true
    }

    private inner class Callback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        private var previousState = TelephonyManager.CALL_STATE_IDLE
        private var previousAudioMode = audioManager?.mode

        override fun onCallStateChanged(state: Int) {
            if (callsMode == 0) return
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    if (!checkPermission()) return
                    @Suppress("DEPRECATION")
                    if (callsMode == 1) {
                        telecomManager?.acceptRingingCall()
                    } else {
                        telecomManager?.endCall()
                    }
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (callsMode == 2) return
                    if (previousState == TelephonyManager.CALL_STATE_RINGING) {
                        if (isHeadsetPluggedIn()) {
                            audioManager?.isSpeakerphoneOn = false
                            AudioSystem.setForceUse(
                                AudioSystem.FOR_COMMUNICATION,
                                AudioSystem.FORCE_NONE
                            )
                        } else {
                            audioManager?.isSpeakerphoneOn = true
                            AudioSystem.setForceUse(
                                AudioSystem.FOR_COMMUNICATION,
                                AudioSystem.FORCE_SPEAKER
                            )
                        }
                        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (callsMode == 2) return
                    if (previousState == TelephonyManager.CALL_STATE_OFFHOOK) {
                        audioManager?.mode = previousAudioMode ?: AudioManager.MODE_NORMAL
                        AudioSystem.setForceUse(
                            AudioSystem.FOR_COMMUNICATION,
                            AudioSystem.FORCE_NONE
                        )
                        audioManager?.isSpeakerphoneOn = false
                    }
                }
            }
            previousState = state
        }
    }

    companion object {
        private const val TAG = "CallListener"
    }
}