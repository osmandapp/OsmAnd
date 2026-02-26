package net.osmand.plus.settings.fragments;

import static net.osmand.plus.settings.backend.backup.exporttype.ExportType.MAP_SOURCES;
import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ApplicationModeBean;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper.SettingsExportListener;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.exporttype.MapSourcesExportType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExportSettingsFragment extends BaseSettingsListFragment {

	public static final String TAG = ExportSettingsFragment.class.getSimpleName();
	public static final Log LOG = PlatformUtil.getLog(ExportSettingsFragment.class.getSimpleName());

	private static final String GLOBAL_EXPORT_KEY = "global_export_key";
	private static final String EXPORT_START_TIME_KEY = "export_start_time_key";
	private static final String EXPORTING_STARTED_KEY = "exporting_started_key";
	private static final String PROGRESS_MAX_KEY = "progress_max_key";
	private static final String PROGRESS_VALUE_KEY = "progress_value_key";
	public static final String SELECTED_TYPES = "selected_types";

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	private ProgressDialog progress;
	private ApplicationMode appMode;
	private SettingsExportListener exportListener;

	private int progressMax;
	private int progressValue;
	private long exportStartTime;
	private boolean globalExport;
	private boolean exportingStarted;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
			globalExport = savedInstanceState.getBoolean(GLOBAL_EXPORT_KEY);
			exportingStarted = savedInstanceState.getBoolean(EXPORTING_STARTED_KEY);
			exportStartTime = savedInstanceState.getLong(EXPORT_START_TIME_KEY);
			progressMax = savedInstanceState.getInt(PROGRESS_MAX_KEY);
			progressValue = savedInstanceState.getInt(PROGRESS_VALUE_KEY);
		}
		exportMode = true;
		dataList = app.getFileSettingsHelper().collectCategorizedExportData(true, true);

		if (savedInstanceState == null) {
			if (!globalExport) {
				updateSelectedProfile();
			}
			Bundle args = getArguments();
			if (args != null && args.containsKey(SELECTED_TYPES)) {
				addSelectedTypes((Map<ExportType, List<?>>) AndroidUtils.getSerializable(args, SELECTED_TYPES, HashMap.class));
			}
		}
	}

	private void addSelectedTypes(@Nullable Map<ExportType, List<?>> selectedTypes) {
		if (!Algorithms.isEmpty(selectedTypes)) {
			for (Map.Entry<ExportType, List<?>> entry : selectedTypes.entrySet()) {
				ExportType exportType = entry.getKey();
				List<?> items = entry.getValue();

				if (exportType == MAP_SOURCES && !Algorithms.isEmpty(items)) {
					items = convertTileSources(items);
				}
				if (items == null) {
					items = getItemsForType(exportType);
				}
				if (!Algorithms.isEmpty(items)) {
					selectedItemsMap.put(exportType, items);
				}
			}
		}
	}

	@NonNull
	private List<ITileSource> convertTileSources(@NonNull List<?> items) {
		List<ITileSource> sources = new ArrayList<>();
		for (Object item : items) {
			if (item instanceof File) {
				File file = (File) item;
				ITileSource template;
				if (file.getName().endsWith(SQLiteTileSource.EXT)) {
					template = new SQLiteTileSource(app, file, TileSourceManager.getKnownSourceTemplates());
				} else {
					template = TileSourceManager.createTileSourceTemplate(file);
				}
				if (!MapSourcesExportType.shouldSkipMapSource(template)) {
					sources.add(template);
				}
			}
		}
		return sources;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		if (view != null) {
			CollapsingToolbarLayout toolbarLayout = view.findViewById(R.id.toolbar_layout);
			toolbarLayout.setTitle(getString(R.string.shared_string_export));
			TextView description = header.findViewById(R.id.description);
			description.setText(R.string.select_data_to_export);
		}
		return view;
	}

	@Override
	protected void onContinueButtonClickAction() {
		prepareFile();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(GLOBAL_EXPORT_KEY, globalExport);
		outState.putBoolean(EXPORTING_STARTED_KEY, exportingStarted);
		outState.putLong(EXPORT_START_TIME_KEY, exportStartTime);
		outState.putString(APP_MODE_KEY, appMode.getStringKey());
		if (progress != null) {
			outState.putInt(PROGRESS_MAX_KEY, progress.getMax());
			outState.putInt(PROGRESS_VALUE_KEY, progress.getProgress());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		checkExportingFile();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (exportingStarted) {
			File file = getExportFile();
			app.getFileSettingsHelper().updateExportListener(file, null);
		}
	}

	@Override
	protected void dismissFragment() {
		super.dismissFragment();

		Bundle args = getArguments();
		MapActivity activity = getMapActivity();
		if (activity != null && args != null && args.containsKey(SELECTED_TYPES) && !exportingStarted) {
			activity.launchPrevActivityIntent();
		}
	}

	private void updateSelectedProfile() {
		List<?> profileItems = getItemsForType(ExportType.PROFILE);
		if (!Algorithms.isEmpty(profileItems)) {
			for (Object item : profileItems) {
				if (item instanceof ApplicationModeBean && appMode.getStringKey().equals(((ApplicationModeBean) item).stringKey)) {
					List<Object> selectedProfiles = new ArrayList<>();
					selectedProfiles.add(item);
					selectedItemsMap.put(ExportType.PROFILE, selectedProfiles);
					break;
				}
			}
		}
	}

	private void prepareFile() {
		exportingStarted = true;
		exportStartTime = System.currentTimeMillis();
		showExportProgressDialog();
		File tempDir = FileUtils.getTempDir(app);
		String fileName = getFileName();
		List<SettingsItem> items = app.getFileSettingsHelper().prepareSettingsItems(adapter.getData(), Collections.emptyList(), true);
		progress.setMax(getMaxProgress(items));
		app.getFileSettingsHelper().exportSettings(tempDir, fileName, getSettingsExportListener(), items, true);
	}

	private int getMaxProgress(List<SettingsItem> items) {
		long maxProgress = 0;
		for (SettingsItem item : items) {
			if (item instanceof FileSettingsItem) {
				maxProgress += ((FileSettingsItem) item).getSize();
			}
		}
		return (int) (maxProgress / (1 << 20));
	}

	private String getFileName() {
		if (globalExport) {
			if (exportStartTime == 0) {
				exportStartTime = System.currentTimeMillis();
			}
			return "Export_" + DATE_FORMAT.format(new Date(exportStartTime));
		} else {
			return appMode.toHumanString();
		}
	}

	private void showExportProgressDialog() {
		Context context = getContext();
		if (context == null) {
			return;
		}
		if (progress != null) {
			progress.dismiss();
		}
		progress = new ProgressDialog(context);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setCancelable(true);
		progress.setTitle(getString(R.string.shared_string_export));
		progress.setMessage(getString(R.string.shared_string_preparing));
		progress.setProgressNumberFormat("%1d/%2d MB");
		progress.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.shared_string_cancel), (dialog, which) -> cancelExport());
		progress.setOnCancelListener(dialog -> cancelExport());
		progress.show();
	}

	private void cancelExport() {
		app.getFileSettingsHelper().cancelExportForFile(getExportFile());
		progress.dismiss();
		dismissFragment();
	}

	private SettingsExportListener getSettingsExportListener() {
		if (exportListener == null) {
			exportListener = new SettingsExportListener() {

				@Override
				public void onSettingsExportFinished(@NonNull File file, boolean succeed) {
					dismissExportProgressDialog();
					if (succeed) {
						shareProfile(file);
						dismissFragment();
					} else {
						app.showToastMessage(R.string.export_profile_failed);
					}
				}

				@Override
				public void onSettingsExportProgressUpdate(int value) {
					progress.setProgress(value);
				}
			};
		}
		return exportListener;
	}

	private void checkExportingFile() {
		if (exportingStarted) {
			File file = getExportFile();
			boolean fileExporting = app.getFileSettingsHelper().isFileExporting(file);
			if (fileExporting) {
				showExportProgressDialog();
				progress.setMax(progressMax);
				progress.setProgress(progressValue);
				app.getFileSettingsHelper().updateExportListener(file, getSettingsExportListener());
			} else if (file.exists()) {
				dismissExportProgressDialog();
				shareProfile(file);
				dismissFragment();
			}
		}
	}

	private void dismissExportProgressDialog() {
		FragmentActivity activity = getActivity();
		if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
			progress.dismiss();
		}
	}

	private File getExportFile() {
		File tempDir = FileUtils.getTempDir(app);
		String fileName = getFileName();
		return new File(tempDir, fileName + IndexConstants.OSMAND_SETTINGS_FILE_EXT);
	}

	private void shareProfile(@NonNull File file) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
		sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, file));
		sendIntent.setType("*/*");
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		Intent chooserIntent = Intent.createChooser(sendIntent, getString(R.string.shared_string_share));
		AndroidUtils.startActivityIfSafe(app, chooserIntent);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull ApplicationMode appMode,
	                                @Nullable HashMap<ExportType, List<?>> selectedTypes, boolean globalExport) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ExportSettingsFragment fragment = new ExportSettingsFragment();
			fragment.appMode = appMode;
			fragment.globalExport = globalExport;

			if (!Algorithms.isEmpty(selectedTypes)) {
				Bundle args = new Bundle();
				args.putSerializable(SELECTED_TYPES, selectedTypes);
				fragment.setArguments(args);
			}
			manager.beginTransaction().
					replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(SETTINGS_LIST_TAG)
					.commitAllowingStateLoss();
		}
	}
}
