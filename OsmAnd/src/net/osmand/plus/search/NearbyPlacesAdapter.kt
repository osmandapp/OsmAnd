package net.osmand.plus.search

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.data.ExploreTopPlacePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.PicassoUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.utils.UpdateLocationUtils
import net.osmand.util.Algorithms

class NearbyPlacesAdapter(
	val ctx: Context,
	var items: List<ExploreTopPlacePoint>,
	private var isVertical: Boolean,
	private val onItemClickListener: NearbyItemClickListener
) : RecyclerView.Adapter<NearbyPlacesAdapter.NearbyViewHolder>() {

	interface NearbyItemClickListener {
		fun onNearbyItemClicked(item: ExploreTopPlacePoint)
	}

	// Initialize the UpdateLocationViewCache
	private val updateLocationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(ctx)

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
		return !(ctx.applicationContext as OsmandApplication).getSettings().isLightContent
	}

	override fun onBindViewHolder(holder: NearbyViewHolder, position: Int) {
		val item = items[position]
		holder.bind(item, onItemClickListener, position)
	}

	override fun getItemCount(): Int = items.size

	class NearbyViewHolder(
		itemView: View,
		private val updateLocationViewCache: UpdateLocationUtils.UpdateLocationViewCache
	) : RecyclerView.ViewHolder(itemView) {
		private val imageView: ImageView = itemView.findViewById(R.id.item_image)
		private val iconImageView: ImageView = itemView.findViewById(R.id.item_icon)
		private val titleTextView: TextView = itemView.findViewById(R.id.item_title)
		private val descriptionTextView: TextView? = itemView.findViewById(R.id.item_description)
		private val itemTypeTextView: TextView = itemView.findViewById(R.id.item_type)
		private val distanceTextView: TextView? = itemView.findViewById(R.id.distance)
		private val arrowImageView: ImageView? = itemView.findViewById(R.id.direction)

		fun bind(item: ExploreTopPlacePoint, onItemClickListener: NearbyItemClickListener, position: Int) {
			val app = imageView.context.applicationContext as OsmandApplication
			val poiTypes = app.poiTypes
			val subType = poiTypes.getPoiTypeByKey(item.poisubtype)
			val poiIcon = RenderingIcons.getBigIcon(app, subType.keyName)
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
			val picasso = PicassoUtils.getPicasso(app)

			item.imageStubUrl?.let {
				val creator = Picasso.get()
					.load(it)
				if (coloredIcon != null) {
					creator.error(coloredIcon)
				}
				creator.into(imageView, object : Callback {
					override fun onSuccess() {
						picasso.setResultLoaded(it, true)
					}

					override fun onError(e: Exception?) {
						picasso.setResultLoaded(it, true)
					}
				})
			}

			// Add row number to the title
			titleTextView.text = "${position + 1}. ${item.wikiTitle}"

			descriptionTextView?.text = item.wikiDesc
			descriptionTextView?.let {
				AndroidUiHelper.updateVisibility(it, !Algorithms.isEmpty(item.wikiDesc))
			}

			itemTypeTextView.text = subType.translation

			// Calculate distance and show arrow
			if (distanceTextView != null && arrowImageView != null) {
				val distance = calculateDistance(app, item)
				if (distance != null) {
					distanceTextView.text = OsmAndFormatter.getFormattedDistance(distance, app)
					distanceTextView.visibility = View.VISIBLE
					arrowImageView.visibility = View.VISIBLE

					// Update compass icon rotation
					val latLon = LatLon(item.latitude, item.longitude)
					UpdateLocationUtils.updateLocationView(app, updateLocationViewCache, arrowImageView, distanceTextView, latLon)
				} else {
					distanceTextView.visibility = View.GONE
					arrowImageView.visibility = View.GONE
				}
			}

			itemView.setOnClickListener { onItemClickListener.onNearbyItemClicked(item) }
		}

		private fun calculateDistance(app: OsmandApplication, item: ExploreTopPlacePoint): Float? {
			val currentLocation = app.locationProvider?.lastKnownLocation
			if (currentLocation != null) {
				val results = FloatArray(1)
				Location.distanceBetween(
					currentLocation.latitude,
					currentLocation.longitude,
					item.latitude,
					item.longitude,
					results
				)
				return results[0]
			}
			return null
		}
	}
}