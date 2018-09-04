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


const val SORT_BY_KEY = "sort_by_key"

const val CURRENT_SORT_TYPE_KEY = "current_sort_type_key"

class SortByBottomSheet : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_sort_by, container, false)
		val intent = Intent()
		val app = activity?.application as TelegramApplication
		val currentSortType = arguments?.getString(CURRENT_SORT_TYPE_KEY)
		val itemsCont = mainView.findViewById<ViewGroup>(R.id.items_container)

		for (sortType in SortType.values()) {
			inflater.inflate(R.layout.item_with_rb_and_btn, itemsCont, false).apply {
				val isCurrentSortType = sortType.name == currentSortType
				val image = if (isCurrentSortType) {
					app.uiUtils.getActiveIcon(sortType.iconId)
				} else {
					app.uiUtils.getThemedIcon(sortType.iconId)
				}
				findViewById<ImageView>(R.id.icon).setImageDrawable(image)
				findViewById<TextView>(R.id.title)?.apply {
					text = getText(sortType.titleId)
					setTextColor(
						ContextCompat.getColor(
							app,
							if (isCurrentSortType) R.color.ctrl_active_light else R.color.primary_text_light
						)
					)
				}
				findViewById<View>(R.id.primary_btn_container).visibility = View.GONE
				findViewById<View>(R.id.radio_button).visibility = View.GONE
				setOnClickListener {
					intent.putExtra(SORT_BY_KEY, sortType.name)
					targetFragment?.also { target ->
						target.onActivityResult(targetRequestCode, SORT_BY_REQUEST_CODE, intent)
					}
					dismiss()
				}
				itemsCont.addView(this)
			}
		}

		BottomSheetBehavior.from(mainView.findViewById<View>(R.id.scroll_view))
			.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						dismiss()
					}
				}

				override fun onSlide(bottomSheet: View, slideOffset: Float) {}
			})

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_cancel)
			setOnClickListener { dismiss() }
		}

		return mainView
	}

	companion object {

		const val SORT_BY_REQUEST_CODE = 3

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

	enum class SortType(@DrawableRes val iconId: Int, @StringRes val titleId: Int) {
		SORT_BY_GROUP(R.drawable.ic_action_sort_by_group, R.string.shared_string_group),
		SORT_BY_NAME(R.drawable.ic_action_sort_by_name, R.string.shared_string_name),
		SORT_BY_DISTANCE(R.drawable.ic_action_sort_by_distance, R.string.shared_string_distance);
	}
}