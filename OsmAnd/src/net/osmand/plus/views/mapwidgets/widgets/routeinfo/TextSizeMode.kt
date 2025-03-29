package net.osmand.plus.views.mapwidgets.widgets.routeinfo

import net.osmand.plus.settings.enums.WidgetSize

enum class TextSizeMode(
    val minTextSizeSp: Int,
    val maxTextSizeSp: Int,
    val singleLineTextSizeSp: Int,
    val secondaryMaxTextSizeSp: Int?
) {
    SMALL(
        minTextSizeSp = 16,
        maxTextSizeSp = 20,
        singleLineTextSizeSp = 24,
        secondaryMaxTextSizeSp = null
    ),

    MEDIUM(
        minTextSizeSp = 16,
        maxTextSizeSp = 30,
        singleLineTextSizeSp = 36,
        secondaryMaxTextSizeSp = 18
    ),

    LARGE(
        minTextSizeSp = 16,
        maxTextSizeSp = 36,
        singleLineTextSizeSp = 60,
        secondaryMaxTextSizeSp = 24
    );

    companion object {
        @JvmStatic
        fun valueOf(widgetSize: WidgetSize): TextSizeMode {
            return when (widgetSize) {
                WidgetSize.SMALL -> SMALL
                WidgetSize.MEDIUM -> MEDIUM
                WidgetSize.LARGE -> LARGE
            }
        }
    }
}