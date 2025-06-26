package net.osmand.plus.auto.screens

import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.PlatformUtil
import net.osmand.plus.AppInitializeListener
import net.osmand.plus.AppInitializer
import net.osmand.plus.R
import net.osmand.plus.search.listitems.QuickSearchListItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.search.core.SearchCoreFactory.SearchAmenityTypesAPI
import net.osmand.search.core.SearchResult
import net.osmand.util.Algorithms
import org.apache.commons.logging.Log
import java.io.IOException

class POICategoriesScreen(
	carContext: CarContext,
	private val settingsAction: Action
) : BaseAndroidAutoScreen(carContext) {

	companion object {
		private val log: Log = PlatformUtil.getLog("POICategoriesScreen")
	}

	private var categories: MutableList<SearchResult> = mutableListOf()

	private var loading = false
	private var destroyed = false

    init {
        reloadCategories()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                recenterMap()
            }
        })
    }

    private var selectedCategoryResult: SearchResult? = null

	override fun getTemplate(): Template {
		val templateBuilder = PlaceListNavigationTemplate.Builder()
		if (!loading) {
			val listBuilder = ItemList.Builder()
			setupPOICategories(listBuilder)
			templateBuilder.setItemList(listBuilder.build())
		}
		return templateBuilder
			.setLoading(loading)
			.setTitle(app.getString(R.string.poi_categories))
			.setActionStrip(ActionStrip.Builder().addAction(createSearchAction()).build())
			.setHeaderAction(Action.BACK)
			.build()
	}

	private fun setupPOICategories(listBuilder: ItemList.Builder) {
		val categoriesSize = categories.size
		val limitedCategories = categories.subList(0, categoriesSize.coerceAtMost(contentLimit - 1))

		for (result in limitedCategories) {
			var title = QuickSearchListItem.getName(app, result);
			if (Algorithms.isEmpty(title)) {
				title = QuickSearchListItem.getTypeName(app, result);
			}
			var groupIcon = QuickSearchListItem.getIcon(app, result)
			if (groupIcon == null) {
				groupIcon = app.uiUtilities.getIcon(R.drawable.mx_special_custom_category)
			}
			val icon = CarIcon.Builder(
				IconCompat.createWithBitmap(AndroidUtils.drawableToBitmap(groupIcon))
			).build()
			listBuilder.addItem(
				Row.Builder()
					.setTitle(title)
					.setImage(icon)
					.setBrowsable(true)
					.setOnClickListener { onClickPOICategory(result) }
					.build())
		}
	}

	private fun onClickPOICategory(category: SearchResult) {
		selectedCategoryResult = category
		Handler(Looper.getMainLooper()).post {
			screenManager.push(POIScreen(carContext, settingsAction, category))
		}
	}

	fun reloadCategories() {
		if (app.isApplicationInitializing) {
			loading = true;
			app.appInitializer.addListener(object : AppInitializeListener {
				override fun onFinish(init: AppInitializer) {
					init.removeListener(this)
					loading = false;
					if (!destroyed) {
						reloadCategoriesInternal()
						invalidate();
					}
				}
			})
		} else {
			reloadCategoriesInternal()
		}
	}

	private fun reloadCategoriesInternal() {
		try {
			val core = app.searchUICore.core
			val res = core.shallowSearch(SearchAmenityTypesAPI::class.java, "", null)
			if (res != null) {
				val routesCategory = app.poiTypes.routes
				categories.addAll(res.currentSearchResults.filter { it.`object` != routesCategory })
			}
		} catch (e: IOException) {
			log.error(e)
		}
	}

	override fun onDestroy(owner: LifecycleOwner) {
		super.onDestroy(owner)
		lifecycle.removeObserver(this)
		destroyed = true
	}
}