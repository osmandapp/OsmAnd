package net.osmand.telegram.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.text.*
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.utils.AndroidNetworkUtils
import net.osmand.telegram.utils.AndroidUtils
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

		private var countryPhoneCodes = mapOf(
			Pair("AB", "+7840,+7940,+99544"), Pair("AF", "+93"), Pair("AX", "+35818"), Pair("AL", "+355"),
			Pair("DZ", "+213"), Pair("AS", "+1684"), Pair("AD", "+376"), Pair("AO", "+244"),
			Pair("AI", "+1264"), Pair("AG", "+1268"), Pair("AR", "+54"), Pair("AM", "+374"),
			Pair("AW", "+297"), Pair("SH", "+247"), Pair("AU", "+61"), Pair("AU", "+672"),
			Pair("AT", "+43"), Pair("AZ", "+994"), Pair("BS", "+1242"), Pair("BH", "+973"),
			Pair("BD", "+880"), Pair("BB", "+1246"), Pair("AG", "+1268"), Pair("BY", "+375"),
			Pair("BE", "+32"), Pair("BZ", "+501"), Pair("BJ", "+229"), Pair("BM", "+1441"),
			Pair("BT", "+975"), Pair("BO", "+591"), Pair("BQ", "+5997"), Pair("BA", "+387"),
			Pair("BW", "+267"), Pair("BR", "+55"), Pair("IO", "+246"), Pair("VG", "+1284"),
			Pair("BN", "+673"), Pair("BG", "+359"), Pair("BF", "+226"), Pair("MY", "+95"),
			Pair("BI", "+257"), Pair("KH", "+855"), Pair("CM", "+237"), Pair("CA", "+1"),
			Pair("CV", "+238"), Pair("KY", "+1345"), Pair("CF", "+236"), Pair("TD", "+235"),
			Pair("CL", "+56"), Pair("CN", "+86"), Pair("CX", "+61"), Pair("CC", "+61"),
			Pair("CO", "+57"), Pair("KM", "+269"), Pair("CG", "+242"), Pair("CD", "+243"),
			Pair("CK", "+682"), Pair("CR", "+506"), Pair("CI", "+225"), Pair("HR", "+385"),
			Pair("CU", "+53"), Pair("CW", "+5999"), Pair("CY", "+357"), Pair("CZ", "+420"),
			Pair("DK", "+45"), Pair("DG", "+246"), Pair("DJ", "+253"), Pair("DM", "+1767"),
			Pair("DO", "+1809,+1829,+1849"), Pair("TL", "+670"), Pair("EC", "+593"),
			Pair("EG", "+20"), Pair("SV", "+503"), Pair("GQ", "+240"), Pair("ER", "+291"),
			Pair("EE", "+372"), Pair("ET", "+251"), Pair("FK", "+500"), Pair("FO", "+298"),
			Pair("FJ", "+679"), Pair("FI", "+358"), Pair("FR", "+33"), Pair("GF", "+594"),
			Pair("PF", "+689"), Pair("GA", "+241"), Pair("GM", "+220"), Pair("GE", "+995"),
			Pair("DE", "+49"), Pair("GH", "+233"), Pair("GI", "+350"), Pair("GR", "+30"),
			Pair("GL", "+299"), Pair("GD", "+1473"), Pair("GP", "+590"), Pair("GU", "+1671"),
			Pair("GT", "+502"), Pair("GG", "+44"), Pair("GN", "+224"), Pair("GW", "+245"),
			Pair("GY", "+592"), Pair("HT", "+509"), Pair("HN", "+504"), Pair("HK", "+852"),
			Pair("HU", "+36"), Pair("IS", "+354"), Pair("IN", "+91"), Pair("ID", "+62"),
			Pair("IR", "+98"), Pair("IQ", "+964"), Pair("IE", "+353"), Pair("IL", "+972"),
			Pair("IT", "+39"), Pair("JM", "+1876"), Pair("SJ", "+4779"), Pair("JP", "+81"),
			Pair("JE", "+44"), Pair("JO", "+962"), Pair("KZ", "+76,+77"), Pair("KE", "+254"),
			Pair("KI", "+686"), Pair("KP", "+850"), Pair("KR", "+82"), Pair("KW", "+965"),
			Pair("KG", "+996"), Pair("LA", "+856"), Pair("LV", "+371"), Pair("LB", "+961"),
			Pair("LS", "+266"), Pair("LR", "+231"), Pair("LY", "+218"), Pair("LI", "+423"),
			Pair("LT", "+370"), Pair("LU", "+352"), Pair("MO", "+853"), Pair("MK", "+389"),
			Pair("MG", "+261"), Pair("MW", "+265"), Pair("MY", "+60"), Pair("MV", "+960"),
			Pair("ML", "+223"), Pair("MT", "+356"), Pair("MH", "+692"), Pair("MQ", "+596"),
			Pair("MR", "+222"), Pair("MU", "+230"), Pair("YT", "+262"), Pair("MX", "+52"),
			Pair("FM", "+691"), Pair("MD", "+373"), Pair("MC", "+377"), Pair("MN", "+976"),
			Pair("ME", "+382"), Pair("MS", "+1664"), Pair("MA", "+212"), Pair("MZ", "+258"),
			Pair("NA", "+264"), Pair("NR", "+674"), Pair("NP", "+977"), Pair("NL", "+31"),
			Pair("NC", "+687"), Pair("NZ", "+64"), Pair("NI", "+505"), Pair("NE", "+227"),
			Pair("NG", "+234"), Pair("NU", "+683"), Pair("NF", "+672"), Pair("MP", "+1670"),
			Pair("NO", "+47"), Pair("OM", "+968"), Pair("PK", "+92"), Pair("PW", "+680"),
			Pair("PS", "+970"), Pair("PA", "+507"), Pair("PG", "+675"), Pair("PY", "+595"),
			Pair("PE", "+51"), Pair("PH", "+63"), Pair("PN", "+64"), Pair("PL", "+48"),
			Pair("PT", "+351"), Pair("PR", "+1787,+1939"), Pair("QA", "+974"), Pair("RE", "+262"),
			Pair("RO", "+40"), Pair("RU", "+7"), Pair("RW", "+250"), Pair("BL", "+590"),
			Pair("SH", "+290"), Pair("KN", "+1869"), Pair("LC", "+1758"), Pair("MF", "+590"),
			Pair("PM", "+508"), Pair("VC", "+1784"), Pair("WS", "+685"), Pair("SM", "+378"),
			Pair("ST", "+239"), Pair("SA", "+966"), Pair("SN", "+221"), Pair("RS", "+381"),
			Pair("SC", "+248"), Pair("SL", "+232"), Pair("SG", "+65"), Pair("BQ", "+5993"),
			Pair("SX", "+1721"), Pair("SK", "+421"), Pair("SI", "+386"), Pair("SB", "+677"),
			Pair("SO", "+252"), Pair("ZA", "+27"), Pair("GS", "+500"), Pair("!1", "+99534"),
			Pair("SS", "+211"), Pair("ES", "+34"), Pair("LK", "+94"), Pair("SD", "+249"),
			Pair("SR", "+597"), Pair("SJ", "+4779"), Pair("SZ", "+268"), Pair("SE", "+46"),
			Pair("CH", "+41"), Pair("SY", "+963"), Pair("TW", "+886"), Pair("TJ", "+992"),
			Pair("TZ", "+255"), Pair("TH", "+66"), Pair("TG", "+228"), Pair("TK", "+690"),
			Pair("TO", "+676"), Pair("TT", "+1868"), Pair("TN", "+216"), Pair("TR", "+90"),
			Pair("TM", "+993"), Pair("TC", "+1649"), Pair("TV", "+688"), Pair("UG", "+256"),
			Pair("UA", "+380"), Pair("AE", "+971"), Pair("UK", "+44"), Pair("US", "+1"),
			Pair("UY", "+598"), Pair("VI", "+1340"), Pair("UZ", "+998"), Pair("VU", "+678"),
			Pair("VE", "+58"), Pair("VA", "+3906698,+379"), Pair("VN", "+84"), Pair("WF", "+681"),
			Pair("YE", "+967"), Pair("ZM", "+260"), Pair("ZW", "+263"))

		private var countryPhoneCode: String = "+"

		fun showWelcomeDialog(fragmentManager: FragmentManager) {
			welcomeDialogShown = true
			showDialog(fragmentManager, welcomeDialog = true)
		}

		fun showDialog(fragmentManager: FragmentManager, loginDialogType: LoginDialogType? = null, welcomeDialog: Boolean = false, privacyPolicyAgreed: Boolean = false) {
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

		fun dismiss(fragmentManager: FragmentManager) {
			val loginDialogFragment = getFragment(fragmentManager)
			loginDialogFragment?.dismissedManually = true
			loginDialogFragment?.dismissAllowingStateLoss()
		}

		private fun getFragment(fragmentManager: FragmentManager): LoginDialogFragment? {
			return fragmentManager.findFragmentByTag(TAG) as LoginDialogFragment?
		}
	}

	private var loginDialogActiveType: LoginDialogType? = null

	private var showWelcomeDialog = false
	private var privacyPolicyAgreed = false
	private var showProgress = false
	private var dismissedManually = false
	private lateinit var continueButton: Button

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
				loginDialogActiveType = LoginDialogType.valueOf(loginDialogTypeParam)
			}
			showProgress = args.getBoolean(SHOW_PROGRESS_PARAM_KEY, false)
			showWelcomeDialog = args.getBoolean(SHOW_WELCOME_DIALOG_PARAM_KEY, false)
			privacyPolicyAgreed = args.getBoolean(PRIVACY_POLICY_AGREED_PARAM_KEY, false)
		}
		val view = inflater.inflate(R.layout.login_dialog, container)
		continueButton = view.findViewById(R.id.continue_button)

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

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return object : Dialog(requireActivity(), theme) {
			override fun onBackPressed() {
				if (!dismissedManually) {
					activity?.finish()
				}
			}
		}
	}

	override fun onDismiss(dialog: DialogInterface?) {
		super.onDismiss(dialog)
		if (!dismissedManually) {
			getMainActivity()?.closeTelegram()
		}
	}

	@SuppressLint("SetTextI18n")
	@Suppress("DEPRECATION")
	private fun buildDialog(view: View?) {
		if (showWelcomeDialog) {
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

			val continueButton = view?.findViewById<Button>(R.id.welcome_continue_button)
			continueButton?.setOnClickListener {
				showWelcomeDialog = false
				if (!privacyPolicyAgreed) {
					loginDialogActiveType = LoginDialogType.PRIVACY_POLICY
					showProgress = false
				} else if (loginDialogActiveType == null) {
					loginDialogActiveType = LoginDialogType.ENTER_PHONE_NUMBER
					showProgress = true
				}
				buildDialog(view)
			}
			view?.findViewById<View>(R.id.login_layout)?.visibility = View.GONE
			view?.findViewById<View>(R.id.welcome_layout)?.visibility = View.VISIBLE
		} else {
			view?.findViewById<View>(R.id.login_layout)?.visibility = View.VISIBLE
			view?.findViewById<View>(R.id.welcome_layout)?.visibility = View.GONE
		}

		val loginDialogActiveType = this.loginDialogActiveType
		var focusRequested = false
		for (t in LoginDialogType.values()) {
			val layout: View? = view?.findViewById(t.viewId)
			val contains = t == loginDialogActiveType
			when {
				contains -> {
					if (layout != null) {
						val titleView: TextView? = view.findViewById(R.id.login_title)
						val descriptionView: TextView? = view.findViewById(R.id.login_description)
						val inputTypeDescriptionView: TextView? = view.findViewById(R.id.edittext_descr)
						if (t.inputTypeDescriptionId != 0) {
							val inputTypeDescription = getText(t.inputTypeDescriptionId).toString() + ":"
							inputTypeDescriptionView?.text = inputTypeDescription
							inputTypeDescriptionView?.visibility = View.VISIBLE
						} else {
							inputTypeDescriptionView?.visibility = View.GONE
						}

						titleView?.text = getText(t.titleId)
						descriptionView?.text = getText(t.descriptionId)

						layout.visibility = View.VISIBLE
						val editText: ExtendedEditText? = layout.findViewById(t.editorId)
						if (editText != null && !showWelcomeDialog) {
							if (loginDialogActiveType == LoginDialogType.ENTER_PHONE_NUMBER && editText.text.isEmpty()) {
								editText.setText(countryPhoneCode)
							}
								editText.setOnEditorActionListener { _, actionId, _ ->
								if (actionId == EditorInfo.IME_ACTION_DONE) {
									applyAuthParam(t, editText.text.toString())
									return@setOnEditorActionListener true
								}
								false
							}
							if (!focusRequested) {
								editText.setSelection(editText.length())
								AndroidUtils.softKeyboardDelayed(editText)
								focusRequested = true
							}
							val passTextLength = if (loginDialogActiveType == LoginDialogType.ENTER_PHONE_NUMBER) countryPhoneCode.length else 0
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
						if (loginDialogActiveType == LoginDialogType.ENTER_PHONE_NUMBER) {
							view.findViewById<TextView>(R.id.no_telegram_title)?.text = getText(R.string.do_not_have_telegram)
							view.findViewById<TextView>(R.id.phone_number_format)?.text = getText(R.string.phone_number_descr)
							view.findViewById<ImageView>(R.id.no_telegram_button)?.setImageResource(R.drawable.ic_arrow_forward)

							noTelegramViewContainer?.setOnClickListener {
								val focusedView = dialog.currentFocus
								val mainActivity = activity
								if (focusedView != null && mainActivity != null) {
									AndroidUtils.hideSoftKeyboard(mainActivity, focusedView)
								}
								updateDialog(LoginDialogType.GET_TELEGRAM, false)
							}
							noTelegramViewContainer?.visibility = View.VISIBLE
						} else {
							noTelegramViewContainer?.visibility = View.GONE
						}

						val getTelegramViewContainer: LinearLayout? = view.findViewById(R.id.get_telegram_layout)
						if (loginDialogActiveType == LoginDialogType.GET_TELEGRAM) {
							view.findViewById<TextView>(R.id.get_telegram_description_first)?.text = getText(R.string.get_telegram_description_continue)
							view.findViewById<TextView>(R.id.get_telegram_description_second)?.text = getText(R.string.get_telegram_after_creating_account)
							val getTelegramButton: ImageView? = view.findViewById(R.id.google_play_button)
							getTelegramButton?.setImageResource(R.drawable.img_google_play_badge)
							getTelegramButton?.setOnClickListener {
								context?.also { ctx ->
									startActivity(AndroidUtils.getPlayMarketIntent(ctx, TELEGRAM_PACKAGE))
								}
							}
							view.findViewById<Button>(R.id.continue_button).visibility = View.GONE
							getTelegramViewContainer?.visibility = View.VISIBLE
						} else {
							getTelegramViewContainer?.visibility = View.GONE
							view.findViewById<Button>(R.id.continue_button).visibility = View.VISIBLE
						}

						val privacyPolicyContainer: LinearLayout? = view.findViewById(R.id.privacy_policy_layout)
						if (loginDialogActiveType == LoginDialogType.PRIVACY_POLICY) {
							titleView?.setTextColor(ContextCompat.getColor(app, R.color.text_bold_highlight))
							val useTelegramDescr = getString(R.string.privacy_policy_use_telegram)
								descriptionView?.text = SpannableString(useTelegramDescr).apply {
									val telegram = getString(R.string.shared_string_telegram)
									val start = useTelegramDescr.indexOf(telegram)
									if (start != -1) {
										setSpan(ForegroundColorSpan(ContextCompat.getColor(app, R.color.text_bold_highlight)), start, start + telegram.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
										setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, start + telegram.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
									}
								}
							view.findViewById<TextView>(R.id.privacy_policy_agree).apply {
								val policyAgreeDescr = getString(R.string.privacy_policy_agree)
								text = SpannableString(policyAgreeDescr).apply {
									val telegramPrivacyPolicy = getString(R.string.telegram_privacy_policy)
									val osmAndPrivacyPolicy = getString(R.string.osmand_privacy_policy)
									var start = policyAgreeDescr.indexOf(telegramPrivacyPolicy)
									if (start != -1) {
										setSpan(ForegroundColorSpan(ContextCompat.getColor(app, R.color.text_bold_highlight)), start, start + telegramPrivacyPolicy.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
									}
									start = policyAgreeDescr.indexOf(osmAndPrivacyPolicy)
									if (start != -1) {
										setSpan(ForegroundColorSpan(ContextCompat.getColor(app, R.color.text_bold_highlight)), start, start + osmAndPrivacyPolicy.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
									}
								}
							}
							view.findViewById<LinearLayout>(R.id.telegram_privacy_policy_btn).apply {
								findViewById<TextView>(R.id.telegram_privacy_policy_title).text = getText(R.string.telegram_privacy_policy)
								findViewById<ImageView>(R.id.telegram_privacy_policy_icon)?.setImageDrawable(app.uiUtils.getIcon(R.drawable.ic_arrow_forward, R.color.text_bold_highlight))
								setOnClickListener {
									context?.also {
										val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_PRIVACY_POLICY))
										if (AndroidUtils.isIntentSafe(context, intent)) {
											startActivity(intent)
										}
									}
								}
							}
							view.findViewById<LinearLayout>(R.id.osmand_privacy_policy_btn).apply {
								findViewById<TextView>(R.id.osmand_privacy_policy_title).text = getText(R.string.osmand_privacy_policy)
								findViewById<ImageView>(R.id.osmand_privacy_policy_icon)?.setImageDrawable(app.uiUtils.getIcon(R.drawable.ic_arrow_forward, R.color.text_bold_highlight))
								setOnClickListener {
									context?.also {
										val intent = Intent(Intent.ACTION_VIEW, Uri.parse(OSMAND_PRIVACY_POLICY))
										if (AndroidUtils.isIntentSafe(context, intent)) {
											startActivity(intent)
										}
									}
								}
							}
							view.findViewById<Button>(R.id.continue_button).text = getText(R.string.shared_string_accept)
							changeContinueButtonEnabled(true)
							transformContinueButton(true, acceptMode = true)
							privacyPolicyContainer?.visibility = View.VISIBLE
						} else {
							privacyPolicyContainer?.visibility = View.GONE
							titleView?.setTextColor(ContextCompat.getColor(app, R.color.ctrl_active_light))
							view.findViewById<Button>(R.id.continue_button).text = getText(R.string.shared_string_continue)
						}
					}
				}
				else -> layout?.visibility = View.GONE
			}
			val progressView: View? = view?.findViewById(R.id.progress_layout)
			if (showProgress) {
				progressView?.visibility = View.VISIBLE
			} else {
				progressView?.visibility = View.GONE
			}
		}
		continueButton.isEnabled = !showProgress && continueButton.isEnabled || loginDialogActiveType == LoginDialogType.PRIVACY_POLICY
		if (showProgress) {
			continueButton.setOnClickListener(null)
		} else {
			continueButton.setOnClickListener {
				showWelcomeDialog = false
				if (loginDialogActiveType == LoginDialogType.PRIVACY_POLICY) {
					this.loginDialogActiveType = LoginDialogType.ENTER_PHONE_NUMBER
					privacyPolicyAgreed = true
					buildDialog(view)
				} else {
					for (t in LoginDialogType.values()) {
						val layout: View? = view?.findViewById(t.viewId)
						val contains = t == loginDialogActiveType
						if (contains && layout != null) {
							val editText: ExtendedEditText? = layout.findViewById(t.editorId)
							val text = editText?.text.toString()
							if (!TextUtils.isEmpty(text) && text.length > 1) {
								continueButton.setTextColor(ContextCompat.getColor(app, R.color.secondary_text_light))
								applyAuthParam(t, text)
							}
						}
					}
				}
			}
		}
		val cancelButton: AppCompatImageView? = view?.findViewById(R.id.back_button)
		cancelButton?.visibility = if (showWelcomeDialog) View.INVISIBLE else View.VISIBLE
		cancelButton?.setOnClickListener {
			when (loginDialogActiveType) {
				LoginDialogType.ENTER_PHONE_NUMBER -> {
					showWelcomeDialog = true
					val focusedView = dialog.currentFocus
					val mainActivity = activity
					if (focusedView != null && mainActivity != null) {
						AndroidUtils.hideSoftKeyboard(mainActivity, focusedView)
					}
					buildDialog(view)
				}
				LoginDialogType.GET_TELEGRAM -> {
					this.loginDialogActiveType = LoginDialogType.ENTER_PHONE_NUMBER
					buildDialog(view)
				}
				else -> {
					showProgress()
					getMainActivity()?.loginTelegram()
				}
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
						countryPhoneCode = countryPhoneCodes[countryId]?.split(",")?.firstOrNull() ?: "+"
					} catch (e: Exception) {
						log.error("JSON parsing error: ", e)
					}
				} else {
					log.debug("Empty response")
				}
			}
		})
	}

	private fun applyAuthParam(t: LoginDialogType, value: String) {
		getMainActivity()?.applyAuthParam(this, t, value)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		val loginDialogActiveType = this.loginDialogActiveType
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
		this.loginDialogActiveType = loginDialogType
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
