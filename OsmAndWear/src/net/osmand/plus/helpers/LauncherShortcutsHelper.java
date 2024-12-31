package net.osmand.plus.helpers;

import static net.osmand.data.SpecialPointType.HOME;
import static net.osmand.data.SpecialPointType.WORK;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.MapActionsHelper;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LauncherShortcutsHelper {

	private static final Log LOG = PlatformUtil.getLog(LauncherShortcutsHelper.class);

	public static final String INTENT_SCHEME = "osmand.shortcuts";

	private static final int VISIBLE_DYNAMIC_SHORTCUTS_LIMIT = 4;
	@ColorRes
	private static final int SHORTCUT_ICON_COLOR_RES = R.color.active_color_primary_light;

	private final OsmandApplication app;
	private final FavouritesHelper favoritesHelper;
	private final FavoritesListener favoritesListener;

	public LauncherShortcutsHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.favoritesHelper = app.getFavoritesHelper();
		this.favoritesListener = new FavoritesListener() {
			@Override
			public void onFavoritesLoaded() {
				updateLauncherShortcuts();
			}
		};
		favoritesHelper.addListener(favoritesListener);
	}

	public void updateLauncherShortcuts() {
		removeNotNeededShortcuts();
		setNeededShortcutsInOrder();
	}

	private void removeNotNeededShortcuts() {
		for (Shortcut shortcut : Shortcut.values()) {
			if (shortcut.shouldRemove(app)) {
				ShortcutManagerCompat.removeDynamicShortcuts(app, Collections.singletonList(shortcut.id));
			}
		}
	}

	private void setNeededShortcutsInOrder() {
		try {
			List<ShortcutInfoCompat> shortcutInfoList = new ArrayList<>();
			int counter = 0;

			for (Shortcut shortcut : Shortcut.values()) {

				boolean noMoreShortcutsAllowed = counter == VISIBLE_DYNAMIC_SHORTCUTS_LIMIT
						|| counter == ShortcutManagerCompat.getMaxShortcutCountPerActivity(app);
				if (noMoreShortcutsAllowed) {
					break;
				}

				if ((shortcut.shouldPublish(app) || shortcut.isPublished(app)) && !shortcut.isPinned(app)) {
					ShortcutInfoCompat orderedShortcutInfo = new ShortcutInfoCompat.Builder(app, shortcut.id)
							.setShortLabel(app.getString(shortcut.labelId))
							.setIcon(getIcon(shortcut.iconId))
							.setIntent(createIntent(shortcut.id))
							.setRank(counter++)
							.build();
					shortcutInfoList.add(orderedShortcutInfo);
				}
			}
			ShortcutManagerCompat.removeAllDynamicShortcuts(app);
			ShortcutManagerCompat.setDynamicShortcuts(app, shortcutInfoList);
		} catch (IllegalArgumentException | IllegalStateException e) {
			LOG.error("Failed to update launcher shortcuts", e);
		}
	}

	private IconCompat getIcon(@DrawableRes int iconId) {
		Drawable drawable = app.getUIUtilities().getIcon(iconId, SHORTCUT_ICON_COLOR_RES);
		Bitmap bitmap = AndroidUtils.drawableToBitmap(drawable);
		return IconCompat.createWithBitmap(bitmap);
	}

	private Intent createIntent(String shortcutId) {
		String uriString = INTENT_SCHEME + "://shortcut?id=" + shortcutId;
		ComponentName component = new ComponentName(app, MapActivity.class);
		return new Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).setComponent(component);
	}

	public void parseIntent(@NonNull MapActivity activity, @NonNull Intent intent) {
		Uri uri = intent.getData();
		if (uri == null) {
			return;
		}

		String id = uri.getQueryParameter("id");
		if (Shortcut.START_RECORDING.id.equals(id)) {
			OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
			if (plugin != null) {
				plugin.askStartRecording(activity);
			}
		} else if (Shortcut.SEARCH.id.equals(id)) {
			activity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
		} else if (Shortcut.MY_PLACES.id.equals(id)) {
			Intent activityIntent = new Intent(activity, app.getAppCustomization().getMyPlacesActivity());
			activity.startActivity(activityIntent);
		} else if (Shortcut.NAVIGATE_TO.id.equals(id)) {
			navigateTo(activity, null);
		} else if (Shortcut.NAVIGATE_TO_HOME.id.equals(id) || Shortcut.NAVIGATE_TO_WORK.id.equals(id)) {
			if (app.isApplicationInitializing()) {
				app.getAppInitializer().addListener(new AppInitializeListener() {
					@Override
					public void onFinish(@NonNull AppInitializer init) {
						init.removeListener(this);
						navigateToPoint(activity, id);
					}
				});
			} else {
				navigateToPoint(activity, id);
			}
		}
	}

	private void navigateToPoint(@NonNull MapActivity activity, @NonNull String id) {
		FavouritePoint point = null;
		if (Shortcut.NAVIGATE_TO_HOME.id.equals(id)) {
			point = favoritesHelper.getSpecialPoint(HOME);
		} else if (Shortcut.NAVIGATE_TO_WORK.id.equals(id)) {
			point = favoritesHelper.getSpecialPoint(WORK);
		}
		if (point != null) {
			navigateTo(activity, point);
		}
	}

	private void navigateTo(@NonNull MapActivity mapActivity, @Nullable FavouritePoint point) {
		if (point == null) {
			MapActionsHelper actionsHelper = mapActivity.getMapLayers().getMapActionsHelper();
			if (actionsHelper != null) {
				actionsHelper.doRoute();
			}
		} else {
			PointDescription description = point.getPointDescription(app);
			LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
			ApplicationMode appMode = ExternalApiHelper.getNavigationProfile(app);

			RoutingHelper routingHelper = app.getRoutingHelper();
			if (routingHelper.isFollowingMode()) {
				WeakReference<MapActivity> activityRef = new WeakReference<>(mapActivity);
				mapActivity.getMapActions().stopNavigationActionConfirm(dialog -> {
					MapActivity activity = activityRef.get();
					if (activity != null && !routingHelper.isFollowingMode() && appMode != null) {
						NavigateGpxHelper.startNavigation(activity, appMode, null, null, latLon, description, true);
					}
				});
			} else if (appMode != null) {
				NavigateGpxHelper.startNavigation(mapActivity, appMode, null, null, latLon, description, true);
			}
		}
	}

	private enum Shortcut {

		NAVIGATE_TO_HOME("navigate_to_home", R.string.shortcut_navigate_to_home, R.drawable.ic_action_home_dark),
		NAVIGATE_TO_WORK("navigate_to_work", R.string.shortcut_navigate_to_work, R.drawable.ic_action_work),
		START_RECORDING("start_recording", R.string.shortcut_start_recording, R.drawable.ic_action_route_distance),
		SEARCH("search", R.string.shared_string_search, R.drawable.ic_action_search_dark),
		MY_PLACES("my_places", R.string.shared_string_my_places, R.drawable.ic_action_folder_favorites),
		NAVIGATE_TO("navigate_to", R.string.shortcut_navigate_to, R.drawable.ic_action_gdirections_dark);

		private final String id;
		@StringRes
		private final int labelId;
		@DrawableRes
		private final int iconId;

		Shortcut(String id, @StringRes int labelId, @DrawableRes int iconId) {
			this.id = id;
			this.labelId = labelId;
			this.iconId = iconId;
		}

		public boolean isPublished(@NonNull OsmandApplication app) {
			return getShortcutInfo(app) != null;
		}

		@SuppressLint("WrongConstant")
		public boolean isPinned(@NonNull OsmandApplication app) throws IllegalStateException {
			List<ShortcutInfoCompat> pinned = ShortcutManagerCompat.getShortcuts(app, ShortcutManagerCompat.FLAG_MATCH_PINNED);
			for (ShortcutInfoCompat shortcutInfo : pinned) {
				if (id.equals(shortcutInfo.getId())) {
					return true;
				}
			}
			return false;
		}

		public boolean shouldPublish(@NonNull OsmandApplication app) {
			boolean missing = !isPublished(app);
			if (this == START_RECORDING) {
				return missing && PluginsHelper.isActive(OsmandMonitoringPlugin.class);
			} else if (this == NAVIGATE_TO_HOME) {
				boolean hasHome = app.getFavoritesHelper().getSpecialPoint(HOME) != null;
				return missing && hasHome;
			} else if (this == NAVIGATE_TO_WORK) {
				boolean hasWork = app.getFavoritesHelper().getSpecialPoint(WORK) != null;
				return missing && hasWork;
			}
			return missing;
		}

		public boolean shouldRemove(@NonNull OsmandApplication app) {
			if (!isPublished(app)) {
				return false;
			} else if (this == START_RECORDING && !PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
				return true;
			} else if (this == NAVIGATE_TO_HOME || this == NAVIGATE_TO_WORK) {
				SpecialPointType type = this == NAVIGATE_TO_HOME ? HOME : WORK;
				return app.getFavoritesHelper().getSpecialPoint(type) == null;
			}
			return false;
		}

		@Nullable
		private ShortcutInfoCompat getShortcutInfo(@NonNull OsmandApplication app) {
			for (ShortcutInfoCompat info : ShortcutManagerCompat.getDynamicShortcuts(app)) {
				if (id.equals(info.getId())) {
					return info;
				}
			}
			return null;
		}
	}
}