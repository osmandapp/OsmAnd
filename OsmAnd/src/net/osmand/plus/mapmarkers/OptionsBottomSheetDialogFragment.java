package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

public class OptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

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
		MapActivity mapActivity = (MapActivity) getActivity();
		final boolean nightMode = isNightMode();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_options_bottom_sheet_dialog, container);

		((ImageView) mainView.findViewById(R.id.sort_by_icon)).setImageDrawable(getContentIcon(R.drawable.ic_sort_waypoint_dark));
		OsmandSettings.MapMarkersMode mode = getMyApplication().getSettings().MAP_MARKERS_MODE.get();
		ImageView showDirectionIcon = (ImageView) mainView.findViewById(R.id.show_direction_icon);
		int imageResId = 0;
		switch (mode) {
			case TOOLBAR:
				imageResId = R.drawable.ic_action_device_topbar;
				break;
			case WIDGETS:
				imageResId = R.drawable.ic_action_device_widget;
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

		((TextView) mainView.findViewById(R.id.show_direction_text_view)).setTextColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.color_dialog_buttons_dark : R.color.map_widget_blue_pressed));
		((TextView) mainView.findViewById(R.id.show_direction_text_view)).setText(getMyApplication().getSettings().MAP_MARKERS_MODE.get().toHumanString(getActivity()));

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
		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.marker_options_scroll_view);

		return mainView;
	}

	interface MarkerOptionsFragmentListener {

		void sortByOnClick();

		void showDirectionOnClick();

		void coordinateInputOnClick();

		void buildRouteOnClick();

		void saveAsNewTrackOnClick();

		void moveAllToHistoryOnClick();
	}
}
