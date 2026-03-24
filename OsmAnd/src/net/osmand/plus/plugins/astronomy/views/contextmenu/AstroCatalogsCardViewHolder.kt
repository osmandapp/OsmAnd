package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.astronomy.Catalog

class AstroCatalogsCardViewHolder(
	itemView: View,
	private val app: OsmandApplication,
	private val onToggleExpanded: () -> Unit,
	private val onCatalogClick: (Catalog) -> Unit
) : RecyclerView.ViewHolder(itemView) {

	private val maxVisible = 5
	private val group: ChipGroup = itemView.findViewById(R.id.chipGroup)

	fun bind(item: AstroCatalogsCardItem) {
		val items = item.catalogs
		group.removeAllViews()
		val inflater = LayoutInflater.from(itemView.context)

		val needShowMore = items.size > maxVisible
		val visible = if (!item.expanded && needShowMore) items.take(maxVisible) else items

		visible.forEach { catalog ->
			group.addView(createCatalogChip(inflater, group, catalog.catalogId) {
				onCatalogClick(catalog)
			})
		}

		if (needShowMore) {
			val label = app.getString(if (item.expanded) R.string.shared_string_show_less else R.string.shared_string_ellipsis)
			group.addView(createCatalogChip(inflater, group, label) {
				onToggleExpanded()
			})
		}
	}

	private fun createCatalogChip(
		inflater: LayoutInflater,
		parent: ViewGroup,
		text: String,
		onClick: () -> Unit
	): View {
		val v = inflater.inflate(R.layout.astro_catalog_chip_item, parent, false)
		v.findViewById<TextView>(R.id.text).text = text
		v.setOnClickListener { onClick() }
		return v
	}
}
