package net.osmand.telegram.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.ui.views.BottomSheetDialog


class SortByBottomSheet : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val app = activity?.application as TelegramApplication
		val mainView = inflater.inflate(R.layout.bottom_sheet_sort_by, container, false)

		mainView.findViewById<View>(R.id.scroll_view_container).setOnClickListener { dismiss() }

		BottomSheetBehavior.from(mainView.findViewById<View>(R.id.scroll_view))
			.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						dismiss()
					}
				}

				override fun onSlide(bottomSheet: View, slideOffset: Float) {}
			})


		val itemsCont = mainView.findViewById<ViewGroup>(R.id.items_container)
		for (sortType in TelegramSettings.LiveNowSortType.values()) {
			inflater.inflate(R.layout.item_with_rb_and_btn, itemsCont, false).apply {
				val currentType = sortType == app.settings.liveNowSortType
				val image = if (currentType) {
					app.uiUtils.getActiveIcon(sortType.iconId)
				} else {
					app.uiUtils.getThemedIcon(sortType.iconId)
				}
				findViewById<ImageView>(R.id.icon).setImageDrawable(image)
				findViewById<TextView>(R.id.title)?.apply {
					text = getText(sortType.titleId)
					val colorId = if (currentType) R.color.ctrl_active_light else R.color.primary_text_light
					setTextColor(ContextCompat.getColor(app, colorId))
				}
				findViewById<View>(R.id.primary_btn_container).visibility = View.GONE
				findViewById<View>(R.id.radio_button).visibility = View.GONE
				setOnClickListener {
					app.settings.liveNowSortType = sortType
					targetFragment?.also { target ->
						target.onActivityResult(targetRequestCode, SORTING_CHANGED_REQUEST_CODE, null)
					}
					dismiss()
				}
				itemsCont.addView(this)
			}
		}

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_cancel)
			setOnClickListener { dismiss() }
		}

		return mainView
	}

	companion object {

		const val SORTING_CHANGED_REQUEST_CODE = 3

		private const val TAG = "SortByBottomSheet"

		fun showInstance(
			fm: androidx.fragment.app.FragmentManager,
			target: androidx.fragment.app.Fragment
		): Boolean {
			return try {
				SortByBottomSheet().apply {
					setTargetFragment(target, SORTING_CHANGED_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}