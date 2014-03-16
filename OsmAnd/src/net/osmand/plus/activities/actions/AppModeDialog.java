package net.osmand.plus.activities.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ToggleButton;
import android.widget.LinearLayout.LayoutParams;

public class AppModeDialog {

	public static View prepareAppModeView(Activity a, final Set<ApplicationMode> selected, boolean showDefault,
			ViewGroup parent, final boolean singleSelection, final View.OnClickListener onClickListener) {
		OsmandSettings settings = ((OsmandApplication) a.getApplication()).getSettings();
		final List<ApplicationMode> values = new ArrayList<ApplicationMode>(ApplicationMode.values(settings));
		if(!showDefault) {
			values.remove(ApplicationMode.DEFAULT);
		}
		if (showDefault || settings.getApplicationMode() != ApplicationMode.DEFAULT) {
			selected.add(settings.getApplicationMode());
		}
		return prepareAppModeView(a, values, selected, parent, singleSelection, onClickListener);
		
	}
	
	public static View prepareAppModeView(Activity a, final List<ApplicationMode> values , final Set<ApplicationMode> selected, 
			ViewGroup parent, final boolean singleSelection, final View.OnClickListener onClickListener) {
		LinearLayout ll = (LinearLayout) a.getLayoutInflater().inflate(R.layout.mode_toggles, parent);
		final ToggleButton[] buttons = createToggles(values, ll, a); 
		final boolean[] selectionChangeLoop = new boolean[] {false};
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] != null) {
				final int ind = i;
				ToggleButton b = buttons[i];
				final ApplicationMode buttonAppMode = values.get(i);
				b.setChecked(selected.contains(buttonAppMode));
				b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (selectionChangeLoop[0]) {
							return;
						}
						selectionChangeLoop[0] = true;
						try {
							handleSelection(values, selected, singleSelection, buttons, ind, buttonAppMode, isChecked);
							if (onClickListener != null) {
								onClickListener.onClick(null);
							}
						} finally {
							selectionChangeLoop[0] = false;
						}
					}

					
				});
			}
		}
		return ll;
	}
	
	private static void handleSelection(final List<ApplicationMode> values,
			final Set<ApplicationMode> selected, final boolean singleSelection,
			final ToggleButton[] buttons, final int ind, final ApplicationMode buttonAppMode,
			boolean isChecked) {
		if (singleSelection) {
			if (isChecked) {
				selected.clear();
				for (int j = 0; j < buttons.length; j++) {
					if (buttons[j] != null) {
						boolean selectedState = ind == j;
						if (selectedState) {
							selected.add(values.get(j));
						} 
						if (buttons[j].isChecked() != selectedState) {
							buttons[j].setChecked(selectedState);
						}
					}
				}
			} else {
				// revert state
				boolean revert = true;
				for (int j = 0; j < buttons.length; j++) {
					if (buttons[j] != null) {
						if (buttons[j].isChecked()) {
							revert = false;
							break;
						}
					}
				}
				if (revert) {
					buttons[ind].setChecked(true);
				}
			}
		} else {
			if (isChecked) {
				selected.add(buttonAppMode);
			} else {
				selected.remove(buttonAppMode);
			}
		}
	}

	static ToggleButton[] createToggles(final List<ApplicationMode> values, LinearLayout topLayout, Context ctx) {
		final ToggleButton[] buttons = new ToggleButton[values.size()];
		HorizontalScrollView scroll = new HorizontalScrollView(ctx);
		
		topLayout.addView(scroll);
		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		scroll.addView(ll);
		
		int k = 0;
		int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, ctx.getResources().getDisplayMetrics());
		int metrics = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, ctx.getResources().getDisplayMetrics());
		for(ApplicationMode ma : values) {
			ToggleButton tb = new ToggleButton(ctx);
			buttons[k++] = tb;
			tb.setTextOn("");
			tb.setTextOff("");
			tb.setContentDescription(ma.toHumanString(ctx));
			tb.setButtonDrawable(ma.getIconId());
			LayoutParams lp = new LinearLayout.LayoutParams(metrics, metrics);
			lp.setMargins(left, 0, 0, 0);
			ll.addView(tb, lp);
		}
		return buttons;
	}
}
