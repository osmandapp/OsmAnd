package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import android.content.Context
import net.osmand.plus.R
import net.osmand.plus.mapcontextmenu.builders.AmenityUIHelper
import net.osmand.shared.osm.PoiType
import net.osmand.util.Algorithms

open class DefaultPoiAdditionalRowBehaviour : IPoiAdditionalRowBehavior {

    override fun applyCustomRules(params: PoiRowParams) {
        with(params) {
            rule.customIconId?.apply { builder.setIconId(rule.customIconId) }
            rule.customTextPrefixId?.apply { builder.setTextPrefix(app.getString(rule.customTextPrefixId)) }

            builder.setIsWiki(rule.isWikipedia)
                .setNeedLinks(rule.isNeedLinks)
                .setIsPhoneNumber(rule.isPhoneNumber)
                .setTextLinesLimit(rule.textLinesLimit)
        }
    }

    override fun applyCommonRules(params: PoiRowParams) {
        with(params) {
            var isUrl = rule.isUrl || Algorithms.isUrl(value)
            if (!builder.hasHiddenUrl() && !isUrl && builder.isNeedLinks()) {
                val hiddenUrl = AmenityUIHelper.getSocialMediaUrl(key, value)
                if (hiddenUrl != null) {
                    builder.setHiddenUrl(hiddenUrl)
                    isUrl = true
                }
            }
            builder.setIsUrl(isUrl)

            if (poiType != null) {
                builder.setOrder(poiType.getOrder())
                builder.setName(poiType.getKeyName())
                builder.setIsText(poiType.isText())

                // try to fetch appropriate icon, text and textPrefix based on poi additional type
                // (if this parameters was not predefined)

                if (!builder.hasIcon()) { // if icon wasn't predefined
                    var iconId = getIconId(context, poiType.getIconKeyName())
                    if (iconId == 0) {
                        val category = poiType.getOsmTag()?.replace(":", "_") ?: ""
                        if (category.isNotEmpty()) {
                            iconId = getIconId(context, category)
                        }
                        val parentType = poiType.getParentType()
                        if (iconId == 0 && parentType is PoiType) {
                            iconId = getIconId(context, parentType.getIconKeyName())
                            if (iconId == 0) {
                                builder.setIconNameCandidates(
                                    listOf(
                                        parentType.getOsmTag() + "_" + category + "_" + parentType.getOsmValue(),
                                        parentType.getOsmTag() + "_" + parentType.getOsmValue()
                                    )
                                )
                            }
                        }
                    }
                    builder.setIconId(iconId)

                    if (iconId == 0) {
                        builder.setFallbackIconId(R.drawable.ic_action_info_dark)
                    }
                }

                val isTextPredefined = builder.hasTextPrefix() || builder.hasText()
                if (!builder.hasTextPrefix() || !builder.hasText()) {
                    val translation = poiType.getTranslation()
                    if (poiType.isText()) {
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

                val textPrefix = builder.getTextPrefix() ?: ""
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
