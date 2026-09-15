package net.osmand.plus.gallery.ui.viewer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
import net.osmand.plus.gallery.ui.GallerySectionCardDecoration
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.audionotes.library.data.MediaAttachment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import kotlin.math.max
import kotlin.math.min

abstract class DetailsHolder(view: View) : RecyclerView.ViewHolder(view) {
	var card = -1
}

class MediaDetailsAdapter(
	private val app: OsmandApplication,
	private val nightMode: Boolean,
	private val actions: Actions
) : RecyclerView.Adapter<DetailsHolder>() {

	interface Actions {
		fun onRowAction(action: RowAction)
		fun onCopy(text: String)
		fun onAttachmentMenu(anchor: View, attachment: MediaAttachment)
	}

	var items: List<DetailsItem> = emptyList()
		private set

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
		diff.dispatchUpdatesTo(this)
	}

	fun cardAt(position: Int): Int = items.getOrNull(position)?.card ?: -1

	fun isFirstInCard(position: Int): Boolean = position == 0 || cardAt(position - 1) != cardAt(position)

	fun isLastInCard(position: Int): Boolean = isLastInCard(items, position)

	private fun isLastInCard(list: List<DetailsItem>, position: Int): Boolean =
		position == list.lastIndex || list[position + 1].card != list[position].card

	override fun getItemCount() = items.size

	override fun getItemViewType(position: Int) = when (items[position]) {
		is DetailsItem.Header -> HEADER
		is DetailsItem.Text -> TEXT
		is DetailsItem.Row -> ROW
		is DetailsItem.Attachment -> ATTACHMENT
		is DetailsItem.EmptyAttachments -> EMPTY
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailsHolder {
		val inflater = LayoutInflater.from(parent.context)
		return when (viewType) {
			HEADER -> HeaderHolder(inflater.inflate(R.layout.gallery_details_header_item, parent, false))
			TEXT -> TextHolder(inflater.inflate(R.layout.gallery_details_text_item, parent, false))
			ROW -> DetailsRowHolder(inflater.inflate(R.layout.gallery_details_row_item, parent, false), nightMode)
			ATTACHMENT -> AttachedTargetViewHolder(inflater.inflate(R.layout.track_list_item, parent, false))
			else -> EmptyAttachmentsHolder(inflater.inflate(R.layout.gallery_details_empty_attachments_item, parent, false), nightMode)
		}
	}

	override fun onBindViewHolder(holder: DetailsHolder, position: Int) {
		val item = items[position]
		holder.card = item.card
		val last = isLastInCard(position)
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
		private const val ATTACHMENT = 3
		private const val EMPTY = 4
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

class DetailsCardDecoration(app: OsmandApplication, nightMode: Boolean, private val adapter: MediaDetailsAdapter) : RecyclerView.ItemDecoration() {
	private val gap = app.resources.getDimensionPixelSize(R.dimen.content_padding)
	private val radius = AndroidUtils.dpToPxF(app, GallerySectionCardDecoration.CARD_RADIUS_DP)
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtilities.getColor(app, ColorUtilities.getListBgColorId(nightMode)) }
	private val card = RectF()

	override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
		val position = parent.getChildAdapterPosition(view)
		if (position > 0 && adapter.isFirstInCard(position)) outRect.top = gap
	}

	override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		var index = 0
		while (index < parent.childCount) {
			val first = parent.getChildAt(index)
			val cardId = (parent.getChildViewHolder(first) as? DetailsHolder)?.card ?: -1
			var open = cardId < 0
			var closed = cardId < 0
			var top = Float.MAX_VALUE
			var bottom = -Float.MAX_VALUE
			var end = index
			while (end < parent.childCount) {
				val child = parent.getChildAt(end)
				val holder = parent.getChildViewHolder(child) as? DetailsHolder
				if (holder == null || holder.card != cardId) break
				val position = parent.getChildAdapterPosition(child)
				if (position >= 0) {
					if (adapter.isFirstInCard(position)) open = true
					if (adapter.isLastInCard(position)) closed = true
				}
				val childTop = child.top + child.translationY
				top = min(top, childTop)
				bottom = max(bottom, childTop + child.height * child.alpha)
				end++
			}
			if (cardId >= 0 && top < bottom) {
				card.set(parent.paddingLeft.toFloat(), if (open) top else min(top, 0f) - radius,
					(parent.width - parent.paddingRight).toFloat(), if (closed) bottom else max(bottom, parent.height.toFloat()) + radius)
				canvas.drawRoundRect(card, radius, radius, paint)
			}
			index = max(end, index + 1)
		}
	}
}
