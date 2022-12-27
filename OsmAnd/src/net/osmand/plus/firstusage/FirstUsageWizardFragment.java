package net.osmand.plus.firstusage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.Location;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.ValueHolder;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.DataStorageFragment.StorageSelectionListener;
import net.osmand.plus.settings.datastorage.DataStorageHelper;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class FirstUsageWizardFragment extends BaseOsmAndFragment implements OsmAndLocationListener,
		AppInitializeListener, DownloadEvents, StorageSelectionListener {

	public static final String TAG = "FirstUsageWizardFrag";
	public static final int FIRST_USAGE_LOCATION_PERMISSION = 300;
	public static final String WIZARD_TYPE_KEY = "wizard_type_key";
	public static final String SEARCH_LOCATION_BY_IP_KEY = "search_location_by_ip_key";
	public static final String FIRST_USAGE = "first_usage";

	private OsmandApplication app;
	private View view;
	private DownloadIndexesThread downloadThread;
	private DownloadValidationManager validationManager;

	private static WizardType wizardType;
	private static final WizardType DEFAULT_WIZARD_TYPE = WizardType.SEARCH_LOCATION;
	private static boolean searchLocationByIp;

	private Timer locationSearchTimer;
	private boolean waitForIndexes;

	private static Location location;
	private static WorldRegion mapDownloadRegion;
	private static IndexItem mapIndexItem;
	private static boolean mapDownloadCancelled;
	private static boolean wizardClosed;

	enum WizardType {
		SEARCH_LOCATION,
		NO_INTERNET,
		NO_LOCATION,
		SEARCH_MAP,
		MAP_FOUND,
		MAP_DOWNLOAD
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		downloadThread = app.getDownloadThread();
		validationManager = new DownloadValidationManager(app);
		Bundle args = getArguments();
		if (args != null) {
			wizardType = WizardType.valueOf(args.getString(WIZARD_TYPE_KEY, DEFAULT_WIZARD_TYPE.name()));
			searchLocationByIp = args.getBoolean(SEARCH_LOCATION_BY_IP_KEY, false);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.first_usage_wizard_fragment, container, false);
		FragmentActivity activity = requireActivity();
		AndroidUtils.addStatusBarPadding21v(activity, view);

		if (!AndroidUiHelper.isOrientationPortrait(activity) && !AndroidUiHelper.isXLargeDevice(activity)) {
			TextView wizardDescription = view.findViewById(R.id.wizard_description);
			wizardDescription.setMinimumHeight(0);
			wizardDescription.setMinHeight(0);
		}

		AppCompatButton skipButton = view.findViewById(R.id.skip_button);
		skipButton.setOnClickListener(v -> {
			if (wizardType == WizardType.MAP_DOWNLOAD) {
				if (location != null) {
					showOnMap(new LatLon(location.getLatitude(), location.getLongitude()), 13);
				} else {
					closeWizard();
				}
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				builder.setTitle(getString(R.string.skip_map_downloading));
				builder.setMessage(getString(R.string.skip_map_downloading_desc, getString(R.string.welmode_download_maps)));
				builder.setNegativeButton(R.string.shared_string_skip, (dialog, which) -> {
					if (location != null) {
						showOnMap(new LatLon(location.getLatitude(), location.getLongitude()), 13);
					} else {
						closeWizard();
					}
				});
				builder.setNeutralButton(R.string.shared_string_cancel, null);
				builder.setPositiveButton(R.string.shared_string_select, (dialog, which) -> searchCountryMap());
				builder.show();
			}
		});

		view.findViewById(R.id.action_button).setVisibility(View.GONE);

		switch (wizardType) {
			case SEARCH_LOCATION:
				view.findViewById(R.id.search_location_card).setVisibility(View.VISIBLE);
				view.findViewById(R.id.search_location_action_button).setEnabled(false);
				break;
			case NO_INTERNET:
				view.findViewById(R.id.no_inet_card).setVisibility(View.VISIBLE);
				view.findViewById(R.id.no_inet_action_button).setOnClickListener(v -> startWizard(getActivity()));
				break;
			case NO_LOCATION:
				view.findViewById(R.id.no_location_card).setVisibility(View.VISIBLE);
				view.findViewById(R.id.no_location_action_button).setOnClickListener(v -> findLocation(getActivity(), false));
				break;
			case SEARCH_MAP:
				view.findViewById(R.id.search_map_card).setVisibility(View.VISIBLE);
				view.findViewById(R.id.search_map_action_button).setEnabled(false);
				break;
			case MAP_FOUND:
				TextView mapTitle = view.findViewById(R.id.map_download_title);
				TextView mapDescription = view.findViewById(R.id.map_download_desc);
				if (mapIndexItem != null) {
					mapTitle.setText(mapIndexItem.getVisibleName(getContext(), app.getRegions(), false));
					mapDescription.setText(mapIndexItem.getSizeDescription(getContext()));
				}
				view.findViewById(R.id.map_download_action_button).setOnClickListener(v -> {
					boolean spaceEnoughForLocal = validationManager.isSpaceEnoughForDownload(getActivity(), true, mapIndexItem);
					if (spaceEnoughForLocal) {
						showMapDownloadFragment(getActivity());
					}
				});
				view.findViewById(R.id.map_download_card).setVisibility(View.VISIBLE);
				AppCompatButton searchCountryButton = view.findViewById(R.id.search_country_button);
				searchCountryButton.setVisibility(View.VISIBLE);
				searchCountryButton.setOnClickListener(v -> searchCountryMap());
				break;
			case MAP_DOWNLOAD:
				if (mapIndexItem != null) {
					String mapName = mapIndexItem.getVisibleName(getContext(), app.getRegions(), false);
					TextView mapNameTextView = view.findViewById(R.id.map_downloading_title);
					mapNameTextView.setText(mapName);
					TextView mapDescriptionTextView = view.findViewById(R.id.map_downloading_desc);
					View progressPadding = view.findViewById(R.id.map_download_padding);
					View progressLayout = view.findViewById(R.id.map_download_progress_layout);
					mapDescriptionTextView.setText(mapIndexItem.getSizeDescription(getContext()));
					ImageButton redownloadButton = view.findViewById(R.id.map_redownload_button);
					redownloadButton.setOnClickListener(v -> {
						if (!downloadThread.isDownloading(mapIndexItem) && !mapIndexItem.isDownloaded()) {
							validationManager.startDownload(activity, mapIndexItem);
							mapDownloadCancelled = false;
						}
					});
					view.findViewById(R.id.map_download_progress_button).setOnClickListener(v -> {
						mapDownloadCancelled = true;
						downloadThread.cancelDownload(mapIndexItem);
						mapDescriptionTextView.setText(mapIndexItem.getSizeDescription(getContext()));
						progressPadding.setVisibility(View.VISIBLE);
						progressLayout.setVisibility(View.GONE);
						redownloadButton.setVisibility(View.VISIBLE);
					});
					progressPadding.setVisibility(View.VISIBLE);
					progressLayout.setVisibility(View.GONE);
					redownloadButton.setVisibility(View.VISIBLE);
					view.findViewById(R.id.map_downloading_layout).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.map_downloading_layout).setVisibility(View.GONE);
				}

				view.findViewById(R.id.map_downloading_action_button).setOnClickListener(v -> {
					if (location != null) {
						showOnMap(new LatLon(location.getLatitude(), location.getLongitude()), 13);
					}
				});
				view.findViewById(R.id.map_downloading_card).setVisibility(View.VISIBLE);
				break;
		}

		updateTermsOfServiceView();
		updateStorageView();

		return view;
	}

	@SuppressLint({"StaticFieldLeak", "HardwareIds"})
	@Override
	public void onStart() {
		super.onStart();

		OsmandApplication app = requireMyApplication();
		switch (wizardType) {
			case SEARCH_LOCATION:
				if (searchLocationByIp) {
					Map<String, String> pms = new LinkedHashMap<>();
					pms.put("version", Version.getFullVersion(app));
					if (app.isUserAndroidIdAllowed()) {
						pms.put("aid", app.getUserAndroidId());
					}
					new AsyncTask<Void, Void, String>() {

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
							if (response != null) {
								try {
									JSONObject obj = new JSONObject(response);
									double latitude = obj.getDouble("latitude");
									double longitude = obj.getDouble("longitude");
									if (latitude == 0 && longitude == 0) {
										showNoLocationFragment(getActivity());
									} else {
										location = new Location("geo-ip");
										location.setLatitude(latitude);
										location.setLongitude(longitude);
										showSearchMapFragment(getActivity());
									}
								} catch (Exception e) {
									logError("JSON parsing error: ", e);
									showNoLocationFragment(getActivity());
								}
							} else {
								showNoLocationFragment(getActivity());
							}
						}
					}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					FragmentActivity activity = requireActivity();
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
								app.runInUIThread(() -> showNoLocationFragment(activity));
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
		}
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
		requireMapActivity().disableDrawer();
	}

	@Override
	public void onPause() {
		super.onPause();
		requireMapActivity().enableDrawer();
	}

	@Override
	public void updateLocation(Location loc) {
		if (loc != null) {
			app.runInUIThread(() -> {
				cancelLocationSearchTimer();
				app.getLocationProvider().removeLocationListener(this);
				if (location == null) {
					location = new Location(loc);
					showSearchMapFragment(getActivity());
				}
			});
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
			String v;
			if (progress != -1) {
				v = getString(R.string.value_downloaded_of_max, mb * progress / 100, mb);
			} else {
				v = getString(R.string.file_size_in_mb, mb);
			}
			if (!mapDownloadCancelled) {
				TextView mapDescriptionTextView = view.findViewById(R.id.map_downloading_desc);
				ProgressBar progressBar = view.findViewById(R.id.map_download_progress_bar);
				mapDescriptionTextView.setText(v);
				progressBar.setProgress(Math.max(progress, 0));
			}
		}
		updateDownloadedItem();
	}

	@Override
	public void downloadHasFinished() {
		updateDownloadedItem();
	}

	private boolean startDownload() {
		boolean downloadStarted = false;
		if (mapIndexItem != null && !downloadThread.isDownloading(mapIndexItem)
				&& !mapIndexItem.isDownloaded()
				&& !mapDownloadCancelled) {
			validationManager.startDownload(requireActivity(), mapIndexItem);
			downloadStarted = true;
		}
		return downloadStarted;
	}

	private void updateDownloadedItem() {
		if (mapIndexItem == null) {
			return;
		}
		View firstRowLayout = view.findViewById(R.id.map_downloading_layout);
		View progressLayout = view.findViewById(R.id.map_download_progress_layout);
		ImageButton redownloadButton = view.findViewById(R.id.map_redownload_button);
		if (mapIndexItem.isDownloaded()) {
			if (progressLayout.getVisibility() == View.VISIBLE) {
				TextView mapDescriptionTextView = view.findViewById(R.id.map_downloading_desc);
				mapDescriptionTextView.setText(mapIndexItem.getSizeDescription(getContext()));
				view.findViewById(R.id.map_download_padding).setVisibility(View.VISIBLE);
				progressLayout.setVisibility(View.GONE);
				firstRowLayout.setClickable(true);
				LatLon mapCenter = getMapCenter();
				int mapZoom = getMapZoom();
				firstRowLayout.setOnClickListener(v -> showOnMap(mapCenter, mapZoom));
			}
		} else {
			if (downloadThread.isDownloading(mapIndexItem)) {
				if (!mapDownloadCancelled) {
					if (progressLayout.getVisibility() == View.GONE) {
						progressLayout.setVisibility(View.VISIBLE);
					}
					if (redownloadButton.getVisibility() == View.VISIBLE) {
						redownloadButton.setVisibility(View.GONE);
					}
				}
			}
		}
	}

	@Override
	public void onStorageSelected(@NonNull StorageItem storageItem) {
		DataStorageHelper.checkAssetsAsync(app);
		DataStorageHelper.updateDownloadIndexes(app);
	}

	private LatLon getMapCenter() {
		LatLon mapCenter;
		if (mapDownloadRegion != null) {
			mapCenter = mapDownloadRegion.getRegionCenter();
		} else {
			mapCenter = new LatLon(48, 17);
		}
		return mapCenter;
	}

	private int getMapZoom() {
		int mapZoom;
		if (mapDownloadRegion != null) {
			mapZoom = 13;
		} else {
			mapZoom = 3;
		}
		return mapZoom;
	}

	private void showOnMap(LatLon mapCenter, int mapZoom) {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			app.getOsmandMap().setMapLocation(mapCenter.getLatitude(), mapCenter.getLongitude());
			app.getOsmandMap().getMapView().setIntZoom(mapZoom);
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
				showMapFoundFragment(getActivity());
			} else {
				closeWizard();
			}

		} else {
			showNoLocationFragment(getActivity());
		}
	}

	private void cancelLocationSearchTimer() {
		if (locationSearchTimer != null) {
			locationSearchTimer.cancel();
			locationSearchTimer = null;
		}
	}

	public static void startWizard(FragmentActivity activity) {
		if (activity != null) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			if (!app.getSettings().isInternetConnectionAvailable()) {
				showNoInternetFragment(activity);
			} else if (location == null) {
				findLocation(activity, true);
			} else {
				showSearchMapFragment(activity);
			}
		}
	}

	public void closeWizard() {
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
			findLocation(getActivity(), false);
		} else {
			showNoLocationFragment(getActivity());
		}
	}

	private static void findLocation(FragmentActivity activity, boolean searchLocationByIp) {
		if (activity != null) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			if (searchLocationByIp) {
				showSearchLocationFragment(activity, true);
			} else if (OsmAndLocationProvider.isLocationPermissionAvailable(activity)) {
				Location loc = app.getLocationProvider().getLastKnownLocation();
				if (loc == null) {
					showSearchLocationFragment(activity, false);
				} else {
					location = new Location(loc);
					showSearchMapFragment(activity);
				}
			} else {
				showSearchLocationFragment(activity, false);
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
		Typeface typeface = FontCache.getRobotoMedium(getContext());
		CustomTypefaceSpan typefaceSpan = new CustomTypefaceSpan(typeface);
		ClickableSpan clickableSpan = new ClickableSpan() {

			@Override
			public void onClick(@NonNull View widget) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					AndroidUtils.openUrl(activity, urlId, false);
				}
			}

			@Override
			public void updateDrawState(@NonNull TextPaint ds) {
				super.updateDrawState(ds);
				ds.setUnderlineText(false);
			}
		};
		text.setSpan(colorSpan, startInd, endInd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		text.setSpan(typefaceSpan, startInd, endInd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		text.setSpan(clickableSpan, startInd, endInd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
	}

	public void updateStorageView() {
		updateStorageView(view.findViewById(R.id.storage_layout));
	}

	private void updateStorageView(View storageView) {
		if (storageView != null) {
			TextView title = storageView.findViewById(R.id.storage_title);
			OsmandSettings settings = app.getSettings();
			int type;
			if (settings.getExternalStorageDirectoryTypeV19() >= 0) {
				type = settings.getExternalStorageDirectoryTypeV19();
			} else {
				ValueHolder<Integer> vh = new ValueHolder<>();
				settings.getExternalStorageDirectory(vh);
				if (vh.value != null && vh.value >= 0) {
					type = vh.value;
				} else {
					type = 0;
				}
			}
			title.setText(getString(R.string.storage_place_description, getStorageName(type)));

			TextView freeSpace = storageView.findViewById(R.id.storage_free_space);
			TextView freeSpaceValue = storageView.findViewById(R.id.storage_free_space_value);
			String freeSpaceStr = getString(R.string.storage_free_space) + ": ";
			freeSpace.setText(freeSpaceStr);
			freeSpaceValue.setText(AndroidUtils.getFreeSpace(storageView.getContext(), settings.getExternalStorageDirectory()));

			AppCompatButton changeStorageButton = storageView.findViewById(R.id.storage_change_button);
			if (wizardType == WizardType.MAP_DOWNLOAD) {
				changeStorageButton.setEnabled(false);
				changeStorageButton.setTextColor(ContextCompat.getColor(app, R.color.text_color_secondary_light));
			} else {
				changeStorageButton.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						Bundle args = new Bundle();
						args.putBoolean(FIRST_USAGE, true);
						BaseSettingsFragment.showInstance(activity, SettingsScreenType.DATA_STORAGE, null, args, this);
					}
				});
			}
		}
	}

	private String getStorageName(int type) {
		if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_INTERNAL_FILE) {
			return getString(R.string.storage_directory_internal_app);
		} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT) {
			return getString(R.string.storage_directory_shared);
		} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE) {
			return getString(R.string.storage_directory_external);
		} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_OBB) {
			return getString(R.string.storage_directory_multiuser);
		} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_SPECIFIED) {
			return getString(R.string.storage_directory_manual);
		} else {
			return getString(R.string.storage_directory_manual);
		}
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return (MapActivity) requireActivity();
	}

	public static void showSearchLocationFragment(FragmentActivity activity, boolean searchByIp) {
		Bundle args = new Bundle();
		args.putString(WIZARD_TYPE_KEY, WizardType.SEARCH_LOCATION.name());
		args.putBoolean(SEARCH_LOCATION_BY_IP_KEY, searchByIp);
		showFragment(activity, args);
	}

	public static void showSearchMapFragment(FragmentActivity activity) {
		Bundle args = new Bundle();
		args.putString(WIZARD_TYPE_KEY, WizardType.SEARCH_MAP.name());
		showFragment(activity, args);
	}

	public static void showMapFoundFragment(FragmentActivity activity) {
		Bundle args = new Bundle();
		args.putString(WIZARD_TYPE_KEY, WizardType.MAP_FOUND.name());
		showFragment(activity, args);
	}

	public static void showMapDownloadFragment(FragmentActivity activity) {
		Bundle args = new Bundle();
		args.putString(WIZARD_TYPE_KEY, WizardType.MAP_DOWNLOAD.name());
		showFragment(activity, args);
	}

	public static void showNoInternetFragment(FragmentActivity activity) {
		Bundle args = new Bundle();
		args.putString(WIZARD_TYPE_KEY, WizardType.NO_INTERNET.name());
		showFragment(activity, args);
	}

	public static void showNoLocationFragment(FragmentActivity activity) {
		Bundle args = new Bundle();
		args.putString(WIZARD_TYPE_KEY, WizardType.NO_LOCATION.name());
		showFragment(activity, args);
	}

	private static void showFragment(@Nullable FragmentActivity activity, @NonNull Bundle args) {
		if (!wizardClosed && activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				Fragment fragment = new FirstUsageWizardFragment();
				fragment.setArguments(args);
				activity.getSupportFragmentManager()
						.beginTransaction()
						.replace(R.id.fragmentContainer, fragment, TAG)
						.commitAllowingStateLoss();
			}
		}
	}

	private static void logError(String msg, Throwable e) {
		Log.e(TAG, "Error: " + msg, e);
	}
}