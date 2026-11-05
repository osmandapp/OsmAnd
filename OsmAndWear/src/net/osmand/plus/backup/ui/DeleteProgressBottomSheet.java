package net.osmand.plus.backup.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.commands.BaseDeleteFilesCommand;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

import java.util.List;
import java.util.Map;

public class DeleteProgressBottomSheet extends MenuBottomSheetDialogFragment implements OnDeleteFilesListener {

	public static final String TAG = DeleteProgressBottomSheet.class.getSimpleName();

	private static final String PROGRESS_KEY = "progress_key";
	private static final String MAX_PROGRESS_KEY = "max_progress_key";
	private static final String DELETION_FINISHED_KEY = "deletion_finished_key";

	private BackupHelper backupHelper;

	private TextView percentage;
	private ProgressBar progressBar;

	private int progress;
	private int maxProgress;
	private boolean deletionFinished;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		backupHelper = requiredMyApplication().getBackupHelper();
		if (savedInstanceState != null) {
			progress = savedInstanceState.getInt(PROGRESS_KEY);
			maxProgress = savedInstanceState.getInt(MAX_PROGRESS_KEY);
			deletionFinished = savedInstanceState.getBoolean(DELETION_FINISHED_KEY);
		}
		if (backupHelper.getExecutor().getActiveCommand(BaseDeleteFilesCommand.class) == null) {
			deletionFinished = true;
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(deletionFinished ? R.string.backup_deleted_all_data : R.string.backup_deleting_all_data)));

		if (!deletionFinished) {
			items.add(createProgressItem());
		}

		int descriptionId = deletionFinished ? R.string.backup_deleted_all_data_descr : R.string.backup_deleting_all_data_descr;
		BaseBottomSheetItem descriptionItem = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(descriptionId))
				.setTitleColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_title_long)
				.create();
		items.add(descriptionItem);

		int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		items.add(new DividerSpaceItem(requireContext(), padding));
	}

	private BaseBottomSheetItem createProgressItem() {
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_progress_with_description, null);

		percentage = view.findViewById(R.id.percentage);
		progressBar = view.findViewById(R.id.progress_bar);
		progressBar.setMax(maxProgress);
		updateProgress();

		return new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(PROGRESS_KEY, progress);
		outState.putInt(MAX_PROGRESS_KEY, maxProgress);
		outState.putBoolean(DELETION_FINISHED_KEY, deletionFinished);
	}

	@Override
	public void onResume() {
		super.onResume();
		backupHelper.getBackupListeners().addDeleteFilesListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		backupHelper.getBackupListeners().removeDeleteFilesListener(this);
	}

	@SuppressLint("SetTextI18n")
	private void updateProgress() {
		if (isAdded() && progressBar != null && percentage != null) {
			progressBar.setProgress(progress);
			percentage.setText(ProgressHelper.normalizeProgressPercent(maxProgress != 0 ? progress * 100 / maxProgress : 0) + "%");
		}
	}

	@Override
	public void onFilesDeleteStarted(@NonNull List<RemoteFile> files) {
		maxProgress = files.size();
	}

	@Override
	public void onFileDeleteProgress(@NonNull RemoteFile file, int progress) {
		this.progress = progress;
		updateProgress();
	}

	@Override
	public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {
		deletionFinished = true;
		updateMenuItems();
	}

	@Override
	public void onFilesDeleteError(int status, @NonNull String message) {
		deletionFinished = true;
		updateMenuItems();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, int maxProgress) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			DeleteProgressBottomSheet fragment = new DeleteProgressBottomSheet();
			fragment.maxProgress = maxProgress;

			fragmentManager.beginTransaction()
					.add(fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}