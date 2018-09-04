package net.osmand.telegram.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.ui.views.BottomSheetDialog
import net.osmand.telegram.utils.AndroidUtils


const val SORT_BY_GROUP = 0
const val SORT_BY_NAME = 1
const val SORT_BY_DISTANCE = 2

const val SORT_BY_KEY = "sort_by_key"

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
		val itemsCont = mainView.findViewById<ViewGroup>(R.id.items_container)

		inflater.inflate(R.layout.item_with_rb_and_btn, itemsCont, false).apply {
			findViewById<ImageView>(R.id.icon).setImageDrawable(getSelectedIcon(app, R.drawable.ic_action_sort_by_distance))
			findViewById<TextView>(R.id.title).apply {
				text = getText(R.string.shared_string_distance)
				setTextColor(getTextColorStateList(context))
			}
			findViewById<View>(R.id.primary_btn_container).visibility = View.GONE
			findViewById<View>(R.id.radio_button).visibility = View.GONE
			setOnClickListener {
				intent.putExtra(SORT_BY_KEY, SORT_BY_DISTANCE)
				targetFragment?.also { target ->
					target.onActivityResult(targetRequestCode, SORT_BY_REQUEST_CODE, intent)
				}
				dismiss()
			}
			itemsCont.addView(this)
		}

		inflater.inflate(R.layout.item_with_rb_and_btn, itemsCont, false).apply {
			findViewById<ImageView>(R.id.icon).setImageDrawable(getSelectedIcon(app, R.drawable.ic_action_sort_by_name))
			findViewById<TextView>(R.id.title).apply {
				text = getText(R.string.shared_string_name)
				setTextColor(getTextColorStateList(context))
			}
			findViewById<View>(R.id.primary_btn_container).visibility = View.GONE
			findViewById<View>(R.id.radio_button).visibility = View.GONE
			setOnClickListener {
				intent.putExtra(SORT_BY_KEY, SORT_BY_NAME)
				targetFragment?.also { target ->
					target.onActivityResult(targetRequestCode, SORT_BY_REQUEST_CODE, intent)
				}
				dismiss()
			}
			itemsCont.addView(this)
		}

		inflater.inflate(R.layout.item_with_rb_and_btn, itemsCont, false).apply {
			findViewById<ImageView>(R.id.icon).setImageDrawable(getSelectedIcon(app, R.drawable.ic_action_sort_by_group))
			findViewById<TextView>(R.id.title).apply {
				text = getText(R.string.shared_string_group)
				setTextColor(getTextColorStateList(context))
			}
			findViewById<View>(R.id.primary_btn_container).visibility = View.GONE
			findViewById<View>(R.id.radio_button).visibility = View.GONE
			setOnClickListener {
				intent.putExtra(SORT_BY_KEY, SORT_BY_GROUP)
				targetFragment?.also { target ->
					target.onActivityResult(targetRequestCode, SORT_BY_REQUEST_CODE, intent)
				}
				dismiss()
			}
			itemsCont.addView(this)
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

	private fun getTextColorStateList(context: Context): ColorStateList {
		return AndroidUtils.createPressedColorStateList(
			context,
			R.color.primary_text_light,
			R.color.ctrl_active_light
		)
	}

	private fun getSelectedIcon(app: TelegramApplication, @DrawableRes id: Int): Drawable? {
		val normal = app.uiUtils.getThemedIcon(id)
		if (Build.VERSION.SDK_INT >= 21) {
			val active = app.uiUtils.getActiveIcon(id)
			if (normal != null && active != null) {
				return AndroidUtils.createPressedStateListDrawable(normal, active)
			}
		}
		return normal
	}

	companion object {

		const val SORT_BY_REQUEST_CODE = 3

		private const val TAG = "SortByBottomSheet"

		fun showInstance(fm: FragmentManager, target: Fragment): Boolean {
			return try {
				SortByBottomSheet().apply {
					setTargetFragment(target, SORT_BY_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}