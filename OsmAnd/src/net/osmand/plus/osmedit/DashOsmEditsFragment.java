package net.osmand.plus.osmedit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.data.PointDescription;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Denis
 * on 20.01.2015.
 */
public class DashOsmEditsFragment extends DashBaseFragment {
	public static final String TAG = "DASH_OSM_EDITS_FRAGMENT";

	OsmEditingPlugin plugin;

	@Override
	public View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
					if (point.getGroup() == OsmPoint.Group.POI) {
						SendPoiDialogFragment.createInstance((OpenstreetmapPoint) point)
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

	private void uploadItem(final OsmPoint point) {
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
		OsmPoint[] toUpload = new OsmPoint[]{point};
		OsmBugsRemoteUtil remotebug = new OsmBugsRemoteUtil(getMyApplication());
		ProgressDialog dialog = ProgressImplementation.createProgressDialog(getActivity(),
				getString(R.string.uploading), getString(R.string.local_openstreetmap_uploading),
				ProgressDialog.STYLE_HORIZONTAL).getDialog();
		OsmEditsUploadListener listener = new OsmEditsUploadListenerHelper(getActivity(),
				getString(R.string.local_openstreetmap_were_uploaded)) {
			@Override
			public void uploadUpdated(OsmPoint point) {
				super.uploadUpdated(point);
				if (!DashOsmEditsFragment.this.isDetached()) {
					onOpenDash();
				}
			}

			@Override
			public void uploadEnded(Map<OsmPoint, String> loadErrorsMap) {
				super.uploadEnded(loadErrorsMap);
				if (!DashOsmEditsFragment.this.isDetached()) {
					onOpenDash();
				}
			}
		};
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(dialog,
				listener, plugin, remotepoi, remotebug, toUpload.length);
		uploadTask.execute(toUpload);
		dialog.show();
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

	public static class SendPoiDialogFragment extends DialogFragment {
		public static final String OPENSTREETMAP_POINT = "openstreetmap_point";

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			final OpenstreetmapPoint poi = (OpenstreetmapPoint) getArguments().getSerializable(OPENSTREETMAP_POINT);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			View view = getActivity().getLayoutInflater().inflate(R.layout.send_poi_dialog, null);
			final EditText messageEditText = (EditText) view.findViewById(R.id.messageEditText);
			final EditText userNameEditText = (EditText) view.findViewById(R.id.userNameEditText);
			final EditText passwordEditText = (EditText) view.findViewById(R.id.passwordEditText);
			final CheckBox closeChangeSetCheckBox =
					(CheckBox) view.findViewById(R.id.closeChangeSetCheckBox);
			final OsmandSettings settings = ((MapActivity) getActivity()).getMyApplication().getSettings();
			userNameEditText.setText(settings.USER_NAME.get());
			passwordEditText.setText(settings.USER_PASSWORD.get());
			builder.setTitle(R.string.commit_poi)
					.setView(view)
					.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							final OpenstreetmapRemoteUtil openstreetmapRemoteUtil
									= new OpenstreetmapRemoteUtil(getActivity());
							settings.USER_NAME.set(userNameEditText.getText().toString());
							settings.USER_PASSWORD.set(passwordEditText.getText().toString());
							final String message = messageEditText.getText().toString();
							final boolean closeChangeSet = closeChangeSetCheckBox.isChecked();
							final Activity activity = getActivity();
							int actionTypeMessageId = -1;
							switch (poi.getAction()) {
								case CREATE: actionTypeMessageId = R.string.poi_action_add;
									break;
								case MODIFY: actionTypeMessageId = R.string.poi_action_change;
									break;
								case DELETE: actionTypeMessageId = R.string.poi_action_delete;
									break;
							}
							final String resultMessage =
									getResources().getString(actionTypeMessageId);
							final String successTemplate = getResources().getString(
									R.string.poi_action_succeded_template);

							new AsyncTask<Void, Void, EntityInfo>() {

								@Override
								protected EntityInfo doInBackground(Void... params) {
									return openstreetmapRemoteUtil.loadNode(poi.getEntity());
								}

								@Override
								protected void onPostExecute(EntityInfo entityInfo) {
									EditPoiFragment.commitNode(poi.getAction(), poi.getEntity(),
											entityInfo,
											message,
											closeChangeSet,
											new Runnable() {
												@Override
												public void run() {
													AccessibleToast.makeText(
															activity,
															MessageFormat.format(
																	successTemplate,
																	resultMessage),
															Toast.LENGTH_LONG).show();

													if (activity instanceof MapActivity) {
														((MapActivity) activity)
																.getMapView().refreshMap(true);
													}
												}
											},
											activity, openstreetmapRemoteUtil);
								}
							}.execute();
//							poi.setComment(messageEditText.getText().toString());
//							((DashOsmEditsFragment) getParentFragment()).showProgressDialog(poi);
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, null);
			return builder.create();
		}

		public static SendPoiDialogFragment createInstance(OpenstreetmapPoint poi) {
			SendPoiDialogFragment fragment = new SendPoiDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable(OPENSTREETMAP_POINT, poi);
			fragment.setArguments(bundle);
			return fragment;
		}
	}
}
