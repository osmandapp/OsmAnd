package net.osmand.plus.activities;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.osmand.CallbackWithObject;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.DrivingRegion;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.SuggestExternalDirectoryDialog;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.view.Window;

public class SettingsGeneralActivity extends SettingsBaseActivity {

	private Preference applicationDir;
	private ListPreference applicationModePreference;
	private ListPreference drivingRegionPreference;

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setSupportProgressBarIndeterminateVisibility(false);
		getSupportActionBar().setTitle(R.string.global_app_settings);
		addPreferencesFromResource(R.xml.general_settings);
		String[] entries;
		String[] entrieValues;
		PreferenceScreen screen = getPreferenceScreen();
		settings = getMyApplication().getSettings();
		
		if (!Version.isBlackberry(getMyApplication())) {
			CheckBoxPreference nativeCheckbox = createCheckBoxPreference(settings.SAFE_MODE, R.string.safe_mode,
					R.string.safe_mode_description);
			// disable the checkbox if the library cannot be used
			if ((NativeOsmandLibrary.isLoaded() && !NativeOsmandLibrary.isSupported()) || settings.NATIVE_RENDERING_FAILED.get()) {
				nativeCheckbox.setEnabled(false);
				nativeCheckbox.setChecked(true);
			}
			screen.addPreference(nativeCheckbox);

			applicationDir = new Preference(this);
			applicationDir.setTitle(R.string.application_dir);
			applicationDir.setKey("external_storage_dir");
			applicationDir.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public void showOtherDialog(){
					AlertDialog.Builder editalert = new AlertDialog.Builder(SettingsGeneralActivity.this);
					editalert.setTitle(R.string.application_dir);
					final EditText input = new EditText(SettingsGeneralActivity.this);
					input.setText(settings.getExternalStorageDirectory().getAbsolutePath());
					input.setPadding(3, 3, 3, 3);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					        LinearLayout.LayoutParams.MATCH_PARENT,
					        LinearLayout.LayoutParams.MATCH_PARENT);
					input.setLayoutParams(lp);
					settings.getExternalStorageDirectory().getAbsolutePath();
					editalert.setView(input);
					editalert.setNegativeButton(R.string.default_buttons_cancel, null);
					editalert.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog, int whichButton) {
					    	warnAboutChangingStorage(input.getText().toString());
					    }
					});
					editalert.show();
				}
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SuggestExternalDirectoryDialog.showDialog(SettingsGeneralActivity.this, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							showOtherDialog();
						}
					}, new CallbackWithObject<String>() {
						
						@Override
						public boolean processResult(String result) {
							warnAboutChangingStorage(result);
							return true;
						}
					});
					return false;
				}
			});
			screen.addPreference(applicationDir);
		}
		
		screen.addPreference(createCheckBoxPreference(settings.USE_KALMAN_FILTER_FOR_COMPASS, R.string.use_kalman_filter_compass, R.string.use_kalman_filter_compass_descr));
		
		registerBooleanPreference(settings.USE_ENGLISH_NAMES, screen);

		
		// List preferences
		registerListPreference(settings.ROTATE_MAP, screen, 
				new String[]{getString(R.string.rotate_map_none_opt), getString(R.string.rotate_map_bearing_opt), getString(R.string.rotate_map_compass_opt)},
				new Integer[]{OsmandSettings.ROTATE_MAP_NONE, OsmandSettings.ROTATE_MAP_BEARING, OsmandSettings.ROTATE_MAP_COMPASS});
		
		registerListPreference(settings.MAP_SCREEN_ORIENTATION, screen, 
				new String[] {getString(R.string.map_orientation_portrait), getString(R.string.map_orientation_landscape), getString(R.string.map_orientation_default)},
				new Integer[] {ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED});
		
		registerListPreference(
				settings.OSMAND_THEME, screen,
				new String[] { "Dark", "Light", "Dark ActionBar" }, new Integer[] { OsmandSettings.OSMAND_DARK_THEME,
						OsmandSettings.OSMAND_LIGHT_THEME, OsmandSettings.OSMAND_LIGHT_DARK_ACTIONBAR_THEME });
		DrivingRegion[] drs  = DrivingRegion.values();
		entries = new String[drs.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = getString(drs[i].name);
		}
		registerListPreference(settings.DRIVING_REGION, screen, entries, drs);
		
		MetricsConstants[] mvls  = MetricsConstants.values();
		entries = new String[mvls.length];
		for(int i=0; i<entries.length; i++){
			entries[i] = mvls[i].toHumanString(getMyApplication());
		}
		registerListPreference(settings.METRIC_SYSTEM, screen, entries, mvls);
		
		String incompleteSuffix = " (" + getString(R.string.incomplete_locale) + ")";
		//getResources().getAssets().getLocales();
		entrieValues = new String[] { "",
				"en", "af", "hy", "eu", "be", "bs", "bg",
				"ca", "cs",  "da", "nl", "fi", "fr", "ka",
				"de", "el", "iw", "hi", "hu", "id",
				"it", "ja", "ko", "lv", "lt", "mr",
				"no", "pl", "pt", "ro", "ru", "sk",
				"sl", "es", "sv", "tr", "uk", "vi",
				"cy" };
		entries = new String[] { getString(R.string.system_locale), 
				"English", "Afrikaans", "Armenian" + incompleteSuffix, "Basque" + incompleteSuffix, "Belarusian" + incompleteSuffix, "Bosnian" + incompleteSuffix, "Bulgarian" + incompleteSuffix,
				"Catalan", "Czech", "Danish", "Dutch", "Finnish" + incompleteSuffix, "French", "Georgian",
				"German", "Greek", "Hebrew", "Hindi" + incompleteSuffix, "Hungarian", "Indonesian" + incompleteSuffix,
				"Italian", "Japanese" + incompleteSuffix, "Korean" + incompleteSuffix, "Latvian", "Lithuanian", "Marathi" +incompleteSuffix,
				"Norwegian" + incompleteSuffix, "Polish", "Portuguese", "Romanian", "Russian", "Slovak",
				"Slovenian", "Spanish", "Swedish", "Turkish" , "Ukrainian" , "Vietnamese",
				"Welsh" + incompleteSuffix };
		registerListPreference(settings.PREFERRED_LOCALE, screen, entries, entrieValues);

		
		
		ApplicationMode[] appModes = ApplicationMode.values(settings).toArray(new ApplicationMode[0]);
		entries = new String[appModes.length];
		for(int i=0; i<entries.length; i++){
			entries[i] = appModes[i].toHumanString(getMyApplication());
		}
		registerListPreference(settings.APPLICATION_MODE, screen, entries, appModes);
		
		if (!Version.isBlackberry((ClientContext) getApplication())) {
			PreferenceScreen cat = getPreferenceScreen();
			int nav = getResources().getConfiguration().navigation;
			if (nav == Configuration.NAVIGATION_DPAD || nav == Configuration.NAVIGATION_TRACKBALL || 
					nav == Configuration.NAVIGATION_WHEEL || 
					nav == Configuration.NAVIGATION_UNDEFINED) {
				cat.addPreference(createCheckBoxPreference(settings.USE_TRACKBALL_FOR_MOVEMENTS, R.string.use_trackball,
						R.string.use_trackball_descr));
			}
			
			ListPreference lp = createListPreference(
					settings.AUDIO_STREAM_GUIDANCE,
					new String[] { getString(R.string.voice_stream_music), getString(R.string.voice_stream_notification),
							getString(R.string.voice_stream_voice_call) }, new Integer[] { AudioManager.STREAM_MUSIC,
							AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_VOICE_CALL }, R.string.choose_audio_stream,
					R.string.choose_audio_stream_descr);
			final OnPreferenceChangeListener prev = lp.getOnPreferenceChangeListener();
			lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					prev.onPreferenceChange(preference, newValue);
					CommandPlayer player = getMyApplication().getPlayer();
					if (player != null) {
						player.updateAudioStream(settings.AUDIO_STREAM_GUIDANCE.get());
					}
					return true;
				}
			});
			cat.addPreference(lp);
		}
		
		applicationModePreference = (ListPreference) screen.findPreference(settings.APPLICATION_MODE.getId());
		applicationModePreference.setOnPreferenceChangeListener(this);
		drivingRegionPreference = (ListPreference) screen.findPreference(settings.DRIVING_REGION.getId());
    }



	private void updateApplicationDirTextAndSummary() {
		if(applicationDir != null) {
			String storageDir = settings.getExternalStorageDirectory().getAbsolutePath();
			applicationDir.setSummary(storageDir);
		}
	}

	public void updateAllSettings() {
		super.updateAllSettings();
		updateApplicationDirTextAndSummary();

		applicationModePreference.setTitle(getString(R.string.settings_preset) + "  ["
				+ settings.APPLICATION_MODE.get().toHumanString(getMyApplication()) + "]");
		drivingRegionPreference.setTitle(getString(R.string.driving_region) + "  ["
				+ getString(settings.DRIVING_REGION.get().name) + "]");
		// Too long
//		metricsAndConstantsPreference.setTitle(getString(R.string.unit_of_length) + "  ["
//				+ settings.METRIC_SYSTEM.get().toHumanString(getMyApplication()) + "]");
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String id = preference.getKey();
		super.onPreferenceChange(preference, newValue);
		if (id.equals(settings.SAFE_MODE.getId())) {
			if ((Boolean) newValue) {
				loadNativeLibrary();
			}
		} else if (preference == applicationDir) {
			warnAboutChangingStorage((String) newValue);
			return false;
		} else if (id.equals(settings.APPLICATION_MODE.getId())) {
			settings.DEFAULT_APPLICATION_MODE.set(settings.APPLICATION_MODE.get());
			updateAllSettings();
		} else if (id.equals(settings.PREFERRED_LOCALE.getId())) {
			// restart application to update locale
			getMyApplication().checkPrefferedLocale();
			Intent intent = getIntent();
			finish();
			startActivity(intent);
		} else if (id.equals(settings.OSMAND_THEME.getId())) {
			Intent intent = getIntent();
			finish();
			startActivity(intent);
		} else {
			updateAllSettings();
		}
		return true;
	}
	
	public static class MoveFilesToDifferentDirectory extends AsyncTask<Void, Void, Boolean> {

		private File to;
		private Context ctx;
		private File from;
		protected ProgressDialogImplementation progress;
		private Runnable runOnSuccess;

		public MoveFilesToDifferentDirectory(Context ctx, File from, File to) {
			this.ctx = ctx;
			this.from = from;
			this.to = to;
		}
		
		public void setRunOnSuccess(Runnable runOnSuccess) {
			this.runOnSuccess = runOnSuccess;
		}
		
		@Override
		protected void onPreExecute() {
			progress = ProgressDialogImplementation.createProgressDialog(
					ctx, ctx.getString(R.string.copying_osmand_files),
					ctx.getString(R.string.copying_osmand_files_descr, to.getPath()),
					ProgressDialog.STYLE_HORIZONTAL);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (result != null) {
				if (result.booleanValue() && runOnSuccess != null) {
					runOnSuccess.run();
				} else if (!result.booleanValue()) {
					Toast.makeText(ctx, R.string.input_output_error, Toast.LENGTH_LONG).show();
				}
			}
			if(progress.getDialog().isShowing()) {
				progress.getDialog().dismiss();
			}
		}
		
		private void movingFiles(File f, File t, int depth) throws IOException {
			if(depth <= 2) {
				progress.startTask(ctx.getString(R.string.copying_osmand_one_file_descr, t.getName()), -1);
			}
			if (f.isDirectory()) {
				t.mkdirs();
				File[] lf = f.listFiles();
				if (lf != null) {
					for (int i = 0; i < lf.length; i++) {
						if (lf[i] != null) {
							movingFiles(lf[i], new File(t, lf[i].getName()), depth + 1);
						}
					}
				}
				f.delete();
			} else if (f.isFile()) {
				if(t.exists()) {
					Algorithms.removeAllFiles(t);
				}
				boolean rnm = false;
				try {
					rnm = f.renameTo(t);
				} catch(RuntimeException e) {
				}
				if (!rnm) {
					FileInputStream fin = new FileInputStream(f);
					FileOutputStream fout = new FileOutputStream(t);
					try {
						Algorithms.streamCopy(fin, fout);
					} finally {
						fin.close();
						fout.close();
					}
					f.delete();
				}
			}
			if(depth <= 2) {
				progress.finishTask();
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			to.mkdirs();
			try {
				movingFiles(from, to, 0);
			} catch (IOException e) {
				return false;
			}
			return true;
		}
		
	}

	private void warnAboutChangingStorage(final String newValue) {
		final String newDir = newValue != null ? newValue.trim() : newValue;
		File path = new File(newDir);
		path.mkdirs();
		if (!path.canRead() || !path.exists()) {
			AccessibleToast.makeText(this, R.string.specified_dir_doesnt_exist, Toast.LENGTH_LONG).show();
			return;
		}
		Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.application_dir_change_warning2));
		builder.setPositiveButton(R.string.default_buttons_yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				MoveFilesToDifferentDirectory task =
						new MoveFilesToDifferentDirectory(SettingsGeneralActivity.this, 
						new File(settings.getExternalStorageDirectory(), IndexConstants.APP_DIR), new File(newDir,
								IndexConstants.APP_DIR));
				task.setRunOnSuccess(new Runnable() {
					@Override
					public void run() {
						updateSettingsToNewDir(newDir);						
					}
				});
				task.execute();
			}
		});
		builder.setNeutralButton(R.string.default_buttons_no, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				updateSettingsToNewDir(newDir);								
			}
		});
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.show();
	}
	
	private void updateSettingsToNewDir(final String newDir) {
		// edit the preference
		settings.setExternalStorageDirectory(newDir);
		getMyApplication().getResourceManager().resetStoreDirectory();
		reloadIndexes();
		updateApplicationDirTextAndSummary();
	}

	public void reloadIndexes() {
		setSupportProgressBarIndeterminateVisibility(true);
		final CharSequence oldTitle = getSupportActionBar().getTitle();
		getSupportActionBar(). setTitle(getString(R.string.loading_data));
		getSupportActionBar().setSubtitle(getString(R.string.reading_indexes));
		new AsyncTask<Void, Void, List<String>>() {

			@Override
			protected List<String> doInBackground(Void... params) {
				return getMyApplication().getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS);
			}
			
			protected void onPostExecute(List<String> result) {
				showWarnings(result);
				getSupportActionBar().setTitle(oldTitle);
				getSupportActionBar().setSubtitle("");
				setSupportProgressBarIndeterminateVisibility(false);
			};
			
		}.execute();
	}

	public void loadNativeLibrary() {
		if (!NativeOsmandLibrary.isLoaded()) {
			final RenderingRulesStorage storage = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected void onPreExecute() {
					setSupportProgressBarIndeterminateVisibility(true);
				};

				@Override
				protected Void doInBackground(Void... params) {
					NativeOsmandLibrary.getLibrary(storage, getMyApplication());
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					setSupportProgressBarIndeterminateVisibility(false);
					if (!NativeOsmandLibrary.isNativeSupported(storage, getMyApplication())) {
						AccessibleToast.makeText(SettingsGeneralActivity.this, R.string.native_library_not_supported, Toast.LENGTH_LONG).show();
					}
				};
			}.execute();
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
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AccessibleToast.makeText(SettingsGeneralActivity.this, b.toString(), Toast.LENGTH_LONG).show();

				}
			});
		}
	}


	
}
