package net.osmand.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import androidx.appcompat.widget.AppCompatCheckBox;

import net.osmand.plus.R;

public class ThreeStateCheckbox extends AppCompatCheckBox {

	private State state;

	public ThreeStateCheckbox(Context context) {
		super(context);
		init();
	}

	public ThreeStateCheckbox(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ThreeStateCheckbox(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		state = State.MISC;
		updateBtn();
		setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				switch (state) {
					case MISC:
					case CHECKED:
						state = State.UNCHECKED;
						break;
					case UNCHECKED:
						state = State.CHECKED;
						break;
				}
				updateBtn();
			}
		});
	}

	private void updateBtn() {
		int btnDrawable = R.drawable.ic_checkbox_indeterminate;
		switch (state) {
			default:
			case MISC:
				break;
			case UNCHECKED:
				btnDrawable = R.drawable.ic_check_box_outline_light;
				break;
			case CHECKED:
				btnDrawable = R.drawable.ic_check_box_light;
				break;
		}
		setButtonDrawable(btnDrawable);
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
		updateBtn();
	}

	public enum State {
		UNCHECKED,
		CHECKED,
		MISC
	}
}
