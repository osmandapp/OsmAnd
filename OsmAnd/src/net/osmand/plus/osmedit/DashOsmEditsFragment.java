package net.osmand.plus.osmedit;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment.PoiUploaderType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Denis
 * on 20.01.2015.
 */
public class DashOsmEditsFragment extends DashBaseFragment
		implements SendPoiDialogFragment.ProgressDialogPoiUploader {
	public static final String TAG = "DASH_OSM_EDITS_FRAGMENT";
	public static final int TITLE_ID = R.string.osm_settings;

	private static final String ROW_NUMBER_TAG = TAG + "_row_number";

	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA =
			new DashFragmentData(TAG, DashOsmEditsFragment.class, SHOULD_SHOW_FUNCTION, 130, ROW_NUMBER_TAG);

	OsmEditingPlugin plugin;

	@Override
	public View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);

		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		TextView header = ((TextView) view.findViewById(R.id.fav_text));
		header.setText(TITLE_ID);
		Button manage = ((Button) view.findViewById(R.id.show_all));
		manage.setText(R.string.shared_string_manage);
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startFavoritesActivity(R.string.osm_edits);
				closeDashboard();
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
		assert mainView != null;
		if (plugin == null) {
			mainView.setVisibility(View.GONE);
			return;
		}

		ArrayList<OsmPoint> dataPoints = new ArrayList<>();
		getOsmPoints(dataPoints);
		if (dataPoints.size() == 0) {
			mainView.setVisibility(View.GONE);
			return;
		} else {
			mainView.setVisibility(View.VISIBLE);
			DashboardOnMap.handleNumberOfRows(dataPoints,
					getMyApplication().getSettings(), ROW_NUMBER_TAG);
		}

		LinearLayout osmLayout = (LinearLayout) mainView.findViewById(R.id.items);
		osmLayout.removeAllViews();

		for (final OsmPoint point : dataPoints) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.note, null, false);

			OsmEditsFragment.getOsmEditView(view, point, getMyApplication());
			ImageButton send = (ImageButton) view.findViewById(R.id.play);
			send.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_action_export));
			send.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (point.getGroup() == OsmPoint.Group.POI) {
						SendPoiDialogFragment.createInstance(new OsmPoint[] {point}, PoiUploaderType.FRAGMENT)
								.show(getChildFragmentManager(), "SendPoiDialogFragment");
					} else {
						uploadItem(point);
					}
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

	// TODO: 9/7/15 Redesign osm notes.
	private void uploadItem(final OsmPoint point) {
		AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
		b.setMessage(getString(R.string.local_osm_changes_upload_all_confirm, 1));
		b.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showProgressDialog(new OsmPoint[] {point}, false, false);
			}
		});
		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.show();
	}

	@Override
	public void showProgressDialog(OsmPoint[] points, boolean closeChangeSet, boolean anonymously) {
		OsmPoint[] toUpload = points;
		ProgressDialogFragment dialog = ProgressDialogFragment.createInstance(R.string.uploading,
				R.string.local_openstreetmap_uploading, ProgressDialog.STYLE_HORIZONTAL);
		OsmEditsUploadListener listener = new OsmEditsUploadListenerHelper(getActivity(),
				getString(R.string.local_openstreetmap_were_uploaded)) {
			@Override
			public void uploadUpdated(OsmPoint point) {
				super.uploadUpdated(point);
				if (DashOsmEditsFragment.this.isAdded()) {
					onOpenDash();
				}
			}

			@Override
			public void uploadEnded(Map<OsmPoint, String> loadErrorsMap) {
				super.uploadEnded(loadErrorsMap);
				if (DashOsmEditsFragment.this.isAdded()) {
					onOpenDash();
				}
			}
		};
		dialog.show(getChildFragmentManager(), ProgressDialogFragment.TAG);
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(dialog,
				listener, plugin, toUpload.length, closeChangeSet, anonymously);
		uploadTask.execute(toUpload);
	}

	private void getOsmPoints(ArrayList<OsmPoint> dataPoints) {
		List<OpenstreetmapPoint> l1 = plugin.getDBPOI().getOpenstreetmapPoints();
		List<OsmNotesPoint> l2 = plugin.getDBBug().getOsmbugsPoints();
		if (l1.isEmpty()) {
			int i = 0;
			for (OsmPoint point : l2) {
				if (i > 2) {
					break;
				}
				dataPoints.add(point);
				i++;
			}
		} else if (l2.isEmpty()) {
			int i = 0;
			for (OsmPoint point : l1) {
				if (i > 2) {
					break;
				}
				dataPoints.add(point);
				i++;
			}
		} else {
			dataPoints.add(l1.get(0));
			dataPoints.add(l2.get(0));
			if (l1.size() > 1) {
				dataPoints.add(l1.get(1));
			} else if (l2.size() > 1) {
				dataPoints.add(l2.get(1));
			}
		}
	}

}
