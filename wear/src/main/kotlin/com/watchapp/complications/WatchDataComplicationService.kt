package com.watchapp.complications

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.watchapp.R
import com.watchapp.data.WatchDataStore

class WatchDataComplicationService : ComplicationDataSourceService() {
    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener,
    ) {
        val data = WatchDataStore.get()
        val complicationData: ComplicationData? = when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(textForSlot(request, data)).build(),
                contentDescription = PlainComplicationText.Builder("Data").build(),
            ).build()
            ComplicationType.SMALL_IMAGE -> {
                val icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_complication)
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(
                        image = icon,
                        type = SmallImageType.ICON,
                    ).build(),
                    contentDescription = PlainComplicationText.Builder("Weather").build(),
                ).build()
            }
            else -> null
        }
        listener.onComplicationData(complicationData)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("59°F").build(),
                contentDescription = PlainComplicationText.Builder("Preview").build(),
            ).build()
            ComplicationType.SMALL_IMAGE -> {
                val icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_complication)
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(image = icon, type = SmallImageType.ICON).build(),
                    contentDescription = PlainComplicationText.Builder("Weather").build(),
                ).build()
            }
            else -> null
        }
    }

    private fun textForSlot(
        request: ComplicationRequest,
        data: com.watchapp.data.WatchFaceData,
    ): String = when (request.complicationInstanceId) {
        100 -> data.temperature
        102 -> data.wind
        103 -> data.conditions
        106 -> data.sunrise
        107 -> data.location
        108 -> data.altitude
        109 -> data.sunset
        110 -> data.ambientTz1
        111 -> data.ambientTz2
        else -> "--"
    }
}
