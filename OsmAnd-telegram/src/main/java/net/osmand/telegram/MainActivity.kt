package net.osmand.telegram

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.view.*
import android.widget.Toast
import net.osmand.PlatformUtil
import net.osmand.telegram.LoginDialogFragment.LoginDialogType
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramHelper.*
import net.osmand.telegram.utils.AndroidUtils
import org.drinkless.td.libcore.telegram.TdApi


class MainActivity : AppCompatActivity(), TelegramListener {

    companion object {
        private const val PERMISSION_REQUEST_LOCATION = 1

        private const val LOGIN_MENU_ID = 0
        private const val LOGOUT_MENU_ID = 1
        private const val PROGRESS_MENU_ID = 2
    }

    private val log = PlatformUtil.getLog(TelegramHelper::class.java)

    private var telegramAuthorizationRequestHandler: TelegramAuthorizationRequestHandler? = null
    private var paused: Boolean = false

    private lateinit var chatsView: RecyclerView
    private lateinit var chatViewAdapter: ChatsAdapter
    private lateinit var chatViewManager: RecyclerView.LayoutManager

    private val app: TelegramApplication
        get() = application as TelegramApplication

    private val telegramHelper get() = app.telegramHelper
    private val settings get() = app.settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        paused = false

        chatViewManager = LinearLayoutManager(this)
        chatViewAdapter = ChatsAdapter()

        chatsView = findViewById<RecyclerView>(R.id.groups_view).apply {
            //setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = chatViewManager

            // specify an viewAdapter (see also next example)
            adapter = chatViewAdapter

        }

