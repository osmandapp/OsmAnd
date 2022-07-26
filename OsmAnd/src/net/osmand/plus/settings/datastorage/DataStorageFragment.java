package net.osmand.plus.settings.datastorage;

import static net.osmand.plus.firstusage.FirstUsageWizardFragment.FIRST_USAGE;
import static net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet.CHOSEN_DIRECTORY;
import static net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet.MOVE_DATA;
import static net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet.NEW_PATH;
import static net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet.PATH_CHANGED;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.INTERNAL_STORAGE;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.MANUALLY_SPECIFIED;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.OTHER_MEMORY;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.SHARED_STORAGE;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.TILES_MEMORY;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.UpdateMemoryInfoUIAdapter;
import static net.osmand.plus.settings.datastorage.SharedStorageWarningFragment.STORAGE_MIGRATION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.BidiFormatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
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
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet;
import net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet;
import net.osmand.plus.settings.datastorage.item.MemoryItem;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.datastorage.task.MoveFilesTask;
import net.osmand.plus.settings.datastorage.task.RefreshUsedMemoryTask;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class DataStorageFragment extends BaseSettingsFragment implements UpdateMemoryInfoUIAdapter {

	public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 500;
	public static final int UI_REFRESH_TIME_MS = 500;

	private static final String CHANGE_DIRECTORY_BUTTON = "change_directory";
	private static final String OSMAND_USAGE = "osmand_usage";

	private OsmandApplication app;
	private ArrayList<MemoryItem> memoryItems;
	private ArrayList<CheckBoxPreference> dataStorageRadioButtonsGroup;
	private Preference changeButton;
	private StorageItem newDataStorage;
	private StorageItem currentDataStorage;
	private String tmpManuallySpecifiedPath;
	private DataStorageHelper dataStorageHelper;
	private boolean calculateTilesBtnPressed;

	private RefreshUsedMemoryTask calculateMemoryTask;
	private RefreshUsedMemoryTask calculateTilesMemoryTask;

	private OsmandActionBarActivity activity;
	private boolean storageMigration;
	private boolean firstUsage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
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
		memoryItems = dataStorageHelper.getMemoryInfoItems();
		dataStorageRadioButtonsGroup = new ArrayList<>();

		for (StorageItem item : dataStorageHelper.getStorageItems()) {
			CheckBoxPreference preference = new CheckBoxPreference(activity);
			preference.setKey(item.getKey());
			preference.setTitle(item.getTitle());
			preference.setLayoutResource(R.layout.data_storage_list_item);
			screen.addPreference(preference);
			dataStorageRadioButtonsGroup.add(preference);
		}
		Preference osmandUsage = findPreference(OSMAND_USAGE);
		osmandUsage.setVisible(!storageMigration && !firstUsage);

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
		if (CHANGE_DIRECTORY_BUTTON.equals(preference.getKey())) {
			showFolderSelectionDialog();
			return false;
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
				StorageItem newDataStorage = resultData.getParcelable(CHOSEN_DIRECTORY);
				if (newDataStorage != null) {
					if (tmpManuallySpecifiedPath != null) {
						String directory = tmpManuallySpecifiedPath;
						tmpManuallySpecifiedPath = null;
						newDataStorage.setDirectory(directory);
					}
					if (moveMaps) {
						moveData(currentDataStorage, newDataStorage);
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
					if (!key.equals(INTERNAL_STORAGE) && !DownloadActivity.hasPermissionToWriteExternalStorage(activity)) {
						requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
					} else if (key.equals(MANUALLY_SPECIFIED)) {
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
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE && grantResults.length > 0
				&& permissions.length > 0 && Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[0])) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (newDataStorage != null) {
					confirm(app, activity, newDataStorage, false);
				}
			} else {
				app.showToastMessage(R.string.missing_write_external_storage_permission);
			}
		}
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		String key = preference.getKey();
		if (key == null) return;

		int activeColor = ColorUtilities.getActiveColor(app, isNightMode());
		int primaryTextColor = ColorUtilities.getPrimaryTextColor(app, isNightMode());

		String[] memoryUnitsFormats = {
				getString(R.string.shared_string_memory_kb_desc),
				getString(R.string.shared_string_memory_mb_desc),
				getString(R.string.shared_string_memory_gb_desc),
				getString(R.string.shared_string_memory_tb_desc)
		};

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
			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
			AndroidUtils.setBackground(itemView, drawable);
			icon.setVisibility(View.INVISIBLE);
			title.setText(R.string.shared_string_change);
		} else if (key.equals(OSMAND_USAGE)) {
			long totalUsageBytes = dataStorageHelper.getTotalUsedBytes();
			TextView tvSummary = itemView.findViewById(R.id.summary);
			tvSummary.setText(DataStorageHelper.getFormattedMemoryInfo(totalUsageBytes, memoryUnitsFormats));
		} else {
			for (MemoryItem mi : memoryItems) {
				if (key.equals(mi.getKey())) {
					TextView tvMemory = itemView.findViewById(R.id.memory);
					String summary = "";
					int color = 0;
					if (mi.getKey().equals(TILES_MEMORY) && !calculateTilesBtnPressed) {
						summary = getString(R.string.shared_string_calculate);
						color = activeColor;
						tvMemory.setOnClickListener(v -> {
							calculateTilesBtnPressed = true;
							calculateTilesMemoryTask = dataStorageHelper.calculateTilesMemoryUsed(this);
							updateAllSettings();
						});
					} else {
						tvMemory.setOnClickListener(null);
						color = primaryTextColor;
						summary = DataStorageHelper.getFormattedMemoryInfo(mi.getUsedMemoryBytes(), memoryUnitsFormats);
					}
					View divider = itemView.findViewById(R.id.divider);
					if (mi.getKey().equals(OTHER_MEMORY)) {
						divider.setVisibility(View.VISIBLE);
					} else {
						divider.setVisibility(View.GONE);
					}
					tvMemory.setTextColor(color);
					tvMemory.setText(summary);
				}
			}
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

	private void setupDetailsButton(StorageItem item, View detailsButton) {
		boolean sharedStorage = item.getKey().equals(SHARED_STORAGE);
		AndroidUiHelper.updateVisibility(detailsButton, sharedStorage && (!storageMigration && !firstUsage));

		if (item.getKey().equals(SHARED_STORAGE)) {
			detailsButton.setClickable(true);
			UiUtilities.setupDialogButton(isNightMode(), detailsButton, DialogButtonType.SECONDARY, R.string.shared_string_migration);
			detailsButton.setOnClickListener(v -> {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					SharedStorageWarningFragment.showInstance(mapActivity.getSupportFragmentManager(), false);
				}
			});
		}
	}

	@Override
	public void onDestroy() {
		if (!activity.isChangingConfigurations()) {
			if (calculateMemoryTask != null) {
				calculateMemoryTask.cancel(true);
			}
			if (calculateTilesMemoryTask != null) {
				calculateTilesMemoryTask.cancel(true);
			}
		}
		super.onDestroy();
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

	private void moveData(StorageItem currentStorage, StorageItem newStorage) {
		File fromDirectory = new File(currentStorage.getDirectory());
		File toDirectory = new File(newStorage.getDirectory());
		@SuppressLint("StaticFieldLeak")
		MoveFilesTask task = new MoveFilesTask(activity, fromDirectory, toDirectory) {


			@NonNull
			private String getFormattedSize(long sizeBytes) {
				return AndroidUtils.formatSize(activity.get(), sizeBytes);
			}

			private void showResultsDialog() {
				StringBuilder sb = new StringBuilder();
				Context ctx = activity.get();
				if (ctx == null) {
					return;
				}
				int moved = getMovedCount();
				int copied = getCopiedCount();
				int failed = getFailedCount();
				sb.append(ctx.getString(R.string.files_moved, moved, getFormattedSize(getMovedSize()))).append("\n");
				if (copied > 0) {
					sb.append(ctx.getString(R.string.files_copied, copied, getFormattedSize(getCopiedSize()))).append("\n");
				}
				if (failed > 0) {
					sb.append(ctx.getString(R.string.files_failed, failed, getFormattedSize(getFailedSize()))).append("\n");
				}
				if (copied > 0 || failed > 0) {
					int count = copied + failed;
					sb.append(ctx.getString(R.string.files_present, count, getFormattedSize(getCopiedSize() + getFailedSize()), newStorage.getDirectory()));
				}
				AlertDialog.Builder bld = new AlertDialog.Builder(ctx);
				bld.setMessage(sb.toString());
				bld.setPositiveButton(R.string.shared_string_restart, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						confirm(app, activity.get(), newStorage, true);
					}
				});
				bld.show();
			}

			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				OsmandActionBarActivity a = this.activity.get();
				if (a == null) {
					return;
				}
				OsmandApplication app = a.getMyApplication();
				if (result) {
					app.getResourceManager().resetStoreDirectory();
					// immediately proceed with change (to not loose where maps are currently located)
					if (getCopiedCount() > 0 || getFailedCount() > 0) {
						showResultsDialog();
					} else {
						confirm(app, a, newStorage, false);
					}
				} else {
					showResultsDialog();
					Toast.makeText(a, R.string.copying_osmand_file_failed,
							Toast.LENGTH_SHORT).show();
				}

			}
		};
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void confirm(OsmandApplication app, OsmandActionBarActivity activity, StorageItem newStorageDirectory, boolean silentRestart) {
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
				if (!firstUsage) {
					if (silentRestart) {
						RestartActivity.doRestartSilent(activity);
					} else {
						RestartActivity.doRestart(activity);
					}
				}
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
		calculateTilesBtnPressed = false;
		dataStorageHelper = new DataStorageHelper(app);
		if (!storageMigration && !firstUsage) {
			calculateMemoryTask = dataStorageHelper.calculateMemoryUsedInfo(this);
		}
	}

	@Override
	public void onMemoryInfoUpdate() {
		updateAllSettings();
	}

	@Override
	public void onFinishUpdating(String tag) {
		updateAllSettings();
		if (TILES_MEMORY.equals(tag)) {
			app.getSettings().OSMAND_USAGE_SPACE.set(dataStorageHelper.getTotalUsedBytes());
		}
	}

	public interface StorageSelectionListener {

		void onStorageSelected(@NonNull StorageItem storageItem);

	}
}