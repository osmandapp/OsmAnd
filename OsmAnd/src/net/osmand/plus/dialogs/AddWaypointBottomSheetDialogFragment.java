package net.osmand.plus.dialogs;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class AddWaypointBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "AddWaypointBottomSheetDialogFragment";
	public static final String LAT_KEY = "latitude";
	public static final String LON_KEY = "longitude";
	public static final String POINT_DESCRIPTION_KEY = "point_description";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = requireArguments();
		LatLon latLon = new LatLon(args.getDouble(LAT_KEY), args.getDouble(LON_KEY));
		PointDescription name = PointDescription.deserializeFromString(args.getString(POINT_DESCRIPTION_KEY), latLon);
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();

		items.add(new TitleItem(getString(R.string.new_destination_point_dialog)));

		BaseBottomSheetItem replaceDestItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getCurrentPointName(targetPointsHelper.getPointToNavigate(), false))
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setIcon(getIcon(R.drawable.list_destination, 0))
				.setTitle(getString(R.string.replace_destination_point))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.setOnClickListener(v -> {
					targetPointsHelper.navigateToPoint(latLon, true, -1, name);
					dismiss();
				})
				.create();
		items.add(replaceDestItem);

		BaseBottomSheetItem replaceStartItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getCurrentPointName(targetPointsHelper.getPointToStart(), true))
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setIcon(getIcon(R.drawable.list_startpoint, 0))
				.setTitle(getString(R.string.make_as_start_point))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.setOnClickListener(v -> {
					targetPointsHelper.setStartPoint(latLon, true, name);
					dismiss();
				})
				.create();
		items.add(replaceStartItem);

		items.add(new DividerHalfItem(getContext(), getDividerColorId()));

		BaseBottomSheetItem subsequentDestItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.subsequent_dest_description))
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setIcon(getSubsequentDestIcon())
				.setTitle(getString(R.string.keep_and_add_destination_point))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.setOnClickListener(v -> {
					targetPointsHelper.navigateToPoint(latLon, true,
							targetPointsHelper.getIntermediatePoints().size() + 1, name);
					dismiss();
				})
				.create();
		items.add(subsequentDestItem);

		BaseBottomSheetItem firstIntermItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.first_intermediate_dest_description))
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setIcon(getFirstIntermDestIcon())
				.setTitle(getString(R.string.add_as_first_destination_point))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.setOnClickListener(v -> {
					targetPointsHelper.navigateToPoint(latLon, true, 0, name);
					dismiss();
				})
				.create();
		items.add(firstIntermItem);

		BaseBottomSheetItem lastIntermItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.last_intermediate_dest_description))
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setIcon(getLastIntermDistIcon())
				.setTitle(getString(R.string.add_as_last_destination_point))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.setOnClickListener(v -> {
					targetPointsHelper.navigateToPoint(latLon, true,
							targetPointsHelper.getIntermediatePoints().size(), name);
					dismiss();
				})
				.create();
		items.add(lastIntermItem);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		closeContextMenu();
	}

	@Override
	protected int getDividerColorId() {
		return nightMode ? R.color.card_and_list_background_dark : -1;
	}

	@Override
	protected Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.icon_color_active_dark : R.color.map_widget_blue);
	}

	private Drawable getBackgroundIcon(@DrawableRes int resId) {
		return getIcon(resId, ColorUtilities.getSecondaryTextColorId(nightMode));
	}

	private LayerDrawable getLayerDrawable(@DrawableRes int bgIdRes, @DrawableRes int icIdRes) {
		return new LayerDrawable(new Drawable[]{getBackgroundIcon(bgIdRes), getActiveIcon(icIdRes)});
	}

	private LayerDrawable getSubsequentDestIcon() {
		return getLayerDrawable(R.drawable.ic_action_route_subsequent_destination,
				R.drawable.ic_action_route_subsequent_destination_point);
	}

	private LayerDrawable getFirstIntermDestIcon() {
		return getLayerDrawable(R.drawable.ic_action_route_first_intermediate,
				R.drawable.ic_action_route_first_intermediate_point);
	}

	private LayerDrawable getLastIntermDistIcon() {
		return getLayerDrawable(R.drawable.ic_action_route_last_intermediate,
				R.drawable.ic_action_route_last_intermediate_point);
	}

	private String getCurrentPointName(@Nullable TargetPoint point, boolean start) {
		StringBuilder builder = new StringBuilder(getString(R.string.shared_string_current));
		builder.append(": ");
		if (point != null) {
			if (!point.getOnlyName().isEmpty()) {
				builder.append(point.getOnlyName());
			} else {
				builder.append(getString(R.string.route_descr_map_location));
				builder.append(" ");
				builder.append(getString(R.string.route_descr_lat_lon, point.getLatitude(), point.getLongitude()));
			}
		} else if (start) {
			builder.append(getString(R.string.shared_string_my_location));
		}
		return builder.toString();
	}

	private void closeContextMenu() {
		callMapActivity(mapActivity -> mapActivity.getContextMenu().close());
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                double lat, double lon, @NonNull PointDescription name) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putDouble(LAT_KEY, lat);
			args.putDouble(LON_KEY, lon);
			args.putString(POINT_DESCRIPTION_KEY, PointDescription.serializeToString(name));
			AddWaypointBottomSheetDialogFragment fragment = new AddWaypointBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}
}
