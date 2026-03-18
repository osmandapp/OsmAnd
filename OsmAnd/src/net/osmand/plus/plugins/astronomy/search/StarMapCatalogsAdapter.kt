package net.osmand.plus.plugins.astronomy.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.plugins.astronomy.Catalog
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities

internal data class StarMapCatalogEntry(
	val catalog: Catalog,
	val displayName: String,
	val description: String?,
	val objectCount: Int
)

internal class StarMapCatalogsAdapter(
	private val uiUtilities: UiUtilities,
	private val nightMode: Boolean,
	private val visibleEntries: List<StarMapCatalogEntry>,
	private val onCatalogSelected: (StarMapCatalogEntry) -> Unit
) : RecyclerView.Adapter<StarMapCatalogsAdapter.CatalogViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_astro_explore_row, parent, false)
		return CatalogViewHolder(view)
	}

	override fun onBindViewHolder(holder: CatalogViewHolder, position: Int) {
		holder.bind(visibleEntries[position], position == itemCount - 1)
	}

	override fun getItemCount(): Int = visibleEntries.size

	inner class CatalogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		private val iconView: ImageView = view.findViewById(R.id.row_icon)
		private val titleView: TextView = view.findViewById(R.id.row_title)
		private val subtitleView: TextView = view.findViewById(R.id.row_subtitle)
		private val countView: TextView = view.findViewById(R.id.row_count)
		private val dividerView: View = view.findViewById(R.id.row_divider)

		fun bind(entry: StarMapCatalogEntry, isLastItem: Boolean) {
			iconView.setImageDrawable(
				uiUtilities.getIcon(
					R.drawable.ic_action_book_info,
					ColorUtilities.getDefaultIconColorId(nightMode)
				)
			)
			titleView.text = entry.displayName
			subtitleView.text = entry.description?.takeIf { it.isNotBlank() }
				?: itemView.resources.getQuantityString(R.plurals.astro_catalog_objects_count, entry.objectCount, entry.objectCount)
			subtitleView.isVisible = subtitleView.text.isNotEmpty()
			countView.isVisible = false
			dividerView.isVisible = !isLastItem
			itemView.setOnClickListener { onCatalogSelected(entry) }
		}
	}
}
