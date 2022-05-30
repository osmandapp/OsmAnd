package net.osmand.telegram.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.utils.AndroidNetworkUtils
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.DataConstants
import net.osmand.telegram.utils.OsmandApiUtils
import org.json.JSONObject
import studio.carbonylgroup.textfieldboxes.ExtendedEditText


class LoginDialogFragment : BaseDialogFragment() {

	companion object {

		private const val TAG = "LoginDialogFragment"
		private val log = PlatformUtil.getLog(LoginDialogFragment::class.java)

		private const val LOGIN_DIALOG_TYPE_PARAM_KEY = "login_dialog_type_param"
		private const val SHOW_PROGRESS_PARAM_KEY = "show_progress_param"
		private const val SHOW_WELCOME_DIALOG_PARAM_KEY = "show_welcome_dialog_param"
		private const val PRIVACY_POLICY_AGREED_PARAM_KEY = "privacy_policy_agreed_param"
		private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
		private const val TELEGRAM_PRIVACY_POLICY = "https://telegram.org/privacy"
		private const val OSMAND_PRIVACY_POLICY = "https://osmand.net/help-online/privacy-policy"
		private const val SOFT_KEYBOARD_MIN_DETECTION_SIZE = 0.15

		var welcomeDialogShown = false
			private set

		private var softKeyboardShown: Boolean = false

		private var countryPhoneCode: String = "+"

		fun showWelcomeDialog(fragmentManager: androidx.fragment.app.FragmentManager) {
			welcomeDialogShown = true
			showDialog(fragmentManager, welcomeDialog = true)
		}

		fun showDialog(fragmentManager: androidx.fragment.app.FragmentManager, loginDialogType: LoginDialogType? = null, welcomeDialog: Boolean = false, privacyPolicyAgreed: Boolean = false) {
			try {

				/*
				mapActivity.getSupportFragmentManager().beginTransaction()
						.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
						.add(R.id.fragmentContainer, fragment, TAG)
						.addToBackStack(TAG).commitAllowingStateLoss()
						*/

				var fragment = getFragment(fragmentManager)
				if (fragment == null) {
					fragment = LoginDialogFragment()
					val args = Bundle()
					if (loginDialogType != null) {
						args.putString(LOGIN_DIALOG_TYPE_PARAM_KEY, loginDialogType.name)
					}
					args.putBoolean(SHOW_WELCOME_DIALOG_PARAM_KEY, welcomeDialog)
					args.putBoolean(PRIVACY_POLICY_AGREED_PARAM_KEY, privacyPolicyAgreed)
					fragment.arguments = args
					fragment.show(fragmentManager, TAG)
				} else {
					var showWelcomeDialog = welcomeDialog
					if (fragment.showWelcomeDialog) {
						showWelcomeDialog = fragment.showWelcomeDialog
					}
					fragment.updateDialog(loginDialogType, showWelcomeDialog)
				}
			} catch (e: RuntimeException) {
				log.error(e)
			}
		}

		fun dismiss(fragmentManager: androidx.fragment.app.FragmentManager) {
			val loginDialogFragment = getFragment(fragmentManager)
			loginDialogFragment?.dismissedManually = true
			loginDialogFragment?.dismissAllowingStateLoss()
		}

		private fun getFragment(fragmentManager: androidx.fragment.app.FragmentManager): LoginDialogFragment? {
			return fragmentManager.findFragmentByTag(TAG) as LoginDialogFragment?
		}
	}

	private var activeDialogType: LoginDialogType? = null

	private var showWelcomeDialog = false
	private var focusRequested = false
	private var privacyPolicyAgreed = false
	private var showProgress = false
	private var dismissedManually = false
	private lateinit var continueButton: Button
	private lateinit var scrollView: ScrollView

