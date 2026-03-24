package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R

class MetricsAdapter : ListAdapter<MetricsAdapter.MetricUi, MetricsAdapter.VH>(DiffCallback()) {
	fun submit(list: List<MetricUi>) {
		submitList(list)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
		val v = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_astro_metric, parent, false)
		return VH(v)
	}

	override fun onBindViewHolder(holder: VH, position: Int) {
		val isLast = position == currentList.lastIndex
		holder.bind(getItem(position), showDivider = !isLast)
	}

	class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
		private val value = itemView.findViewById<TextView>(R.id.value)
		private val label = itemView.findViewById<TextView>(R.id.label)
		private val divider = itemView.findViewById<View>(R.id.divider)

		fun bind(item: MetricUi, showDivider: Boolean) {
			value.text = item.value
			label.text = item.label
			divider.visibility = if (showDivider) View.VISIBLE else View.GONE
		}
	}

	data class MetricUi(
		val value: String,
		val label: String
	)

	private class DiffCallback : DiffUtil.ItemCallback<MetricUi>() {
		override fun areItemsTheSame(oldItem: MetricUi, newItem: MetricUi): Boolean {
			return oldItem.label == newItem.label
		}

		override fun areContentsTheSame(oldItem: MetricUi, newItem: MetricUi): Boolean {
			return oldItem == newItem
		}
	}
}