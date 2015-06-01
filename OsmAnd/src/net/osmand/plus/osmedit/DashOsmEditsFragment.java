package net.osmand.plus.osmedit;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Denis
 * on 20.01.2015.
 */
public class DashOsmEditsFragment extends DashBaseFragment implements OsmEditsUploadListener {
	public static final String TAG = "DASH_OSM_EDITS_FRAGMENT";

	OsmEditingPlugin plugin;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);

		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		TextView header = ((TextView) view.findViewById(R.id.fav_text));
		header.setText(R.string.osm_settings);
		Button manage = ((Button) view.findViewById(R.id.show_all));
		manage.setText(R.string.shared_string_manage);
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startFavoritesActivity(R.string.osm_edits);
			}
		});

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

		ArrayList<OsmPoint> dataPoints = new ArrayList<>();
		getOsmPoints(dataPoints);
		if (dataPoints.size() == 0){
			mainView.setVisibility(View.GONE);
			return;
		} else {
			mainView.setVisibility(View.VISIBLE);
		}

		LinearLayout osmLayout = (LinearLayout) mainView.findViewById(R.id.items);
		osmLayout.removeAllViews();

		for (final OsmPoint point : dataPoints) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.note, null, false);

			OsmEditsFragment.getOsmEditView(view, point, getMyApplication());
			ImageButton send = (ImageButton) view.findViewById(R.id.play);
			send.setImageDrawable(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_export));
			send.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					uploadItem(point);
				}
			});
			view.findViewById(R.id.options).setVisibility(View.GONE);
			view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean poi = point.getGroup() == OsmPoint.Group.POI;
					String name = poi ? ((OpenstreetmapPoint) point).getName() : ((OsmNotesPoint) point).getText();
					getMyApplication().getSettings().setMapLocationToShow(
							point.getLatitude(),
							point.getLongitude(),
							15,
							new PointDescription(poi ? PointDescription.POINT_TYPE_POI
									: PointDescription.POINT_TYPE_OSM_BUG, name), true, point); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			osmLayout.addView(view);
		}
	}

	private void uploadItem(final OsmPoint point){
		AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
		b.setMessage(getString(R.string.local_osm_changes_upload_all_confirm, 1));
		b.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showProgressDialog(point);
			}
		});
		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.show();
	}

	private void showProgressDialog(OsmPoint point) {
		OpenstreetmapRemoteUtil remotepoi = new OpenstreetmapRemoteUtil(getActivity());
		OsmPoint[] toUpload = new OsmPoint[] { point };
		OsmBugsRemoteUtil remotebug = new OsmBugsRemoteUtil(getMyApplication());
		ProgressDialog dialog = ProgressImplementation.createProgressDialog(getActivity(),
				getString(R.string.uploading), getString(R.string.local_openstreetmap_uploading),
				ProgressDialog.STYLE_HORIZONTAL).getDialog();
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(dialog,
				DashOsmEditsFragment.this, plugin, remotepoi, remotebug, toUpload.length);
		uploadTask.execute(toUpload);
		dialog.show();
	}

	private void getOsmPoints(ArrayList<OsmPoint> dataPoints) {
		List<OpenstreetmapPoint> l1 = plugin.getDBPOI().getOpenstreetmapPoints();
		List<OsmNotesPoint> l2 = plugin.getDBBug().getOsmbugsPoints();
		if (l1.isEmpty()){
			int i = 0;
			for(OsmPoint point : l2){
				if (i > 2) {
					break;
				}
				dataPoints.add(point);
				i++;
			}
		} else if (l2.isEmpty()) {
			int i = 0;
			for(OsmPoint point : l1){
				if (i > 2) {
					break;
				}
				dataPoints.add(point);
				i++;
			}
		} else {
			dataPoints.add(l1.get(0));
			dataPoints.add(l2.get(0));
			if (l1.size() > 1){
				dataPoints.add(l1.get(1));
			} else if (l2.size() > 1){
				dataPoints.add(l2.get(1));
			}
		}
	}

	@Override
	public void uploadUpdated(OsmPoint point) {
		if (!this.isDetached()){
			onOpenDash();
		}
	}

	@Override
	public void uploadEnded(Integer result) {
		if (result != null) {
			AccessibleToast.makeText(getActivity(),
					MessageFormat.format(getString(R.string.local_openstreetmap_were_uploaded), result), Toast.LENGTH_LONG)
					.show();
		}
		if (!this.isDetached()){
			onOpenDash();
		}
	}
}
