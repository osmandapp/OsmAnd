package net.osmand.plus.inapp;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.views.mapwidgets.WidgetType;

import java.util.Calendar;

public class InAppPurchaseUtils {

	public static final int HMD_PROMO_YEARS = 3;
	public static final int HUGEROCK_PROMO_MONTHS = 6;
	public static final int TRIPLTEK_PROMO_MONTHS = 12;
	private static final long ANDROID_AUTO_START_DATE_MS = 10L * 1000L * 60L * 60L * 24L; // 10 days

	public static boolean isFullVersionPurchased(@NonNull OsmandApplication app) {
		return app.getSettings().FULL_VERSION_PURCHASED.get();
	}

	public static boolean isLiveUpdatesPurchased(@NonNull OsmandApplication app) {
		return app.getSettings().LIVE_UPDATES_PURCHASED.get();
	}

	public static boolean isMapsPlusPurchased(@NonNull OsmandApplication app) {
		return app.getSettings().OSMAND_MAPS_PURCHASED.get();
	}

	public static boolean isOsmAndProPurchased(@NonNull OsmandApplication app) {
		return app.getSettings().OSMAND_PRO_PURCHASED.get();
	}

	public static boolean isContourLinesPurchased(@NonNull OsmandApplication app) {
		return app.getSettings().CONTOUR_LINES_PURCHASED.get();
	}

	public static boolean isDepthContoursPurchased(@NonNull OsmandApplication app) {
		return app.getSettings().DEPTH_CONTOURS_PURCHASED.get();
	}

	public static boolean isPromoSubscribed(@NonNull OsmandApplication app) {
		return app.getSettings().BACKUP_PURCHASE_ACTIVE.get();
	}

	public static boolean isMapperUpdatesSubscribed(@NonNull OsmandApplication app) {
		return app.getSettings().MAPPER_LIVE_UPDATES_EXPIRE_TIME.get() > System.currentTimeMillis();
	}


	public static boolean isFullVersionAvailable(@NonNull OsmandApplication app) {
		return isFullVersionAvailable(app, true);
	}

	public static boolean isFullVersionAvailable(@NonNull OsmandApplication app, boolean checkDevBuild) {
		return isFullVersionPurchased(app) || checkDeveloperBuildIfNeeded(app, checkDevBuild);
	}

	public static boolean isMapsPlusAvailable(@NonNull OsmandApplication app) {
		return isMapsPlusAvailable(app, true);
	}

	public static boolean isMapsPlusAvailable(@NonNull OsmandApplication app, boolean checkDevBuild) {
		return isMapsPlusPurchased(app) || checkDeveloperBuildIfNeeded(app, checkDevBuild);
	}

	public static boolean isOsmAndProAvailable(@NonNull OsmandApplication app) {
		return isOsmAndProAvailable(app, true);
	}

	public static boolean isOsmAndProAvailable(@NonNull OsmandApplication app, boolean checkDevBuild) {
		return isOsmAndProPurchased(app) || isPromoSubscribed(app) || checkDeveloperBuildIfNeeded(app, checkDevBuild);
	}

	private static boolean checkDeveloperBuildIfNeeded(@NonNull OsmandApplication app, boolean shouldCheck) {
		return shouldCheck && Version.isDeveloperBuild(app);
	}

	public static boolean isSubscribedToAny(@NonNull OsmandApplication app) {
		return isSubscribedToAny(app, true);
	}

	public static boolean isSubscribedToAny(@NonNull OsmandApplication app, boolean checkDevBuild) {
		return checkDeveloperBuildIfNeeded(app, checkDevBuild)
				|| isMapsPlusAvailable(app, checkDevBuild)
				|| isOsmAndProAvailable(app, checkDevBuild)
				|| isMapperUpdatesSubscribed(app)
				|| isLiveUpdatesPurchased(app)
				|| isBrandPromoAvailable(app);
	}

	public static boolean isLiveUpdatesAvailable(@NonNull OsmandApplication app) {
		return isLiveUpdatesPurchased(app)
				|| isOsmAndProAvailable(app)
				|| isMapperUpdatesSubscribed(app)
				|| checkDeveloperBuildIfNeeded(app, true)
				|| isBrandPromoAvailable(app);
	}

