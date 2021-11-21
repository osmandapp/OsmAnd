package net.osmand.plus.settings.datastorage;

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

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.datastorage.StorageMigrationAsyncTask.StorageMigrationListener;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageMigrationFragment extends BaseOsmAndDialogFragment implements StorageMigrationListener {

	private static final String TAG = StorageMigrationFragment.class.getSimpleName();

	private OsmandApplication app;
	private StorageItem selectedStorage;

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

	private boolean nightMode;
	private boolean usedOnMap;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		nightMode = isNightMode(usedOnMap);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		mainView = themedInflater.inflate(R.layout.copy_files_fragment, container, false);
		ViewCompat.setNestedScrollingEnabled(mainView.findViewById(R.id.list), true);

		updateContent();

		return mainView;
	}

	private void updateContent() {
		setupToolbar();
		setupButtons();
		setupRestartDescr();
		setupRemainingFiles();
		setupProgressContainer();
	}

	private void setupToolbar() {
		Toolbar toolbar = mainView.findViewById(R.id.toolbar);
		toolbar.setTitle(copyFinished ? R.string.copying_completed : R.string.copying_osmand_files);
		toolbar.setNavigationIcon(copyFinished ? null : getIcon(AndroidUtils.getNavigationIconResId(app)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void setupButtons() {
		View actionButton = mainView.findViewById(R.id.dismiss_button);
		actionButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity instanceof MapActivity) {
				MapActivity.doRestart(activity);
			} else {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});
		UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.PRIMARY, R.string.shared_string_restart);
		AndroidUiHelper.updateVisibility(actionButton, copyFinished);
	}

	private void setupProgressContainer() {
		progressBar = mainView.findViewById(R.id.progress_bar);
		progressTitle = mainView.findViewById(R.id.progress_title);
		copyFilesDescr = mainView.findViewById(R.id.copy_files_descr);

		progressBar.setMin(0);
		progressBar.setMax((int) (filesSize.second / 1024));

		setupFilesTitle();
		setupFilesDescr();
		updateProgress(generalProgress);
		AndroidUiHelper.updateVisibility(copyFilesDescr.findViewById(android.R.id.icon), false);
	}

	private void updateProgress(int progress) {
		generalProgress = progress;
		progressBar.setProgress(progress);

		int maxProgress = progressBar.getMax();
		int percentage = maxProgress != 0 ? BasicProgressAsyncTask.normalizeProgress(progress * 100 / maxProgress) : 0;
		progressTitle.setText(getString(R.string.progress_complete, percentage + "%"));
	}

	private void setupFilesTitle() {
		String amount = String.valueOf(filesCount);
		String formattedSize = "(" + AndroidUtils.formatSize(app, filesSize.first) + ")";
		String warning = getString(copyFinished ? R.string.storage_copied_files_size : R.string.storage_copying_files_size, amount, formattedSize);

		SpannableStringBuilder spannable = new SpannableStringBuilder(warning);
		int index = warning.indexOf(amount);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), index, index + amount.length(), 0);
		index = warning.indexOf(formattedSize);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode)), index, index + formattedSize.length(), 0);

		if (copyFinished) {
			if (!Algorithms.isEmpty(errors)) {
				long size = 0;
				for (Pair<String, Long> pair : errors.values()) {
					size += pair.second;
				}
				spannable.append("\n").append(getString(R.string.files_failed, errors.size(), size));
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
		String sharedStorage = "\"" + getString(R.string.shared_storage) + "\"";
		String currentStorage = "\"" + selectedStorage.getTitle() + "\"";
		String description = getString(R.string.from_to_with_params, sharedStorage, currentStorage);

		TextView summary = copyFilesDescr.findViewById(android.R.id.summary);
		summary.setText(UiUtilities.createCustomFontSpannable(FontCache.getRobotoMedium(app), description, sharedStorage, currentStorage));
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
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), index, index + amount.length(), 0);
		index = warning.indexOf(formattedSize);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode)), index, index + formattedSize.length(), 0);

		TextView title = remainingFiles.findViewById(android.R.id.title);
		title.setText(spannable);

		AndroidUiHelper.updateVisibility(remainingFiles, !copyFinished);
		AndroidUiHelper.updateVisibility(remainingFiles.findViewById(android.R.id.icon), false);
	}

	@Override
	public void onFileCopyStarted(@NonNull String path) {
		String fileName = Algorithms.getFileWithoutDirs(path);
		FileSubtype subtype = FileSubtype.getSubtypeByFileName(path);
		if (subtype == FileSubtype.TILES_MAP) {
			fileName = path.replace(IndexConstants.TILES_INDEX_DIR, "");
		} else if (subtype.isMap() || subtype == FileSubtype.TTS_VOICE || subtype == FileSubtype.VOICE) {
			fileName = FileNameTranslationHelper.getFileNameWithRegion(app, fileName);
		} else if (subtype == FileSubtype.GPX) {
			fileName = GpxUiHelper.getGpxTitle(fileName);
		}

		String description = getString(R.string.copying_file, fileName);
		SpannableString spannable = new SpannableString(description);
		int index = description.indexOf(fileName);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), index, index + fileName.length(), 0);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getActiveColor(app, nightMode)), index, index + fileName.length(), 0);

		TextView summary = remainingFiles.findViewById(android.R.id.summary);
		summary.setText(spannable);
	}

	@Override
	public void onRemainingFilesUpdate(@NonNull Pair<Integer, Long> pair) {
		remainingSize = pair.second;
		remainingCount = pair.first;
		setupRemainingFiles();
	}

	@Override
	public void onFilesCopyProgress(int progress) {
		updateProgress(progress);
	}

	@Override
	public void onFilesCopyFinished(@NonNull Map<String, Pair<String, Long>> errors, @NonNull List<File> existingFiles) {
		copyFinished = true;
		this.errors = errors;
		this.existingFiles = existingFiles;
		generalProgress = (int) (filesSize.second / 1024);
		app.getSettings().SHARED_STORAGE_MIGRATION_FINISHED.set(true);
		updateContent();
	}

	public static StorageMigrationListener showInstance(@NonNull FragmentManager fragmentManager,
	                                                    @NonNull StorageItem selectedStorage,
	                                                    int generalProgress,
	                                                    int filesCount,
	                                                    Pair<Long, Long> filesSize,
	                                                    boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			StorageMigrationFragment fragment = new StorageMigrationFragment();
			fragment.usedOnMap = usedOnMap;
			fragment.filesSize = filesSize;
			fragment.filesCount = filesCount;
			fragment.selectedStorage = selectedStorage;
			fragment.generalProgress = generalProgress;
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
			return fragment;
		}
		return null;
	}
}
