package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
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

public class SaveAsNewTrackBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "SaveAsNewTrackBottomSheetDialogFragment";

	private SaveAsNewTrackFragmentListener listener;
	private boolean nightMode;
	private boolean portrait;

	public void setListener(SaveAsNewTrackFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_save_as_new_track_bottom_sheet_dialog, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			mainView.findViewById(R.id.images_row).setVisibility(View.GONE);
		} else {
			final ImageView routePointImage = (ImageView) mainView.findViewById(R.id.route_point_image);
			ImageView lineImage = (ImageView) mainView.findViewById(R.id.line_image);
			if (nightMode) {
				routePointImage.setImageResource(R.drawable.img_help_trip_route_points_night);
				lineImage.setImageResource(R.drawable.img_help_trip_track_night);
			} else {
				routePointImage.setImageResource(R.drawable.img_help_trip_route_points_day);
				lineImage.setImageResource(R.drawable.img_help_trip_track_day);
			}

			mainView.findViewById(R.id.line_text).setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					return false;
				}
			});
			lineImage.setOnClickListener(saveAsLineOnClickListener);

			mainView.findViewById(R.id.route_point_text).setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					return false;
				}
			});
			routePointImage.setOnClickListener(saveAsRoutePointOnClickListener);
		}

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.save_as_new_track_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		((ImageView) mainView.findViewById(R.id.route_point_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_route_points));
		((ImageView) mainView.findViewById(R.id.line_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_split_interval));

		mainView.findViewById(R.id.save_as_line_row).setOnClickListener(saveAsLineOnClickListener);
		mainView.findViewById(R.id.save_as_route_point_row).setOnClickListener(saveAsRoutePointOnClickListener);

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
				final View scrollView = mainView.findViewById(R.id.save_as_new_track_scroll_view);
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
						AndroidUtils.setBackground(getActivity(), mainView, nightMode,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(getActivity(), mainView, nightMode,
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
		return getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	private View.OnClickListener saveAsLineOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			if (listener != null) {
				listener.saveAsLineOnClick();
			}
			dismiss();
		}
	};

	private View.OnClickListener saveAsRoutePointOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			if (listener != null) {
				listener.saveAsRoutePointOnClick();
			}
			dismiss();
		}
	};

	interface SaveAsNewTrackFragmentListener {

		void saveAsRoutePointOnClick();

		void saveAsLineOnClick();
	}
}
