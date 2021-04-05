package net.osmand.plus.settings.datastorage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.BidiFormatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import net.osmand.AndroidUtils;
import net.osmand.FileUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet;
import net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet;
import net.osmand.plus.settings.datastorage.item.MemoryItem;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.datastorage.task.MoveFilesTask;
import net.osmand.plus.settings.datastorage.task.RefreshUsedMemoryTask;
import net.osmand.plus.settings.datastorage.task.ReloadDataTask;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import static net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet.CHOSEN_DIRECTORY;
import static net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet.MOVE_DATA;
import static net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet.NEW_PATH;
import static net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet.PATH_CHANGED;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.INTERNAL_STORAGE;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.MANUALLY_SPECIFIED;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.OTHER_MEMORY;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.SHARED_STORAGE;
import static net.osmand.plus.settings.datastorage.DataStorageHelper.TILES_MEMORY;

public class DataStorageFragment extends BaseSettingsFragment implements DataStorageHelper.UpdateMemoryInfoUIAdapter {
	public final static int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 500;
	public final static int UI_REFRESH_TIME_MS = 500;

	private final static String CHANGE_DIRECTORY_BUTTON = "change_directory";
	private final static String OSMAND_USAGE = "osmand_usage";

	private ArrayList<StorageItem> storageItems;
	private ArrayList<MemoryItem> memoryItems;
	private ArrayList<CheckBoxPreference> dataStorageRadioButtonsGroup;
	private Preference changeButton;
	private StorageItem currentDataStorage;
	private String tmpManuallySpecifiedPath;
	private DataStorageHelper dataStorageHelper;
	private boolean calculateTilesBtnPressed;

	private RefreshUsedMemoryTask calculateMemoryTask;
	private RefreshUsedMemoryTask calculateTilesMemoryTask;

