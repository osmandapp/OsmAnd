package net.osmand.plus.dashboard;

import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.development.OsmandDevelopmentPlugin;

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

	private View.OnClickListener getListener(final OsmandPlugin plugin) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
				closeDashboard();
			}
		};
	}

	private final View.OnClickListener pluginDetailsListener(final OsmandPlugin plugin) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getActivity(), PluginActivity.class);
				intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, plugin.getId());
				startActivity(intent);
				closeDashboard();
			}
		};
	}

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dash_common_fragment, container, false);
		TextView header = ((TextView) view.findViewById(R.id.fav_text));
		header.setText(TITLE_ID);
		view.findViewById(R.id.show_all).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(new Intent(getActivity(), getMyApplication().getAppCustomization().getPluginsActivity()));
				closeDashboard();
			}
		});
		initPlugins();
		return view;
	}


	private void initPlugins() {
		List<OsmandPlugin> notActivePlugins = OsmandPlugin.getNotEnabledPlugins();
		Collections.shuffle(notActivePlugins);
		plugins = new ArrayList<OsmandPlugin>();
		Iterator<OsmandPlugin> nit = notActivePlugins.iterator();
		Iterator<OsmandPlugin> it = OsmandPlugin.getEnabledPlugins().iterator();
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
		LinearLayout pluginsContainer = (LinearLayout) contentView.findViewById(R.id.items);
		pluginsContainer.removeAllViews();
		for (OsmandPlugin p : plugins) {
			inflatePluginView(inflater, pluginsContainer, p);
		}
	}

	private void updatePluginState(View pluginView, OsmandPlugin plugin) {
		CompoundButton enableDisableButton = (CompoundButton) pluginView.findViewById(R.id.plugin_enable_disable);
		Button getButton = (Button) pluginView.findViewById(R.id.get_plugin);
		enableDisableButton.setOnCheckedChangeListener(null);
		if (plugin.needsInstallation()) {
			getButton.setVisibility(View.VISIBLE);
			enableDisableButton.setVisibility(View.GONE);
		} else {
			getButton.setVisibility(View.GONE);
			enableDisableButton.setVisibility(View.VISIBLE);
			enableDisableButton.setChecked(plugin.isActive());
		}
		setListener(plugin, enableDisableButton, pluginView);

		ImageButton logoView = (ImageButton) pluginView.findViewById(R.id.plugin_logo);
		if (plugin.isActive()) {
			logoView.setBackgroundResource(R.drawable.bg_plugin_logo_enabled);
		} else {
			TypedArray attributes = getActivity().getTheme().obtainStyledAttributes(
					new int[]{R.attr.bg_plugin_logo_disabled});
			logoView.setBackgroundDrawable(attributes.getDrawable(0));
			attributes.recycle();
		}
	}

	private void inflatePluginView(LayoutInflater inflater, ViewGroup container, final OsmandPlugin plugin) {
		View view = inflater.inflate(R.layout.dash_plugin_item, container, false);
		view.setOnClickListener(pluginDetailsListener(plugin));

		TextView nameView = (TextView) view.findViewById(R.id.plugin_name);
		nameView.setText(plugin.getName());

		ImageButton logoView = (ImageButton) view.findViewById(R.id.plugin_logo);
		logoView.setImageResource(plugin.getLogoResourceId());

		CompoundButton enableDisableButton = (CompoundButton) view.findViewById(R.id.plugin_enable_disable);
		Button getButton = (Button) view.findViewById(R.id.get_plugin);
		getButton.setOnClickListener(getListener(plugin));
		enableDisableButton.setOnCheckedChangeListener(null);
		updatePluginState(view, plugin);
		final View pluginView = view;
		setListener(plugin, enableDisableButton, pluginView);
		container.addView(view);
	}

	private void setListener(final OsmandPlugin plugin, CompoundButton enableDisableButton, final View pluginView) {
		enableDisableButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (plugin.isActive() == isChecked || plugin.needsInstallation()) {
					return;
				}
				if (OsmandPlugin.enablePlugin(getActivity(), getMyApplication(), plugin, isChecked)) {
					updatePluginState(pluginView, plugin);
				}
			}
		});
	}
}