        telegramAuthorizationRequestHandler = telegramHelper.setTelegramAuthorizationRequestHandler(object : TelegramAuthorizationRequestListener {
            override fun onRequestTelegramAuthenticationParameter(parameterType: TelegramAuthenticationParameterType) {
                runOnUi {
                    showLoginDialog(parameterType)
                }
            }

            override fun onTelegramAuthorizationRequestError(code: Int, message: String) {
                runOnUi {
                    Toast.makeText(this@MainActivity, "$code - $message", Toast.LENGTH_LONG).show()
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

        if (settings.hasAnyChatToShareLocation() && AndroidUtils.isLocationPermissionAvailable(this)) {
            requestLocationPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onStop() {
        super.onStop()
        settings.save()
    }

    override fun onDestroy() {
        super.onDestroy()
        telegramHelper.close()
    }

    override fun onTelegramStatusChanged(prevTelegramAuthorizationState: TelegramAuthorizationState,
                                         newTelegramAuthorizationState: TelegramAuthorizationState) {
        runOnUi {
            val fm = supportFragmentManager
            when (newTelegramAuthorizationState) {
                TelegramAuthorizationState.READY,
                TelegramAuthorizationState.CLOSED,
                TelegramAuthorizationState.UNKNOWN -> LoginDialogFragment.dismiss(fm)
                else -> Unit
            }
            invalidateOptionsMenu()
            updateTitle()

            when (newTelegramAuthorizationState) {
                TelegramAuthorizationState.READY -> {
                    updateChatsList()
                    telegramHelper.requestChats()
                }
                TelegramAuthorizationState.CLOSED,
                TelegramAuthorizationState.UNKNOWN -> {
                    chatViewAdapter.chats = emptyList()
                }
                else -> Unit
            }
        }
    }

    override fun onTelegramChatsRead() {
        runOnUi {
            updateChatsList()
        }
    }

    override fun onTelegramError(code: Int, message: String) {
        runOnUi {
            Toast.makeText(this@MainActivity, "$code - $message", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSendLiveLicationError(code: Int, message: String) {
        log.error("Send live location error: $code - $message")
    }

    private fun updateChatsList() {
        val chatList = telegramHelper.getChatList()
        val chats: MutableList<TdApi.Chat> = mutableListOf()
        for (orderedChat in chatList) {
            val chat = telegramHelper.getChat(orderedChat.chatId)
            if (chat != null) {
                chats.add(chat)
            }
        }
        chatViewAdapter.chats = chats
    }

    fun logoutTelegram(silent: Boolean = false) {
        if (telegramHelper.getTelegramAuthorizationState() == TelegramAuthorizationState.READY) {
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

    private fun runOnUi(action: (() -> Unit)) {
        if (!paused) {
            runOnUiThread(action)
        }
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
            when (telegramHelper.getTelegramAuthorizationState()) {
                TelegramAuthorizationState.UNKNOWN,
                TelegramAuthorizationState.WAIT_PARAMETERS,
                TelegramAuthorizationState.WAIT_PHONE_NUMBER,
                TelegramAuthorizationState.WAIT_CODE,
                TelegramAuthorizationState.WAIT_PASSWORD,
                TelegramAuthorizationState.LOGGING_OUT,
                TelegramAuthorizationState.CLOSING -> createProgressMenuItem(menu)
                TelegramAuthorizationState.READY -> createMenuItem(menu, LOGOUT_MENU_ID, R.string.shared_string_logout,
                        MenuItem.SHOW_AS_ACTION_WITH_TEXT or MenuItem.SHOW_AS_ACTION_ALWAYS)
                TelegramAuthorizationState.CLOSED -> createMenuItem(menu, LOGIN_MENU_ID, R.string.shared_string_login,
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
        title = when (telegramHelper.getTelegramAuthorizationState()) {

            TelegramAuthorizationState.UNKNOWN,
            TelegramAuthorizationState.WAIT_PHONE_NUMBER,
            TelegramAuthorizationState.WAIT_CODE,
            TelegramAuthorizationState.WAIT_PASSWORD,
            TelegramAuthorizationState.READY,
            TelegramAuthorizationState.CLOSED -> getString(R.string.app_name)

            TelegramAuthorizationState.WAIT_PARAMETERS -> getString(R.string.initialization) + "..."
            TelegramAuthorizationState.LOGGING_OUT -> getString(R.string.logging_out) + "..."
            TelegramAuthorizationState.CLOSING -> getString(R.string.closing) + "..."
        }
    }

    private fun showLoginDialog(telegramAuthenticationParameterType: TelegramAuthenticationParameterType) {
        when (telegramAuthenticationParameterType) {
            TelegramAuthenticationParameterType.PHONE_NUMBER -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_PHONE_NUMBER)
            TelegramAuthenticationParameterType.CODE -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_CODE)
            TelegramAuthenticationParameterType.PASSWORD -> LoginDialogFragment.showDialog(supportFragmentManager, LoginDialogType.ENTER_PASSWORD)
        }
    }

    fun applyAuthParam(loginDialogFragment: LoginDialogFragment?, loginDialogType: LoginDialogType, text: String) {
        loginDialogFragment?.updateDialog(LoginDialogType.SHOW_PROGRESS)
        when (loginDialogType) {
            LoginDialogType.ENTER_PHONE_NUMBER -> telegramAuthorizationRequestHandler?.applyAuthenticationParameter(TelegramAuthenticationParameterType.PHONE_NUMBER, text)
            LoginDialogType.ENTER_CODE -> telegramAuthorizationRequestHandler?.applyAuthenticationParameter(TelegramAuthenticationParameterType.CODE, text)
            LoginDialogType.ENTER_PASSWORD -> telegramAuthorizationRequestHandler?.applyAuthenticationParameter(TelegramAuthenticationParameterType.PASSWORD, text)
            else -> Unit
        }
    }

    private fun requestLocationPermission() {
        if (!AndroidUtils.isLocationPermissionAvailable(this)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                PERMISSION_REQUEST_LOCATION -> {
                    if (settings.hasAnyChatToShareLocation()) {
                        app.shareLocationHelper.startSharingLocation()
                    }
                }
            }
        }
    }

    inner class ChatsAdapter :
            RecyclerView.Adapter<ChatsAdapter.ViewHolder>() {

        var chats: List<TdApi.Chat> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val icon: AppCompatImageView? = view.findViewById(R.id.icon)
            val groupName: AppCompatTextView? = view.findViewById(R.id.name)
            val shareLocationSwitch: SwitchCompat? = view.findViewById(R.id.share_location_switch)
            val showOnMapSwitch: SwitchCompat? = view.findViewById(R.id.show_on_map_switch)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsAdapter.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val chatId = chats[position].id
            holder.groupName?.text = chats[position].title
            holder.shareLocationSwitch?.setOnCheckedChangeListener(null)
            holder.shareLocationSwitch?.isChecked = settings.isSharingLocationToChat(chatId)
            holder.shareLocationSwitch?.setOnCheckedChangeListener { view, isChecked ->
                settings.shareLocationToChat(chatId, isChecked)
                if (settings.hasAnyChatToShareLocation()) {
                    if (!AndroidUtils.isLocationPermissionAvailable(view.context)) {
                        requestLocationPermission()
                    } else {
                        app.shareLocationHelper.startSharingLocation()
                    }
                } else {
                    app.shareLocationHelper.stopSharingLocation()
                }
            }
        }

        override fun getItemCount() = chats.size
    }
}
