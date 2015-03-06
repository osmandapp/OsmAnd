package net.osmand.plus.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
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

/**
 * Created by Denis
 * on 21.11.2014.
 */
public class DashPluginsFragment extends DashBaseFragment {

	public static final String TAG = "DASH_PLUGINS_FRAGMENT";



	private final View.OnClickListener getListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			View pluginView = AndroidUtils.findParentViewById(view, R.id.dash_plugin_item);
			OsmandPlugin plugin = (OsmandPlugin)pluginView.getTag();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
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
		List<OsmandPlugin> notActivePlugins = OsmandPlugin.getNotEnabledPlugins();
		Collections.shuffle(notActivePlugins);
		for(OsmandPlugin plugin : notActivePlugins) {
			if (plugin instanceof OsmandDevelopmentPlugin) {
				continue;
			}

			inflatePluginView(inflater, pluginsContainer, plugin);
			break;
		}
		for(OsmandPlugin plugin : OsmandPlugin.getEnabledPlugins()) {
			if (pluginsContainer.getChildCount() >= 5) {
				break;
			}
			if (plugin instanceof OsmandDevelopmentPlugin) {
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
		CompoundButton enableDisableButton = (CompoundButton)pluginView.findViewById(
				R.id.plugin_enable_disable);
		Button getButton = (Button)pluginView.findViewById(R.id.get_plugin);
		if (plugin.needsInstallation()) {
			getButton.setVisibility(View.VISIBLE);
			enableDisableButton.setVisibility(View.GONE);
		} else {
			getButton.setVisibility(View.GONE);
			enableDisableButton.setVisibility(View.VISIBLE);
			enableDisableButton.setChecked(plugin.isActive());
		}

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
								   final OsmandPlugin plugin) {
		View view = inflater.inflate(R.layout.dash_plugin_item, container, false);
		view.setTag(plugin);

		view.setOnClickListener(pluginDetailsListener);

		TextView nameView = (TextView)view.findViewById(R.id.plugin_name);
		nameView.setText(plugin.getName());

		ImageButton logoView = (ImageButton)view.findViewById(R.id.plugin_logo);
		logoView.setImageResource(plugin.getLogoResourceId());

		CompoundButton enableDisableButton =
				(CompoundButton)view.findViewById(R.id.plugin_enable_disable);
		Button getButton = (Button)view.findViewById(R.id.get_plugin);
		getButton.setOnClickListener(getListener);
		enableDisableButton.setOnCheckedChangeListener(null);
		updatePluginState(view);
		final View pluginView = view;
		enableDisableButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (plugin.isActive() == isChecked || plugin.needsInstallation()) {
					return;
				}
				if (OsmandPlugin.enablePlugin(getActivity(), getMyApplication(), plugin, isChecked)) {
					
					updatePluginState(pluginView);
				}
			}
		});
		container.addView(view);
	}
}
