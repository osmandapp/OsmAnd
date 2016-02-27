package net.osmand.plus.osmedit;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;

public class SettingsOsmEditingActivity extends SettingsBaseActivity {


	@Override
	public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.osm_settings);
		@SuppressWarnings("deprecation")
		PreferenceScreen grp = getPreferenceScreen();

		DialogPreference loginDialogPreference = new OsmLoginDataDialogPreference(this, null);
		grp.addPreference(loginDialogPreference);

		CheckBoxPreference poiEdit = createCheckBoxPreference(settings.OFFLINE_EDITION,
				R.string.offline_edition, R.string.offline_edition_descr);
		grp.addPreference(poiEdit);

		Preference pref = new Preference(this);
		pref.setTitle(R.string.local_openstreetmap_settings);
		pref.setSummary(R.string.local_openstreetmap_settings_descr);
		pref.setKey("local_openstreetmap_points");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
				final Intent favorites = new Intent(SettingsOsmEditingActivity.this,
						appCustomization.getFavoritesActivity());
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().FAVORITES_TAB.set(R.string.osm_edits);
				startActivity(favorites);
				return true;
			}
		});
		grp.addPreference(pref);
    }

	public class OsmLoginDataDialogPreference extends DialogPreference {
		private TextView userNameEditText;
		private TextView passwordEditText;

		public OsmLoginDataDialogPreference(Context context, AttributeSet attrs) {
			super(context, attrs);

			setDialogLayoutResource(R.layout.osm_user_login_details);
			setPositiveButtonText(android.R.string.ok);
			setNegativeButtonText(android.R.string.cancel);
			setDialogTitle(R.string.open_street_map_login_and_pass);

			setTitle(R.string.open_street_map_login_and_pass);
			setSummary(R.string.open_street_map_login_descr);

			setDialogIcon(null);
		}

		@Override
		protected void onBindDialogView(View view) {
			userNameEditText = (TextView) view.findViewById(R.id.user_name_field);
			userNameEditText.setText(settings.USER_NAME.get());
			passwordEditText = (TextView) view.findViewById(R.id.password_field);
			passwordEditText.setText(settings.USER_PASSWORD.get());
			super.onBindDialogView(view);
		}

		@Override
		protected void onDialogClosed(boolean positiveResult) {
			if (positiveResult) {
				settings.USER_NAME.set(userNameEditText.getText().toString());
				settings.USER_PASSWORD.set(passwordEditText.getText().toString());
				new ValidateOsmLoginDetailsTask(SettingsOsmEditingActivity.this).execute();
			}
		}
	}

	private static class ValidateOsmLoginDetailsTask extends AsyncTask<Void, Void, OsmBugsUtil.OsmBugResult> {
		private final Context context;

		public ValidateOsmLoginDetailsTask(Context context) {
			this.context = context;
		}

		@Override
		protected OsmBugsUtil.OsmBugResult doInBackground(Void... params) {
			OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
			assert plugin != null;
			OsmBugsRemoteUtil remoteUtil = plugin.getOsmNotesRemoteUtil();
			return remoteUtil.validateLoginDetails();
		}

		@Override
		protected void onPostExecute(OsmBugsUtil.OsmBugResult osmBugResult) {
			if (osmBugResult.warning != null) {
				Toast.makeText(context, osmBugResult.warning, Toast.LENGTH_LONG).show();
			}
		}
	}
}
