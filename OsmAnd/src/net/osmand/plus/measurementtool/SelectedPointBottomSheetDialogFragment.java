package net.osmand.plus.measurementtool;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleDividerItem;
import net.osmand.plus.measurementtool.NewGpxData.ActionType;
import net.osmand.util.MapUtils;

import java.util.List;

public class SelectedPointBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "SelectedPointBottomSheetDialogFragment";

	private SelectedPointFragmentListener listener;

	public void setListener(SelectedPointFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getDescription())
				.setIcon(getActiveIcon(R.drawable.ic_action_measure_point))
				.setTitle(getTitle())
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.create();
		items.add(titleItem);

		items.add(new TitleDividerItem(getContext()));

		BaseBottomSheetItem moveItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_move_point))
				.setTitle(getString(R.string.shared_string_move))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.moveOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(moveItem);

		BaseBottomSheetItem deleteItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_remove_dark))
				.setTitle(getString(R.string.shared_string_delete))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.deleteOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(deleteItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem addAfterItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_addpoint_above))
				.setTitle(getString(R.string.add_point_after))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.addPointAfterOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(addAfterItem);

		BaseBottomSheetItem addBeforeItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_addpoint_below))
				.setTitle(getString(R.string.add_point_before))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.addPointBeforeOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(addBeforeItem);
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

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected void onDismissButtonClickAction() {
		if (listener != null) {
			listener.onClearSelection();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	@NonNull
	private String getTitle() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return "";
		}

		MeasurementEditingContext editingCtx = mapActivity.getMapLayers().getMeasurementToolLayer().getEditingCtx();
		int pos = editingCtx.getSelectedPointPosition();

		String pointName = editingCtx.getPoints().get(pos).name;
		if (!TextUtils.isEmpty(pointName)) {
			return pointName;
		}

		NewGpxData newGpxData = editingCtx.getNewGpxData();
		if (newGpxData != null && newGpxData.getActionType() == ActionType.ADD_ROUTE_POINTS) {
			return getString(R.string.route_point) + " - " + (pos + 1);
		}

		return getString(R.string.plugin_distance_point) + " - " + (pos + 1);
	}

	@NonNull
	private String getDescription() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return "";
		}

		StringBuilder description = new StringBuilder();

		MeasurementEditingContext editingCtx = mapActivity.getMapLayers().getMeasurementToolLayer().getEditingCtx();
		int pos = editingCtx.getSelectedPointPosition();
		List<WptPt> points = editingCtx.getPoints();
		WptPt pt = points.get(pos);

		String pointDesc = pt.desc;
		if (!TextUtils.isEmpty(pointDesc)) {
			description.append(pointDesc);
		} else if (pos < 1) {
			description.append(getString(R.string.shared_string_control_start));
		} else {
			float dist = 0;
			for (int i = 1; i <= pos; i++) {
				WptPt first = points.get(i - 1);
				WptPt second = points.get(i);
				dist += MapUtils.getDistance(first.lat, first.lon, second.lat, second.lon);
			}
			description.append(OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication()));
		}

		NewGpxData newGpxData = editingCtx.getNewGpxData();
		if (newGpxData != null && newGpxData.getActionType() == ActionType.EDIT_SEGMENT) {
			double elevation = pt.ele;
			if (!Double.isNaN(elevation)) {
				description.append("  ").append((getString(R.string.altitude)).substring(0, 1)).append(": ");
				description.append(OsmAndFormatter.getFormattedAlt(elevation, mapActivity.getMyApplication()));
			}
			float speed = (float) pt.speed;
			if (speed != 0) {
				description.append("  ").append((getString(R.string.map_widget_speed)).substring(0, 1)).append(": ");
				description.append(OsmAndFormatter.getFormattedSpeed(speed, mapActivity.getMyApplication()));
			}
		}

		return description.toString();
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
