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
import net.osmand.plus.CustomOsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.SettingsHelper.CheckDuplicatesListener;
import net.osmand.plus.settings.backend.SettingsHelper.PluginSettingsItem;
import net.osmand.plus.settings.backend.SettingsHelper.ProfileSettingsItem;
import net.osmand.plus.settings.backend.SettingsHelper.SettingsCollectListener;
import net.osmand.plus.settings.backend.SettingsHelper.SettingsImportListener;
import net.osmand.plus.settings.backend.SettingsHelper.SettingsItem;
import net.osmand.plus.settings.fragments.ImportSettingsFragment;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.AppInitializer.loadRoutingFiles;

class SettingsImportTask extends BaseImportAsyncTask<Void, Void, String> {

	private Uri uri;
	private String name;
	private String latestChanges;
	private int version;
	private CallbackWithObject<List<SettingsItem>> callback;

	public SettingsImportTask(@NonNull FragmentActivity activity, @NonNull Uri uri,
							  @NonNull String name, String latestChanges, int version,
							  CallbackWithObject<List<SettingsItem>> callback) {
		super(activity);
		this.uri = uri;
		this.name = name;
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
			app.getSettingsHelper().collectSettings(file, latestChanges, version, new SettingsCollectListener() {
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
							FragmentActivity activity = activityRef.get();
							if (activity != null) {
								FragmentManager fragmentManager = activity.getSupportFragmentManager();
								ImportSettingsFragment.showInstance(fragmentManager, pluginIndependentItems, file);
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
			public void onSettingsImportFinished(boolean succeed, @NonNull List<SettingsItem> items) {
				FragmentActivity activity = activityRef.get();
				if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				CustomOsmandPlugin plugin = pluginItem.getPlugin();
				plugin.loadResources();

				for (SettingsItem item : items) {
					if (item instanceof ProfileSettingsItem) {
						((ProfileSettingsItem) item).applyAdditionalPrefs();
					}
				}
				if (!Algorithms.isEmpty(plugin.getDownloadMaps())) {
					app.getDownloadThread().runReloadIndexFilesSilent();
				}
				if (!Algorithms.isEmpty(plugin.getRendererNames())) {
					app.getRendererRegistry().updateExternalRenderers();
				}
				if (!Algorithms.isEmpty(plugin.getRouterNames())) {
					loadRoutingFiles(app, null);
				}
				if (activity != null) {
					plugin.onInstall(app, activity);
				}
				String pluginId = pluginItem.getPluginId();
				File pluginDir = new File(app.getAppPath(null), IndexConstants.PLUGINS_DIR + pluginId);
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