	enum class LoginDialogType(val viewId: Int, val editorId: Int,
							   @StringRes val titleId: Int, @StringRes val descriptionId: Int,
							   @StringRes val inputTypeDescriptionId: Int) {
		ENTER_PHONE_NUMBER(R.id.enter_phone_number_layout, R.id.phone_number_edit_text,
				R.string.shared_string_authorization, R.string.shared_string_authorization_descr, R.string.enter_phone_number),
		ENTER_CODE(R.id.enter_code_layout, R.id.code_edit_text,
				R.string.enter_code, R.string.authentication_code_descr, R.string.enter_authentication_code),
		ENTER_PASSWORD(R.id.enter_password_layout, R.id.password_edit_text,
				R.string.enter_password, R.string.password_descr, R.string.enter_password),
		GET_TELEGRAM(R.id.get_telegram_layout, 0,
				R.string.get_telegram_title, R.string.get_telegram_account_first, 0),
		PRIVACY_POLICY(R.id.privacy_policy_layout, 0, R.string.how_it_works, R.string.privacy_policy_use_telegram, 0);
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val args = savedInstanceState ?: arguments
		if (args != null) {
			val loginDialogTypeParam = args.getString(LOGIN_DIALOG_TYPE_PARAM_KEY)
			if (loginDialogTypeParam != null) {
				activeDialogType = LoginDialogType.valueOf(loginDialogTypeParam)
			}
			showProgress = args.getBoolean(SHOW_PROGRESS_PARAM_KEY, false)
			showWelcomeDialog = args.getBoolean(SHOW_WELCOME_DIALOG_PARAM_KEY, false)
			privacyPolicyAgreed = args.getBoolean(PRIVACY_POLICY_AGREED_PARAM_KEY, false)
		}
		val view = inflater.inflate(R.layout.login_dialog, container)
		continueButton = view.findViewById(R.id.continue_button)
		scrollView = view.findViewById(R.id.scroll_view)

		buildDialog(view)
		view.viewTreeObserver.addOnGlobalLayoutListener {
			val r = Rect()
			view.getWindowVisibleDisplayFrame(r)
			val screenHeight = view.rootView.height
			val keypadHeight = screenHeight - r.bottom
			val softKeyboardVisible = keypadHeight > screenHeight * SOFT_KEYBOARD_MIN_DETECTION_SIZE
			if (!softKeyboardShown && softKeyboardVisible) {
				softKeyboardShown = softKeyboardVisible
				transformContinueButton(true)
				scrollToBottom()
			} else if (softKeyboardShown && !softKeyboardVisible) {
				transformContinueButton(false)
			}
			softKeyboardShown = softKeyboardVisible
		}
		return view
	}

	private fun transformContinueButton(expanded: Boolean, acceptMode: Boolean = false) {
		val params = continueButton.layoutParams as ViewGroup.MarginLayoutParams
		val bottomMargin = if (expanded || acceptMode) 16f else 40f
		val width = if (expanded) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
		params.apply {
			val horizontalMargin = app.resources.getDimensionPixelSize(if (acceptMode) R.dimen.dialog_welcome_padding_horizontal else R.dimen.content_padding_half)
			setMargins(horizontalMargin, topMargin, horizontalMargin, AndroidUtils.dpToPx(app, bottomMargin))
			this.width = width
		}
		continueButton.requestLayout()
	}

