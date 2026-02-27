package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard
import net.osmand.plus.mapcontextmenu.gallery.GalleryController
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter
import net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroContextCard
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroCatalogsCardModel
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroCatalogsCardViewHolder
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroDescriptionCardModel
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroDescriptionCardViewHolder
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroGalleryCardModel
import net.osmand.plus.plugins.astronomy.views.AstroGalleryCardViewHolder
import net.osmand.plus.plugins.astronomy.views.AstroScheduleCardModel
import net.osmand.plus.plugins.astronomy.views.AstroScheduleCardViewHolder
import net.osmand.plus.plugins.astronomy.views.AstroVisibilityCardModel
import net.osmand.plus.plugins.astronomy.views.AstroVisibilityCardViewHolder

class AstroContextMenuAdapter(
	private val app: OsmandApplication,
	private val mapActivity: MapActivity,
	private val nightMode: Boolean,
	private val astroContextMenuFragment: AstroContextMenuFragment,
	private val galleryController: GalleryController?,
	private val onGalleryToggle: (String) -> Unit,
	private val onUpdateImage: () -> Unit,
	private val onScheduleResetPeriod: () -> Unit,
	private val onScheduleShiftPeriod: (daysDelta: Int) -> Unit
) :
	RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	var skyObject: SkyObject? = null

	val items = mutableListOf<AstroContextCard>()

	companion object {
		private const val VIEW_TYPE_DESCRIPTION = 0
		private const val VIEW_TYPE_VISIBILITY = 1
		private const val VIEW_TYPE_SCHEDULE = 2
		private const val VIEW_TYPE_CATALOGS = 3
		private const val VIEW_TYPE_GALLERY = 5

		const val PAYLOAD_GALLERY_STATE = "gallery_state"
		const val PAYLOAD_GALLERY_CONTENT = "gallery_content"
	}

	fun setItems(newItems: List<AstroContextCard>) {
		items.clear()
		items.addAll(newItems)
		notifyDataSetChanged()
	}

	override fun getItemViewType(position: Int): Int {
		return when (val item = items[position]) {
			is AstroDescriptionCardModel -> {
				VIEW_TYPE_DESCRIPTION
			}

			is AstroVisibilityCardModel -> {
				VIEW_TYPE_VISIBILITY
			}

			is AstroScheduleCardModel -> {
				VIEW_TYPE_SCHEDULE
			}

			is AstroCatalogsCardModel -> {
				VIEW_TYPE_CATALOGS
			}

			is AstroGalleryCardModel -> {
				VIEW_TYPE_GALLERY
			}

			else -> throw IllegalArgumentException("Unknown card type: ${item::class.java.simpleName}")
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			VIEW_TYPE_DESCRIPTION -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_context_description_card, parent, false)
				AstroDescriptionCardViewHolder(view)
			}

			VIEW_TYPE_VISIBILITY -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_visibility_card, parent, false)
				AstroVisibilityCardViewHolder(view)
			}

			VIEW_TYPE_SCHEDULE -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_schedule_card, parent, false)
				AstroScheduleCardViewHolder(
					itemView = view,
					onResetPeriod = onScheduleResetPeriod,
					onShiftPeriod = onScheduleShiftPeriod
				)
			}

			VIEW_TYPE_CATALOGS -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_context_catalogs_card, parent, false)
				AstroCatalogsCardViewHolder(view, app)
			}

			VIEW_TYPE_GALLERY -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_gallery_card, parent, false)
				AstroGalleryCardViewHolder(
					itemView = view,
					app = app,
					mapActivity = mapActivity,
					getShowAllTitle = {
						skyObject?.localizedName ?: skyObject?.name
					},
					listener = object : GalleryGridAdapter.ImageCardListener {

						override fun onImageClicked(imageCard: ImageCard) {
							galleryController?.let { controller ->
								GalleryPhotoPagerFragment.showInstance(
									mapActivity,
									controller.getImageCardFromUrl(imageCard.imageUrl)
								)
							}
						}

						override fun onReloadImages() {
							onUpdateImage()
						}
					},
					onToggle = { wid ->
						onGalleryToggle(wid)
					}
				)
			}

			else -> throw IllegalArgumentException("Unknown view type: $viewType")
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]
		when (holder) {
			is AstroDescriptionCardViewHolder -> holder.bind(
				app,
				nightMode,
				item as AstroDescriptionCardModel,
				astroContextMenuFragment
			)

			is AstroVisibilityCardViewHolder -> holder.bind(item as AstroVisibilityCardModel)
			is AstroScheduleCardViewHolder -> holder.bind(item as AstroScheduleCardModel)
			is AstroCatalogsCardViewHolder -> holder.bind(item as AstroCatalogsCardModel)
			is AstroGalleryCardViewHolder -> holder.bind(item as AstroGalleryCardModel, nightMode)
		}
	}

	override fun onBindViewHolder(
		holder: RecyclerView.ViewHolder,
		position: Int,
		payloads: MutableList<Any>
	) {
		if (payloads.isNotEmpty() && holder is AstroGalleryCardViewHolder) {
			val model = items[position] as AstroGalleryCardModel
			holder.bindGallery(model, nightMode)
			return
		}
		super.onBindViewHolder(holder, position, payloads)
	}

	override fun getItemCount(): Int = items.size

	inline fun <reified T> getItemPosition(): Int =
		items.indexOfFirst { it is T }
}