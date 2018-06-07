package net.osmand.telegram

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.PlatformUtil


class LoginDialogFragment : DialogFragment() {

    companion object {

        private const val TAG = "LoginDialogFragment"
        private val LOG = PlatformUtil.getLog(LoginDialogFragment::class.java)

        private const val ENTER_PHONE_NUMBER_PARAM_KEY: String = "enter_phone_number_param_key"
        private const val ENTER_CODE_PARAM_KEY = "enter_code_param_key"
        private const val ENTER_PASSWORD_PARAM_KEY = "enter_password_param_key"
        private const val SHOW_PROGRESS_PARAM_KEY = "show_progress_param_key"

        fun showDialog(fragmentManager: FragmentManager, vararg loginDialogType: LoginDialogType) {
            try {
                var fragment = getFragment(fragmentManager)
                if (fragment == null) {
                    fragment = LoginDialogFragment()
                    val args = Bundle()
                    for (t in loginDialogType) {
                        args.putBoolean(t.paramKey, true)
                    }
                    fragment.arguments = args
                    fragment.show(fragmentManager, TAG)
                } else {
                    fragment.updateDialog(*loginDialogType)
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

    private var loginDialogActiveTypes: Set<LoginDialogType>? = null

    private var dismissedManually = false

    enum class LoginDialogType(val paramKey: String, val viewId: Int, val editorId: Int) {
        ENTER_PHONE_NUMBER(ENTER_PHONE_NUMBER_PARAM_KEY, R.id.enterPhoneNumberLayout, R.id.phoneNumberEditText),
        ENTER_CODE(ENTER_CODE_PARAM_KEY, R.id.enterCodeLayout, R.id.codeEditText),
        ENTER_PASSWORD(ENTER_PASSWORD_PARAM_KEY, R.id.enterPasswordLayout, R.id.passwordEditText),
        SHOW_PROGRESS(SHOW_PROGRESS_PARAM_KEY, R.id.progressLayout, 0);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_NoActionbar)
        val activity = requireActivity()
        val window = activity.window
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val loginDialogActiveTypes: MutableSet<LoginDialogType> = HashSet()

        val args = savedInstanceState ?: arguments
        if (args != null) {
            for (t in LoginDialogType.values()) {
                if (args.getBoolean(t.paramKey, false)) {
                    loginDialogActiveTypes.add(t)
                }
            }
        }

        this.loginDialogActiveTypes = loginDialogActiveTypes

        val view = inflater.inflate(R.layout.login_dialog, container)
        buildDialog(view)
        return view
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        if (!dismissedManually) {
            getMainActivity()?.closeTelegram()
        }
    }

    private fun buildDialog(view: View?) {
        val loginDialogActiveTypes = this.loginDialogActiveTypes
        var hasProgress = false
        var focusRequested = false
        for (t in LoginDialogType.values()) {
            val layout: View? = view?.findViewById(t.viewId)
            val contains = loginDialogActiveTypes?.contains(t) ?: false
            when {
                contains -> {
                    if (t == LoginDialogType.SHOW_PROGRESS) {
                        hasProgress = true
                    }
                    if (layout != null) {
                        layout.visibility = View.VISIBLE
                        val editText: EditText? = layout.findViewById(t.editorId)
                        if (editText != null) {
                            editText.setOnEditorActionListener { _, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                    applyAuthParam(t, editText.text.toString())
                                    return@setOnEditorActionListener true
                                }
                                false
                            }
                            if (!focusRequested) {
                                editText.requestFocus()
                                AndroidUtils.softKeyboardDelayed(editText)
                                focusRequested = true
                            }
                        }
                    }
                }
                else -> layout?.visibility = View.GONE
            }
        }
        val continueButton: Button? = view?.findViewById(R.id.continueButton)
        if (continueButton != null) {
            continueButton.isEnabled = !hasProgress
            if (hasProgress) {
                continueButton.setOnClickListener(null)
            } else {
                continueButton.setOnClickListener {
                    for (t in LoginDialogType.values()) {
                        val layout: View? = view.findViewById(t.viewId)
                        val contains = loginDialogActiveTypes?.contains(t) ?: false
                        if (contains && layout != null) {
                            val editText: EditText? = layout.findViewById(t.editorId)
                            if (editText != null) {
                                applyAuthParam(t, editText.text.toString())
                            }
                        }
                    }
                }
            }
        }
        val cancelButton: Button? = view?.findViewById(R.id.calcelButton)
        cancelButton?.setOnClickListener {
            dismiss()
        }
    }

    private fun applyAuthParam(t: LoginDialogType, value: String) {
        getMainActivity()?.applyAuthParam(this, t, value)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val loginDialogActiveTypes = this.loginDialogActiveTypes
        if (loginDialogActiveTypes != null) {
            for (t in loginDialogActiveTypes) {
                outState.putBoolean(t.paramKey, true)
            }
        }
    }

    private fun getMainActivity(): MainActivity? {
        val activity = this.activity
        return if (activity != null) {
            activity as MainActivity
        } else {
            null
        }
    }

    fun updateDialog(vararg loginDialogType: LoginDialogType) {
        val loginDialogActiveTypes: MutableSet<LoginDialogType> = HashSet()
        for (t in loginDialogType) {
            loginDialogActiveTypes.add(t)
        }
        this.loginDialogActiveTypes = loginDialogActiveTypes

        buildDialog(view)
    }
}
