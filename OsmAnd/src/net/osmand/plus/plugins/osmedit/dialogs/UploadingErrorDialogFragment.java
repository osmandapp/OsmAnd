package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.utils.AndroidUtils;

import java.text.MessageFormat;

public final class UploadingErrorDialogFragment extends BaseAlertDialogFragment {
	private static final String TAG = UploadingErrorDialogFragment.class.getSimpleName();
	private static final String ERROR_MESSAGE = "error_message";
	private static final String POINT = "point";

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		Bundle argument = requireArguments();
		String errorMessage = argument.getString(ERROR_MESSAGE);
		OsmPoint point = AndroidUtils.getSerializable(argument, POINT, OsmPoint.class);
		AlertDialog.Builder builder = createDialogBuilder();
		builder.setTitle(getString(R.string.failed_to_upload))
				.setMessage(MessageFormat.format(getString(R.string.error_message_pattern), errorMessage))
				.setPositiveButton(R.string.shared_string_ok, null);
		builder.setNeutralButton(getString(R.string.delete_change), (dialog, id) -> {
			OsmEditingPlugin plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
			assert point != null;
			assert plugin != null;
			if (point.getGroup() == OsmPoint.Group.BUG) {
				plugin.getDBBug().deleteAllBugModifications((OsmNotesPoint) point);
			} else if (point.getGroup() == OsmPoint.Group.POI) {
				plugin.getDBPOI().deletePOI((OpenstreetmapPoint) point);
			}
		});
		return builder.create();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable String errorMessage, @NonNull OsmPoint point) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			UploadingErrorDialogFragment fragment = new UploadingErrorDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putString(ERROR_MESSAGE, errorMessage);
			bundle.putSerializable(POINT, point);
			fragment.setArguments(bundle);
			fragment.show(fragmentManager, TAG);
		}
	}
}
