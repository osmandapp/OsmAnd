package net.osmand.plus.widgets.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.google.android.material.materialswitch.MaterialSwitch
import net.osmand.plus.R

/**
 * Binds a row of a segmented settings list ([R.layout.item_ui_setting_row]).
 * A row without an icon starts its text at the same 16dp as the group header above it.
 */
class SettingRow(val view: View) {

	private val iconView: ImageView = view.findViewById(R.id.icon)
	private val titleView: TextView = view.findViewById(R.id.title)
	private val subtitleView: TextView = view.findViewById(R.id.subtitle)
	private val switchView: MaterialSwitch = view.findViewById(R.id.switch_widget)

	fun setIcon(@DrawableRes iconId: Int?, @ColorInt tint: Int? = null) {
		if (iconId == null) {
			iconView.visibility = View.GONE
		} else {
			iconView.visibility = View.VISIBLE
			iconView.setImageResource(iconId)
			tint?.let { iconView.setColorFilter(it) } ?: iconView.clearColorFilter()
		}
	}

	fun setTitle(title: CharSequence?) {
		titleView.text = title
	}

	fun setTitle(titleId: Int) {
		titleView.setText(titleId)
	}

	fun setTitleColor(@ColorInt color: Int) {
		titleView.setTextColor(color)
	}

	fun setSubtitle(subtitle: CharSequence?) {
		subtitleView.text = subtitle
		subtitleView.visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
	}

	fun setSubtitleColor(@ColorInt color: Int) {
		subtitleView.setTextColor(color)
	}

	fun setChecked(checked: Boolean) {
		switchView.isChecked = checked
		switchView.visibility = View.VISIBLE
	}

	fun hideSwitch() {
		switchView.visibility = View.GONE
	}

	fun setRowEnabled(enabled: Boolean) {
		view.isEnabled = enabled
		view.isClickable = enabled
		switchView.isEnabled = enabled
	}

	fun setOnClickListener(listener: View.OnClickListener?) {
		view.setOnClickListener(listener)
	}

	fun setContentDescription(description: CharSequence?) {
		view.contentDescription = description
	}
}
