package com.watchapp.watchface

import androidx.annotation.DrawableRes
import com.watchapp.R

object LayoutPreviewIcons {
    @DrawableRes
    fun activeLayoutPreview(): Int =
        if (WatchLayoutState.isFlightLayout()) R.drawable.flight_active_preview else R.drawable.ref_active_preview
}
