package net.osmand.plus.settings.datastorage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.OnDismissDialogFragmentListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.ui.DataStoragePlaceDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.DocumentFilesCollectTask.FilesCollectListener;
import net.osmand.plus.settings.datastorage.SkipMigrationBottomSheet.OnConfirmMigrationSkipListener;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SharedStorageWarningFragment extends BaseOsmAndFragment implements OnConfirmMigrationSkipListener, OnDismissDialogFragmentListener, FilesCollectListener {

	public static final String TAG = SharedStorageWarningFragment.class.getSimpleName();

	private static final int FOLDER_ACCESS_REQUEST = 1009;

	private OsmandApplication app;
	private DataStorageHelper storageHelper;
	private DocumentFilesCollectTask collectTask;
	private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	private String selectedStorageKey;
	private DocumentFile folderFile;
	private List<DocumentFile> documentFiles = new ArrayList<>();
	private long filesSize;

	private View mainView;
	private View stepsContainer;
	private View foldersContainer;
	private View progressContainer;
	private View buttonsContainer;

	private boolean nightMode;
	private boolean usedOnMap;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		storageHelper = new DataStorageHelper(app);
		nightMode = isNightMode(usedOnMap);

		if (selectedStorageKey == null) {
			selectedStorageKey = DataStorageHelper.INTERNAL_STORAGE;
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
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		mainView = themedInflater.inflate(R.layout.shared_storage_warning, container, false);

		stepsContainer = mainView.findViewById(R.id.steps_container);
		foldersContainer = mainView.findViewById(R.id.migration_folders);
		progressContainer = mainView.findViewById(R.id.progress_container);
		buttonsContainer = mainView.findViewById(R.id.control_buttons);

		setupToolbar();
		updateContent();
		AndroidUtils.addStatusBarPadding21v(mainView.getContext(), mainView);
		ViewCompat.setNestedScrollingEnabled(mainView.findViewById(R.id.list), true);

		return mainView;
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
			migrationDescr.setText(UiUtilities.createCustomFontSpannable(FontCache.getRobotoMedium(app), warning, sharedStorage));

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

		String storageName = storageHelper.getStorage(selectedStorageKey).getTitle();
		SpannableString spannable = new SpannableString(storageName);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), 0, storageName.length(), 0);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getActiveColor(app, nightMode)), 0, storageName.length(), 0);
		summary.setText(spannable);

		View selectableItem = container.findViewById(R.id.selectable_list_item);
		selectableItem.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				DataStoragePlaceDialogFragment.showInstance(activity.getSupportFragmentManager(), this, false);
			}
		});
		AndroidUtils.setBackground(selectableItem, UiUtilities.getSelectableDrawable(app));
	}

	private void setupFoundFilesSize(@NonNull View view) {
		String amount = String.valueOf(documentFiles.size());
		String formattedSize = "(" + AndroidUtils.formatSize(app, filesSize) + ")";
		String warning = getString(R.string.storage_found_files_size, amount, formattedSize);

		SpannableString spannable = new SpannableString(warning);
		int index = warning.indexOf(amount);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), index, index + amount.length(), 0);
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
		String storageName = storageHelper.getStorage(selectedStorageKey).getTitle();

		TextView title = view.findViewById(R.id.found_files_descr);
		title.setText(getString(R.string.storage_found_files_descr, startCopying, amount, sharedStorage, storageName));
	}

	private void setupButtons() {
		View skipButton = buttonsContainer.findViewById(R.id.dismiss_button);
		View rightButton = buttonsContainer.findViewById(R.id.right_bottom_button);
		View buttonsShadow = buttonsContainer.findViewById(R.id.buttons_shadow);
		View buttonsDivider = buttonsContainer.findViewById(R.id.buttons_divider);
		View bottomButtons = buttonsContainer.findViewById(R.id.buttons_container);

		boolean folderSelected = folderFile != null;
		if (folderSelected) {
			skipButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					StorageItem currentStorage = storageHelper.getCurrentStorage();
					if (!Algorithms.stringsEqual(currentStorage.getKey(), selectedStorageKey)) {
						StorageItem selectedStorage = storageHelper.getStorage(selectedStorageKey);
						File dir = new File(selectedStorage.getDirectory());
						int type = selectedStorage.getType();
						DataStoragePlaceDialogFragment.saveFilesLocation(activity, type, dir);
						DataStoragePlaceDialogFragment.checkAssets(app);
						DataStoragePlaceDialogFragment.updateDownloadIndexes(app);
					}
					StorageMigrationAsyncTask copyFilesTask = new StorageMigrationAsyncTask(activity, documentFiles, filesSize, usedOnMap);
					copyFilesTask.executeOnExecutor(singleThreadExecutor);
					dismiss();
				}
			});
			bottomButtons.setBackground(null);
			UiUtilities.setupDialogButton(nightMode, skipButton, DialogButtonType.PRIMARY, R.string.start_copying);
		} else {
			skipButton.setOnClickListener(v -> showSkipMigrationDialog());
			rightButton.setOnClickListener(v -> {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
				startActivityForResult(intent, FOLDER_ACCESS_REQUEST);
			});
			UiUtilities.setupDialogButton(nightMode, skipButton, DialogButtonType.SECONDARY, R.string.shared_string_skip);
			UiUtilities.setupDialogButton(nightMode, rightButton, DialogButtonType.PRIMARY, R.string.shared_string_continue);
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
	public void onDismissDialogFragment(DialogFragment dialogFragment) {
		storageHelper = new DataStorageHelper(app);
		selectedStorageKey = storageHelper.getCurrentStorage().getKey();
		updateContent();
	}

	@Override
	public void onFilesCollectingStarted() {
		updateContent();
	}

	@Override
	public void onFilesCollectingFinished(@NonNull DocumentFile folder, @NonNull List<DocumentFile> files, long size) {
		filesSize = size;
		folderFile = folder;
		documentFiles = files;
		collectTask = null;

		updateContent();
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.remove(SharedStorageWarningFragment.this)
					.commitAllowingStateLoss();
		}
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
		collectTask.executeOnExecutor(singleThreadExecutor);
	}

	public static boolean dialogShowRequired(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return Build.VERSION.SDK_INT >= 30 && DataStorageHelper.isCurrentStorageShared(app)
				&& !settings.SHARED_STORAGE_MIGRATION_FINISHED.get();
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
}