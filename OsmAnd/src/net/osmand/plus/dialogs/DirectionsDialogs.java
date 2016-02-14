package net.osmand.plus.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DirectionsDialogs {
	
	public static void directionsToDialogAndLaunchMap(final Activity act, final double lat, final double lon, 
			final PointDescription name) {
		final OsmandApplication ctx = (OsmandApplication) act.getApplication();
		final TargetPointsHelper targetPointsHelper = ctx.getTargetPointsHelper();
		if (targetPointsHelper.getIntermediatePoints().size() > 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(act);
			builder.setTitle(R.string.new_directions_point_dialog);
			builder.setItems(
					new String[] { act.getString(R.string.keep_intermediate_points),
							act.getString(R.string.clear_intermediate_points)},
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 1) {
								targetPointsHelper.clearPointToNavigate(false);
							}
							ctx.getSettings().navigateDialog();
							targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
							MapActivity.launchMapActivityMoveToTop(act);
						}
					});
			builder.show();
		} else {
			ctx.getSettings().navigateDialog();
			targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
			MapActivity.launchMapActivityMoveToTop(act);
		}
	}
	
	public static void addWaypointDialogAndLaunchMap(final Activity act, final double lat, final double lon, final PointDescription name) {
		final OsmandApplication ctx = (OsmandApplication) act.getApplication();
		final TargetPointsHelper targetPointsHelper = ctx.getTargetPointsHelper();
		if (targetPointsHelper.getPointToNavigate() != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(act);
			builder.setTitle(R.string.new_destination_point_dialog);
			builder.setItems(
					new String[] { act.getString(R.string.replace_destination_point),
							act.getString(R.string.keep_and_add_destination_point),
							act.getString(R.string.add_as_first_destination_point), act.getString(R.string.add_as_last_destination_point) },
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 0) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
								closeContextMenu(act);
							} else if (which == 1) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, 
										targetPointsHelper.getIntermediatePoints().size() + 1, name);
								closeContextMenu(act);
							} else if (which == 2) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, 0, name);
								closeContextMenu(act);
							} else {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, targetPointsHelper.getIntermediatePoints().size(), name);
								closeContextMenu(act);
							}
							MapActivity.launchMapActivityMoveToTop(act);
						}
					});
			builder.show();
		} else {
			targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
			closeContextMenu(act);
			MapActivity.launchMapActivityMoveToTop(act);
		}
	}

	private static void closeContextMenu(Activity act) {
		if (act instanceof MapActivity) {
			((MapActivity) act).getContextMenu().close();
		}
	}

	public static void setupPopUpMenuIcon(PopupMenu menu){
		try {
			Field[] fields = menu.getClass().getDeclaredFields();
			for (Field field : fields) {
				if ("mPopup".equals(field.getName())) {
					field.setAccessible(true);
					Object menuPopupHelper = field.get(menu);
					Class<?> classPopupHelper = Class.forName(menuPopupHelper
							.getClass().getName());
					Method setForceIcons = classPopupHelper.getMethod(
							"setForceShowIcon", boolean.class);
					setForceIcons.invoke(menuPopupHelper, true);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
