package net.osmand.plus.liveupdates;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.osmand.plus.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LiveUpdatesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LiveUpdatesFragment extends Fragment {
	public static final String TITILE = "Live Updates";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live_updates, container, false);
		ListView listView = (ListView) view.findViewById(android.R.id.list);
		View header = inflater.inflate(R.layout.live_updates_header, listView, false);
		listView.addHeaderView(header);
		return view;
	}

	private static class LiveUpdatesAdapter extends ArrayAdapter<Object> {

		public LiveUpdatesAdapter(Context context, int resource, Object[] objects) {
			super(context, resource, objects);
		}
	}
}
