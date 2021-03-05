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

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescriptionDifHeight;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.apache.commons.logging.Log;

public class OptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = OptionsBottomSheetDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(OptionsBottomSheetDialogFragment.class);

	public static final String TRACK_SNAPPED_TO_ROAD_KEY = "track_snapped_to_road";
	public static final String SNAP_TO_ROAD_APP_MODE_KEY = "snap_to_road_app_mode";
	public static final String ADD_NEW_SEGMENT_ALLOWED_KEY = "add_new_segment_allowed";

	private ApplicationMode routeAppMode;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		boolean trackSnappedToRoad = false;
		boolean addNewSegmentAllowed = false;
		if (args != null) {
			trackSnappedToRoad = args.getBoolean(TRACK_SNAPPED_TO_ROAD_KEY);
			addNewSegmentAllowed = args.getBoolean(ADD_NEW_SEGMENT_ALLOWED_KEY);
			routeAppMode = ApplicationMode.valueOfStringKey(args.getString(SNAP_TO_ROAD_APP_MODE_KEY), null);
		}

		items.add(new TitleItem(getString(R.string.shared_string_options)));

		String description;
		Drawable icon;
		if (trackSnappedToRoad) {
			if (routeAppMode == null || routeAppMode == MeasurementEditingContext.DEFAULT_APP_MODE) {
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
				.setMinHeight(getResources().getDimensionPixelSize(R.dimen.card_row_min_height))
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

		if (addNewSegmentAllowed) {
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
				.setTitle(getString(R.string.get_directions))
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

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem clearAllItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getIcon(R.drawable.ic_action_reset_to_default_dark,
						nightMode ? R.color.color_osm_edit_delete : R.color.color_osm_edit_delete))
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
		params.rightMargin = view.getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin_large);
	}

	public static void showInstance(@NonNull FragmentManager fm, Fragment targetFragment,
									boolean trackSnappedToRoad, boolean addNewSegmentAllowed, String routeAppModeStringKey) {
		try {
			if (!fm.isStateSaved()) {
				OptionsBottomSheetDialogFragment fragment = new OptionsBottomSheetDialogFragment();
				Bundle args = new Bundle();
				args.putBoolean(TRACK_SNAPPED_TO_ROAD_KEY, trackSnappedToRoad);
				args.putBoolean(ADD_NEW_SEGMENT_ALLOWED_KEY, addNewSegmentAllowed);
				args.putString(SNAP_TO_ROAD_APP_MODE_KEY, routeAppModeStringKey);
				fragment.setArguments(args);
				fragment.setTargetFragment(targetFragment,0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	interface OptionsFragmentListener {

		void snapToRoadOnCLick();

		void addNewSegmentOnClick();

		void saveChangesOnClick();

		void saveAsNewTrackOnClick();

		void addToTrackOnClick();

		void directionsOnClick();

		void reverseRouteOnClick();

		void clearAllOnClick();
	}
}
