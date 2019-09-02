package net.osmand.plus.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.StateChangedListener;
import net.osmand.ValueHolder;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.dashboard.DashChooseAppDirFragment;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.dialogs.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GlobalSettingsFragment extends BaseSettingsFragment {

	public static final String TAG = "GlobalSettingsFragment";

	private Preference applicationDir;
	private boolean permissionRequested;
	private boolean permissionGranted;

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
		setupDoNotShowStartupMessagesPref();
		setupEnableProxyPref();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.ProgressBar), false);

		return view;
	}

	private void setupDefaultAppModePref() {
		ApplicationMode selectedMode = getSelectedAppMode();

		int iconRes = selectedMode.getIconRes();
		String title = selectedMode.toHumanString(getContext());

		ApplicationMode[] appModes = ApplicationMode.values(app).toArray(new ApplicationMode[0]);
		String[] entries = new String[appModes.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = appModes[i].toHumanString(app);
		}

		final ListPreferenceEx defaultApplicationMode = (ListPreferenceEx) findPreference(settings.DEFAULT_APPLICATION_MODE.getId());
		defaultApplicationMode.setIcon(getContentIcon(iconRes));
		defaultApplicationMode.setSummary(title);
		defaultApplicationMode.setEntries(entries);
		defaultApplicationMode.setEntryValues(appModes);
	}

	private void setupPreferredLocalePref() {
		// See language list and statistics at: https://hosted.weblate.org/projects/osmand/main/
		// Hardy maintenance 2016-05-29:
		//  - Include languages if their translation is >= ~10%    (but any language will be visible if it is the device's system locale)
		//  - Mark as "incomplete" if                    < ~80%
		String incompleteSuffix = " (" + getString(R.string.incomplete_locale) + ")";

		// Add " (Device language)" to system default entry in Latin letters, so it can be more easily identified if a foreign language has been selected by mistake
		String latinSystemDefaultSuffix = " (" + getString(R.string.system_locale_no_translate) + ")";

		//getResources().getAssets().getLocales();
		String[] entrieValues = new String[] {"",
				"en",
				"af",
				"ar",
				"ast",
				"az",
				"be",
				//"be_BY",
				"bg",
				"ca",
				"cs",
				"cy",
				"da",
				"de",
				"el",
				"en_GB",
				"eo",
				"es",
				"es_AR",
				"es_US",
				"eu",
				"fa",
				"fi",
				"fr",
				"gl",
				"he",
				"hr",
				"hsb",
				"hu",
				"hy",
				"is",
				"it",
				"ja",
				"ka",
				"kab",
				"kn",
				"ko",
				"lt",
				"lv",
				"ml",
				"mr",
				"nb",
				"nl",
				"nn",
				"oc",
				"pl",
				"pt",
				"pt_BR",
				"ro",
				"ru",
				"sc",
				"sk",
				"sl",
				"sr",
				"sr+Latn",
				"sv",
				"tr",
				"uk",
				"vi",
				"zh_CN",
				"zh_TW"};

		String[] entries = new String[] {getString(R.string.system_locale) + latinSystemDefaultSuffix,
				getString(R.string.lang_en),
				getString(R.string.lang_af) + incompleteSuffix,
				getString(R.string.lang_ar),
				getString(R.string.lang_ast) + incompleteSuffix,
				getString(R.string.lang_az),
				getString(R.string.lang_be),
				// getString(R.string.lang_be_by),
				getString(R.string.lang_bg),
				getString(R.string.lang_ca),
				getString(R.string.lang_cs),
				getString(R.string.lang_cy) + incompleteSuffix,
				getString(R.string.lang_da),
				getString(R.string.lang_de),
				getString(R.string.lang_el) + incompleteSuffix,
				getString(R.string.lang_en_gb),
				getString(R.string.lang_eo),
				getString(R.string.lang_es),
				getString(R.string.lang_es_ar),
				getString(R.string.lang_es_us),
				getString(R.string.lang_eu),
				getString(R.string.lang_fa),
				getString(R.string.lang_fi) + incompleteSuffix,
				getString(R.string.lang_fr),
				getString(R.string.lang_gl),
				getString(R.string.lang_he) + incompleteSuffix,
				getString(R.string.lang_hr) + incompleteSuffix,
				getString(R.string.lang_hsb) + incompleteSuffix,
				getString(R.string.lang_hu),
				getString(R.string.lang_hy),
				getString(R.string.lang_is),
				getString(R.string.lang_it),
				getString(R.string.lang_ja),
				getString(R.string.lang_ka) + incompleteSuffix,
				getString(R.string.lang_kab) + incompleteSuffix,
				getString(R.string.lang_kn) + incompleteSuffix,
				getString(R.string.lang_ko),
				getString(R.string.lang_lt),
				getString(R.string.lang_lv),
				getString(R.string.lang_ml) + incompleteSuffix,
				getString(R.string.lang_mr) + incompleteSuffix,
				getString(R.string.lang_nb),
				getString(R.string.lang_nl),
				getString(R.string.lang_nn) + incompleteSuffix,
				getString(R.string.lang_oc) + incompleteSuffix,
				getString(R.string.lang_pl),
				getString(R.string.lang_pt),
				getString(R.string.lang_pt_br),
				getString(R.string.lang_ro) + incompleteSuffix,
				getString(R.string.lang_ru),
				getString(R.string.lang_sc),
				getString(R.string.lang_sk),
				getString(R.string.lang_sl),
				getString(R.string.lang_sr) + incompleteSuffix,
				getString(R.string.lang_sr_latn) + incompleteSuffix,
				getString(R.string.lang_sv),
				getString(R.string.lang_tr),
				getString(R.string.lang_uk),
				getString(R.string.lang_vi) + incompleteSuffix,
				getString(R.string.lang_zh_cn) + incompleteSuffix,
				getString(R.string.lang_zh_tw)};

		String[] valuesPl = ConfigureMapMenu.getSortedMapNamesIds(getContext(), entries, entries);
		String[] idsPl = ConfigureMapMenu.getSortedMapNamesIds(getContext(), entrieValues, entries);

		ListPreferenceEx preferredLocale = (ListPreferenceEx) findPreference(settings.PREFERRED_LOCALE.getId());
		preferredLocale.setIcon(getContentIcon(R.drawable.ic_action_map_language));
		preferredLocale.setSummary(settings.PREFERRED_LOCALE.get());
		preferredLocale.setEntries(valuesPl);
		preferredLocale.setEntryValues(idsPl);

		// Add " (Display language)" to menu title in Latin letters for all non-en languages
		if (!getResources().getString(R.string.preferred_locale).equals(getResources().getString(R.string.preferred_locale_no_translate))) {
			preferredLocale.setTitle(getString(R.string.preferred_locale) + " (" + getString(R.string.preferred_locale_no_translate) + ")");
		}

		// This setting now only in "Confgure map" menu
		//String[] values = ConfigureMapMenu.getMapNamesValues(this, ConfigureMapMenu.mapNamesIds);
		//String[] ids = ConfigureMapMenu.getSortedMapNamesIds(this, ConfigureMapMenu.mapNamesIds, values);
		//registerListPreference(settings.MAP_PREFERRED_LOCALE, screen, ConfigureMapMenu.getMapNamesValues(this, ids), ids);

		settings.PREFERRED_LOCALE.addListener(new StateChangedListener<String>() {
			@Override
			public void stateChanged(String change) {
				// recreate activity to update locale
				Activity activity = getActivity();
				OsmandApplication app = getMyApplication();
				if (app != null && activity != null) {
					app.checkPreferredLocale();
					activity.recreate();
				}
			}
		});
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

	private void setupSendAnonymousDataPref() {
		SwitchPreferenceEx sendAnonymousData = (SwitchPreferenceEx) findPreference(settings.SEND_ANONYMOUS_DATA.getId());
		sendAnonymousData.setSummaryOn(R.string.shared_string_on);
		sendAnonymousData.setSummaryOff(R.string.shared_string_off);
	}

	private void setupDoNotShowStartupMessagesPref() {
		SwitchPreference doNotShowStartupMessages = (SwitchPreference) findPreference(settings.DO_NOT_SHOW_STARTUP_MESSAGES.getId());
		doNotShowStartupMessages.setSummaryOn(R.string.shared_string_on);
		doNotShowStartupMessages.setSummaryOff(R.string.shared_string_off);
	}

	private void setupEnableProxyPref() {
		SwitchPreferenceEx enableProxy = (SwitchPreferenceEx) findPreference(settings.ENABLE_PROXY.getId());
		enableProxy.setIcon(getContentIcon(R.drawable.ic_action_proxy));
		enableProxy.setSummaryOn(R.string.shared_string_on);
		enableProxy.setSummaryOff(R.string.shared_string_off);
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (preference.getKey().equals(settings.SEND_ANONYMOUS_DATA.getId())) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SendAnalyticsBottomSheetDialogFragment.showInstance(app, fragmentManager);
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
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
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();

		if (prefId.equals(settings.DEFAULT_APPLICATION_MODE.getId()) && newValue instanceof ApplicationMode) {
			preference.setIcon(getContentIcon(((ApplicationMode) newValue).getIconRes()));
			settings.APPLICATION_MODE.set((ApplicationMode) newValue);
		}

		return super.onPreferenceChange(preference, newValue);
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
				showWarnings(result);
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

	protected void showWarnings(List<String> warnings) {
		if (!warnings.isEmpty()) {
			final StringBuilder b = new StringBuilder();
			boolean f = true;
			for (String w : warnings) {
				if (f) {
					f = false;
				} else {
					b.append('\n');
				}
				b.append(w);
			}
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getContext(), b.toString(), Toast.LENGTH_LONG).show();
				}
			});
		}
	}

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
}