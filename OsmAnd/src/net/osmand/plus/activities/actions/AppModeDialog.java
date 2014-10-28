package net.osmand.plus.activities.actions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

	//special method for drawer menu
	// needed because if there's more than 4 items  - the don't fit in drawer
	public static View prepareAppModeDrawerView(Activity a, List<ApplicationMode> visible, final Set<ApplicationMode> selected,
												final View.OnClickListener onClickListener, DialogInterface.OnClickListener onDialogOk) {
		OsmandSettings settings = ((OsmandApplication) a.getApplication()).getSettings();
		final List<ApplicationMode> values = new ArrayList<ApplicationMode>(ApplicationMode.values(settings));
		selected.add(settings.getApplicationMode());
		if (values.size() > 4) {
			return prepareAppModeDrawerView(a, visible, values, selected, onClickListener, onDialogOk);
		} else {
			return prepareAppModeView(a, values, selected, null, true, onClickListener);
		}
	}

	private static View prepareAppModeDrawerView(Activity a,final List<ApplicationMode> visible, final List<ApplicationMode> values,
												 final Set<ApplicationMode> selected,
												final View.OnClickListener onClickListener,
												DialogInterface.OnClickListener onDialogOk){
		getVisibleModes(values, selected, visible);
		LinearLayout ll = (LinearLayout) a.getLayoutInflater().inflate(R.layout.mode_toggles, null);
		final ToggleButton[] buttons = createDrawerToggles(visible, values, a, ll, onDialogOk);
		final boolean[] selectionChangeLoop = new boolean[] {false};
		for (int i = 0; i < buttons.length - 1; i++) {
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
							handleSelection(visible, selected, true, buttons, ind, buttonAppMode, isChecked);
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

	private static ToggleButton[] createDrawerToggles(final List<ApplicationMode> visible, final List<ApplicationMode> modes,
													  final Activity a, LinearLayout ll, final DialogInterface.OnClickListener onDialogOk) {
		ToggleButton[] buttons = createToggles(visible, ll, a);
		ToggleButton[] newButtons = new ToggleButton[buttons.length + 1];
		for (int i = 0; i< buttons.length; i++){
			newButtons[i] = buttons[i];
		}
		ToggleButton tb = new ToggleButton(a);
		newButtons[buttons.length] = tb;
		tb.setTextOn("");
		tb.setTextOff("");
		int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, a.getResources().getDisplayMetrics());
		int metrics = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, a.getResources().getDisplayMetrics());
		tb.setButtonDrawable(R.drawable.ic_other_modes);
		LayoutParams lp = new LayoutParams(metrics, metrics);
		lp.setMargins(left, 0, 0, 0);
		tb.setChecked(false);
		tb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				CompoundButton compoundButton = (CompoundButton) view;
				compoundButton.setChecked(false);
				final AlertDialog.Builder builder = new AlertDialog.Builder(a);

				final Set<ApplicationMode> selected = new LinkedHashSet<ApplicationMode>(visible);
				builder.setTitle(R.string.profile_settings);
				builder.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						visible.clear();
						visible.addAll(selected);
						if(onDialogOk != null){
							onDialogOk.onClick(dialogInterface, i);
						}
					}
				});
				final AlertDialog dialog = builder.create();
				View v = AppModeDialog.prepareAppModeView(a, modes, selected, null, false,
						new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (selected.size() == 3){
									dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
								} else {
									dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
								}
							}
						});
				dialog.setView(v);
				dialog.show();
			}
		});
		ll.addView(tb, lp);
		return newButtons;
	}

	private static void getVisibleModes(List<ApplicationMode> values, Set<ApplicationMode> selected, List<ApplicationMode> visible) {
		if (visible.size() == 3) {
			for (ApplicationMode mode : selected){
				if (visible.contains(mode)){
					return;
				}
			}
		}
		visible.clear();
		if (selected.size() > 0) {
			List<Integer> positions = new ArrayList<Integer>();
			for (ApplicationMode mode : selected){
				for (int i =0; i< values.size(); i++){
					if (mode.equals(values.get(i))){
						positions.add(i);
					}
				}
			}

			if (positions.size() == 1) {
				int pos = positions.get(0);
				if (pos < values.size() - 2 && pos > 0){
					visible.add(values.get(pos - 1));
					visible.add(values.get(pos));
					visible.add(values.get(pos + 1));
				} else if (pos == 0) {
					visible.add(values.get(pos));
					visible.add(values.get(1));
					visible.add(values.get(2));
				} else if (pos == values.size() -1) {
					visible.add(values.get(pos - 1));
					visible.add(values.get(pos - 2));
					visible.add(values.get(pos));
				}
			} else if (positions.size() == 2) {
				int pos1 = positions.get(0);
				int pos2 = positions.get(1);
				if (pos1 + 1 != pos2){
					visible.add(values.get(pos1));
					visible.add(values.get(pos1 + 1));
					visible.add(values.get(pos2));
				} else if (pos1 > 0 && pos1 + 1 == pos2) {
					visible.add(values.get(pos1 - 1));
					visible.add(values.get(pos1));
					visible.add(values.get(pos2));
				} else if (pos1 == 0 && pos1 + 1 == pos2) {
					visible.add(values.get(pos1));
					visible.add(values.get(pos2));
					visible.add(values.get(pos2 + 1));
				}
			}
		} else {
			for (int i =0; i<3; i++){
				visible.add(values.get(i));
			}
		}
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