	private fun scrollToBottom() {
		scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN); }
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return object : Dialog(requireActivity(), theme) {
			override fun onBackPressed() {
				if (!dismissedManually) {
					activity?.finish()
				}
			}
		}
	}

	override fun onDismiss(dialog: DialogInterface) {
		super.onDismiss(dialog)
		if (!dismissedManually) {
			getMainActivity()?.closeTelegram()
		}
	}

	@SuppressLint("SetTextI18n")
	@Suppress("DEPRECATION")
	private fun buildDialog(view: View?) {
		view?: return
		focusRequested = false
		if (showWelcomeDialog) {
			bindWelcomePage(view)
			view.findViewById<View>(R.id.login_layout)?.visibility = View.GONE
			view.findViewById<View>(R.id.welcome_layout)?.visibility = View.VISIBLE
		} else {
			view.findViewById<View>(R.id.login_layout)?.visibility = View.VISIBLE
			view.findViewById<View>(R.id.welcome_layout)?.visibility = View.GONE
		}

		val activeDialogType = this.activeDialogType
		for (type in LoginDialogType.values()) {
			val layout: View? = view.findViewById(type.viewId)
			if (type == activeDialogType) {
				bindPage(view, layout, type)
			} else {
				layout?.visibility = View.GONE
			}
		}

		updateProgressView(view)

		continueButton.isEnabled = !showProgress && continueButton.isEnabled || activeDialogType == LoginDialogType.PRIVACY_POLICY
		if (showProgress) {
			continueButton.setOnClickListener(null)
		} else {
			continueButton.setOnClickListener {
				onApplyButtonClick(view)
			}
		}
		val cancelButton: AppCompatImageView? = view.findViewById(R.id.back_button)
		cancelButton?.visibility = if (showWelcomeDialog) View.INVISIBLE else View.VISIBLE
		cancelButton?.setOnClickListener {
			onCancelButtonClick(view, activeDialogType)
		}
	}

	private fun bindWelcomePage(view: View?) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			view?.findViewById<TextView>(R.id.welcome_descr)?.text = Html.fromHtml(getString(R.string.welcome_descr), android.text.Html.FROM_HTML_MODE_LEGACY)
		} else {
			view?.findViewById<TextView>(R.id.welcome_descr)?.text = Html.fromHtml(getString(R.string.welcome_descr))
		}
		val welcomeImage = view?.findViewById<ImageView>(R.id.welcome_image)
		if (Build.VERSION.SDK_INT >= 18) {
			welcomeImage?.setImageResource(R.drawable.bg_introduction_image_top)
		} else {
			welcomeImage?.visibility = View.GONE
		}

		view?.findViewById<Button>(R.id.welcome_continue_button)?.apply {
			val params = layoutParams as ViewGroup.MarginLayoutParams
			val bottomMargin = AndroidUtils.getNavBarHeight(context) + resources.getDimensionPixelSize(R.dimen.dialog_button_bottom_padding)
			params.apply {
				setMargins(leftMargin, topMargin, rightMargin, bottomMargin)
			}
			setOnClickListener {
				showWelcomeDialog = false
				if (!privacyPolicyAgreed) {
					activeDialogType = LoginDialogType.PRIVACY_POLICY
					showProgress = false
				} else if (activeDialogType == null) {
					activeDialogType = LoginDialogType.ENTER_PHONE_NUMBER
					showProgress = true
				}
				buildDialog(view)
			}
		}
	}

	private fun bindPage(
		view: View,
		layout: View?,
		type: LoginDialogType
	) {
		layout?: return

		val titleView: TextView? = view.findViewById(R.id.login_title)
		val descriptionView: TextView? = view.findViewById(R.id.login_description)
		val inputTypeDescriptionView: TextView? = view.findViewById(R.id.edittext_descr)
		if (type.inputTypeDescriptionId != 0) {
			val inputTypeDescription = getText(type.inputTypeDescriptionId).toString() + ":"
			inputTypeDescriptionView?.text = inputTypeDescription
			inputTypeDescriptionView?.visibility = View.VISIBLE
		} else {
			inputTypeDescriptionView?.visibility = View.GONE
		}

		titleView?.text = getText(type.titleId)
		descriptionView?.text = getText(type.descriptionId)

		layout.visibility = View.VISIBLE
		val editText: ExtendedEditText? = layout.findViewById(type.editorId)
		if (editText != null && !showWelcomeDialog) {
			if (activeDialogType == LoginDialogType.ENTER_PHONE_NUMBER && editText.text.isEmpty()) {
				editText.setText(countryPhoneCode)
			}
			editText.setOnEditorActionListener { _, actionId, _ ->
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					applyAuthParam(type, editText.text.toString())
					return@setOnEditorActionListener true
				}
				false
			}
			if (!focusRequested) {
				editText.setSelection(editText.length())
				AndroidUtils.softKeyboardDelayed(editText)
				focusRequested = true
			}
			val passTextLength =
				if (activeDialogType == LoginDialogType.ENTER_PHONE_NUMBER) countryPhoneCode.length else 0
			editText.addTextChangedListener(object : TextWatcher {
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

				override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

				override fun afterTextChanged(s: Editable) {
					changeContinueButtonEnabled(s.length > passTextLength)
				}
			})
			changeContinueButtonEnabled(editText.text.length > passTextLength)
			editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
		}

		val noTelegramViewContainer: LinearLayout? = view.findViewById(R.id.no_telegram)
		if (activeDialogType == LoginDialogType.ENTER_PHONE_NUMBER) {
			bindPhoneNumberPage(view, noTelegramViewContainer)
			noTelegramViewContainer?.visibility = View.VISIBLE
		} else {
			noTelegramViewContainer?.visibility = View.GONE
		}

		val getTelegramViewContainer: LinearLayout? = view.findViewById(R.id.get_telegram_layout)
		if (activeDialogType == LoginDialogType.GET_TELEGRAM) {
			bindGetTelegramPage(view)
			view.findViewById<Button>(R.id.continue_button).visibility = View.GONE
			getTelegramViewContainer?.visibility = View.VISIBLE
		} else {
			getTelegramViewContainer?.visibility = View.GONE
			view.findViewById<Button>(R.id.continue_button).visibility = View.VISIBLE
		}

		val privacyPolicyContainer: LinearLayout? = view.findViewById(R.id.privacy_policy_layout)
		if (activeDialogType == LoginDialogType.PRIVACY_POLICY) {
			bindPrivacyPolicyPage(view, titleView, descriptionView)
			privacyPolicyContainer?.visibility = View.VISIBLE
		} else {
			privacyPolicyContainer?.visibility = View.GONE
			val titleColor = ContextCompat.getColor(app, R.color.ctrl_active_light)
			titleView?.setTextColor(titleColor)
			view.findViewById<Button>(R.id.continue_button).text = getText(R.string.shared_string_continue)
		}
	}

	private fun bindPhoneNumberPage(
		view: View,
		noTelegramViewContainer: View?
	) {
		view.findViewById<TextView>(R.id.no_telegram_title)?.text = getText(R.string.do_not_have_telegram)
		view.findViewById<TextView>(R.id.phone_number_format)?.text = getText(R.string.phone_number_descr)
		view.findViewById<ImageView>(R.id.no_telegram_button)?.setImageResource(R.drawable.ic_arrow_forward)

		noTelegramViewContainer?.setOnClickListener {
			val focusedView = dialog?.currentFocus
			val mainActivity = activity
			if (focusedView != null && mainActivity != null) {
				AndroidUtils.hideSoftKeyboard(mainActivity, focusedView)
			}
			updateDialog(LoginDialogType.GET_TELEGRAM, false)
		}
	}

	private fun bindGetTelegramPage(view: View) {
		view.findViewById<TextView>(R.id.get_telegram_description_first)?.text = getText(R.string.get_telegram_description_continue)
		view.findViewById<TextView>(R.id.get_telegram_description_second)?.text = getText(R.string.get_telegram_after_creating_account)
		val getTelegramButton: ImageView? = view.findViewById(R.id.google_play_button)
		getTelegramButton?.setImageResource(R.drawable.img_google_play_badge)
		getTelegramButton?.setOnClickListener {
			context?.also { ctx ->
				startActivity(
					AndroidUtils.getPlayMarketIntent(
						ctx,
						TELEGRAM_PACKAGE
					)
				)
			}
		}
	}

	private fun bindPrivacyPolicyPage(
		view: View,
		titleView: TextView?,
		descriptionView: TextView?
	) {
		titleView?.setTextColor(
			ContextCompat.getColor(
				app,
				R.color.text_bold_highlight
			)
		)
		val useTelegramDescr = getString(R.string.privacy_policy_use_telegram)
		descriptionView?.apply {
			text = SpannableString(useTelegramDescr).apply {
				val telegram = getString(R.string.shared_string_telegram)
				val start = useTelegramDescr.indexOf(telegram)
				if (start != -1) {
					val clickableSpan = object : ClickableSpan() {
						override fun onClick(textView: View) {
							context?.also { ctx ->
								startActivity(
									AndroidUtils.getPlayMarketIntent(
										ctx,
										TELEGRAM_PACKAGE
									)
								)
							}
						}

						override fun updateDrawState(ds: TextPaint) {
							super.updateDrawState(ds)
							ds.isUnderlineText = false
						}
					}
					setSpan(
						clickableSpan,
						start,
						start + telegram.length,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
					)
					setSpan(
						ForegroundColorSpan(
							ContextCompat.getColor(
								app,
								R.color.text_bold_highlight
							)
						),
						start, start + telegram.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
					)
					setSpan(
						StyleSpan(android.graphics.Typeface.BOLD),
						start, start + telegram.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
					)
				}
			}
			movementMethod = LinkMovementMethod.getInstance()
		}

		view.findViewById<TextView>(R.id.privacy_policy_agree).apply {
			val policyAgreeDescr = getString(R.string.privacy_policy_agree)
			text = SpannableString(policyAgreeDescr).apply {
				val telegramPrivacyPolicy = getString(R.string.telegram_privacy_policy)
				val osmAndPrivacyPolicy = getString(R.string.osmand_privacy_policy)
				var start = policyAgreeDescr.indexOf(telegramPrivacyPolicy)
				if (start != -1) {
					val clickableSpanTelegram = object : ClickableSpan() {
						override fun onClick(textView: View) {
							val intent = Intent(
								Intent.ACTION_VIEW,
								Uri.parse(TELEGRAM_PRIVACY_POLICY)
							)
							if (AndroidUtils.isIntentSafe(context, intent)) {
								startActivity(intent)
							}
						}

						override fun updateDrawState(ds: TextPaint) {
							super.updateDrawState(ds)
							ds.isUnderlineText = false
						}
					}
					setSpan(
						clickableSpanTelegram,
						start,
						start + telegramPrivacyPolicy.length,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
					)
					setSpan(
						ForegroundColorSpan(
							ContextCompat.getColor(
								app,
								R.color.text_bold_highlight
							)
						),
						start,
						start + telegramPrivacyPolicy.length,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
					)
				}
				start = policyAgreeDescr.indexOf(osmAndPrivacyPolicy)
				if (start != -1) {
					val clickableSpanOsmAnd = object : ClickableSpan() {
						override fun onClick(textView: View) {
							val intent = Intent(
								Intent.ACTION_VIEW,
								Uri.parse(OSMAND_PRIVACY_POLICY)
							)
							if (AndroidUtils.isIntentSafe(context, intent)) {
								startActivity(intent)
							}
						}

						override fun updateDrawState(ds: TextPaint) {
							super.updateDrawState(ds)
							ds.isUnderlineText = false
						}
					}
					setSpan(
						clickableSpanOsmAnd,
						start,
						start + osmAndPrivacyPolicy.length,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
					)
					setSpan(
						ForegroundColorSpan(
							ContextCompat.getColor(
								app,
								R.color.text_bold_highlight
							)
						),
						start, start + osmAndPrivacyPolicy.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
					)
				}
			}
			movementMethod = LinkMovementMethod.getInstance()
		}
		view.findViewById<LinearLayout>(R.id.telegram_privacy_policy_btn).apply {
			findViewById<TextView>(R.id.telegram_privacy_policy_title).text =
				getText(R.string.telegram_privacy_policy)
			findViewById<ImageView>(R.id.telegram_privacy_policy_icon)?.setImageDrawable(
				app.uiUtils.getIcon(
					R.drawable.ic_arrow_forward,
					R.color.text_bold_highlight
				)
			)
			setOnClickListener {
				context?.also {
					val intent = Intent(
						Intent.ACTION_VIEW,
						Uri.parse(TELEGRAM_PRIVACY_POLICY)
					)
					if (AndroidUtils.isIntentSafe(context, intent)) {
						startActivity(intent)
					}
				}
			}
		}
		view.findViewById<LinearLayout>(R.id.osmand_privacy_policy_btn).apply {
			findViewById<TextView>(R.id.osmand_privacy_policy_title).text =
				getText(R.string.osmand_privacy_policy)
			findViewById<ImageView>(R.id.osmand_privacy_policy_icon)?.setImageDrawable(
				app.uiUtils.getIcon(R.drawable.ic_arrow_forward, R.color.text_bold_highlight)
			)
			setOnClickListener {
				context?.also {
					val intent = Intent(Intent.ACTION_VIEW, Uri.parse(OSMAND_PRIVACY_POLICY))
					if (AndroidUtils.isIntentSafe(context, intent)) {
						startActivity(intent)
					}
				}
			}
		}
		view.findViewById<Button>(R.id.continue_button).text =
			getText(R.string.shared_string_accept)
		changeContinueButtonEnabled(true)
		transformContinueButton(true, acceptMode = true)
	}

	private fun updateProgressView(view: View) {
		val progressView: View? = view.findViewById(R.id.progress_layout)
		if (showProgress) {
			progressView?.visibility = View.VISIBLE
		} else {
			progressView?.visibility = View.GONE
		}
	}

	private fun onApplyButtonClick(view: View) {
		showWelcomeDialog = false
		if (activeDialogType == LoginDialogType.PRIVACY_POLICY) {
			this.activeDialogType = LoginDialogType.ENTER_PHONE_NUMBER
			privacyPolicyAgreed = true
			buildDialog(view)
		} else {
			for (type in LoginDialogType.values()) {
				val layout: View? = view.findViewById(type.viewId)
				if (type == activeDialogType && layout != null) {
					val editText: ExtendedEditText? = layout.findViewById(type.editorId)
					val text = editText?.text.toString()
					if (!TextUtils.isEmpty(text) && text.length > 1) {
						continueButton.setTextColor(ContextCompat.getColor(app, R.color.secondary_text_light))
						applyAuthParam(type, text)
					}
				}
			}
		}
	}

	private fun onCancelButtonClick(view: View, type: LoginDialogType?) {
		when (type) {
			LoginDialogType.ENTER_PHONE_NUMBER -> {
				showWelcomeDialog = true
				val focusedView = dialog?.currentFocus
				val mainActivity = activity
				if (focusedView != null && mainActivity != null) {
					AndroidUtils.hideSoftKeyboard(mainActivity, focusedView)
				}
				buildDialog(view)
			}
			LoginDialogType.GET_TELEGRAM -> {
				this.activeDialogType = LoginDialogType.ENTER_PHONE_NUMBER
				buildDialog(view)
			}
			else -> {
				showProgress()
				getMainActivity()?.loginTelegram()
			}
		}
	}

	override fun onStart() {
		super.onStart()
		checkCountryPhoneCode()
	}

	private fun checkCountryPhoneCode() {
		OsmandApiUtils.getLocationByIp(app, object : AndroidNetworkUtils.OnRequestResultListener {
			override fun onResult(result: String?) {
				if (result != null) {
					try {
						val obj = JSONObject(result)
						val countryId = obj.getString("country_code")
						countryPhoneCode = DataConstants.countryPhoneCodes[countryId]?.split(",")?.firstOrNull() ?: "+"
					} catch (e: Exception) {
						log.error("JSON parsing error: ", e)
					}
				} else {
					log.debug("Empty response")
				}
			}
		})
	}

	private fun applyAuthParam(type: LoginDialogType, value: String) {
		getMainActivity()?.applyAuthParam(this, type, value)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		val loginDialogActiveType = this.activeDialogType
		if (loginDialogActiveType != null) {
			outState.putString(LOGIN_DIALOG_TYPE_PARAM_KEY, loginDialogActiveType.name)
		}
		outState.putBoolean(SHOW_PROGRESS_PARAM_KEY, showProgress)
		outState.putBoolean(SHOW_WELCOME_DIALOG_PARAM_KEY, showWelcomeDialog)
		outState.putBoolean(PRIVACY_POLICY_AGREED_PARAM_KEY, privacyPolicyAgreed)
	}

	private fun getMainActivity(): MainActivity? {
		val activity = this.activity
		return if (activity != null) {
			activity as MainActivity
		} else {
			null
		}
	}

	private fun updateDialog(loginDialogType: LoginDialogType? = null, welcomeDialog: Boolean = false) {
		this.activeDialogType = loginDialogType
		showProgress = false
		showWelcomeDialog = welcomeDialog
		buildDialog(view)
	}

	fun showProgress() {
		showProgress = true
		buildDialog(view)
	}

	private fun changeContinueButtonEnabled(enabled: Boolean) {
		if (enabled != continueButton.isEnabled) {
			val color = if (enabled) R.color.white else R.color.secondary_text_light
			continueButton.setTextColor(ContextCompat.getColor(app, color))
			continueButton.isEnabled = enabled
		}
	}
}
