package net.osmand.plus.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StatFs;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.ValueHolder;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsGeneralActivity;
import net.osmand.plus.dashboard.DashChooseAppDirFragment;
import net.osmand.plus.dialogs.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GlobalSettingsFragment extends BaseSettingsFragment implements SendAnalyticsBottomSheetDialogFragment.OnSendAnalyticsPrefsUpdate, OnPreferenceChanged {

	public static final String TAG = "GlobalSettingsFragment";

	private static final String SEND_ANONYMOUS_DATA_PREF_ID = "send_anonymous_data";

	@Override
	protected int getPreferencesResId() {
		return R.xml.global_settings;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.global_preference_toolbar;
	}

	@Override
	protected String getToolbarTitle() {
		return getString(R.string.osmand_settings);
	}

	@Override
	protected void setupPreferences() {
		setupDefaultAppModePref();
		setupPreferredLocalePref();
		setupExternalStorageDirPref();

		setupSendAnonymousDataPref();
		setupEnableProxyPref();
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();

		if (prefId.equals(SEND_ANONYMOUS_DATA_PREF_ID)) {
			if (newValue instanceof Boolean) {
				boolean enabled = (Boolean) newValue;
				if (enabled) {
					FragmentManager fragmentManager = getFragmentManager();
					if (fragmentManager != null) {
						SendAnalyticsBottomSheetDialogFragment.showInstance(app, fragmentManager, this);
					}
				} else {
					settings.SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.set(false);
					settings.SEND_ANONYMOUS_APP_USAGE_DATA.set(false);
					return true;
				}
			}
			return false;
		}

		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();

		if (prefId.equals(OsmandSettings.EXTERNAL_STORAGE_DIR)) {
			showAppDirDialog();
			return true;
		}

		return super.onPreferenceClick(preference);
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		String prefId = preference.getKey();

		if (prefId.equals(SEND_ANONYMOUS_DATA_PREF_ID)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SendAnalyticsBottomSheetDialogFragment.showInstance(app, fragmentManager, this);
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		if (prefId.equals(settings.DEFAULT_APPLICATION_MODE.getId())) {
			setupDefaultAppModePref();
		} else if (prefId.equals(settings.PREFERRED_LOCALE.getId())) {
			// recreate activity to update locale
			Activity activity = getActivity();
			OsmandApplication app = getMyApplication();
			if (app != null && activity != null) {
				app.checkPreferredLocale();
				activity.recreate();
			}
		}
	}

	@Override
	public void onAnalyticsPrefsUpdate() {
		setupSendAnonymousDataPref();
	}

	private void setupDefaultAppModePref() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		ApplicationMode selectedMode = settings.DEFAULT_APPLICATION_MODE.get();

		ApplicationMode[] appModes = ApplicationMode.values(app).toArray(new ApplicationMode[0]);
		String[] entries = new String[appModes.length];
		String[] entryValues = new String[appModes.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = appModes[i].toHumanString(app);
			entryValues[i] = appModes[i].getStringKey();
		}

		ListPreferenceEx defaultApplicationMode = (ListPreferenceEx) findPreference(settings.DEFAULT_APPLICATION_MODE.getId());
		defaultApplicationMode.setIcon(getContentIcon(selectedMode.getIconRes()));
		defaultApplicationMode.setEntries(entries);
		defaultApplicationMode.setEntryValues(entryValues);
	}

	private void setupPreferredLocalePref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		ListPreferenceEx preferredLocale = (ListPreferenceEx) findPreference(settings.PREFERRED_LOCALE.getId());
		preferredLocale.setIcon(getContentIcon(R.drawable.ic_action_map_language));
		preferredLocale.setSummary(settings.PREFERRED_LOCALE.get());

		Pair<String[], String[]> preferredLocaleInfo = SettingsGeneralActivity.getPreferredLocaleIdsAndValues(ctx);
		if (preferredLocaleInfo != null) {
			preferredLocale.setEntries(preferredLocaleInfo.first);
			preferredLocale.setEntryValues(preferredLocaleInfo.second);
		}

		// Add " (Display language)" to menu title in Latin letters for all non-en languages
		if (!getResources().getString(R.string.preferred_locale).equals(getResources().getString(R.string.preferred_locale_no_translate))) {
			preferredLocale.setTitle(getString(R.string.preferred_locale) + " (" + getString(R.string.preferred_locale_no_translate) + ")");
		}
	}

	private void setupSendAnonymousDataPref() {
		boolean enabled = settings.SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.get() || settings.SEND_ANONYMOUS_APP_USAGE_DATA.get();

		SwitchPreference sendAnonymousData = (SwitchPreference) findPreference(SEND_ANONYMOUS_DATA_PREF_ID);
		sendAnonymousData.setChecked(enabled);
	}

	private void setupEnableProxyPref() {
		SwitchPreferenceEx enableProxy = (SwitchPreferenceEx) findPreference(settings.ENABLE_PROXY.getId());
		enableProxy.setIcon(getContentIcon(R.drawable.ic_action_proxy));
	}


