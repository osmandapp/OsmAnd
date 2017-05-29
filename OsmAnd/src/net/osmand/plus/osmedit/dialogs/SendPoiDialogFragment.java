package net.osmand.plus.osmedit.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmBugsLayer;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmEditsUploadListener;
import net.osmand.plus.osmedit.OsmEditsUploadListenerHelper;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.osmedit.UploadOpenstreetmapPointAsyncTask;

import java.util.Map;

public class SendPoiDialogFragment extends DialogFragment {
	public static final String TAG = "SendPoiDialogFragment";
	public static final String OPENSTREETMAP_POINT = "openstreetmap_point";
	public static final String POI_UPLOADER_TYPE = "poi_uploader_type";
	private static String comment;

	public enum PoiUploaderType {
		SIMPLE,
		FRAGMENT
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final OsmPoint[] poi = (OsmPoint[]) getArguments().getSerializable(OPENSTREETMAP_POINT);
		final PoiUploaderType poiUploaderType = PoiUploaderType.valueOf(getArguments().getString(POI_UPLOADER_TYPE, PoiUploaderType.SIMPLE.name()));
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		View view = getActivity().getLayoutInflater().inflate(R.layout.send_poi_dialog, null);
		final SwitchCompat uploadAnonymously = (SwitchCompat) view.findViewById(R.id.upload_anonymously_switch);
		final EditText messageEditText = (EditText) view.findViewById(R.id.message_field);
		final EditText userNameEditText = (EditText) view.findViewById(R.id.user_name_field);
		final EditText passwordEditText = (EditText) view.findViewById(R.id.password_field);
		final View messageLabel = view.findViewById(R.id.message_label);
		final View userNameLabel = view.findViewById(R.id.osm_user_name_label);
		final View passwordLabel = view.findViewById(R.id.osm_user_password_label);
		final CheckBox closeChangeSetCheckBox =
				(CheckBox) view.findViewById(R.id.close_change_set_checkbox);
		messageEditText.setText(comment);
		final OsmandSettings settings = ((OsmandApplication) getActivity().getApplication())
				.getSettings();
		userNameEditText.setText(settings.USER_NAME.get());
		passwordEditText.setText(settings.USER_PASSWORD.get());
		boolean hasPoiGroup = false;
		assert poi != null;
		for (OsmPoint p : poi) {
			if (p.getGroup() == OsmPoint.Group.POI) {
				hasPoiGroup = true;
				break;
			}
		}
		final boolean hasPOI = hasPoiGroup;
		messageLabel.setVisibility(hasPOI ? View.VISIBLE : View.GONE);
		messageEditText.setVisibility(hasPOI ? View.VISIBLE : View.GONE);
		closeChangeSetCheckBox.setVisibility(hasPOI ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.osm_note_header).setVisibility(hasPOI ? View.GONE : View.VISIBLE);
		uploadAnonymously.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				userNameLabel.setVisibility(isChecked ? View.GONE : View.VISIBLE);
				userNameEditText.setVisibility(isChecked ? View.GONE : View.VISIBLE);
				passwordLabel.setVisibility(isChecked ? View.GONE : View.VISIBLE);
				passwordEditText.setVisibility(isChecked ? View.GONE : View.VISIBLE);
			}
		});

		final ProgressDialogPoiUploader progressDialogPoiUploader;
		if (poiUploaderType == PoiUploaderType.SIMPLE && getActivity() instanceof MapActivity) {
			progressDialogPoiUploader =
					new SendPoiDialogFragment.SimpleProgressDialogPoiUploader((MapActivity) getActivity());
		} else {
			progressDialogPoiUploader = (ProgressDialogPoiUploader) getParentFragment();
		}
		builder.setTitle(hasPOI ? R.string.upload_poi : R.string.upload_osm_note)
				.setView(view)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (progressDialogPoiUploader != null) {
							comment = messageEditText.getText().toString();
							settings.USER_NAME.set(userNameEditText.getText().toString());
							settings.USER_PASSWORD.set(passwordEditText.getText().toString());
							if (comment.length() > 0) {
								for (OsmPoint osmPoint : poi) {
									if (osmPoint.getGroup() == OsmPoint.Group.POI) {
										((OpenstreetmapPoint) osmPoint).setComment(comment);
										break;
									}
								}
							}
							progressDialogPoiUploader.showProgressDialog(poi,
									closeChangeSetCheckBox.isChecked(),
									!hasPOI && uploadAnonymously.isChecked());
						}
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		return builder.create();
	}

	public static SendPoiDialogFragment createInstance(@NonNull OsmPoint[] points, @NonNull PoiUploaderType uploaderType) {
		SendPoiDialogFragment fragment = new SendPoiDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(OPENSTREETMAP_POINT, points);
		bundle.putString(POI_UPLOADER_TYPE, uploaderType.name());
		fragment.setArguments(bundle);
		return fragment;
	}

	public interface ProgressDialogPoiUploader {
		void showProgressDialog(OsmPoint[] points, boolean closeChangeSet, boolean anonymously);
	}

	public static class SimpleProgressDialogPoiUploader implements ProgressDialogPoiUploader {

		private MapActivity mapActivity;

		public SimpleProgressDialogPoiUploader(MapActivity mapActivity) {
			this.mapActivity = mapActivity;
		}

		@Override
		public void showProgressDialog(OsmPoint[] points, boolean closeChangeSet, boolean anonymously) {
			ProgressDialogFragment dialog = ProgressDialogFragment.createInstance(
					R.string.uploading,
					R.string.local_openstreetmap_uploading,
					ProgressDialog.STYLE_HORIZONTAL);
			OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
			OsmEditsUploadListener listener = new OsmEditsUploadListenerHelper(mapActivity,
					mapActivity.getString(R.string.local_openstreetmap_were_uploaded)) {
				@Override
				public void uploadEnded(Map<OsmPoint, String> loadErrorsMap) {
					super.uploadEnded(loadErrorsMap);
					mapActivity.getContextMenu().close();
					OsmBugsLayer l = mapActivity.getMapView().getLayerByClass(OsmBugsLayer.class);
					if(l != null) {
						l.clearCache();
						mapActivity.refreshMap();
					}
				}
			};
			dialog.show(mapActivity.getSupportFragmentManager(), ProgressDialogFragment.TAG);
			UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(
					dialog, listener, plugin, points.length, closeChangeSet, anonymously);
			uploadTask.execute(points);
		}
	}
}
