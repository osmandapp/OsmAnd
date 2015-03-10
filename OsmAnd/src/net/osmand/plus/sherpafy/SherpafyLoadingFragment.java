package net.osmand.plus.sherpafy;

import android.support.v4.app.Fragment;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


public class SherpafyLoadingFragment extends Fragment {
	OsmandApplication app;
	private View view;

	public SherpafyLoadingFragment() {
	}
	

	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container,
			Bundle savedInstanceState) {
		app = (OsmandApplication) getActivity().getApplication();
		view = inflater.inflate(R.layout.loading, container, false);
		return view;
	}
	
	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		((OsmandApplication) getActivity().getApplication()).checkApplicationIsBeingInitialized(getActivity(),
				new AppInitializeListener() {

					@Override
					public void onProgress(AppInitializer init, InitEvents event) {
						String tn = init.getCurrentInitTaskName();
						if (tn != null) {
							((TextView) view.findViewById(R.id.ProgressMessage)).setText(tn);
						}
					}

					@Override
					public void onFinish(AppInitializer init) {
						((TourViewActivity) getActivity()).showSelectedItem();
					}
				});

	}

}