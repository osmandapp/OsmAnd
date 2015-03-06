package net.osmand.plus.osmedit;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;

/**
 * Created by Denis
 * on 06.03.2015.
 */
public class OsmEditsFragment extends ListFragment {
	OsmEditingPlugin plugin;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.select_all).setVisibility(View.GONE);
		((TextView) view.findViewById(R.id.header)).setText(R.string.your_edits);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

	}


}
