package net.osmand.plus.activities;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PluginsActivity extends OsmandListActivity {
	public static final int ACTIVE_PLUGINS_LIST_MODIFIED = 1;

	private boolean activePluginsListModified = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.plugins);
		getSupportActionBar().setTitle(R.string.plugins_screen);

		setListAdapter(new PluginsListAdapter());
	}

	@Override
	public PluginsListAdapter getListAdapter() {
		return (PluginsListAdapter) super.getListAdapter();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		OsmandPlugin plugin = view.getTag() instanceof OsmandPlugin
				? (OsmandPlugin)view.getTag()
				: null;
		if (plugin == null) {
			return;
		}

		Intent intent = new Intent(this, PluginActivity.class);
		intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, plugin.getId());
		startActivity(intent);
	}

	private void enableDisablePlugin(OsmandPlugin plugin, boolean enable) {
		boolean ok = OsmandPlugin.enablePlugin(((OsmandApplication) getApplication()), plugin,
				enable);
		if (!ok) {
			return;
		}

		if (!activePluginsListModified) {
			setResult(ACTIVE_PLUGINS_LIST_MODIFIED);
			activePluginsListModified = true;
		}
		getListAdapter().notifyDataSetChanged();
	}

	protected class PluginsListAdapter extends ArrayAdapter<OsmandPlugin> {
		public PluginsListAdapter() {
			super(PluginsActivity.this, R.layout.plugins_list_item,
					OsmandPlugin.getAvailablePlugins());
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = getLayoutInflater().inflate(R.layout.plugins_list_item, parent, false);
			}

			final OsmandPlugin plugin = getItem(position);

			view.setTag(plugin);

			ImageView pluginLogo = (ImageView)view.findViewById(R.id.plugin_logo);
			pluginLogo.setImageResource(plugin.getLogoResourceId());

			TextView pluginName = (TextView)view.findViewById(R.id.plugin_name);
			pluginName.setText(plugin.getName());
			pluginName.setContentDescription(plugin.getName() + " " + getString(plugin.isActive()
					? R.string.item_checked
					: R.string.item_unchecked));

			TextView pluginDescription = (TextView)view.findViewById(R.id.plugin_description);
			pluginDescription.setText(plugin.getDescription());

			View pluginIsEnabled = view.findViewById(R.id.plugin_is_enabled);
			pluginIsEnabled.setVisibility(plugin.isActive() ? View.VISIBLE : View.INVISIBLE);

			View pluginOptions = view.findViewById(R.id.plugin_options);
			pluginOptions.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showOptionsMenu(v, plugin);
				}
			});

			return view;
		}
	}

	private void showOptionsMenu(View v, final OsmandPlugin plugin) {
		final Class<? extends Activity> settingsActivity = plugin.getSettingsActivity();

		final PopupMenu optionsMenu = new PopupMenu(this, v);

		MenuItem enableDisableItem = optionsMenu.getMenu().add(plugin.isActive()
				? R.string.disable_plugin
				: R.string.enable_plugin);
		enableDisableItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				enableDisablePlugin(plugin, !plugin.isActive());
				optionsMenu.dismiss();
				return true;
			}
		});

		if (settingsActivity != null) {
			MenuItem settingsItem = optionsMenu.getMenu().add(R.string.settings);
			settingsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					startActivity(new Intent(PluginsActivity.this, settingsActivity));
					optionsMenu.dismiss();
					return true;
				}
			});
			settingsItem.setEnabled(plugin.isActive());
		}

		optionsMenu.show();
	}
}
