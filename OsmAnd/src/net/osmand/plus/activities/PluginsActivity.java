package net.osmand.plus.activities;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PluginsActivity extends OsmandListActivity {
	
	private List<OsmandPlugin> availablePlugins;
	private Set<String> enabledPlugins = new LinkedHashSet<String>();
	private Set<String> restartPlugins = new LinkedHashSet<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CustomTitleBar titleBar = new CustomTitleBar(this, R.string.plugins_screen, R.drawable.tab_favorites_screen_icon);
		setContentView(R.layout.plugins);
		
		availablePlugins = OsmandPlugin.getAvailablePlugins();
		List<OsmandPlugin> enabledPlugins = OsmandPlugin.getEnabledPlugins();
		for(OsmandPlugin p : enabledPlugins) {
			restartPlugins.add(p.getId());
			this.enabledPlugins.add(p.getId());
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
		getListAdapter().notifyDataSetInvalidated();
	}
	
	@Override
	public OsmandPluginsAdapter getListAdapter() {
		return (OsmandPluginsAdapter) super.getListAdapter();
	}
	
	private int colorGreen = 0xff23CC6C;
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
			final View row = v;
			OsmandPlugin plugin = getItem(position);
			ImageView imgView = (ImageView) row.findViewById(R.id.plugin_icon);
			imgView.setImageDrawable(getResources().getDrawable(R.drawable.icon_small));
			TextView nameView = (TextView) row.findViewById(R.id.plugin_name);
			nameView.setText(plugin.getName());
			
			TextView description = (TextView) row.findViewById(R.id.plugin_descr);
			description.setText(plugin.getDescription());
			boolean enabled = enabledPlugins.contains(plugin.getId());
			boolean toBeEnabled = restartPlugins.contains(plugin.getId());
			description.setTextColor(toBeEnabled? colorGreen : Color.LTGRAY);
			nameView.setTextColor(toBeEnabled? colorGreen : Color.LTGRAY);
//			description.setTypeface(enabled? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
			nameView.setTypeface(enabled? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

			return row;
		}
		
	}
	

}
