package net.osmand.plus.activities;


import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends SettingsBaseActivity {

	public static final String INTENT_KEY_SETTINGS_SCREEN = "INTENT_KEY_SETTINGS_SCREEN";
	public static final int SCREEN_GENERAL_SETTINGS = 1;
	public static final int SCREEN_NAVIGATION_SETTINGS = 2;

	private static final int PLUGINS_SELECTION_REQUEST = 1;
    private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";
	

	private Preference plugins;
	private Preference localIndexes;
	private Preference general;
	private Preference routing;
	private Preference about;
	private Preference version;
	private Preference help;


	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_pref);
		PreferenceScreen screen = getPreferenceScreen();
		localIndexes =(Preference) screen.findPreference("local_indexes");
		localIndexes.setOnPreferenceClickListener(this);
		plugins = (Preference) screen.findPreference("plugins");
		plugins.setOnPreferenceClickListener(this);
		general = (Preference) screen.findPreference("general_settings");
		general.setOnPreferenceClickListener(this);
		routing = (Preference) screen.findPreference("routing_settings");
		routing .setOnPreferenceClickListener(this);
		help = (Preference) screen.findPreference("help");
		help.setOnPreferenceClickListener(this);
		
		getToolbar().setTitle(Version.getFullVersion(getMyApplication()));
		
		Intent intent = getIntent();
		if(intent != null && intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0) != 0){
			int s = intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0);
			if(s == SCREEN_GENERAL_SETTINGS){
				startActivity(new Intent(this, SettingsGeneralActivity.class));
			} else if(s == SCREEN_NAVIGATION_SETTINGS){
				startActivity(new Intent(this, SettingsNavigationActivity.class));
			} 
		}
		if ((Version.isDeveloperVersion(getMyApplication())) &&
				OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) != null){
			version = new Preference(this);
			version.setOnPreferenceClickListener(this);
			version.setSummary(R.string.version_settings_descr);
			version.setTitle(R.string.version_settings);
			version.setKey("version");
			screen.addPreference(version);
		}
		about = new Preference(this);
		about.setOnPreferenceClickListener(this);
		about.setSummary(R.string.about_settings_descr);
		about.setTitle(R.string.about_settings);
		about.setKey("about");
		screen.addPreference(about);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == PLUGINS_SELECTION_REQUEST) && (resultCode == PluginsActivity.ACTIVE_PLUGINS_LIST_MODIFIED)) {
			finish();
			startActivity(getIntent());
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == localIndexes) {
			startActivity(new Intent(this, getMyApplication().getAppCustomization().getDownloadIndexActivity()));
			return true;
		} else if (preference == general) {
			startActivity(new Intent(this, SettingsGeneralActivity.class));
			return true;
		} else if (preference == help) {
			startActivity(new Intent(this, HelpActivity.class));
			return true;
		} else if (preference == routing) {
			startActivity(new Intent(this, SettingsNavigationActivity.class));
			return true;
		} else if (preference == about) {
			showAboutDialog(getMyApplication());
			return true;
		} else if (preference == plugins) {
			startActivityForResult(new Intent(this, getMyApplication().getAppCustomization().getPluginsActivity()), PLUGINS_SELECTION_REQUEST);
			return true;
		} else if (preference == version){
			final Intent mapIntent = new Intent(this, ContributionVersionActivity.class);
			this.startActivityForResult(mapIntent, 0);
		} else {
			super.onPreferenceClick(preference);
		}
		return false;
	}
	
	public void showAboutDialog(final OsmandApplication app) {
		final Dialog dialog = new Dialog(this, 
				app.getSettings().isLightContent() ?
						R.style.OsmandLightTheme:
							R.style.OsmandDarkTheme);
		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		Toolbar tb = new Toolbar(this);
		tb.setClickable(true);
		Drawable back = ((OsmandApplication)getApplication()).getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		tb.setNavigationIcon(back);
		tb.setTitle(R.string.about_settings);
		tb.setBackgroundColor(getResources().getColor( getResIdFromAttribute(this, R.attr.pstsTabBackground)));
		tb.setTitleTextColor(getResources().getColor(getResIdFromAttribute(this, R.attr.pstsTextColor)));
		tb.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.dismiss();
			}
		});
		ScrollView sv = new ScrollView(this);
		TextView tv = new TextView(this);
		sv.addView(tv);
		String version = Version.getFullVersion(app);
		String vt = this.getString(R.string.about_version) + "\t";
		String edition = "";
		if (!this.getString(R.string.app_edition).equals("")) {
			edition = this.getString(R.string.shared_string_release) + " : \t" + this.getString(R.string.app_edition);
		}
		tv.setText(vt + version + "\n" +
				edition + "\n\n" +
				this.getString(R.string.about_content));

		DisplayMetrics m = new DisplayMetrics();

		WindowManager mgr = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		mgr.getDefaultDisplay().getMetrics(m);
		int dp = (int) (5 * m.density);
		tv.setPadding(3 * dp , dp, 3 * dp, dp);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
		if(app.getSettings().isLightContent() ) {
			tv.setTextColor(Color.BLACK);
		}
//		tv.setMovementMethod(LinkMovementMethod.getInstance());
		ll.addView(tb);
		ll.addView(sv);
		dialog.setContentView(ll);
		dialog.show();
//		bld.setPositiveButton(R.string.shared_string_ok, null);
	}

	

}
