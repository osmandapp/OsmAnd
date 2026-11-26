package net.osmand.aiconnector

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import net.osmand.aiconnector.ConnectorSettings.RunMode
import net.osmand.aiconnector.assistant.ChatEvent
import net.osmand.aiconnector.assistant.LocalAssistant
import net.osmand.aiconnector.assistant.ModelState
import java.text.DateFormat
import java.util.Date

/**
 * The main screen. On top: which OsmAnd app is used and who uses it, the model on this phone (a chat
 * below, with the model to choose) or an AI assistant on a computer (the connection settings below).
 */
class MainActivity : AppCompatActivity() {

	companion object {
		/** Opens the screen on the computer connection, e.g. from the connector's notification. */
		const val EXTRA_COMPUTER = "computer"
		private const val REQUEST_PERMISSIONS = "net.osmand.aidl.REQUEST_PERMISSIONS"

		/** Groups the assistant asks for: OsmAnd preselects only the safe ones, the user turns on the rest. */
		private val REQUESTED_GROUPS = arrayOf("map", "search", "location", "navigation", "favorites",
			"tracks_view", "tracks_edit", "recording", "screen", "settings")
		private const val ACCESS_STATUS = "access_status"
		private const val MODE = "mode"
		private const val COMPUTER_TAG = "computer"
	}

	private lateinit var assistant: LocalAssistant
	private lateinit var toolbar: MaterialToolbar
	private lateinit var osmandName: TextView
	private lateinit var accessStatus: TextView
	private lateinit var modeToggle: MaterialButtonToggleGroup
	private lateinit var modelRow: View
	private lateinit var modelName: TextView
	private lateinit var modelState: TextView
	private lateinit var progress: LinearProgressIndicator
	private lateinit var chatPanel: View
	private lateinit var computerPanel: View
	private lateinit var inputRow: View
	private lateinit var messages: RecyclerView
	private lateinit var empty: View
	private lateinit var emptyText: TextView
	private lateinit var chooseModel: Button
	private lateinit var examples: ChipGroup
	private lateinit var input: TextView
	private lateinit var send: MaterialButton
	private val adapter = ChatAdapter()

	private val permissionRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		val granted = it.data?.getStringArrayExtra("granted_groups")
		setAccessStatus(if (it.resultCode == RESULT_OK && !granted.isNullOrEmpty()) {
			getString(R.string.access_granted, granted.joinToString())
		} else {
			getString(R.string.access_denied)
		})
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		assistant = (application as AssistantApplication).assistant

