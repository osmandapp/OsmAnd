package net.osmand.plus.activities.actions;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppModeDialog {

	public static View prepareAppModeView(Activity a, final Set<ApplicationMode> selected, boolean showDefault,
			ViewGroup parent, final boolean singleSelection, boolean useListBg, boolean useMapTheme, final View.OnClickListener onClickListener) {
		OsmandSettings settings = ((OsmandApplication) a.getApplication()).getSettings();
		final List<ApplicationMode> values = new ArrayList<ApplicationMode>(ApplicationMode.values(settings));
		if(!showDefault) {
			values.remove(ApplicationMode.DEFAULT);
		}
		if (showDefault || (settings.getApplicationMode() != ApplicationMode.DEFAULT && !singleSelection)) {
			selected.add(settings.getApplicationMode());
		}
		return prepareAppModeView(a, values, selected, parent, singleSelection, useListBg, useMapTheme, onClickListener);
	}

	//special method for drawer menu
	//needed because if there's more than 4 items  - the don't fit in drawer
	public static View prepareAppModeDrawerView(Activity a, final Set<ApplicationMode> selected,
												boolean useMapTheme, final View.OnClickListener onClickListener) {
		OsmandSettings settings = ((OsmandApplication) a.getApplication()).getSettings();
		final List<ApplicationMode> values = new ArrayList<ApplicationMode>(ApplicationMode.values(settings));
		selected.add(settings.getApplicationMode());
		return prepareAppModeView(a, values, selected, null, true, true, useMapTheme, onClickListener);
	}
	
	public static View prepareAppModeView(Activity a, final List<ApplicationMode> values , final Set<ApplicationMode> selected, 
				ViewGroup parent, final boolean singleSelection, boolean useListBg, boolean useMapTheme, final View.OnClickListener onClickListener) {
		View ll = a.getLayoutInflater().inflate(R.layout.mode_toggles, parent);
		boolean nightMode = isNightMode(((OsmandApplication) a.getApplication()), useMapTheme);
		if (useListBg) {
			AndroidUtils.setListItemBackground(a, ll, nightMode);
		} else {
			ll.setBackgroundColor(ContextCompat.getColor(a, nightMode ? R.color.route_info_bg_dark : R.color.route_info_bg_light));
		}
		final View[] buttons = new View[values.size()];
		int k = 0;
		for(ApplicationMode ma : values) {
			buttons[k++] = createToggle(a.getLayoutInflater(), (OsmandApplication) a.getApplication(), (LinearLayout) ll.findViewById(R.id.app_modes_content), ma, useMapTheme);
		}
		for (int i = 0; i < buttons.length; i++) {
			updateButtonState((OsmandApplication) a.getApplication(), values, selected, onClickListener, buttons, i,
					singleSelection, useMapTheme);
		}
		return ll;
	}


	private static void updateButtonState(final OsmandApplication ctx, final List<ApplicationMode> visible,
			final Set<ApplicationMode> selected, final View.OnClickListener onClickListener, final View[] buttons,
			int i, final boolean singleChoice, final boolean useMapTheme) {
		if (buttons[i] != null) {
			View tb = buttons[i];
			final ApplicationMode mode = visible.get(i);
			final boolean checked = selected.contains(mode);
			ImageView iv = (ImageView) tb.findViewById(R.id.app_mode_icon);
			boolean nightMode = isNightMode(ctx, useMapTheme);
			if (checked) {
				iv.setImageDrawable(ctx.getIconsCache().getIcon(mode.getSmallIconDark(), nightMode ? R.color.route_info_checked_mode_icon_color_dark : R.color.route_info_checked_mode_icon_color_light));
				iv.setContentDescription(String.format("%s %s", mode.toHumanString(ctx), ctx.getString(R.string.item_checked)));
				tb.findViewById(R.id.selection).setVisibility(View.VISIBLE);
			} else {
				if (useMapTheme) {
					iv.setImageDrawable(ctx.getIconsCache().getIcon(mode.getSmallIconDark(), R.color.route_info_unchecked_mode_icon_color));
					iv.setBackgroundResource(AndroidUtils.resolveAttribute(ctx, android.R.attr.selectableItemBackground));
				} else {
					iv.setImageDrawable(ctx.getIconsCache().getThemedIcon(mode.getSmallIconDark()));
				}
				iv.setContentDescription(String.format("%s %s", mode.toHumanString(ctx), ctx.getString(R.string.item_unchecked)));
				tb.findViewById(R.id.selection).setVisibility(View.INVISIBLE);
			}
			iv.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean isChecked = !checked;
					if (singleChoice) {
						if (isChecked) {
							selected.clear();
							selected.add(mode);
						}
					} else {
						if (isChecked) {
							selected.add(mode);
						} else {
							selected.remove(mode);
						}
					}
					if (onClickListener != null) {
						onClickListener.onClick(null);
					}
					for(int i = 0; i < visible.size(); i++) {
						updateButtonState(ctx, visible, selected, onClickListener, buttons, i, singleChoice, useMapTheme);
					}
				}
			});
		}
	}


	static private View createToggle(LayoutInflater layoutInflater, OsmandApplication ctx, LinearLayout layout, ApplicationMode mode, boolean useMapTheme){
		int metricsX = (int) ctx.getResources().getDimension(R.dimen.route_info_modes_height);
		int metricsY = (int) ctx.getResources().getDimension(R.dimen.route_info_modes_height);
		View tb = layoutInflater.inflate(R.layout.mode_view, null);
		ImageView iv = (ImageView) tb.findViewById(R.id.app_mode_icon);
		iv.setImageDrawable(ctx.getIconsCache().getIcon(mode.getSmallIconDark(), isNightMode(ctx, useMapTheme) ? R.color.route_info_checked_mode_icon_color_dark : R.color.route_info_checked_mode_icon_color_light));
		iv.setContentDescription(mode.toHumanString(ctx));
//		tb.setCompoundDrawablesWithIntrinsicBounds(null, ctx.getIconsCache().getIcon(mode.getIconId(), R.color.app_mode_icon_color), null, null);
		LayoutParams lp = new LinearLayout.LayoutParams(metricsX, metricsY);
//		lp.setMargins(left, 0, 0, 0);
		layout.addView(tb, lp);
		return tb;
	}

	private static boolean isNightMode(OsmandApplication ctx, boolean useMapTheme) {
		boolean nightMode;
		if (useMapTheme) {
			nightMode = ctx.getDaynightHelper().isNightModeForMapControls();
		} else {
			nightMode = !ctx.getSettings().isLightContent();
		}
		return nightMode;
	}
}
