package net.osmand.plus.download.freeregion

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.osmand.map.WorldRegion
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialModalBottomSheetDialogFragment
import net.osmand.plus.chooseplan.ChoosePlanFragment
import net.osmand.plus.chooseplan.OsmAndFeature
import net.osmand.plus.download.DownloadValidationManager
import net.osmand.plus.download.IndexItem
import net.osmand.plus.download.local.LocalItem
import net.osmand.plus.download.local.LocalOperationTask
import net.osmand.plus.download.local.OperationType
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils

/**
 * Free version: a map of another region. One sheet goes through the steps - replace, then what the
 * server answers: replace now, one more replace for an email, or only Maps+.
 */
class FreeRegionBottomSheet : BaseMaterialModalBottomSheetDialogFragment() {

	private enum class Step { REPLACE, EMAIL, BUY }

	private var step = Step.REPLACE
	private var loading = false

	/** Continues the download that opened the sheet; lost when the sheet is recreated. */
	private var onReplaced: Runnable? = null

	private lateinit var freeRegion: WorldRegion
	private lateinit var region: WorldRegion
	private lateinit var mapsToDelete: Map<WorldRegion, List<LocalItem>>
	private lateinit var mainView: View

	private val freeVersionMapDownloads: FreeVersionMapDownloads
		get() = osmandApp.downloadThread.freeVersionMapDownloads

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val regions = osmandApp.regions
		val from = regions.getRegionData(requireArguments().getString(FREE_REGION_KEY))
		val to = regions.getRegionData(requireArguments().getString(REGION_KEY))
		if (from == null || to == null) {
			dismissAllowingStateLoss()
			return
		}
		freeRegion = from
		region = to
		mapsToDelete = freeVersionMapDownloads.collectMapsToDelete(region)
		savedInstanceState?.getString(STEP_KEY)?.let { step = Step.valueOf(it) }
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		if (!::region.isInitialized) {
			return null
		}
		mainView = inflater.inflate(R.layout.bottom_sheet_free_region, container, false)
		setupMapsToDelete(inflater)
		setupEmailField()
		mainView.findViewById<View>(R.id.get_maps_plus_button).setOnClickListener {
			dismiss()
			ChoosePlanFragment.showInstance(requireActivity(), OsmAndFeature.UNLIMITED_MAP_DOWNLOADS)
		}
		mainView.findViewById<View>(R.id.cancel_button).setOnClickListener { dismiss() }
		mainView.findViewById<View>(R.id.replace_button).setOnClickListener {
			if (step == Step.EMAIL) sendEmail() else requestReplace()
		}
		updateStep()
		return mainView
	}

	override fun onStart() {
		super.onStart()
		dialog?.window?.apply {
			addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
			setDimAmount(0.30f)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString(STEP_KEY, step.name)
	}

	private fun setupMapsToDelete(inflater: LayoutInflater) {
		val list = mainView.findViewById<LinearLayout>(R.id.maps_list)
		for ((deleted, items) in mapsToDelete) {
			for (item in items) {
				val row = inflater.inflate(R.layout.item_free_region_map, list, false)
				row.findViewById<TextView>(R.id.name).text = deleted.localeName
				row.findViewById<TextView>(R.id.description).text = getString(
					R.string.ltr_or_rtl_combine_via_bold_point,
					getString(item.type.titleId), AndroidUtils.formatSize(osmandApp, item.size))
				list.addView(row)
			}
		}
		mainView.findViewById<TextView>(R.id.maps_to_delete_size).text = getMapsToDeleteSize()
	}

	private fun getMapsToDeleteSize(): String {
		return AndroidUtils.formatSize(osmandApp, mapsToDelete.values.flatten().sumOf { it.size })
	}

	private fun setupEmailField() {
		val emailLayout = mainView.findViewById<TextInputLayout>(R.id.email_layout)
		val emailEdit = mainView.findViewById<TextInputEditText>(R.id.email_edit)
		emailEdit.setOnFocusChangeListener { _, hasFocus ->
			if (!hasFocus && getEmail().isNotEmpty() && !AndroidUtils.isValidEmail(getEmail())) {
				emailLayout.error = getString(R.string.osm_live_enter_email)
			}
		}
		emailEdit.doAfterTextChanged {
			if (emailLayout.error != null && AndroidUtils.isValidEmail(getEmail())) {
				emailLayout.error = null
			}
		}
	}

	private fun getEmail(): String {
		return mainView.findViewById<TextInputEditText>(R.id.email_edit).text?.toString()?.trim().orEmpty()
	}

	private fun setStep(step: Step) {
		this.step = step
		updateStep()
	}

	private fun updateStep() {
		val title = mainView.findViewById<TextView>(R.id.title)
		val description = mainView.findViewById<TextView>(R.id.description)
		val replaceButton = mainView.findViewById<MaterialButton>(R.id.replace_button)
		when (step) {
			Step.REPLACE -> {
				title.text = getString(R.string.free_region_other_title, region.localeName)
				description.text = getString(R.string.free_region_other_descr, region.localeName)
				replaceButton.setText(R.string.free_region_delete_and_download)
			}
			Step.EMAIL -> {
				title.setText(R.string.free_region_replace_used_title)
				description.setText(R.string.free_region_not_eligible_email_descr)
				replaceButton.setText(R.string.free_region_send_and_replace)
				val names = mapsToDelete.keys.map { it.localeName }
				mainView.findViewById<TextView>(R.id.email_maps_note).text = getString(
					R.string.free_region_email_maps_deleted, TextUtils.join(", ", names), getMapsToDeleteSize())
			}
			Step.BUY -> {
				title.setText(R.string.free_region_replace_used_title)
				description.text = getString(R.string.free_region_not_eligible_buy_descr,
					freeRegion.localeName, region.localeName)
			}
		}
		AndroidUiHelper.setVisibility(step == Step.REPLACE && mapsToDelete.isNotEmpty(),
			mainView.findViewById(R.id.maps_to_delete))
		AndroidUiHelper.setVisibility(step == Step.EMAIL, mainView.findViewById(R.id.email_layout))
		AndroidUiHelper.setVisibility(step == Step.EMAIL && mapsToDelete.isNotEmpty(),
			mainView.findViewById(R.id.email_maps_note))
		AndroidUiHelper.setVisibility(step != Step.BUY, replaceButton)
		AndroidUiHelper.setVisibility(step == Step.BUY, mainView.findViewById(R.id.cancel_button))
		updateLoading()
	}

	/** While the server answers, the buttons are disabled and the pressed one shows a spinner. */
	private fun setLoading(loading: Boolean) {
		this.loading = loading
		updateLoading()
	}

	private fun updateLoading() {
		val replaceButton = mainView.findViewById<MaterialButton>(R.id.replace_button)
		mainView.findViewById<View>(R.id.get_maps_plus_button).isEnabled = !loading
		mainView.findViewById<View>(R.id.email_layout).isEnabled = !loading
		replaceButton.isEnabled = !loading
		replaceButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
		replaceButton.icon = if (loading) createProgressDrawable(replaceButton) else null
		isCancelable = !loading
	}

	private fun createProgressDrawable(button: MaterialButton): IndeterminateDrawable<*> {
		val context = button.context
		val spec = CircularProgressIndicatorSpec(context, null, 0,
			com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall)
		spec.indicatorColors = intArrayOf(button.currentTextColor)
		return IndeterminateDrawable.createCircularDrawable(context, spec)
	}

	private fun requestReplace() {
		setLoading(true)
		freeVersionMapDownloads.requestReplace(freeRegion, region) { answer ->
			if (!isAdded) {
				return@requestReplace
			}
			setLoading(false)
			when (answer) {
				ReplaceAnswer.OK -> applyReplace()
				// the email is already left - the server must not ask again, so only Maps+
				ReplaceAnswer.REQUEST_EMAIL ->
					setStep(if (osmandSettings.EMAIL_SUBSCRIBED.get()) Step.BUY else Step.EMAIL)
				ReplaceAnswer.NO -> setStep(Step.BUY)
				null -> osmandApp.showShortToastMessage(R.string.no_inet_connection)
			}
		}
	}

	private fun sendEmail() {
		val emailLayout = mainView.findViewById<TextInputLayout>(R.id.email_layout)
		if (!AndroidUtils.isValidEmail(getEmail())) {
			emailLayout.error = getString(R.string.osm_live_enter_email)
			return
		}
		setLoading(true)
		freeVersionMapDownloads.registerEmail(getEmail()) { registered ->
			if (!isAdded) {
				return@registerEmail
			}
			if (registered) {
				// ask again - now with the email left
				requestReplace()
			} else {
				setLoading(false)
				osmandApp.showShortToastMessage(R.string.shared_string_unexpected_error)
			}
		}
	}

	private fun applyReplace() {
		val app = osmandApp
		val activity = requireActivity()
		val continueDownload = onReplaced ?: Runnable { restartDownload(activity) }
		// the server may answer after the app went to the background
		dismissAllowingStateLoss()
		freeVersionMapDownloads.replaceFreeRegion(region)
		val items = mapsToDelete.values.flatten()
		if (items.isEmpty()) {
			continueDownload.run()
			return
		}
		val listener = object : LocalOperationTask.OperationListener {
			override fun onOperationFinished(type: OperationType, result: String) {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					continueDownload.run()
				}
			}
		}
		OsmAndTaskManager.executeTask(
			LocalOperationTask(app, OperationType.DELETE_OPERATION, listener), *items.toTypedArray())
	}

	/** The sheet was recreated and lost the pending download: start it again by the file names. */
	private fun restartDownload(activity: FragmentActivity) {
		val app = activity.application as OsmandApplication
		val indexes = app.downloadThread.indexes
		val fileNames = requireArguments().getStringArrayList(ITEMS_KEY).orEmpty()
		val items = fileNames.mapNotNull { indexes.getIndexItem(it) }
		if (items.isNotEmpty() && AndroidUtils.isActivityNotDestroyed(activity)) {
			DownloadValidationManager(app).startDownload(activity, *items.toTypedArray())
		}
	}

	override fun initialBottomSheetState(): Int = BottomSheetBehavior.STATE_EXPANDED

	override fun shouldSkipCollapsed(): Boolean = true

	override fun onBottomSheetReady(bottomSheet: FrameLayout, behavior: BottomSheetBehavior<FrameLayout>) {
		behavior.isFitToContents = true
	}

	override fun getScrollableView(): View? {
		return if (::mainView.isInitialized) mainView else null
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		if (::mainView.isInitialized) {
			collection.add(
				InsetTarget.createCustomBuilder(R.id.bottomSheetContent)
					.portraitSides(InsetsUtils.InsetSide.BOTTOM)
					.landscapeSides(InsetsUtils.InsetSide.BOTTOM)
					.applyPadding(true)
			)
		}
		collection.removeType(InsetTarget.Type.ROOT_INSET)
		return collection
	}

	companion object {
		private val TAG = FreeRegionBottomSheet::class.java.simpleName
		private const val FREE_REGION_KEY = "free_region"
		private const val REGION_KEY = "region"
		private const val ITEMS_KEY = "items"
		private const val STEP_KEY = "step"

		/**
		 * @param items the download that opened the sheet, to start it again after the sheet is recreated
		 * @param onReplaced continues the download after the replace
		 */
		@JvmStatic
		fun showInstance(activity: FragmentActivity, freeRegion: WorldRegion, region: WorldRegion,
		                 items: Array<IndexItem>, onReplaced: Runnable) {
			val manager = activity.supportFragmentManager
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
				val fragment = FreeRegionBottomSheet()
				fragment.arguments = Bundle().apply {
					putString(FREE_REGION_KEY, freeRegion.regionId)
					putString(REGION_KEY, region.regionId)
					putStringArrayList(ITEMS_KEY, ArrayList(items.map { it.fileName }))
				}
				fragment.onReplaced = onReplaced
				fragment.show(manager, TAG)
			}
		}
	}
}
