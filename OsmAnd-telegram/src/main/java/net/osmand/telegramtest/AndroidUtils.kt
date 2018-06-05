package net.osmand.telegramtest

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.inputmethod.InputMethodManager

object AndroidUtils {

    private fun isHardwareKeyboardAvailable(context: Context): Boolean {
        return context.resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS
    }

    fun softKeyboardDelayed(view: View) {
        view.post {
            if (!isHardwareKeyboardAvailable(view.context)) {
                val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    fun hideSoftKeyboard(activity: Activity, input: View?) {
        val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (inputMethodManager != null) {
            if (input != null) {
                val windowToken = input.windowToken
                if (windowToken != null) {
                    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
                }
            }
        }
    }
}
