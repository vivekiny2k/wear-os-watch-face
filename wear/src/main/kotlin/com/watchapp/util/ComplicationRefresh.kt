package com.watchapp.util

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

object ComplicationRefresh {
    fun requestUpdateAll(context: Context) {
        val component = ComponentName(context, "com.watchapp.complications.WatchDataComplicationService")
        ComplicationDataSourceUpdateRequester.create(context, component).requestUpdateAll()
    }
}
