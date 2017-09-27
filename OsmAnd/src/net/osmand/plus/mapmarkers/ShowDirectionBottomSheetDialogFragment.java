package net.osmand.plus.mapmarkers;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class ShowDirectionBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "ShowDirectionBottomSheetDialogFragment";

	private ShowDirectionFragmentListener listener;
	private boolean portrait;
	private View mainView;
	private boolean night;

	public void setListener(ShowDirectionFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final OsmandSettings settings = getMyApplication().getSettings();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		night = !settings.isLightContent();
		final int themeRes = night ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_show_direction_bottom_sheet_dialog, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, night, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		OsmandSettings.MapMarkersMode mode = settings.MAP_MARKERS_MODE.get();
		highlightSelectedItem(mode, true);

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			mainView.findViewById(R.id.images_row).setVisibility(View.GONE);
		} else {
			ImageView topBarImage = (ImageView) mainView.findViewById(R.id.top_bar_image);
			ImageView widgetImage = (ImageView) mainView.findViewById(R.id.widget_image);
			if (night) {
				topBarImage.setImageResource(R.drawable.img_help_markers_topbar_night);
				widgetImage.setImageResource(R.drawable.img_help_markers_widgets_night);
			} else {
				topBarImage.setImageResource(R.drawable.img_help_markers_topbar_day);
				widgetImage.setImageResource(R.drawable.img_help_markers_widgets_day);
			}

			mainView.findViewById(R.id.top_bar_image_text).setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					return false;
				}
			});
			topBarImage.setOnClickListener(showDirectionOnClickListener);

			mainView.findViewById(R.id.widget_image_text).setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					return false;
				}
			});
			widgetImage.setOnClickListener(showDirectionOnClickListener);
		}

		if (night) {
			((TextView) mainView.findViewById(R.id.show_direction_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		final CompoundButton showArrowsToggle = (CompoundButton) mainView.findViewById(R.id.show_arrows_switch);
		showArrowsToggle.setChecked(settings.SHOW_ARROWS_TO_FIRST_MARKERS.get());
		mainView.findViewById(R.id.show_arrows_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean newState = !settings.SHOW_ARROWS_TO_FIRST_MARKERS.get();
				settings.SHOW_ARROWS_TO_FIRST_MARKERS.set(newState);
				showArrowsToggle.setChecked(newState);
				if (getMapActivity() != null) {
					getMapActivity().refreshMap();
				}
			}
		});

		final CompoundButton showLinesToggle = (CompoundButton) mainView.findViewById(R.id.show_guide_line_switch);
		showLinesToggle.setChecked(settings.SHOW_LINES_TO_FIRST_MARKERS.get());
		mainView.findViewById(R.id.show_guide_line_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean newState = !settings.SHOW_LINES_TO_FIRST_MARKERS.get();
				settings.SHOW_LINES_TO_FIRST_MARKERS.set(newState);
				showLinesToggle.setChecked(newState);
				if (getMapActivity() != null) {
					getMapActivity().refreshMap();
				}
			}
		});

		ImageView topBarIcon = (ImageView) mainView.findViewById(R.id.top_bar_icon);
		topBarIcon.setBackgroundDrawable(getContentIcon(R.drawable.ic_action_device_top));
		topBarIcon.setImageDrawable(getIcon(R.drawable.ic_action_device_topbar, R.color.dashboard_blue));

		ImageView widgetIcon = (ImageView) mainView.findViewById(R.id.widget_icon);
		widgetIcon.setBackgroundDrawable(getContentIcon(R.drawable.ic_action_device_top));
		widgetIcon.setImageDrawable(getIcon(R.drawable.ic_action_device_widget, R.color.dashboard_blue));

		ImageView noneIcon = (ImageView) mainView.findViewById(R.id.none_icon);
		noneIcon.setBackgroundDrawable(getContentIcon(R.drawable.ic_action_device_top));

		mainView.findViewById(R.id.top_bar_row).setOnClickListener(showDirectionOnClickListener);
		mainView.findViewById(R.id.widget_row).setOnClickListener(showDirectionOnClickListener);
		mainView.findViewById(R.id.none_row).setOnClickListener(showDirectionOnClickListener);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		final int screenHeight = AndroidUtils.getScreenHeight(getActivity());
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(getActivity());
		final int navBarHeight = AndroidUtils.getNavBarHeight(getActivity());

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final View scrollView = mainView.findViewById(R.id.marker_show_direction_scroll_view);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				if (!portrait) {
					if (screenHeight - statusBarHeight - mainView.getHeight()
							>= AndroidUtils.dpToPx(getActivity(), 8)) {
						AndroidUtils.setBackground(getActivity(), mainView, false,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(getActivity(), mainView, false,
								R.drawable.bg_bottom_sheet_sides_landscape_light, R.drawable.bg_bottom_sheet_sides_landscape_dark);
					}
				}

				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
			}
		});

		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!portrait) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, night ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null) {
			return (MapActivity) activity;
		}
		return null;
	}

	private void highlightSelectedItem(OsmandSettings.MapMarkersMode mode, boolean check) {
		int iconBgColor = check ? R.color.dashboard_blue : R.color.on_map_icon_color;
		int iconColor = check ? R.color.color_dialog_buttons_dark : R.color.dashboard_blue;
		int textColor = ContextCompat.getColor(getContext(), check ? (night ? R.color.color_dialog_buttons_dark : R.color.dashboard_blue) : night ? R.color.color_white : R.color.color_black);
		switch (mode) {
			case TOOLBAR:
				((RadioButton) mainView.findViewById(R.id.top_bar_radio_button)).setChecked(check);
				ImageView topBarIcon = (ImageView) mainView.findViewById(R.id.top_bar_icon);
				if (check) {
					mainView.findViewById(R.id.top_bar_row_frame).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.show_direction_menu_selected_item_bg));
				} else {
					mainView.findViewById(R.id.top_bar_row_frame).setBackgroundResource(0);
				}
				((TextView) mainView.findViewById(R.id.top_bar_text)).setTextColor(textColor);
				topBarIcon.setBackgroundDrawable(getIcon(R.drawable.ic_action_device_top, iconBgColor));
				topBarIcon.setImageDrawable(getIcon(R.drawable.ic_action_device_topbar, iconColor));
				break;
			case WIDGETS:
				((RadioButton) mainView.findViewById(R.id.widget_radio_button)).setChecked(check);
				ImageView widgetIcon = (ImageView) mainView.findViewById(R.id.widget_icon);
				if (check) {
					mainView.findViewById(R.id.widget_row_frame).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.show_direction_menu_selected_item_bg));
				} else {
					mainView.findViewById(R.id.widget_row_frame).setBackgroundResource(0);
				}
				((TextView) mainView.findViewById(R.id.widget_text)).setTextColor(textColor);
				widgetIcon.setBackgroundDrawable(getIcon(R.drawable.ic_action_device_top, iconBgColor));
				widgetIcon.setImageDrawable(getIcon(R.drawable.ic_action_device_widget, iconColor));
				break;
			case NONE:
				((RadioButton) mainView.findViewById(R.id.none_radio_button)).setChecked(check);
				ImageView noneIcon = (ImageView) mainView.findViewById(R.id.none_icon);
				if (check) {
					mainView.findViewById(R.id.none_row_frame).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.show_direction_menu_selected_item_bg));
				} else {
					mainView.findViewById(R.id.none_row_frame).setBackgroundResource(0);
				}
				((TextView) mainView.findViewById(R.id.none_text)).setTextColor(textColor);
				noneIcon.setBackgroundDrawable(getIcon(R.drawable.ic_action_device_top, iconBgColor));
				break;
		}
	}

	private View.OnClickListener showDirectionOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View view) {
			OsmandSettings.MapMarkersMode previousMode = getMyApplication().getSettings().MAP_MARKERS_MODE.get();
			highlightSelectedItem(previousMode, false);
			boolean showDirectionEnabled = false;
			switch (view.getId()) {
				case R.id.top_bar_image:
				case R.id.top_bar_row:
					getMyApplication().getSettings().MAP_MARKERS_MODE.set(OsmandSettings.MapMarkersMode.TOOLBAR);
					highlightSelectedItem(OsmandSettings.MapMarkersMode.TOOLBAR, true);
					showDirectionEnabled = true;
					break;
				case R.id.widget_image:
				case R.id.widget_row:
					getMyApplication().getSettings().MAP_MARKERS_MODE.set(OsmandSettings.MapMarkersMode.WIDGETS);
					highlightSelectedItem(OsmandSettings.MapMarkersMode.WIDGETS, true);
					showDirectionEnabled = true;
					break;
				case R.id.none_row:
					getMyApplication().getSettings().MAP_MARKERS_MODE.set(OsmandSettings.MapMarkersMode.NONE);
					highlightSelectedItem(OsmandSettings.MapMarkersMode.NONE, true);
					showDirectionEnabled = false;
					break;
			}
			if (listener != null) {
				listener.onMapMarkersModeChanged(showDirectionEnabled);
			}
			dismiss();
		}
	};

	interface ShowDirectionFragmentListener {
		void onMapMarkersModeChanged(boolean showDirectionEnabled);
	}
}
