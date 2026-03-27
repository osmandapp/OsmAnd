package net.osmand.plus.plugins.astronomy.search

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities

internal class StarMapSearchResultsAdapter(
	private val uiUtilities: UiUtilities,
	private val nightMode: Boolean,
	private val visibleEntries: List<StarMapSearchEntry>,
	private val widToDisplayName: Map<String, String>,
	private val shouldShowInfoHeader: () -> Boolean,
	private val useExploreRowLayout: () -> Boolean,
	private val categoryPresetProvider: () -> StarMapSearchCategoryFilter?,
	private val eventTextProvider: (StarMapSearchEntry) -> CharSequence,
	private val onEntrySelected: (StarMapSearchEntry) -> Unit
) : RecyclerView.Adapter<StarMapSearchResultsAdapter.SearchViewHolder>() {

	private val resultFormatter = StarMapSearchResultFormatter(
		uiUtilities = uiUtilities,
		nightMode = nightMode,
		widToDisplayName = widToDisplayName,
		categoryPresetProvider = categoryPresetProvider,
		eventTextProvider = eventTextProvider
	)

	override fun getItemViewType(position: Int): Int {
		return when {
			shouldShowInfoHeader() && position == 0 -> VIEW_TYPE_INFO
			useExploreRowLayout() -> VIEW_TYPE_EXPLORE_ROW
			else -> VIEW_TYPE_ITEM
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
		val layoutId = when (viewType) {
			VIEW_TYPE_INFO -> R.layout.item_star_search_info
			VIEW_TYPE_EXPLORE_ROW -> R.layout.item_astro_explore_row
			else -> R.layout.item_star_search
		}
		val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
		return SearchViewHolder(view)
	}

	override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
		if (getItemViewType(position) == VIEW_TYPE_INFO) {
			holder.bindInfo()
		} else {
			holder.bindResult(getEntryForPosition(position))
		}
	}

	override fun getItemCount(): Int = visibleEntries.size + if (shouldShowInfoHeader()) 1 else 0

	private fun getEntryForPosition(position: Int): StarMapSearchEntry {
		val entryIndex = if (shouldShowInfoHeader()) position - 1 else position
		return visibleEntries[entryIndex]
	}

	inner class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		private val nameText: TextView? = view.findViewById(R.id.object_name) ?: view.findViewById(R.id.row_title)
		private val infoText: TextView? = view.findViewById(R.id.object_info) ?: view.findViewById(R.id.row_subtitle)
		private val iconView: ImageView? = view.findViewById(R.id.object_icon) ?: view.findViewById(R.id.row_icon)
		private val countView: TextView? = view.findViewById(R.id.row_count)
		private val headerInfoIcon: ImageView? = view.findViewById(R.id.info_icon)
		private val headerInfoText: TextView? = view.findViewById(R.id.info_text)

		fun bindInfo() {
			val presetCategory = categoryPresetProvider() ?: return
			headerInfoIcon?.setImageDrawable(
				uiUtilities.getIcon(
					getCategoryIconRes(presetCategory),
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			headerInfoText?.setText(getCategoryInfoTextRes(presetCategory))
			itemView.setOnClickListener(null)
		}

		fun bindResult(entry: StarMapSearchEntry) {
			val subtitle = resultFormatter.buildSubtitle(itemView, entry)
			nameText?.text = entry.displayName
			infoText?.text = subtitle
			infoText?.isVisible = subtitle.isNotEmpty()
			resultFormatter.bindIcon(iconView, entry)
			countView?.isVisible = false
			itemView.setOnClickListener { onEntrySelected(entry) }
		}
	}

	private companion object {
		const val VIEW_TYPE_INFO = 0
		const val VIEW_TYPE_ITEM = 1
		const val VIEW_TYPE_EXPLORE_ROW = 2
	}
}

private class StarMapSearchResultFormatter(
	private val uiUtilities: UiUtilities,
	private val nightMode: Boolean,
	private val widToDisplayName: Map<String, String>,
	private val categoryPresetProvider: () -> StarMapSearchCategoryFilter?,
	private val eventTextProvider: (StarMapSearchEntry) -> CharSequence
) {

	fun bindIcon(iconView: ImageView?, entry: StarMapSearchEntry) {
		val iconCategory = categoryPresetProvider() ?: entry.category
		iconView?.setImageDrawable(
			uiUtilities.getIcon(
				getCategoryIconRes(iconCategory),
				ColorUtilities.getDefaultIconColorId(nightMode)
			)
		)
		iconView?.clearColorFilter()
	}

	fun buildSubtitle(itemView: View, entry: StarMapSearchEntry): CharSequence {
		val descriptorText = buildDescriptor(itemView, entry)
		if (entry.objectRef.type == SkyObject.Type.CONSTELLATION) {
			return SpannableStringBuilder()
				.append(descriptorText)
				.append(" • ")
				.append(eventTextProvider(entry))
		}
		val magnitudeText = itemView.context.getString(R.string.astro_search_magnitude_short, entry.magnitude)
		return SpannableStringBuilder()
			.append(descriptorText)
			.append(" • ")
			.append(magnitudeText)
			.append(" • ")
			.append(eventTextProvider(entry))
	}

	private fun buildDescriptor(itemView: View, entry: StarMapSearchEntry): String {
		val obj = entry.objectRef
		val parentName = resolveParentName(obj)
		return when (categoryPresetProvider()) {
			StarMapSearchCategoryFilter.CONSTELLATIONS -> {
				itemView.context.getString(R.string.astro_type_constellation)
			}
			StarMapSearchCategoryFilter.STARS -> {
				if (parentName.isNullOrEmpty()) {
					itemView.context.getString(R.string.astro_type_star)
				} else {
					itemView.context.getString(R.string.astro_search_in_location, parentName)
				}
			}
			StarMapSearchCategoryFilter.NEBULAS,
			StarMapSearchCategoryFilter.STAR_CLUSTERS,
			StarMapSearchCategoryFilter.DEEP_SKY -> {
				val typeLabel = getSingularTypeLabel(itemView, obj.type)
				if (parentName.isNullOrEmpty()) {
					typeLabel
				} else {
					itemView.context.getString(R.string.astro_search_type_in_location, typeLabel, parentName)
				}
			}
			else -> getSingularTypeLabel(itemView, obj.type)
		}
	}

	private fun getSingularTypeLabel(itemView: View, type: SkyObject.Type): String {
		return when (type) {
			SkyObject.Type.SUN -> itemView.context.getString(R.string.astro_name_sun)
			SkyObject.Type.MOON -> itemView.context.getString(R.string.astro_name_moon)
			SkyObject.Type.PLANET -> itemView.context.getString(R.string.astro_type_planet)
			SkyObject.Type.STAR -> itemView.context.getString(R.string.astro_type_star)
			SkyObject.Type.GALAXY -> itemView.context.getString(R.string.astro_type_galaxy)
			SkyObject.Type.NEBULA -> itemView.context.getString(R.string.astro_type_nebula)
			SkyObject.Type.BLACK_HOLE -> itemView.context.getString(R.string.astro_type_black_hole)
			SkyObject.Type.OPEN_CLUSTER -> itemView.context.getString(R.string.astro_type_open_cluster)
			SkyObject.Type.GLOBULAR_CLUSTER -> itemView.context.getString(R.string.astro_type_globular_cluster)
			SkyObject.Type.GALAXY_CLUSTER -> itemView.context.getString(R.string.astro_type_galaxy_cluster)
			SkyObject.Type.CONSTELLATION -> itemView.context.getString(R.string.astro_type_constellation)
		}
	}

	private fun resolveParentName(obj: SkyObject): String? {
		val centerWid = obj.centerWId?.trim().orEmpty()
		if (centerWid.isEmpty()) {
			return null
		}
		val mappedName = widToDisplayName[centerWid]
		if (!mappedName.isNullOrEmpty()) {
			return mappedName
		}
		return centerWid.replace('_', ' ').ifEmpty { null }
	}
}

private fun getCategoryIconRes(category: StarMapSearchCategoryFilter): Int {
	return when (category) {
		StarMapSearchCategoryFilter.SOLAR_SYSTEM -> R.drawable.ic_action_planet_outlined
		StarMapSearchCategoryFilter.CONSTELLATIONS -> R.drawable.ic_action_constellations
		StarMapSearchCategoryFilter.STARS -> R.drawable.ic_action_stars
		StarMapSearchCategoryFilter.NEBULAS -> R.drawable.ic_action_nebulas
		StarMapSearchCategoryFilter.STAR_CLUSTERS -> R.drawable.ic_action_star_clusters
		StarMapSearchCategoryFilter.DEEP_SKY -> R.drawable.ic_action_galaxy
		StarMapSearchCategoryFilter.ALL -> R.drawable.ic_action_search_dark
	}
}

private fun getCategoryInfoTextRes(category: StarMapSearchCategoryFilter): Int {
	return when (category) {
		StarMapSearchCategoryFilter.SOLAR_SYSTEM -> R.string.astro_search_info_solar_system
		StarMapSearchCategoryFilter.CONSTELLATIONS -> R.string.astro_search_info_constellations
		StarMapSearchCategoryFilter.STARS -> R.string.astro_search_info_stars
		StarMapSearchCategoryFilter.NEBULAS -> R.string.astro_search_info_nebulas
		StarMapSearchCategoryFilter.STAR_CLUSTERS -> R.string.astro_search_info_star_clusters
		StarMapSearchCategoryFilter.DEEP_SKY -> R.string.astro_search_info_deep_sky
		StarMapSearchCategoryFilter.ALL -> R.string.astro_search_info_solar_system
	}
}
