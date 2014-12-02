package net.osmand.plus.dashboard;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.DashboardActivity;

/**
 * Created by dummy on 02.12.14.
 */
public class DashLoadingFragment extends SherlockFragment {
    OsmandApplication app;
    private View view;

    public DashLoadingFragment() {
    }


    public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container,
                             Bundle savedInstanceState) {
        app = (OsmandApplication) getSherlockActivity().getApplication();
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
                            (view.findViewById(R.id.ProgressMessage)).setVisibility(View.GONE);
                            view.findViewById(R.id.ProgressBar).setVisibility(View.GONE);
                            DashboardActivity dashboardActivity =((DashboardActivity)getSherlockActivity());
                            dashboardActivity.getSupportFragmentManager().beginTransaction().detach(DashLoadingFragment.this);
                            dashboardActivity.updateDownloads();
                            dashboardActivity.addFragments();
                        }
                    });
        } else {
            DashboardActivity dashboardActivity =((DashboardActivity)getSherlockActivity());
            dashboardActivity.getSupportFragmentManager().beginTransaction().detach(DashLoadingFragment.this);
            dashboardActivity.updateDownloads();
            dashboardActivity.addFragments();
        }
    }

}
