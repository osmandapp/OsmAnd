package net.osmand.plus.mapcontextmenu.builders.rows

import net.osmand.data.Amenity
import net.osmand.plus.R
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.CuisineRowBehavior
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.DistanceRowBehaviour
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.EleRowBehaviour
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.CapacityRowBehaviour
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.LiquidCapacityRowBehaviour
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.MaxWeightRowBehaviour
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.MetricRowBehaviour
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.OpeningHoursRowBehavior
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.PopulationRowBehaviour
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.UsMapsRecreationAreaRowBehaviour
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.WikipediaRowBehavior

object PoiAdditionalUiRules {

    private val RULES: List<PoiAdditionalUiRule> = listOf(
        PoiAdditionalUiRule(
            key = Amenity.COLLECTION_TIMES,
            customIconId = R.drawable.ic_action_time,
            isNeedLinks = false
        ),
        PoiAdditionalUiRule(
            key = Amenity.SERVICE_TIMES,
            customIconId = R.drawable.ic_action_time,
            isNeedLinks = false
        ),
        PoiAdditionalUiRule(
            key = Amenity.OPENING_HOURS,
            customIconId = R.drawable.ic_action_time,
            customTextPrefixId = R.string.opening_hours,
            isNeedLinks = false,
            behavior = OpeningHoursRowBehavior
        ),
        PoiAdditionalUiRule(
            key = Amenity.PHONE,
            customIconId = R.drawable.ic_action_call_dark,
            customTextPrefixId = R.string.poi_phone,
            isPhoneNumber = true
        ),
        PoiAdditionalUiRule(
            key = Amenity.MOBILE,
            customIconId = R.drawable.ic_action_phone,
            customTextPrefixId = R.string.poi_mobile,
            isPhoneNumber = true
        ),
        PoiAdditionalUiRule(
            key = Amenity.WEBSITE,
            customIconId = R.drawable.ic_world_globe_dark,
            isUrl = true,
        ),
        PoiAdditionalUiRule(
            key = Amenity.URL,
            customIconId = R.drawable.ic_world_globe_dark,
            isUrl = true,
        ),
        PoiAdditionalUiRule(
            key = Amenity.CUISINE,
            customIconId = R.drawable.ic_action_cuisine,
            customTextPrefixId = R.string.poi_cuisine,
            behavior = CuisineRowBehavior
        ),
        PoiAdditionalUiRule(
            key = Amenity.DESCRIPTION,
            customIconId = R.drawable.ic_action_note_dark,
            checkBaseKey = true,
            checkKeyOnContains = true
        ),
        PoiAdditionalUiRule(
            key = Amenity.WIKIPEDIA,
            customIconId = R.drawable.ic_plugin_wikipedia,
            customTextPrefixId = R.string.shared_string_wikipedia,
            isWikipedia = true,
            checkBaseKey = false,
            checkKeyOnContains = true,
            behavior = WikipediaRowBehavior
        ),
        PoiAdditionalUiRule(
            key = "addr:housename",
            checkBaseKey = false,
            customIconId = R.drawable.ic_action_poi_name
        ),
        PoiAdditionalUiRule(
            key = "whitewater:rapid_name",
            checkBaseKey = false,
            customIconId = R.drawable.ic_action_poi_name
        ),
        PoiAdditionalUiRule(
            key = Amenity.OPERATOR,
            customIconId = R.drawable.ic_action_poi_brand,
            customTextPrefixId = R.string.poi_operator,
            checkBaseKey = true
        ),
        PoiAdditionalUiRule(
            key = Amenity.BRAND,
            customIconId = R.drawable.ic_action_poi_brand,
            customTextPrefixId = R.string.poi_brand,
            checkBaseKey = true
        ),
        PoiAdditionalUiRule(
            key = Amenity.POPULATION,
            isNeedLinks = false,
            behavior = PopulationRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "internet_access_fee_yes",
            customIconId = R.drawable.ic_action_internet_access_fee
        ),
        PoiAdditionalUiRule(
            key = "instagram",
            customIconId = R.drawable.ic_action_social_instagram
        ),
        PoiAdditionalUiRule(
            key = Amenity.HEIGHT,
            customTextPrefixId = R.string.shared_string_height,
            isNeedLinks = false,
            behavior = MetricRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = Amenity.WIDTH,
            customTextPrefixId = R.string.shared_string_width,
            isNeedLinks = false,
            behavior = MetricRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "depth",
            behavior = MetricRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "seamark_height",
            behavior = MetricRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = Amenity.DISTANCE,
            customTextPrefixId = R.string.distance,
            isNeedLinks = false,
            behavior = DistanceRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "capacity",
            behavior = LiquidCapacityRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "maxweight",
            behavior = MaxWeightRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "students",
            behavior = CapacityRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "spots",
            behavior = CapacityRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "seats",
            behavior = CapacityRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "us_maps_recreation_area",
            behavior = UsMapsRecreationAreaRowBehaviour
        ),
        PoiAdditionalUiRule(
            key = "ele",
            behavior = EleRowBehaviour
        ),
    )

    private val RULES_BY_KEY: Map<String, PoiAdditionalUiRule> = RULES.associateBy { it.key }

    fun findRule(key: String): PoiAdditionalUiRule {
        val baseKey = key.substringBefore(":")

        // First, try exact match
        RULES_BY_KEY[key]?.let { return it }

        for (rule in RULES) {
            // Decide whether to use baseKey or full key based on the rule
            val keyToSearch = if (rule.checkBaseKey) baseKey else key

            // If the rule requires "contains" match
            if (rule.checkKeyOnContains) {
                if (keyToSearch.contains(rule.key)) {
                    return rule
                }
            } else {
                // Else check for exact match
                if (keyToSearch == rule.key) {
                    return rule
                }
            }
        }

        // If no match found, return a default rule with the given key
        return PoiAdditionalUiRule(key = key)
    }
}