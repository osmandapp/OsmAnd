package net.osmand.plus.dashboard;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.chooseplan.ChoosePlanUtils;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.plugins.skimaps.SkiMapsPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Denis
 * on 21.11.2014.
 */
public class DashPluginsFragment extends DashBaseFragment {
	public static final String TAG = "DASH_PLUGINS_FRAGMENT";
	private static final int TITLE_ID = R.string.prefs_plugins;
	public static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	private List<OsmandPlugin> plugins;

	private View.OnClickListener getListener(@NonNull OsmandPlugin plugin) {
		return view -> {
			ChoosePlanUtils.onGetPlugin(getActivity(), plugin);
			closeDashboard();
		};
	}

	private View.OnClickListener pluginDetailsListener() {
		return view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				PluginsFragment.showInstance(activity.getSupportFragmentManager());
			}
			closeDashboard();
		};
	}

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dash_common_fragment, container, false);
		TextView header = view.findViewById(R.id.fav_text);
		header.setText(TITLE_ID);
		view.findViewById(R.id.show_all).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				PluginsFragment.showInstance(activity.getSupportFragmentManager());
			}
			closeDashboard();
		});
		initPlugins();
		return view;
	}


	private void initPlugins() {
		List<OsmandPlugin> notFunctionalPlugins = PluginsHelper.getNotActivePlugins();
		notFunctionalPlugins.remove(PluginsHelper.getPlugin(SkiMapsPlugin.class));
		notFunctionalPlugins.remove(PluginsHelper.getPlugin(NauticalMapsPlugin.class));
		Collections.shuffle(notFunctionalPlugins);

		List<OsmandPlugin> enabledPlugins = PluginsHelper.getActivePlugins();
		enabledPlugins.remove(PluginsHelper.getPlugin(SkiMapsPlugin.class));
		enabledPlugins.remove(PluginsHelper.getPlugin(NauticalMapsPlugin.class));

		plugins = new ArrayList<>();
		Iterator<OsmandPlugin> nit = notFunctionalPlugins.iterator();
		Iterator<OsmandPlugin> it = enabledPlugins.iterator();
		addPluginsToLimit(nit, 1);
		addPluginsToLimit(it, 5);
		addPluginsToLimit(nit, 5);
	}


	private void addPluginsToLimit(Iterator<OsmandPlugin> it, int l) {
		while (plugins.size() < l && it.hasNext()) {
			OsmandPlugin plugin = it.next();
			if (plugin instanceof OsmandDevelopmentPlugin) {
				continue;
			}
			plugins.add(plugin);
		}
	}

	@Override
	public void onOpenDash() {
		View contentView = getView();
		LayoutInflater inflater = getActivity().getLayoutInflater();
		LinearLayout pluginsContainer = contentView.findViewById(R.id.items);
		pluginsContainer.removeAllViews();
		for (OsmandPlugin p : plugins) {
			inflatePluginView(inflater, pluginsContainer, p);
		}
	}

	private void updatePluginState(View pluginView, OsmandPlugin plugin) {
		CompoundButton enableDisableButton = pluginView.findViewById(R.id.plugin_enable_disable);
		Button getButton = pluginView.findViewById(R.id.get_plugin);
		enableDisableButton.setOnCheckedChangeListener(null);
		if (plugin.isLocked()) {
			getButton.setVisibility(View.VISIBLE);
			enableDisableButton.setVisibility(View.GONE);
		} else {
			getButton.setVisibility(View.GONE);
			enableDisableButton.setVisibility(View.VISIBLE);
			enableDisableButton.setChecked(plugin.isEnabled());
		}
		setListener(plugin, enableDisableButton, pluginView);

		ImageButton logoView = pluginView.findViewById(R.id.plugin_logo);
		if (plugin.isEnabled()) {
			logoView.setBackgroundResource(R.drawable.bg_plugin_logo_enabled_light);
			logoView.setContentDescription(getString(R.string.shared_string_disable));
		} else {
			TypedArray attributes = getActivity().getTheme().obtainStyledAttributes(
					new int[]{R.attr.bg_plugin_logo_disabled});
			logoView.setBackground(attributes.getDrawable(0));
			logoView.setContentDescription(getString(plugin.isLocked() ? R.string.access_shared_string_not_installed : R.string.shared_string_enable));
			attributes.recycle();
		}
	}

	private void inflatePluginView(LayoutInflater inflater, ViewGroup container, OsmandPlugin plugin) {
		View view = inflater.inflate(R.layout.dash_plugin_item, container, false);
		view.setOnClickListener(pluginDetailsListener());

		TextView nameView = view.findViewById(R.id.plugin_name);
		nameView.setText(plugin.getName());

		ImageButton logoView = view.findViewById(R.id.plugin_logo);
		logoView.setImageResource(plugin.getLogoResourceId());

		CompoundButton enableDisableButton = view.findViewById(R.id.plugin_enable_disable);
		Button getButton = view.findViewById(R.id.get_plugin);
		getButton.setText(plugin.isPaid() ? R.string.shared_string_get : R.string.shared_string_install);
		getButton.setOnClickListener(getListener(plugin));
		enableDisableButton.setOnCheckedChangeListener(null);
		updatePluginState(view, plugin);
		setListener(plugin, enableDisableButton, view);
		container.addView(view);
	}

	private void setListener(OsmandPlugin plugin, CompoundButton enableDisableButton, View pluginView) {
		enableDisableButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (PluginsHelper.enablePluginIfNeeded(getActivity(), getMyApplication(), plugin, isChecked)) {
				updatePluginState(pluginView, plugin);
			}
		});
	}
}
