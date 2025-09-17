package net.osmand.plus.settings.datastorage;


import android.app.AlertDialog;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageMigrationFragment extends BaseFullScreenDialogFragment implements StorageMigrationListener {

	private static final String TAG = StorageMigrationFragment.class.getSimpleName();

	private static final String USED_ON_MAP_KEY = "used_on_map";
	private static final String FILES_COUNT_KEY = "files_count";
	private static final String REMAINING_COUNT_KEY = "remaining_count";
	private static final String GENERAL_PROGRESS_KEY = "general_progress";
	private static final String REMAINING_SIZE_KEY = "remaining_size";
	private static final String COPY_FINISHED_KEY = "copy_finished";
	private static final String FILES_SIZE_KEY = "files_size";
	private static final String ESTIMATED_SIZE_KEY = "estimated_size";
	private static final String EXISTING_FILES_KEY = "existing_files";
	private static final String SELECTED_STORAGE_KEY = "selected_storage";
	private static final String CURRENT_STORAGE_KEY = "current_storage";

	private StorageItem selectedStorage;
	private StorageItem currentStorage;

	private StorageMigrationRestartListener restartListener;
	private MoveFilesStopListener stopTaskListener;
	private View mainView;
	private View remainingFiles;
	private View copyFilesDescr;
	private TextView progressTitle;
	private ProgressBar progressBar;

	private Pair<Long, Long> filesSize;
	private Map<String, Pair<String, Long>> errors = new HashMap<>();
	private List<File> existingFiles = new ArrayList<>();

	private int filesCount;
	private long remainingSize;
	private int remainingCount;
	private int generalProgress;
	private boolean copyFinished;

	private boolean usedOnMap;

	private AlertDialog closeDialog = null;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null && filesSize == null) {
			filesCount = savedInstanceState.getInt(FILES_COUNT_KEY);
			remainingSize = savedInstanceState.getLong(REMAINING_SIZE_KEY);
			remainingCount = savedInstanceState.getInt(REMAINING_COUNT_KEY);
			generalProgress = savedInstanceState.getInt(GENERAL_PROGRESS_KEY);
			copyFinished = savedInstanceState.getBoolean(COPY_FINISHED_KEY);
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
			selectedStorage = savedInstanceState.getParcelable(SELECTED_STORAGE_KEY);
			currentStorage = savedInstanceState.getParcelable(CURRENT_STORAGE_KEY);

			long size = savedInstanceState.getLong(FILES_SIZE_KEY);
			long estimatedSize = savedInstanceState.getLong(ESTIMATED_SIZE_KEY);
			filesSize = new Pair<>(size, estimatedSize);

			ArrayList<String> filePaths = savedInstanceState.getStringArrayList(EXISTING_FILES_KEY);
			if (filePaths != null) {
				for (String path : filePaths) {
					existingFiles.add(new File(path));
				}
			}
		}
	}

	@Override
	protected boolean isUsedOnMap() {
		return usedOnMap;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		mainView = inflate(R.layout.copy_files_fragment, container, false);
		ViewCompat.setNestedScrollingEnabled(mainView.findViewById(R.id.list), true);
		updateContent();
		return mainView;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(FILES_COUNT_KEY, filesCount);
		outState.putLong(REMAINING_SIZE_KEY, remainingSize);
		outState.putInt(REMAINING_COUNT_KEY, remainingCount);
		outState.putInt(GENERAL_PROGRESS_KEY, generalProgress);
		outState.putBoolean(COPY_FINISHED_KEY, copyFinished);
		outState.putBoolean(USED_ON_MAP_KEY, usedOnMap);
		outState.putLong(FILES_SIZE_KEY, filesSize.first);
		outState.putLong(ESTIMATED_SIZE_KEY, filesSize.second);
		outState.putParcelable(SELECTED_STORAGE_KEY, selectedStorage);
		outState.putParcelable(CURRENT_STORAGE_KEY, currentStorage);

		ArrayList<String> filePaths = new ArrayList<>();
		for (File file : existingFiles) {
			filePaths.add(file.getPath());
		}
		outState.putStringArrayList(EXISTING_FILES_KEY, filePaths);
	}

	private void updateContent() {
		setupToolbar();
		setupButtons();
		setupRestartDescr();
		setupRemainingFiles();
		setupProgressContainer();
	}

	private void setupToolbar() {
		CollapsingToolbarLayout toolbarLayout = mainView.findViewById(R.id.toolbar_layout);
		toolbarLayout.setTitle(copyFinished ? getString(R.string.copying_completed) : getString(R.string.copying_osmand_files));
		Toolbar toolbar = mainView.findViewById(R.id.toolbar);

		toolbar.setNavigationIcon(copyFinished ? null : getIcon(R.drawable.ic_action_close));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			closeDialog = createCancelDialog();
			closeDialog.show();
		});
	}

	private void updateDialogMessage() {
		int maxProgress = progressBar.getMax();
		int percentage = maxProgress != 0 ? ProgressHelper.normalizeProgressPercent(generalProgress * 100 / maxProgress) : 0;
		closeDialog.setMessage(getString(R.string.storage_migration_fragment_close_descr, String.valueOf(percentage)));
	}

	private AlertDialog createCancelDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		int maxProgress = progressBar.getMax();
		int percentage = maxProgress != 0 ? ProgressHelper.normalizeProgressPercent(generalProgress * 100 / maxProgress) : 0;

		builder.setMessage(getString(R.string.storage_migration_fragment_close_descr, String.valueOf(percentage)))
				.setTitle(R.string.osmand_parking_warning);

		builder.setPositiveButton(R.string.shared_string_continue, (dialog, id) -> dialog.dismiss());

		builder.setNeutralButton(R.string.stop_and_exit, (dialog, id) -> {
			stopTaskListener.onStopTask();
			dialog.dismiss();
			dismiss();
		});

		return builder.create();
	}

	private void setupButtons() {
		DialogButton actionButton = mainView.findViewById(R.id.dismiss_button);
		actionButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (restartListener != null) {
				restartListener.onRestartSelected();
			} else {
				if (activity != null) {
					RestartActivity.doRestartSilent(activity);
				}
			}
		});
		actionButton.setButtonType(DialogButtonType.PRIMARY);
		actionButton.setTitleId(R.string.shared_string_restart);
		AndroidUiHelper.updateVisibility(actionButton, copyFinished);
	}

	private void setupProgressContainer() {
		progressBar = mainView.findViewById(R.id.progress_bar);
		progressTitle = mainView.findViewById(R.id.progress_title);
		copyFilesDescr = mainView.findViewById(R.id.copy_files_descr);

		progressBar.setMax((int) (filesSize.second / 1024));

		setupFilesTitle();
		setupFilesDescr();
		updateProgress(generalProgress);
		AndroidUiHelper.updateVisibility(copyFilesDescr.findViewById(android.R.id.icon), false);
	}

	private void updateProgress(int progress) {
		progressBar.setProgress(progress);

		int maxProgress = progressBar.getMax();
		int percentage = maxProgress != 0 ? ProgressHelper.normalizeProgressPercent(progress * 100 / maxProgress) : 0;
		progressTitle.setText(getString(R.string.progress_complete, percentage + "%"));
	}

	private void setupFilesTitle() {
		String amount = String.valueOf(filesCount);
		String formattedSize = "(" + AndroidUtils.formatSize(app, filesSize.first) + ")";
		String warning = getString(copyFinished ? R.string.storage_copied_files_size : R.string.storage_copying_files_size, amount, formattedSize);

		SpannableStringBuilder spannable = new SpannableStringBuilder(warning);
		int index = warning.indexOf(amount);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), index, index + amount.length(), 0);
		index = warning.indexOf(formattedSize);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode)), index, index + formattedSize.length(), 0);

		if (copyFinished) {
			if (!Algorithms.isEmpty(errors)) {
				long size = 0;
				for (Pair<String, Long> pair : errors.values()) {
					size += pair.second;
				}
				spannable.append("\n").append(getString(R.string.files_failed, errors.size(), AndroidUtils.formatSize(app, size)));
			}
			if (!Algorithms.isEmpty(existingFiles)) {
				long size = 0;
				for (File file : existingFiles) {
					size += file.length();
				}
				String currentStorage = selectedStorage.getTitle();
				spannable.append("\n").append(getString(R.string.migration_files_present,
						existingFiles.size(), AndroidUtils.formatSize(app, size), currentStorage));
			}
		}
		TextView warningInfo = copyFilesDescr.findViewById(android.R.id.title);
		warningInfo.setSingleLine(false);
		warningInfo.setText(spannable);
	}

	private void setupFilesDescr() {
		String currentStorageName = "\"" + currentStorage.getTitle() + "\"";
		String selectedStorageName = "\"" + selectedStorage.getTitle() + "\"";
		String description = getString(R.string.from_to_with_params, currentStorageName, selectedStorageName);

		TextView summary = copyFilesDescr.findViewById(android.R.id.summary);
		summary.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), description, currentStorageName, selectedStorageName));
	}

	private void setupRestartDescr() {
		View container = mainView.findViewById(R.id.restart_required);
		TextView title = container.findViewById(android.R.id.title);
		title.setText(R.string.restart_is_required);
		AndroidUiHelper.updateVisibility(container, copyFinished);
		AndroidUiHelper.updateVisibility(container.findViewById(android.R.id.icon), false);
	}

	private void setupRemainingFiles() {
		remainingFiles = mainView.findViewById(R.id.remaining_files);

		String amount = String.valueOf(remainingCount);
		String formattedSize = "(" + AndroidUtils.formatSize(app, remainingSize) + ")";
		String warning = getString(R.string.files_remaining, amount, formattedSize);

		SpannableString spannable = new SpannableString(warning);
		int index = warning.indexOf(amount);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), index, index + amount.length(), 0);
		index = warning.indexOf(formattedSize);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode)), index, index + formattedSize.length(), 0);

		TextView title = remainingFiles.findViewById(android.R.id.title);
		title.setText(spannable);

		AndroidUiHelper.updateVisibility(remainingFiles, !copyFinished);
	}

	@Override
	public void onFileCopyStarted(@NonNull String path) {
		if (isAdded()) {
			String fileName = Algorithms.getFileWithoutDirs(path);
			FileSubtype subtype = FileSubtype.getSubtypeByFileName(path);
			if (subtype == FileSubtype.TILES_MAP) {
				fileName = path.replace(IndexConstants.TILES_INDEX_DIR, "");
			} else if (subtype.isMap() || subtype == FileSubtype.TTS_VOICE || subtype == FileSubtype.VOICE) {
				fileName = FileNameTranslationHelper.getFileNameWithRegion(app, fileName);
			} else if (subtype == FileSubtype.GPX) {
				fileName = GpxHelper.INSTANCE.getGpxTitle(fileName);
			}

			String description = getString(R.string.copying_file, fileName);
			SpannableString spannable = new SpannableString(description);
			int index = description.indexOf(fileName);
			spannable.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), index, index + fileName.length(), 0);
			spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getActiveColor(app, nightMode)), index, index + fileName.length(), 0);

			TextView summary = remainingFiles.findViewById(android.R.id.summary);
			summary.setText(spannable);
		}
	}

	@Override
	public void onRemainingFilesUpdate(@NonNull Pair<Integer, Long> pair) {
		remainingSize = pair.second;
		remainingCount = pair.first;
		if (isAdded()) {
			setupRemainingFiles();
		}
	}

	@Override
	public void onFilesCopyProgress(int progress) {
		generalProgress = progress;
		if (isAdded()) {
			updateProgress(progress);
			if (closeDialog != null && closeDialog.isShowing()) {
				updateDialogMessage();
			}
		}
	}

	@Override
	public void onFilesCopyFinished(@NonNull Map<String, Pair<String, Long>> errors, @NonNull List<File> existingFiles) {
		copyFinished = true;
		this.errors = errors;
		this.existingFiles = existingFiles;
		generalProgress = (int) (filesSize.second / 1024);
		app.getSettings().SHARED_STORAGE_MIGRATION_FINISHED.set(true);
		if (isAdded()) {
			updateContent();
			if (closeDialog != null && closeDialog.isShowing()) {
				closeDialog.dismiss();
			}
		}
	}

	public static StorageMigrationListener showInstance(@NonNull FragmentManager fragmentManager,
	                                                    @NonNull StorageItem selectedStorage,
	                                                    @NonNull StorageItem currentStorage,
	                                                    @NonNull Pair<Long, Long> filesSize,
	                                                    int generalProgress,
	                                                    int filesCount,
	                                                    boolean usedOnMap,
	                                                    @Nullable StorageMigrationRestartListener listener,
	                                                    @Nullable MoveFilesStopListener cancelTaskListener) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			StorageMigrationFragment fragment = new StorageMigrationFragment();
			fragment.usedOnMap = usedOnMap;
			fragment.filesSize = filesSize;
			fragment.filesCount = filesCount;
			fragment.selectedStorage = selectedStorage;
			fragment.generalProgress = generalProgress;
			fragment.currentStorage = currentStorage;
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
			fragment.restartListener = listener;
			fragment.stopTaskListener = cancelTaskListener;
			return fragment;
		}
		return null;
	}
}

