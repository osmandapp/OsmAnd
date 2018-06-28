package net.osmand.telegram.ui

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication

class SetTimeDialogFragment : DialogFragment() {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_NoActionbar)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val view = inflater.inflate(R.layout.fragment_set_time_dialog, container)

		view.findViewById<View>(R.id.time_for_all_row).apply {
			findViewById<ImageView>(R.id.time_for_all_icon).setImageDrawable(
				app.uiUtils.getIcon(R.drawable.ic_action_time_span, R.color.ctrl_active_light)
			)
			findViewById<TextView>(R.id.time_for_all_value).text = "1 hour"
			setOnClickListener {
				Toast.makeText(context, "Time for all", Toast.LENGTH_SHORT).show()
			}
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
				Toast.makeText(context, "Share", Toast.LENGTH_SHORT).show()
			}
		}

		return view
	}

	companion object {

		private const val TAG = "SetTimeDialogFragment"
		private const val CHATS_KEY = "chats_key"

		fun showInstance(fm: FragmentManager, chats: Set<Long>): Boolean {
			return try {
				val fragment = SetTimeDialogFragment()
				fragment.arguments = Bundle().apply {
					putLongArray(CHATS_KEY, chats.toLongArray())
				}
				fragment.show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
