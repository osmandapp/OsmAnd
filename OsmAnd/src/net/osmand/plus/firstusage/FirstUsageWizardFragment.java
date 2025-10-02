package net.osmand.plus.firstusage;

import static net.osmand.plus.importfiles.ImportType.SETTINGS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.Location;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.*;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.ui.BackupAuthorizationFragment;
import net.osmand.plus.backup.ui.BackupCloudFragment;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.datastorage.DataStorageFragment.StorageSelectionListener;
import net.osmand.plus.settings.datastorage.DataStorageHelper;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.style.CustomClickableSpan;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class FirstUsageWizardFragment extends BaseFullScreenFragment implements OsmAndLocationListener,
		AppInitializeListener, DownloadEvents, StorageSelectionListener, FirstUsageActionsListener {

	public static final String TAG = FirstUsageWizardFragment.class.getSimpleName();

	public static final String FIRST_USAGE = "first_usage";
	public static final String SHOW_OSMAND_WELCOME_SCREEN = "show_osmand_welcome_screen";
	public static final int FIRST_USAGE_LOCATION_PERMISSION = 300;
	private static final int NO_MAP_ZOOM_LEVEL = 9;
	private static final int DOWNLOAD_MAP_ZOOM_LEVEL = 13;

	private DownloadIndexesThread downloadThread;
	private DownloadValidationManager validationManager;

	private View view;

	public WizardType wizardType = null;
	private final WizardType DEFAULT_WIZARD_TYPE = WizardType.SEARCH_LOCATION;
	private boolean searchLocationByIp;

	private Timer locationSearchTimer;
	private boolean waitForIndexes;
	public boolean deviceNightMode;

	private Location location;
	private WorldRegion mapDownloadRegion;
	private IndexItem mapIndexItem;
	private boolean mapDownloadCancelled;
	private static boolean wizardClosed;

	private DialogButton wizardButton;
	private AppCompatImageView wizardIcon;
	private TextView wizardTitle;
	private ProgressBar wizardProgressBar;
	private ProgressBar wizardProgressBarCircle;
	private FragmentActivity activity;

	public void setWizardType(WizardType wizardType, boolean updateWizardView) {
		this.wizardType = wizardType;
		if (updateWizardView && isAdded()) {
			updateWizardView();
			doWizardTypeTask();
			updateSkipButton();
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		downloadThread = app.getDownloadThread();
		validationManager = new DownloadValidationManager(app);
		if (wizardType == null) {
			wizardType = DEFAULT_WIZARD_TYPE;
		}
		setupNightMode();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		view = UiUtilities.getInflater(getContext(), deviceNightMode).inflate(R.layout.first_usage_wizard_fragment, container, false);
		activity = requireActivity();
		AndroidUtils.addStatusBarPadding21v(activity, view);
		wizardIcon = view.findViewById(R.id.wizard_icon);
		wizardTitle = view.findViewById(R.id.wizard_title);
		wizardProgressBar = view.findViewById(R.id.wizard_progress_bar);
		wizardButton = view.findViewById(R.id.wizard_action_button);
		wizardProgressBarCircle = view.findViewById(R.id.wizard_progress_bar_icon);

		if (!AndroidUiHelper.isOrientationPortrait(activity) && !AndroidUiHelper.isTablet(activity)) {
			TextView wizardDescription = view.findViewById(R.id.wizard_description);
			wizardDescription.setMinimumHeight(0);
			wizardDescription.setMinHeight(0);
		}

		setupLocationButton();
		setupActionButton();
		setupSkipButton();
		updateWizardView();
		updateTermsOfServiceView();

		return view;
	}

	private void updateWizardView() {
		switch (wizardType) {
			case SEARCH_LOCATION:
				AndroidUiHelper.updateVisibility(wizardProgressBar, false);
				AndroidUiHelper.updateVisibility(wizardProgressBarCircle, true);
				AndroidUiHelper.updateVisibility(wizardIcon, false);
				wizardButton.setEnabled(false);
				wizardButton.setButtonType(DialogButtonType.SECONDARY);
				wizardButton.setTitleId(R.string.shared_string_download);
				wizardTitle.setText(getString(R.string.search_location));
				break;
			case NO_INTERNET:
				wizardIcon.setImageDrawable(ContextCompat.getDrawable(app, R.drawable.ic_action_wifi_off));
				wizardIcon.getDrawable().setTint(ColorUtilities.getDefaultIconColor(app, deviceNightMode));
				AndroidUiHelper.updateVisibility(wizardProgressBar, false);
				AndroidUiHelper.updateVisibility(wizardProgressBarCircle, false);
				AndroidUiHelper.updateVisibility(wizardIcon, true);
				wizardButton.setEnabled(true);
				wizardButton.setButtonType(DialogButtonType.SECONDARY);
				wizardButton.setTitleId(R.string.try_again);
				wizardButton.setOnClickListener(view -> showAppropriateWizard(activity, true));
				wizardTitle.setText(getString(R.string.no_inet_connection));
				break;
			case NO_LOCATION:
				wizardIcon.setImageDrawable(ContextCompat.getDrawable(app, R.drawable.ic_action_location_off));
				wizardIcon.getDrawable().setTint(ColorUtilities.getDefaultIconColor(app, deviceNightMode));
				AndroidUiHelper.updateVisibility(wizardProgressBar, false);
				AndroidUiHelper.updateVisibility(wizardProgressBarCircle, false);
				AndroidUiHelper.updateVisibility(wizardIcon, true);
				wizardButton.setEnabled(true);
				wizardButton.setButtonType(DialogButtonType.SECONDARY);
				wizardButton.setTitleId(R.string.try_again);
				wizardButton.setOnClickListener(view -> findLocation(getActivity(), false, true));
				wizardTitle.setText(getString(R.string.location_not_found));

				break;
			case SEARCH_MAP:
				AndroidUiHelper.updateVisibility(wizardProgressBar, false);
				AndroidUiHelper.updateVisibility(wizardProgressBarCircle, true);
				AndroidUiHelper.updateVisibility(wizardIcon, false);
				wizardButton.setEnabled(false);
				wizardButton.setButtonType(DialogButtonType.SECONDARY);
				wizardButton.setTitleId(R.string.shared_string_download);
				wizardTitle.setText(getString(R.string.search_map));
				break;
			case MAP_FOUND:
				AndroidUiHelper.updateVisibility(wizardProgressBar, false);
				AndroidUiHelper.updateVisibility(wizardProgressBarCircle, false);
				AndroidUiHelper.updateVisibility(wizardIcon, true);
				wizardIcon.setImageDrawable(ContextCompat.getDrawable(app, R.drawable.ic_map));
				wizardIcon.getDrawable().setTint(app.getColor(R.color.icon_color_active_light));

				if (mapIndexItem != null) {
					wizardTitle.setText(mapIndexItem.getVisibleName(getContext(), app.getRegions(), false));
					wizardButton.setEnabled(true);
					wizardButton.setButtonType(DialogButtonType.PRIMARY);
					wizardButton.setTitle(getString(R.string.shared_string_download) + " " + mapIndexItem.getSizeDescription(getContext()));

					wizardButton.setOnClickListener(view -> {
						boolean spaceEnoughForLocal = validationManager.isSpaceEnoughForDownload(getActivity(), true, mapIndexItem);
						if (spaceEnoughForLocal) {
							mapDownloadCancelled = false;
							showMapDownloadWizard(true);
						}
					});
				}
				break;
			case MAP_DOWNLOAD:
				wizardButton.setEnabled(true);
				AndroidUiHelper.updateVisibility(wizardProgressBarCircle, false);
				AndroidUiHelper.updateVisibility(wizardIcon, true);
				wizardIcon.setImageDrawable(ContextCompat.getDrawable(app, R.drawable.ic_map));
				wizardIcon.getDrawable().setTint(app.getColor(R.color.icon_color_active_light));

				if (mapIndexItem != null) {
					AndroidUiHelper.updateVisibility(wizardProgressBar, true);
					wizardButton.setOnClickListener(view -> {
						mapDownloadCancelled = true;
						downloadThread.cancelDownload(mapIndexItem);
						AndroidUiHelper.updateVisibility(wizardProgressBar, false);
						wizardProgressBar.setProgress(0);
						showMapFoundWizard(true);
					});
				}
				break;
			case MAP_DOWNLOADED:
				wizardButton.setEnabled(true);
				AndroidUiHelper.updateVisibility(wizardProgressBarCircle, false);
				AndroidUiHelper.updateVisibility(wizardProgressBar, false);
				AndroidUiHelper.updateVisibility(wizardIcon, true);
				wizardIcon.setImageDrawable(ContextCompat.getDrawable(app, R.drawable.ic_map));
				wizardIcon.getDrawable().setTint(app.getColor(R.color.icon_color_active_light));
				wizardButton.setButtonType(DialogButtonType.PRIMARY);
				wizardButton.setTitleId(R.string.go_to_map);

				wizardButton.setOnClickListener(view -> {
					showOnMap(new LatLon(location.getLatitude(), location.getLongitude()));
				});
				break;
		}
	}

	private void setupNightMode() {
		int deviceUiMode = app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		switch (deviceUiMode) {
			case Configuration.UI_MODE_NIGHT_YES:
				deviceNightMode = true;
				break;
			case Configuration.UI_MODE_NIGHT_NO:
				deviceNightMode = false;
				break;
			case Configuration.UI_MODE_NIGHT_UNDEFINED:
				deviceNightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
				break;
		}
	}

	private void updateSkipButton() {
		AppCompatButton skipButton = view.findViewById(R.id.skip_button);
		if ((wizardType == WizardType.MAP_DOWNLOAD || wizardType == WizardType.MAP_DOWNLOADED) && mapIndexItem != null) {
			skipButton.setText(getString(R.string.shared_string_continue));
		} else {
			skipButton.setText(getString(R.string.skip_download));
		}
	}

	private void setupSkipButton() {
		AppCompatButton skipButton = view.findViewById(R.id.skip_button);
		skipButton.setOnClickListener(v -> {
			if (location != null) {
				showOnMap(new LatLon(location.getLatitude(), location.getLongitude()));
			} else {
				closeWizard();
			}
		});
		updateSkipButton();
	}

	private void setupActionButton() {
		ImageButton otherButton = view.findViewById(R.id.actions_button);
		otherButton.setOnClickListener(v -> FirstUsageActionsBottomSheet.showInstance(activity, this));
	}

	private void setupLocationButton() {
		ImageButton locationButton = view.findViewById(R.id.location_button);
		locationButton.setOnClickListener(v -> FirstUsageLocationBottomSheet.showInstance(activity, this));
	}

	@SuppressLint("StaticFieldLeak")
	private void doWizardTypeTask() {
		switch (wizardType) {
			case SEARCH_LOCATION:
				if (searchLocationByIp) {
					Map<String, String> pms = new LinkedHashMap<>();
					pms.put("version", Version.getFullVersion(app));
					if (app.isUserAndroidIdAllowed()) {
						pms.put("aid", app.getUserAndroidId());
					}
					OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, String>() {

						@Override
						protected String doInBackground(Void... params) {
							try {
								return AndroidNetworkUtils.sendRequest(app, "https://osmand.net/api/geo-ip", pms,
										"Requesting location by IP...", false, false);

							} catch (Exception e) {
								logError("Requesting location by IP error: ", e);
								return null;
							}
						}

						@Override
						protected void onPostExecute(String response) {
							if (!isAdded()) {
								return;
							}
							if (response != null) {
								try {
									JSONObject obj = new JSONObject(response);
									double latitude = obj.getDouble("latitude");
									double longitude = obj.getDouble("longitude");
									if (latitude == 0 && longitude == 0) {
										showNoLocationWizard(true);
									} else {
										location = new Location("geo-ip");
										location.setLatitude(latitude);
										location.setLongitude(longitude);
										showSearchMapWizard(true);
									}
								} catch (Exception e) {
									logError("JSON parsing error: ", e);
									showNoLocationWizard(true);
								}
							} else {
								showNoLocationWizard(true);
							}
						}
					});
				} else {
					if (!OsmAndLocationProvider.isLocationPermissionAvailable(activity)) {
						ActivityCompat.requestPermissions(activity,
								new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
										Manifest.permission.ACCESS_COARSE_LOCATION},
								FIRST_USAGE_LOCATION_PERMISSION);
					} else {
						app.getLocationProvider().addLocationListener(this);
						locationSearchTimer = new Timer();
						locationSearchTimer.schedule(new TimerTask() {
							@Override
							public void run() {
								if (isAdded()) {
									app.runInUIThread(() -> showNoLocationWizard(true));
								}
							}
						}, 1000 * 10);
					}
				}
				break;
			case NO_INTERNET:
				break;
			case NO_LOCATION:
				break;
			case SEARCH_MAP:
				if (app.isApplicationInitializing()) {
					app.getAppInitializer().addListener(this);
				} else {
					if (!downloadThread.getIndexes().isDownloadedFromInternet) {
						waitForIndexes = true;
						downloadThread.runReloadIndexFilesSilent();
					} else {
						searchMap();
					}
				}
				break;
			case MAP_FOUND:
				break;
			case MAP_DOWNLOAD:
				startDownload();
				if (mapDownloadRegion != null) {
					downloadThread.initSettingsFirstMap(mapDownloadRegion);
				}
				break;
			case MAP_DOWNLOADED:
				break;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		doWizardTypeTask();
	}

	@Override
	public void onStop() {
		super.onStop();
		cancelLocationSearchTimer();
		app.getLocationProvider().removeLocationListener(this);
		app.getAppInitializer().removeListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.enableDrawer();
		}
	}

	@Override
	public void updateLocation(Location loc) {
		if (loc != null) {
			cancelLocationSearchTimer();
			app.getLocationProvider().removeLocationListener(this);
			if (location == null) {
				location = new Location(loc);
				showSearchMapWizard(true);
			}
		}
	}

	@Override
	public void onFinish(@NonNull AppInitializer init) {
		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			waitForIndexes = true;
			downloadThread.runReloadIndexFilesSilent();
		} else {
			searchMap();
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		if (waitForIndexes && wizardType == WizardType.SEARCH_MAP) {
			waitForIndexes = false;
			searchMap();
		}
	}

	@Override
	public void downloadInProgress() {
		IndexItem indexItem = downloadThread.getCurrentDownloadingItem();
		if (indexItem != null && !indexItem.isDownloaded()) {
			int progress = (int) downloadThread.getCurrentDownloadProgress();
			double mb = indexItem.getArchiveSizeMB();
			String downloadProgress;
			if (progress != -1) {
				downloadProgress = getString(R.string.value_downloaded_of_mb, mb * progress / 100, mb);
			} else {
				downloadProgress = indexItem.getSizeDescription(getContext());
			}
			if (!mapDownloadCancelled) {
				wizardButton.setButtonType(DialogButtonType.SECONDARY);
				wizardButton.setTitle(downloadProgress);
				wizardProgressBar.setProgress(Math.max(progress, 0));
			}
		}
	}

	@Override
	public void downloadHasFinished() {
		if (mapIndexItem != null && mapIndexItem.isDownloaded()) {
			showMapDownloadedWizard(true);
		}
	}

	private boolean startDownload() {
		boolean downloadStarted = false;
		if (mapIndexItem != null && !downloadThread.isDownloading(mapIndexItem)
				&& !mapIndexItem.isDownloaded()
				&& !mapDownloadCancelled) {
			validationManager.startDownload(activity, mapIndexItem);
			downloadStarted = true;
		}
		return downloadStarted;
	}

	@Override
	public void onStorageSelected(@NonNull StorageItem storageItem) {
		DataStorageHelper.checkAssetsAsync(app);
		DataStorageHelper.updateDownloadIndexes(app);
	}

	private void showOnMap(LatLon mapCenter) {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			app.getOsmandMap().setMapLocation(mapCenter.getLatitude(), mapCenter.getLongitude());
		}
		closeWizard();
	}

	private void searchCountryMap() {
		closeWizard();
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Intent intent = new Intent(activity, DownloadActivity.class);
			intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
			activity.startActivity(intent);
		}
	}

	private void searchMap() {
		if (location != null) {
			int point31x = MapUtils.get31TileNumberX(location.getLongitude());
			int point31y = MapUtils.get31TileNumberY(location.getLatitude());

			ResourceManager rm = app.getResourceManager();
			OsmandRegions osmandRegions = rm.getOsmandRegions();

			List<BinaryMapDataObject> mapDataObjects = null;
			try {
				mapDataObjects = osmandRegions.query(point31x, point31x, point31y, point31y);
			} catch (IOException e) {
				e.printStackTrace();
			}

			String selectedFullName = "";
			if (mapDataObjects != null) {
				Iterator<BinaryMapDataObject> it = mapDataObjects.iterator();
				while (it.hasNext()) {
					BinaryMapDataObject o = it.next();
					if (!osmandRegions.contain(o, point31x, point31y)) {
						it.remove();
					}
				}
				for (BinaryMapDataObject o : mapDataObjects) {
					String fullName = osmandRegions.getFullName(o);
					if (fullName != null && fullName.length() > selectedFullName.length()) {
						selectedFullName = fullName;
					}
				}
			}

			if (!Algorithms.isEmpty(selectedFullName)) {
				WorldRegion downloadRegion = osmandRegions.getRegionData(selectedFullName);
				if (downloadRegion != null && downloadRegion.isRegionMapDownload()) {
					mapDownloadRegion = downloadRegion;
					List<IndexItem> indexItems = new LinkedList<>(downloadThread.getIndexes().getIndexItems(downloadRegion));
					for (IndexItem item : indexItems) {
						if (item.getType() == DownloadActivityType.NORMAL_FILE) {
							mapIndexItem = item;
							break;
						}
					}
				}
			}
			if (mapIndexItem != null) {
				showMapFoundWizard(true);
			} else {
				closeWizard();
			}

		} else {
			showNoLocationWizard(true);
		}
	}

	private void cancelLocationSearchTimer() {
		if (locationSearchTimer != null) {
			locationSearchTimer.cancel();
			locationSearchTimer = null;
		}
	}

	public void showAppropriateWizard(FragmentActivity activity, boolean updateWizardView) {
		if (activity != null) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			if (!app.getSettings().isInternetConnectionAvailable()) {
				showNoInternetWizard(updateWizardView);
			} else if (location == null) {
				findLocation(activity, true, updateWizardView);
			} else {
				showSearchMapWizard(updateWizardView);
			}
		}
	}

	private void setProperZoom() {
		int zoom;
		if (app.getResourceManager().isAnyMapInstalled() || (mapIndexItem != null && app.getDownloadThread().isDownloading(mapIndexItem))) {
			zoom = DOWNLOAD_MAP_ZOOM_LEVEL;
		} else {
			zoom = NO_MAP_ZOOM_LEVEL;
		}
		app.getOsmandMap().getMapView().setIntZoom(zoom);
	}

	public void closeWizard() {
		app.getSettings().SHOW_OSMAND_WELCOME_SCREEN.set(false);
		setProperZoom();
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.getSupportFragmentManager()
					.beginTransaction()
					.remove(this)
					.commitAllowingStateLoss();
			location = null;
			mapDownloadRegion = null;
			mapIndexItem = null;
			wizardClosed = true;
		}
	}

	public void processLocationPermission(boolean granted) {
		if (granted) {
			findLocation(getActivity(), false, true);
		} else {
			showNoLocationWizard(true);
		}
	}

	private void findLocation(FragmentActivity activity, boolean searchLocationByIp, boolean updateWizardView) {
		if (activity != null) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			if (searchLocationByIp) {
				showSearchLocationWizard(updateWizardView, true);
			} else if (OsmAndLocationProvider.isLocationPermissionAvailable(activity)) {
				Location loc = app.getLocationProvider().getLastKnownLocation();
				if (loc == null) {
					showSearchLocationWizard(updateWizardView, false);
				} else {
					location = new Location(loc);
					showSearchMapWizard(updateWizardView);
				}
			} else {
				showSearchLocationWizard(updateWizardView, false);
			}
		}
	}

	public void updateTermsOfServiceView() {
		TextView textView = view.findViewById(R.id.terms_of_service_description);
		if (textView != null) {
			String termsOfUse = getString(R.string.shared_string_terms_of_use);
			String privacyPolicy = getString(R.string.shared_string_privacy_policy);
			String text = getString(R.string.terms_of_service_desc, termsOfUse, privacyPolicy);
			SpannableString spannable = new SpannableString(text);
			setupClickableToSText(spannable, termsOfUse, R.string.docs_legal_terms_of_use);
			setupClickableToSText(spannable, privacyPolicy, R.string.docs_legal_privacy_policy);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setText(spannable);
		}
	}

	private void setupClickableToSText(SpannableString text, String part, int urlId) {
		int startInd = text.toString().indexOf(part);
		int endInd = startInd + part.length();
		int color = ColorUtilities.getColor(app, R.color.active_color_primary_light);
		ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
		Typeface typeface = FontCache.getMediumFont();
		CustomTypefaceSpan typefaceSpan = new CustomTypefaceSpan(typeface);
		ClickableSpan clickableSpan = new CustomClickableSpan() {
			@Override
			public void onClick(@NonNull View widget) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					AndroidUtils.openUrl(activity, urlId, false);
				}
			}
		};
		text.setSpan(colorSpan, startInd, endInd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		text.setSpan(typefaceSpan, startInd, endInd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		text.setSpan(clickableSpan, startInd, endInd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), deviceNightMode);
		return ColorUtilities.getListBgColorId(deviceNightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	public void showSearchLocationWizard(boolean updateWizardView, boolean searchByIp) {
		searchLocationByIp = searchByIp;
		setWizardType(WizardType.SEARCH_LOCATION, updateWizardView);
	}

	public void showSearchMapWizard(boolean updateWizardView) {
		setWizardType(WizardType.SEARCH_MAP, updateWizardView);
	}

	public void showMapFoundWizard(boolean updateWizardView) {
		setWizardType(WizardType.MAP_FOUND, updateWizardView);
	}

	public void showMapDownloadWizard(boolean updateWizardView) {
		setWizardType(WizardType.MAP_DOWNLOAD, updateWizardView);
	}

	public void showNoInternetWizard(boolean updateWizardView) {
		setWizardType(WizardType.NO_INTERNET, updateWizardView);
	}

	public void showNoLocationWizard(boolean updateWizardView) {
		setWizardType(WizardType.NO_LOCATION, updateWizardView);
	}

	public void showMapDownloadedWizard(boolean updateWizardView) {
		setWizardType(WizardType.MAP_DOWNLOADED, updateWizardView);
	}

	@Override
	public void processActionClick(@NonNull FirstUsageAction action) {
		switch (action) {
			case SELECT_COUNTRY:
				searchCountryMap();
				break;
			case DETERMINE_LOCATION:
				determineLocation();
				break;
			case RESTORE_FROM_CLOUD:
				restoreFromCloud();
				break;
			case RESTORE_FROM_FILE:
				restoreFromFile();
				break;
			case SELECT_STORAGE_FOLDER:
				selectStorageFolder();
				break;
		}
	}

	public void determineLocation() {
		if (!OsmAndLocationProvider.isLocationPermissionAvailable(activity)) {
			location = null;
			ActivityCompat.requestPermissions(activity,
					new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_COARSE_LOCATION},
					FIRST_USAGE_LOCATION_PERMISSION);
		} else {
			findLocation(activity, false, true);
		}
	}

	public void restoreFromCloud() {
		if (app.getBackupHelper().isRegistered()) {
			BackupCloudFragment.showInstance(activity.getSupportFragmentManager());
		} else {
			BackupAuthorizationFragment.showInstance(activity.getSupportFragmentManager());
		}
	}

	public void restoreFromFile() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getImportHelper().chooseFileToImport(SETTINGS);
		}
	}

	public void selectStorageFolder() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Bundle args = new Bundle();
			args.putBoolean(FIRST_USAGE, true);
			BaseSettingsFragment.showInstance(activity, SettingsScreenType.DATA_STORAGE, null, args, FirstUsageWizardFragment.this);
		}
	}

	private void logError(String msg, Throwable e) {
		Log.e(TAG, "Error: " + msg, e);
	}

	public static boolean showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (!wizardClosed && AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
			FirstUsageWizardFragment fragment = new FirstUsageWizardFragment();
			fragment.showAppropriateWizard(activity, false);
			activity.getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}

interface FirstUsageActionsListener {
	void processActionClick(@NonNull FirstUsageAction action);
}