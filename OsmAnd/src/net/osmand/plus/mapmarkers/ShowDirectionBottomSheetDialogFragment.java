package net.osmand.plus.mapmarkers;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class ShowDirectionBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "ShowDirectionBottomSheetDialogFragment";

	private ShowDirectionFragmentListener listener;
	private boolean portrait;

	public void setListener(ShowDirectionFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		boolean nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_show_direction_bottom_sheet_dialog, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			mainView.findViewById(R.id.images_row).setVisibility(View.GONE);
		} else {
			ImageView topBarImage = (ImageView) mainView.findViewById(R.id.top_bar_image);
			ImageView widgetImage = (ImageView) mainView.findViewById(R.id.widget_image);
			if (nightMode) {
				topBarImage.setImageResource(R.drawable.img_help_markers_topbar_night);
				widgetImage.setImageResource(R.drawable.img_help_markers_widgets_night);
			} else {
				topBarImage.setImageResource(R.drawable.img_help_markers_topbar_day);
				widgetImage.setImageResource(R.drawable.img_help_markers_widgets_day);
			}

			mainView.findViewById(R.id.top_bar_text).setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					return false;
				}
			});

			mainView.findViewById(R.id.widget_text).setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					return false;
				}
			});
		}

		if (nightMode) {
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

	interface ShowDirectionFragmentListener {

	}
}
