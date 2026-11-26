package net.osmand.plus.widgets.alert

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import net.osmand.plus.base.BaseAlertDialogFragment
import net.osmand.plus.utils.AndroidUtils

/**
 * Single choice dialog of [CustomAlert.showSingleSelection] that survives activity recreation
 * (screen rotation, process death). It is shown in the child fragment manager of the host fragment
 * and returns the index of the chosen item as a fragment result, see [setResultListener].
 */
class SingleSelectionDialogFragment : BaseAlertDialogFragment() {

	override fun resolveNightMode(): Boolean {
		return requireArguments().getBoolean(NIGHT_MODE_KEY)
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val args = requireArguments()
		val requestKey = args.getString(REQUEST_KEY_KEY)!!
		val data = AlertDialogData(requireContext(), nightMode)
		val titleId = args.getInt(TITLE_ID_KEY)
		if (titleId != 0) {
			data.setTitle(titleId)
		} else {
			data.setTitle(args.getString(TITLE_KEY))
		}
		if (args.containsKey(CONTROLS_COLOR_KEY)) {
			data.setControlsColor(args.getInt(CONTROLS_COLOR_KEY))
		}
		if (args.containsKey(ITEMS_LAYOUT_KEY)) {
			data.setItemsLayoutRes(args.getInt(ITEMS_LAYOUT_KEY))
		}
		val titles = args.getCharSequenceArray(ITEM_TITLES_KEY)!!
		val descriptions = args.getCharSequenceArray(ITEM_DESCRIPTIONS_KEY)
		val items = Array(titles.size) { i -> SelectionDialogItem(titles[i], descriptions?.get(i)) }

		return CustomAlert.createSingleSelection(data, items, args.getInt(SELECTED_INDEX_KEY)) { v ->
			val result = Bundle().apply { putInt(SELECTED_INDEX_KEY, v.tag as Int) }
			parentFragmentManager.setFragmentResult(requestKey, result)
		}
	}

	fun interface OnItemSelectedListener {
		fun onItemSelected(index: Int)
	}

	companion object {

		private val TAG = SingleSelectionDialogFragment::class.java.simpleName

		private const val REQUEST_KEY_KEY = "request_key"
		private const val NIGHT_MODE_KEY = "night_mode"
		private const val TITLE_KEY = "title"
		private const val TITLE_ID_KEY = "title_id"
		private const val CONTROLS_COLOR_KEY = "controls_color"
		private const val ITEMS_LAYOUT_KEY = "items_layout"
		private const val ITEM_TITLES_KEY = "item_titles"
		private const val ITEM_DESCRIPTIONS_KEY = "item_descriptions"
		private const val SELECTED_INDEX_KEY = "selected_index"

		/**
		 * Only the title, controls color and items layout of [data] are used:
		 * buttons, icon and listeners cannot be restored after activity recreation.
		 */
		@JvmStatic
		fun showInstance(
			host: Fragment, requestKey: String, data: AlertDialogData,
			items: Array<out CharSequence>, selectedIndex: Int
		) {
			showInstance(host, requestKey, data, items, null, selectedIndex)
		}

		@JvmStatic
		fun showInstance(
			host: Fragment, requestKey: String, data: AlertDialogData,
			items: Array<SelectionDialogItem>, selectedIndex: Int
		) {
			val titles = Array(items.size) { i -> items[i].title }
			val descriptions = Array(items.size) { i -> items[i].description }
			showInstance(host, requestKey, data, titles, descriptions, selectedIndex)
		}

		private fun showInstance(
			host: Fragment, requestKey: String, data: AlertDialogData,
			titles: Array<out CharSequence>, descriptions: Array<CharSequence?>?, selectedIndex: Int
		) {
			val manager = host.childFragmentManager
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
				val args = Bundle().apply {
					putString(REQUEST_KEY_KEY, requestKey)
					putBoolean(NIGHT_MODE_KEY, data.isNightMode)
					data.titleId?.let { putInt(TITLE_ID_KEY, it) }
					data.title?.let { putString(TITLE_KEY, it) }
					data.controlsColor?.let { putInt(CONTROLS_COLOR_KEY, it) }
					data.itemsLayoutRes?.let { putInt(ITEMS_LAYOUT_KEY, it) }
					putCharSequenceArray(ITEM_TITLES_KEY, arrayOf(*titles))
					putCharSequenceArray(ITEM_DESCRIPTIONS_KEY, descriptions)
					putInt(SELECTED_INDEX_KEY, selectedIndex)
				}
				val fragment = SingleSelectionDialogFragment()
				fragment.arguments = args
				fragment.showNow(manager, TAG)
			}
		}

		/**
		 * Registers the listener of the dialog shown by [showInstance] with the same host and request key.
		 * Call it on every creation of the host (e.g. in onCreate or onCreateView), so the choice made
		 * in a dialog restored after rotation is delivered too.
		 */
		@JvmStatic
		fun setResultListener(host: Fragment, requestKey: String, listener: OnItemSelectedListener) {
			host.childFragmentManager.setFragmentResultListener(requestKey, host) { _, result ->
				listener.onItemSelected(result.getInt(SELECTED_INDEX_KEY))
			}
		}
	}
}
