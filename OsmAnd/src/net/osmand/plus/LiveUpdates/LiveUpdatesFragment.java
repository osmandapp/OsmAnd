package net.osmand.plus.liveupdates;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
		return inflater.inflate(R.layout.fragment_live_updates, container, false);
	}

}
