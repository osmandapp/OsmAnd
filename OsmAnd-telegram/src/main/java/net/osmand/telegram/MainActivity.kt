package net.osmand.telegram

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import net.osmand.telegram.LoginDialogFragment.LoginDialogType
import net.osmand.telegram.TelegramHelper.*


class MainActivity : AppCompatActivity(), TelegramListener {

    companion object {
        private const val LOGIN_MENU_ID = 0
        private const val LOGOUT_MENU_ID = 1
        private const val PROGRESS_MENU_ID = 2
    }

    private var authParamRequestHandler: AuthParamRequestHandler? = null
    private var paused: Boolean = false

    private val app: TelegramApplication
        get() = application as TelegramApplication

    private val telegramHelper: TelegramHelper
        get() = app.telegramHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        paused = false

        authParamRequestHandler = telegramHelper.setAuthParamRequestHandler(object : AuthParamRequestListener {
            override fun onRequestAuthParam(paramType: AuthParamType) {
                runOnUiThread {
                    if (!paused) {
                        showLoginDialog(paramType)
                    }
                }
            }

            override fun onAuthRequestError(code: Int, message: String) {
                runOnUiThread {
                    if (!paused) {
                        Toast.makeText(this@MainActivity, "$code - $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
        telegramHelper.listener = this
        telegramHelper.init()
    }

    override fun onResume() {
        super.onResume()
        paused = false

        invalidateOptionsMenu()
        updateTitle()
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onTelegramStatusChanged(prevAutoState: AuthState, newAuthState: AuthState) {
        runOnUiThread {
            if (!paused) {
                val fm = supportFragmentManager
                when (newAuthState) {
                    AuthState.READY,
                    AuthState.CLOSED,
                    AuthState.UNKNOWN -> LoginDialogFragment.dismiss(fm)
                    else -> Unit
                }
                invalidateOptionsMenu()
                updateTitle()
            }
        }
    }

    fun logoutTelegram(silent: Boolean = false) {
        if (telegramHelper.getAuthState() == AuthState.READY) {
            telegramHelper.logout()
        } else {
            invalidateOptionsMenu()
            updateTitle()
            if (!silent) {
                Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun closeTelegram() {
        telegramHelper.close()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            LOGIN_MENU_ID -> {
                telegramHelper.init()
                true
            }
            LOGOUT_MENU_ID -> {
                logoutTelegram()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menu != null) {
            menu.clear()
            when (telegramHelper.getAuthState()) {
                AuthState.UNKNOWN,
                AuthState.WAIT_PARAMETERS,
                AuthState.WAIT_PHONE_NUMBER,
                AuthState.WAIT_CODE,
                AuthState.WAIT_PASSWORD,
                AuthState.LOGGING_OUT,
                AuthState.CLOSING -> createProgressMenuItem(menu)
                AuthState.READY -> createMenuItem(menu, LOGOUT_MENU_ID, R.string.shared_string_logout,
                        MenuItem.SHOW_AS_ACTION_WITH_TEXT or MenuItem.SHOW_AS_ACTION_ALWAYS)
                AuthState.CLOSED -> createMenuItem(menu, LOGIN_MENU_ID, R.string.shared_string_login,
                        MenuItem.SHOW_AS_ACTION_WITH_TEXT or MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun createMenuItem(m: Menu, id: Int, titleRes: Int, menuItemType: Int): MenuItem {
        val menuItem = m.add(0, id, 0, titleRes)
        menuItem.setOnMenuItemClickListener { item -> onOptionsItemSelected(item) }
        menuItem.setShowAsAction(menuItemType)
        return menuItem
    }

    private fun createProgressMenuItem(m: Menu): MenuItem {

        val menuItem = m.add(0, PROGRESS_MENU_ID, 0, "")
        menuItem.actionView = layoutInflater.inflate(R.layout.action_progress_bar, null)
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return menuItem
    }

    private fun updateTitle() {
        title = when (telegramHelper.getAuthState()) {

            AuthState.UNKNOWN,
            AuthState.WAIT_PHONE_NUMBER,
            AuthState.WAIT_CODE,
            AuthState.WAIT_PASSWORD,
            AuthState.READY,
            AuthState.CLOSED -> getString(R.string.app_name)

            AuthState.WAIT_PARAMETERS -> getString(R.string.initialization) + "..."
            AuthState.LOGGING_OUT -> getString(R.string.logging_out) + "..."
            AuthState.CLOSING -> getString(R.string.closing) + "..."
        }
    }

    private fun showLoginDialog(authParamType: AuthParamType) {
        when (authParamType) {
            AuthParamType.PHONE_NUMBER -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_PHONE_NUMBER)
            AuthParamType.CODE -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_CODE)
            AuthParamType.PASSWORD -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_PASSWORD)
        }
    }

    fun applyAuthParam(loginDialogFragment: LoginDialogFragment?, loginDialogType: LoginDialogType, text: String) {
        loginDialogFragment?.updateDialog(LoginDialogType.SHOW_PROGRESS)
        when (loginDialogType) {
            LoginDialogType.ENTER_PHONE_NUMBER -> authParamRequestHandler?.applyAuthParam(AuthParamType.PHONE_NUMBER, text)
            LoginDialogType.ENTER_CODE -> authParamRequestHandler?.applyAuthParam(AuthParamType.CODE, text)
            LoginDialogType.ENTER_PASSWORD -> authParamRequestHandler?.applyAuthParam(AuthParamType.PASSWORD, text)
            else -> Unit
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        telegramHelper.close()
    }
}
