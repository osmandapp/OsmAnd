package net.osmand.plus.dialogs;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DirectionsDialogs {
	
	public static void directionsToDialogAndLaunchMap(@NonNull Activity activity, double lat, double lon,
	                                                  @NonNull PointDescription name) {
		OsmandApplication ctx = (OsmandApplication) activity.getApplication();
		TargetPointsHelper targetPointsHelper = ctx.getTargetPointsHelper();
		if (!targetPointsHelper.getIntermediatePoints().isEmpty()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle(R.string.new_directions_point_dialog);
			builder.setItems(
					new String[] {
							activity.getString(R.string.keep_intermediate_points),
							activity.getString(R.string.clear_intermediate_points)
					},
					(dialog, which) -> {
						if (which == 1) {
							targetPointsHelper.clearPointToNavigate(false);
						}
						ctx.getSettings().navigateDialog();
						targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
						MapActivity.launchMapActivityMoveToTop(activity);
					});
			builder.show();
		} else {
			ctx.getSettings().navigateDialog();
			targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
			MapActivity.launchMapActivityMoveToTop(activity);
		}
	}

	public static void addWaypointDialogAndLaunchMap(AppCompatActivity act, double lat, double lon, PointDescription name) {
		TargetPointsHelper targetPointsHelper = ((OsmandApplication) act.getApplication()).getTargetPointsHelper();
		if (targetPointsHelper.getPointToNavigate() != null) {
			AddWaypointBottomSheetDialogFragment.showInstance(act, lat, lon, name);
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
