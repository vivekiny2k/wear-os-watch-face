package com.watchapp.watchface

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.watchapp.R

/** Typefaces aligned with [faces/preview.jpg]. */
object FaceFonts {
    private var dseg7Bold: Typeface? = null
    private var clockMonoBold: Typeface? = null

    fun digitalSeven(context: Context): Typeface {
        dseg7Bold?.let { return it }
        val loaded = ResourcesCompat.getFont(context, R.font.dseg7_classic_bold)
            ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        dseg7Bold = loaded
        return loaded
    }

    /** Boxed secondary timezone (TZ2) — 7-segment LCD. */
    fun altClock(context: Context): Typeface = digitalSeven(context)

    /** Ambient TZ2 line. */
    fun timezone2(context: Context): Typeface = digitalSeven(context)

    /** Main HH:mm:ss — fixed-width square mono (no tick jitter). */
    fun clock(context: Context): Typeface {
        clockMonoBold?.let { return it }
        val loaded = ResourcesCompat.getFont(context, R.font.clock_mono_bold)
            ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        clockMonoBold = loaded
        return loaded
    }

    /** Ordinal date — narrow condensed so it fits before the TZ2 box. */
    fun date(): Typeface =
        Typeface.create("sans-serif-condensed", Typeface.BOLD)
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun temperature(): Typeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun wind(): Typeface =
        Typeface.create(Typeface.SERIF, Typeface.NORMAL)

    fun conditions(): Typeface =
        Typeface.create("sans-serif-condensed", Typeface.BOLD)
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun solarTime(): Typeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    /** City name — narrow condensed, not bold. */
    fun location(): Typeface =
        Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    fun altitude(): Typeface =
        Typeface.create(Typeface.SERIF, Typeface.NORMAL)
}
