package net.osmand.plus.search

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiContext
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import net.osmand.Location
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.helpers.LocaleHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.PicassoUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.utils.UpdateLocationUtils
import net.osmand.plus.wikipedia.WikipediaPlugin
import net.osmand.util.Algorithms
import kotlin.math.ceil

class NearbyPlacesAdapter(
	@UiContext val context: Context,
	var items: List<Amenity>,
	private var isVertical: Boolean,
	private val onItemClickListener: NearbyItemClickListener
) : RecyclerView.Adapter<NearbyPlacesAdapter.NearbyViewHolder>() {

	interface NearbyItemClickListener {
		fun onNearbyItemClicked(amenity: Amenity)
	}

	var isLoading = false
	val locale = LocaleHelper.getPreferredPlacesLanguage(getApp())
	val transliterate = getApp().getSettings().MAP_TRANSLITERATE_NAMES.get()

	// Initialize the UpdateLocationViewCache
	private val updateLocationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(context)
	private var location: Location? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NearbyViewHolder {
		val inflater = UiUtilities.getInflater(parent.context, isNightMode())
		val view = inflater.inflate(
			if (isVertical) R.layout.search_nearby_item_vertical else R.layout.search_nearby_item,
			parent,
			false
		)
		return NearbyViewHolder(view, updateLocationViewCache)
	}

	private fun getApp(): OsmandApplication {
		return context.applicationContext as OsmandApplication
	}

	private fun isNightMode(): Boolean {
		return getApp().daynightHelper.isNightMode(ThemeUsageContext.APP)
	}

	override fun onBindViewHolder(holder: NearbyViewHolder, position: Int) {
		if (!isLoading) {
			val item = items[position]
			holder.bind(item, position)
		}
	}

	override fun getItemCount(): Int = if (isLoading) 5 else items.size

	fun hasData(): Boolean = items.isNotEmpty()

	fun updateLocation(location: Location?) {
		this.location = location
		notifyDataSetChanged()
	}

	inner class NearbyViewHolder(
		itemView: View,
		private val updateLocationViewCache: UpdateLocationUtils.UpdateLocationViewCache
	) : RecyclerView.ViewHolder(itemView) {
		private var item: Amenity? = null
		private val imageView: ImageView = itemView.findViewById(R.id.item_image)
		private val imageViewContainer: ViewGroup = itemView.findViewById(R.id.item_image_container)
		private val errorImageView: ImageView = itemView.findViewById(R.id.item_image_error)
		private val iconImageView: ImageView = itemView.findViewById(R.id.item_icon)
		private val titleTextView: TextView = itemView.findViewById(R.id.item_title)
		private val itemTypeContainer: View? = itemView.findViewById(R.id.item_type_container)
		private val descriptionTextView: TextView? = itemView.findViewById(R.id.item_description)
		private val itemTypeTextView: TextView = itemView.findViewById(R.id.item_type)
		private val distanceTextView: TextView? = itemView.findViewById(R.id.distance)
		private val arrowImageView: ImageView? = itemView.findViewById(R.id.direction)
		private val maxHeight = calculateMaxItemHeight(context)

		fun bind(item: Amenity, position: Int) {
			this.item = item
			val layoutParams = itemView.layoutParams
			if (shouldShowImage()) {
				layoutParams.height =
					if (isVertical)
						context.resources.getDimensionPixelSize(R.dimen.nearby_place_item_height)
					else maxHeight
			} else {
				layoutParams.height =
					context.resources.getDimensionPixelSize(R.dimen.nearby_place_item_height_non_image)
			}
			val app = imageView.context.applicationContext as OsmandApplication
			val poiTypes = app.poiTypes
			val osmanPoiType = item.osmandPoiKey
			val itemType = osmanPoiType ?: item.subType
			val subType = poiTypes.getPoiTypeByKey(itemType)
			val poiIcon =
				if (subType == null) null else RenderingIcons.getBigIcon(app, subType.keyName)
			val uiUtilities = app.uiUtilities
			val nightMode = app.daynightHelper.isNightMode(ThemeUsageContext.MAP)
			val coloredIcon = if (poiIcon != null) {
				uiUtilities.getRenderingIcon(
					app,
					subType.keyName,
					nightMode
				)
			} else {
				uiUtilities.getIcon(R.drawable.ic_action_info_dark, nightMode)
			}
			iconImageView.setImageDrawable(coloredIcon)
			errorImageView.setImageDrawable(coloredIcon)
			AndroidUiHelper.updateVisibility(errorImageView, true)

			if (shouldShowImage()) {
				AndroidUiHelper.updateVisibility(imageViewContainer, true)
				val picasso = PicassoUtils.getPicasso(app)
				if (!Algorithms.objectEquals(imageView.tag, item.wikiImageStubUrl)) {
					imageView.tag = item.wikiImageStubUrl
					item.wikiImageStubUrl?.let {
						val creator = Picasso.get()
							.load(it)
						if (coloredIcon != null) {
							creator.error(coloredIcon)
						}
						creator.into(imageView, object : Callback {
							override fun onSuccess() {
								AndroidUiHelper.updateVisibility(imageView, true)
								AndroidUiHelper.updateVisibility(errorImageView, false)
								picasso.setResultLoaded(it, true)
							}

							override fun onError(e: Exception?) {
								AndroidUiHelper.updateVisibility(imageView, false)
								AndroidUiHelper.updateVisibility(errorImageView, true)
								picasso.setResultLoaded(it, false)
							}
						})
					}
				}
			} else {
				AndroidUiHelper.updateVisibility(imageViewContainer, false)
			}

			itemTypeContainer?.let {
				it.setBackgroundResource(0)
				it.alpha = 1f
			}
			titleTextView.setBackgroundResource(0)
			titleTextView.alpha = 1f
			titleTextView.text = "${position + 1}. ${item.getName(locale, transliterate)}"

			descriptionTextView?.text = item.getDescription(locale)
			descriptionTextView?.let {
				AndroidUiHelper.updateVisibility(it, !Algorithms.isEmpty(item.getDescription(null)))
			}

			itemTypeTextView.text = subType?.translation ?: ""

			// Calculate distance and show arrow
			if (distanceTextView != null && arrowImageView != null) {
				val distance = calculateDistance(item, location)
				val hasDistance = distance != null
				if (hasDistance) {
					distanceTextView.text = OsmAndFormatter.getFormattedDistance(distance, app)

					// Update compass icon rotation
					val latLon = LatLon(item.location.latitude, item.location.longitude)
					UpdateLocationUtils.updateLocationView(
						app,
						updateLocationViewCache,
						arrowImageView,
						distanceTextView,
						latLon
					)
				}
				AndroidUiHelper.updateVisibility(arrowImageView, hasDistance)
				AndroidUiHelper.updateVisibility(distanceTextView, hasDistance)
			}
			if (!itemView.hasOnClickListeners()) {
				itemView.setOnClickListener(clickListener)
			}
		}

		private fun calculateMaxItemHeight(context: Context): Int {
			val res: Resources = context.resources
			val metrics: DisplayMetrics = res.displayMetrics

			val padding: Float = res.getDimension(R.dimen.content_padding) * 2
			val imageHeight: Float = res.getDimension(R.dimen.nearby_place_image_height)

			val lineHeight: Int = AndroidUtils.getTextHeight(titleTextView.paint)
			val twoLinesTitleHeight = lineHeight * 2
			val marginTopTitle: Float = 9 * metrics.density

			val typeLineHeight: Int = AndroidUtils.getTextHeight(itemTypeTextView.paint)
			val marginTopType: Float = 3 * metrics.density

			val totalHeight =
				imageHeight + padding + marginTopTitle + twoLinesTitleHeight + marginTopType + typeLineHeight

			return ceil(totalHeight.toDouble()).toInt()
		}

		private val clickListener =
			OnClickListener { item?.let { onItemClickListener.onNearbyItemClicked(it) } }

		private fun calculateDistance(item: Amenity, location: Location?): Float? {
			if (location != null) {
				val results = FloatArray(1)
				Location.distanceBetween(
					location.latitude,
					location.longitude,
					item.location.latitude,
					item.location.longitude,
					results
				)
				return results[0]
			}
			return null
		}
	}

	private fun shouldShowImage(): Boolean {
		val plugin = PluginsHelper.getPlugin(WikipediaPlugin::class.java)
		return plugin?.topWikiPoiFilter?.showLayoutWithImages() == true
	}

}