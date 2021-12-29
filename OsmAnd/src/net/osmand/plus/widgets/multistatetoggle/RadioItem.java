package net.osmand.plus.widgets.multistatetoggle;

import android.view.View;

public class RadioItem {

	private boolean enabled = true;
	private OnRadioItemClickListener listener;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

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
