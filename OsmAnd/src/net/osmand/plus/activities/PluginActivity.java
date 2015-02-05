package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;

/**
 * Created by Alexey Pelykh on 02.02.2015.
 */
public class PluginActivity extends OsmandActionBarActivity {
	private static final String TAG = "PluginActivity";
	public static final String EXTRA_PLUGIN_ID = "plugin_id";

	private OsmandPlugin plugin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent == null || !intent.hasExtra(EXTRA_PLUGIN_ID)) {
			Log.e(TAG, "Required extra '" + EXTRA_PLUGIN_ID + "' is missing");
			finish();
			return;
		}
		String pluginId = intent.getStringExtra(EXTRA_PLUGIN_ID);
		if (pluginId == null) {
			Log.e(TAG, "Extra '" + EXTRA_PLUGIN_ID + "' is null");
			finish();
			return;
		}
		for (OsmandPlugin plugin : OsmandPlugin.getAvailablePlugins()) {
			if (!plugin.getId().equals(pluginId))
				continue;

			this.plugin = plugin;
			break;
		}
		if (plugin == null) {
			Log.e(TAG, "Plugin '" + EXTRA_PLUGIN_ID + "' not found");
			finish();
			return;
		}

		setContentView(R.layout.plugin);
		getSupportActionBar().setTitle(plugin.getName());

		TextView descriptionView = (TextView)findViewById(R.id.plugin_description);
		descriptionView.setText(plugin.getDescription());

		final Class<? extends Activity> settingsActivity = plugin.getSettingsActivity();
		final Button settingsButton = (Button)findViewById(R.id.plugin_settings);
		if (settingsActivity == null) {
			settingsButton.setVisibility(View.GONE);
		} else {
			settingsButton.setEnabled(plugin.isActive());
			settingsButton.setVisibility(View.VISIBLE);
			settingsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					startActivity(new Intent(PluginActivity.this, settingsActivity));
				}
			});
		}

		CompoundButton enableDisableButton = (CompoundButton)findViewById(
				R.id.plugin_enable_disable);
		enableDisableButton.setChecked(plugin.isActive());
		enableDisableButton.setOnCheckedChangeListener(
				new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (plugin.isActive() == isChecked) {
					return;
				}

				boolean ok = OsmandPlugin.enablePlugin((OsmandApplication)getApplication(), plugin,
						isChecked);
				settingsButton.setEnabled(isChecked && ok);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		CompoundButton enableDisableButton = (CompoundButton)findViewById(
				R.id.plugin_enable_disable);
		enableDisableButton.setChecked(plugin.isActive());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				return true;

		}
		return false;
	}
}
