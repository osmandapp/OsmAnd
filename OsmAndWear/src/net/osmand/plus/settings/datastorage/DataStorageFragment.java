package net.osmand.plus.settings.datastorage;

import static net.osmand.plus.download.DownloadActivity.LOCAL_TAB;
import static net.osmand.plus.download.DownloadActivity.TAB_TO_OPEN;
import static net.osmand.plus.firstusage.FirstUsageWizardFragment.FIRST_USAGE;
import static net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet.CHOSEN_DIRECTORY;
import static net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet.MOVE_DATA;
import static net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet.NEW_PATH;
import static net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet.PATH_CHANGED;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.INTERNAL_STORAGE;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.MANUALLY_SPECIFIED;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.SHARED_STORAGE;
import static net.osmand.plus.settings.datastorage.SharedStorageWarningFragment.STORAGE_MIGRATION;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.BidiFormatter;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.ui.BannerAndDownloadFreeVersion;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet;
import net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.datastorage.task.FilesCollectTask;
import net.osmand.plus.settings.datastorage.task.FilesCollectTask.FilesCollectListener;
import net.osmand.plus.settings.datastorage.task.MoveFilesTask;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataStorageFragment extends BaseSettingsFragment implements FilesCollectListener,
		StorageMigrationRestartListener, MoveFilesStopListener {

	private static final String MEMORY_USAGE = "memory_usage";
	private static final String CHANGE_DIRECTORY_BUTTON = "change_directory";

	private List<CheckBoxPreference> dataStorageRadioButtonsGroup;
	private Preference changeButton;
	private StorageItem newDataStorage;
	private StorageItem currentDataStorage;
	private String tmpManuallySpecifiedPath;
	private DataStorageHelper dataStorageHelper;

	private OsmandActionBarActivity activity;
	private boolean storageMigration;
	private boolean firstUsage;

	private MoveFilesTask moveFileTask;
	private FilesCollectTask collectTask;
	private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		app = getMyApplication();
		activity = getMyActivity();
		Bundle args = getArguments();
		if (args != null) {
			storageMigration = args.getBoolean(STORAGE_MIGRATION, false);
			firstUsage = args.getBoolean(FIRST_USAGE, false);
		}
		if (dataStorageHelper == null) {
			setRetainInstance(true);
			refreshDataInfo();
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public Bundle buildArguments() {
		Bundle args = super.buildArguments();
		args.putBoolean(STORAGE_MIGRATION, storageMigration);
		args.putBoolean(FIRST_USAGE, firstUsage);
		return args;
	}

	@Override
	protected void setupPreferences() {
		PreferenceScreen screen = getPreferenceScreen();
		if (screen == null || dataStorageHelper == null) {
			return;
		}
		dataStorageRadioButtonsGroup = new ArrayList<>();

		for (StorageItem item : dataStorageHelper.getStorageItems()) {
			CheckBoxPreference preference = new CheckBoxPreference(activity);
			preference.setKey(item.getKey());
			preference.setTitle(item.getTitle());
			preference.setLayoutResource(R.layout.data_storage_list_item);
			screen.addPreference(preference);
			dataStorageRadioButtonsGroup.add(preference);
		}
		Preference preference = findPreference(MEMORY_USAGE);
		preference.setVisible(!storageMigration && !firstUsage);

		changeButton = new Preference(app);
		changeButton.setKey(CHANGE_DIRECTORY_BUTTON);
		changeButton.setLayoutResource(R.layout.bottom_sheet_item_btn_with_icon_and_text);
		screen.addPreference(changeButton);

		currentDataStorage = dataStorageHelper.getCurrentStorage();
		updateView(currentDataStorage.getKey());
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
		recyclerView.setItemAnimator(null);
		return recyclerView;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		if (CHANGE_DIRECTORY_BUTTON.equals(key)) {
			showFolderSelectionDialog();
			return false;
		} else if (MEMORY_USAGE.equals(key)) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				Intent intent = new Intent(activity, DownloadActivity.class);
				intent.putExtra(TAB_TO_OPEN, LOCAL_TAB);
				activity.startActivity(intent);
			}
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (newValue instanceof Bundle) {
			//results from BottomSheets
			Bundle resultData = (Bundle) newValue;
			if (resultData.containsKey(ChangeDataStorageBottomSheet.TAG)) {
				boolean moveMaps = resultData.getBoolean(MOVE_DATA);
				newDataStorage = resultData.getParcelable(CHOSEN_DIRECTORY);
				if (newDataStorage != null) {
					if (tmpManuallySpecifiedPath != null) {
						String directory = tmpManuallySpecifiedPath;
						tmpManuallySpecifiedPath = null;
						newDataStorage.setDirectory(directory);
					}
					if (moveMaps) {
						File fromDirectory = new File(currentDataStorage.getDirectory());
						updateSelectedFolderFiles(fromDirectory);
					} else {
						confirm(app, activity, newDataStorage, false);
					}
				}
			} else if (resultData.containsKey(SelectFolderBottomSheet.TAG)) {
				boolean pathChanged = resultData.getBoolean(PATH_CHANGED);
				if (pathChanged) {
					tmpManuallySpecifiedPath = resultData.getString(NEW_PATH);
					if (tmpManuallySpecifiedPath != null) {
						StorageItem manuallySpecified = null;
						try {
							manuallySpecified = (StorageItem) dataStorageHelper.getManuallySpecified().clone();
							manuallySpecified.setDirectory(tmpManuallySpecifiedPath);
						} catch (CloneNotSupportedException e) {
							return false;
						}
						if (storageMigration || firstUsage) {
							confirm(app, activity, manuallySpecified, false);
						} else {
							ChangeDataStorageBottomSheet.showInstance(getFragmentManager(), MANUALLY_SPECIFIED,
									currentDataStorage, manuallySpecified, this, false);
						}
					}
				}
			}
		} else {
			//show necessary dialog
			String key = preference.getKey();
			if (key != null) {
				newDataStorage = dataStorageHelper.getStorage(key);
				if (newDataStorage != null && !currentDataStorage.getKey().equals(newDataStorage.getKey())) {
					if (key.equals(MANUALLY_SPECIFIED)) {
						showFolderSelectionDialog();
					} else if (storageMigration || firstUsage) {
						confirm(app, activity, newDataStorage, false);
					} else {
						ChangeDataStorageBottomSheet.showInstance(getFragmentManager(), key,
								currentDataStorage, newDataStorage, this, false);
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		String key = preference.getKey();
		if (key == null) return;

		View itemView = holder.itemView;
		if (preference instanceof CheckBoxPreference) {
			StorageItem item = dataStorageHelper.getStorage(key);
			if (item != null) {
				TextView tvTitle = itemView.findViewById(android.R.id.title);
				TextView tvSummary = itemView.findViewById(R.id.summary);
				ImageView ivIcon = itemView.findViewById(R.id.icon);
				View secondPart = itemView.findViewById(R.id.secondPart);

				setupStorageItemView(item, itemView);
				tvTitle.setText(item.getTitle());
				setupStorageItemSummary(item, tvSummary);
				ivIcon.setImageDrawable(getStorageItemIcon(item));
				setupSecondPart(item, secondPart);
			}
		} else if (key.equals(CHANGE_DIRECTORY_BUTTON)) {
			ImageView icon = itemView.findViewById(R.id.button_icon);
			TextView title = itemView.findViewById(R.id.button_text);
			int activeColor = ColorUtilities.getActiveColor(app, isNightMode());
			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
			AndroidUtils.setBackground(itemView, drawable);
			icon.setVisibility(View.INVISIBLE);
			title.setText(R.string.shared_string_change);
		} else if (key.equals(MEMORY_USAGE)) {
			BannerAndDownloadFreeVersion.updateDescriptionTextWithSize(app, itemView);
		}
	}

	private void setupStorageItemView(StorageItem item, View itemView) {
		boolean notSharedStorage = !SHARED_STORAGE.equals(item.getKey());
		itemView.setClickable(notSharedStorage);
		AndroidUiHelper.updateVisibility(itemView.findViewById(android.R.id.checkbox), notSharedStorage);
	}

	private void setupStorageItemSummary(StorageItem item, TextView summary) {
		if (!item.isStorageSizeDefinable()) {
			summary.setText(getStorageItemDescriptionOrPath(item));
			summary.setVisibility(View.VISIBLE);
		} else {
			String space = getSpaceDescription(item.getDirectory());
			if (!space.isEmpty()) {
				space = space.replaceAll(" • ", "  •  ");
				summary.setText(space);
				summary.setVisibility(View.VISIBLE);
			} else {
				summary.setVisibility(View.GONE);
			}
		}
	}

	private String getSpaceDescription(String path) {
		File dir = new File(path);
		File dirParent = dir.getParentFile();
		while (!dir.exists() && dirParent != null) {
			dir = dir.getParentFile();
			dirParent = dir.getParentFile();
		}
		if (dir.exists()) {
			DecimalFormat formatter = new DecimalFormat("#.##");
			float freeSpace = AndroidUtils.getFreeSpaceGb(dir);
			float totalSpace = AndroidUtils.getTotalSpaceGb(dir);
			if (freeSpace < 0 || totalSpace < 0) {
				return "";
			}
			return String.format(getString(R.string.data_storage_space_description),
					formatter.format(freeSpace),
					formatter.format(totalSpace));
		}
		return "";
	}

	private Drawable getStorageItemIcon(StorageItem item) {
		boolean current = currentDataStorage.getKey().equals(item.getKey());
		int iconId = current ? item.getSelectedIconResId() : item.getNotSelectedIconResId();
		int defaultIconColor = ColorUtilities.getDefaultIconColorId(isNightMode());
		int chosenIconColor = isNightMode() ? R.color.icon_color_osmand_dark : R.color.icon_color_osmand_light;
		int iconColor = current ? chosenIconColor : defaultIconColor;
		return app.getUIUtilities().getIcon(iconId, iconColor);
	}

	private void setupSecondPart(StorageItem item, View secondPart) {
		boolean manuallySpecified = item.getKey().equals(MANUALLY_SPECIFIED);
		AndroidUiHelper.updateVisibility(secondPart, !manuallySpecified);
		setupAdditionalDescription(item, secondPart.findViewById(R.id.additionalDescription));
		setupDetailsButton(item, secondPart.findViewById(R.id.details_button));
	}

	private void setupAdditionalDescription(StorageItem item, TextView additionalDescription) {
		if (item.isStorageSizeDefinable()) {
			additionalDescription.setText(getStorageItemDescriptionOrPath(item));
			additionalDescription.setVisibility(View.VISIBLE);
		} else {
			additionalDescription.setVisibility(View.GONE);
		}
	}

	private String getStorageItemDescriptionOrPath(StorageItem item) {
		String key = item.getKey();
		if (INTERNAL_STORAGE.equals(key) || SHARED_STORAGE.equals(key)) {
			return item.getDescription();
		} else {
			BidiFormatter pathRtlFormatter = BidiFormatter.getInstance();
			return pathRtlFormatter.unicodeWrap(item.getDirectory());
		}
	}

	private void setupDetailsButton(StorageItem item, DialogButton btnDetails) {
		boolean sharedStorage = item.getKey().equals(SHARED_STORAGE);
		AndroidUiHelper.updateVisibility(btnDetails, sharedStorage && (!storageMigration && !firstUsage));

		if (item.getKey().equals(SHARED_STORAGE)) {
			btnDetails.setClickable(true);
			btnDetails.setOnClickListener(v -> {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					SharedStorageWarningFragment.showInstance(mapActivity.getSupportFragmentManager(), false);
				}
			});
		}
	}

	private void stopCollectFilesTask() {
		if (collectTask != null && collectTask.getStatus() == AsyncTask.Status.RUNNING) {
			collectTask.cancel(false);
		}
	}

	private void updateSelectedFolderFiles(@NonNull File file) {
		stopCollectFilesTask();
		collectTask = new FilesCollectTask(app, file, this);
		collectTask.executeOnExecutor(singleThreadExecutor);
	}

	@Override
	public void onFilesCollectingFinished(@Nullable String error,
	                                      @NonNull File folder,
	                                      @NonNull List<File> files,
	                                      @NonNull Pair<Long, Long> size) {
		collectTask = null;
		if (Algorithms.isEmpty(error)) {
			moveData(files, size);
		} else {
			showErrorDialog(error);
		}
	}

	private void updateView(String key) {
		//selection set up
		for (CheckBoxPreference preference : dataStorageRadioButtonsGroup) {
			String preferenceKey = preference.getKey();
			boolean checked = preferenceKey != null && preferenceKey.equals(key);
			preference.setChecked(checked);
		}
		boolean visible = key.equals(MANUALLY_SPECIFIED);
		changeButton.setVisible(visible);
	}

	private void showFolderSelectionDialog() {
		StorageItem manuallySpecified = dataStorageHelper.getManuallySpecified();
		if (manuallySpecified != null) {
			SelectFolderBottomSheet.showInstance(getFragmentManager(), manuallySpecified.getKey(),
					manuallySpecified.getDirectory(), this,
					getString(R.string.storage_directory_manual), getString(R.string.paste_Osmand_data_folder_path),
					getString(R.string.shared_string_select_folder), false);
		}
	}

	private void moveData(@NonNull List<File> files,
	                      @NonNull Pair<Long, Long> filesSize) {
		moveFileTask = new MoveFilesTask(activity, currentDataStorage, newDataStorage, files, filesSize, this, this);
		moveFileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void showErrorDialog(@NonNull String error) {
		StringBuilder sb = new StringBuilder();
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		sb.append(error);
		AlertDialog.Builder bld = new AlertDialog.Builder(ctx);
		bld.setMessage(sb.toString());
		bld.setPositiveButton(R.string.shared_string_restart, (dialog, which) -> {
			confirm(app, activity, newDataStorage, true);
		});
		bld.show();
	}

	private void confirm(OsmandApplication app, OsmandActionBarActivity activity, StorageItem newStorageDirectory, boolean silentRestart) {
		confirm(app, activity, newStorageDirectory);
		if (!firstUsage) {
			RestartActivity.doRestart(activity, silentRestart);
		}
	}

	private void confirm(OsmandApplication app, OsmandActionBarActivity activity, StorageItem newStorageDirectory) {
		String newDirectory = newStorageDirectory.getDirectory();
		int type = newStorageDirectory.getType();
		File newDirectoryFile = new File(newDirectory);
		boolean wr = FileUtils.isWritable(newDirectoryFile);
		if (wr) {
			if (storageMigration) {
				dismiss();
			} else {
				app.setExternalStorageDirectory(type, newDirectory);
				DataStorageHelper.reloadData(app, activity);
			}
			Fragment target = getTargetFragment();
			if (target instanceof StorageSelectionListener) {
				((StorageSelectionListener) target).onStorageSelected(newStorageDirectory);
			}
		} else {
			Toast.makeText(activity, R.string.specified_directiory_not_writeable,
					Toast.LENGTH_LONG).show();
		}
		refreshDataInfo();
		updateAllSettings();
	}

	private void refreshDataInfo() {
		dataStorageHelper = new DataStorageHelper(app);
	}

	@Override
	public void onRestartSelected() {
		confirm(app, activity, newDataStorage, true);
	}

	@Override
	public void onStopTask() {
		if (moveFileTask != null && moveFileTask.getStatus() == AsyncTask.Status.RUNNING) {
			moveFileTask.cancel(false);
		}
		confirm(app, activity, newDataStorage);
	}

	public interface StorageSelectionListener {
		void onStorageSelected(@NonNull StorageItem storageItem);
	}
}
