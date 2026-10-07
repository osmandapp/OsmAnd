package net.osmand.plus.gallery.ui.viewer

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.library.MediaAttachment
import net.osmand.plus.gallery.ui.GallerySectionBoundary
import net.osmand.plus.gallery.ui.GallerySectionSource
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.ColorUtilities

abstract class DetailsHolder(view: View) : RecyclerView.ViewHolder(view) {
	var boundary: GallerySectionBoundary? = null
}

class MediaDetailsAdapter(
	private val nightMode: Boolean,
	private val actions: Actions
) : RecyclerView.Adapter<DetailsHolder>(), GallerySectionSource {

	interface Actions {
		fun onRowAction(action: RowAction)
		fun onCopy(text: String)
		fun onAttachmentMenu(anchor: View, attachment: MediaAttachment)
	}

	internal var items: List<DetailsItem> = emptyList()
		private set
	private var boundaries: List<GallerySectionBoundary> = emptyList()

	fun submit(newItems: List<DetailsItem>) {
		val oldItems = items
		val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
			override fun getOldListSize() = oldItems.size
			override fun getNewListSize() = newItems.size
			override fun areItemsTheSame(oldPosition: Int, newPosition: Int) = oldItems[oldPosition].key == newItems[newPosition].key
			override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
				oldItems[oldPosition] == newItems[newPosition] && isLastInCard(oldItems, oldPosition) == isLastInCard(newItems, newPosition)
		})
		items = newItems
		boundaries = cardBoundaries(newItems)
		diff.dispatchUpdatesTo(this)
	}

	override fun getSectionBoundary(position: Int): GallerySectionBoundary? = boundaries.getOrNull(position)

	override fun getBoundSectionBoundary(holder: RecyclerView.ViewHolder): GallerySectionBoundary? = (holder as? DetailsHolder)?.boundary

	override fun isGridCell(position: Int): Boolean = false

	private fun cardBoundaries(list: List<DetailsItem>): List<GallerySectionBoundary> {
		val result = ArrayList<GallerySectionBoundary>(list.size)
		var first = 0
		while (first < list.size) {
			var last = first
			while (last + 1 < list.size && list[last + 1].card == list[first].card) last++
			for (index in first..last) {
				result += GallerySectionBoundary("card:" + list[first].card, first, last, first, false, index == first, index == last)
			}
			first = last + 1
		}
		return result
	}

	private fun isLastInCard(list: List<DetailsItem>, position: Int): Boolean =
		position == list.lastIndex || list[position + 1].card != list[position].card

	override fun getItemCount() = items.size

	override fun getItemViewType(position: Int) = when (val item = items[position]) {
		is DetailsItem.Header -> HEADER
		is DetailsItem.Text -> TEXT
		is DetailsItem.Row -> ROW
		is DetailsItem.Attachment -> when (item.attachment.kind) {
			MediaAttachment.Kind.FAVORITE -> ATTACHED_FAVORITE
			MediaAttachment.Kind.TRACK_POINT -> ATTACHED_TRACK
		}
		is DetailsItem.EmptyAttachments -> EMPTY
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailsHolder {
		val inflater = LayoutInflater.from(parent.context)
		return when (viewType) {
			HEADER -> HeaderHolder(inflater.inflate(R.layout.gallery_details_header_item, parent, false))
			TEXT -> TextHolder(inflater.inflate(R.layout.gallery_details_text_item, parent, false))
			ROW -> DetailsRowHolder(inflater.inflate(R.layout.gallery_details_row_item, parent, false), nightMode)
			ATTACHED_FAVORITE -> AttachedTargetViewHolder(inflater.inflate(AttachedTargetViewHolder.layoutFor(MediaAttachment.Kind.FAVORITE), parent, false))
			ATTACHED_TRACK -> AttachedTargetViewHolder(inflater.inflate(AttachedTargetViewHolder.layoutFor(MediaAttachment.Kind.TRACK_POINT), parent, false))
			else -> EmptyAttachmentsHolder(inflater.inflate(R.layout.gallery_details_empty_attachments_item, parent, false), nightMode)
		}
	}

	override fun onBindViewHolder(holder: DetailsHolder, position: Int) {
		val item = items[position]
		holder.boundary = boundaries[position]
		val last = isLastInCard(items, position)
		when (holder) {
			is HeaderHolder -> holder.title.text = (item as DetailsItem.Header).title
			is TextHolder -> holder.bind((item as DetailsItem.Text).text, last, actions)
			is DetailsRowHolder -> holder.bind(item as DetailsItem.Row, last, actions)
			is AttachedTargetViewHolder -> holder.bind((item as DetailsItem.Attachment).attachment, !last, actions)
			is EmptyAttachmentsHolder -> {}
		}
	}

	class HeaderHolder(view: View) : DetailsHolder(view) {
		val title: TextView = view.findViewById(R.id.title)
	}

	class TextHolder(view: View) : DetailsHolder(view) {
		private val text: TextView = view.findViewById(R.id.text)
		private val divider: View = view.findViewById(R.id.divider)

		fun bind(value: String, last: Boolean, actions: Actions) {
			text.text = value
			AndroidUiHelper.updateVisibility(divider, !last)
			itemView.setOnLongClickListener {
				actions.onCopy(value)
				true
			}
		}
	}

	class EmptyAttachmentsHolder(view: View, nightMode: Boolean) : DetailsHolder(view) {
		init {
			val app = view.context.applicationContext as OsmandApplication
			view.findViewById<ImageView>(R.id.icon).setImageDrawable(
				app.uiUtilities.getPaintedIcon(R.drawable.ic_action_attachment_add, ColorUtilities.getDefaultIconColor(app, nightMode)))
		}
	}

	companion object {
		private const val HEADER = 0
		private const val TEXT = 1
		private const val ROW = 2
		private const val ATTACHED_FAVORITE = 3
		private const val ATTACHED_TRACK = 4
		private const val EMPTY = 5
	}
}

class DetailsRowHolder(view: View, private val nightMode: Boolean) : DetailsHolder(view) {
	private val app = view.context.applicationContext as OsmandApplication
	private val row: View = view.findViewById(R.id.row)
	private val label: TextView = view.findViewById(R.id.label)
	private val value: TextView = view.findViewById(R.id.value)
	private val icon: ImageView = view.findViewById(R.id.trailing_icon)
	private val divider: View = view.findViewById(R.id.divider)

	fun bind(item: DetailsItem.Row, last: Boolean, actions: MediaDetailsAdapter.Actions) {
		label.text = item.label
		value.text = item.value
		value.ellipsize = if (item.middleEllipsis) TextUtils.TruncateAt.MIDDLE else TextUtils.TruncateAt.END
		val link = item.action is RowAction.OpenUrl
		value.setTextColor(if (link) ColorUtilities.getActiveColor(app, nightMode) else ColorUtilities.getPrimaryTextColor(app, nightMode))
		if (item.iconId != 0) {
			icon.setImageDrawable(if (item.tintIcon) {
				app.uiUtilities.getPaintedIcon(item.iconId, ColorUtilities.getDefaultIconColor(app, nightMode))
			} else {
				app.uiUtilities.getIcon(item.iconId)
			})
		}
		AndroidUiHelper.updateVisibility(icon, item.iconId != 0)
		AndroidUiHelper.updateVisibility(divider, !last)
		val action = item.action
		row.setOnClickListener(if (action != null) View.OnClickListener { actions.onRowAction(action) } else null)
		row.isClickable = action != null
		row.setOnLongClickListener {
			actions.onCopy(item.value)
			true
		}
	}
}
