package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.TextViewEx;

public class OptionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "OptionsBottomSheetDialogFragment";

	private OptionsFragmentListener listener;
	private boolean addLineMode;
	private boolean portrait;
	private boolean nightMode;
	private boolean snapToRoadEnabled;

	public void setListener(OptionsFragmentListener listener) {
		this.listener = listener;
	}

	public void setAddLineMode(boolean addLineMode) {
		this.addLineMode = addLineMode;
	}

	public void setSnapToRoadEnabled(boolean snapToRoadEnabled) {
		this.snapToRoadEnabled = snapToRoadEnabled;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_options_bottom_sheet_dialog, null);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		if (nightMode) {
			((TextViewEx) mainView.findViewById(R.id.options_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}
		if (snapToRoadEnabled) {
			mainView.findViewById(R.id.snap_to_road_enabled_text_view).setVisibility(View.VISIBLE);
			((SwitchCompat) mainView.findViewById(R.id.snap_to_road_switch)).setChecked(true);
		}
		((ImageView) mainView.findViewById(R.id.snap_to_road_icon)).setImageDrawable(snapToRoadEnabled
				? getActiveIcon(R.drawable.ic_action_snap_to_road)
				: getContentIcon(R.drawable.ic_action_snap_to_road));
		((ImageView) mainView.findViewById(R.id.clear_all_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_reset_to_default_dark));
		if (!addLineMode) {
			((ImageView) mainView.findViewById(R.id.save_as_new_track_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));
			((ImageView) mainView.findViewById(R.id.add_to_the_track_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_split_interval));
		} else {
			mainView.findViewById(R.id.save_as_new_track_row).setVisibility(View.GONE);
			mainView.findViewById(R.id.add_to_the_track_row).setVisibility(View.GONE);
			mainView.findViewById(R.id.save_as_new_segment_row).setVisibility(View.VISIBLE);
			((ImageView) mainView.findViewById(R.id.save_as_new_segment_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));
		}

		mainView.findViewById(R.id.snap_to_road_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.snapToRoadOnCLick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.clear_all_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.clearAllOnClick();
				}
				dismiss();
			}
		});
		if (!addLineMode) {
			mainView.findViewById(R.id.save_as_new_track_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.saveAsNewTrackOnClick();
					}
					dismiss();
				}
			});
			mainView.findViewById(R.id.add_to_the_track_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.addToTheTrackOnClick();
					}
					dismiss();
				}
			});
		} else {
			mainView.findViewById(R.id.save_as_new_segment_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.addToGpxOnClick();
					}
					dismiss();
				}
			});
		}
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
				final View scrollView = mainView.findViewById(R.id.measure_options_scroll_view);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.measure_distance_bottom_sheet_cancel_button_height);
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

	private Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	interface OptionsFragmentListener {

		void snapToRoadOnCLick();

		void addToGpxOnClick();

		void saveAsNewTrackOnClick();

		void addToTheTrackOnClick();

		void clearAllOnClick();
	}
}
