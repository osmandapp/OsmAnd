package net.osmand.plus.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.development.OsmandDevelopmentPlugin;

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

	public static final String TAG = "DASH_PLUGINS_FRAGMENT";
	private ArrayList<OsmandPlugin> showedPlugins;
	private ArrayList<CompoundButton> checks;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_plugins_fragment,
				container, false);
		view.findViewById(R.id.show_all).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivityForResult(new Intent(getActivity(),
						getMyApplication().getAppCustomization().getPluginsActivity()), 1);
			}
		});
		LinearLayout layout = (LinearLayout) view.findViewById(R.id.plugins);
		addPlugins(inflater, layout);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		for (int i = 0; i < checks.size(); i++) {
			final CompoundButton ch = checks.get(i);
			final OsmandPlugin o = showedPlugins.get(i);
			ch.setOnCheckedChangeListener(null);
			ch.setChecked(OsmandPlugin.getEnabledPlugins().contains(o));
			ch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					OsmandPlugin.enablePlugin(getMyApplication(), o, b);
				}
			});
		}
		
	}

	private void addPlugins(LayoutInflater inflater, View parent) {
		LinearLayout layout = (LinearLayout) parent;

		List<OsmandPlugin> availablePlugins = OsmandPlugin.getAvailablePlugins();
		List<OsmandPlugin> enabledPlugins = OsmandPlugin.getEnabledPlugins();
		List<OsmandPlugin> toShow = new ArrayList<OsmandPlugin>();
		showedPlugins = new ArrayList<OsmandPlugin>();
		checks = new ArrayList<CompoundButton>();
		for(OsmandPlugin o : availablePlugins) {
			if(!(o instanceof OsmandDevelopmentPlugin)) {
				if(enabledPlugins.contains(o)) {
					showedPlugins.add(o);
				} else{
					toShow.add(o);
				}
			}
		}
		Collections.shuffle(toShow, new Random(System.currentTimeMillis()));
		while (!toShow.isEmpty()) {
			showedPlugins.add(toShow.remove(0));
			if (showedPlugins.size() > 2) {
				break;
			}
		}
		
		for (int i = 0; i < showedPlugins.size(); i++) {
			final OsmandPlugin plugin = showedPlugins.get(i);
			View view = inflater.inflate(R.layout.dash_plugin_item, layout, false);

			((TextView) view.findViewById(R.id.plugin_name)).setText(plugin.getName());
			((ImageView) view.findViewById(R.id.plugin_logo)).setImageResource(
					plugin.getLogoResourceId());

			CompoundButton check = (CompoundButton) view.findViewById(R.id.check_item);
			checks.add(check);
			check.setChecked(enabledPlugins.contains(plugin));
//			int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources()
//					.getDisplayMetrics());
//			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
//			view.setLayoutParams(lp);
			layout.addView(view);
		}
	}
}
