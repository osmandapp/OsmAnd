package net.osmand.plus.auto

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.car.app.CarContext
import androidx.car.app.HostException
import androidx.car.app.model.CarIcon
import androidx.car.app.notification.CarPendingIntent
import androidx.car.app.suggestion.SuggestionManager
import androidx.car.app.suggestion.model.Suggestion
import androidx.car.app.versioning.CarAppApiLevels
import androidx.core.graphics.drawable.IconCompat
import net.osmand.PlatformUtil
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmandApplication
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.search.listitems.QuickSearchListItem
import net.osmand.plus.settings.enums.HistorySource
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchResult
import org.apache.commons.logging.Log

class CarSuggestionsHelper(private val app: OsmandApplication) {

	companion object {
		const val EXTRA_SUGGESTION_POINT = "net.osmand.plus.auto.EXTRA_SUGGESTION_POINT"
		const val EXTRA_SUGGESTION_LAT = "net.osmand.plus.auto.EXTRA_SUGGESTION_LAT"
		const val EXTRA_SUGGESTION_LON = "net.osmand.plus.auto.EXTRA_SUGGESTION_LON"
		private const val MAX_SUGGESTIONS = 5
		private val LOG: Log = PlatformUtil.getLog(CarSuggestionsHelper::class.java)

		@JvmStatic
		fun createCardIntent(context: Context, entry: HistoryEntry, title: String): Intent {
			val serializedName = PointDescription.serializeToString(entry.name)
			return Intent(CarContext.ACTION_NAVIGATE)
				.setComponent(ComponentName(context, NavigationCarAppService::class.java))
				.setData(Uri.parse("geo:${entry.lat},${entry.lon}?q=${entry.lat},${entry.lon}(${Uri.encode(title)})"))
				.putExtra(EXTRA_SUGGESTION_POINT, serializedName)
				.putExtra(EXTRA_SUGGESTION_LAT, entry.lat)
				.putExtra(EXTRA_SUGGESTION_LON, entry.lon)
		}

		@JvmStatic
		fun getSuggestionLatLon(intent: Intent): LatLon? {
			if (!intent.hasExtra(EXTRA_SUGGESTION_LAT) || !intent.hasExtra(EXTRA_SUGGESTION_LON)) {
				return null
			}
			return LatLon(intent.getDoubleExtra(EXTRA_SUGGESTION_LAT, 0.0), intent.getDoubleExtra(EXTRA_SUGGESTION_LON, 0.0))
		}
	}

	private class Card(val key: String, val suggestion: Suggestion)

	private var lastPublishedKeys: List<String>? = null

	fun update(carContext: CarContext) {
		if (carContext.carAppApiLevel < CarAppApiLevels.LEVEL_5) {
			return
		}
		val routingHelper = app.routingHelper
		if (!InAppPurchaseUtils.isAndroidAutoAvailable(app)
			|| !OsmAndLocationProvider.isLocationPermissionAvailable(app)
			|| routingHelper.isFollowingMode
			|| routingHelper.isPauseNavigation
		) {
			publish(carContext, emptyList())
		} else {
			publish(carContext, buildCards(carContext))
		}
	}

	@VisibleForTesting
	internal fun getSuggestionEntries(): List<HistoryEntry> {
		return app.searchHistoryHelper.getVisibleHistoryEntries(HistorySource.NAVIGATION, true, false)
			.filter { entry ->
				!entry.name.isGpxFile && !(entry.lat == 0.0 && entry.lon == 0.0)
			}
			.sortedByDescending { entry ->
				entry.lastAccessTime
			}
	}

	private fun buildCards(carContext: CarContext): List<Card> {
		val visibleEntries = getSuggestionEntries()
		val cards = ArrayList<Card>()
		val phrase = SearchPhrase.emptyPhrase(app.searchUICore.core.searchSettings)
		for (entry in visibleEntries) {
			if (cards.size >= MAX_SUGGESTIONS) {
				break
			}
			try {
				val pointDescription = entry.name
				val searchResult = SearchHistoryAPI.createSearchResult(app, entry, phrase)
				var title = searchResult.localeName
				if (title.isNullOrEmpty()) {
					title = PointDescription.getLocationName(app, entry.lat, entry.lon, true)
				}
				val subtitle = if (!entry.address.isNullOrEmpty()) {
					entry.address
				} else if (!entry.typeName.isNullOrEmpty()) {
					entry.typeName
				} else {
					""
				}
				val iconId = getIconId(entry, searchResult)
				val icon = CarIcon.Builder(IconCompat.createWithResource(app, iconId)).build()
				val id = "${entry.lat},${entry.lon},${PointDescription.serializeToString(pointDescription)}"
				val intent = createCardIntent(app, entry, title)
				val action = CarPendingIntent.getCarApp(carContext, cards.size, intent, PendingIntent.FLAG_UPDATE_CURRENT)
				val suggestion = Suggestion.Builder()
					.setIdentifier(id)
					.setTitle(title)
					.setSubtitle(subtitle)
					.setIcon(icon)
					.setAction(action)
					.build()
				cards.add(Card("$id|$title|$subtitle|$iconId", suggestion))
			} catch (e: Exception) {
				LOG.error("Failed to build suggestion for entry: $entry", e)
			}
		}
		return cards
	}

	@DrawableRes
	@VisibleForTesting
	internal fun getIconId(entry: HistoryEntry, searchResult: SearchResult): Int {
		val pointDescription = entry.name
		val searchObject = searchResult.`object`
		val iconName = if (!pointDescription.iconName.isNullOrEmpty()) {
			pointDescription.iconName
		} else if (searchObject is Amenity) {
			QuickSearchListItem.getAmenityIconName(app, searchObject)
		} else {
			QuickSearchListItem.getAddressIconName(searchResult)
		}
		val iconId = QuickSearchListItem.getIconIdByName(app, iconName)
		return if (iconId > 0) {
			iconId
		} else {
			pointDescription.itemIcon
		}
	}

	private fun publish(carContext: CarContext, cards: List<Card>) {
		val keys = cards.map { card ->
			card.key
		}
		if (keys == lastPublishedKeys) {
			return
		}
		try {
			val suggestions = cards.map { card ->
				card.suggestion
			}
			carContext.getCarService(SuggestionManager::class.java).updateSuggestions(suggestions)
			lastPublishedKeys = keys
		} catch (e: HostException) {
			LOG.error("Failed to update suggestions: host exception", e)
		} catch (e: SecurityException) {
			LOG.error("Failed to update suggestions: security exception", e)
		} catch (e: IllegalArgumentException) {
			LOG.error("Failed to update suggestions: illegal argument exception", e)
		}
	}
}
