package net.osmand.plus.mapmarkers;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class OptionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "OptionsBottomSheetDialogFragment";
	public final static String SHOW_SORT_BY_ROW = "show_sort_by_row";
	public final static String SHOW_MOVE_ALL_TO_HISTORY_ROW = "show_move_to_history_row";

	private MarkerOptionsFragmentListener listener;
	private boolean showSortBy;
	private boolean showMoveAllToHistory;

	public void setListener(MarkerOptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		showSortBy = args == null || args.getBoolean(SHOW_SORT_BY_ROW, true);
		showMoveAllToHistory = args == null || args.getBoolean(SHOW_MOVE_ALL_TO_HISTORY_ROW, true);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = getMyApplication().getSettings().isLightContent() ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_options_bottom_sheet_dialog, null);

		((ImageView) mainView.findViewById(R.id.sort_by_icon)).setImageDrawable(getContentIcon(R.drawable.ic_sort_waypoint_dark));
		OsmandSettings.MapMarkersMode mode = getMyApplication().getSettings().MAP_MARKERS_MODE.get();
		int displayedCount = getMyApplication().getSettings().DISPLAYED_MARKERS_WIDGETS_COUNT.get();
		ImageView showDirectionIcon = (ImageView) mainView.findViewById(R.id.show_direction_icon);
		int imageResId = 0;
		switch (mode) {
			case TOOLBAR:
				imageResId = displayedCount == 1 ? R.drawable.ic_action_device_topbar : R.drawable.ic_action_device_topbar_two;
				break;
			case WIDGETS:
				imageResId = displayedCount == 1 ? R.drawable.ic_action_device_widget : R.drawable.ic_action_device_widget_two;
				break;
		}
		showDirectionIcon.setBackgroundDrawable(getContentIcon(R.drawable.ic_action_device_top));
		if (imageResId != 0) {
			showDirectionIcon.setImageDrawable(getIcon(imageResId, R.color.dashboard_blue));
		}
		((ImageView) mainView.findViewById(R.id.coordinate_input_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_longitude));
		((ImageView) mainView.findViewById(R.id.build_route_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_gdirections_dark));
		((ImageView) mainView.findViewById(R.id.save_as_new_track_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));
		((ImageView) mainView.findViewById(R.id.move_all_to_history_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_history2));

		View sortByRow = mainView.findViewById(R.id.sort_by_row);
		if (!showSortBy) {
			sortByRow.setVisibility(View.GONE);
		} else {
			sortByRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.sortByOnClick();
					}
					dismiss();
				}
			});
		}
		mainView.findViewById(R.id.show_direction_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.showDirectionOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.coordinate_input_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.coordinateInputOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.build_route_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.buildRouteOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.save_as_new_track_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.saveAsNewTrackOnClick();
				}
				dismiss();
			}
		});
		View moveAllToHistoryRow = mainView.findViewById(R.id.move_all_to_history_row);
		if (!showMoveAllToHistory) {
			mainView.findViewById(R.id.move_all_to_history_divider).setVisibility(View.GONE);
			moveAllToHistoryRow.setVisibility(View.GONE);
		} else {
			moveAllToHistoryRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.moveAllToHistoryOnClick();
					}
					dismiss();
				}
			});
		}

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				Activity activity = getActivity();
				boolean nightMode = !getMyApplication().getSettings().isLightContent();
				int allowedHeight = getAllowedHeight();

				if (AndroidUiHelper.isOrientationPortrait(activity)) {
					if (allowedHeight - mainView.getHeight() >= getResources().getDimension(R.dimen.bottom_sheet_content_padding_small)) {
						AndroidUtils.setBackground(activity, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
					}
				} else {
					if (allowedHeight - mainView.getHeight() >= getResources().getDimension(R.dimen.bottom_sheet_content_padding_small)) {
						AndroidUtils.setBackground(activity, mainView, nightMode,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(activity, mainView, nightMode,
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
	public void dismiss() {
		if (listener != null) {
			listener.dismiss();
		}
		super.dismiss();
	}

	private int getAllowedHeight() {
		Activity activity = getActivity();
		int scrH = AndroidUtils.getScreenHeight(activity);
		int stBarH = AndroidUtils.getStatusBarHeight(activity);
		int nBarH = AndroidUtils.getNavBarHeight(activity);
		return scrH - stBarH - nBarH - getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar) - AndroidUtils.dpToPx(activity, 56);
	}

	interface MarkerOptionsFragmentListener {

		void sortByOnClick();

		void showDirectionOnClick();

		void coordinateInputOnClick();

		void buildRouteOnClick();

		void saveAsNewTrackOnClick();

		void moveAllToHistoryOnClick();

		void dismiss();
	}
}
