package net.osmand.plus.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.graphics.drawable.IconCompat
import net.osmand.data.LatLon
import net.osmand.plus.R
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.parking.ParkingPositionPlugin
import net.osmand.plus.poi.PoiFiltersHelper
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.search.SearchUICore
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchCoreFactory
import net.osmand.search.core.SearchResult

class DestinationReachedScreen(carContext: CarContext) : BaseAndroidAutoScreen(carContext) {

	override fun getTemplate(): Template {
		return MapWithContentTemplate.Builder()
			.setActionStrip(
				createSearchAction().let { searchAction ->
					ActionStrip.Builder()
						.addAction(searchAction)
						.addAction(
							Action.Builder()
								.setIcon(
									CarIcon.Builder(
										IconCompat.createWithResource(
											carContext,
											R.drawable.ic_action_search_dark)
									).build())
								.setOnClickListener { recenterMap() }
								.build()
						)
						.build()
				}
			)
			.setContentTemplate(createArrivalListTemplate())
			.build()
	}

	private fun createArrivalListTemplate(): ListTemplate {
		val headerBuilder = Header.Builder()
		headerBuilder.setTitle(app.getString(R.string.arrived_at_destination))
		headerBuilder.setStartHeaderAction(Action.BACK)

		val itemListBuilder = ItemList.Builder()
		val plugin = PluginsHelper.getActivePlugin(ParkingPositionPlugin::class.java)
		plugin?.let {
			itemListBuilder.addItem(
				Row.Builder()
					.setTitle(CarText.create(app.getString(R.string.context_menu_item_add_parking_point)))
					.setImage(
						CarIcon.Builder(
							IconCompat.createWithResource(
								carContext,
								R.drawable.ic_action_parking_location)
						).build(), Row.IMAGE_TYPE_ICON
					)
					.setOnClickListener {
						val location = app.locationProvider.lastKnownLocation
						location?.let {
							screenManager.push(
								ConfirmScreen(
									carContext,
									app.getString(R.string.quick_action_parking_place),
									app.getString(R.string.is_parking_time_limited),
									onConfirm = {
										plugin.setParkingPosition(it.latitude, it.longitude)
										plugin.setParkingType(true)
										app.osmandMap.mapView.refreshMap()
										app.favoritesHelper.setParkingPoint(
											plugin.parkingPosition,
											null,
											plugin.parkingTime,
											plugin.isParkingEventAdded);
									},
									onCancel = {
										plugin.setParkingPosition(it.latitude, it.longitude)
										plugin.setParkingType(false)
										app.osmandMap.mapView.refreshMap()
										app.favoritesHelper.setParkingPoint(
											plugin.parkingPosition,
											null,
											plugin.parkingTime,
											plugin.isParkingEventAdded);
									}
								)
							)
						}
						finish()
					}
					.build()
			)
		}

		itemListBuilder.addItem(
			Row.Builder()
				.setTitle(CarText.create(app.getString(R.string.find_parking)))
				.setImage(
					CarIcon.Builder(
						IconCompat.createWithResource(
							carContext,
							R.drawable.ic_action_parking_dark)
					).build(), Row.IMAGE_TYPE_ICON
				)
				.setOnClickListener {
					app.carNavigationSession?.let { session ->
						val helper: PoiFiltersHelper = app.poiFilters
						val parkingFilter = helper.getFilterById(PoiUIFilter.STD_PREFIX + "parking")
						val objectLocalizedName = parkingFilter.getName()
						val searchUICore: SearchUICore = app.searchUICore.core
						val phrase = searchUICore.resetPhrase()
						val sr = SearchResult(phrase)
						sr.localeName = objectLocalizedName
						sr.`object` = parkingFilter
						sr.priority = SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY.toDouble()
						sr.priorityDistance = 0.0
						sr.objectType = ObjectType.POI_TYPE
						val screenManager = screenManager
						screenManager.popToRoot()
						screenManager.push(POIScreen(carContext, session.settingsAction, sr))
					}
					app.stopNavigation()
					finish()
				}
				.build()
		)

		itemListBuilder.addItem(
			Row.Builder()
				.setTitle(CarText.create(app.getString(R.string.recalculate_route)))
				.setImage(
					CarIcon.Builder(
						IconCompat.createWithResource(
							carContext,
							R.drawable.ic_action_gdirections_dark)
					).build(), Row.IMAGE_TYPE_ICON
				)
				.setOnClickListener {
					val mapActions = app.osmandMap.mapActions
					mapActions.recalculateRoute(false)
					mapActions.startNavigation()
					finish()
				}
				.build()
		)

		itemListBuilder.addItem(
			Row.Builder()
				.setTitle(CarText.create(app.getString(R.string.finish_navigation)))
				.setImage(
					CarIcon.Builder(
						IconCompat.createWithResource(
							carContext,
							R.drawable.ic_action_finish_navigation)
					).build(), Row.IMAGE_TYPE_ICON
				)
				.setOnClickListener {
					app.stopNavigation()
					screenManager.popToRoot()
				}
				.build()
		)

		val arrivalListBuilder = ListTemplate.Builder()
			.setHeader(headerBuilder.build())
			.setSingleList(itemListBuilder.build())

		return arrivalListBuilder.build()
	}
}