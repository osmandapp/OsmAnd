package net.osmand.plus.quickaction.actions;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.MapScrollHelper;
import net.osmand.plus.helpers.MapScrollHelper.ScrollDirection;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public abstract class BaseMapScrollAction extends QuickAction {

	public BaseMapScrollAction(QuickActionType type) {
		super(type);
	}

	public BaseMapScrollAction(QuickAction quickAction) {
		super(quickAction);
	}

	@NonNull
	protected abstract ScrollDirection getScrollingDirection();

	@StringRes
	public abstract int getQuickActionDescription();

	@Override
	public boolean onKeyDown(@NonNull MapActivity mapActivity, int keyCode, KeyEvent event) {
		MapScrollHelper scrollHelper = mapActivity.getMapScrollHelper();
		scrollHelper.startScrolling(getScrollingDirection());
		return true;
	}

	@Override
	public boolean onKeyUp(@NonNull MapActivity mapActivity, int keyCode, KeyEvent event) {
		MapScrollHelper scrollHelper = mapActivity.getMapScrollHelper();
		scrollHelper.removeDirection(getScrollingDirection());
		return true;
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		MapScrollHelper scrollHelper = mapActivity.getMapScrollHelper();
		scrollHelper.scrollMapAction(getScrollingDirection());
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(getQuickActionDescription()));
		parent.addView(view);
	}
}
