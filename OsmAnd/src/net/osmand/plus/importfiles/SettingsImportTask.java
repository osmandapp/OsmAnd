package net.osmand.plus.importfiles;

import android.app.ProgressDialog;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.FileUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.CustomOsmandPlugin;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.PluginSettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.CheckDuplicatesListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.SettingsCollectListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.SettingsImportListener;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.plus.settings.fragments.ImportCompleteFragment;
import net.osmand.plus.settings.fragments.ImportSettingsFragment;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.AppInitializer.loadRoutingFiles;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.getSettingsToOperate;

class SettingsImportTask extends BaseLoadAsyncTask<Void, Void, String> {

	private Uri uri;
	private String name;
	private List<ExportSettingsType> settingsTypes;
	private boolean replace;
	private boolean silentImport;
	private String latestChanges;
	private int version;
	private CallbackWithObject<List<SettingsItem>> callback;

	public SettingsImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri,
							  @NonNull String name, List<ExportSettingsType> settingsTypes,
							  boolean replace, boolean silentImport, String latestChanges, int version,
							  CallbackWithObject<List<SettingsItem>> callback) {
		super(activity);
		this.uri = uri;
		this.name = name;
		this.settingsTypes = settingsTypes;
		this.replace = replace;
		this.silentImport = silentImport;
		this.latestChanges = latestChanges;
		this.version = version;
		this.callback = callback;
	}

	@Override
	protected String doInBackground(Void... voids) {
		File tempDir = FileUtils.getTempDir(app);
		File dest = new File(tempDir, name);
		return ImportHelper.copyFile(app, dest, uri, true);
	}

	@Override
	protected void onPostExecute(String error) {
		File tempDir = FileUtils.getTempDir(app);
		final File file = new File(tempDir, name);
		if (error == null && file.exists()) {
			final SettingsHelper settingsHelper = app.getSettingsHelper();
			settingsHelper.collectSettings(file, latestChanges, version, new SettingsCollectListener() {
				@Override
				public void onSettingsCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items) {
					hideProgress();
					if (succeed) {
						List<SettingsItem> pluginIndependentItems = new ArrayList<>();
						List<PluginSettingsItem> pluginSettingsItems = new ArrayList<>();
						for (SettingsItem item : items) {
							if (item instanceof PluginSettingsItem) {
								pluginSettingsItems.add((PluginSettingsItem) item);
							} else if (Algorithms.isEmpty(item.getPluginId())) {
								pluginIndependentItems.add(item);
							}
						}
						for (PluginSettingsItem pluginItem : pluginSettingsItems) {
							handlePluginImport(pluginItem, file);
						}
						if (!pluginIndependentItems.isEmpty()) {
							if (settingsTypes == null) {
								FragmentActivity activity = activityRef.get();
								if (!silentImport && activity != null) {
									FragmentManager fragmentManager = activity.getSupportFragmentManager();
									ImportSettingsFragment.showInstance(fragmentManager, pluginIndependentItems, file);
								}
							} else {
								Map<ExportSettingsType, List<?>> allSettingsMap = getSettingsToOperate(pluginIndependentItems, false);
								List<SettingsItem> settingsList = settingsHelper.getFilteredSettingsItems(allSettingsMap, settingsTypes, pluginIndependentItems, false);
								settingsHelper.checkDuplicates(file, settingsList, settingsList, getDuplicatesListener(file, replace));
							}
						}
					} else if (empty) {
						app.showShortToastMessage(app.getString(R.string.file_import_error, name, app.getString(R.string.shared_string_unexpected_error)));
					}
				}
			});
		} else {
			hideProgress();
			app.showShortToastMessage(app.getString(R.string.file_import_error, name, error));
		}
	}

	private CheckDuplicatesListener getDuplicatesListener(final File file, final boolean replace) {
		return new CheckDuplicatesListener() {
			@Override
			public void onDuplicatesChecked(@NonNull List<Object> duplicates, List<SettingsItem> items) {
				if (replace) {
					for (SettingsItem item : items) {
						item.setShouldReplace(true);
					}
				}
				app.getSettingsHelper().importSettings(file, items, "", 1, getImportListener(file));
			}
		};
	}

	private SettingsImportListener getImportListener(final File file) {
		return new SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
				if (succeed) {
					app.getRendererRegistry().updateExternalRenderers();
					app.getPoiFilters().loadSelectedPoiFilters();
					AppInitializer.loadRoutingFiles(app, null);
					FragmentActivity activity = activityRef.get();
					AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
					if (plugin != null) {
						plugin.indexingFiles(null, true, true);
					}
					if (activity instanceof MapActivity) {
						((MapActivity) activity).getMapLayers().getMapWidgetRegistry().updateVisibleWidgets();
						((MapActivity) activity).updateApplicationModeSettings();
					}
					if (!silentImport && file != null && activity != null) {
						FragmentManager fm = activity.getSupportFragmentManager();
						ImportCompleteFragment.showInstance(fm, items, file.getName(), needRestart);
					}
				}
			}
		};
	}


	private void handlePluginImport(final PluginSettingsItem pluginItem, final File file) {
		FragmentActivity activity = activityRef.get();
		final ProgressDialog progress;
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			progress = new ProgressDialog(activity);
			progress.setTitle(app.getString(R.string.loading_smth, ""));
			progress.setMessage(app.getString(R.string.importing_from, pluginItem.getPublicName(app)));
			progress.setIndeterminate(true);
			progress.setCancelable(false);
			progress.show();
		} else {
			progress = null;
		}

		final SettingsImportListener importListener = new SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
				FragmentActivity activity = activityRef.get();
				if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				AudioVideoNotesPlugin pluginAudioVideo = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
				if (pluginAudioVideo != null) {
					pluginAudioVideo.indexingFiles(null, true, true);
				}
				CustomOsmandPlugin plugin = pluginItem.getPlugin();
				plugin.loadResources();

				if (!Algorithms.isEmpty(plugin.getDownloadMaps())) {
					app.getDownloadThread().runReloadIndexFilesSilent();
				}
				if (!Algorithms.isEmpty(plugin.getRendererNames())) {
					app.getRendererRegistry().updateExternalRenderers();
				}
				if (!Algorithms.isEmpty(plugin.getRouterNames())) {
					loadRoutingFiles(app, null);
				}
				if (!silentImport && activity != null) {
					plugin.onInstall(app, activity);
				}
				String pluginId = pluginItem.getPluginId();
				File pluginDir = app.getAppPath(IndexConstants.PLUGINS_DIR + pluginId);
				if (!pluginDir.exists()) {
					pluginDir.mkdirs();
				}
				app.getSettingsHelper().exportSettings(pluginDir, "items", null, items, false);
			}
		};
		List<SettingsItem> pluginItems = new ArrayList<>(pluginItem.getPluginDependentItems());
		pluginItems.add(0, pluginItem);
		app.getSettingsHelper().checkDuplicates(file, pluginItems, pluginItems, new CheckDuplicatesListener() {
			@Override
			public void onDuplicatesChecked(@NonNull List<Object> duplicates, List<SettingsItem> items) {
				for (SettingsItem item : items) {
					item.setShouldReplace(true);
				}
				app.getSettingsHelper().importSettings(file, items, "", 1, importListener);
			}
		});
	}
}