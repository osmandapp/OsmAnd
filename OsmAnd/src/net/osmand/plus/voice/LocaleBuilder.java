package net.osmand.plus.voice;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.util.Locale;

import androidx.annotation.NonNull;

public class LocaleBuilder {

	private static final Log log = PlatformUtil.getLog(LocaleBuilder.class);

	private static final String LANG_PORTUGUESE = "pt";
	private static final String LANG_DUTCH = "nl";

	private static final String REGION_PORTUGAL = "PT";
	private static final String REGION_BELGIUM = "BE";

	private static final String WORLD_REGION_ID_BELGIUM = "europe_belgium";

	private final OsmandApplication app;
	private final String localeParams;
	private final CallbackWithObject<Locale> callback;

	public static void buildLocale(@NonNull OsmandApplication app,
	                               @NonNull String localeParams,
	                               @NonNull CallbackWithObject<Locale> callback) {
		new LocaleBuilder(app, localeParams, callback);
	}

	private LocaleBuilder(@NonNull OsmandApplication app,
	                      @NonNull String localeParams,
	                      @NonNull CallbackWithObject<Locale> callback) {
		this.app = app;
		this.localeParams = localeParams;
		this.callback = callback;
		buildLocale();
	}

	private void buildLocale() {
		final String[] params = (localeParams + "____.").split("[\\_\\-]");
		String language = params[0];
		                        // As per BCP 47:
		String region = "";     // [a-zA-Z]{2} | [0-9]{3}
		String variant = "";    // [0-9][0-9a-zA-Z]{3} | [0-9a-zA-Z]{5,8}
		String script = "";     // [a-zA-Z]{4}
		for (int i = 3; i > 0; i--) {
			String param = params[i];
			if (param.matches("[a-zA-Z]{4}")) {
				script = param;
			} else if (param.length() >= 4) {
				variant = param;
			} else {
				region = param;
			}
		}

		String finalVariant = variant;
		String finalScript = script;
		defineRegion(region, properRegion -> {
			Locale locale = buildLocale(language, properRegion, finalVariant, finalScript);
			callback.processResult(locale);
			return true;
		});
	}

	private void defineRegion(@NonNull String regionFromParams,
	                          @NonNull CallbackWithObject<String> regionCallback) {
		boolean forcePortuguesePronunciation = localeParams.equals(LANG_PORTUGUESE); // Fix #10232
		Location lastKnownLocation = app.getLocationProvider().getLastKnownLocation();
		boolean detectPronunciationForNl = localeParams.equals(LANG_DUTCH) && lastKnownLocation != null;
		if (forcePortuguesePronunciation) {
			regionCallback.processResult(REGION_PORTUGAL);
		} else if (detectPronunciationForNl) {
			LatLon latLon = new LatLon(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
			app.getMapViewTrackingUtilities().detectCurrentRegion(latLon, worldRegion -> {
				String region = getSpecialOrDefaultRegion(worldRegion, WORLD_REGION_ID_BELGIUM,
						REGION_BELGIUM, regionFromParams);
				regionCallback.processResult(region);
				return true;
			});
		} else {
			regionCallback.processResult(regionFromParams);
		}
	}

	@NonNull
	private String getSpecialOrDefaultRegion(@NonNull WorldRegion worldRegion, @NonNull String specialRegionId,
	                                         @NonNull String specialRegion, @NonNull String defaultRegion) {
		return hasRegionWithId(worldRegion, specialRegionId) ? specialRegion : defaultRegion;
	}

	private boolean hasRegionWithId(@NonNull WorldRegion worldRegion, @NonNull String regionId) {
		while (worldRegion != null && !regionId.equals(worldRegion.getRegionId())) {
			worldRegion = worldRegion.getSuperregion();
		}
		return worldRegion != null && regionId.equals(worldRegion.getRegionId());
	}

	@NonNull
	private Locale buildLocale(@NonNull String language, @NonNull String region,
	                           @NonNull String variant, @NonNull String script) {
		try {
			return new Locale.Builder()
					.setLanguage(language)
					.setRegion(region)
					.setVariant(variant)
					.setScript(script)
					.build();
		} catch (RuntimeException e) {
			log.error("Trying to build locale with ill-formed param", e);
			return new Locale(language, region); // Variant not passed to prevent errors on some devices
		}
	}
}