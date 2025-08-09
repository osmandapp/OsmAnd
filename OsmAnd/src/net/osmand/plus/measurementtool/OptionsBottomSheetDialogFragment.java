package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescriptionDifHeight;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem.Builder;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.OptionsDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;

public class OptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private static final String TAG = OptionsBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		MeasurementEditingContext editingCtx = null;
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof MeasurementToolFragment) {
			editingCtx = ((MeasurementToolFragment) targetFragment).getEditingCtx();
		}
		if (editingCtx == null) {
			return;
		}
		items.add(new TitleItem(getString(R.string.shared_string_options)));

		String description;
		Drawable icon;
		boolean trackSnappedToRoad = !editingCtx.shouldCheckApproximation() || !editingCtx.isApproximationNeeded();
		if (trackSnappedToRoad) {
			ApplicationMode routeAppMode = editingCtx.getAppMode();
			if (routeAppMode == MeasurementEditingContext.DEFAULT_APP_MODE) {
				description = getString(R.string.routing_profile_straightline);
				icon = getContentIcon(R.drawable.ic_action_split_interval);
			} else {
				description = routeAppMode.toHumanString();
				icon = getPaintedIcon(routeAppMode.getIconRes(), routeAppMode.getProfileColor(nightMode));
			}
		} else {
			description = getString(R.string.shared_string_undefined);
			icon = getContentIcon(R.drawable.ic_action_help);
		}

		BaseBottomSheetItem snapToRoadItem = new BottomSheetItemWithDescriptionDifHeight.Builder()
				.setMinHeight(getDimensionPixelSize(R.dimen.card_row_min_height))
				.setDescription(description)
				.setIcon(icon)
				.setTitle(getString(R.string.route_between_points))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment fragment = getTargetFragment();
						if (fragment instanceof OptionsFragmentListener) {
							((OptionsFragmentListener) fragment).snapToRoadOnCLick();
						}
						dismiss();
					}
				})
				.create();

		items.add(snapToRoadItem);

		if (editingCtx.isAddNewSegmentAllowed()) {
			BaseBottomSheetItem addNewSegment = new BottomSheetItemWithDescription.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_new_segment))
					.setTitle(getString(R.string.plan_route_add_new_segment))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Fragment fragment = getTargetFragment();
							if (fragment instanceof OptionsFragmentListener) {
								((OptionsFragmentListener) fragment).addNewSegmentOnClick();
							}
							dismiss();
						}
					})
					.create();
			items.add(addNewSegment);
		}

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem saveAsNewSegmentItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_save_to_file))
				.setTitle(getString(R.string.shared_string_save_changes))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment fragment = getTargetFragment();
						if (fragment instanceof OptionsFragmentListener) {
							((OptionsFragmentListener) fragment).saveChangesOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(saveAsNewSegmentItem);

		items.add(getSaveAsNewTrackItem());

		BaseBottomSheetItem addToTrackItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_add_to_track))
				.setTitle(getString(R.string.add_to_a_track))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment fragment = getTargetFragment();
						if (fragment instanceof OptionsFragmentListener) {
							((OptionsFragmentListener) fragment).addToTrackOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(addToTrackItem);

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem directions = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark))
				.setTitle(getString(R.string.shared_string_navigation))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment fragment = getTargetFragment();
						if (fragment instanceof OptionsFragmentListener) {
							((OptionsFragmentListener) fragment).directionsOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(directions);

		BaseBottomSheetItem reverse = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_change_navigation_points))
				.setTitle(getString(R.string.reverse_route))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment fragment = getTargetFragment();
						if (fragment instanceof OptionsFragmentListener) {
							((OptionsFragmentListener) fragment).reverseRouteOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(reverse);

		BaseBottomSheetItem attachToRoads = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_attach_track))
				.setTitle(getString(R.string.attach_to_the_roads))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					Fragment target = getTargetFragment();
					if (target instanceof OptionsFragmentListener) {
						((OptionsFragmentListener) target).attachToRoadsClick();
					}
					dismiss();
				})
				.create();
		items.add(attachToRoads);

		boolean plainTrack = editingCtx.getPointsCount() > 0 && !editingCtx.hasRoutePoints() && !editingCtx.hasRoute();
		if (plainTrack) {
			BaseBottomSheetItem gpsFilter = new Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_filter_dark))
					.setTitle(getString(R.string.shared_string_gps_filter))
					.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
					.setOnClickListener(v -> {
						Fragment fragment = getTargetFragment();
						if (fragment instanceof OptionsFragmentListener) {
							((OptionsFragmentListener) fragment).gpsFilterOnClick();
						}
						dismiss();
					})
					.create();
			items.add(gpsFilter);
		}

		GpxData gpxData = editingCtx.getGpxData();
		if (gpxData != null && gpxData.getGpxFile() != null && !editingCtx.hasElevationData()) {
			BaseBottomSheetItem gpsFilter = new Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_terrain))
					.setTitle(getString(R.string.get_altitude_data))
					.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
					.setOnClickListener(v -> {
						Fragment fragment = getTargetFragment();
						if (fragment instanceof OptionsFragmentListener) {
							((OptionsFragmentListener) fragment).getAltitudeClick();
						}
						dismiss();
					})
					.create();
			items.add(gpsFilter);
		}

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem clearAllItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getIcon(R.drawable.ic_action_reset_to_default_dark,
						R.color.color_osm_edit_delete))
				.setTitle(getString(R.string.shared_string_clear_all))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment fragment = getTargetFragment();
						if (fragment instanceof OptionsFragmentListener) {
							((OptionsFragmentListener) fragment).clearAllOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(clearAllItem);
	}

	private BaseBottomSheetItem getSaveAsNewTrackItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_save_as_new_file))
				.setTitle(getString(R.string.save_as_new_track))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment fragment = getTargetFragment();
						if (fragment instanceof OptionsFragmentListener) {
							((OptionsFragmentListener) fragment).saveAsNewTrackOnClick();
						}
						dismiss();
					}
				})
				.create();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ImageView icon = view.findViewById(R.id.icon);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) icon.getLayoutParams();
		params.rightMargin = getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin_large);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull MeasurementToolFragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			OptionsBottomSheetDialogFragment fragment = new OptionsBottomSheetDialogFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	interface OptionsFragmentListener {

		void snapToRoadOnCLick();

		void addNewSegmentOnClick();

		void saveChangesOnClick();

		void saveAsNewTrackOnClick();

		void addToTrackOnClick();

		void directionsOnClick();

		void reverseRouteOnClick();

		void attachToRoadsClick();

		void gpsFilterOnClick();

		void getAltitudeClick();

		void clearAllOnClick();
	}
}
