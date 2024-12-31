package net.osmand.plus.plugins.osmedit.helpers;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditsUploadListener;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadOpenstreetmapPointAsyncTask;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;

public class OsmEditsUploadListenerHelper implements OsmEditsUploadListener {
	public static final String TAG = "OsmEditUploadListenerHe";
	private final FragmentActivity activity;
	private final String numberFormat;

	public OsmEditsUploadListenerHelper(FragmentActivity activity, String numberFormat) {
		this.activity = activity;
		this.numberFormat = numberFormat;
	}

	@Override
	public void uploadUpdated(OsmPoint point) {
	}

	@MainThread
	@Override
	public void uploadEnded(Map<OsmPoint, String> loadErrorsMap) {
		if (activity.getSupportFragmentManager().isStateSaved()) {
			return;
		}
		int uploaded = 0;
		int pointsNum = loadErrorsMap.keySet().size();
		for (String s : loadErrorsMap.values()) {
			if (s == null) {
				uploaded++;
			}
		}
		if (uploaded == pointsNum) {
			Toast.makeText(activity,
					MessageFormat.format(numberFormat, uploaded),
					Toast.LENGTH_LONG)
					.show();
		} else if (pointsNum == 1) {
			Log.v(TAG, "in if1");
			OsmPoint point = loadErrorsMap.keySet().iterator().next();
			String message = loadErrorsMap.get(point);
			if (message.equals(activity.getString(R.string.auth_failed))) {
				LoginBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), null);
			} else {
				DialogFragment dialogFragment =
						UploadingErrorDialogFragment.getInstance(message, point);
				dialogFragment.show(activity.getSupportFragmentManager(), "error_loading");
			}
		} else {
			UploadingMultipleErrorDialogFragment dialogFragment =
					UploadingMultipleErrorDialogFragment.createInstance(loadErrorsMap);
			dialogFragment.show(activity.getSupportFragmentManager(), "multiple_error_loading");

		}
	}

	private static void showUploadItemsProgressDialog(Fragment fragment, OsmPoint[] toUpload) {
		FragmentActivity activity = fragment.getActivity();
		OsmEditingPlugin plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		OsmEditsUploadListenerHelper helper = new OsmEditsUploadListenerHelper(activity,
				activity.getResources().getString(R.string.local_openstreetmap_were_uploaded));

		ProgressDialogFragment dialog = ProgressDialogFragment.createInstance(
				R.string.uploading,
				R.string.local_openstreetmap_uploading,
				ProgressDialog.STYLE_HORIZONTAL);
		dialog.show(activity.getSupportFragmentManager(), ProgressDialogFragment.TAG);
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(
				dialog, helper, plugin, toUpload.length, false, false);
		uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, toUpload);
	}

	public static final class UploadingErrorDialogFragment extends DialogFragment {
		private static final String ERROR_MESSAGE = "error_message";
		private static final String POINT = "point";

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle argument = getArguments();
			String errorMessage = argument.getString(ERROR_MESSAGE);
			OsmPoint point = AndroidUtils.getSerializable(argument, POINT, OsmPoint.class);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getResources().getString(R.string.failed_to_upload))
					.setMessage(MessageFormat.format(
							getResources().getString(R.string.error_message_pattern), errorMessage))
					.setPositiveButton(R.string.shared_string_ok, null);
			builder.setNeutralButton(getResources().getString(R.string.delete_change),
					new DialogInterface.OnClickListener() {
						public void onClick(@Nullable DialogInterface dialog, int id) {
							OsmEditingPlugin plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
							assert point != null;
							assert plugin != null;
							if (point.getGroup() == OsmPoint.Group.BUG) {
								plugin.getDBBug().deleteAllBugModifications(
										(OsmNotesPoint) point);
							} else if (point.getGroup() == OsmPoint.Group.POI) {
								plugin.getDBPOI().deletePOI((OpenstreetmapPoint) point);
							}
						}
					});
			return builder.create();
		}

		public static UploadingErrorDialogFragment getInstance(String errorMessage,
															   OsmPoint point) {
			UploadingErrorDialogFragment fragment = new UploadingErrorDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putString(ERROR_MESSAGE, errorMessage);
			bundle.putSerializable(POINT, point);
			fragment.setArguments(bundle);
			return fragment;
		}
	}

	public static final class UploadingMultipleErrorDialogFragment extends DialogFragment {
		private static final String HAS_ERROR = "has_error";
		private static final String POINT_NAMES = "point_names";
		private static final String POINTS_WITH_ERRORS = "points_with_errors";

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle arguments = getArguments();
			String[] pointNames = arguments.getStringArray(POINT_NAMES);
			boolean[] hasErrors = arguments.getBooleanArray(HAS_ERROR);
			OsmPoint[] points = AndroidUtils.getSerializable(arguments, POINTS_WITH_ERRORS, OsmPoint[].class);
			int successfulUploads = 0;
			assert hasErrors != null;
			for (boolean hasError : hasErrors) {
				if (!hasError) {
					successfulUploads++;
				}
			}
			PointsWithErrorsAdapter adapter =
					PointsWithErrorsAdapter.createInstance(getActivity(), pointNames, hasErrors);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(MessageFormat.format(getResources()
							.getString(R.string.successfully_uploaded_pattern),
					successfulUploads, hasErrors.length))
					.setAdapter(adapter, null)
					.setPositiveButton(R.string.shared_string_ok, null)
					.setNeutralButton(getResources().getString(R.string.try_again),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									showUploadItemsProgressDialog(
													UploadingMultipleErrorDialogFragment.this,
													points);
								}
							});
			return builder.create();
		}

		public static UploadingMultipleErrorDialogFragment createInstance(
				Map<OsmPoint, String> loadErrorsMap) {
			String[] pointNames = new String[loadErrorsMap.keySet().size()];
			boolean[] hasErrors = new boolean[loadErrorsMap.keySet().size()];
			ArrayList<OsmPoint> pointsWithErrors = new ArrayList<>();
			int i = 0;
			for (Map.Entry<OsmPoint, String> entry : loadErrorsMap.entrySet()) {
				OsmPoint point = entry.getKey();
				pointNames[i] = point.getGroup() == OsmPoint.Group.BUG ?
						((OsmNotesPoint) point).getText() :
						((OpenstreetmapPoint) point).getName();
				pointNames[i] = TextUtils.isEmpty(pointNames[i]) ?
						"id:" + point.getId() : pointNames[i];
				hasErrors[i] = entry.getValue() != null;
				if (hasErrors[i]) {
					pointsWithErrors.add(point);
				}
				i++;
			}

			if (pointNames.length != hasErrors.length) {
				throw new IllegalArgumentException("pointNames and hasError arrays " +
						"must me of equal length");
			}
			UploadingMultipleErrorDialogFragment fragment =
					new UploadingMultipleErrorDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable(POINTS_WITH_ERRORS,
					pointsWithErrors.toArray(new OsmPoint[0]));
			bundle.putStringArray(POINT_NAMES, pointNames);
			bundle.putBooleanArray(HAS_ERROR, hasErrors);
			fragment.setArguments(bundle);
			return fragment;
		}
	}

	private static final class PointWithPotentialError {
		String point;
		boolean hasError;

		public PointWithPotentialError(String point, boolean hasError) {
			this.point = point;
			this.hasError = hasError;
		}
	}

	private static final class PointsWithErrorsAdapter extends ArrayAdapter<PointWithPotentialError> {
		private final int layoutResourceId;
		PointWithPotentialError[] data;
		Activity context;

		private PointsWithErrorsAdapter(Activity context, int layoutResourceId,
										PointWithPotentialError[] objects) {
			super(context, layoutResourceId, objects);
			data = objects;
			this.context = context;
			this.layoutResourceId = layoutResourceId;
		}

		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			View row = convertView;
			PointHolder holder;

			if (row == null) {
				LayoutInflater inflater = context.getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);

				holder = new PointHolder();
				holder.checkedUncheckedImageView = row.findViewById(R.id.iconImageView);
				holder.pointNameTextView = row.findViewById(R.id.nameTextView);

				row.setTag(holder);
			} else {
				holder = (PointHolder) row.getTag();
			}

			PointWithPotentialError pointWrapper = data[position];
			holder.pointNameTextView.setText(pointWrapper.point);
			UiUtilities cache = ((OsmandApplication) context.getApplication()).getUIUtilities();
			holder.checkedUncheckedImageView.setImageDrawable(pointWrapper.hasError ?
					cache.getThemedIcon(R.drawable.ic_action_remove_dark) :
					cache.getThemedIcon(R.drawable.ic_action_done));

			return row;
		}

		public static PointsWithErrorsAdapter createInstance(Activity activity,
															 String[] pointNames,
															 boolean[] hasError) {
			PointWithPotentialError[] array = new PointWithPotentialError[pointNames.length];
			for (int i = 0; i < pointNames.length; i++) {
				array[i] = new PointWithPotentialError(pointNames[i], hasError[i]);
			}
			return new PointsWithErrorsAdapter(activity, R.layout.osm_edit_list_item, array);
		}

		private static class PointHolder {
			TextView pointNameTextView;
			ImageView checkedUncheckedImageView;
		}
	}
}
