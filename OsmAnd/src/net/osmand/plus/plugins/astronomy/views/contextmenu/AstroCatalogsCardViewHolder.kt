package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R

class AstroCatalogsCardViewHolder(
	itemView: View,
	private val app: OsmandApplication
) : RecyclerView.ViewHolder(itemView) {

	private val maxVisible = 10
	private var expanded = false
	private val group: ChipGroup = itemView.findViewById(R.id.chipGroup)

	fun bind(catalogCard: AstroCatalogsCardModel) {
		val items = catalogCard.catalogs
		group.removeAllViews()
		val inflater = LayoutInflater.from(itemView.context)

		val needShowMore = items.size > maxVisible
		val visible = if (!expanded && needShowMore) items.take(maxVisible) else items

		visible.forEach { text ->
			group.addView(createCatalogChip(inflater, group, text.catalogId) {
			})
		}

		if (needShowMore) {
			val label = app.getString(if (expanded) R.string.shared_string_show_less else R.string.show_more)
			group.addView(createCatalogChip(inflater, group, label) {
				expanded = !expanded
				bind(catalogCard)
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