		toolbar = findViewById(R.id.toolbar)
		toolbar.inflateMenu(R.menu.main)
		toolbar.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.reset_token -> computerFragment()?.confirmResetToken()
			}
			true
		}

		osmandName = findViewById(R.id.osmand_name)
		accessStatus = findViewById(R.id.access_status)
		findViewById<View>(R.id.osmand_row).setOnClickListener { showOsmandDialog() }
		findViewById<View>(R.id.open_osmand).setOnClickListener { openOsmand() }

		modeToggle = findViewById(R.id.mode_toggle)
		// the saved choice is the only state: restoring the buttons' own state would undo the notification's "Computer"
		modeToggle.isSaveEnabled = false
		if (intent.getBooleanExtra(EXTRA_COMPUTER, false)) prefs().edit().putBoolean(MODE, true).apply()
		modeToggle.check(if (isComputerMode()) R.id.mode_computer else R.id.mode_phone)
		modeToggle.addOnButtonCheckedListener { _, id, checked ->
			if (checked) {
				prefs().edit().putBoolean(MODE, id == R.id.mode_computer).apply()
				showMode()
			}
		}
		modelRow = findViewById(R.id.model_row)
		modelName = findViewById(R.id.model_name)
		modelState = findViewById(R.id.model_state)
		modelRow.setOnClickListener { startActivity(Intent(this, ModelsActivity::class.java)) }

		progress = findViewById(R.id.progress)
		chatPanel = findViewById(R.id.chat_panel)
		computerPanel = findViewById(R.id.computer_panel)
		inputRow = findViewById(R.id.input_row)
		messages = findViewById(R.id.messages)
		messages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
		messages.adapter = adapter
		empty = findViewById(R.id.empty)
		emptyText = findViewById(R.id.empty_text)
		chooseModel = findViewById(R.id.choose_model)
		chooseModel.setOnClickListener { startActivity(Intent(this, ModelsActivity::class.java)) }
		examples = findViewById(R.id.examples)
		for (example in resources.getStringArray(R.array.chat_examples)) {
			examples.addView(Chip(this).apply {
				text = example
				setOnClickListener { submit(example) }
			})
		}
		input = findViewById(R.id.input)
		input.setOnEditorActionListener { _, action, _ ->
			if (action == EditorInfo.IME_ACTION_SEND) submit(input.text.toString())
			true
		}
		findViewById<View>(R.id.chats).setOnClickListener { showChats() }
		send = findViewById(R.id.send)
		send.setOnClickListener {
			if (assistant.busy) assistant.cancel() else submit(input.text.toString())
		}

		if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
		}
		// the computer connection keeps working as before when it runs in the background
		if (ConnectorSettings.isEnabled(this) && ConnectorSettings.runMode(this) == RunMode.BACKGROUND) {
			ConnectorService.start(this)
		}
		showMode()
	}

	/** The connector's notification opens the screen on the computer connection, also when it is open already. */
	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		if (intent.getBooleanExtra(EXTRA_COMPUTER, false)) modeToggle.check(R.id.mode_computer)
	}

	override fun onResume() {
		super.onResume()
		assistant.listener = { refresh() }
		refresh()
	}

	override fun onPause() {
		super.onPause()
		assistant.listener = null
	}

	private fun isComputerMode() = prefs().getBoolean(MODE, false)

	/** The chat or the computer connection; the connection panel lives only while it is shown. */
	private fun showMode() {
		val computer = isComputerMode()
		chatPanel.visibility = if (computer) View.GONE else View.VISIBLE
		inputRow.visibility = chatPanel.visibility
		modelRow.visibility = chatPanel.visibility
		computerPanel.visibility = if (computer) View.VISIBLE else View.GONE
		toolbar.menu.findItem(R.id.reset_token).isVisible = computer
		val fragment = computerFragment()
		if (computer && fragment == null) {
			supportFragmentManager.beginTransaction().replace(R.id.computer_panel, ComputerFragment(), COMPUTER_TAG).commit()
		} else if (!computer && fragment != null) {
			supportFragmentManager.beginTransaction().remove(fragment).commit()
		}
		refresh()
	}

	private fun computerFragment() = supportFragmentManager.findFragmentByTag(COMPUTER_TAG) as ComputerFragment?

	private fun showOsmandDialog() {
		val installed = ConnectorSettings.installedOsmAnd(this)
		val names = installed.map { "${ConnectorSettings.OSMAND_PACKAGES[it]} ($it)" }.toTypedArray()
		val current = installed.indexOf(ConnectorSettings.osmandPackage(this))
		val dialog = MaterialAlertDialogBuilder(this)
			.setTitle(R.string.choose_osmand)
			.setNegativeButton(R.string.check_connection) { _, _ -> checkConnection() }
			.setPositiveButton(R.string.allow_access) { _, _ -> requestAccess() }
		if (installed.isEmpty()) dialog.setMessage(R.string.osmand_not_installed)
		else dialog.setSingleChoiceItems(names, current) { d, which ->
			if (installed[which] != ConnectorSettings.osmandPackage(this)) {
				ConnectorSettings.setOsmandPackage(this, installed[which])
				setAccessStatus(getString(R.string.access_not_checked))
				assistant.osmandChanged()
				// the computer connection serves the chosen app too, whichever tab is shown
				if (ConnectorSettings.isEnabled(this)) ConnectorService.start(this)
				refresh()
			}
			d.dismiss()
		}
		dialog.show()
	}

	/**
	 * The menu: the saved chats, newest first (tap opens one, the bin deletes it), and New session at the
	 * bottom. Chats are saved as they go, there is nothing to save by hand.
	 */
	private fun showChats() {
		val sheet = BottomSheetDialog(this)
		val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		val title = TextView(this).apply {
			setText(R.string.chats)
			setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
			val pad = resources.getDimensionPixelSize(R.dimen.gutter)
			setPadding(pad, pad, pad, pad / 2)
		}
		list.addView(title)
		val chats = assistant.store.list()
		if (chats.isEmpty()) {
			list.addView(TextView(this).apply {
				setText(R.string.no_chats)
				val pad = resources.getDimensionPixelSize(R.dimen.gutter)
				setPadding(pad, pad / 2, pad, pad * 2)
			})
		}
		val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		for (c in chats) {
			val row = layoutInflater.inflate(R.layout.item_chat_summary, list, false)
			row.findViewById<TextView>(R.id.chat_title).text = c.title
			val date = format.format(Date(c.updated))
			row.findViewById<TextView>(R.id.chat_date).text =
				if (c.id == assistant.chatId) getString(R.string.current_chat, date) else date
			row.setOnClickListener {
				assistant.openChat(c.id)
				sheet.dismiss()
			}
			val delete = row.findViewById<View>(R.id.chat_delete)
			delete.isEnabled = !assistant.busy
			delete.setOnClickListener {
				assistant.deleteChat(c.id)
				list.removeView(row)
			}
			list.addView(row)
		}
		list.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
			setText(R.string.new_chat)
			setIconResource(R.drawable.ic_new_chat)
			isEnabled = !assistant.busy
			val pad = resources.getDimensionPixelSize(R.dimen.gutter)
			layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(pad, pad / 2, pad, pad * 2) }
			setOnClickListener {
				assistant.newChat()
				sheet.dismiss()
			}
		})
		sheet.setContentView(ScrollView(this).apply { addView(list) })
		sheet.show()
	}

	private fun openOsmand() {
		val launch = packageManager.getLaunchIntentForPackage(ConnectorSettings.osmandPackage(this))
		if (launch == null) {
			Toast.makeText(this, R.string.osmand_not_installed, Toast.LENGTH_SHORT).show()
		} else {
			startActivity(launch)
		}
	}

	private fun requestAccess() {
		val intent = Intent(REQUEST_PERMISSIONS)
			.setPackage(ConnectorSettings.osmandPackage(this))
			.putExtra("groups", REQUESTED_GROUPS)
		try {
			permissionRequest.launch(intent)
		} catch (e: ActivityNotFoundException) {
			setAccessStatus(getString(R.string.access_old_osmand))
		}
	}

	private fun checkConnection() {
		accessStatus.text = "…"
		assistant.checkOsmand { ok, text ->
			setAccessStatus(if (ok) getString(R.string.check_ok, text) else getString(R.string.check_failed, text))
		}
	}

	private fun setAccessStatus(text: String) {
		prefs().edit().putString(ACCESS_STATUS, text).apply()
		refresh()
	}

	private fun submit(text: String) {
		val t = text.trim()
		if (t.isEmpty() || assistant.busy || !modelReady()) return
		input.text = ""
		assistant.send(t)
	}

	private fun modelReady(): Boolean {
		val m = assistant.models.selected ?: return false
		return assistant.models.state(m) is ModelState.Ready
	}

	private fun refresh() {
		val pack = ConnectorSettings.osmandPackage(this)
		osmandName.text = if (ConnectorSettings.installedOsmAnd(this).isEmpty()) getString(R.string.osmand_not_installed)
		else ConnectorSettings.OSMAND_PACKAGES[pack] ?: pack
		accessStatus.text = prefs().getString(ACCESS_STATUS, null) ?: getString(R.string.access_not_checked)

		val model = assistant.models.selected
		val ready = modelReady()
		modelName.text = when {
			model == null -> getString(R.string.no_model)
			model.recommended -> "${model.title} · ${getString(R.string.recommended)}"
			else -> model.title
		}
		modelState.text = when (val st = model?.let { assistant.models.state(it) }) {
			null -> getString(R.string.model_choose_hint)
			is ModelState.Ready -> getString(R.string.model_ready)
			is ModelState.Downloading -> getString(R.string.model_downloading,
				if (st.total > 0) (st.done * 100 / st.total).toInt() else 0)
			is ModelState.Failed -> getString(R.string.model_failed)
			else -> getString(R.string.model_not_downloaded_hint)
		}
		progress.visibility = if (assistant.busy && !isComputerMode()) View.VISIBLE else View.INVISIBLE
		send.setIconResource(if (assistant.busy) R.drawable.ic_stop else R.drawable.ic_send)
		send.contentDescription = getString(if (assistant.busy) R.string.stop else R.string.send)
		val noChat = assistant.events.isEmpty()
		empty.visibility = if (noChat) View.VISIBLE else View.GONE
		emptyText.setText(if (ready) R.string.chat_empty else R.string.chat_choose_model)
		chooseModel.visibility = if (ready) View.GONE else View.VISIBLE
		examples.visibility = if (ready) View.VISIBLE else View.GONE
		adapter.submit(assistant.events)
		if (!noChat) messages.scrollToPosition(adapter.itemCount - 1)
	}

	private fun prefs() = getSharedPreferences("connector_ui", Context.MODE_PRIVATE)

	/** The values of a tool call in one short line: daynight_mode NIGHT, not the JSON. */
	private fun shortArgs(args: String): String = try {
		val o = org.json.JSONObject(args)
		o.keys().asSequence().joinToString(" ") { o.opt(it).toString() }.let { if (it.length > 48) it.take(47) + "…" else it }
	} catch (e: Exception) {
		args.take(48)
	}

	private inner class ChatAdapter : RecyclerView.Adapter<ChatAdapter.Holder>() {

		private var items: List<ChatEvent> = emptyList()

		/** Tool calls show one line; a tap opens the result. */
		private val expanded = mutableSetOf<ChatEvent.Tool>()

		inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
			val row: LinearLayout = view.findViewById(R.id.row)
			val text: TextView = view.findViewById(R.id.text)
			val meta: TextView = view.findViewById(R.id.meta)
		}

		fun submit(events: List<ChatEvent>) {
			items = events.toList()
			notifyDataSetChanged()
		}

		override fun getItemCount() = items.size

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
			Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false))

		override fun onBindViewHolder(h: Holder, position: Int) {
			val e = items[position]
			var gravity = Gravity.START
			var bubble = MaterialColors.getColor(h.row, com.google.android.material.R.attr.colorSurfaceContainerHigh)
			var textColor = MaterialColors.getColor(h.row, com.google.android.material.R.attr.colorOnSurface)
			var meta: String? = null
			var small = false
			h.text.setTextIsSelectable(e !is ChatEvent.Tool)
			h.text.setOnClickListener(null)
			when (e) {
				is ChatEvent.User -> {
					h.text.text = e.text
					gravity = Gravity.END
					bubble = MaterialColors.getColor(h.row, com.google.android.material.R.attr.colorPrimaryContainer)
					textColor = MaterialColors.getColor(h.row, com.google.android.material.R.attr.colorOnPrimaryContainer)
				}
				is ChatEvent.Answer -> {
					h.text.text = e.text
					meta = e.stats
				}
				is ChatEvent.Tool -> {
					val mark = when {
						e.result == null -> "…"
						e.failed -> "✗"
						else -> "✓"
					}
					val open = e in expanded
					h.text.text = "${if (open) "▾" else "▸"} ⚙ ${e.name}  ${shortArgs(e.args)}  $mark"
					meta = if (open) "${e.args}\n${e.result ?: ""}".take(1500) else null
					h.text.setOnClickListener {
						if (!expanded.remove(e)) expanded.add(e)
						notifyItemChanged(h.bindingAdapterPosition)
					}
					small = true
					bubble = MaterialColors.getColor(h.row, com.google.android.material.R.attr.colorSecondaryContainer)
					textColor = MaterialColors.getColor(h.row, com.google.android.material.R.attr.colorOnSecondaryContainer)
				}
				is ChatEvent.Info -> {
					h.text.text = e.text
					small = true
					bubble = 0
					textColor = MaterialColors.getColor(h.row, com.google.android.material.R.attr.colorOnSurfaceVariant)
				}
				is ChatEvent.Error -> {
					h.text.text = e.text
					bubble = MaterialColors.getColor(h.row, com.google.android.material.R.attr.colorErrorContainer)
					textColor = MaterialColors.getColor(h.row, com.google.android.material.R.attr.colorOnErrorContainer)
				}
			}
			h.row.gravity = gravity
			h.text.backgroundTintList = ColorStateList.valueOf(bubble)
			h.text.setTextColor(textColor)
			h.text.setTextAppearance(if (small) com.google.android.material.R.style.TextAppearance_Material3_BodySmall
			else com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
			h.text.setTextColor(textColor)
			h.meta.text = meta
			h.meta.visibility = if (meta.isNullOrEmpty()) View.GONE else View.VISIBLE
		}
	}
}
