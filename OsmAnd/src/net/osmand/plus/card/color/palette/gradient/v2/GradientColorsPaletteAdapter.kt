package net.osmand.plus.card.color.palette.gradient.v2

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.GradientUiHelper
import net.osmand.plus.card.color.palette.main.v2.IColorsPaletteController
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.domain.PaletteItem

class GradientColorsPaletteAdapter(
	activity: FragmentActivity,
	private val controller: IColorsPaletteController,
	nightMode: Boolean
) : RecyclerView.Adapter<GradientColorsPaletteAdapter.ColorViewHolder>() {

	private val gradientUiHelper = GradientUiHelper(activity, nightMode)
	private var items: List<PaletteItem>

	init {
		items = controller.getPaletteItems(PaletteSortMode.LAST_USED_TIME)
		setHasStableIds(true)
	}

	@SuppressLint("NotifyDataSetChanged")
	fun updateItemsList() {
		items = controller.getPaletteItems(PaletteSortMode.LAST_USED_TIME)
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
		val view = gradientUiHelper.createRectangleView(parent)
		return ColorViewHolder(view)
	}

	override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
		val item = items[position]
		val isSelected = controller.isPaletteItemSelected(item)

		if (item is PaletteItem.Gradient) {
			gradientUiHelper.updateColorItemView(holder.itemView, item, isSelected)
		}

		holder.itemView.setOnClickListener {
			controller.onSelectItemFromPalette(item, false)
		}
	}

	fun indexOf(item: PaletteItem): Int {
		return items.indexOfFirst { it.id == item.id }
	}

	fun askNotifyItemChanged(item: PaletteItem?) {
		if (item != null) {
			val index = indexOf(item)
			if (index >= 0) {
				notifyItemChanged(index)
			}
		}
	}

	override fun getItemCount(): Int = items.size

	override fun getItemId(position: Int): Long {
		// TODO: use string to long mapper instead
		return items[position].id.hashCode().toLong()
	}

	class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val outline: ImageView = itemView.findViewById(R.id.outline)
		val background: ImageView = itemView.findViewById(R.id.background)
	}
}