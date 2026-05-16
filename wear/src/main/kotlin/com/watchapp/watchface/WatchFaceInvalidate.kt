package com.watchapp.watchface

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicReference

object WatchFaceInvalidate {
    private val callback = AtomicReference<(() -> Unit)?>(null)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun register(invalidate: () -> Unit) {
        callback.set(invalidate)
    }

    fun unregister() {
        callback.set(null)
    }

    fun request() {
        val action = callback.get() ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post { callback.get()?.invoke() }
        }
    }
}
