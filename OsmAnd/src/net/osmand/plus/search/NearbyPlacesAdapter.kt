package net.osmand.plus.search

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiContext
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import net.osmand.Location
import net.osmand.data.Amenity
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.PicassoUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.utils.UpdateLocationUtils
import net.osmand.plus.wikipedia.WikipediaPlugin
import net.osmand.util.Algorithms

class NearbyPlacesAdapter(
	@UiContext val context: Context,
	var items: List<Amenity>,
	private var isVertical: Boolean,
	private val onItemClickListener: NearbyItemClickListener
) : RecyclerView.Adapter<NearbyPlacesAdapter.NearbyViewHolder>() {

	interface NearbyItemClickListener {
		fun onNearbyItemClicked(amenity: Amenity)
	}

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

	private fun isNightMode(): Boolean {
		val app = context.applicationContext as OsmandApplication
		return !app.getSettings().isLightContent
	}

	override fun onBindViewHolder(holder: NearbyViewHolder, position: Int) {
		val item = items[position]
		holder.bind(item, position)
	}

	override fun getItemCount(): Int = items.size

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
		private val descriptionTextView: TextView? = itemView.findViewById(R.id.item_description)
		private val itemTypeTextView: TextView = itemView.findViewById(R.id.item_type)
		private val distanceTextView: TextView? = itemView.findViewById(R.id.distance)
		private val arrowImageView: ImageView? = itemView.findViewById(R.id.direction)

		fun bind(item: Amenity, position: Int) {
			this.item = item
			val layoutParams = itemView.layoutParams
			val heightResId =
				if (shouldShowImage()) R.dimen.nearby_place_item_height else R.dimen.nearby_place_item_height_non_image
			layoutParams.height = context.resources.getDimensionPixelSize(heightResId)
			val app = imageView.context.applicationContext as OsmandApplication
			val poiTypes = app.poiTypes
			val osmanPoiType = item.osmandPoiKey
			val itemType = osmanPoiType ?: item.subType
			val subType = poiTypes.getPoiTypeByKey(itemType)
			val poiIcon =
				if (subType == null) null else RenderingIcons.getBigIcon(app, subType.keyName)
			val uiUtilities = app.uiUtilities
			val nightMode = app.daynightHelper.isNightMode
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
			if (shouldShowImage()) {
				AndroidUiHelper.updateVisibility(imageViewContainer, true)
				val picasso = PicassoUtils.getPicasso(app)
				if (!Algorithms.objectEquals(imageView.tag, item.wikiImageStubUrl)) {
					imageView.tag = item.wikiImageStubUrl
					item.wikiImageStubUrl?.let {
						val creator = Picasso.get()
							.load(it)
						if (coloredIcon != null) {
							if (coloredIcon != null) {
								creator.error(coloredIcon)
							}
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

			// Add row number to the title
			titleTextView.text = "${position + 1}. ${item.name}"

			descriptionTextView?.text = item.getDescription(null)
			descriptionTextView?.let {
				AndroidUiHelper.updateVisibility(it, !Algorithms.isEmpty(item.getDescription(null)))
			}

			itemTypeTextView.text = subType?.translation ?: ""

			// Calculate distance and show arrow
			if (distanceTextView != null && arrowImageView != null) {
				val distance = calculateDistance(app, item, location)
				if (distance != null) {
					distanceTextView.text = OsmAndFormatter.getFormattedDistance(distance, app)
					distanceTextView.visibility = View.VISIBLE
					arrowImageView.visibility = View.VISIBLE

					// Update compass icon rotation
					val latLon = LatLon(item.location.latitude, item.location.longitude)
					UpdateLocationUtils.updateLocationView(
						app,
						updateLocationViewCache,
						arrowImageView,
						distanceTextView,
						latLon)
				} else {
					distanceTextView.visibility = View.GONE
					arrowImageView.visibility = View.GONE
				}
			}
			if (!itemView.hasOnClickListeners()) {
				itemView.setOnClickListener(clickListener)
			}
		}

		private val clickListener =
			OnClickListener { item?.let { onItemClickListener.onNearbyItemClicked(it) } }

		private fun calculateDistance(
			app: OsmandApplication,
			item: Amenity,
			location: Location?): Float? {
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