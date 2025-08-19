package net.osmand.plus.plugins;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class PluginsListAdapter extends ArrayAdapter<Object> {

	private final PluginsFragment pluginsFragment;
	private final OsmandApplication app;

	PluginsListAdapter(PluginsFragment pluginsFragment, Context context) {
		super(context, R.layout.plugins_list_item, new ArrayList<>());
		this.pluginsFragment = pluginsFragment;
		this.app = (OsmandApplication) context.getApplicationContext();
		addAll(getFilteredPluginsAndApps());
	}

	private List<Object> getFilteredPluginsAndApps() {
		List<ConnectedApp> connectedApps = app.getAidlApi().getConnectedApps();
		List<OsmandPlugin> visiblePlugins = PluginsHelper.getAvailablePlugins();

		for (Iterator<OsmandPlugin> iterator = visiblePlugins.iterator(); iterator.hasNext(); ) {
			OsmandPlugin plugin = iterator.next();
			for (ConnectedApp app : connectedApps) {
				if (plugin.getId().equals(app.getPack())) {
					iterator.remove();
				}
			}
		}
		List<Object> list = new ArrayList<>();
		list.addAll(connectedApps);
		list.addAll(visiblePlugins);

		return list;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = pluginsFragment.inflate(R.layout.plugins_list_item, parent, false);
		}
		Context context = view.getContext();

		boolean active = false;
		int logoContDescId = R.string.shared_string_disable;
		String name = "";

		ImageButton pluginLogo = view.findViewById(R.id.plugin_logo);
		ImageView pluginOptions = view.findViewById(R.id.plugin_options);
		TextView pluginDescription = view.findViewById(R.id.plugin_description);

		Object item = getItem(position);
		if (item instanceof ConnectedApp connectedApp) {
			active = connectedApp.isEnabled();
			if (!active) {
				logoContDescId = R.string.shared_string_enable;
			}
			name = connectedApp.getName();
			pluginDescription.setText(R.string.third_party_application);
			pluginLogo.setImageDrawable(connectedApp.getIcon());
			pluginLogo.setOnClickListener(v -> pluginsFragment.switchEnabled(connectedApp));
			pluginOptions.setVisibility(View.GONE);
			pluginOptions.setOnClickListener(null);
			view.setTag(connectedApp);
		} else if (item instanceof OsmandPlugin plugin) {
			active = plugin.isEnabled();
			if (!active) {
				logoContDescId = plugin.isLocked()
						? R.string.access_shared_string_not_installed : R.string.shared_string_enable;
			}
			name = plugin.getName();
			pluginDescription.setText(plugin.getDescription(false));

			int color = AndroidUtils.getColorFromAttr(context, R.attr.list_background_color);
			Drawable pluginIcon = plugin.getLogoResource();
			if (pluginIcon.getConstantState() != null) {
				pluginIcon = pluginIcon.getConstantState().newDrawable().mutate();
			}
			pluginLogo.setImageDrawable(UiUtilities.tintDrawable(pluginIcon, color));
			pluginLogo.setOnClickListener(v -> {
				if (!plugin.isOnline()) {
					pluginsFragment.enableDisablePlugin(plugin);
				}
			});
			pluginOptions.setVisibility(View.VISIBLE);
			pluginOptions.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
			pluginOptions.setOnClickListener(v -> pluginsFragment.showOptionsMenu(v, plugin));
			view.setTag(plugin);
		}

		pluginLogo.setContentDescription(pluginsFragment.getString(logoContDescId));
		if (active) {
			pluginLogo.setBackgroundResource(pluginsFragment.isNightMode() ? R.drawable.bg_plugin_logo_enabled_dark : R.drawable.bg_plugin_logo_enabled_light);
		} else {
			TypedArray attributes = context.getTheme().obtainStyledAttributes(new int[]{R.attr.bg_plugin_logo_disabled});
			pluginLogo.setBackground(attributes.getDrawable(0));
			attributes.recycle();
		}

		TextView pluginName = view.findViewById(R.id.plugin_name);
		pluginName.setText(name);
		pluginName.setContentDescription(name + " " + pluginsFragment.getString(active
				? R.string.item_checked
				: R.string.item_unchecked));

		return view;
	}
}
