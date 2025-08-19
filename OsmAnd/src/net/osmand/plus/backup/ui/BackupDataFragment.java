package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class BackupDataFragment extends BackupTypesFragment {

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ExpandableListView expandableList = view.findViewById(R.id.list);
		expandableList.addHeaderView(createManageStorageItem());
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!controller.isBackupPreparing()) {
			updateContent();
		}
	}

	@Override
	public void updateContent() {
		super.updateContent();
		View view = getView();
		if (view != null) {
			updateManageStorageSummary(view);
		}
	}

	@NonNull
	private View createManageStorageItem() {
		View view = inflate(R.layout.preference_with_descr, null);

		TextView tvTitle = view.findViewById(android.R.id.title);
		tvTitle.setText(R.string.manage_storage);

		ImageView ivIcon = view.findViewById(android.R.id.icon);
		ivIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_storage));

		updateManageStorageSummary(view);

		view.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				ManageCloudStorageController.showScreen(activity);
			}
		});

		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		View selectableView = view.findViewById(R.id.selectable_list_item);
		UiUtilities.setupListItemBackground(view.getContext(), selectableView, activeColor);

		return view;
	}

	private void updateManageStorageSummary(@NonNull View view) {
		TextView tvSummary = view.findViewById(android.R.id.summary);
		long totalUsed = 0;
		for (ExportCategory category : ExportCategory.values()) {
			for (ExportType exportType : controller.getCategoryItems(category).getTypes()) {
				totalUsed += BackupUtils.calculateItemsSize(controller.getItemsForType(exportType));
			}
		}
		if (totalUsed > 0) {
			long maximumAccountSize = controller.getMaximumAccountSize();
			if (maximumAccountSize > 0) {
				String summary = getString(R.string.amount_of_total_used,
						formatSize(totalUsed), formatSize(maximumAccountSize));
				tvSummary.setText(summary);
			} else {
				tvSummary.setText(formatSize(totalUsed));
			}
		} else {
			tvSummary.setText(R.string.shared_string_none);
		}
	}

	private String formatSize(long size) {
		return AndroidUtils.formatSize(app, size);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String processId) {
		showInstance(manager, processId, new BackupDataFragment());
	}
}
