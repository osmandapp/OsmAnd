package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import android.content.Context
import net.osmand.osm.PoiType
import net.osmand.plus.R
import net.osmand.plus.mapcontextmenu.builders.AmenityUIHelper
import net.osmand.util.Algorithms

open class DefaultPoiAdditionalRowBehaviour : IPoiAdditionalRowBehavior {

    override fun applyCustomRules(params: PoiRowParams) {
        with(params) {
            rule.customIconId?.apply { builder.setIconId(rule.customIconId) }
            rule.customTextPrefixId?.apply { builder.setTextPrefix(app.getString(rule.customTextPrefixId)) }

            builder.setIsWiki(rule.isWikipedia)
                .setNeedLinks(rule.isNeedLinks)
                .setIsPhoneNumber(rule.isPhoneNumber)
        }
    }

    override fun applyCommonRules(params: PoiRowParams) {
        with(params) {
            var isUrl = rule.isUrl || Algorithms.isUrl(value)
            if (!builder.hasHiddenUrl() && !isUrl && builder.isNeedLinks) {
                val hiddenUrl = AmenityUIHelper.getSocialMediaUrl(key, value)
                if (hiddenUrl != null) {
                    builder.setHiddenUrl(hiddenUrl)
                    isUrl = true
                }
            }
            builder.setIsUrl(isUrl)

            if (poiType != null) {
                builder.setOrder(poiType.order)
                builder.setName(poiType.keyName)
                builder.setIsText(poiType.isText)

                // try to fetch appropriate icon, text and textPrefix based on poi additional type
                // (if this parameters was not predefined)

                if (!builder.hasIcon()) { // if icon wasn't predefined
                    var iconId = getIconId(context, poiType.iconKeyName)
                    if (iconId == 0) {
                        val category = poiType.osmTag.replace(":", "_")
                        if (category.isNotEmpty()) {
                            iconId = getIconId(context, category)
                        }
                        val parentType = poiType.parentType
                        if (iconId == 0 && parentType is PoiType) {
                            iconId = getIconId(context, parentType.iconKeyName)
                            if (iconId == 0) {
                                var iconName =
                                    parentType.osmTag + "_" + category + "_" + parentType.osmValue
                                builder.setIcon(menuBuilder.getRowIcon(context, iconName))
                                if (!builder.hasIcon()) {
                                    iconName = parentType.osmTag + "_" + parentType.osmValue
                                    builder.setIcon(menuBuilder.getRowIcon(context, iconName))
                                }
                            }
                        }
                    }
                    builder.setIconId(iconId)

                    if (!builder.hasIcon()) {
                        builder.setIconId(R.drawable.ic_action_info_dark)
                    }
                }

                val isTextPredefined = builder.hasTextPrefix() || builder.hasText()
                if (!builder.hasTextPrefix() || !builder.hasText()) {
                    val translation = poiType.translation
                    if (poiType.isText) {
                        builder.setTextPrefixIfNotPresent(translation)
                        builder.setTextIfNotPresent(value)
                    } else if (translation.contains(":")) {
                        val parts = translation.split(":")
                        builder.setTextPrefixIfNotPresent(parts[0].trim())
                        builder.setTextIfNotPresent(Algorithms.capitalizeFirstLetter(parts[1].trim()))
                    } else {
                        builder.setTextIfNotPresent(translation)
                    }
                }

                val textPrefix = builder.textPrefix
                if (!isTextPredefined && textPrefix.contains(" (")) {
                    val prefixParts = textPrefix.split(" (")
                    if (prefixParts.size == 2) {
                        builder.setTextPrefix(
                            context.getString(
                                R.string.ltr_or_rtl_combine_via_colon, prefixParts[0],
                                Algorithms.capitalizeFirstLetterAndLowercase(prefixParts[1])
                                    .replace(Regex("[()]"), "")
                            )
                        )
                    }
                }
            }
        }
    }

    fun getIconId(context: Context, key: String): Int {
        return context.resources.getIdentifier("mx_$key", "drawable", context.packageName)
    }

    fun formatPrefix(prefix: String, units: String): String {
        return if (prefix.isNotEmpty()) "$prefix, $units" else units
    }
}