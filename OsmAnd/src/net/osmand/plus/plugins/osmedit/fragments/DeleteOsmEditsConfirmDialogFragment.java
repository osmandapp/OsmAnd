package net.osmand.plus.plugins.osmedit.fragments;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class DeleteOsmEditsConfirmDialogFragment extends BaseAlertDialogFragment {

	public static final String TAG = DeleteOsmEditsConfirmDialogFragment.class.getSimpleName();
	private static final String POINTS_LIST = "points_list";

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		List<OsmPoint> points = (List<OsmPoint>) AndroidUtils.getSerializable(requireArguments(), POINTS_LIST, ArrayList.class);

		AlertDialog.Builder builder = createDialogBuilder();
		assert points != null;
		builder.setMessage(getString(R.string.local_osm_changes_delete_all_confirm, points.size()));
		builder.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
			if (getParentFragment() instanceof DeleteOsmEditsConfirmDialogListener listener) {
				listener.onDeleteItemsConfirmed(points);
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		return builder.create();
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager,
	                                @NonNull ArrayList<OsmPoint> points) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			DeleteOsmEditsConfirmDialogFragment fragment = new DeleteOsmEditsConfirmDialogFragment();
			Bundle args = new Bundle();
			args.putSerializable(POINTS_LIST, points);
			fragment.setArguments(args);
			fragment.show(childFragmentManager, TAG);
		}
	}

	public interface DeleteOsmEditsConfirmDialogListener {
		void onDeleteItemsConfirmed(@NonNull List<OsmPoint> points);
	}
}
