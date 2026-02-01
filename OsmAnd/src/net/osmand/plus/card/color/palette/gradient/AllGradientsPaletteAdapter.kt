package net.osmand.plus.card.color.palette.gradient

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.palette.controller.BasePaletteController
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.domain.PaletteItem

class AllGradientsPaletteAdapter(
	private val app: OsmandApplication,
	private val activity: FragmentActivity,
	private val controller: BasePaletteController,
	private val nightMode: Boolean
) : RecyclerView.Adapter<AllGradientsPaletteAdapter.GradientViewHolder>() {

	private var items: List<PaletteItem> = controller.getPaletteItems(PaletteSortMode.LAST_USED_TIME)
	private val themedInflater = UiUtilities.getInflater(activity, nightMode)

	init {
		setHasStableIds(true)
	}

	@SuppressLint("NotifyDataSetChanged")
	fun update() {
		items = controller.getPaletteItems(PaletteSortMode.LAST_USED_TIME)
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradientViewHolder {
		val itemView = themedInflater.inflate(R.layout.gradient_palette_item, parent, false)
		return GradientViewHolder(itemView)
	}

	override fun onBindViewHolder(holder: GradientViewHolder, position: Int) {
		val item = items[position]
		val isSelected = controller.isPaletteItemSelected(item)
		holder.bind(item, isSelected)
	}

	override fun getItemCount(): Int = items.size

	override fun getItemId(position: Int): Long {
		// TODO: use string to long mapper instead
		return items[position].id.hashCode().toLong()
	}

	fun askNotifyItemChanged(item: PaletteItem?) {
		if (item != null) {
			val index = items.indexOfFirst { it.id == item.id }
			if (index >= 0) {
				notifyItemChanged(index)
			}
		}
	}

	inner class GradientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		private val radioButton: AppCompatRadioButton = itemView.findViewById(R.id.compound_button)
		private val icon: ImageView = itemView.findViewById(R.id.icon)
		private val title: TextView = itemView.findViewById(R.id.title)
		private val description: TextView = itemView.findViewById(R.id.description)
		private val menuButton: ImageButton = itemView.findViewById(R.id.menu_button)
		private val bottomDivider: View = itemView.findViewById(R.id.divider_bottom)
		private val verticalDivider: View = itemView.findViewById(R.id.vertical_end_button_divider)

		fun bind(item: PaletteItem, isSelected: Boolean) {
			UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), radioButton)
			radioButton.isChecked = isSelected

			if (item is PaletteItem.Gradient) {
				// 1. Icon (Draw Gradient)
				val colors = item.points.map { it.color }.toIntArray()
				// GradientDrawable need at least 2 colors, if there is only 1 - duplicate it
				val safeColors = if (colors.size < 2) {
					val c = colors.firstOrNull() ?: Color.TRANSPARENT
					intArrayOf(c, c)
				} else {
					colors
				}

				val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, safeColors).apply {
					gradientType = GradientDrawable.LINEAR_GRADIENT
					shape = GradientDrawable.RECTANGLE
					cornerRadius = AndroidUtils.dpToPx(app, 2f).toFloat()
				}
				icon.setImageDrawable(gradientDrawable)

				// 2. Texts
				title.text = item.displayName

				// Build points description
				val sb = StringBuilder()
				item.points.forEachIndexed { index, point ->
					if (index > 0) sb.append(" • ")

					val value = point.value
					val formattedValue = if (item.getPaletteCategory().isTerrainRelated()) {
						GradientUiHelper.formatTerrainTypeValues(value)
					} else {
						value.toString()
					}
					sb.append(formattedValue)
				}
				description.text = sb.toString()

				bottomDivider.visibility = View.VISIBLE
				verticalDivider.visibility = View.GONE

				// 3. Clicks
				itemView.setOnClickListener {
					controller.onPaletteItemClick(item, markAsUsed = false)
				}

				// 4. Menu Button
				menuButton.visibility = View.VISIBLE
				menuButton.setOnClickListener {
					controller.onPaletteItemLongClick(menuButton, item)
				}
			}
		}
	}
}