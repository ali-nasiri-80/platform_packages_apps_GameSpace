/*
 * Copyright (C) 2020 The exTHmUI Open Source Project
 *               2021 AOSP-Krypton Project
 *               2022 crDroid Android Project
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
package io.chaldeaprjkt.gamespace.widget

import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.AttributeSet
import android.widget.TextView

import io.chaldeaprjkt.gamespace.R

@SuppressLint("AppCompatCustomView")
class BatteryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : TextView(context, attrs, defStyleAttr, defStyleRes) {

    private val batteryChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
                val percent = (level.toFloat() / scale * 100).toInt()
                text = context.getString(R.string.battery_format, percent)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                 val drawable = ContextCompat.getDrawable(context, R.drawable.battery_outline_chargeing)?.apply {
                 setBounds(0, 0, intrinsicWidth, intrinsicHeight) }

                setCompoundDrawables(drawable, null, null, null) }
            else {
                val drawable = ContextCompat.getDrawable(context, R.drawable.battery_outline)?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight) }

                setCompoundDrawables(drawable, null, null, null) }
        }
    }
}
    init {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        text = context.getString(
            R.string.battery_format,
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.registerReceiver(
            batteryChangeReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    override fun onDetachedFromWindow() {
        context.unregisterReceiver(batteryChangeReceiver)
        super.onDetachedFromWindow()
    }
}
