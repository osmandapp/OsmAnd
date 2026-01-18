package net.osmand.plus.plugins.astro

import android.annotation.SuppressLint
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
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.plugins.astro.utils.AstroUtils
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import java.util.Locale

class StarMapSearchDialogFragment : BaseFullScreenDialogFragment() {

	var onObjectSelected: ((SkyObject) -> Unit)? = null
	private var allObjects: List<SkyObject> = emptyList()
	private val filteredObjects = mutableListOf<SkyObject>()
	private lateinit var adapter: SearchAdapter
	private var currentQuery: String = ""
	private var isAscending = true

	override fun getThemeUsageContext(): ThemeUsageContext = ThemeUsageContext.APP

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return themedInflater.inflate(R.layout.dialog_star_map_search, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val searchView = view.findViewById<SearchView>(R.id.search_view)
		val recyclerView = view.findViewById<RecyclerView>(R.id.search_results)
		val closeButton = view.findViewById<View>(R.id.close_button)
		val sortBar = view.findViewById<View>(R.id.sort_bar)
		val sortIcon = view.findViewById<ImageView>(R.id.sort_icon)
		val sortText = view.findViewById<TextView>(R.id.sort_text)

		adapter = SearchAdapter()
		recyclerView.layoutManager = LinearLayoutManager(context)
		recyclerView.adapter = adapter

		closeButton.setOnClickListener {
			dismiss()
		}

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

		sortBar.setOnClickListener {
			isAscending = !isAscending
			val iconColorId = ColorUtilities.getActiveIconColorId(nightMode)
			val icon = app.uiUtilities.getIcon(
				if (isAscending) R.drawable.ic_action_sort_by_name_ascending
				else R.drawable.ic_action_sort_by_name_descending, iconColorId)
			sortIcon.setImageDrawable(icon)
			sortText.setText(if (isAscending) R.string.sort_name_ascending else R.string.sort_name_descending)
			filter(currentQuery)
		}

		recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					AndroidUtils.hideSoftKeyboard(requireActivity(), searchView)
				}
			}
		})

		searchView.requestFocus()
		filter("")
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		collection.add(InsetTarget.createScrollable(R.id.search_results))
		return collection
	}

	fun setObjects(objects: List<SkyObject>) {
		allObjects = objects
		if (isAdded) filter(currentQuery)
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun filter(query: String?) {
		currentQuery = query ?: ""
		filteredObjects.clear()
		val list = if (currentQuery.isBlank()) {
			allObjects
		} else {
			val lowerQuery = currentQuery.lowercase(Locale.getDefault())
			allObjects.filter {
				it.name.lowercase(Locale.getDefault()).contains(lowerQuery)
			}
		}
		val sortedList = if (isAscending) {
			list.sortedBy { it.name }
		} else {
			list.sortedByDescending { it.name }
		}
		filteredObjects.addAll(if (currentQuery.isBlank()) sortedList else sortedList)
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
		private val infoText = view.findViewById<TextView>(R.id.object_info)
		private val iconView = view.findViewById<ImageView>(R.id.object_icon)

		fun bind(obj: SkyObject) {
			nameText.text = obj.name
			
			val typeName = AstroUtils.getObjectTypeName(itemView.context, obj.type)
			val magStr = String.format(Locale.getDefault(), "mag %.1f", obj.magnitude)
			if (obj.type.isSunSystem()) {
				infoText.text = String.format(Locale.getDefault(), "%s • %s", typeName, magStr)
			} else {
				val raStr = formatRA(obj.ra)
				val decStr = formatDec(obj.dec)
				infoText.text = String.format(Locale.getDefault(), "%s • %s, %s • %s", typeName, raStr, decStr, magStr)
			}

			val iconRes = AstroUtils.getObjectTypeIcon(obj.type)
			val iconColor = if (obj.type.isSunSystem()) obj.color else ColorUtilities.getPrimaryIconColor(itemView.context, nightMode)
			
			iconView.setImageResource(iconRes)
			iconView.setColorFilter(iconColor)

			itemView.setOnClickListener {
				onObjectSelected?.invoke(obj)
				dismiss()
			}
		}

		private fun formatRA(ra: Double): String {
			val hours = ra / 15.0
			val h = hours.toInt()
			val m = ((hours - h) * 60).toInt()
			return String.format(Locale.getDefault(), "%02dh %02dm", h, m)
		}

		private fun formatDec(dec: Double): String {
			val d = dec.toInt()
			val m = Math.abs((dec - d) * 60).toInt()
			return String.format(Locale.getDefault(), "%+02d° %02d′", d, m)
		}
	}

	companion object {
		const val TAG = "StarMapSearchDialog"
	}
}
