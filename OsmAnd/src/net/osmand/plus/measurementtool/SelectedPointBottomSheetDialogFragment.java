package net.osmand.plus.measurementtool;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleDividerItem;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.List;

public class SelectedPointBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectedPointBottomSheetDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SelectedPointBottomSheetDialogFragment.class);
	private MeasurementEditingContext editingCtx;

	@SuppressLint("InflateParams")
	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		editingCtx = mapActivity.getMapLayers().getMeasurementToolLayer().getEditingCtx();

		View titleView = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.bottom_sheet_item_with_descr_pad_32dp, null, false);
		TextView title = titleView.findViewById(R.id.title);
		title.setTypeface(FontCache.getRobotoMedium(getActivity()));

		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getDescription(true))
				.setIcon(getActiveIcon(R.drawable.ic_action_measure_point))
				.setTitle(getTitle())
				.setCustomView(titleView)
				.create();
		items.add(titleItem);

		items.add(new TitleDividerItem(getContext()));

		BaseBottomSheetItem moveItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_move_point))
				.setTitle(getString(R.string.shared_string_move))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof SelectedPointFragmentListener) {
							((SelectedPointFragmentListener) targetFragment).onMovePoint();
						}
						dismiss();
					}
				})
				.create();
		items.add(moveItem);

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem addAfterItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_addpoint_above))
				.setTitle(getString(R.string.add_point_after))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof SelectedPointFragmentListener) {
							((SelectedPointFragmentListener) targetFragment).onAddPointAfter();
						}
						dismiss();
					}
				})
				.create();
		items.add(addAfterItem);

		BaseBottomSheetItem addBeforeItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_addpoint_below))
				.setTitle(getString(R.string.add_point_before))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof SelectedPointFragmentListener) {
							((SelectedPointFragmentListener) targetFragment).onAddPointBefore();
						}
						dismiss();
					}
				})
				.create();
		items.add(addBeforeItem);

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem trimRouteBefore = new BottomSheetItemWithDescription.Builder()
				.setDescription(getDescription(true))
				.setIcon(getContentIcon(R.drawable.ic_action_trim_left))
				.setTitle(getString(R.string.plan_route_trim_before))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof SelectedPointFragmentListener) {
							((SelectedPointFragmentListener) targetFragment).onTrimRouteBefore();
						}
						dismiss();
					}
				})
				.setDisabled(editingCtx.isFirstPointSelected(false))
				.create();
		items.add(trimRouteBefore);

		BaseBottomSheetItem trimRouteAfter = new BottomSheetItemWithDescription.Builder()
				.setDescription(getDescription(false))
				.setIcon(getContentIcon(R.drawable.ic_action_trim_right))
				.setTitle(getString(R.string.plan_route_trim_after))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof SelectedPointFragmentListener) {
							((SelectedPointFragmentListener) targetFragment).onTrimRouteAfter();
						}
						dismiss();
					}
				})
				.setDisabled(editingCtx.isLastPointSelected(false))
				.create();
		items.add(trimRouteAfter);

		if (editingCtx.isFirstPointSelected(true)) {
			// skip
		} else if (editingCtx.isLastPointSelected(true)) {
			items.add(new OptionsDividerItem(getContext()));

			// new segment
			BaseBottomSheetItem addNewSegment = new BottomSheetItemWithDescription.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_new_segment))
					.setTitle(getString(R.string.plan_route_add_new_segment))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Fragment targetFragment = getTargetFragment();
							if (targetFragment instanceof SelectedPointFragmentListener) {
								((SelectedPointFragmentListener) targetFragment).onSplitPointsAfter();
							}
							dismiss();
						}
					})
					.create();
			items.add(addNewSegment);
		} else if (editingCtx.isFirstPointSelected(false) || editingCtx.isLastPointSelected(false)) {
			items.add(new OptionsDividerItem(getContext()));

			// join
			BaseBottomSheetItem joinSegments = new BottomSheetItemWithDescription.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_join_segments))
					.setTitle(getString(R.string.plan_route_join_segments))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Fragment targetFragment = getTargetFragment();
							if (targetFragment instanceof SelectedPointFragmentListener) {
								((SelectedPointFragmentListener) targetFragment).onJoinPoints();
							}
							dismiss();
						}
					})
					.create();
			items.add(joinSegments);
		} else {
			items.add(new OptionsDividerItem(getContext()));

			// split
			BaseBottomSheetItem splitAfter = new BottomSheetItemWithDescription.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_split_after))
					.setTitle(getString(R.string.plan_route_split_after))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Fragment targetFragment = getTargetFragment();
							if (targetFragment instanceof SelectedPointFragmentListener) {
								((SelectedPointFragmentListener) targetFragment).onSplitPointsAfter();
							}
							dismiss();
						}
					})
					.setDisabled(!editingCtx.canSplit(true))
					.create();
			items.add(splitAfter);

			BaseBottomSheetItem splitBefore = new BottomSheetItemWithDescription.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_split_after))
					.setTitle(getString(R.string.plan_route_split_before))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Fragment targetFragment = getTargetFragment();
							if (targetFragment instanceof SelectedPointFragmentListener) {
								((SelectedPointFragmentListener) targetFragment).onSplitPointsBefore();
							}
							dismiss();
						}
					})
					.setDisabled(!editingCtx.canSplit(false))
					.create();
			items.add(splitBefore);
		}

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem changeRouteTypeBefore = new BottomSheetItemWithDescription.Builder()
				.setIcon(getRouteTypeIcon(true))
				.setTitle(getString(R.string.plan_route_change_route_type_before))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof SelectedPointFragmentListener) {
							((SelectedPointFragmentListener) targetFragment).onChangeRouteTypeBefore();
						}
						dismiss();
					}
				})
				.setDisabled(editingCtx.isFirstPointSelected(false) || editingCtx.isApproximationNeeded())
				.create();
		items.add(changeRouteTypeBefore);

		BaseBottomSheetItem changeRouteTypeAfter = new BottomSheetItemWithDescription.Builder()
				.setIcon(getRouteTypeIcon(false))
				.setTitle(getString(R.string.plan_route_change_route_type_after))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof SelectedPointFragmentListener) {
							((SelectedPointFragmentListener) targetFragment).onChangeRouteTypeAfter();
						}
						dismiss();
					}
				})
				.setDisabled(editingCtx.isLastPointSelected(false) || editingCtx.isApproximationNeeded())
				.create();
		items.add(changeRouteTypeAfter);

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem deleteItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getIcon(R.drawable.ic_action_delete_dark,
						nightMode ? R.color.color_osm_edit_delete : R.color.color_osm_edit_delete))
				.setTitle(getString(R.string.shared_string_delete))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof SelectedPointFragmentListener) {
							((SelectedPointFragmentListener) targetFragment).onDeletePoint();
						}
						dismiss();
					}
				})
				.create();
		items.add(deleteItem);
	}

	@Override
	public void dismiss() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof SelectedPointFragmentListener) {
			((SelectedPointFragmentListener) targetFragment).onCloseMenu();
		}
		super.dismiss();
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof SelectedPointFragmentListener) {
			((SelectedPointFragmentListener) targetFragment).onCloseMenu();
			((SelectedPointFragmentListener) targetFragment).onClearSelection();
		}
		super.onCancel(dialog);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected void onDismissButtonClickAction() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof SelectedPointFragmentListener) {
			((SelectedPointFragmentListener) targetFragment).onClearSelection();
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
		int pos = editingCtx.getSelectedPointPosition();
		String pointName = editingCtx.getPoints().get(pos).name;
		if (!TextUtils.isEmpty(pointName)) {
			return pointName;
		}
		return getString(R.string.ltr_or_rtl_combine_via_dash, getString(R.string.plugin_distance_point), String.valueOf(pos + 1));
	}

	@NonNull
	private String getDescription(boolean before) {
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
		} else if (pos < 1 && before) {
			description.append(getString(R.string.shared_string_control_start));
		} else {
			float dist = 0;
			int startIdx;
			int endIdx;
			if (before) {
				startIdx = 1;
				endIdx = pos;
			} else {
				startIdx = pos + 1;
				endIdx = points.size() - 1;
			}
			for (int i = startIdx; i <= endIdx; i++) {
				WptPt first = points.get(i - 1);
				WptPt second = points.get(i);
				dist += MapUtils.getDistance(first.lat, first.lon, second.lat, second.lon);
			}
			description.append(OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication()));
		}
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
		return description.toString();
	}

	@Nullable
	private Drawable getRouteTypeIcon(boolean before) {
		ApplicationMode routeAppMode = before ? editingCtx.getBeforeSelectedPointAppMode() : editingCtx.getSelectedPointAppMode();
		Drawable icon;
		if (MeasurementEditingContext.DEFAULT_APP_MODE.equals(routeAppMode)) {
			icon = getContentIcon(R.drawable.ic_action_split_interval);
		} else {
			icon = getPaintedIcon(routeAppMode.getIconRes(), routeAppMode.getProfileColor(nightMode));
		}
		return icon;
	}

	public static void showInstance(@NonNull FragmentManager fm, @Nullable Fragment targetFragment) {
		try {
			if (!fm.isStateSaved()) {
				SelectedPointBottomSheetDialogFragment fragment = new SelectedPointBottomSheetDialogFragment();
				fragment.setRetainInstance(true);
				fragment.setTargetFragment(targetFragment, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	interface SelectedPointFragmentListener {

		void onMovePoint();

		void onDeletePoint();

		void onAddPointAfter();

		void onAddPointBefore();

		void onTrimRouteBefore();

		void onTrimRouteAfter();

		void onSplitPointsAfter();

		void onSplitPointsBefore();

		void onJoinPoints();

		void onChangeRouteTypeBefore();

		void onChangeRouteTypeAfter();

		void onCloseMenu();

		void onClearSelection();
	}
}
