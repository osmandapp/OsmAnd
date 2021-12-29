package net.osmand.plus.widgets.multistatetoggle;

import android.view.View;

public class RadioItem {

	private boolean isEnabled;

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

	private OnRadioItemClickListener listener;

	public void setOnClickListener(OnRadioItemClickListener listener) {
		this.listener = listener;
	}

	public OnRadioItemClickListener getListener() {
		return listener;
	}

	public interface OnRadioItemClickListener {
		boolean onRadioItemClick(RadioItem radioItem, View view);
	}
}
