package net.osmand.telegram.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.telegram.R
import net.osmand.telegram.ui.views.BottomSheetDialog
import net.osmand.telegram.utils.AndroidNetworkUtils
import net.osmand.telegram.utils.OsmandApiUtils
import org.drinkless.tdlib.TdApi
import org.json.JSONException
import org.json.JSONObject

class AddNewDeviceBottomSheet : BaseDialogFragment() {

	private lateinit var editText: EditText
	private lateinit var errorTextDescription: TextView
	private lateinit var primaryBtn: TextView
	private lateinit var progressBar: ProgressBar

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val dialog = BottomSheetDialog(context!!)
		dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
		return dialog
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_add_new_device, container, false)

		mainView.findViewById<View>(R.id.scroll_view_container).setOnClickListener { dismiss() }

		BottomSheetBehavior.from(mainView.findViewById<View>(R.id.scroll_view))
			.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						dismiss()
					}
				}

				override fun onSlide(bottomSheet: View, slideOffset: Float) {}
			})

		editText = mainView.findViewById<EditText>(R.id.edit_text).apply {
			addTextChangedListener(object :
				TextWatcher {
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

				override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

				override fun afterTextChanged(s: Editable) {
					updateErrorTextDescription(s.toString())
				}
			})
		}

		errorTextDescription = mainView.findViewById<TextView>(R.id.error_text_descr)

		progressBar = mainView.findViewById<ProgressBar>(R.id.progressBar).apply {
			indeterminateDrawable.setColorFilter(ContextCompat.getColor(app, R.color.primary_btn_text_light), android.graphics.PorterDuff.Mode.MULTIPLY)
		}

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_cancel)
			setOnClickListener { dismiss() }
		}

		primaryBtn = mainView.findViewById<TextView>(R.id.primary_btn).apply {
			setText(R.string.shared_string_add)
			setOnClickListener {
				val deviceName = editText.text.toString()
				updateErrorTextDescription(deviceName)
				if (deviceName.isNotEmpty() && deviceName.length < MAX_DEVICE_NAME_LENGTH
					&& !app.settings.containsShareDeviceWithName(deviceName)) {
					val user = app.telegramHelper.getCurrentUser()
					if (user != null) {
						updatePrimaryBtnAndProgress(true)
						createNewDeviceWithName(user, deviceName)
					}
				}
			}
		}
		return mainView
	}

	private fun createNewDeviceWithName(user: TdApi.User, deviceName: String) {
		OsmandApiUtils.createNewDevice(app, user, app.telegramHelper.isBot(user.id), deviceName, 0,
			object : AndroidNetworkUtils.OnRequestResultListener {
				override fun onResult(result: String?) {
					updatePrimaryBtnAndProgress(false)
					val deviceJson = getDeviceJson(result)
					if (deviceJson != null) {
						targetFragment?.also { target ->
							val intent = Intent().putExtra(DEVICE_JSON, deviceJson.toString())
							target.onActivityResult(targetRequestCode, NEW_DEVICE_REQUEST_CODE, intent)
						}
						dismiss()
					} else {
						updateErrorTextDescription(null)
					}
				}
			})
	}

	private fun updateErrorTextDescription(text: String?) {
		when {
			text == null -> {
				errorTextDescription.visibility = View.VISIBLE
				errorTextDescription.text = getString(R.string.error_adding_new_device)
			}
			text.isEmpty() -> {
				errorTextDescription.visibility = View.VISIBLE
				errorTextDescription.text = getString(R.string.device_name_cannot_be_empty)
			}
			text.length > MAX_DEVICE_NAME_LENGTH -> {
				errorTextDescription.visibility = View.VISIBLE
				errorTextDescription.text = getString(R.string.device_name_is_too_long)
			}
			app.settings.containsShareDeviceWithName(text.toString()) -> {
				errorTextDescription.visibility = View.VISIBLE
				errorTextDescription.text = getString(R.string.enter_another_device_name)
			}
			else -> errorTextDescription.visibility = View.INVISIBLE
		}
	}

	private fun updatePrimaryBtnAndProgress(showProgress: Boolean) {
		if (showProgress) {
			progressBar.visibility = View.VISIBLE
			primaryBtn.text = ""
		} else {
			progressBar.visibility = View.GONE
			primaryBtn.setText(R.string.shared_string_add)
		}
	}

	private fun getDeviceJson(json: String?): JSONObject? {
		if (json != null) {
			try {
				val jsonResult = JSONObject(json)
				val status = jsonResult.getString("status")
				if (status == "OK") {
					return jsonResult.getJSONObject("device")
				}
			} catch (e: JSONException) {
			}
		}
		return null
	}

	companion object {
		const val NEW_DEVICE_REQUEST_CODE = 5
		const val DEVICE_JSON = "device_json"
		const val MAX_DEVICE_NAME_LENGTH = 200

		private const val TAG = "AddNewDeviceBottomSheet"
		fun showInstance(fm: androidx.fragment.app.FragmentManager, target: androidx.fragment.app.Fragment): Boolean {
			return try {
				AddNewDeviceBottomSheet().apply {
					setTargetFragment(target, NEW_DEVICE_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}