package net.osmand.plus.settings.datastorage;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.DataStorageFragment.StorageSelectionListener;
import net.osmand.plus.settings.datastorage.SkipMigrationBottomSheet.OnConfirmMigrationSkipListener;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.datastorage.task.DocumentFilesCollectTask;
import net.osmand.plus.settings.datastorage.task.DocumentFilesCollectTask.DocumentFilesCollectListener;
import net.osmand.plus.settings.datastorage.task.StorageMigrationAsyncTask;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SharedStorageWarningFragment extends BaseFullScreenFragment implements OnConfirmMigrationSkipListener, DocumentFilesCollectListener, StorageSelectionListener {

	public static final String TAG = SharedStorageWarningFragment.class.getSimpleName();

	public static final String STORAGE_MIGRATION = "storage_migration";
	private static final int FOLDER_ACCESS_REQUEST = 1009;

	private DataStorageHelper storageHelper;
	private DocumentFilesCollectTask collectTask;
	private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	private StorageItem selectedStorage;
	private DocumentFile folderFile;
	private List<DocumentFile> documentFiles = new ArrayList<>();
	private Pair<Long, Long> filesSize;

	private View mainView;
	private View stepsContainer;
	private View foldersContainer;
	private View progressContainer;
	private View buttonsContainer;

	private boolean usedOnMap;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	protected boolean isUsedOnMap() {
		return usedOnMap;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		storageHelper = new DataStorageHelper(app);

		if (selectedStorage == null) {
			selectedStorage = storageHelper.getStorage(DataStorageHelper.INTERNAL_STORAGE);
		}
		FragmentActivity activity = requireMyActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				showSkipMigrationDialog();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		mainView = inflate(R.layout.shared_storage_warning, container, false);

		stepsContainer = mainView.findViewById(R.id.steps_container);
		foldersContainer = mainView.findViewById(R.id.migration_folders);
		progressContainer = mainView.findViewById(R.id.progress_container);
		buttonsContainer = mainView.findViewById(R.id.control_buttons);

		setupToolbar();
		updateContent();
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), mainView);
		ViewCompat.setNestedScrollingEnabled(mainView.findViewById(R.id.list), true);

		return mainView;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	private void updateContent() {
		setupButtons();
		setupProgress();
		setupMigrationSteps();
		setupMigrationFolders();
	}

	private void setupToolbar() {
		Toolbar toolbar = mainView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> showSkipMigrationDialog());
	}

	private void showSkipMigrationDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SkipMigrationBottomSheet.showInstance(activity.getSupportFragmentManager(), this, usedOnMap);
		}
	}

	@Override
	public void onMigrationSkipConfirmed() {
		if (collectingFiles()) {
			stopCollectFilesTask();
		}
		dismiss();
	}

	private void setupFolderFrom(@NonNull View view) {
		View container = view.findViewById(R.id.folder_from);
		ImageView icon = container.findViewById(android.R.id.icon);
		TextView title = container.findViewById(android.R.id.title);
		TextView summary = container.findViewById(android.R.id.summary);

		summary.setText("/" + folderFile.getName() + "/");
		title.setText(Algorithms.capitalizeFirstLetter(getString(R.string.shared_string_from)));
		icon.setImageDrawable(getIcon(R.drawable.ic_action_folder_move, ColorUtilities.getActiveColorId(nightMode)));
	}

	private void setupMigrationSteps() {
		if (folderFile == null) {
			String sharedStorage = getString(R.string.shared_storage);
			String warning = getString(R.string.shared_storage_migration_descr, sharedStorage);
			TextView migrationDescr = stepsContainer.findViewById(R.id.shared_storage_migration);
			migrationDescr.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(migrationDescr.getTypeface()), warning, sharedStorage));

			TextView firstStep = stepsContainer.findViewById(R.id.shared_storage_first_step);
			firstStep.setText(getString(R.string.shared_storage_first_step, getString(R.string.shared_string_continue)));
		}
		AndroidUiHelper.updateVisibility(stepsContainer, folderFile == null && !collectingFiles());
	}

	private void setupMigrationFolders() {
		if (folderFile != null) {
			setupFolderTo(foldersContainer);
			setupFolderFrom(foldersContainer);
			setupFoundFilesSize(foldersContainer);
			setupFoundFilesDescr(foldersContainer);
		}
		AndroidUiHelper.updateVisibility(foldersContainer, folderFile != null && !collectingFiles());
	}

	private void setupFolderTo(@NonNull View view) {
		View container = view.findViewById(R.id.folder_to);
		ImageView icon = container.findViewById(android.R.id.icon);
		TextView title = container.findViewById(android.R.id.title);
		TextView summary = container.findViewById(android.R.id.summary);

		title.setText(R.string.shared_string_copy_to);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_folder_move_to, ColorUtilities.getActiveColorId(nightMode)));

		String storageName = selectedStorage.getTitle();
		SpannableString spannable = new SpannableString(storageName);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), 0, storageName.length(), 0);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getActiveColor(app, nightMode)), 0, storageName.length(), 0);
		summary.setText(spannable);

		View selectableItem = container.findViewById(R.id.selectable_list_item);
		selectableItem.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				Bundle args = new Bundle();
				args.putBoolean(STORAGE_MIGRATION, true);
				BaseSettingsFragment.showInstance(activity, SettingsScreenType.DATA_STORAGE, null, args, this);
			}
		});
		AndroidUtils.setBackground(selectableItem, UiUtilities.getSelectableDrawable(app));
	}

	private void setupFoundFilesSize(@NonNull View view) {
		String amount = String.valueOf(documentFiles.size());
		String formattedSize = "(" + AndroidUtils.formatSize(app, filesSize.first) + ")";
		String warning = getString(R.string.storage_found_files_size, amount, formattedSize);

		SpannableString spannable = new SpannableString(warning);
		int index = warning.indexOf(amount);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), index, index + amount.length(), 0);
		index = warning.indexOf(formattedSize);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode)), index, index + formattedSize.length(), 0);

		TextView foundFiles = view.findViewById(R.id.found_files);
		foundFiles.setText(spannable);

		TextView foundFilesDescr = view.findViewById(R.id.found_files_descr);
		foundFilesDescr.setText(getString(R.string.storage_found_files_descr));
	}

	private void setupFoundFilesDescr(@NonNull View view) {
		String amount = String.valueOf(documentFiles.size());
		String startCopying = getString(R.string.start_copying);
		String sharedStorage = getString(R.string.shared_storage);
		String storageName = selectedStorage.getTitle();

		TextView title = view.findViewById(R.id.found_files_descr);
		title.setText(getString(R.string.storage_found_files_descr, startCopying, amount, sharedStorage, storageName));
	}

	private void setupButtons() {
		DialogButton skipButton = buttonsContainer.findViewById(R.id.dismiss_button);
		DialogButton rightButton = buttonsContainer.findViewById(R.id.right_bottom_button);
		View buttonsShadow = buttonsContainer.findViewById(R.id.buttons_shadow);
		View buttonsDivider = buttonsContainer.findViewById(R.id.buttons_divider);
		View bottomButtons = buttonsContainer.findViewById(R.id.bottom_buttons_container);

		boolean folderSelected = folderFile != null;
		if (folderSelected) {
			skipButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					StorageMigrationAsyncTask copyFilesTask = new StorageMigrationAsyncTask(activity,
							documentFiles, selectedStorage, filesSize, usedOnMap);
					OsmAndTaskManager.executeTask(copyFilesTask, singleThreadExecutor);
					dismiss();
				}
			});
			bottomButtons.setBackground(null);
			skipButton.setButtonType(DialogButtonType.PRIMARY);
			skipButton.setTitleId(R.string.start_copying);
		} else {
			skipButton.setOnClickListener(v -> showSkipMigrationDialog());
			rightButton.setOnClickListener(v -> {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
				AndroidUtils.startActivityForResultIfSafe(this, intent, FOLDER_ACCESS_REQUEST);
			});
			skipButton.setButtonType(DialogButtonType.SECONDARY);
			skipButton.setTitleId(R.string.shared_string_skip);
			rightButton.setButtonType(DialogButtonType.PRIMARY);
			rightButton.setTitleId(R.string.shared_string_continue);
			bottomButtons.setBackgroundColor(AndroidUtils.getColorFromAttr(buttonsContainer.getContext(), R.attr.bg_color));
		}
		AndroidUiHelper.updateVisibility(rightButton, !folderSelected);
		AndroidUiHelper.updateVisibility(buttonsShadow, !folderSelected);
		AndroidUiHelper.updateVisibility(buttonsDivider, !folderSelected);
		AndroidUiHelper.updateVisibility(buttonsContainer, !collectingFiles());
	}

	private void setupProgress() {
		boolean collectingFiles = collectingFiles();
		AndroidUiHelper.updateVisibility(progressContainer, collectingFiles);
		mainView.setBackgroundColor(collectingFiles ? ColorUtilities.getActivityBgColor(app, nightMode)
				: ColorUtilities.getListBgColor(app, nightMode));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == FOLDER_ACCESS_REQUEST) {
			if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
				updateSelectedFolderFiles(data.getData());
			} else {
				app.showShortToastMessage(R.string.folder_access_denied);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onFilesCollectingStarted() {
		if (isAdded()) {
			updateContent();
		}
	}

	@Override
	public void onFilesCollectingFinished(@Nullable String error,
	                                      @NonNull DocumentFile folder,
	                                      @NonNull List<DocumentFile> files,
	                                      @NonNull Pair<Long, Long> size) {
		collectTask = null;
		if (Algorithms.isEmpty(error)) {
			filesSize = size;
			folderFile = folder;
			documentFiles = files;
		} else {
			app.showToastMessage(error);
		}
		if (isAdded()) {
			updateContent();
		}
	}

	private void dismiss() {
		callActivity(activity -> {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.remove(this)
					.commitAllowingStateLoss();
		});
	}

	private boolean collectingFiles() {
		return collectTask != null;
	}

	private void stopCollectFilesTask() {
		if (collectTask != null) {
			collectTask.cancel(false);
		}
	}

	private void updateSelectedFolderFiles(@NonNull Uri uri) {
		stopCollectFilesTask();
		collectTask = new DocumentFilesCollectTask(app, uri, this);
		OsmAndTaskManager.executeTask(collectTask, singleThreadExecutor);
	}

	public static boolean dialogShowRequired(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		File dir = settings.getExternalStorageDirectory();
		return !FileUtils.isWritable(dir) && !settings.SHARED_STORAGE_MIGRATION_FINISHED.get();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SharedStorageWarningFragment fragment = new SharedStorageWarningFragment();
			fragment.usedOnMap = usedOnMap;
			fragment.setRetainInstance(true);
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
		}
	}

	@Override
	public void onStorageSelected(@NonNull StorageItem storageItem) {
		selectedStorage = storageItem;
		setupMigrationFolders();
	}
}