package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadIndexesThread;

import java.util.ArrayList;

public class PluginsActivity extends OsmandListActivity implements DownloadIndexesThread.DownloadEvents {

	public static final int ACTIVE_PLUGINS_LIST_MODIFIED = 1;

	private boolean listModified = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
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
		Object tag = view.getTag();
		if (tag instanceof OsmandPlugin) {
			Intent intent = new Intent(this, PluginActivity.class);
			intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, ((OsmandPlugin) tag).getId());
			startActivity(intent);
		} else if (tag instanceof ConnectedApp) {
			switchEnabled((ConnectedApp) tag);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		OsmandApplication app = getMyApplication();
		OsmandPlugin.checkInstalledMarketPlugins(app, this);
		app.getDownloadThread().setUiActivity(this);
		getListAdapter().notifyDataSetChanged();
	}

	@Override
	protected void onPause() {
		super.onPause();
		getMyApplication().getDownloadThread().resetUiActivity(this);
	}

	private void enableDisablePlugin(OsmandPlugin plugin, boolean enable) {
		OsmandApplication app = getMyApplication();
		if (OsmandPlugin.enablePlugin(this, app, plugin, enable)) {
			if (!listModified) {
				setResult(ACTIVE_PLUGINS_LIST_MODIFIED);
				listModified = true;
			}
			getListAdapter().notifyDataSetChanged();
		}
	}

	private void switchEnabled(@NonNull ConnectedApp app) {
		getMyApplication().getAidlApi().switchEnabled(app);
		getListAdapter().notifyDataSetChanged();
	}

	// DownloadEvents
	@Override
	public void newDownloadIndexes() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof DownloadIndexesThread.DownloadEvents && fragment.isAdded()) {
				((DownloadIndexesThread.DownloadEvents) fragment).newDownloadIndexes();
			}
		}
	}

	@Override
	public void downloadInProgress() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof DownloadIndexesThread.DownloadEvents && fragment.isAdded()) {
				((DownloadIndexesThread.DownloadEvents) fragment).downloadInProgress();
			}
		}
	}

	@Override
	public void downloadHasFinished() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof DownloadIndexesThread.DownloadEvents && fragment.isAdded()) {
				((DownloadIndexesThread.DownloadEvents) fragment).downloadHasFinished();
			}
		}
	}

	protected class PluginsListAdapter extends ArrayAdapter<Object> {

		PluginsListAdapter() {
			super(PluginsActivity.this, R.layout.plugins_list_item, new ArrayList<>());
			addAll(getMyApplication().getAidlApi().getConnectedApps());
			addAll(OsmandPlugin.getVisiblePlugins());
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = getLayoutInflater().inflate(R.layout.plugins_list_item, parent, false);
			}

			final Object item = getItem(position);

			boolean active = false;
			int logoContDescId = R.string.shared_string_disable;
			String name = "";
			boolean isLightTheme = getMyApplication().getSettings().isLightContent();

			ImageButton pluginLogo = (ImageButton) view.findViewById(R.id.plugin_logo);
			ImageView pluginOptions = (ImageView) view.findViewById(R.id.plugin_options);
			TextView pluginDescription = (TextView) view.findViewById(R.id.plugin_description);

			if (item instanceof ConnectedApp) {
				final ConnectedApp app = (ConnectedApp) item;
				active = app.isEnabled();
				if (!active) {
					logoContDescId = R.string.shared_string_enable;
				}
				name = app.getName();
				pluginDescription.setText(R.string.third_party_application);
				pluginLogo.setImageDrawable(app.getIcon());
				pluginLogo.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						switchEnabled(app);
					}
				});
				pluginOptions.setVisibility(View.GONE);
				pluginOptions.setOnClickListener(null);
				view.setTag(app);
			} else if (item instanceof OsmandPlugin) {
				final OsmandPlugin plugin = (OsmandPlugin) item;
				active = plugin.isActive();
				if (!active) {
					logoContDescId = plugin.needsInstallation()
							? R.string.access_shared_string_not_installed : R.string.shared_string_enable;
				}
				name = plugin.getName();
				pluginDescription.setText(plugin.getDescription());
				pluginLogo.setImageResource(plugin.getLogoResourceId());
				pluginLogo.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (plugin.isActive() || !plugin.needsInstallation()) {
							enableDisablePlugin(plugin, !plugin.isActive());
						}
					}
				});
				pluginOptions.setVisibility(View.VISIBLE);
				pluginOptions.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
				pluginOptions.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showOptionsMenu(v, plugin);
					}
				});
				view.setTag(plugin);
			}

			pluginLogo.setContentDescription(getString(logoContDescId));
			if (active) {
				pluginLogo.setBackgroundResource(isLightTheme ? R.drawable.bg_plugin_logo_enabled_light : R.drawable.bg_plugin_logo_enabled_dark);
			} else {
				TypedArray attributes = getTheme().obtainStyledAttributes(new int[]{R.attr.bg_plugin_logo_disabled});
				pluginLogo.setBackgroundDrawable(attributes.getDrawable(0));
				attributes.recycle();
			}

			TextView pluginName = (TextView) view.findViewById(R.id.plugin_name);
			pluginName.setText(name);
			pluginName.setContentDescription(name + " " + getString(active
					? R.string.item_checked
					: R.string.item_unchecked));

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
