package net.osmand.plus.osmedit.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmPoint;

public class SendPoiDialogFragment extends DialogFragment {
	public static final String TAG = "SendPoiDialogFragment";
	public static final String OPENSTREETMAP_POINT = "openstreetmap_point";
	private static String comment;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final OsmPoint[] poi = (OsmPoint[]) getArguments().getSerializable(OPENSTREETMAP_POINT);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		View view = getActivity().getLayoutInflater().inflate(R.layout.send_poi_dialog, null);
		final EditText messageEditText = (EditText) view.findViewById(R.id.messageEditText);
		final EditText userNameEditText = (EditText) view.findViewById(R.id.userNameEditText);
		final EditText passwordEditText = (EditText) view.findViewById(R.id.passwordEditText);
		final CheckBox closeChangeSetCheckBox =
				(CheckBox) view.findViewById(R.id.closeChangeSetCheckBox);
		messageEditText.setText(comment);
		final OsmandSettings settings = ((OsmandApplication) getActivity().getApplication())
				.getSettings();
		userNameEditText.setText(settings.USER_NAME.get());
		passwordEditText.setText(settings.USER_PASSWORD.get());

		final ProgressDialogPoiUploader progressDialogPoiUploader =
				(ProgressDialogPoiUploader) getParentFragment();

		builder.setTitle(R.string.commit_poi)
				.setView(view)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						comment = messageEditText.getText().toString();
						settings.USER_NAME.set(userNameEditText.getText().toString());
						settings.USER_PASSWORD.set(passwordEditText.getText().toString());
						for (OsmPoint osmPoint : poi) {
							if (osmPoint.getGroup() == OsmPoint.Group.POI) {
								((OpenstreetmapPoint) osmPoint)
										.setComment(comment);
								break;
							}
						}
						progressDialogPoiUploader.showProgressDialog(poi,
								closeChangeSetCheckBox.isChecked());
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		return builder.create();
	}

	public static SendPoiDialogFragment createInstance(OsmPoint[] points) {
		SendPoiDialogFragment fragment = new SendPoiDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(OPENSTREETMAP_POINT, points);
		fragment.setArguments(bundle);
		return fragment;
	}

	public interface ProgressDialogPoiUploader {
		void showProgressDialog(OsmPoint[] points, boolean closeChangeSet);
	}
}