	private OsmandApplication app;
	private OsmandActionBarActivity activity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		activity = getMyActivity();
		if (dataStorageHelper == null) {
			setRetainInstance(true);
			refreshDataInfo();
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void setupPreferences() {

		PreferenceScreen screen = getPreferenceScreen();

		if (screen == null || dataStorageHelper == null) {
			return;
		}

		storageItems = dataStorageHelper.getStorageItems();
		memoryItems = dataStorageHelper.getMemoryInfoItems();
		dataStorageRadioButtonsGroup = new ArrayList<>();

		for (StorageItem item : storageItems) {
			CheckBoxPreference preference = new CheckBoxPreference(activity);
			preference.setKey(item.getKey());
			preference.setTitle(item.getTitle());
			preference.setLayoutResource(R.layout.data_storage_list_item);
			screen.addPreference(preference);
			dataStorageRadioButtonsGroup.add(preference);
		}

		currentDataStorage = dataStorageHelper.getCurrentStorage();

		changeButton = new Preference(app);
		changeButton.setKey(CHANGE_DIRECTORY_BUTTON);
		changeButton.setLayoutResource(R.layout.bottom_sheet_item_btn_with_icon_and_text);
		screen.addPreference(changeButton);

		updateView(currentDataStorage.getKey());
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
		super.onPreferenceChange(preference, newValue);

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
						ChangeDataStorageBottomSheet.showInstance(getFragmentManager(), MANUALLY_SPECIFIED,
								currentDataStorage, manuallySpecified, this, false);
					}
				}
			}
		} else {
			//show necessary dialog
			String key = preference.getKey();
			if (key != null) {
				StorageItem newDataStorage = dataStorageHelper.getStorage(key);
				if (newDataStorage != null) {
					if (!currentDataStorage.getKey().equals(newDataStorage.getKey())) {
						if (newDataStorage.getType() == OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT
								&& !DownloadActivity.hasPermissionToWriteExternalStorage(activity)) {
							ActivityCompat.requestPermissions(activity,
									new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									DataStorageFragment.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
						} else if (key.equals(MANUALLY_SPECIFIED)) {
							showFolderSelectionDialog();
						} else {
							ChangeDataStorageBottomSheet.showInstance(getFragmentManager(), key,
									currentDataStorage, newDataStorage, DataStorageFragment.this, false);
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		String key = preference.getKey();
		if (key == null) {
			return;
		}
		int activeColorResId = isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		int activeColor = ContextCompat.getColor(app, activeColorResId);
		int primaryTextColorResId = isNightMode() ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
		int primaryTextColor = ContextCompat.getColor(app, primaryTextColorResId);

		String[] memoryUnitsFormats = new String[]{
				getString(R.string.shared_string_memory_kb_desc),
				getString(R.string.shared_string_memory_mb_desc),
				getString(R.string.shared_string_memory_gb_desc),
				getString(R.string.shared_string_memory_tb_desc)
		};

		final View itemView = holder.itemView;
		if (preference instanceof CheckBoxPreference) {
			StorageItem item = dataStorageHelper.getStorage(key);
			if (item != null) {
				TextView tvTitle = itemView.findViewById(android.R.id.title);
				TextView tvSummary = itemView.findViewById(R.id.summary);
				TextView tvAdditionalDescription = itemView.findViewById(R.id.additionalDescription);
				ImageView ivIcon = itemView.findViewById(R.id.icon);
				View divider = itemView.findViewById(R.id.divider);
				View secondPart = itemView.findViewById(R.id.secondPart);

				tvTitle.setText(item.getTitle());
				String currentKey = item.getKey();
				boolean isCurrent = currentDataStorage.getKey().equals(currentKey);

				int defaultIconColor = isNightMode() ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
				int chosenIconColor = isNightMode() ? R.color.icon_color_osmand_dark : R.color.icon_color_osmand_light;
				Drawable icon = app.getUIUtilities().getIcon(item.getIconResId(),
						isCurrent ? chosenIconColor : defaultIconColor);
				ivIcon.setImageDrawable(icon);

				if (currentKey.equals(MANUALLY_SPECIFIED)) {
					setFormattedPath(item, tvSummary);
					secondPart.setVisibility(View.GONE);
					tvAdditionalDescription.setVisibility(View.GONE);
					divider.setVisibility(View.GONE);
				} else {
					tvAdditionalDescription.setVisibility(View.VISIBLE);
					divider.setVisibility(View.VISIBLE);
					secondPart.setVisibility(View.VISIBLE);
					String space = getSpaceDescription(item.getDirectory());
					if (!space.isEmpty()) {
						space = space.replaceAll(" • ", "  •  ");
						tvSummary.setText(space);
						tvSummary.setVisibility(View.VISIBLE);
					} else {
						tvSummary.setVisibility(View.GONE);
					}
					if (currentKey.equals(INTERNAL_STORAGE)) {
						tvAdditionalDescription.setText(item.getDescription());
					} else {
						setFormattedPath(item, tvAdditionalDescription);
					}
				}
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
						tvMemory.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								calculateTilesBtnPressed = true;
								calculateTilesMemoryTask = dataStorageHelper.calculateTilesMemoryUsed(DataStorageFragment.this);
								updateAllSettings();
							}
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

	private void setFormattedPath(StorageItem item, TextView tvAdditionalDescription) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			BidiFormatter pathRtlFormatter = BidiFormatter.getInstance();
			tvAdditionalDescription.setText(pathRtlFormatter.unicodeWrap(item.getDirectory()));
		} else {
			tvAdditionalDescription.setText(String.format("\u200E%s", item.getDirectory()));
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
					manuallySpecified.getDirectory(), DataStorageFragment.this,
					getString(R.string.storage_directory_manual), getString(R.string.paste_Osmand_data_folder_path),
					getString(R.string.shared_string_select_folder), false);
		}
	}

	private void moveData(final StorageItem currentStorage, final StorageItem newStorage) {
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
			app.setExternalStorageDirectory(type, newDirectory);
			reloadData();
			if (silentRestart) {
				MapActivity.doRestart(activity);
			} else {
				app.restartApp(activity);
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
		calculateMemoryTask = dataStorageHelper.calculateMemoryUsedInfo(this);
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

	protected void reloadData() {
		new ReloadDataTask(activity, app).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	@Override
	public void onMemoryInfoUpdate() {
		updateAllSettings();
	}

	@Override
	public void onFinishUpdating(String tag) {
		updateAllSettings();
		if (tag != null && tag.equals(TILES_MEMORY)) {
			app.getSettings().OSMAND_USAGE_SPACE.set(dataStorageHelper.getTotalUsedBytes());
		}
	}
}