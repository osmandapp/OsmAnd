package net.osmand.plus.mapmarkers;

import android.os.Build;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
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
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		night = !getMyApplication().getSettings().isLightContent();
		final int themeRes = night ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_show_direction_bottom_sheet_dialog, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, night, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		OsmandSettings.MapMarkersMode mode = getMyApplication().getSettings().MAP_MARKERS_MODE.get();
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

		ImageView topBarIcon = (ImageView) mainView.findViewById(R.id.top_bar_icon);
		topBarIcon.setBackgroundDrawable(getIcon(R.drawable.ic_action_device_top, R.color.on_map_icon_color));
		topBarIcon.setImageDrawable(getIcon(R.drawable.ic_action_device_topbar, R.color.dashboard_blue));

		ImageView widgetIcon = (ImageView) mainView.findViewById(R.id.widget_icon);
		widgetIcon.setBackgroundDrawable(getIcon(R.drawable.ic_action_device_top, R.color.on_map_icon_color));
		widgetIcon.setImageDrawable(getIcon(R.drawable.ic_action_device_widget, R.color.dashboard_blue));

		ImageView noneIcon = (ImageView) mainView.findViewById(R.id.none_icon);
		noneIcon.setBackgroundDrawable(getIcon(R.drawable.ic_action_device_top, R.color.on_map_icon_color));

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

	private void highlightSelectedItem(OsmandSettings.MapMarkersMode mode, boolean check) {
		int iconBgColor = check ? R.color.dashboard_blue : R.color.on_map_icon_color;
		int iconColor = check ? R.color.color_dialog_buttons_dark : R.color.dashboard_blue;
		int textColor = ContextCompat.getColor(getContext(), check ? R.color.dashboard_blue : night ? R.color.color_white : R.color.color_black);
		switch (mode) {
			case TOOLBAR:
				((RadioButton) mainView.findViewById(R.id.top_bar_radio_button)).setChecked(check);
				ImageView topBarIcon = (ImageView) mainView.findViewById(R.id.top_bar_icon);
				if (check) {
					mainView.findViewById(R.id.top_bar_row).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.show_direction_menu_selected_item_bg));
				} else {
					mainView.findViewById(R.id.top_bar_row).setBackgroundResource(0);
				}
				((TextView) mainView.findViewById(R.id.top_bar_text)).setTextColor(textColor);
				topBarIcon.setBackgroundDrawable(getIcon(R.drawable.ic_action_device_top, iconBgColor));
				topBarIcon.setImageDrawable(getIcon(R.drawable.ic_action_device_topbar, iconColor));
				break;
			case WIDGETS:
				((RadioButton) mainView.findViewById(R.id.widget_radio_button)).setChecked(check);
				ImageView widgetIcon = (ImageView) mainView.findViewById(R.id.widget_icon);
				if (check) {
					mainView.findViewById(R.id.widget_row).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.show_direction_menu_selected_item_bg));
				} else {
					mainView.findViewById(R.id.widget_row).setBackgroundResource(0);
				}
				((TextView) mainView.findViewById(R.id.widget_text)).setTextColor(textColor);
				widgetIcon.setBackgroundDrawable(getIcon(R.drawable.ic_action_device_top, iconBgColor));
				widgetIcon.setImageDrawable(getIcon(R.drawable.ic_action_device_widget, iconColor));
				break;
			case NONE:
				((RadioButton) mainView.findViewById(R.id.none_radio_button)).setChecked(check);
				ImageView noneIcon = (ImageView) mainView.findViewById(R.id.none_icon);
				if (check) {
					mainView.findViewById(R.id.none_row).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.show_direction_menu_selected_item_bg));
				} else {
					mainView.findViewById(R.id.none_row).setBackgroundResource(0);
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
			switch (view.getId()) {
				case R.id.top_bar_image:
				case R.id.top_bar_row:
					getMyApplication().getSettings().MAP_MARKERS_MODE.set(OsmandSettings.MapMarkersMode.TOOLBAR);
					highlightSelectedItem(OsmandSettings.MapMarkersMode.TOOLBAR, true);
					break;
				case R.id.widget_image:
				case R.id.widget_row:
					getMyApplication().getSettings().MAP_MARKERS_MODE.set(OsmandSettings.MapMarkersMode.WIDGETS);
					highlightSelectedItem(OsmandSettings.MapMarkersMode.WIDGETS, true);
					break;
				case R.id.none_row:
					getMyApplication().getSettings().MAP_MARKERS_MODE.set(OsmandSettings.MapMarkersMode.NONE);
					highlightSelectedItem(OsmandSettings.MapMarkersMode.NONE, true);
					break;
			}
			if (listener != null) {
				listener.onMapMarkersModeChanged();
			}
			dismiss();
		}
	};

	interface ShowDirectionFragmentListener {
		void onMapMarkersModeChanged();
	}
}
