package net.osmand.telegram.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramHelper.TelegramAuthorizationState
import net.osmand.telegram.helpers.TelegramHelper.TelegramListener
import org.drinkless.td.libcore.telegram.TdApi

class LiveNowTabFragment : Fragment(), TelegramListener {

	companion object {
		private const val CHAT_VIEW_TYPE = 0
		private const val CONTACT_VIEW_TYPE = 1
	}

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val telegramHelper get() = app.telegramHelper

	private val adapter = LiveNowListAdapter()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val mainView = inflater.inflate(R.layout.fragment_live_now_tab, container, false)
		mainView.findViewById<RecyclerView>(R.id.recycler_view).apply {
			layoutManager = LinearLayoutManager(context)
			adapter = this@LiveNowTabFragment.adapter
		}
		return mainView
	}

	override fun onTelegramStatusChanged(prevTelegramAuthorizationState: TelegramAuthorizationState,
										 newTelegramAuthorizationState: TelegramAuthorizationState) {
		// TODO: update list
	}

	override fun onTelegramChatsRead() {
		updateList()
	}

	override fun onTelegramChatsChanged() {
		updateList()
	}

	override fun onTelegramChatChanged(chat: TdApi.Chat) {
		updateList()
	}

	override fun onTelegramUserChanged(user: TdApi.User) {
		updateList()
	}

	override fun onTelegramError(code: Int, message: String) {
	}

	override fun onSendLiveLocationError(code: Int, message: String) {
	}

	private fun updateList() {
		val res = mutableListOf<Any>()
		for ((id, messages) in telegramHelper.getMessagesByChatIds()) {
			telegramHelper.getChat(id)?.let { chat ->
				res.add(chat)
				if (chat.type !is TdApi.ChatTypePrivate && chat.type !is TdApi.ChatTypeSecret && messages.size > 1) {
					messages.forEach { message ->
						telegramHelper.getUser(message.senderUserId)?.let { user ->
							res.add(user)
						}
					}
				}
			}
		}
		adapter.items = res
	}

	inner class LiveNowListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

		var items: List<Any> = emptyList()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		override fun getItemViewType(position: Int): Int {
			return when (items[position]) {
				is TdApi.Chat -> CHAT_VIEW_TYPE
				else -> CONTACT_VIEW_TYPE
			}
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val inflater = LayoutInflater.from(parent.context)
			return when (viewType) {
				CHAT_VIEW_TYPE -> ChatViewHolder(inflater.inflate(R.layout.live_now_chat_card, parent, false))
				else -> ContactViewHolder(inflater.inflate(R.layout.live_now_contact_item, parent, false))
			}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		}

		override fun getItemCount() = items.size

		inner class ContactViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val description: TextView? = view.findViewById(R.id.description)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
		}

		inner class ChatViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
			val icon: ImageView? = view.findViewById(R.id.icon)
			val title: TextView? = view.findViewById(R.id.title)
			val description: TextView? = view.findViewById(R.id.description)
			val imageButton: ImageView? = view.findViewById(R.id.image_button)
			val showOnMapRow: View? = view.findViewById(R.id.show_on_map_row)
			val showOnMapSwitch: Switch? = view.findViewById(R.id.show_on_map_switch)
			val bottomDivider: View? = view.findViewById(R.id.bottom_divider)
			val bottomShadow: View? = view.findViewById(R.id.bottom_shadow)
		}
	}
}
