package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.apache.commons.logging.Log;

public class OptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = OptionsBottomSheetDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(OptionsBottomSheetDialogFragment.class);

	public static final String SNAP_TO_ROAD_ENABLED_KEY = "snap_to_road_enabled";
	public static final String TRACK_SNAPPED_TO_ROAD_KEY = "track_snapped_to_road";

	private OptionsFragmentListener listener;
	private ApplicationMode routeAppMode;

	public void setListener(OptionsFragmentListener listener) {
		this.listener = listener;
	}

	private void setRouteAppMode(ApplicationMode routeAppMode) {
		this.routeAppMode = routeAppMode;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		boolean snapToRoadEnabled = false;
		boolean trackSnappedToRoad = false;
		if (args != null) {
			snapToRoadEnabled = args.getBoolean(SNAP_TO_ROAD_ENABLED_KEY);
			trackSnappedToRoad = args.getBoolean(TRACK_SNAPPED_TO_ROAD_KEY);
		}

		items.add(new TitleItem(getString(R.string.shared_string_options)));

		String description;
		Drawable icon;
		if (trackSnappedToRoad) {
			if (!snapToRoadEnabled || routeAppMode == null) {
				description = getString(R.string.routing_profile_straightline);
				icon = getContentIcon(R.drawable.ic_action_split_interval);
			} else {
				description = routeAppMode.toHumanString();
				icon = getIcon(routeAppMode.getIconRes(), routeAppMode.getIconColorInfo().getColor(nightMode));
			}
		} else {
			description = getString(R.string.shared_string_undefined);
			icon = getContentIcon(R.drawable.ic_action_help);
		}

		BaseBottomSheetItem snapToRoadItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(description)
				.setIcon(icon)
				.setTitle(getString(R.string.route_between_points))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.snapToRoadOnCLick();
						}
						dismiss();
					}
				})
				.create();

		items.add(snapToRoadItem);

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem saveAsNewSegmentItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_save_to_file))
				.setTitle(getString(R.string.shared_string_save_changes))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.addToGpxOnClick();
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
						if (listener != null) {
							listener.addToTheTrackOnClick();
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
						if (listener != null) {
							listener.directionsOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(directions);

		BaseBottomSheetItem reverseRoute = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_change_navigation_points))
				.setTitle(getString(R.string.reverse_route))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.reverseRouteOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(reverseRoute);

		items.add(new OptionsDividerItem(getContext()));

		BaseBottomSheetItem clearAllItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getIcon(R.drawable.ic_action_reset_to_default_dark, (
						nightMode ? R.color.color_osm_edit_delete : R.color.color_osm_edit_delete)))
				.setTitle(getString(R.string.shared_string_clear_all))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.clearAllOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(clearAllItem);
	}

	private BaseBottomSheetItem getSaveAsNewTrackItem() {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_save_to_file))
				.setTitle(getString(R.string.save_as_new_track))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.saveAsNewTrackOnClick();
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

	public static void showInstance(@NonNull FragmentManager fm, boolean trackSnappedToRoad, boolean snapToRoad,
	                                ApplicationMode routeAppMode, OptionsFragmentListener optionsFragmentListener) {
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putBoolean(TRACK_SNAPPED_TO_ROAD_KEY, trackSnappedToRoad);
				args.putBoolean(SNAP_TO_ROAD_ENABLED_KEY, snapToRoad);
				OptionsBottomSheetDialogFragment fragment = new OptionsBottomSheetDialogFragment();
				fragment.setArguments(args);
				fragment.setRouteAppMode(routeAppMode);
				fragment.setUsedOnMap(true);
				fragment.setListener(optionsFragmentListener);
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

		void addToGpxOnClick();

		void saveAsNewTrackOnClick();

		void addToTheTrackOnClick();

		void directionsOnClick();

		void reverseRouteOnClick();

		void clearAllOnClick();
	}
}
