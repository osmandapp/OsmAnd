package net.osmand.plus.plugins.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.base.BaseBottomSheetDialogFragment
import net.osmand.plus.plugins.astro.utils.AstroUtils
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.ColorUtilities
import java.util.Locale

class StarMapSearchDialogFragment : BaseBottomSheetDialogFragment() {

	var onObjectSelected: ((SkyObject) -> Unit)? = null
	private var allObjects: List<SkyObject> = emptyList()
	private val filteredObjects = mutableListOf<SkyObject>()
	private lateinit var adapter: SearchAdapter

	override fun getThemeUsageContext(): ThemeUsageContext = ThemeUsageContext.APP

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return themedInflater.inflate(R.layout.dialog_star_map_search, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val searchView = view.findViewById<SearchView>(R.id.search_view)
		val recyclerView = view.findViewById<RecyclerView>(R.id.search_results)

		adapter = SearchAdapter()
		recyclerView.layoutManager = LinearLayoutManager(context)
		recyclerView.adapter = adapter

		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
			override fun onQueryTextSubmit(query: String?): Boolean {
				filter(query)
				return true
			}

			override fun onQueryTextChange(newText: String?): Boolean {
				filter(newText)
				return true
			}
		})

		filter("")
	}

	fun setObjects(objects: List<SkyObject>) {
		allObjects = objects
		if (isAdded) filter("")
	}

	private fun filter(query: String?) {
		filteredObjects.clear()
		if (query.isNullOrBlank()) {
			filteredObjects.addAll(allObjects.take(20))
		} else {
			val lowerQuery = query.lowercase(Locale.getDefault())
			filteredObjects.addAll(allObjects.filter {
				it.name.lowercase(Locale.getDefault()).contains(lowerQuery)
			})
		}
		adapter.notifyDataSetChanged()
	}

	inner class SearchAdapter : RecyclerView.Adapter<SearchViewHolder>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
			val view = LayoutInflater.from(parent.context).inflate(R.layout.item_star_search, parent, false)
			return SearchViewHolder(view)
		}

		override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
			holder.bind(filteredObjects[position])
		}

		override fun getItemCount(): Int = filteredObjects.size
	}

	inner class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		private val nameText = view.findViewById<TextView>(R.id.object_name)
		private val typeText = view.findViewById<TextView>(R.id.object_type)
		private val magText = view.findViewById<TextView>(R.id.object_magnitude)
		private val iconView = view.findViewById<ImageView>(R.id.object_icon)

		fun bind(obj: SkyObject) {
			nameText.text = obj.name
			typeText.text = AstroUtils.getObjectTypeName(itemView.context, obj.type)
			magText.text = String.format(Locale.getDefault(), "mag %.1f", obj.magnitude)
			
			val iconRes = AstroUtils.getObjectTypeIcon(obj.type)
			val iconColor = if (obj.type.isSunSystem()) obj.color else ColorUtilities.getPrimaryIconColor(itemView.context, nightMode)
			
			iconView.setImageResource(iconRes)
			iconView.setColorFilter(iconColor)

			itemView.setOnClickListener {
				onObjectSelected?.invoke(obj)
				dismiss()
			}
		}
	}

	companion object {
		const val TAG = "StarMapSearchDialog"
	}
}