package net.osmand.telegram.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.AppCompatImageView
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.utils.AndroidUtils
import studio.carbonylgroup.textfieldboxes.ExtendedEditText
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.view.*
import android.view.ViewGroup
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.TypedValue


class LoginDialogFragment : DialogFragment() {

	companion object {

		private const val TAG = "LoginDialogFragment"
		private val LOG = PlatformUtil.getLog(LoginDialogFragment::class.java)

		private const val LOGIN_DIALOG_TYPE_PARAM_KEY = "login_dialog_type_param"
		private const val SHOW_PROGRESS_PARAM_KEY = "show_progress_param"
		private const val SHOW_WELCOME_DIALOG_PARAM_KEY = "show_welcome_dialog_param"
		private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
		private const val SOFT_KEYBOARD_MIN_DETECTION_SIZE = 0.15

		var welcomeDialogShown = false
			private set

		private var softKeyboardShown: Boolean = false

		fun showWelcomeDialog(fragmentManager: FragmentManager) {
			welcomeDialogShown = true
			showDialog(fragmentManager, welcomeDialog = true)
		}

		fun showDialog(fragmentManager: FragmentManager, loginDialogType: LoginDialogType? = null, welcomeDialog: Boolean = false) {
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
				LOG.error(e)
			}
		}

		fun dismiss(fragmentManager: FragmentManager) {
			val loginDialogFragment = getFragment(fragmentManager)
			loginDialogFragment?.dismissedManually = true
			loginDialogFragment?.dismiss()
		}

		private fun getFragment(fragmentManager: FragmentManager): LoginDialogFragment? {
			return fragmentManager.findFragmentByTag(TAG) as LoginDialogFragment?
		}
	}

	private var loginDialogActiveType: LoginDialogType? = null

	private var showWelcomeDialog = false
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
				R.string.get_telegram_title, R.string.get_telegram_account_first, 0);
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_NoActionbar_Translucent)
		val activity = requireActivity()
		val window = activity.window
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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

	private fun transformContinueButton(expanded: Boolean) {
		val params = continueButton.layoutParams as ViewGroup.MarginLayoutParams
		val margin = if (expanded) 16f else 40f
		val width = if (expanded) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
		params.apply {
			setMargins(leftMargin, topMargin, rightMargin, AndroidUtils.dpToPx(context!!, margin))
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
				if (loginDialogActiveType == null) {
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
			val contains = t == loginDialogActiveType && !showProgress
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
							editText.addTextChangedListener(object : TextWatcher {
								override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
								override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
								override fun afterTextChanged(s: Editable) {
									changeContinueButtonEnabled(!s.isEmpty())
								}
							})
							changeContinueButtonEnabled(!editText.text.isEmpty())
							editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
						}

						val noTelegramViewContainer: LinearLayout? = view.findViewById(R.id.no_telegram)
						if (loginDialogActiveType == LoginDialogType.ENTER_PHONE_NUMBER) {
							view.findViewById<TextView>(R.id.no_telegram_title)?.text = getText(R.string.do_not_have_telegram)
							view.findViewById<TextView>(R.id.phone_number_format)?.text = getText(R.string.phone_number_descr)
							view.findViewById<ImageView>(R.id.no_telegram_button)?.setImageResource(R.drawable.ic_arrow_forward)

							noTelegramViewContainer?.setOnClickListener {
								val focusedView = dialog.currentFocus
								if (focusedView != null) {
									AndroidUtils.hideSoftKeyboard(activity!!, focusedView)
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
								val app = getMainActivity()?.application
								if (app != null) {
									startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AndroidUtils.getPlayMarketLink(app, TELEGRAM_PACKAGE))))
								}
							}
							view.findViewById<Button>(R.id.continue_button).visibility = View.GONE
							getTelegramViewContainer?.visibility = View.VISIBLE
						} else {
							getTelegramViewContainer?.visibility = View.GONE
							view.findViewById<Button>(R.id.continue_button).visibility = View.VISIBLE
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
		continueButton.isEnabled = !showProgress && continueButton.isEnabled
		if (showProgress) {
			continueButton.setOnClickListener(null)
		} else {
			continueButton.setOnClickListener {
				showWelcomeDialog = false
				for (t in LoginDialogType.values()) {
					val layout: View? = view?.findViewById(t.viewId)
					val contains = t == loginDialogActiveType
					if (contains && layout != null) {
						val editText: ExtendedEditText? = layout.findViewById(t.editorId)
						val text = editText?.text.toString()
						if (!TextUtils.isEmpty(text) && text.length > 1) {
							continueButton.setTextColor(ContextCompat.getColor(context!!, R.color.secondary_text_light))
							applyAuthParam(t, text)
						}
					}
				}
			}
		}
		val cancelButton: AppCompatImageView? = view?.findViewById(R.id.back_button)
		cancelButton?.visibility = if (showWelcomeDialog) View.INVISIBLE else View.VISIBLE
		cancelButton?.setOnClickListener {
			if (loginDialogActiveType == LoginDialogType.ENTER_PHONE_NUMBER) {
				showWelcomeDialog = true
				val focusedView = dialog.currentFocus
				if (focusedView != null) {
					AndroidUtils.hideSoftKeyboard(activity!!, focusedView)
				}
				buildDialog(view)
			} else {
				showProgress()
				getMainActivity()?.loginTelegram()
			}
		}
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
			continueButton.setTextColor(ContextCompat.getColor(context!!, color))
			continueButton.isEnabled = enabled
		}
	}
}
