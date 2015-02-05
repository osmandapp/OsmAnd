package net.osmand.plus.dashboard;

import java.util.List;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
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
			View pluginView = AndroidUtils.findParentViewById(buttonView, R.id.dash_plugin_item);
			OsmandPlugin plugin = (OsmandPlugin)pluginView.getTag();
			if (plugin.isActive() == isChecked) {
				return;
			}
			if (OsmandPlugin.enablePlugin(getMyApplication(), plugin, isChecked)) {
				updatePluginState(pluginView);
			}
		}
	};

	private final View.OnClickListener toggleEnableDisableListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			View pluginView = AndroidUtils.findParentViewById(view, R.id.dash_plugin_item);
			OsmandPlugin plugin = (OsmandPlugin)pluginView.getTag();
			if (OsmandPlugin.enablePlugin(getMyApplication(), plugin, !plugin.isActive())) {
				updatePluginState(pluginView);
			}
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
		List<OsmandPlugin> enabledPlugins = OsmandPlugin.getAvailablePlugins();
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
			if (plugin instanceof OsmandDevelopmentPlugin) {
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

		for (int pluginIndex = 0; pluginIndex < pluginsContainer.getChildCount(); pluginIndex++) {
			View pluginView = pluginsContainer.getChildAt(pluginIndex);

			updatePluginState(pluginView);
		}
	}

	private void updatePluginState(View pluginView) {
		OsmandPlugin plugin = (OsmandPlugin)pluginView.getTag();
		boolean isEnabled = plugin.isActive();

		CompoundButton enableDisableButton = (CompoundButton)pluginView.findViewById(
				R.id.check_item);
		enableDisableButton.setChecked(isEnabled);

		ImageButton logoView = (ImageButton)pluginView.findViewById(R.id.plugin_logo);
		if (plugin.isActive()) {
			logoView.setBackgroundResource(R.drawable.bg_plugin_logo_enabled);
		} else {
			TypedArray attributes = getActivity().getTheme().obtainStyledAttributes(
					new int[] {R.attr.bg_plugin_logo_disabled});
			logoView.setBackgroundDrawable(attributes.getDrawable(0));
			attributes.recycle();
		}
	}

	private void inflatePluginView(LayoutInflater inflater, ViewGroup container,
								   OsmandPlugin plugin) {
		View view = inflater.inflate(R.layout.dash_plugin_item, container, false);
		view.setTag(plugin);

		view.setOnClickListener(pluginDetailsListener);

		TextView nameView = (TextView)view.findViewById(R.id.plugin_name);
		nameView.setText(plugin.getName());

		ImageButton logoView = (ImageButton)view.findViewById(R.id.plugin_logo);
		logoView.setOnClickListener(toggleEnableDisableListener);
		logoView.setImageResource(plugin.getLogoResourceId());

		CompoundButton enableDisableButton = (CompoundButton)view.findViewById(R.id.check_item);
		enableDisableButton.setOnCheckedChangeListener(enableDisableListener);

		updatePluginState(view);

		container.addView(view);
	}
}
