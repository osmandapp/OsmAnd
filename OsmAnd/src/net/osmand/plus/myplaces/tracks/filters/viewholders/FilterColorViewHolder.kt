package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.filters.ColorTrackFilter
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx
import net.osmand.util.Algorithms

class FilterColorViewHolder(itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val app: OsmandApplication
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val recycler: RecyclerView
	private val titleContainer: View
	private val divider: View
	private val explicitIndicator: ImageView
	private var filter: ColorTrackFilter? = null

	init {
		app = itemView.context.applicationContext as OsmandApplication
		this.nightMode = nightMode
		title = itemView.findViewById(R.id.title)
		selectedValue = itemView.findViewById(R.id.selected_value)
		divider = itemView.findViewById(R.id.divider)
		explicitIndicator = itemView.findViewById(R.id.explicit_indicator)
		titleContainer = itemView.findViewById(R.id.title_container)
		titleContainer.setOnClickListener { _: View? ->
			expanded = !expanded
			updateExpandState()
		}
		recycler = itemView.findViewById(R.id.variants)
	}

	fun bindView(filter: ColorTrackFilter) {
		this.filter = filter
		title.setText(filter.displayNameId)
		updateExpandState()
		updateValues()
	}

	private fun updateExpandState() {
		val iconRes =
			if (expanded) R.drawable.ic_action_arrow_up else R.drawable.ic_action_arrow_down
		explicitIndicator.setImageDrawable(app.uiUtilities.getIcon(iconRes, !nightMode))
		AndroidUiHelper.updateVisibility(recycler, expanded)
	}

	private fun updateValues() {
		filter?.let {
			val adapter = ColorAdapter()
			adapter.items.clear()
			adapter.items.addAll(it.allColors)
			adapter.items.remove("")
			adapter.items.add(0, "")
			recycler.adapter = adapter
			recycler.layoutManager = LinearLayoutManager(app)
			recycler.itemAnimator = null
			updateSelectedValue(it)
		}
	}

	private fun updateSelectedValue(it: ColorTrackFilter): Boolean {
		selectedValue.text = "${it.selectedColors.size}"
		return AndroidUiHelper.updateVisibility(selectedValue, it.selectedColors.size > 0)
	}

	inner class ColorAdapter : RecyclerView.Adapter<ColorViewHolder>() {
		var items = ArrayList<String>()
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
			val inflater = UiUtilities.getInflater(parent.context, nightMode)
			val view =
				inflater.inflate(R.layout.track_filter_checkbox_item, parent, false)
			return ColorViewHolder(view)
		}

		override fun getItemCount(): Int {
			return items.size
		}

		override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
			val colorName = items[position]
			if (Algorithms.isEmpty(colorName)) {
				holder.title.text = app.getString(R.string.not_specified)
				holder.icon.setImageDrawable(app.uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_disabled))
			} else {
				val color = Color.parseColor(colorName)
				holder.title.text = colorName
				val transparencyIcon = getTransparencyIcon(app, color)
				val colorIcon = app.uiUtilities.getPaintedIcon(R.drawable.bg_point_circle, color)
				val layeredIcon = UiUtilities.getLayeredIcon(transparencyIcon, colorIcon)
				holder.icon.setImageDrawable(layeredIcon)
			}
			AndroidUiHelper.updateVisibility(holder.icon, true)
			AndroidUiHelper.updateVisibility(holder.divider, position != itemCount - 1)
			filter?.let { colorFilter ->
				holder.itemView.setOnClickListener {
					colorFilter.setColorSelected(colorName, !colorFilter.isColorSelected(colorName))
					this.notifyItemChanged(position)
					updateSelectedValue(colorFilter)
				}
				holder.checkBox.isChecked = colorFilter.isColorSelected(colorName)
				holder.count.text = colorFilter.getTracksCountForColor(colorName).toString()
			}
		}

		private fun getTransparencyIcon(app: OsmandApplication, @ColorInt color: Int): Drawable? {
			val colorWithoutAlpha = ColorUtilities.removeAlpha(color)
			val transparencyColor = ColorUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f)
			return app.uiUtilities.getPaintedIcon(R.drawable.ic_bg_transparency, transparencyColor)
		}

	}

	inner class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		var title: TextViewEx
		var count: TextViewEx
		var checkBox: AppCompatCheckBox
		var icon: AppCompatImageView
		var divider: View

		init {
			title = view.findViewById(R.id.title)
			count = view.findViewById(R.id.count)
			checkBox = view.findViewById(R.id.compound_button)
			icon = view.findViewById(R.id.icon)
			divider = view.findViewById(R.id.divider)
			UiUtilities.setupCompoundButton(
				nightMode,
				ColorUtilities.getActiveColor(app, nightMode),
				checkBox)
		}
	}
}