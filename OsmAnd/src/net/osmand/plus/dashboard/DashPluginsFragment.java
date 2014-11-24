package net.osmand.plus.dashboard;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;

import java.util.List;

/**
 * Created by Denis on 21.11.2014.
 */
public class DashPluginsFragment extends DashBaseFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_plugins_fragment, container, false);
		Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Medium.ttf");
		((TextView) view.findViewById(R.id.plugin_text)).setTypeface(typeface);
		((Button) view.findViewById(R.id.show_all)).setTypeface(typeface);
		view.findViewById(R.id.show_all).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivityForResult(new Intent(getActivity(), getMyApplication().getAppCustomization().getPluginsActivity()), 1);
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		LinearLayout layout = (LinearLayout) getView().findViewById(R.id.plugins);
		layout.removeAllViews();
		addPlugins(layout);
	}

	private void addPlugins(View parent){
		LinearLayout layout = (LinearLayout) parent;
		LayoutInflater inflater = getActivity().getLayoutInflater();

		List<OsmandPlugin> availablePlugins = OsmandPlugin.getAvailablePlugins();
		List<OsmandPlugin> enabledPlugins = OsmandPlugin.getEnabledPlugins();
		for (int i=0; i < availablePlugins.size(); i++){
			if (i> 2){
				break;
			}
			final OsmandPlugin plugin = availablePlugins.get(i);
			View view = inflater.inflate(R.layout.dash_plugin_item, null, false);
			((TextView) view.findViewById(R.id.plugin_name)).setText(plugin.getName());
			((TextView) view.findViewById(R.id.plugin_descr)).setText(plugin.getDescription());
			CompoundButton check = (CompoundButton) view.findViewById(R.id.check_item);
			check.setChecked(enabledPlugins.contains(plugin));
			check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					OsmandPlugin.enablePlugin(getMyApplication(),plugin, b);
				}
			});
			int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
			view.setLayoutParams(lp);
			layout.addView(view);
		}



	}
}
