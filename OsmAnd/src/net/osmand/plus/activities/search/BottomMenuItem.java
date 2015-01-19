package net.osmand.plus.activities.search;

import android.view.View;

/**
 * Created by Denis
 * on 19.01.2015.
 */
public class BottomMenuItem {
	private int icon;
	private View.OnClickListener onClickListener;
	private int msg;

	public BottomMenuItem setIcon(int iconId) {
		this.icon = iconId;
		return BottomMenuItem.this;
	}

	public BottomMenuItem setMsg(int message) {
		this.msg = message;
		return BottomMenuItem.this;
	}

	public BottomMenuItem setOnClickListener(View.OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
		return BottomMenuItem.this;
	}

	public int getIcon() {
		return icon;
	}

	public int getMsg() {
		return msg;
	}

	public View.OnClickListener getOnClickListener(){
		return onClickListener;
	}
}