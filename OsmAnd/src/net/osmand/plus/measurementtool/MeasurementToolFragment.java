package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class MeasurementToolFragment extends Fragment {

	public static final String TAG = "MeasurementToolFragment";

	private MapActivity mapActivity;
	private MeasurementToolLayer measurementLayer;

	private TextView distanceTv;
	private TextView pointsTv;
	private String pointsSt;

	private boolean wasCollapseButtonVisible;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mapActivity = (MapActivity) getActivity();
		measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		final boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		pointsSt = mapActivity.getString(R.string.points).toLowerCase();

		View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_measurement_tool, null);

		final View mainView = view.findViewById(R.id.main_view);
		AndroidUtils.setBackground(mapActivity, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);

		distanceTv = (TextView) mainView.findViewById(R.id.measurement_distance_text_view);
		pointsTv = (TextView) mainView.findViewById(R.id.measurement_points_text_view);

		((ImageView) mainView.findViewById(R.id.ruler_icon))
				.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_ruler, R.color.color_myloc_distance));
		((ImageView) mainView.findViewById(R.id.up_down_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_arrow_up));
		((ImageView) mainView.findViewById(R.id.previous_dot_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_undo_dark));
		((ImageView) mainView.findViewById(R.id.next_dot_icon))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_redo_dark));

		mainView.findViewById(R.id.add_point_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				measurementLayer.addPointOnClick();
				updateText();
			}
		});

		enterMeasurementMode();

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitMeasurementMode();
	}

	private void updateText() {
		distanceTv.setText(measurementLayer.getDistanceSt() + ",");
		pointsTv.setText(pointsSt + ": " + measurementLayer.getPointsCount());
	}

	private void enterMeasurementMode() {
		measurementLayer.setInMeasurementMode(true);
		mapActivity.refreshMap();
		mapActivity.disableDrawer();
		mark(View.INVISIBLE, R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);
		mark(View.GONE, R.id.map_route_info_button, R.id.map_menu_button, R.id.map_compass_button, R.id.map_layers_button,
				R.id.map_search_button, R.id.map_quick_actions_button);

		View collapseButton = mapActivity.findViewById(R.id.map_collapse_button);
		if (collapseButton != null && collapseButton.getVisibility() == View.VISIBLE) {
			wasCollapseButtonVisible = true;
			collapseButton.setVisibility(View.INVISIBLE);
		} else {
			wasCollapseButtonVisible = false;
		}

		updateText();
	}

	private void exitMeasurementMode() {
		measurementLayer.setInMeasurementMode(false);
		mapActivity.refreshMap();
		mapActivity.enableDrawer();
		mark(View.VISIBLE, R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info,
				R.id.map_route_info_button, R.id.map_menu_button, R.id.map_compass_button, R.id.map_layers_button,
				R.id.map_search_button, R.id.map_quick_actions_button);

		View collapseButton = mapActivity.findViewById(R.id.map_collapse_button);
		if (collapseButton != null && wasCollapseButtonVisible) {
			collapseButton.setVisibility(View.VISIBLE);
		}

		measurementLayer.clearPoints();
	}

	private void mark(int status, int... widgets) {
		for (int widget : widgets) {
			View v = mapActivity.findViewById(widget);
			if (v != null) {
				v.setVisibility(status);
			}
		}
	}
}
