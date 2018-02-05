package net.osmand.plus.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

public class AddWaypointBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "AddWaypointBottomSheetDialogFragment";
	public static final String LAT_KEY = "latitude";
	public static final String LON_KEY = "longitude";
	public static final String POINT_DESCRIPTION_KEY = "point_description";

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Bundle args = getArguments();
		final LatLon latLon = new LatLon(args.getDouble(LAT_KEY), args.getDouble(LON_KEY));
		final PointDescription name = PointDescription.deserializeFromString(args.getString(POINT_DESCRIPTION_KEY), latLon);
		final TargetPointsHelper targetPointsHelper = getMyApplication().getTargetPointsHelper();

		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.fragment_add_waypoint_bottom_sheet_dialog, container);

		((TextView) mainView.findViewById(R.id.current_dest_text_view))
				.setText(getCurrentPointName(targetPointsHelper.getPointToNavigate(), false));
		((TextView) mainView.findViewById(R.id.current_start_text_view))
				.setText(getCurrentPointName(targetPointsHelper.getPointToStart(), true));

		((ImageView) mainView.findViewById(R.id.subsequent_dest_icon)).setImageDrawable(getSubsequentDestIcon());
		((ImageView) mainView.findViewById(R.id.first_interm_dest_icon)).setImageDrawable(getFirstIntermDestIcon());
		((ImageView) mainView.findViewById(R.id.last_interm_dest_icon)).setImageDrawable(getLastIntermDistIcon());

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int id = v.getId();
				if (id == R.id.replace_dest_row) {
					targetPointsHelper.navigateToPoint(latLon, true, -1, name);
				} else if (id == R.id.replace_start_row) {
					TargetPoint start = targetPointsHelper.getPointToStart();
					if (start != null) {
						targetPointsHelper.navigateToPoint(new LatLon(start.getLatitude(), start.getLongitude()),
								false, 0, start.getOriginalPointDescription());
					}
					targetPointsHelper.setStartPoint(latLon, true, name);
				} else if (id == R.id.subsequent_dest_row) {
					targetPointsHelper.navigateToPoint(latLon, true,
							targetPointsHelper.getIntermediatePoints().size() + 1, name);
				} else if (id == R.id.first_intermediate_dest_row) {
					targetPointsHelper.navigateToPoint(latLon, true, 0, name);
				} else if (id == R.id.last_intermediate_dest_row) {
					targetPointsHelper.navigateToPoint(latLon, true, targetPointsHelper.getIntermediatePoints().size(), name);
				}
				dismiss();
			}
		};

		mainView.findViewById(R.id.replace_dest_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.replace_start_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.subsequent_dest_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.first_intermediate_dest_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.last_intermediate_dest_row).setOnClickListener(onClickListener);
		mainView.findViewById(R.id.cancel_row).setOnClickListener(onClickListener);

		if (nightMode) {
			int dividerColor = ContextCompat.getColor(getContext(), R.color.route_info_bottom_view_bg_dark);
			mainView.findViewById(R.id.current_dest_divider).setBackgroundColor(dividerColor);
			mainView.findViewById(R.id.cancel_divider).setBackgroundColor(dividerColor);
		}

		setupHeightAndBackground(mainView, R.id.scroll_view);

		return mainView;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		closeContextMenu();
	}

	@Override
	protected Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.ctx_menu_direction_color_dark : R.color.map_widget_blue);
	}

	@Override
	protected int getPortraitBgResId() {
		return nightMode ? R.drawable.bg_additional_menu_dark : R.drawable.bg_bottom_menu_light;
	}

	@Override
	protected int getLandscapeTopsidesBgResId() {
		return nightMode ? R.drawable.bg_additional_menu_topsides_dark : R.drawable.bg_bottom_sheet_topsides_landscape_light;
	}

	@Override
	protected int getLandscapeSidesBgResId() {
		return nightMode ? R.drawable.bg_additional_menu_sides_dark : R.drawable.bg_bottom_sheet_sides_landscape_light;
	}

	private Drawable getBackgroundIcon(@DrawableRes int resId) {
		return getIcon(resId, R.color.searchbar_text_hint_light);
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
		Context ctx = getContext();
		StringBuilder builder = new StringBuilder(ctx.getString(R.string.shared_string_current));
		builder.append(": ");
		if (point != null) {
			if (point.getOnlyName().length() > 0) {
				builder.append(point.getOnlyName());
			} else {
				builder.append(ctx.getString(R.string.route_descr_map_location));
				builder.append(" ");
				builder.append(ctx.getString(R.string.route_descr_lat_lon, point.getLatitude(), point.getLongitude()));
			}
		} else if (start) {
			builder.append(ctx.getString(R.string.shared_string_my_location));
		}
		return builder.toString();
	}

	private void closeContextMenu() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).getContextMenu().close();
		}
	}
}
