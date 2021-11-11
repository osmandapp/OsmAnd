package net.osmand.plus.settings.datastorage;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.datastorage.CopyFilesAsyncTask.CopyFilesListener;
import net.osmand.plus.settings.datastorage.SkipStorageMigrationBottomSheet.OnConfirmMigrationSkipListener;

import java.util.ArrayList;
import java.util.List;

public class SharedStorageMigrationFragment extends BaseOsmAndDialogFragment implements OnConfirmMigrationSkipListener, CopyFilesListener {

	private static final String TAG = SharedStorageMigrationFragment.class.getSimpleName();

	private static final String USED_ON_MAP_KEY = "used_on_map";
	private static final String FOLDER_URI_KEY = "folder_uri_key";

	private OsmandApplication app;
	private DataStorageHelper dataStorageHelper;
	private CopyFilesAsyncTask copyFilesTask;

	private Uri folderUri;
	private DocumentFile folderFile;
	private final List<DocumentFile> documentFiles = new ArrayList<>();

	private View mainView;
	private Toolbar toolbar;
	private View actionButton;
	private TextView progressTitle;
	private ProgressBar progressBar;

	private boolean nightMode;
	private boolean usedOnMap;
	private boolean copyFinished;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		dataStorageHelper = new DataStorageHelper(app);

		if (savedInstanceState != null) {
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
			folderUri = savedInstanceState.getParcelable(FOLDER_URI_KEY);
		}
		nightMode = isNightMode(usedOnMap);

		folderFile = DocumentFile.fromTreeUri(app, folderUri);
		if (folderFile != null) {
			collectFiles(folderFile, documentFiles);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		mainView = themedInflater.inflate(R.layout.shared_storage_migration, container, false);

		setupToolbar(mainView);
		setupButtons(mainView);
		setupMigrationFolders(mainView);
		setupProgressContainer(mainView);

		ViewCompat.setNestedScrollingEnabled(mainView.findViewById(R.id.list), true);

		return mainView;
	}