	public static boolean isWidgetPurchased(@NonNull OsmandApplication app, @NonNull WidgetType wt) {
		if (wt.isProWidget()) {
			return wt.isOBDWidget() ? isVehicleMetricsAvailable(app) : isProWidgetsAvailable(app);
		}
		return true;
	}

	public static boolean isVehicleMetricsAvailable(@NonNull OsmandApplication app) {
		return isOsmAndProAvailable(app);
	}

	public static boolean isProWidgetsAvailable(@NonNull OsmandApplication app) {
		return isOsmAndProAvailable(app) || isBrandPromoAvailable(app);
	}

	public static boolean is3dMapsAvailable(@NonNull OsmandApplication app) {
		return isOsmAndProAvailable(app) || isBrandPromoAvailable(app);
	}

	public static boolean isExportTypeAvailable(@NonNull OsmandApplication app,
	                                            @NonNull ExportType exportType) {
		return isBackupAvailable(app) || exportType.isAvailableInFreeVersion();
	}

	public static boolean isBackupAvailable(@NonNull OsmandApplication app) {
		return isOsmAndProAvailable(app);
	}

	public static boolean isWeatherAvailable(@NonNull OsmandApplication app) {
		return isOsmAndProAvailable(app) || isBrandPromoAvailable(app);
	}

	public static boolean isColoringTypeAvailable(@NonNull OsmandApplication app) {
		return isOsmAndProAvailable(app) || isBrandPromoAvailable(app);
	}

	public static boolean isBrandPromoAvailable(@NonNull OsmandApplication app) {
		return isTripltekPromoAvailable(app) || isHugerockPromoAvailable(app) || isHMDPromoAvailable(app);
	}

	public static boolean isDepthContoursAvailable(@NonNull OsmandApplication app) {
		return isDepthContoursPurchased(app) || Version.isPaidVersion(app) ||
				checkDeveloperBuildIfNeeded(app, true);
	}

	public static boolean isContourLinesAvailable(@NonNull OsmandApplication app) {
		return isContourLinesPurchased(app) || Version.isPaidVersion(app) ||
				checkDeveloperBuildIfNeeded(app, true);
	}

	public static boolean isGridColorAvailable(@NonNull OsmandApplication app) {
		return isSubscribedToAny(app);
	}

	public static boolean isAndroidAutoAvailable(@NonNull OsmandApplication app) {
		long time = System.currentTimeMillis();
		long installTime = Math.max(Version.getUpdateTime(app), Version.getInstallTime(app));
		if (time >= installTime + ANDROID_AUTO_START_DATE_MS) {
			return checkDeveloperBuildIfNeeded(app, true) || Version.isPaidVersion(app);
		}
		return true;
	}

	public static boolean isTripltekPromoAvailable(@NonNull OsmandApplication app) {
		if (Version.isTripltekBuild()) {
			long expirationTime = getTripltekPromoExpirationTime(app);
			return expirationTime >= System.currentTimeMillis();
		}
		return false;
	}

	public static long getTripltekPromoExpirationTime(@NonNull OsmandApplication app) {
		if (Version.isTripltekBuild()) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(Version.getInstallTime(app));
			calendar.add(Calendar.MONTH, TRIPLTEK_PROMO_MONTHS);

			return calendar.getTimeInMillis();
		}
		return 0;
	}

	public static boolean isHugerockPromoAvailable(@NonNull OsmandApplication app) {
		if (Version.isHugerockBuild()) {
			long expirationTime = getHugerockPromoExpirationTime(app);
			return expirationTime >= System.currentTimeMillis();
		}
		return false;
	}

	public static long getHugerockPromoExpirationTime(@NonNull OsmandApplication app) {
		if (Version.isHugerockBuild()) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(Version.getInstallTime(app));
			calendar.add(Calendar.MONTH, HUGEROCK_PROMO_MONTHS);

			return calendar.getTimeInMillis();
		}
		return 0;
	}

	public static boolean isHMDPromoAvailable(@NonNull OsmandApplication app) {
		if (Version.isHMDBuild()) {
			long expirationTime = getHMDPromoExpirationTime(app);
			return expirationTime >= System.currentTimeMillis();
		}
		return false;
	}

	public static long getHMDPromoExpirationTime(@NonNull OsmandApplication app) {
		if (Version.isHMDBuild()) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(Version.getInstallTime(app));
			calendar.add(Calendar.YEAR, HMD_PROMO_YEARS);

			return calendar.getTimeInMillis();
		}
		return 0;
	}
}