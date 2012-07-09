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
import android.widget.ListView;
import android.widget.TextView;

public class PluginsActivity extends OsmandListActivity {
	
	private List<OsmandPlugin> availablePlugins;
	private Set<String> clickedPlugins = new LinkedHashSet<String>();
	private Set<String> restartPlugins = new LinkedHashSet<String>();
	private static int colorGreen = 0xff23CC6C;
	
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
				
		OsmandPlugin item = getListAdapter().getItem(position);
		boolean enable = !restartPlugins.contains(item.getId());
		boolean ok = OsmandPlugin.enablePlugin(((OsmandApplication) getApplication()), item, enable);
		if (ok) {
			if (!enable) {
				restartPlugins.remove(item.getId());
			} else {
				restartPlugins.add(item.getId());
			}
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
			int resourceId = toBeEnabled ? R.drawable.list_activities_dot_marker2_pressed : R.drawable.list_activities_dot_marker1_pressed;
			
			final View row = v;
			TextView nameView = (TextView) row.findViewById(R.id.plugin_name);
			nameView.setText(plugin.getName());
			nameView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.list_activities_plugin_menu_symbol), null, getResources().getDrawable(resourceId), null);
			
			TextView description = (TextView) row.findViewById(R.id.plugin_descr);
			description.setText(plugin.getDescription());
			description.setVisibility(clickedPlugins.contains(plugin.getId()) || !restartPlugins.contains(plugin.getId()) ? View.VISIBLE : View.GONE);
			description.setTextColor(toBeEnabled? colorGreen : Color.LTGRAY);

			return row;
		}
		
	}
	

}
