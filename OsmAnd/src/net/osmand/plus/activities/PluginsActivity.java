package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;

public class PluginsActivity extends OsmandListActivity {
	public static final int ACTIVE_PLUGINS_LIST_MODIFIED = 1;

	private boolean activePluginsListModified = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
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

	@Override
	protected void onResume() {
		super.onResume();

		getListAdapter().notifyDataSetChanged();
	}

	private void enableDisablePlugin(OsmandPlugin plugin, boolean enable) {
		boolean ok = OsmandPlugin.enablePlugin(this, ((OsmandApplication) getApplication()), plugin,
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

			ImageButton pluginLogo = (ImageButton)view.findViewById(R.id.plugin_logo);
			pluginLogo.setImageResource(plugin.getLogoResourceId());
			if (plugin.isActive()) {
				pluginLogo.setBackgroundResource(R.drawable.bg_plugin_logo_enabled);
				pluginLogo.setContentDescription(getString(R.string.shared_string_disable));
			} else {
				TypedArray attributes = getTheme().obtainStyledAttributes(
						new int[] {R.attr.bg_plugin_logo_disabled});
				pluginLogo.setBackgroundDrawable(attributes.getDrawable(0));
				pluginLogo.setContentDescription(getString(plugin.needsInstallation() ? R.string.access_shared_string_not_installed : R.string.shared_string_enable));
				attributes.recycle();
			}
			pluginLogo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!plugin.isActive() && plugin.needsInstallation()) {
						// nothing
					} else {
						enableDisablePlugin(plugin, !plugin.isActive());
					}
				}
			});

			TextView pluginName = (TextView)view.findViewById(R.id.plugin_name);
			pluginName.setText(plugin.getName());
			pluginName.setContentDescription(plugin.getName() + " " + getString(plugin.isActive()
					? R.string.item_checked
					: R.string.item_unchecked));

			TextView pluginDescription = (TextView)view.findViewById(R.id.plugin_description);
			pluginDescription.setText(plugin.getDescription());


			ImageView pluginOptions = (ImageView) view.findViewById(R.id.plugin_options);
			pluginOptions.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_overflow_menu_white));
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
		if (plugin.isActive() || !plugin.needsInstallation()) {
			MenuItem enableDisableItem = optionsMenu.getMenu().add(
					plugin.isActive() ? R.string.shared_string_disable
							: R.string.shared_string_enable);
			enableDisableItem
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							enableDisablePlugin(plugin, !plugin.isActive());
							optionsMenu.dismiss();
							return true;
						}
					});
		}

		if (settingsActivity != null && plugin.isActive()) {
			MenuItem settingsItem = optionsMenu.getMenu().add(R.string.shared_string_settings);
			settingsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					startActivity(new Intent(PluginsActivity.this, settingsActivity));
					optionsMenu.dismiss();
					return true;
				}
			});
		}

		optionsMenu.show();
	}
}
