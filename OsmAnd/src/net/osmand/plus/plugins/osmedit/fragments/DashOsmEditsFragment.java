package net.osmand.plus.plugins.osmedit.fragments;

import static net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dashboard.tools.DashFragmentData.DefaultShouldShow;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditsUploadListener;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadOpenstreetmapPointAsyncTask;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.dialogs.ProgressDialogPoiUploader;
import net.osmand.plus.plugins.osmedit.dialogs.SendOsmNoteBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.dialogs.SendPoiBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.helpers.OsmEditsUploadListenerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Denis
 * on 20.01.2015.
 */
public class DashOsmEditsFragment extends DashBaseFragment
		implements ProgressDialogPoiUploader, OsmAuthorizationListener {
	public static final String TAG = "DASH_OSM_EDITS_FRAGMENT";
	public static final int TITLE_ID = R.string.osm_editing_plugin_name;

	private static final String ROW_NUMBER_TAG = TAG + "_row_number";

	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	public static final DashFragmentData FRAGMENT_DATA =
			new DashFragmentData(TAG, DashOsmEditsFragment.class, SHOULD_SHOW_FUNCTION, 130, ROW_NUMBER_TAG);

	OsmEditingPlugin plugin;
	private OsmPoint selectedPoint;

	@Override
	public View initView(@Nullable ViewGroup container, @Nullable Bundle savedState) {
		plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		View view = inflate(R.layout.dash_common_fragment, container, false);

		((TextView) view.findViewById(R.id.fav_text)).setText(TITLE_ID);
		((TextView) view.findViewById(R.id.show_all)).setText(R.string.shared_string_manage);

		(view.findViewById(R.id.show_all)).setOnClickListener(v -> {
			startMyPlacesActivity(R.string.osm_edits);
			closeDashboard();
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		if (plugin == null) {
			plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		}
		setupEditings();
	}

	private void setupEditings() {
		View mainView = getView();
		if (mainView == null) return;

		if (plugin == null) {
			mainView.setVisibility(View.GONE);
			return;
		}

		ArrayList<OsmPoint> dataPoints = new ArrayList<>();
		getOsmPoints(dataPoints);
		if (dataPoints.isEmpty()) {
			mainView.setVisibility(View.GONE);
			return;
		}

		mainView.setVisibility(View.VISIBLE);
		DashboardOnMap.handleNumberOfRows(dataPoints, settings, ROW_NUMBER_TAG);

		LinearLayout osmLayout = mainView.findViewById(R.id.items);
		osmLayout.removeAllViews();

		for (OsmPoint point : dataPoints) {
			View view = inflate(R.layout.note);

			OsmEditsFragment.getOsmEditView(view, point, app);
			ImageButton send = view.findViewById(R.id.play);
			send.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_export));
			send.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity == null) return;

				if (point.getGroup() == OsmPoint.Group.POI) {
					selectedPoint = point;
					if (app.getOsmOAuthHelper().isLogged(plugin)) {
						SendPoiBottomSheetFragment.showInstance(getChildFragmentManager(), new OsmPoint[] {point});
					} else {
						LoginBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), this);
					}
				} else {
					SendOsmNoteBottomSheetFragment.showInstance(getChildFragmentManager(), new OsmPoint[] {point});
				}
			});
			view.findViewById(R.id.options).setVisibility(View.GONE);
			view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			view.setOnClickListener(v -> {
				boolean poi = point.getGroup() == OsmPoint.Group.POI;
				String name = poi ? ((OpenstreetmapPoint) point).getName() : ((OsmNotesPoint) point).getText();
				settings.setMapLocationToShow(point.getLatitude(), point.getLongitude(), 15,
						new PointDescription(poi ? PointDescription.POINT_TYPE_POI
								: PointDescription.POINT_TYPE_OSM_BUG, name), true, point); //$NON-NLS-1$
				MapActivity.launchMapActivityMoveToTop(requireActivity());
			});
			osmLayout.addView(view);
		}
	}

	@Override
	public void authorizationCompleted() {
		if (selectedPoint != null) {
			SendPoiBottomSheetFragment.showInstance(getChildFragmentManager(), new OsmPoint[] {selectedPoint});
		}
	}

	@Override
	public void showProgressDialog(@NonNull OsmPoint[] points, boolean closeChangeSet, boolean anonymously) {
		FragmentActivity activity = getActivity();
		if (activity == null) return;

		OsmEditsUploadListener listener = new OsmEditsUploadListenerHelper(activity,
				getString(R.string.local_openstreetmap_were_uploaded)) {
			@Override
			public void uploadUpdated(@NonNull OsmPoint point) {
				super.uploadUpdated(point);
				if (DashOsmEditsFragment.this.isAdded()) {
					onOpenDash();
				}
			}

			@Override
			public void uploadEnded(@NonNull Map<OsmPoint, String> loadErrorsMap) {
				super.uploadEnded(loadErrorsMap);
				if (DashOsmEditsFragment.this.isAdded()) {
					onOpenDash();
				}
			}
		};
		ProgressDialogFragment dialog = ProgressDialogFragment.showInstance(getChildFragmentManager(),
				R.string.uploading, R.string.local_openstreetmap_uploading, ProgressDialog.STYLE_HORIZONTAL);
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(dialog,
				listener, plugin, points.length, closeChangeSet, anonymously);
		OsmAndTaskManager.executeTask(uploadTask, points);
	}

	private void getOsmPoints(@NonNull ArrayList<OsmPoint> dataPoints) {
		List<OpenstreetmapPoint> l1 = plugin.getDBPOI().getOpenstreetmapPoints();
		List<OsmNotesPoint> l2 = plugin.getDBBug().getOsmBugsPoints();
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