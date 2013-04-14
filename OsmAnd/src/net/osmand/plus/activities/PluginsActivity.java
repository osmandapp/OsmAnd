package net.osmand.plus.activities;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class PluginsActivity extends OsmandListActivity {

	public static final int ACTIVE_PLUGINS_LIST_MODIFIED = 1;

	private List<OsmandPlugin> availablePlugins;
	private Set<String> clickedPlugins = new LinkedHashSet<String>();
	private Set<String> restartPlugins = new LinkedHashSet<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CustomTitleBar titleBar = new CustomTitleBar(this, R.string.plugins_screen, R.drawable.tab_plugin_screen_icon);
		setContentView(R.layout.plugins);
		
		availablePlugins = OsmandPlugin.getAvailablePlugins();
		List<OsmandPlugin> enabledPlugins = OsmandPlugin.getEnabledPlugins();
		for(OsmandPlugin p : enabledPlugins) {
			restartPlugins.add(p.getId());
		}
		
		titleBar.afterSetContentView();
		setListAdapter(new OsmandPluginsAdapter(availablePlugins));
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
				
		click(position);
	}

	private void click(int position) {
		OsmandPlugin item = getListAdapter().getItem(position);
		boolean enable = !restartPlugins.contains(item.getId());
		boolean ok = OsmandPlugin.enablePlugin(((OsmandApplication) getApplication()), item, enable);
		if (ok) {
			if (!enable) {
				restartPlugins.remove(item.getId());
			} else {
				restartPlugins.add(item.getId());
			}
			setResult(ACTIVE_PLUGINS_LIST_MODIFIED);
		}
		clickedPlugins.add(item.getId());
		getListAdapter().notifyDataSetChanged();
	}
	
	@Override
	public OsmandPluginsAdapter getListAdapter() {
		return (OsmandPluginsAdapter) super.getListAdapter();
	}
	
	protected class OsmandPluginsAdapter extends ArrayAdapter<OsmandPlugin> {
		
		public OsmandPluginsAdapter(List<OsmandPlugin> plugins) {
			super(PluginsActivity.this, R.layout.plugins_list_item, plugins);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.plugins_list_item, parent, false);
			}
			OsmandPlugin plugin = getItem(position);
			boolean toBeEnabled = restartPlugins.contains(plugin.getId());
			
			final View row = v;
			CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
			ch.setOnClickListener(null);
			ch.setChecked(toBeEnabled);
			ch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					click(position);
				}
			});
			TextView nameView = (TextView) row.findViewById(R.id.plugin_name);
			nameView.setText(plugin.getName());
			nameView.setContentDescription(plugin.getName() + " " + getString(toBeEnabled ? R.string.item_checked : R.string.item_unchecked));
			
			
			TextView description = (TextView) row.findViewById(R.id.plugin_descr);
			description.setText(plugin.getDescription());
			description.setVisibility(clickedPlugins.contains(plugin.getId()) ||
					!restartPlugins.contains(plugin.getId()) ? View.VISIBLE : View.GONE);
			description.setTextColor(Color.LTGRAY);

			return row;
		}
		
	}
	

}
