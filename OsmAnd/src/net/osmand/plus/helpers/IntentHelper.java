package net.osmand.plus.helpers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import net.osmand.AndroidNetworkUtils;
import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginsFragment;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment;
import net.osmand.plus.openplacereviews.OPRConstants;
import net.osmand.plus.openplacereviews.OprAuthHelper.OprAuthorizationListener;
import net.osmand.plus.search.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.track.TrackMenuFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.osmand.plus.activities.TrackActivity.CURRENT_RECORDING;
import static net.osmand.plus.activities.TrackActivity.TRACK_FILE_NAME;
import static net.osmand.plus.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import static net.osmand.plus.track.TrackMenuFragment.RETURN_SCREEN_NAME;

public class IntentHelper {

	private static final Log LOG = PlatformUtil.getLog(IntentHelper.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapActivity mapActivity;

	public IntentHelper(MapActivity mapActivity, OsmandApplication app) {
		this.app = app;
		this.mapActivity = mapActivity;
		this.settings = app.getSettings();
	}

	public boolean parseLaunchIntents() {
		boolean applied = parseLocationIntent();
		if (!applied) {
			applied = parseTileSourceIntent();
		}
		if (!applied) {
			applied = parseOpenGpxIntent();
		}
		if (!applied) {
			applied = parseSendIntent();
		}
		if (!applied) {
			applied = parseOAuthIntent();
		}
		if (!applied) {
			applied = parseOprOAuthIntent();
		}
		return applied;
	}

	private boolean parseLocationIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && intent.getData() != null) {
			Uri data = intent.getData();
			if (("http".equalsIgnoreCase(data.getScheme()) || "https".equalsIgnoreCase(data.getScheme()))
					&& data.getHost() != null && data.getHost().contains("osmand.net") &&
					data.getPath() != null && data.getPath().startsWith("/go")) {
				String lat = data.getQueryParameter("lat");
				String lon = data.getQueryParameter("lon");
				String url = data.getQueryParameter("url");
				if (lat != null && lon != null) {
					try {
						double lt = Double.parseDouble(lat);
						double ln = Double.parseDouble(lon);
						String zoom = data.getQueryParameter("z");
						int z = settings.getLastKnownMapZoom();
						if (zoom != null) {
							z = (int) Double.parseDouble(zoom);
						}
						settings.setMapLocationToShow(lt, ln, z, new PointDescription(lt, ln));
					} catch (NumberFormatException e) {
						LOG.error("error", e);
					}
				} else if (url != null) {
					url = DiscountHelper.parseUrl(app, url);
					if (DiscountHelper.validateUrl(app, url)) {
						DiscountHelper.openUrl(mapActivity, url);
					}
				}
				mapActivity.setIntent(null);
				return true;
			}
		}
		return false;
	}

	private boolean parseTileSourceIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && intent.getData() != null) {
			Uri data = intent.getData();
			if (("http".equalsIgnoreCase(data.getScheme()) || "https".equalsIgnoreCase(data.getScheme()))
					&& data.getHost() != null && data.getHost().contains("osmand.net") &&
					data.getPath() != null && data.getPath().startsWith("/add-tile-source")) {
				Map<String, String> attrs = new HashMap<>();
				for (String name : data.getQueryParameterNames()) {
					String value = data.getQueryParameter(name);
					if (value != null) {
						attrs.put(name, value);
					}
				}
				if (!attrs.isEmpty()) {
					try {
						TileSourceManager.TileSourceTemplate r = TileSourceManager.createTileSourceTemplate(attrs);
						if (r != null) {
							EditMapSourceDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), r);
						}
					} catch (Exception e) {
						LOG.error("parseAddTileSourceIntent error", e);
					}
				}
				mapActivity.setIntent(null);
				return true;
			}
		}
		return false;
	}

	private boolean parseOpenGpxIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && intent.getData() != null) {
			Uri data = intent.getData();
			if (("http".equalsIgnoreCase(data.getScheme()) || "https".equalsIgnoreCase(data.getScheme()))
					&& data.getHost() != null && data.getHost().contains("osmand.net")
					&& data.getPath() != null && data.getPath().startsWith("/open-gpx")) {
				String url = data.getQueryParameter("url");
				if (Algorithms.isEmpty(url)) {
					return false;
				}
				String name = data.getQueryParameter("name");
				if (Algorithms.isEmpty(name)) {
					name = Algorithms.getFileWithoutDirs(url);
				}
				if (!name.endsWith(IndexConstants.GPX_FILE_EXT)) {
					name += IndexConstants.GPX_FILE_EXT;
				}
				final String fileName = name;
				AndroidNetworkUtils.downloadFileAsync(url, app.getAppPath(IndexConstants.GPX_IMPORT_DIR + fileName),
						new CallbackWithObject<String>() {
							@Override
							public boolean processResult(String error) {
								if (error == null) {
									String downloaded = app.getString(R.string.shared_string_download_successful);
									app.showShortToastMessage(app.getString(R.string.ltr_or_rtl_combine_via_colon, downloaded, fileName));
								} else {
									app.showShortToastMessage(app.getString(R.string.error_occurred_loading_gpx));
								}
								return true;
							}
						});

				mapActivity.setIntent(null);
				return true;
			}
		}
		return false;
	}

	public void parseContentIntent() {
		final Intent intent = mapActivity.getIntent();
		if (intent != null) {
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				final Uri data = intent.getData();
				if (data != null) {
					final String scheme = data.getScheme();
					if ("file".equals(scheme)) {
						final String path = data.getPath();
						if (path != null) {
							mapActivity.getImportHelper().handleFileImport(data, new File(path).getName(), intent.getExtras(), true);
						}
						clearIntent(intent);
					} else if ("content".equals(scheme)) {
						mapActivity.getImportHelper().handleContentImport(data, intent.getExtras(), true);
						clearIntent(intent);
					} else if ("google.navigation".equals(scheme) || "osmand.navigation".equals(scheme)) {
						parseNavigationIntent(intent);
					} else if ("osmand.api".equals(scheme)) {
						ExternalApiHelper apiHelper = new ExternalApiHelper(mapActivity);
						Intent result = apiHelper.processApiRequest(intent);
						mapActivity.setResult(apiHelper.getResultCode(), result);
						result.setAction(null);
						mapActivity.setIntent(result);
						if (apiHelper.needFinish()) {
							mapActivity.finish();
						}
					}
				}
			}

			if (intent.hasExtra(MapMarkersDialogFragment.OPEN_MAP_MARKERS_GROUPS)) {
				Bundle openMapMarkersGroupsExtra = intent.getBundleExtra(MapMarkersDialogFragment.OPEN_MAP_MARKERS_GROUPS);
				if (openMapMarkersGroupsExtra != null) {
					MapMarkersDialogFragment.showInstance(mapActivity, openMapMarkersGroupsExtra.getString(MapMarkersGroup.MARKERS_SYNC_GROUP_ID));
				}
				mapActivity.setIntent(null);
			}
			if (intent.hasExtra(BaseSettingsFragment.OPEN_SETTINGS)) {
				String appMode = intent.getStringExtra(BaseSettingsFragment.APP_MODE_KEY);
				String settingsTypeName = intent.getStringExtra(BaseSettingsFragment.OPEN_SETTINGS);
				if (!Algorithms.isEmpty(settingsTypeName)) {
					try {
						SettingsScreenType screenType = SettingsScreenType.valueOf(settingsTypeName);
						BaseSettingsFragment.showInstance(mapActivity, screenType, ApplicationMode.valueOfStringKey(appMode, null));
					} catch (IllegalArgumentException e) {
						LOG.error("error", e);
					}
				}
				mapActivity.setIntent(null);
			}
			if (intent.hasExtra(PluginsFragment.OPEN_PLUGINS)) {
				boolean openPlugins = intent.getBooleanExtra(PluginsFragment.OPEN_PLUGINS, false);
				if (openPlugins) {
					PluginsFragment.showInstance(mapActivity.getSupportFragmentManager());
				}
				mapActivity.setIntent(null);
			}
			if (intent.hasExtra(BaseSettingsFragment.OPEN_CONFIG_ON_MAP)) {
				switch (intent.getStringExtra(BaseSettingsFragment.OPEN_CONFIG_ON_MAP)) {
					case BaseSettingsFragment.MAP_CONFIG:
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP, null);
						break;

					case BaseSettingsFragment.SCREEN_CONFIG:
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_SCREEN, null);
						break;
				}
				mapActivity.setIntent(null);
			}
			if (intent.hasExtra(TrackMenuFragment.OPEN_TRACK_MENU)) {
				String path = intent.getStringExtra(TRACK_FILE_NAME);
				String name = intent.getStringExtra(RETURN_SCREEN_NAME);
				boolean currentRecording = intent.getBooleanExtra(CURRENT_RECORDING, false);
				TrackMenuFragment.showInstance(mapActivity, path, currentRecording, name, null);
				mapActivity.setIntent(null);
			}
		}
	}

	private void parseNavigationIntent(Intent intent) {
		Uri data = intent.getData();
		if (data != null) {
			final String schemeSpecificPart = data.getSchemeSpecificPart();

			final Matcher matcher = Pattern.compile("(?:q|ll)=([\\-0-9.]+),([\\-0-9.]+)(?:.*)").matcher(schemeSpecificPart);
			if (matcher.matches()) {
				try {
					double lat = Double.parseDouble(matcher.group(1));
					double lon = Double.parseDouble(matcher.group(2));

					app.getTargetPointsHelper().navigateToPoint(new LatLon(lat, lon), false, -1);
					mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, false, true);
				} catch (NumberFormatException e) {
					app.showToastMessage(app.getString(R.string.navigation_intent_invalid, schemeSpecificPart));
				}
			} else {
				app.showToastMessage(app.getString(R.string.navigation_intent_invalid, schemeSpecificPart));
			}
			clearIntent(intent);
		}
	}

	private void clearIntent(Intent intent) {
		intent.setAction(null);
		intent.setData(null);
	}

	private boolean parseSendIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null) {
			String action = intent.getAction();
			String type = intent.getType();
			if (Intent.ACTION_SEND.equals(action) && type != null) {
				if ("text/plain".equals(type)) {
					return handleSendText(intent);
				}
			}
		}
		return false;
	}

	private boolean parseOAuthIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && intent.getData() != null) {
			Uri uri = intent.getData();
			if (uri.toString().startsWith("osmand-oauth")) {
				String oauthVerifier = uri.getQueryParameter("oauth_verifier");
				app.getOsmOAuthHelper().addListener(getOnAuthorizeListener());
				app.getOsmOAuthHelper().authorize(oauthVerifier);
				mapActivity.setIntent(null);
				return true;
			}
		}
		return false;
	}

	private boolean parseOprOAuthIntent() {
		Intent intent = mapActivity.getIntent();
		if (intent != null && intent.getData() != null) {
			Uri uri = intent.getData();
			if (uri.toString().startsWith(OPRConstants.OPR_OAUTH_PREFIX)) {
				String token = uri.getQueryParameter("opr-token");
				String username = uri.getQueryParameter("opr-nickname");
				app.getOprAuthHelper().addListener(getOprAuthorizationListener());
				app.getOprAuthHelper().authorize(token, username);
				mapActivity.setIntent(null);
				return true;
			}
		}
		return false;
	}

	private OsmAuthorizationListener getOnAuthorizeListener() {
		return new OsmAuthorizationListener() {
			@Override
			public void authorizationCompleted() {
				for (Fragment fragment : mapActivity.getSupportFragmentManager().getFragments()) {
					if (fragment instanceof OsmAuthorizationListener) {
						((OsmAuthorizationListener) fragment).authorizationCompleted();
					}
				}
			}
		};
	}

	private OprAuthorizationListener getOprAuthorizationListener() {
		return new OprAuthorizationListener() {
			@Override
			public void authorizationCompleted() {
				for (Fragment fragment : mapActivity.getSupportFragmentManager().getFragments()) {
					if (fragment instanceof OprAuthorizationListener) {
						((OprAuthorizationListener) fragment).authorizationCompleted();
					}
				}
			}
		};
	}

	private boolean handleSendText(Intent intent) {
		String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
		if (!Algorithms.isEmpty(sharedText)) {
			return QuickSearchDialogFragment.showInstance(
					mapActivity,
					sharedText,
					null,
					QuickSearchDialogFragment.QuickSearchType.REGULAR,
					QuickSearchDialogFragment.QuickSearchTab.CATEGORIES,
					null
			);
		}
		return false;
	}
}
