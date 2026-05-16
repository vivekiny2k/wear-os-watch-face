package com.watchapp.watchface

import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType

/**
 * Opens the action panel only on a short tap in the center zone.
 * Long press sends [TapType.CANCEL] so the system watch-face picker still works.
 */
internal class CenterTapHandler(
    private val surfaceHolder: SurfaceHolder,
) {
    private var downInCenter = false
    private var cancelled = false

    fun onTapEvent(tapType: Int, tapEvent: TapEvent): Boolean {
        val bounds = surfaceHolder.surfaceFrame ?: return false
        if (bounds.width() <= 0 || bounds.height() <= 0) return false

        when (tapType) {
            TapType.DOWN -> {
                cancelled = false
                downInCenter = tapInCenter(tapEvent, bounds)
            }
            TapType.CANCEL -> cancelled = true
            TapType.UP -> {
                if (!cancelled && downInCenter && tapInCenter(tapEvent, bounds)) {
                    return true
                }
            }
        }
        return false
    }

    private fun tapInCenter(tapEvent: TapEvent, bounds: Rect): Boolean {
        val (designX, designY) = bounds.tapToDesign(tapEvent.xPos, tapEvent.yPos)
        return isCenterTap(designX, designY)
    }
}
