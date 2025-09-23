package net.osmand.plus.measurementtool;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.OptionsDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleDividerItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;

import org.apache.commons.logging.Log;

public class SelectedPointBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectedPointBottomSheetDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SelectedPointBottomSheetDialogFragment.class);
	private MeasurementEditingContext editingCtx;
	private PlanRoutePointUtils planRoutePointUtils;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		if (requireMapActivity().getMapLayers().getMeasurementToolLayer().getEditingCtx().getPoints().isEmpty()) {
			dismiss();
			return null;
		}
		return super.onCreateView(inflater, parent, savedInstanceState);
	}

	@SuppressLint("InflateParams")
	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		editingCtx = mapActivity.getMapLayers().getMeasurementToolLayer().getEditingCtx();
		planRoutePointUtils = new PlanRoutePointUtils();

		View titleView = inflate(R.layout.bottom_sheet_item_with_descr_pad_32dp);
		TextView title = titleView.findViewById(R.id.title);
		title.setTypeface(FontCache.getMediumFont());

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
				.setOnClickListener(v -> {
					Fragment targetFragment = getTargetFragment();
					if (targetFragment instanceof SelectedPointFragmentListener) {
						((SelectedPointFragmentListener) targetFragment).onMovePoint();
					}
					dismiss();
				})
				.create();
		items.add(moveItem);

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem addAfterItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_addpoint_above))
				.setTitle(getString(R.string.add_point_after))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
						listener.onAddPointAfter();
					}
					dismiss();
				})
				.create();
		items.add(addAfterItem);

		BaseBottomSheetItem addBeforeItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_addpoint_below))
				.setTitle(getString(R.string.add_point_before))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
						listener.onAddPointBefore();
					}
					dismiss();
				})
				.create();
		items.add(addBeforeItem);

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem trimRouteBefore = new BottomSheetItemWithDescription.Builder()
				.setDescription(getDescription(true))
				.setIcon(getContentIcon(R.drawable.ic_action_trim_left))
				.setTitle(getString(R.string.plan_route_trim_before))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(v -> {
					if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
						listener.onTrimRouteBefore();
					}
					dismiss();
				})
				.setDisabled(editingCtx.isFirstPointSelected(false))
				.create();
		items.add(trimRouteBefore);

		BaseBottomSheetItem trimRouteAfter = new BottomSheetItemWithDescription.Builder()
				.setDescription(getDescription(false))
				.setIcon(getContentIcon(R.drawable.ic_action_trim_right))
				.setTitle(getString(R.string.plan_route_trim_after))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(v -> {
					if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
						listener.onTrimRouteAfter();
					}
					dismiss();
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
					.setOnClickListener(v -> {
						if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
							listener.onSplitPointsAfter();
						}
						dismiss();
					})
					.create();
			items.add(addNewSegment);
		} else if (editingCtx.isFirstPointSelected(false) || editingCtx.isLastPointSelected(false)) {
			items.add(new OptionsDividerItem(getContext()));

			// join
			BaseBottomSheetItem joinSegments = new BottomSheetItemWithDescription.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_join_segments))
					.setTitle(getString(R.string.join_segments))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
					.setOnClickListener(v -> {
						if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
							listener.onJoinPoints();
						}
						dismiss();
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
					.setOnClickListener(v -> {
						if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
							listener.onSplitPointsAfter();
						}
						dismiss();
					})
					.setDisabled(!editingCtx.canSplit(true))
					.create();
			items.add(splitAfter);

			BaseBottomSheetItem splitBefore = new BottomSheetItemWithDescription.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_split_after))
					.setTitle(getString(R.string.plan_route_split_before))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
					.setOnClickListener(v -> {
						if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
							listener.onSplitPointsBefore();
						}
						dismiss();
					})
					.setDisabled(!editingCtx.canSplit(false))
					.create();
			items.add(splitBefore);
		}

		items.add(new OptionsDividerItem(getContext()));

		boolean approximationNeeded = editingCtx.shouldCheckApproximation() && editingCtx.isApproximationNeeded();

		BaseBottomSheetItem changeRouteTypeBefore = new BottomSheetItemWithDescription.Builder()
				.setIcon(getRouteTypeIcon(true))
				.setTitle(getString(R.string.plan_route_change_route_type_before))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(v -> {
					if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
						listener.onChangeRouteTypeBefore();
					}
					dismiss();
				})
				.setDisabled(editingCtx.isFirstPointSelected(false) || approximationNeeded)
				.create();
		items.add(changeRouteTypeBefore);

		BaseBottomSheetItem changeRouteTypeAfter = new BottomSheetItemWithDescription.Builder()
				.setIcon(getRouteTypeIcon(false))
				.setTitle(getString(R.string.plan_route_change_route_type_after))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(v -> {
					if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
						listener.onChangeRouteTypeAfter();
					}
					dismiss();
				})
				.setDisabled(editingCtx.isLastPointSelected(false) || approximationNeeded)
				.create();
		items.add(changeRouteTypeAfter);

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem deleteItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getIcon(R.drawable.ic_action_delete_dark,
						R.color.color_osm_edit_delete))
				.setTitle(getString(R.string.shared_string_delete))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
						listener.onDeletePoint();
					}
					dismiss();
				})
				.create();
		items.add(deleteItem);
	}

	@Override
	public void dismiss() {
		if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
			listener.onCloseMenu();
		}
		super.dismiss();
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
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
		if (getTargetFragment() instanceof SelectedPointFragmentListener listener) {
			listener.onClearSelection();
		}
	}

	@NonNull
	private String getTitle() {
		return planRoutePointUtils.getPointTitle(getMapActivity(), getSelectedPointPosition());
	}

	@NonNull
	private String getDescription(boolean before) {
		return planRoutePointUtils.getPointSummary(getMapActivity(), getSelectedPointPosition(), before);
	}

	private int getSelectedPointPosition() {
		return editingCtx.getSelectedPointPosition();
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
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			SelectedPointBottomSheetDialogFragment fragment = new SelectedPointBottomSheetDialogFragment();
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(targetFragment, 0);
			fragment.show(fm, TAG);
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
