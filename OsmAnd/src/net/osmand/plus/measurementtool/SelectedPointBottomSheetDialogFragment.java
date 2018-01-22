package net.osmand.plus.measurementtool;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.measurementtool.NewGpxData.ActionType;
import net.osmand.util.MapUtils;

import java.util.List;

public class SelectedPointBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "SelectedPointBottomSheetDialogFragment";

	private SelectedPointFragmentListener listener;

	public void setListener(SelectedPointFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		final MeasurementToolLayer measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_selected_menu_bottom_sheet_dialog, null);

		((ImageView) mainView.findViewById(R.id.selected_point_icon)).setImageDrawable(getActiveIcon(R.drawable.ic_action_measure_point));
		((ImageView) mainView.findViewById(R.id.move_point_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_move_point));
		((ImageView) mainView.findViewById(R.id.delete_point_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		((ImageView) mainView.findViewById(R.id.add_point_after_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_addpoint_above));
		((ImageView) mainView.findViewById(R.id.add_point_before_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_addpoint_below));

		mainView.findViewById(R.id.move_point_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.moveOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.delete_point_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.deleteOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.add_point_after_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.addPointAfterOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.add_point_before_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.addPointBeforeOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.onClearSelection();
				}
				dismiss();
			}
		});

		List<WptPt> points = measurementLayer.getEditingCtx().getPoints();
		int pos = measurementLayer.getEditingCtx().getSelectedPointPosition();
		WptPt pt = points.get(pos);
		String pointTitle = pt.name;
		if (!TextUtils.isEmpty(pointTitle)) {
			((TextView) mainView.findViewById(R.id.selected_point_title)).setText(pointTitle);
		} else {
			NewGpxData newGpxData = measurementLayer.getEditingCtx().getNewGpxData();
			if (newGpxData != null) {
				ActionType actionType = measurementLayer.getEditingCtx().getNewGpxData().getActionType();
				if (actionType == ActionType.ADD_ROUTE_POINTS) {
					((TextView) mainView.findViewById(R.id.selected_point_title)).setText(mapActivity.getString(R.string.route_point) + " - " + (pos + 1));
				} else {
					((TextView) mainView.findViewById(R.id.selected_point_title)).setText(mapActivity.getString(R.string.plugin_distance_point) + " - " + (pos + 1));
				}
			} else {
				((TextView) mainView.findViewById(R.id.selected_point_title)).setText(mapActivity.getString(R.string.plugin_distance_point) + " - " + (pos + 1));
			}
		}
		String pointDesc = pt.desc;
		if (!TextUtils.isEmpty(pointDesc)) {
			((TextView) mainView.findViewById(R.id.selected_point_distance)).setText(pointDesc);
		} else {
			if (pos < 1) {
				((TextView) mainView.findViewById(R.id.selected_point_distance)).setText(mapActivity.getString(R.string.shared_string_control_start));
			} else {
				float dist = 0;
				for (int i = 1; i <= pos; i++) {
					dist += MapUtils.getDistance(points.get(i - 1).lat, points.get(i - 1).lon, points.get(i).lat, points.get(i).lon);
				}
				((TextView) mainView.findViewById(R.id.selected_point_distance)).setText(OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication()));
			}
		}
		NewGpxData newGpxData = measurementLayer.getEditingCtx().getNewGpxData();
		if (newGpxData != null && newGpxData.getActionType() == ActionType.EDIT_SEGMENT) {
			double elevation = pt.ele;
			if (!Double.isNaN(elevation)) {
				String eleStr = (mapActivity.getString(R.string.altitude)).substring(0, 1);
				((TextView) mainView.findViewById(R.id.selected_point_ele)).setText(eleStr + ": " + OsmAndFormatter.getFormattedAlt(elevation, mapActivity.getMyApplication()));
			}
			float speed = (float) pt.speed;
			if (speed != 0) {
				String speedStr = (mapActivity.getString(R.string.map_widget_speed)).substring(0, 1);
				((TextView) mainView.findViewById(R.id.selected_point_speed)).setText(speedStr + ": " + OsmAndFormatter.getFormattedSpeed(speed, mapActivity.getMyApplication()));
			}
		}

		setupHeightAndBackground(mainView, R.id.selected_point_options_scroll_view);

		return mainView;
	}

	@Override
	public void dismiss() {
		if (listener != null) {
			listener.onCloseMenu();
		}
		super.dismiss();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (listener != null) {
			listener.onCloseMenu();
			listener.onClearSelection();
		}
		super.onCancel(dialog);
	}

	interface SelectedPointFragmentListener {

		void moveOnClick();

		void deleteOnClick();

		void addPointAfterOnClick();

		void addPointBeforeOnClick();

		void onCloseMenu();

		void onClearSelection();
	}
}
