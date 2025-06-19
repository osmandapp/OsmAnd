package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadOpenstreetmapPointAsyncTask;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.helpers.OsmEditsUploadListenerHelper;
import net.osmand.plus.utils.AndroidUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public final class UploadingMultipleErrorDialogFragment extends BaseAlertDialogFragment {
	private static final String TAG = UploadingMultipleErrorDialogFragment.class.getSimpleName();
	private static final String HAS_ERROR = "has_error";
	private static final String POINT_NAMES = "point_names";
	private static final String POINTS_WITH_ERRORS = "points_with_errors";

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		Bundle arguments = requireArguments();
		String[] pointNames = Objects.requireNonNull(arguments.getStringArray(POINT_NAMES));
		boolean[] hasErrors = Objects.requireNonNull(arguments.getBooleanArray(HAS_ERROR));
		OsmPoint[] points = Objects.requireNonNull(AndroidUtils.getSerializable(arguments, POINTS_WITH_ERRORS, OsmPoint[].class));
		int successfulUploads = 0;
		for (boolean hasError : hasErrors) {
			if (!hasError) {
				successfulUploads++;
			}
		}
		PointWithPotentialError[] array = new PointWithPotentialError[pointNames.length];
		for (int i = 0; i < pointNames.length; i++) {
			array[i] = new PointWithPotentialError(pointNames[i], hasErrors[i]);
		}
		PointsWithErrorsAdapter adapter = new PointsWithErrorsAdapter(getThemedContext(), R.layout.osm_edit_list_item, array);

		AlertDialog.Builder builder = createDialogBuilder();
		String msgPattern = getString(R.string.successfully_uploaded_pattern);
		builder.setTitle(MessageFormat.format(msgPattern, successfulUploads, hasErrors.length))
				.setAdapter(adapter, null)
				.setPositiveButton(R.string.shared_string_ok, null)
				.setNeutralButton(getString(R.string.try_again),
						(dialog, which) -> showUploadItemsProgressDialog(points));
		return builder.create();
	}

	private void showUploadItemsProgressDialog(@NonNull OsmPoint[] toUpload) {
		FragmentActivity activity = requireActivity();
		OsmEditingPlugin plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			OsmEditsUploadListenerHelper helper = new OsmEditsUploadListenerHelper(
					activity, getString(R.string.local_openstreetmap_were_uploaded));
			UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(
					showProgressDialog(), helper, plugin, toUpload.length, false, false);
			uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, toUpload);
		}
	}

	@NonNull
	private ProgressDialogFragment showProgressDialog() {
		return ProgressDialogFragment.showInstance(requireActivity().getSupportFragmentManager(),
				R.string.uploading, R.string.local_openstreetmap_uploading, ProgressDialog.STYLE_HORIZONTAL);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull Map<OsmPoint, String> loadErrorsMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
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
				throw new IllegalArgumentException("pointNames and hasError arrays must me of equal length");
			}
			UploadingMultipleErrorDialogFragment fragment = new UploadingMultipleErrorDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable(POINTS_WITH_ERRORS, pointsWithErrors.toArray(new OsmPoint[0]));
			bundle.putStringArray(POINT_NAMES, pointNames);
			bundle.putBooleanArray(HAS_ERROR, hasErrors);
			fragment.setArguments(bundle);
			fragment.show(fragmentManager, TAG);
		}
	}

	private final class PointsWithErrorsAdapter extends ArrayAdapter<PointWithPotentialError> {
		@LayoutRes private final int layoutId;
		private final PointWithPotentialError[] data;

		private PointsWithErrorsAdapter(@NonNull Context context, @LayoutRes int layoutId,
		                                @NonNull PointWithPotentialError[] objects) {
			super(context, layoutId, objects);
			this.data = objects;
			this.layoutId = layoutId;
		}

		@Override
		@NonNull
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			View row = convertView;
			PointHolder holder;

			if (row == null) {
				row = inflate(layoutId, parent, false);
				holder = new PointHolder(row.findViewById(R.id.nameTextView), row.findViewById(R.id.iconImageView));
				row.setTag(holder);
			} else {
				holder = (PointsWithErrorsAdapter.PointHolder) row.getTag();
			}

			PointWithPotentialError pointWrapper = data[position];
			int statusIcon = pointWrapper.hasError ? R.drawable.ic_action_remove_dark : R.drawable.ic_action_done;
			holder.tvTitle.setText(pointWrapper.point);
			holder.ivStatus.setImageDrawable(getContentIcon(statusIcon));
			return row;
		}

		private record PointHolder(@NonNull TextView tvTitle, @NonNull ImageView ivStatus) {}
	}

	private record PointWithPotentialError(@NonNull String point, boolean hasError) { }
}
