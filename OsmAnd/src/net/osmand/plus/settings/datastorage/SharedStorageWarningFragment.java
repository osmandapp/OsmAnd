package net.osmand.plus.settings.datastorage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
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
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.SkipMigrationBottomSheet.OnConfirmMigrationSkipListener;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class SharedStorageWarningFragment extends BaseOsmAndFragment implements OnConfirmMigrationSkipListener {

	public static final String TAG = SharedStorageWarningFragment.class.getSimpleName();

	private static final String USED_ON_MAP_KEY = "used_on_map";
	private static final String FOLDER_URI_KEY = "folder_uri_key";
	private static final int FOLDER_ACCESS_REQUEST = 1009;

	private OsmandApplication app;
	private DataStorageHelper storageHelper;

	private Uri folderUri;
	private DocumentFile folderFile;
	private final List<DocumentFile> documentFiles = new ArrayList<>();

	private View stepsContainer;
	private View foldersContainer;
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

		if (savedInstanceState != null) {
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
			updateSelectedFolderFiles(savedInstanceState.getParcelable(FOLDER_URI_KEY));
		}
		nightMode = isNightMode(usedOnMap);

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
		View view = themedInflater.inflate(R.layout.shared_storage_warning, container, false);
		ViewCompat.setNestedScrollingEnabled(view.findViewById(R.id.list), true);
		AndroidUtils.addStatusBarPadding21v(view.getContext(), view);

		stepsContainer = view.findViewById(R.id.steps_container);
		foldersContainer = view.findViewById(R.id.migration_folders);
		buttonsContainer = view.findViewById(R.id.control_buttons);

		setupToolbar(view);
		setupButtons();
		setupMigrationSteps();
		setupMigrationFolders();

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(USED_ON_MAP_KEY, usedOnMap);
		outState.putParcelable(FOLDER_URI_KEY, folderUri);
	}

	@Override
	public void onMigrationSkipConfirmed() {
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
		AndroidUiHelper.updateVisibility(stepsContainer, folderFile == null);
	}

	private void setupMigrationFolders() {
		if (folderFile != null) {
			setupFolderTo(foldersContainer);
			setupFolderFrom(foldersContainer);
			setupFoundFilesSize(foldersContainer);
			setupFoundFilesDescr(foldersContainer);
		}
		AndroidUiHelper.updateVisibility(foldersContainer, folderFile != null);
	}

	private void setupFolderTo(@NonNull View view) {
		View container = view.findViewById(R.id.folder_to);
		ImageView icon = container.findViewById(android.R.id.icon);
		TextView title = container.findViewById(android.R.id.title);
		TextView summary = container.findViewById(android.R.id.summary);

		title.setText(R.string.shared_string_copy_to);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_folder_move_to, ColorUtilities.getActiveColorId(nightMode)));

		String storageName = storageHelper.getCurrentStorage().getTitle();
		SpannableString spannable = new SpannableString(storageName);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), 0, storageName.length(), 0);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getActiveColor(app, nightMode)), 0, storageName.length(), 0);
		summary.setText(spannable);
	}

	private void setupFoundFilesSize(@NonNull View view) {
		String amount = String.valueOf(documentFiles.size());
		String formattedSize = "(" + AndroidUtils.formatSize(app, StorageMigrationAsyncTask.getFilesSize(documentFiles)) + ")";
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
		String storageName = storageHelper.getCurrentStorage().getTitle();

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
					StorageMigrationAsyncTask copyFilesTask = new StorageMigrationAsyncTask(activity, documentFiles, usedOnMap);
					copyFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == FOLDER_ACCESS_REQUEST) {
			if (resultCode == Activity.RESULT_OK && data != null) {
				updateSelectedFolderFiles(data.getData());
				setupButtons();
				setupMigrationSteps();
				setupMigrationFolders();
			} else {
				app.showShortToastMessage(R.string.folder_access_denied);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
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

	private void updateSelectedFolderFiles(Uri uri) {
		if (uri != null) {
			folderUri = uri;
			folderFile = DocumentFile.fromTreeUri(app, folderUri);
			if (folderFile != null) {
				collectFiles(folderFile, documentFiles);
			}
		}
	}

	private void collectFiles(DocumentFile documentFile, List<DocumentFile> documentFiles) {
		if (documentFile.isDirectory()) {
			DocumentFile[] files = documentFile.listFiles();
			for (DocumentFile file : files) {
				collectFiles(file, documentFiles);
			}
		} else {
			documentFiles.add(documentFile);
		}
	}

	public static boolean dialogShowRequired(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return VERSION.SDK_INT >= 30 && settings.getDefaultInternalStorage().exists()
				&& !settings.SHARED_STORAGE_MIGRATION_DIALOG_SHOWN.get();
	}

	public static void showInstance(@NonNull FragmentActivity activity, boolean usedOnMap) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			app.getSettings().SHARED_STORAGE_MIGRATION_DIALOG_SHOWN.set(true);

			SharedStorageWarningFragment fragment = new SharedStorageWarningFragment();
			fragment.usedOnMap = usedOnMap;
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}