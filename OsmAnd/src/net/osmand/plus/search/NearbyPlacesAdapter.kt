package net.osmand.plus.search

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import net.osmand.data.NearbyPlacePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.utils.PicassoUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.util.Algorithms
import net.osmand.wiki.WikiImage

class NearbyPlacesAdapter(
	val app: OsmandApplication,
	var items: List<NearbyPlacePoint>,
	private var isVertical: Boolean,
	private val onItemClickListener: NearbyItemClickListener
) : RecyclerView.Adapter<NearbyPlacesAdapter.NearbyViewHolder>() {

	interface NearbyItemClickListener {
		fun onNearbyItemClicked(item: NearbyPlacePoint)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NearbyViewHolder {
		val app = parent.context.applicationContext as OsmandApplication
		val inflater = UiUtilities.getInflater(app, isNightMode())
		val view = inflater.inflate(
			if (isVertical) R.layout.search_nearby_item_vertical else R.layout.search_nearby_item,
			parent,
			false)
		return NearbyViewHolder(view)
	}

	private fun isNightMode(): Boolean {
		return !app.getSettings().isLightContent
	}

	override fun onBindViewHolder(holder: NearbyViewHolder, position: Int) {
		val item = items[position]
		holder.bind(item, onItemClickListener)
	}

	override fun getItemCount(): Int = items.size

	class NearbyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		private val imageView: ImageView = itemView.findViewById(R.id.item_image)
		private val iconImageView: ImageView = itemView.findViewById(R.id.item_icon)
		private val titleTextView: TextView = itemView.findViewById(R.id.item_title)
		private val descriptionTextView: TextView? = itemView.findViewById(R.id.item_description)
		private val itemTypeTextView: TextView = itemView.findViewById(R.id.item_type)

		fun bind(item: NearbyPlacePoint, onItemClickListener: NearbyItemClickListener) {
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
					nightMode)

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
			descriptionTextView?.text = item.wikiDesc
			descriptionTextView?.let {
				AndroidUiHelper.updateVisibility(it, !Algorithms.isEmpty(item.wikiDesc))
			}
			titleTextView.text = item.wikiTitle
			itemTypeTextView.text = subType.translation
			itemView.setOnClickListener { onItemClickListener.onNearbyItemClicked(item) }
		}
	}
}