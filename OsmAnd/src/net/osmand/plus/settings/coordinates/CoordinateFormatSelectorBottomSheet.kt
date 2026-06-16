package net.osmand.plus.settings.coordinates

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.view.doOnPreDraw
import androidx.core.widget.CompoundButtonCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialModalBottomSheetDialogFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils
import net.osmand.plus.utils.UiUtilities

class CoordinateFormatSelectorBottomSheet : BaseMaterialModalBottomSheetDialogFragment() {

	private lateinit var mainView: View
	private lateinit var requestKey: String
	private lateinit var targetAppMode: ApplicationMode
	private var selectedFormatId: String? = null
	private var showSelectOtherFormat: Boolean = true

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		requestKey = arguments?.getString(ARG_REQUEST_KEY) ?: DEFAULT_REQUEST_KEY
		targetAppMode = ApplicationMode.valueOfStringKey(arguments?.getString(ARG_APP_MODE_KEY), currentAppMode)
			?: currentAppMode
		selectedFormatId = CoordinateFormatIds.normalize(arguments?.getString(ARG_SELECTED_FORMAT_ID))
		showSelectOtherFormat = arguments?.getBoolean(ARG_SHOW_SELECT_OTHER_FORMAT, true) ?: true
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		mainView = inflater.inflate(R.layout.coordinate_format_selector_bottom_sheet, container, false)
		setupHeaderCloseButton(mainView)
		bindFormats()
		bindSelectOtherFormat()
		return mainView
	}

	override fun onStart() {
		super.onStart()
		dialog?.window?.apply {
			addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
			setDimAmount(0.30f)
		}
	}

	override fun shouldSkipCollapsed(): Boolean = false

	override fun initialBottomSheetState(): Int = BottomSheetBehavior.STATE_COLLAPSED

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		if (::mainView.isInitialized) {
			collection.replace(
				InsetTarget.createScrollable(mainView).landscapeSides(InsetsUtils.InsetSide.BOTTOM)
			)
		}
		collection.removeType(InsetTarget.Type.ROOT_INSET)
		return collection
	}

	override fun onBottomSheetReady(
		bottomSheet: FrameLayout,
		behavior: BottomSheetBehavior<FrameLayout>
	) {
		val layoutParams = bottomSheet.layoutParams
		if (layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
			layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
			bottomSheet.layoutParams = layoutParams
		}
		behavior.isFitToContents = true
		behavior.skipCollapsed = false
		bottomSheet.doOnPreDraw {
			val parentHeight = (bottomSheet.parent as? View)?.height ?: bottomSheet.height
			behavior.peekHeight = parentHeight / 2
			behavior.state = BottomSheetBehavior.STATE_COLLAPSED
		}
	}

	override fun getScrollableView(): View? {
		return if (::mainView.isInitialized) {
			mainView.findViewById<NestedScrollView>(R.id.formatSelectorRoot)
		} else {
			null
		}
	}

	private fun bindFormats() {
		val preferences = osmandSettings.coordinateFormatSettingsStorage
		val preferredIds = preferences.getPreferredIds(targetAppMode)
		val selectedId = selectedFormatId ?: preferences.getPrimaryId(targetAppMode)
		val preferredFormats = resolveFormats(preferredIds)
		val recentFormats = resolveFormats(preferences.getRecentIds())

		val preferredContainer = mainView.findViewById<LinearLayout>(R.id.preferredFormatsContainer)
		preferredContainer.removeAllViews()
		preferredFormats.forEachIndexed { index, format ->
			preferredContainer.addView(
				createFormatRow(
					parent = preferredContainer,
					format = format,
					selected = format.id == selectedId,
					showDivider = index == 0 && preferredFormats.size > 1
				)
			)
		}

		val recentContainer = mainView.findViewById<LinearLayout>(R.id.recentFormatsContainer)
		recentContainer.removeAllViews()
		AndroidUiHelper.updateVisibility(recentContainer, recentFormats.isNotEmpty())
		if (recentFormats.isNotEmpty()) {
			recentContainer.addView(createHeader(recentContainer, R.string.coordinate_format_recent))
		}
		recentFormats.forEachIndexed { index, format ->
			recentContainer.addView(
				createFormatRow(
					parent = recentContainer,
					format = format,
					selected = format.id == selectedId,
					showDivider = false
				)
			)
		}
	}

	private fun bindSelectOtherFormat() {
		val action = mainView.findViewById<View>(R.id.selectOtherFormat)
		val divider = mainView.findViewById<View>(R.id.actionDivider)
		AndroidUiHelper.updateVisibility(action, showSelectOtherFormat)
		AndroidUiHelper.updateVisibility(divider, showSelectOtherFormat)
		if (!showSelectOtherFormat) {
			return
		}
		AndroidUtils.setBackground(action, UiUtilities.getSelectableDrawable(action.context))
		action.findViewById<TextView>(R.id.button_text)?.setText(R.string.coordinate_format_select_other)
		action.setOnClickListener {
			parentFragmentManager.setFragmentResult(
				requestKey,
				Bundle().apply { putBoolean(RESULT_SELECT_OTHER_FORMAT, true) }
			)
			dismiss()
		}
	}

	private fun createHeader(parent: ViewGroup, titleRes: Int): View {
		return LinearLayout(parent.context).apply {
			orientation = LinearLayout.VERTICAL
			addView(View(context).apply {
				setBackgroundColor(AndroidUtils.getColorFromAttr(context, R.attr.divider_color))
			}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
			addView(TextView(context).apply {
				setText(titleRes)
				gravity = Gravity.CENTER_VERTICAL
				setTextColor(AndroidUtils.getColorFromAttr(context, android.R.attr.textColorSecondary))
				textSize = 14f
				typeface = android.graphics.Typeface.DEFAULT_BOLD
				setPadding(dp(16), 0, dp(16), 0)
			}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
		}
	}

	private fun createFormatRow(
		parent: ViewGroup,
		format: CoordinateFormat,
		selected: Boolean,
		showDivider: Boolean
	): View {
		val row = layoutInflater.inflate(R.layout.coordinate_format_selector_item, parent, false)
		row.findViewById<TextView>(R.id.title).text = format.title

		val description = getFormatDescription(format)
		val descriptionView = row.findViewById<TextView>(R.id.description)
		val itemContainer = row.findViewById<View>(R.id.itemContainer)
		descriptionView.text = description
		val hasDescription = description.isNotEmpty()
		AndroidUiHelper.updateVisibility(descriptionView, hasDescription)
		if (hasDescription) {
			itemContainer.minimumHeight = dp(64)
			itemContainer.setPadding(
				itemContainer.paddingLeft,
				dp(8),
				itemContainer.paddingRight,
				dp(8)
			)
		} else {
			itemContainer.minimumHeight = dp(48)
			itemContainer.setPadding(
				itemContainer.paddingLeft,
				0,
				itemContainer.paddingRight,
				0
			)
		}

		val radioButton = row.findViewById<AppCompatRadioButton>(R.id.compound_button)
		radioButton.isChecked = selected
		CompoundButtonCompat.setButtonTintList(
			radioButton,
			AndroidUtils.createCheckedColorStateList(
				osmandApp,
				ColorUtilities.getSecondaryIconColorId(nightMode),
				ColorUtilities.getActiveIconColorId(nightMode)
			)
		)
		AndroidUiHelper.updateVisibility(row.findViewById(R.id.divider), showDivider)

		row.setOnClickListener {
			parentFragmentManager.setFragmentResult(
				requestKey,
				Bundle().apply { putString(RESULT_FORMAT_ID, format.id) }
			)
			dismiss()
		}
		return row
	}

	private fun getFormatDescription(format: CoordinateFormat): String {
		return format.epsgCode?.let { "EPSG:$it" }.orEmpty()
	}

	private fun dp(value: Int): Int = AndroidUtils.dpToPx(osmandApp, value.toFloat())

	private fun resolveFormats(ids: List<String>): List<CoordinateFormat> {
		return osmandApp.coordinateFormatHelper.resolveFormats(ids)
	}

	interface FormatSelectionListener {
		fun onFormatSelected(formatId: String)
		fun onSelectOtherFormat()
	}

	companion object {
		val TAG: String = CoordinateFormatSelectorBottomSheet::class.java.simpleName

		const val DEFAULT_REQUEST_KEY = "coordinate_format_selector_request"
		const val RESULT_FORMAT_ID = "coordinate_format_id"
		const val RESULT_SELECT_OTHER_FORMAT = "select_other_format"

		private const val ARG_REQUEST_KEY = "request_key"
		private const val ARG_APP_MODE_KEY = "app_mode_key"
		private const val ARG_SELECTED_FORMAT_ID = "selected_format_id"
		private const val ARG_SHOW_SELECT_OTHER_FORMAT = "show_select_other_format"

		@JvmStatic
		@JvmOverloads
		fun showInstance(
			fragmentManager: FragmentManager,
			requestKey: String = DEFAULT_REQUEST_KEY,
			appMode: ApplicationMode? = null,
			selectedFormatId: String? = null,
			showSelectOtherFormat: Boolean = true
		) {
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
				CoordinateFormatSelectorBottomSheet().apply {
					arguments = Bundle().apply {
						putString(ARG_REQUEST_KEY, requestKey)
						putString(ARG_APP_MODE_KEY, appMode?.stringKey)
						putString(ARG_SELECTED_FORMAT_ID, selectedFormatId)
						putBoolean(ARG_SHOW_SELECT_OTHER_FORMAT, showSelectOtherFormat)
					}
				}.show(fragmentManager, TAG)
			}
		}

		@JvmStatic
		@JvmOverloads
		fun setupResultListener(
			fragmentManager: FragmentManager,
			lifecycleOwner: LifecycleOwner,
			listener: FormatSelectionListener,
			requestKey: String = DEFAULT_REQUEST_KEY
		) {
			fragmentManager.setFragmentResultListener(requestKey, lifecycleOwner) { _, bundle ->
				val formatId = CoordinateFormatIds.normalize(bundle.getString(RESULT_FORMAT_ID))
				if (formatId != null) {
					listener.onFormatSelected(formatId)
				} else if (bundle.getBoolean(RESULT_SELECT_OTHER_FORMAT)) {
					listener.onSelectOtherFormat()
				}
			}
		}
	}
}
