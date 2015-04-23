package net.osmand.plus.dashboard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivity;

/**
 * Created by Denis on
 * 26.03.2015.
 */
public class DashFirstTimeFragment extends DashBaseFragment {

	public static final String TAG = "DASH_FIRST_TIME_FRAGMENT";


	@Override
	public void onOpenDash() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_first_time, container, false);

		view.findViewById(R.id.select_region).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				final OsmandSettings.DrivingRegion[] drs  = OsmandSettings.DrivingRegion.values();

				final OsmandSettings.DrivingRegion currentRegion = getMyApplication().getSettings().DRIVING_REGION.get();

				String[] entries = new String[drs.length];
				int currentIndex = 0;
				for (int i = 0; i < entries.length; i++) {
					if (currentRegion.equals(drs[i])){
						currentIndex = i;
					}
					entries[i] = getString(drs[i].name); // + " (" + drs[i].defMetrics.toHumanString(this) +")" ;
				}
				builder.setSingleChoiceItems(entries, currentIndex, null);
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
						getMyApplication().getSettings().DRIVING_REGION.set(drs[selectedPosition]);
						updateCurrentRegion(getView());
					}
				});
				builder.show();
			}
		});

		view.findViewById(R.id.hide).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getMyApplication().getAppInitializer().setFirstTime(false);
				dashboard.refreshDashboardFragments();
			}
		});

		view.findViewById(R.id.download_map).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), DownloadActivity.class);
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				getActivity().startActivity(intent);
			}
		});

		updateCurrentRegion(view);
		((ImageView)view.findViewById(R.id.car_icon)).setImageDrawable(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_car_dark));
		return view;
	}

	private void updateCurrentRegion(View view) {
		((TextView) view.findViewById(R.id.region)).setText(getMyApplication().getSettings().DRIVING_REGION.get().name);
	}
}
