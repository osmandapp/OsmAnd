package net.osmand.plus.osmedit;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.helpers.FontCache;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Denis
 * on 20.01.2015.
 */
public class DashOsmEditsFragment extends DashBaseFragment {
	OsmEditingPlugin plugin;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);

		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_audio_video_notes_plugin, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		TextView header = ((TextView) view.findViewById(R.id.notes_text));
		header.setTypeface(typeface);
		header.setText(R.string.osm_settings);
		Button manage = ((Button) view.findViewById(R.id.show_all));
		manage.setTypeface(typeface);
		manage.setText(R.string.osm_editing_manage);

		return view;
	}


	@Override
	public void onOpenDash() {
		if (plugin == null) {
			plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		}
		setupEditings();		
	}
	
	
	private void setupEditings() {
		View mainView = getView();
		if (plugin == null){
			mainView.setVisibility(View.GONE);
			return;
		}
	}
}
