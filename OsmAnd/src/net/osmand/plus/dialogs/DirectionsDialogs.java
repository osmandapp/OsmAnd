package net.osmand.plus.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
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
			Builder builder = new AlertDialog.Builder(act);
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
	
	public static void createDirectionsActionsPopUpMenu(final PopupMenu optionsMenu , final LatLon location, final Object obj, final PointDescription name,
											   final int z, final Activity activity, final boolean saveHistory) {
		createDirectionActionsPopUpMenu(optionsMenu, location, obj, name, z, activity, saveHistory, true);
	}


	public static void createDirectionActionsPopUpMenu(final PopupMenu optionsMenu, final LatLon location, final Object obj, final PointDescription name,
															final int z, final Activity activity, final boolean saveHistory, boolean favorite) {
		setupPopUpMenuIcon(optionsMenu);
		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		IconsCache iconsCache = app.getIconsCache();

		final TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		MenuItem item = optionsMenu.getMenu().add(
				R.string.context_menu_item_directions_to).setIcon(iconsCache.getContentIcon((R.drawable.ic_action_gdirections_dark)));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				DirectionsDialogs.directionsToDialogAndLaunchMap(activity, location.getLatitude(), location.getLongitude(), name);
				optionsMenu.dismiss();
				return true;
			}
		});

		if (targetPointsHelper.getPointToNavigate() != null) {
			item = optionsMenu.getMenu().add(
					R.string.context_menu_item_intermediate_point).setIcon(
					iconsCache.getContentIcon(R.drawable.ic_action_flage_dark));
		} else {
			item = optionsMenu.getMenu().add(
					R.string.context_menu_item_destination_point).setIcon(
					iconsCache.getContentIcon(R.drawable.ic_action_flag_dark));
		}
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				DirectionsDialogs.addWaypointDialogAndLaunchMap(activity, location.getLatitude(), location.getLongitude(), name);
				optionsMenu.dismiss();
				return true;
			}
		});
		item = optionsMenu.getMenu().add(
				R.string.shared_string_show_on_map).setIcon(iconsCache.getContentIcon(R.drawable.ic_action_marker_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				app.getSettings().setMapLocationToShow(location.getLatitude(), location.getLongitude(), z, name, saveHistory,
						obj); //$NON-NLS-1$
				MapActivity.launchMapActivityMoveToTop(activity);
				return true;
			}
		});
		if (favorite) {
			item = optionsMenu.getMenu().add(
					R.string.shared_string_add_to_favorites).setIcon(iconsCache.getContentIcon(R.drawable.ic_action_fav_dark));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					Bundle args = new Bundle();
					Dialog dlg = FavoriteDialogs.createAddFavouriteDialog(activity, args);
					dlg.show();
					FavoriteDialogs.prepareAddFavouriteDialog(activity, dlg, args, location.getLatitude(), location.getLongitude(),
							name);
					return true;
				}
			});
		}
	}
	
	public static void addWaypointDialogAndLaunchMap(final Activity act, final double lat, final double lon, final PointDescription name) {
		final OsmandApplication ctx = (OsmandApplication) act.getApplication();
		final TargetPointsHelper targetPointsHelper = ctx.getTargetPointsHelper();
		if (targetPointsHelper.getPointToNavigate() != null) {
			Builder builder = new AlertDialog.Builder(act);
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
							} else if (which == 1) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, 
										targetPointsHelper.getIntermediatePoints().size() + 1, name);
							} else if (which == 2) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, 0, name);
							} else {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, targetPointsHelper.getIntermediatePoints().size(), name);
							}
							MapActivity.launchMapActivityMoveToTop(act);
						}
					});
			builder.show();
		} else {
			targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
			MapActivity.launchMapActivityMoveToTop(act);
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
