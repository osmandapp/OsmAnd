package net.osmand.telegram.ui

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import net.osmand.telegram.R
import net.osmand.telegram.TelegramSettings.*

class ProxySettingsDialogFragment : BaseDialogFragment() {

	private val uiUtils get() = app.uiUtils

	private lateinit var mainView: View
	private lateinit var proxyEnableSwitcher: Switch
	private lateinit var saveButtonContainer: LinearLayout

	private lateinit var selectedProxyType: ProxyType

	private lateinit var serverEditText: EditText
	private lateinit var portEditText: EditText

	override fun onCreateView(
		inflater: LayoutInflater,
		parent: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		mainView = inflater.inflate(R.layout.fragment_proxy_settings_dialog, parent)

		val window = dialog?.window
		if (window != null) {
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
			if (Build.VERSION.SDK_INT >= 21) {
				window.statusBarColor = ContextCompat.getColor(app, R.color.card_bg_light)
			}
		}
		mainView.findViewById<Toolbar>(R.id.toolbar).apply {
			navigationIcon = uiUtils.getThemedIcon(R.drawable.ic_arrow_back)
			setNavigationOnClickListener { dismiss() }
		}

		selectedProxyType = settings.currentProxyPref.type

		mainView.findViewById<ViewGroup>(R.id.enable_proxy_btn).apply {
			val title = findViewById<TextView>(R.id.title).apply {
				text = if (settings.proxyEnabled) getText(R.string.shared_string_disable) else getText(
						R.string.shared_string_enable
					)
			}
			proxyEnableSwitcher = findViewById<Switch>(R.id.switcher).apply {
				isChecked = settings.proxyEnabled
			}
			setOnClickListener {
				val checked = !proxyEnableSwitcher.isChecked
				proxyEnableSwitcher.isChecked = checked
				title.text = if (checked) getText(R.string.shared_string_disable) else getText(R.string.shared_string_enable)
				updateSaveButtonVisibility(true)
			}
		}

		val container = mainView.findViewById<ViewGroup>(R.id.proxy_type_container)
		ProxyType.values().forEach {
			addItemToContainer(inflater, container, it)
		}

		serverEditText = mainView.findViewById<EditText>(R.id.server_edit_text).apply {
			val server = settings.currentProxyPref.server
			setText(server)
			setSelection(server.length)
			addTextChangedListener(object :
				TextWatcher {
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
				override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

				override fun afterTextChanged(s: Editable) {
					updateSaveButtonVisibility(s.isNotEmpty() && portEditText.text.isNotEmpty())
				}
			})
		}
		portEditText = mainView.findViewById<EditText>(R.id.port_edit_text).apply {
			val port = settings.currentProxyPref.port
			setText(if (port != -1) port.toString() else "")
			addTextChangedListener(object :
				TextWatcher {
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
				override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

				override fun afterTextChanged(s: Editable) {
					updateSaveButtonVisibility(s.isNotEmpty() && serverEditText.text.isNotEmpty())
				}
			})
		}
		saveButtonContainer = mainView.findViewById<LinearLayout>(R.id.save_button_Container).apply {
				findViewById<TextView>(R.id.primary_btn).apply {
					text = getString(R.string.shared_string_save)
					setOnClickListener {
						saveChanges()
						targetFragment?.also { target ->
							target.onActivityResult(targetRequestCode, PROXY_PREFERENCES_UPDATED_REQUEST_CODE, null)
						}
						dismiss()
					}
				}
			}

		updateSelectedProxyType()
		updateEditingMode()
		updateProxyPrefInfo()
		updateSaveButtonVisibility(false)

		return mainView
	}

	private fun updateSaveButtonVisibility(visible: Boolean) {
		saveButtonContainer.visibility = if (visible) View.VISIBLE else View.GONE
	}

	private fun saveChanges() {
		val proxyPref = getSelectedProxyPref()
		settings.updateCurrentProxyPref(proxyPref, proxyEnableSwitcher.isChecked)
	}

	private fun updateProxyPrefInfo() {
		val proxyPref = settings.currentProxyPref
		if (proxyPref is ProxyMTProtoPref) {
			mainView.findViewById<TextView>(R.id.key_text).text = proxyPref.key
		} else if (proxyPref is ProxySOCKS5Pref) {
			mainView.findViewById<TextView>(R.id.username_text).text = proxyPref.login
			mainView.findViewById<TextView>(R.id.password_text).text = proxyPref.password
		}
	}

	private fun getSelectedProxyPref(): ProxyPref {
		val server = serverEditText.text.toString()
		val port = portEditText.text.toString().toIntOrNull() ?: -1
		return when (selectedProxyType) {
			ProxyType.MTPROTO -> {
				val key = mainView.findViewById<TextView>(R.id.key_text).text.toString()
				ProxyMTProtoPref(settings.currentProxyPref.id, server, port, key)
			}
			ProxyType.SOCKS5 -> {
				val username = mainView.findViewById<TextView>(R.id.username_text).text.toString()
				val password = mainView.findViewById<TextView>(R.id.password_text).text.toString()
				ProxySOCKS5Pref(settings.currentProxyPref.id, server, port, username, password)
			}
		}
	}

	private fun updateSelectedProxyType() {
		view?.findViewById<ViewGroup>(R.id.proxy_type_container)?.apply {
			for (i in 0 until childCount) {
				getChildAt(i).apply {
					findViewById<RadioButton>(R.id.radio_button).isChecked = tag == selectedProxyType
				}
			}
		}
	}

	private fun updateEditingMode() {
		mainView.findViewById<LinearLayout>(R.id.proxy_sosks5_container)?.visibility =
			if (selectedProxyType == ProxyType.SOCKS5) View.VISIBLE else View.GONE
		mainView.findViewById<LinearLayout>(R.id.proxy_mtproto_container)?.visibility =
			if (selectedProxyType == ProxyType.MTPROTO) View.VISIBLE else View.GONE
	}

	private fun addItemToContainer(
		inflater: LayoutInflater,
		container: ViewGroup,
		proxyTypeTag: ProxyType
	) {
		inflater.inflate(R.layout.item_with_rb_and_btn, container, false).apply {
			findViewById<TextView>(R.id.title).text = proxyTypeTag.name
			findViewById<View>(R.id.primary_btn).visibility = View.GONE
			findViewById<View>(R.id.icon).visibility = View.GONE
			findViewById<RadioButton>(R.id.radio_button).isChecked = selectedProxyType == proxyTypeTag

			setOnClickListener {
				selectedProxyType = proxyTypeTag
				updateSelectedProxyType()
				updateEditingMode()
				updateSaveButtonVisibility(selectedProxyType != settings.currentProxyPref.type && portEditText.text.isNotEmpty() && serverEditText.text.isNotEmpty())
			}
			this.tag = proxyTypeTag
			container.addView(this)
		}
	}

	companion object {

		private const val TAG = "ProxySettingsDialogFragment"
		const val PROXY_PREFERENCES_UPDATED_REQUEST_CODE = 6

		fun showInstance(fm: androidx.fragment.app.FragmentManager, target: androidx.fragment.app.Fragment): Boolean {
			return try {
				ProxySettingsDialogFragment().apply {
					setTargetFragment(target, PROXY_PREFERENCES_UPDATED_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}