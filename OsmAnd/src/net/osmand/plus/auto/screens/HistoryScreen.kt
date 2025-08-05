package net.osmand.plus.auto.screens

import android.os.AsyncTask
import android.text.SpannableString
import android.text.Spanned
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.plus.shared.SharedUtil
import net.osmand.data.LatLon
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.R
import net.osmand.plus.auto.TripUtils
import net.osmand.plus.search.history.SearchHistoryHelper
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI
import net.osmand.plus.search.listitems.QuickSearchListItem
import net.osmand.plus.track.data.GPXInfo
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchResult
import net.osmand.shared.extensions.kFile
import net.osmand.shared.gpx.GpxDataItem
import net.osmand.shared.gpx.GpxDbHelper
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils

class HistoryScreen(
    carContext: CarContext,
    private val settingsAction: Action) : BaseAndroidAutoScreen(carContext) {
	private lateinit var updateItemsTask: UpdateHistoryItemsTask
	private lateinit var searchItems: ArrayList<QuickSearchListItem>
	val gpxDbHelper: GpxDbHelper = app.gpxDbHelper

	override fun onFirstGetTemplate() {
		super.onFirstGetTemplate()
		updateItemsTask = UpdateHistoryItemsTask()
		OsmAndTaskManager.executeTask(updateItemsTask)
	}

	private inner class UpdateHistoryItemsTask : AsyncTask<Unit, Unit, Unit>() {
		override fun doInBackground(vararg params: Unit?) {
			prepareHistoryItems()
		}

		override fun onPostExecute(result: Unit?) {
			invalidate()
		}
	}


    override fun getTemplate(): Template {
        val templateBuilder = ListTemplate.Builder()
        val app = app
	    val isLoading = updateItemsTask.status != AsyncTask.Status.FINISHED
	    if (!isLoading) {
		    prepareList(templateBuilder)
	    }
        val actionStripBuilder = ActionStrip.Builder()
        actionStripBuilder.addAction(
            Action.Builder()
                .setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext, R.drawable.ic_action_search_dark)).build())
                .setOnClickListener { openSearch() }
                .build())
        return templateBuilder
	        .setLoading(isLoading)
            .setTitle(app.getString(R.string.shared_string_history))
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStripBuilder.build())
            .build()
    }

	private fun prepareHistoryItems() {
		val historyHelper = app.getSearchHistoryHelper()
		val results = historyHelper.getHistoryEntries(true)
		val resultsSize = results.size
		searchItems = ArrayList()
		val limitedResults = results.subList(0, resultsSize.coerceAtMost(contentLimit - 1))
		for (result in limitedResults) {
			val searchResult =
				SearchHistoryAPI.createSearchResult(app, result, SearchPhrase.emptyPhrase())
			val listItem = QuickSearchListItem(app, searchResult)
			if (listItem.searchResult.objectType == ObjectType.GPX_TRACK && listItem.searchResult.location == null) {
				val gpxInfo = listItem.searchResult.relatedObject as GPXInfo
				val gpxFile = gpxInfo.gpxFile
				if (gpxFile == null) {
					gpxInfo.file?.let { file ->
						val item = gpxDbHelper.getItem(file.kFile()) { updateSearchResult(listItem.searchResult, it) }
						if (item != null) {
							updateSearchResult(listItem.searchResult, item)
						}
					}
				} else {
					val latLonStart = gpxFile.getAnalysis(0).getLatLonStart()
					listItem.searchResult.location = if (latLonStart != null) {
						SharedUtil.jLatLon(latLonStart)
					} else {
						null
					}
				}
			}
			searchItems.add(listItem)
		}
	}

	private fun updateSearchResult(searchResult: SearchResult, dataItem: GpxDataItem) {
		val latLonStart = dataItem.getAnalysis()?.getLatLonStart()
		searchResult.location = if (latLonStart != null) {
			SharedUtil.jLatLon(latLonStart)
		} else {
			null
		}
	}

	private fun prepareList(templateBuilder: ListTemplate.Builder) {
		val listBuilder = ItemList.Builder()
		val location = app.mapViewTrackingUtilities.defaultLocation
		for (item in searchItems) {
			if (item.searchResult?.`object` is HistoryEntry) {
				val result = item.searchResult?.`object` as HistoryEntry
				val pointDescription = result.name
				var title = item.name
				val icon = CarIcon.Builder(
					IconCompat.createWithResource(app, pointDescription.itemIcon)).build()
				if (Algorithms.isEmpty(title)) {
					title = item.searchResult?.location?.toString()
				}
				val rowBuilder = Row.Builder()
					.setTitle(title)
					.setImage(icon)
					.setOnClickListener { onClickHistoryItem(item) }
				val dist = if (item.searchResult.location == null) {
					0.0
				} else {
					val startLocation = item.searchResult.location
					rowBuilder.setMetadata(
						Metadata.Builder().setPlace(
							Place.Builder(
								CarLocation.create(
									startLocation.latitude,
									startLocation.longitude)).build()).build())
					MapUtils.getDistance(
						startLocation.latitude, startLocation.longitude,
						location.latitude, location.longitude)
				}
				val address = SpannableString(" ")
				val distanceSpan = DistanceSpan.create(TripUtils.getDistance(app, dist))
				address.setSpan(distanceSpan, 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
				rowBuilder.addText(address)
				listBuilder.addItem(rowBuilder.build())
			}
		}
		templateBuilder.setSingleList(listBuilder.build())
	}

    override fun getConstraintLimitType(): Int {
        return ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST
    }

    private fun onClickHistoryItem(historyItem: QuickSearchListItem) {
        val result = SearchResult()
	    if (historyItem.searchResult.location != null) {
		    result.location = LatLon(
			    historyItem.searchResult.location.latitude,
			    historyItem.searchResult.location.longitude)
	    }
        result.objectType = ObjectType.RECENT_OBJ
        result.`object` = historyItem.searchResult.`object`
        openRoutePreview(settingsAction, result)
    }

    private fun openSearch() {
        screenManager.pushForResult(
	        SearchScreen(
		        carContext,
		        settingsAction
	        )
        ) { }
    }
}