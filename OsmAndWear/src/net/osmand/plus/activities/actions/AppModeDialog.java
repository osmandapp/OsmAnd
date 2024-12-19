package net.osmand.plus.activities.actions;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppModeDialog {

	//special method for drawer menu
	//needed because if there's more than 4 items  - the don't fit in drawer
	public static View prepareAppModeDrawerView(Activity a, Set<ApplicationMode> selected,
												boolean useMapTheme, View.OnClickListener onClickListener) {
		OsmandApplication app = (OsmandApplication) a.getApplication();
		OsmandSettings settings = app.getSettings();
		List<ApplicationMode> values = new ArrayList<>(ApplicationMode.values(app));
		selected.add(settings.getApplicationMode());
		return prepareAppModeView(a, values, selected, null, true, true, useMapTheme, onClickListener);
	}

	public static View prepareAppModeView(Activity a, List<ApplicationMode> values, Set<ApplicationMode> selected,
										  ViewGroup parent, boolean singleSelection, boolean useListBg, boolean useMapTheme, View.OnClickListener onClickListener) {
		boolean nightMode = isNightMode(((OsmandApplication) a.getApplication()), useMapTheme);

		return prepareAppModeView(a, values, selected, parent, singleSelection, useListBg, useMapTheme, onClickListener, nightMode);
	}

	public static View prepareAppModeView(Activity a, List<ApplicationMode> values, Set<ApplicationMode> selected,
										  ViewGroup parent, boolean singleSelection, boolean useListBg, boolean useMapTheme, View.OnClickListener onClickListener, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) a.getApplication();
		View ll = a.getLayoutInflater().inflate(R.layout.mode_toggles, parent);
		if (useListBg) {
			AndroidUtils.setListItemBackground(a, ll, nightMode);
		} else {
			ll.setBackgroundColor(ContextCompat.getColor(a, nightMode ? R.color.card_and_list_background_dark : R.color.card_and_list_background_light));
		}
		View[] buttons = new View[values.size()];
		int k = 0;
		for (ApplicationMode ma : values) {
			buttons[k++] = createToggle(a.getLayoutInflater(), app, R.layout.mode_view, ll.findViewById(R.id.app_modes_content), ma, useMapTheme);
		}
		for (int i = 0; i < buttons.length; i++) {
			updateButtonState(app, values, selected, onClickListener, buttons, i, singleSelection, useMapTheme, nightMode);
		}

		ApplicationMode activeMode = app.getSettings().getApplicationMode();
		int idx = values.indexOf(activeMode);

		OnGlobalLayoutListener globalListener = new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				HorizontalScrollView scrollView = ll.findViewById(R.id.app_modes_scroll_container);
				LinearLayout container = ll.findViewById(R.id.app_modes_content);
				int s = container.getChildAt(idx) != null ? container.getChildAt(idx).getRight() : 0;
				scrollView.scrollTo(Math.max(s - scrollView.getWidth(), 0), 0);
				ll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		};
		ll.getViewTreeObserver().addOnGlobalLayoutListener(globalListener);

		return ll;
	}


	public static void updateButtonState(OsmandApplication app, List<ApplicationMode> visible,
	                                     Set<ApplicationMode> selected, View.OnClickListener onClickListener, View[] buttons,
	                                     int i, boolean singleChoice, boolean useMapTheme, boolean nightMode) {
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);
		if (buttons[i] != null) {
			View tb = buttons[i];
			ApplicationMode mode = visible.get(i);
			boolean checked = selected.contains(mode);
			View selection = tb.findViewById(R.id.selection);
			ImageView iv = tb.findViewById(R.id.app_mode_icon);
			if (checked) {
				iv.setImageDrawable(app.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode)));
				iv.setContentDescription(String.format("%s %s", mode.toHumanString(), app.getString(R.string.item_checked)));
				selection.setBackgroundColor(mode.getProfileColor(nightMode));
				selection.setVisibility(View.VISIBLE);
			} else {
				if (useMapTheme) {
					iv.setImageDrawable(app.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode)));
					tb.setBackgroundResource(AndroidUtils.resolveAttribute(themedCtx, android.R.attr.selectableItemBackground));
				} else {
					iv.setImageDrawable(app.getUIUtilities().getThemedIcon(mode.getIconRes()));
				}
				iv.setContentDescription(String.format("%s %s", mode.toHumanString(), app.getString(R.string.item_unchecked)));
				selection.setVisibility(View.INVISIBLE);
			}
			tb.setOnClickListener(new View.OnClickListener() {

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
					for (int i = 0; i < visible.size(); i++) {
						updateButtonState(app, visible, selected, onClickListener, buttons, i, singleChoice, useMapTheme, nightMode);
					}
				}
			});
		}
	}

	public static void updateButtonStateForRoute(OsmandApplication ctx, List<ApplicationMode> visible,
	                                             Set<ApplicationMode> selected, View.OnClickListener onClickListener, View[] buttons,
	                                             int i, boolean singleChoice, boolean useMapTheme, boolean nightMode) {
		if (buttons[i] != null) {
			View tb = buttons[i];
			ApplicationMode mode = visible.get(i);
			boolean checked = selected.contains(mode);
			ImageView iv = tb.findViewById(R.id.app_mode_icon);
			ImageView selection = tb.findViewById(R.id.selection);
			Drawable drawable = ctx.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode));
			if (checked) {
				iv.setImageDrawable(drawable);
				iv.setContentDescription(String.format("%s %s", mode.toHumanString(), ctx.getString(R.string.item_checked)));
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
					selection.setImageDrawable(ctx.getDrawable(R.drawable.btn_checked_border_light));
					AndroidUtils.setBackground(ctx, selection, nightMode, R.drawable.ripple_light, R.drawable.ripple_light);
				} else {
					AndroidUtils.setBackground(ctx, selection, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_light);
				}
			} else {
				if (useMapTheme) {
					if (Build.VERSION.SDK_INT >= 21) {
						Drawable active = ctx.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode));
						drawable = AndroidUtils.createPressedStateListDrawable(drawable, active);
					}
					iv.setImageDrawable(drawable);
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
						selection.setImageDrawable(ctx.getDrawable(R.drawable.btn_border_pressed_light));
						AndroidUtils.setBackground(ctx, selection, nightMode, R.drawable.ripple_light, R.drawable.ripple_light);
					} else {
						AndroidUtils.setBackground(ctx, selection, nightMode, R.drawable.btn_border_pressed_trans_light, R.drawable.btn_border_pressed_trans_light);
					}
				} else {
					iv.setImageDrawable(ctx.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode)));
				}
				iv.setContentDescription(String.format("%s %s", mode.toHumanString(), ctx.getString(R.string.item_unchecked)));
			}
			tb.setOnClickListener(new View.OnClickListener() {

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
					for (int i = 0; i < visible.size(); i++) {
						updateButtonStateForRoute(ctx, visible, selected, onClickListener, buttons, i, singleChoice, useMapTheme, nightMode);
					}
				}
			});
		}
	}

	public static View createToggle(LayoutInflater layoutInflater, OsmandApplication ctx, @LayoutRes int layoutId, LinearLayout layout, ApplicationMode mode, boolean useMapTheme) {
		int metricsX = (int) ctx.getResources().getDimension(R.dimen.route_info_modes_height);
		int metricsY = (int) ctx.getResources().getDimension(R.dimen.route_info_modes_height);
		View tb = layoutInflater.inflate(layoutId, null);
		ImageView iv = tb.findViewById(R.id.app_mode_icon);
		iv.setImageDrawable(ctx.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(isNightMode(ctx, useMapTheme))));
		iv.setContentDescription(mode.toHumanString());
		LayoutParams lp = new LinearLayout.LayoutParams(metricsX, metricsY);
		layout.addView(tb, lp);
		return tb;
	}

	private static boolean isNightMode(OsmandApplication app, boolean usedOnMap) {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}
}
