package net.osmand.plus.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.PicassoUtils
import net.osmand.wiki.WikiCoreHelper
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData
import net.osmand.wiki.WikiImage

class NearbyAdapter(
	var items: List<OsmandApiFeatureData>,
	private val onItemClickListener: NearbyItemClickListener
) : RecyclerView.Adapter<NearbyAdapter.NearbyViewHolder>() {

	interface NearbyItemClickListener {
		fun onNearbyItemClicked(item: OsmandApiFeatureData)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NearbyViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.search_nearby_item, parent, false)
		return NearbyViewHolder(view)
	}

	override fun onBindViewHolder(holder: NearbyViewHolder, position: Int) {
		val item = items[position]
		holder.bind(item, onItemClickListener)
	}

	override fun getItemCount(): Int = items.size

	class NearbyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		private val imageView: ImageView = itemView.findViewById(R.id.item_image)
		private val titleTextView: TextView = itemView.findViewById(R.id.item_title)
		private val descriptionTextView: TextView = itemView.findViewById(R.id.item_description)
		private var imageData: WikiImage? = null

		fun bind(item: OsmandApiFeatureData, onItemClickListener: NearbyItemClickListener) {
			imageData = WikiCoreHelper.getImageData(item.properties.photoTitle);
			val app = imageView.context.applicationContext as OsmandApplication
			val picasso = PicassoUtils.getPicasso(app)

			imageData?.let {
				Picasso.get()
					.load(it.imageStubUrl)
					.error(R.drawable.mm_ferry_terminal_small_night)
					.into(imageView, object : Callback {
						override fun onSuccess() {
							picasso.setResultLoaded(it.imageStubUrl, true)
						}

						override fun onError(e: Exception?) {
							picasso.setResultLoaded(it.imageStubUrl, true)
						}
					})
			}
			titleTextView.text = item.properties.wikiTitle
			descriptionTextView.text = item.properties.poisubtype
			itemView.setOnClickListener { onItemClickListener.onNearbyItemClicked(item) }
		}
	}
}