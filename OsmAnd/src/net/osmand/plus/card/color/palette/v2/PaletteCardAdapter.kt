package net.osmand.plus.card.color.palette.v2

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.palette.contract.IPaletteController
import net.osmand.plus.palette.contract.IPaletteInteractionListener
import net.osmand.plus.palette.view.binder.PaletteItemViewBinder
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.domain.PaletteItem

class PaletteCardAdapter(
	private val listener: IPaletteInteractionListener,
	private val controller: IPaletteController,
	private val itemViewBinder: PaletteItemViewBinder
) : RecyclerView.Adapter<PaletteCardAdapter.ViewHolder>() {

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

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = itemViewBinder.createView(parent)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = items[position]
		val isSelected = controller.isPaletteItemSelected(item)

		itemViewBinder.bindView(holder.itemView, item, isSelected)

		holder.itemView.setOnClickListener {
			listener.onPaletteItemClick(item, false)
		}
		holder.itemView.setOnLongClickListener {
			listener.onPaletteItemLongClick(holder.itemView, item)
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

	class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}