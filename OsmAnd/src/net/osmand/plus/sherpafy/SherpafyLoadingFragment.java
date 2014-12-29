package net.osmand.plus.sherpafy;

import android.support.v4.app.Fragment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
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
		if(app.isApplicationInitializing()) {
		app.checkApplicationIsBeingInitialized(getActivity(), (TextView) view.findViewById(R.id.ProgressMessage),
				(ProgressBar) view.findViewById(R.id.ProgressBar), new Runnable() {
					@Override
					public void run() {
						((TextView) view.findViewById(R.id.ProgressMessage)).setVisibility(View.GONE);
						view.findViewById(R.id.ProgressBar).setVisibility(View.GONE);
						((TourViewActivity)getActivity()).showSelectedItem();
					}
				});
		} else {
			((TourViewActivity)getActivity()).showSelectedItem();
		}
	}

}