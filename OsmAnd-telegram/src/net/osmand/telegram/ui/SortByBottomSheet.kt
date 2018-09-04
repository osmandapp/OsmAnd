package net.osmand.telegram.ui

import android.content.Intent
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.ui.views.BottomSheetDialog


class SortByBottomSheet : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val app = activity?.application as TelegramApplication
		val currentSortType = arguments?.getString(CURRENT_SORT_TYPE_KEY)
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
		for (sortType in SortType.values()) {
			inflater.inflate(R.layout.item_with_rb_and_btn, itemsCont, false).apply {
				val currentType = sortType.name == currentSortType
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
					app.settings.sortType = sortType.name
					val intent = Intent().apply {
						putExtra(SORT_BY_KEY, sortType.name)
					}
					targetFragment?.also { target ->
						target.onActivityResult(targetRequestCode, SORT_BY_REQUEST_CODE, intent)
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

		const val SORT_BY_REQUEST_CODE = 3

		const val SORT_BY_KEY = "sort_by_key"
		const val CURRENT_SORT_TYPE_KEY = "current_sort_type_key"

		private const val TAG = "SortByBottomSheet"

		fun showInstance(
			fm: FragmentManager,
			target: Fragment,
			currentSortType: SortType
		): Boolean {
			return try {
				SortByBottomSheet().apply {
					val bundle = Bundle()
					bundle.putString(CURRENT_SORT_TYPE_KEY, currentSortType.name)
					setTargetFragment(target, SORT_BY_REQUEST_CODE)
					arguments = bundle
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}

	enum class SortType(@DrawableRes val iconId: Int, @StringRes val titleId: Int, @StringRes val shortTitleId: Int) {
		SORT_BY_GROUP(
			R.drawable.ic_action_sort_by_group,
			R.string.shared_string_group,
			R.string.by_group
		),
		SORT_BY_NAME(
			R.drawable.ic_action_sort_by_name,
			R.string.shared_string_name,
			R.string.by_name
		),
		SORT_BY_DISTANCE(
			R.drawable.ic_action_sort_by_distance,
			R.string.shared_string_distance,
			R.string.by_distance
		);

		fun isSortByGroup() = this == SORT_BY_GROUP
	}
}