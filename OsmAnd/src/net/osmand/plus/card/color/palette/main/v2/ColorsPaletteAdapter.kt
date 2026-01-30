package net.osmand.plus.card.color.palette.main.v2

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.main.ColorsPaletteElements
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.domain.PaletteItem

// TODO: rename to PaletteItemsCardAdapter
class ColorsPaletteAdapter(
	private val activity: FragmentActivity,
	private val controller: IColorsPaletteController,
	private val nightMode: Boolean
) : RecyclerView.Adapter<ColorsPaletteAdapter.ColorViewHolder>() {

	private val paletteElements = ColorsPaletteElements(activity, nightMode)
	private var items: List<PaletteItem>

	init {
		items = controller.getPaletteItems(PaletteSortMode.LAST_USED_TIME)
		// TODO: we could use DiffUtils instead of setHasStableIds
		setHasStableIds(true)
	}

	@SuppressLint("NotifyDataSetChanged")
	fun updateItemsList() {
		items = controller.getPaletteItems(PaletteSortMode.LAST_USED_TIME)
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
		val view = paletteElements.createCircleView(parent)
		return ColorViewHolder(view)
	}

	override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
		val item = items[position]
		val isSelected = controller.isPaletteItemSelected(item)

		// TODO: don't check here
		if (item is PaletteItem.Solid) {
			paletteElements.updateColorItemView(holder.itemView, item.color, isSelected)
		}

		holder.itemView.setOnClickListener {
			controller.onSelectItemFromPalette(item, false)
		}
		holder.itemView.setOnLongClickListener {
			controller.onPaletteItemLongClick(activity, holder.background, item, nightMode)
			false
		}
	}

	fun askNotifyItemChanged(item: PaletteItem?) {
		if (item != null) {
			val index = indexOf(item)
			if (index >= 0) {
				notifyItemChanged(index)
			}
		}
	}

	fun indexOf(item: PaletteItem): Int {
		return items.indexOfFirst { it.id == item.id }
	}

	override fun getItemCount(): Int = items.size

	override fun getItemId(position: Int): Long {
		// TODO: use string to long id mapper (store and fetch via controller)
		return items[position].id.hashCode().toLong()
	}

	// TODO: rename to PaletteItemViewHolder
	class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val outline: ImageView = itemView.findViewById(R.id.outline)
		val background: ImageView = itemView.findViewById(R.id.background)
	}
}