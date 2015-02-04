package net.osmand.plus.dashboard;

import java.util.List;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.development.OsmandDevelopmentPlugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Denis on 21.11.2014.
 */
public class DashPluginsFragment extends DashBaseFragment {
	private final CompoundButton.OnCheckedChangeListener enableDisableListener =
			new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			OsmandPlugin plugin = (OsmandPlugin)buttonView.getTag();
			boolean isEnabled = OsmandPlugin.getEnabledPlugins().contains(plugin);

			if (isEnabled == isChecked) {
				return;
			}

			OsmandPlugin.enablePlugin(getMyApplication(), plugin, isChecked);
		}
	};

	private final View.OnClickListener pluginSettingsListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			OsmandPlugin plugin = (OsmandPlugin)view.getTag();

			Class<? extends Activity> settingsActivity = plugin.getSettingsActivity();
			startActivity(new Intent(getActivity(), settingsActivity));
		}
	};

	private final View.OnClickListener pluginDetailsListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			OsmandPlugin plugin = (OsmandPlugin)view.getTag();

			Intent intent = new Intent(getActivity(), PluginActivity.class);
			intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, plugin.getId());
			startActivity(intent);
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		View contentView = inflater.inflate(R.layout.dash_plugins_fragment, container, false);
		contentView.findViewById(R.id.show_all).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(new Intent(getActivity(),
						getMyApplication().getAppCustomization().getPluginsActivity()));
			}
		});

		LinearLayout pluginsContainer = (LinearLayout) contentView.findViewById(R.id.plugins);
		List<OsmandPlugin> enabledPlugins = OsmandPlugin.getEnabledPlugins();
		for(OsmandPlugin plugin : enabledPlugins) {
			if (plugin instanceof  OsmandDevelopmentPlugin) {
				continue;
			}

			inflatePluginView(inflater, pluginsContainer, plugin);
		}
		for(OsmandPlugin plugin : OsmandPlugin.getAvailablePlugins()) {
			if (pluginsContainer.getChildCount() > 2) {
				break;
			}
			if (plugin instanceof  OsmandDevelopmentPlugin) {
				continue;
			}
			if (enabledPlugins.contains(plugin)) {
				continue;
			}

			inflatePluginView(inflater, pluginsContainer, plugin);
		}

		return contentView;
	}

	@Override
	public void onResume() {
		super.onResume();

		View contentView = getView();
		if (contentView == null) {
			return;
		}
		LinearLayout pluginsContainer = (LinearLayout) contentView.findViewById(R.id.plugins);

		List<OsmandPlugin> enabledPlugins = OsmandPlugin.getEnabledPlugins();
		for (int pluginIndex = 0; pluginIndex < pluginsContainer.getChildCount(); pluginIndex++) {
			View pluginView = pluginsContainer.getChildAt(pluginIndex);
			OsmandPlugin plugin = (OsmandPlugin)pluginView.getTag();
			boolean isEnabled = enabledPlugins.contains(plugin);

			CompoundButton enableDisableButton = (CompoundButton) pluginView.findViewById(
					R.id.check_item);
			enableDisableButton.setChecked(isEnabled);
		}
	}

	private void inflatePluginView(LayoutInflater inflater, ViewGroup container,
								   OsmandPlugin plugin) {
		boolean isEnabled = OsmandPlugin.getEnabledPlugins().contains(plugin);

		View view = inflater.inflate(R.layout.dash_plugin_item, container, false);
		view.setTag(plugin);

		// To discuss: too much confusing and not consistent
//		boolean hasSettings = (plugin.getSettingsActivity() != null);
//		if (isEnabled && hasSettings) {
//			view.setOnClickListener(pluginSettingsListener);
//		} else {
//		view.setOnClickListener(pluginDetailsListener);
//		}
		view.setOnClickListener(pluginDetailsListener);

		TextView nameView = (TextView)view.findViewById(R.id.plugin_name);
		nameView.setText(plugin.getName());

		ImageView logoView = (ImageView)view.findViewById(R.id.plugin_logo);
		logoView.setImageResource(plugin.getLogoResourceId());

		CompoundButton enableDisableButton = (CompoundButton)view.findViewById(R.id.check_item);
		enableDisableButton.setTag(plugin);
		enableDisableButton.setChecked(isEnabled);
		enableDisableButton.setOnCheckedChangeListener(enableDisableListener);

		container.addView(view);
	}
}