	private void setupProgressContainer(@NonNull View view) {
		progressBar = view.findViewById(R.id.progress_bar);
		progressTitle = view.findViewById(R.id.progress_title);

		String formattedSize = AndroidUtils.formatSize(app, getFilesSize());
		String warning = getString(R.string.storage_copying_files_size, String.valueOf(documentFiles.size()), formattedSize);
		View container = view.findViewById(R.id.copy_files_descr);
		TextView warningInfo = container.findViewById(android.R.id.title);
		TextView summary = container.findViewById(android.R.id.summary);

		warningInfo.setText(warning);
		summary.setText(getString(R.string.from_to_with_params, getString(R.string.shared_storage), dataStorageHelper.getCurrentStorage().getTitle()));
		AndroidUiHelper.updateVisibility(container.findViewById(android.R.id.icon), false);

		progressBar.setMin(0);
		progressBar.setMax((int) (getFilesSize() / 1024));
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.progress_container), copyFilesTask != null || copyFinished);
	}

	private void setupMigrationFolders(@NonNull View view) {
		View container = view.findViewById(R.id.migration_folders);
		setupFolderTo(container);
		setupFolderFrom(container);

		setupFoundFilesDescr(view);
		setupStartCopyDescr(view);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.migration_folders), copyFilesTask == null && !copyFinished);
	}

	@Override
	public void onMigrationSkipConfirmed() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(USED_ON_MAP_KEY, usedOnMap);
		outState.putParcelable(FOLDER_URI_KEY, folderUri);
	}

	private void setupToolbar(@NonNull View view) {
		toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});
	}

	private void setupFolderFrom(@NonNull View view) {
		View container = view.findViewById(R.id.folder_from);
		ImageView icon = container.findViewById(android.R.id.icon);
		TextView title = container.findViewById(android.R.id.title);
		TextView summary = container.findViewById(android.R.id.summary);

		title.setText(R.string.shared_string_from);
		summary.setText(folderFile.getName());
		icon.setImageDrawable(getIcon(R.drawable.ic_action_folder_move, ColorUtilities.getActiveColorId(nightMode)));
	}

	private void setupFolderTo(@NonNull View view) {
		View container = view.findViewById(R.id.folder_to);
		ImageView icon = container.findViewById(android.R.id.icon);
		TextView title = container.findViewById(android.R.id.title);
		TextView summary = container.findViewById(android.R.id.summary);

		title.setText(R.string.shared_string_copy_to);
		summary.setText(dataStorageHelper.getCurrentStorage().getTitle());
		icon.setImageDrawable(getIcon(R.drawable.ic_action_folder_move_to, ColorUtilities.getActiveColorId(nightMode)));
	}

	private void setupFoundFilesDescr(@NonNull View view) {
		String formattedSize = AndroidUtils.formatSize(app, getFilesSize());
		String warning = getString(R.string.storage_found_files_size, String.valueOf(documentFiles.size()), formattedSize);
		TextView warningInfo = view.findViewById(R.id.found_files);
		warningInfo.setText(warning);
	}

	private long getFilesSize() {
		long size = 0;
		for (DocumentFile file : documentFiles) {
			size += file.length();
		}
		return size;
	}

	private void setupStartCopyDescr(@NonNull View view) {
		String startCopy = getString(R.string.start_copying);
		String sharedStorage = getString(R.string.shared_storage);
		String currentStorage = dataStorageHelper.getCurrentStorage().getTitle();

		TextView startCopyDescr = view.findViewById(R.id.start_copy_descr);
		startCopyDescr.setText(getString(R.string.storage_found_files_descr, startCopy,
				documentFiles.size(), sharedStorage, currentStorage));
	}

	private void setupButtons(@NonNull View view) {
		View container = view.findViewById(R.id.buttons_container);
		container.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.bg_color));

		actionButton = container.findViewById(R.id.dismiss_button);
		actionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					if (copyFinished) {
						app.restartApp(activity);
					} else {
						copyFilesTask = new CopyFilesAsyncTask(app, folderFile, SharedStorageMigrationFragment.this, getFilesSize() / 1024);
						copyFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					}
				}
			}
		});
		UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.PRIMARY, R.string.start_copying);
	}

	@Override
	public void onFilesCopyStarted() {
		toolbar.setTitle(R.string.copying_osmand_files);
		AndroidUiHelper.updateVisibility(actionButton, false);
		AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.migration_folders), false);
		AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.progress_container), true);
	}

	@Override
	public void onFileCopyStarted(String fileName) {
		View container = mainView.findViewById(R.id.remaining_files_container);
		TextView title = container.findViewById(android.R.id.title);
		TextView summary = container.findViewById(android.R.id.summary);
		AndroidUiHelper.updateVisibility(container.findViewById(android.R.id.icon), false);

		summary.setText(getString(R.string.copying_file, fileName));
	}

	@Override
	public void onFilesCopyProgress(int progress) {
		progressBar.setProgress(progress);

		int maxProgress = progressBar.getMax();
		int percentage = maxProgress != 0 ? BasicProgressAsyncTask.normalizeProgress(progress * 100 / maxProgress) : 0;
		progressTitle.setText(getString(R.string.ltr_or_rtl_combine_via_space, percentage + "%", getString(R.string.shared_string_complete)));
	}

	@Override
	public void onFilesCopyFinished() {
		copyFinished = true;
		toolbar.setTitle(R.string.copying_completed);
		toolbar.setNavigationIcon(null);

		AndroidUiHelper.updateVisibility(actionButton, true);
		UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.PRIMARY, R.string.shared_string_restart);
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

	public static void showInstance(@NonNull FragmentManager fragmentManager, Uri data, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SharedStorageMigrationFragment fragment = new SharedStorageMigrationFragment();
			fragment.folderUri = data;
			fragment.usedOnMap = usedOnMap;
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}
}
