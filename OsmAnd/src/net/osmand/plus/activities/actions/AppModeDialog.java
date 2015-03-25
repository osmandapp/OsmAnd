package net.osmand.plus.activities.actions;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ToggleButton;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
		return prepareAppModeView(a, values, selected, parent, singleSelection, false, onClickListener);
	}

	//special method for drawer menu
	//needed because if there's more than 4 items  - the don't fit in drawer
	public static View prepareAppModeDrawerView(Activity a, List<ApplicationMode> visible, final Set<ApplicationMode> selected, ContextMenuAdapter.BooleanResult allModes,
												final View.OnClickListener onClickListener) {
		OsmandSettings settings = ((OsmandApplication) a.getApplication()).getSettings();
		final List<ApplicationMode> values = new ArrayList<ApplicationMode>(ApplicationMode.values(settings));
		selected.add(settings.getApplicationMode());
		if (values.size() > 4) {
			return createDrawerView(a, visible, values, selected, allModes, onClickListener);
		} else {
			return prepareAppModeView(a, values, selected, null, true, true, onClickListener);
		}
	}

	private static View createDrawerView(Activity a, final List<ApplicationMode> visible, final List<ApplicationMode> allModes,
										 final Set<ApplicationMode> selected, ContextMenuAdapter.BooleanResult allModesVisible,
										 final View.OnClickListener onClickListener) {
		getVisibleModes(allModes, selected, visible);
		LinearLayout ll = (LinearLayout) a.getLayoutInflater().inflate(R.layout.mode_toggles, null);
		if (allModesVisible.getResult()) {
			createAllToggleButtons(allModes, selected, onClickListener, a, ll);
		} else {
			final ToggleButton[] buttons = createDrawerToggles(visible, allModes, selected, a, ll, allModesVisible, onClickListener);
			for (int i = 0; i < buttons.length - 1; i++) {
				setButtonListener(visible, selected, onClickListener, buttons, i, true);
			}
		}
		return ll;
	}

	private static void setButtonListener(final List<ApplicationMode> visible, final Set<ApplicationMode> selected,
										  final View.OnClickListener onClickListener, final ToggleButton[] buttons,
										  int i,final boolean singleChoise) {
		final boolean[] selectionChangeLoop = new boolean[] {false};
		if (buttons[i] != null) {
			final int ind = i;
			ToggleButton b = buttons[i];
			final ApplicationMode buttonAppMode = visible.get(i);
			b.setChecked(selected.contains(buttonAppMode));
			b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (selectionChangeLoop[0]) {
						return;
					}
					selectionChangeLoop[0] = true;
					try {
						handleSelection(visible, selected, singleChoise, buttons, ind, buttonAppMode, isChecked);
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

	private static ToggleButton[] createDrawerToggles(final List<ApplicationMode> visible,final List<ApplicationMode> modes,
													  final Set<ApplicationMode> selected,
													  final Activity a, final LinearLayout ll, final ContextMenuAdapter.BooleanResult allModes, final View.OnClickListener onClickListener) {
		ToggleButton[] buttons = createToggles(visible, ll, (OsmandApplication)a.getApplication(), true);
		ToggleButton[] newButtons = new ToggleButton[buttons.length + 1];
		for (int i = 0; i < buttons.length; i++) {
			newButtons[i] = buttons[i];
		}
		ToggleButton tb = new ToggleButton(a);
		newButtons[buttons.length] = tb;
		tb.setTextOn("");
		tb.setTextOff("");
		int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, a.getResources().getDisplayMetrics());
		int metrics = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, a.getResources().getDisplayMetrics());
		tb.setButtonDrawable(R.drawable.ic_other_modes);
		LayoutParams lp = new LayoutParams(metrics, metrics);
		lp.setMargins(left, 0, 0, 0);
		tb.setChecked(false);
		tb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				allModes.setResult(true);
				createAllToggleButtons(modes, selected, onClickListener, a, ll);
			}
		});
		ll.addView(tb, lp);
		return newButtons;
	}

	private static void createAllToggleButtons(List<ApplicationMode> allModes,
											   Set<ApplicationMode> selected,
											   View.OnClickListener onClickListener,
											   Activity a, LinearLayout ll) {
		if (ll.getChildCount() > 0) {
			ll.removeAllViews();
		}
		ToggleButton[] buttons;
		ll.setOrientation(LinearLayout.VERTICAL);
		buttons = new ToggleButton[allModes.size()];
		LinearLayout container = new LinearLayout(a);
		container.setOrientation(LinearLayout.HORIZONTAL);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		container.setLayoutParams(params);
		ll.addView(container);
		for (int i =0; i<allModes.size(); i++){
			if (i != 0 && i % 4 == 0) {
				container = new LinearLayout(a);
				container.setOrientation(LinearLayout.HORIZONTAL);
				container.setLayoutParams(params);
				ll.addView(container);
			}
			buttons[i] = createToggle((OsmandApplication)a.getApplication(), container, allModes.get(i), true);
		}
		for (int i = 0; i < buttons.length; i++) {
			setButtonListener(allModes, selected, onClickListener, buttons, i, true);
		}
	}

	private static void getVisibleModes(List<ApplicationMode> values, Set<ApplicationMode> selected, List<ApplicationMode> visible) {
		if (visible.size() <= 4 && (selected.isEmpty() || visible.contains(selected.iterator().next()))) {
			return;
		}
		visible.clear();
		visible.addAll(selected);
		if(!selected.contains(ApplicationMode.DEFAULT)) {
			visible.add(0, ApplicationMode.DEFAULT);
		}
		for(ApplicationMode mode : values) {
			if(visible.size() >= 3) {
				break;
			}
			if (!visible.contains(mode)) {
				visible.add(mode);
			}
		}
	}

	public static View prepareAppModeView(Activity a, final List<ApplicationMode> values , final Set<ApplicationMode> selected, 
			ViewGroup parent, final boolean singleSelection,boolean drawer, final View.OnClickListener onClickListener) {
		LinearLayout ll = (LinearLayout) a.getLayoutInflater().inflate(R.layout.mode_toggles, parent);
		final ToggleButton[] buttons = createToggles(values, ll, (OsmandApplication)a.getApplication(), drawer);
		for (int i = 0; i < buttons.length; i++) {
			setButtonListener(values, selected, onClickListener, buttons, i, singleSelection);
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

	static ToggleButton[] createToggles(final List<ApplicationMode> values, LinearLayout topLayout, OsmandApplication ctx, boolean drawer) {
		final ToggleButton[] buttons = new ToggleButton[values.size()];
		HorizontalScrollView scroll = new HorizontalScrollView(ctx);
		
		topLayout.addView(scroll);
		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		scroll.addView(ll);
		
		int k = 0;
		for(ApplicationMode ma : values) {
			buttons[k++] = createToggle(ctx, ll, ma, drawer);
		}
		return buttons;
	}

	static private ToggleButton createToggle(OsmandApplication ctx, LinearLayout layout, ApplicationMode mode, boolean drawer){
		int margin = 0;
		if (drawer) {
			margin = 2;
		} else {
			margin = 10;
		}
		int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, margin, ctx.getResources().getDisplayMetrics());
		int metrics = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, ctx.getResources().getDisplayMetrics());
		int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 11, ctx.getResources().getDisplayMetrics());
		ToggleButton tb = new ToggleButton(ctx);
		tb.setTextOn("");
		tb.setTextOff("");
		tb.setContentDescription(mode.toHumanString(ctx));
		tb.setCompoundDrawablesWithIntrinsicBounds(null, ctx.getIconsCache().getIcon(mode.getIconId(), R.color.dashboard_blue), null, null);
		tb.setPadding(0, padding, 0, 0);
		LayoutParams lp = new LinearLayout.LayoutParams(metrics, metrics);
		lp.setMargins(left, 0, 0, 0);
		layout.addView(tb, lp);
		return tb;
	}
}