//	-------------------------- APP DIR PREF --------------------------------------------------------

	private Preference applicationDir;
	private boolean permissionRequested;
	private boolean permissionGranted;

	@Override
	public void onResume() {
		super.onResume();
		if (permissionRequested) {
			showAppDirDialogV19();
			permissionRequested = false;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String permissions[], int[] grantResults) {
		permissionRequested = requestCode == DownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;
		if (permissionRequested
				&& grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			permissionGranted = true;
		} else {
			permissionGranted = false;
			Toast.makeText(getContext(),
					R.string.missing_write_external_storage_permission,
					Toast.LENGTH_LONG).show();
		}
	}

	private void setupExternalStorageDirPref() {
		applicationDir = findPreference(OsmandSettings.EXTERNAL_STORAGE_DIR);
		if (!Version.isBlackberry(app)) {
			int type;
			if (settings.getExternalStorageDirectoryTypeV19() >= 0) {
				type = settings.getExternalStorageDirectoryTypeV19();
			} else {
				ValueHolder<Integer> vh = new ValueHolder<Integer>();
				settings.getExternalStorageDirectory(vh);
				if (vh.value != null && vh.value >= 0) {
					type = vh.value;
				} else {
					type = 0;
				}
			}

			String appDirType = "";
			if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_INTERNAL_FILE) {
				appDirType = getString(R.string.storage_directory_internal_app);
			} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT) {
				appDirType = getString(R.string.storage_directory_shared);
			} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE) {
				appDirType = getString(R.string.storage_directory_external);
			} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_OBB) {
				appDirType = getString(R.string.storage_directory_multiuser);
			} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_SPECIFIED) {
				appDirType = getString(R.string.storage_directory_manual);
			}

			File currentAppFile = settings.getExternalStorageDirectory();

			applicationDir.setSummary(appDirType + " \u2022 " + getFreeSpace(currentAppFile));
			applicationDir.setIcon(getContentIcon(R.drawable.ic_action_folder));
		} else {
			getPreferenceScreen().removePreference(applicationDir);
		}
	}

	private String getFreeSpace(File dir) {
		if (dir.canRead()) {
			StatFs fs = new StatFs(dir.getAbsolutePath());
			return DownloadActivity.formatGb.format(new Object[] {(float) (fs.getAvailableBlocks()) * fs.getBlockSize() / (1 << 30)});
		}
		return "";
	}

	public void showAppDirDialog() {
		if (Build.VERSION.SDK_INT >= 19) {
			showAppDirDialogV19();
			return;
		}
		AlertDialog.Builder editalert = new AlertDialog.Builder(getContext());
		editalert.setTitle(R.string.application_dir);
		final EditText input = new EditText(getContext());
		input.setText(settings.getExternalStorageDirectory().getAbsolutePath());
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		lp.leftMargin = lp.rightMargin = 5;
		lp.bottomMargin = lp.topMargin = 5;
		input.setLayoutParams(lp);
		settings.getExternalStorageDirectory().getAbsolutePath();
		editalert.setView(input);
		editalert.setNegativeButton(R.string.shared_string_cancel, null);
		editalert.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				warnAboutChangingStorage(input.getText().toString());
			}
		});
		editalert.show();
	}

	private void showAppDirDialogV19() {
		AlertDialog.Builder bld = new AlertDialog.Builder(getContext());
		DashChooseAppDirFragment.ChooseAppDirFragment chooseAppDirFragment = new DashChooseAppDirFragment.ChooseAppDirFragment(getActivity(), (Dialog) null) {
			@Override
			protected void successCallback() {
				updateApplicationDirTextAndSummary();
			}
		};
		if (permissionRequested && !permissionGranted) {
			chooseAppDirFragment.setPermissionDenied();
		}
		bld.setView(chooseAppDirFragment.initView(getLayoutInflater(), null, null));
		AlertDialog dlg = bld.show();
		chooseAppDirFragment.setDialog(dlg);
	}

	private void warnAboutChangingStorage(final String newValue) {
		String newDir = newValue != null ? newValue.trim() : newValue;
		if (!newDir.replace('/', ' ').trim().
				toLowerCase().endsWith(IndexConstants.APP_DIR.replace('/', ' ').trim())) {
			newDir += "/" + IndexConstants.APP_DIR;
		}
		final File path = new File(newDir);
		path.mkdirs();
		if (!path.canRead() || !path.exists()) {
			Toast.makeText(getContext(), R.string.specified_dir_doesnt_exist, Toast.LENGTH_LONG).show();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setMessage(getString(R.string.application_dir_change_warning3));
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				DashChooseAppDirFragment.MoveFilesToDifferentDirectory task =
						new DashChooseAppDirFragment.MoveFilesToDifferentDirectory(getContext(),
								settings.getExternalStorageDirectory(), path);
				task.setRunOnSuccess(new Runnable() {
					@Override
					public void run() {
						updateSettingsToNewDir(path.getParentFile().getAbsolutePath());
					}
				});
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});
		builder.setNeutralButton(R.string.shared_string_no, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				updateSettingsToNewDir(path.getParentFile().getAbsolutePath());
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	private void updateSettingsToNewDir(final String newDir) {
		// edit the preference
		settings.setExternalStorageDirectoryPre19(newDir);
		app.getResourceManager().resetStoreDirectory();
		reloadIndexes();
		updateApplicationDirTextAndSummary();
	}

	private void updateApplicationDirTextAndSummary() {
		if (applicationDir != null) {
			String storageDir = settings.getExternalStorageDirectory().getAbsolutePath();
			applicationDir.setSummary(storageDir);
		}
	}

	public void reloadIndexes() {
		final Toolbar toolbar = getView().findViewById(R.id.toolbar);
		setProgressVisibility(true);
		final CharSequence oldTitle = toolbar.getTitle();
		toolbar.setTitle(getString(R.string.loading_data));
		toolbar.setSubtitle(getString(R.string.reading_indexes));
		new AsyncTask<Void, Void, List<String>>() {

			@Override
			protected List<String> doInBackground(Void... params) {
				return getMyApplication().getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS,
						new ArrayList<String>());
			}

			protected void onPostExecute(List<String> result) {
				SettingsBaseActivity.showWarnings(getMyApplication(), result);
				toolbar.setTitle(oldTitle);
				toolbar.setSubtitle("");
				setProgressVisibility(false);
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	protected void setProgressVisibility(boolean visibility) {
		if (visibility) {
			getView().findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
		} else {
			getView().findViewById(R.id.ProgressBar).setVisibility(View.GONE);
		}
	}
}