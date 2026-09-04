package net.osmand.plus.widgets.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.materialswitch.MaterialSwitch
import net.osmand.plus.R
import net.osmand.plus.widgets.TextViewEx

/**
 * Master toggle at the top of a feature or plugin screen. One per screen, always the first
 * element, controls everything below it.
 *
 * When off, the content below is hidden and replaced by an empty state - it is not disabled,
 * because a screen full of greyed out controls gives the user nothing to act on.
 */
class MainSwitchView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

	private val labelView: TextViewEx
	private val switchView: MaterialSwitch

	/**
	 * true - the label reads On / Off, because the app bar title above already names the feature.
	 * false - the label is the feature name, and the footer has to state the value in words.
	 */
	private var useStateLabel = true
	private var featureLabel: CharSequence? = null
	private var listener: OnCheckedChangeListener? = null

	fun interface OnCheckedChangeListener {
		fun onCheckedChanged(checked: Boolean)
	}

	init {
		orientation = HORIZONTAL
		gravity = android.view.Gravity.CENTER_VERTICAL
		/* the vertical padding sits on the label, so the 32dp switch fits into the 56dp row
		 * instead of stretching it to the height of its own touch target */
		minimumHeight = resources.getDimensionPixelSize(R.dimen.ui_main_switch_height)
		val padding = resources.getDimensionPixelSize(R.dimen.content_padding)
		setPaddingRelative(padding, 0, padding, 0)
		isClickable = true
		isFocusable = true

		inflate(context, R.layout.ui_main_switch, this)
		labelView = findViewById(R.id.label)
		switchView = findViewById(R.id.switch_widget)

		setOnClickListener { setChecked(!switchView.isChecked, true) }

		var checked = false
		attrs?.let { attributeSet ->
			val typedArray =
				context.obtainStyledAttributes(attributeSet, R.styleable.MainSwitchView)
			try {
				featureLabel = typedArray.getString(R.styleable.MainSwitchView_mainSwitchLabel)
				useStateLabel =
					typedArray.getBoolean(R.styleable.MainSwitchView_mainSwitchUseStateLabel, true)
				checked = typedArray.getBoolean(R.styleable.MainSwitchView_mainSwitchChecked, false)
			} finally {
				typedArray.recycle()
			}
		}
		setChecked(checked, false)
	}

	/**
	 * @param label the feature name. It is used as the visible label when the screen does not
	 * name the feature above the switch, and as the content description otherwise.
	 */
	fun setLabel(label: CharSequence?, useStateLabel: Boolean) {
		this.featureLabel = label
		this.useStateLabel = useStateLabel
		updateLabel()
	}

	fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
		this.listener = listener
	}

	fun isChecked(): Boolean = switchView.isChecked

	fun setChecked(checked: Boolean, notifyListener: Boolean) {
		switchView.isChecked = checked
		setBackgroundResource(
			if (checked) R.drawable.bg_ui_main_switch_on else R.drawable.bg_ui_main_switch_off)
		updateLabel()
		if (notifyListener) {
			listener?.onCheckedChanged(checked)
		}
	}

	private fun updateLabel() {
		if (useStateLabel) {
			labelView.setText(
				if (switchView.isChecked) R.string.shared_string_on else R.string.shared_string_off)
			contentDescription = featureLabel
		} else {
			labelView.text = featureLabel
			contentDescription = null
		}
	}
}
