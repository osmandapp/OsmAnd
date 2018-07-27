package net.osmand.telegram.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.ShareLocationHelper
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.ui.SetTimeDialogFragment.SetTimeListAdapter.ChatViewHolder
import net.osmand.telegram.utils.AndroidUtils
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.TimeUnit

class SetTimeDialogFragment : DialogFragment() {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper
	private val settings get() = app.settings

	private val adapter = SetTimeListAdapter()

	private lateinit var timeForAllTitle: TextView
	private lateinit var timeForAllValue: TextView

	private val chatLivePeriods = HashMap<Long, Long>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_NoActionbar)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		readFromBundle(savedInstanceState ?: arguments)

		val view = inflater.inflate(R.layout.fragment_set_time_dialog, container)

		view.findViewById<View>(R.id.time_for_all_row).apply {
			findViewById<ImageView>(R.id.time_for_all_icon).setImageDrawable(
				app.uiUtils.getIcon(R.drawable.ic_action_time_span, R.color.ctrl_active_light)
			)
			timeForAllTitle = findViewById(R.id.time_for_all_title)
			timeForAllValue = findViewById(R.id.time_for_all_value)
			setOnClickListener {
				selectDuration()
			}
		}

		updateTimeForAllRow()

		view.findViewById<RecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@SetTimeDialogFragment.adapter
		}

		view.findViewById<TextView>(R.id.secondary_btn).apply {
			text = getString(R.string.shared_string_back)
			setOnClickListener {
				dismiss()
			}
		}

		view.findViewById<TextView>(R.id.primary_btn).apply {
			text = getString(R.string.shared_string_share)
			setOnClickListener {
				if (!AndroidUtils.isLocationPermissionAvailable(view.context)) {
					AndroidUtils.requestLocationPermission(activity!!)
				} else {
					chatLivePeriods.forEach { chatId, livePeriod ->
						settings.shareLocationToChat(chatId, true, livePeriod)
					}
					app.shareLocationHelper.startSharingLocation()
					targetFragment?.also {
						it.onActivityResult(targetRequestCode, LOCATION_SHARED_REQUEST_CODE, null)
					}
					dismiss()
				}
			}
		}

		return view
	}

	override fun onResume() {
		super.onResume()
		updateList()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		val chats = mutableListOf<Long>()
		for ((id, livePeriod) in chatLivePeriods) {
			chats.add(id)
			chats.add(livePeriod)
		}
		outState.putLongArray(CHATS_KEY, chats.toLongArray())
	}

	private fun readFromBundle(bundle: Bundle?) {
		chatLivePeriods.clear()
		bundle?.getLongArray(CHATS_KEY)?.also {
			for (i in 0 until it.size step 2) {
				val livePeriod = settings.getChatLivePeriod(it[i])
				chatLivePeriods[it[i]] = livePeriod ?: it[i + 1]
			}
		}
	}

	private fun getTimeForAll(useDefValue: Boolean = false): Long {
		val returnVal = if (useDefValue) DEFAULT_VISIBLE_TIME_SECONDS else NO_VALUE
		val iterator = chatLivePeriods.values.iterator()
		if (!iterator.hasNext()) {
			return returnVal
		}
		val first = iterator.next()
		while (iterator.hasNext()) {
			if (first != iterator.next()) {
				return returnVal
			}
		}
		return first
	}

	private fun updateTimeForAllRow() {
		val timeForAll = getTimeForAll()
		if (timeForAll != NO_VALUE) {
			timeForAllTitle.text = getString(R.string.visible_time_for_all)
			timeForAllValue.visibility = View.VISIBLE
			timeForAllValue.text = formatLivePeriod(timeForAll)
		} else {
			timeForAllTitle.text = getString(R.string.set_visible_time_for_all)
			timeForAllValue.visibility = View.GONE
		}
	}

	private fun selectDuration(id: Long? = null) {
		val timeForAll = getTimeForAll(true)
		val defSeconds = if (id == null) timeForAll else chatLivePeriods[id] ?: timeForAll
		val (defHours, defMinutes) = secondsToHoursAndMinutes(defSeconds)
		TimePickerDialog(
			context,
			TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
				val seconds = TimeUnit.HOURS.toSeconds(hours.toLong()) +
						TimeUnit.MINUTES.toSeconds(minutes.toLong())
				if (seconds >= ShareLocationHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC) {
					if (id != null) {
						chatLivePeriods[id] = seconds
					} else {
						chatLivePeriods.keys.forEach {
							chatLivePeriods[it] = seconds
						}
					}
					updateTimeForAllRow()
					adapter.notifyDataSetChanged()
				}
			}, defHours, defMinutes, true
		).show()
	}

	private fun secondsToHoursAndMinutes(seconds: Long): Pair<Int, Int> {
		val hours = TimeUnit.SECONDS.toHours(seconds)
		val minutes = TimeUnit.SECONDS.toMinutes(seconds - TimeUnit.HOURS.toSeconds(hours))
		return Pair(hours.toInt(), minutes.toInt())
	}

	private fun formatLivePeriod(seconds: Long): String {
		val (hours, minutes) = secondsToHoursAndMinutes(seconds)
		return when {
			hours != 0 && minutes == 0 -> getString(R.string.hours_format, hours)
			hours == 0 && minutes != 0 -> getString(R.string.minutes_format, minutes)
			else -> getString(R.string.hours_and_minutes_format, hours, minutes)
		}
	}

	private fun updateList() {
		val chats: MutableList<TdApi.Chat> = mutableListOf()
		telegramHelper.getChatList().filter { chatLivePeriods.keys.contains(it.chatId) }
			.forEach { orderedChat ->
				telegramHelper.getChat(orderedChat.chatId)?.also { chats.add(it) }
			}
		adapter.chats = chats
	}

	inner class SetTimeListAdapter : RecyclerView.Adapter<ChatViewHolder>() {

		var chats: List<TdApi.Chat> = emptyList()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
			val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.user_list_item, parent, false)
			return ChatViewHolder(view)
		}

		override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
			val chat = chats[position]
			val placeholderId = if (telegramHelper.isGroup(chat)) R.drawable.img_group_picture else R.drawable.img_user_picture

			TelegramUiHelper.setupPhoto(app, holder.icon, chat.photo?.small?.local?.path, placeholderId, false)
			holder.title?.text = chat.title
			holder.description?.text = "Some description" // FIXME
			holder.textInArea?.apply {
				visibility = View.VISIBLE
				chatLivePeriods[chat.id]?.also { text = formatLivePeriod(it) }
			}
			holder.bottomShadow?.visibility = View.GONE
			holder.itemView.setOnClickListener {
				selectDuration(chat.id)
			}
		}

		override fun getItemCount() = chats.size

		inner class ChatViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val description: TextView? = view.findViewById(R.id.description)
			val textInArea: TextView? = view.findViewById(R.id.text_in_area)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
		}
	}

	companion object {

		const val LOCATION_SHARED_REQUEST_CODE = 0

		private const val TAG = "SetTimeDialogFragment"
		private const val CHATS_KEY = "chats_key"
		private const val DEFAULT_VISIBLE_TIME_SECONDS = 60 * 60L // 1 hour
		private const val NO_VALUE = -1L

		fun showInstance(fm: FragmentManager, chatIds: Set<Long>, target: Fragment): Boolean {
			return try {
				val chats = mutableListOf<Long>()
				for (id in chatIds) {
					chats.add(id)
					chats.add(DEFAULT_VISIBLE_TIME_SECONDS)
				}
				SetTimeDialogFragment().apply {
					arguments = Bundle().apply { putLongArray(CHATS_KEY, chats.toLongArray()) }
					setTargetFragment(target, LOCATION_SHARED_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
