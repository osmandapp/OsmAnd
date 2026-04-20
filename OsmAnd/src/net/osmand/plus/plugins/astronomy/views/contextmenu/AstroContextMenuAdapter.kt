package net.osmand.plus.plugins.astronomy.views.contextmenu
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard
import net.osmand.plus.mapcontextmenu.gallery.GalleryController
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter
import net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment
import net.osmand.plus.plugins.astronomy.Catalog
import java.time.LocalDate

class AstroContextMenuAdapter(
	private val app: OsmandApplication,
	private val mapActivity: MapActivity,
	private val nightMode: Boolean,
	private val galleryController: GalleryController?,
	private val onDescriptionRead: (AstroDescriptionCardItem) -> Unit,
	private val onGalleryToggle: (String) -> Unit,
	private val onUpdateImage: () -> Unit,
	private val onKnowledgeCardAction: () -> Unit,
	private val onVisibilityResetToToday: () -> Unit,
	private val onVisibilityCursorChanged: (Long) -> Unit,
	private val onScheduleResetPeriod: () -> Unit,
	private val onScheduleShiftPeriod: (daysDelta: Int) -> Unit,
	private val onScheduleSelectDate: (LocalDate) -> Unit,
	private val onCatalogsToggleExpanded: () -> Unit,
	private val onCatalogClick: (Catalog) -> Unit
) : ListAdapter<AstroContextMenuItem, RecyclerView.ViewHolder>(DiffCallback()) {

	companion object {
		private const val VIEW_TYPE_DESCRIPTION = 0
		private const val VIEW_TYPE_VISIBILITY = 1
		private const val VIEW_TYPE_SCHEDULE = 2
		private const val VIEW_TYPE_CATALOGS = 3
		private const val VIEW_TYPE_KNOWLEDGE = 4
		private const val VIEW_TYPE_GALLERY = 5
	}

	init {
		setHasStableIds(true)
	}

	override fun getItemId(position: Int): Long = getItem(position).key.stableId

	fun submitItems(items: List<AstroContextMenuItem>, onCommitted: () -> Unit = {}) {
		submitList(items, onCommitted)
	}

	fun getItemPosition(cardKey: AstroContextCardKey): Int =
		currentList.indexOfFirst { it.key == cardKey }

	override fun getItemViewType(position: Int): Int {
		return when (getItem(position).key) {
			AstroContextCardKey.DESCRIPTION -> VIEW_TYPE_DESCRIPTION
			AstroContextCardKey.VISIBILITY -> VIEW_TYPE_VISIBILITY
			AstroContextCardKey.SCHEDULE -> VIEW_TYPE_SCHEDULE
			AstroContextCardKey.CATALOGS -> VIEW_TYPE_CATALOGS
			AstroContextCardKey.KNOWLEDGE -> VIEW_TYPE_KNOWLEDGE
			AstroContextCardKey.GALLERY -> VIEW_TYPE_GALLERY
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
				AstroVisibilityCardViewHolder(view, onVisibilityResetToToday, onVisibilityCursorChanged)
			}

			VIEW_TYPE_SCHEDULE -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_schedule_card, parent, false)
				AstroScheduleCardViewHolder(
					itemView = view,
					onResetPeriod = onScheduleResetPeriod,
					onShiftPeriod = onScheduleShiftPeriod,
					onSelectDate = onScheduleSelectDate
				)
			}

			VIEW_TYPE_CATALOGS -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_context_catalogs_card, parent, false)
				AstroCatalogsCardViewHolder(view, app, onCatalogsToggleExpanded, onCatalogClick)
			}

			VIEW_TYPE_KNOWLEDGE -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_context_knowledge_card, parent, false)
				AstroKnowledgeCardViewHolder(view, onKnowledgeCardAction)
			}

			VIEW_TYPE_GALLERY -> {
				val view = LayoutInflater.from(parent.context)
					.inflate(R.layout.astro_gallery_card, parent, false)
				AstroGalleryCardViewHolder(
					itemView = view,
					app = app,
					mapActivity = mapActivity,
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
					onToggle = onGalleryToggle
				)
			}

			else -> throw IllegalArgumentException("Unknown view type: $viewType")
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		when (holder) {
			is AstroDescriptionCardViewHolder -> holder.bind(
				app = app,
				nightMode = nightMode,
				item = getItem(position) as AstroDescriptionCardItem,
				onReadClick = onDescriptionRead
			)

			is AstroVisibilityCardViewHolder -> holder.bind(getItem(position) as AstroVisibilityCardItem)
			is AstroScheduleCardViewHolder -> holder.bind(getItem(position) as AstroScheduleCardItem)
			is AstroCatalogsCardViewHolder -> holder.bind(getItem(position) as AstroCatalogsCardItem)
			is AstroKnowledgeCardViewHolder -> holder.bind(getItem(position) as AstroKnowledgeCardItem)

			is AstroGalleryCardViewHolder -> holder.bind(
				getItem(position) as AstroGalleryCardItem,
				nightMode
			)
		}
	}

	private class DiffCallback : DiffUtil.ItemCallback<AstroContextMenuItem>() {
		override fun areItemsTheSame(
			oldItem: AstroContextMenuItem,
			newItem: AstroContextMenuItem
		): Boolean {
			return oldItem.key == newItem.key
		}

		override fun areContentsTheSame(
			oldItem: AstroContextMenuItem,
			newItem: AstroContextMenuItem
		): Boolean {
			return oldItem == newItem
		}
	}
}
