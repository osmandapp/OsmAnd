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

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.DashOsmEditsFragment;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmPoint;

/**
 * Created by GaidamakUA on 9/7/15.
 */
public class SendPoiDialogFragment extends DialogFragment {
	public static final String OPENSTREETMAP_POINT = "openstreetmap_point";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final OsmPoint poi = (OpenstreetmapPoint) getArguments().getSerializable(OPENSTREETMAP_POINT);
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
						settings.USER_NAME.set(userNameEditText.getText().toString());
						settings.USER_PASSWORD.set(passwordEditText.getText().toString());

						poi.setComment(messageEditText.getText().toString());
						((ProgressDialogPoiUploader) getParentFragment()).showProgressDialog(poi,
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

	interface ProgressDialogPoiUploader {
		void showProgressDialog(OsmPoint[] points, boolean closeChangeSet);
	}
}
