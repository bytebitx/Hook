package com.bbggo.hooklib.startup

import android.content.Context
import androidx.startup.Initializer
import com.bbggo.hooklib.HookUtils

class HookInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        HookUtils.hookInit(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}