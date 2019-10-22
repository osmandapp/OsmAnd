package net.osmand.plus.settings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet;
import net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;

import static net.osmand.plus.settings.DataStorageHelper.INTERNAL_STORAGE;
import static net.osmand.plus.settings.DataStorageHelper.MANUALLY_SPECIFIED;
import static net.osmand.plus.settings.DataStorageHelper.OTHER_MEMORY;
import static net.osmand.plus.settings.DataStorageHelper.TILES_MEMORY;
import static net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet.CHOSEN_DIRECTORY;
import static net.osmand.plus.settings.bottomsheets.ChangeDataStorageBottomSheet.MOVE_DATA;
import static net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet.PATH_CHANGED;
import static net.osmand.plus.settings.bottomsheets.SelectFolderBottomSheet.NEW_PATH;

public class DataStorageFragment extends BaseSettingsFragment implements DataStorageHelper.UpdateMemoryInfoUIAdapter {
	public final static int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 500;
	
	private final static String CHANGE_DIRECTORY_BUTTON = "change_directory";
	private final static String OSMAND_USAGE = "osmand_usage";
	private final static String CALCULATE_TILES_BTN_PRESSED = "calculate_tiles_btn_pressed";
	private final static String ITEMS_HOLDER = "items_holder";

	private ArrayList<DataStorageMenuItem> menuItems;
	private ArrayList<DataStorageMemoryItem> memoryItems;
	private ArrayList<CheckBoxPreference> dataStorageRadioButtonsGroup;
	private Preference changeButton;
	private DataStorageMenuItem currentDataStorage;
	private String tmpManuallySpecifiedPath;
	private DataStorageHelper itemsHolder;
	private boolean calculateTilesBtnPressed;
	
	private DataStorageHelper.RefreshMemoryUsedInfo calculateMemoryTask;
	private DataStorageHelper.RefreshMemoryUsedInfo calculateTilesMemoryTask;

	private OsmandApplication app;
	private OsmandActionBarActivity activity;
	private OsmandSettings settings;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		activity = getMyActivity();
		settings = app.getSettings();
		if (savedInstanceState != null) {
			calculateTilesBtnPressed = savedInstanceState.getBoolean(CALCULATE_TILES_BTN_PRESSED);
			itemsHolder = savedInstanceState.getParcelable(ITEMS_HOLDER);
		}
		if (itemsHolder == null) {
			refreshDataInfo();
		}
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void setupPreferences() {
		
		PreferenceScreen screen = getPreferenceScreen();

		if (screen == null || itemsHolder == null) {
			return;
		}

		menuItems = itemsHolder.getStorageItems();
		memoryItems = itemsHolder.getMemoryInfoItems();
		dataStorageRadioButtonsGroup = new ArrayList<>();

		for (DataStorageMenuItem item : menuItems) {
			CheckBoxPreference preference = new CheckBoxPreference(activity);
			preference.setKey(item.getKey());
			preference.setTitle(item.getTitle());
			preference.setLayoutResource(R.layout.data_storage_list_item);
			screen.addPreference(preference);
			dataStorageRadioButtonsGroup.add(preference);
		}

		currentDataStorage = itemsHolder.getCurrentStorage();

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
				DataStorageMenuItem newDataStorage = resultData.getParcelable(CHOSEN_DIRECTORY);
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
						DataStorageMenuItem manuallySpecified = null;
						try {
							manuallySpecified = (DataStorageMenuItem) itemsHolder.getManuallySpecified().clone();
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
				DataStorageMenuItem newDataStorage = itemsHolder.getStorage(key);
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
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(CALCULATE_TILES_BTN_PRESSED, calculateTilesBtnPressed);
		outState.putParcelable(ITEMS_HOLDER, itemsHolder);
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

		String[] memoryUnitsFormats = new String[] {
				getString(R.string.shared_string_memory_kb_desc),
				getString(R.string.shared_string_memory_mb_desc),
				getString(R.string.shared_string_memory_gb_desc),
				getString(R.string.shared_string_memory_tb_desc)
		};
		
		final View itemView = holder.itemView;
		if (preference instanceof CheckBoxPreference) {
			DataStorageMenuItem item = itemsHolder.getStorage(key);
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
					tvSummary.setText(item.getDirectory());
					secondPart.setVisibility(View.GONE);
					tvAdditionalDescription.setVisibility(View.GONE);
					divider.setVisibility(View.GONE);
				} else {
					tvAdditionalDescription.setVisibility(View.VISIBLE);
					divider.setVisibility(View.VISIBLE);
					secondPart.setVisibility(View.VISIBLE);
					String space = getSpaceDescription(item.getDirectory());
					if (!space.equals("")) {
						space = space.replaceAll(" • ", "  •  ");
						tvSummary.setText(space);
						tvSummary.setVisibility(View.VISIBLE);
					} else {
						tvSummary.setVisibility(View.GONE);
					}
					if (currentKey.equals(INTERNAL_STORAGE)) {
						tvAdditionalDescription.setText(item.getDescription());
					} else {
						tvAdditionalDescription.setText(item.getDirectory());
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
		} else if(key.equals(OSMAND_USAGE)) {
			long totalUsageBytes = itemsHolder.getTotalUsedBytes();
			TextView tvSummary = itemView.findViewById(R.id.summary);
			tvSummary.setText(DataStorageHelper.getFormattedMemoryInfo(totalUsageBytes, memoryUnitsFormats));
		} else {
			for (DataStorageMemoryItem mi : memoryItems) {
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
								calculateTilesMemoryTask = itemsHolder.calculateTilesMemoryUsed(DataStorageFragment.this);
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
		DataStorageMenuItem manuallySpecified = itemsHolder.getManuallySpecified();
		if (manuallySpecified != null) {
			SelectFolderBottomSheet.showInstance(getFragmentManager(), manuallySpecified.getKey(),
					manuallySpecified.getDirectory(), DataStorageFragment.this,
					getString(R.string.storage_directory_manual), getString(R.string.paste_Osmand_data_folder_path),
					getString(R.string.shared_string_select_folder), false);
		}
	}

	private void moveData(final DataStorageMenuItem currentStorage, final DataStorageMenuItem newStorage) {
		File fromDirectory = new File(currentStorage.getDirectory());
		File toDirectory = new File(newStorage.getDirectory());
		@SuppressLint("StaticFieldLeak")
		MoveFilesToDifferentDirectory task = new MoveFilesToDifferentDirectory(activity, fromDirectory, toDirectory) {

			private MessageFormat formatMb = new MessageFormat("{0, number,##.#} MB", Locale.US);

			@NonNull
			private String getFormattedSize(long sizeBytes) {
				int size = (int) ((sizeBytes + 512) >> 10);
				if (size >= 0) {
					if (size > 100) {
						return formatMb.format(new Object[]{(float) size / (1 << 10)});
					} else {
						return size + " kB";
					}
				}
				return "";
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

	private void confirm(OsmandApplication app, OsmandActionBarActivity activity, DataStorageMenuItem newStorageDirectory, boolean silentRestart) {
		String newDirectory = newStorageDirectory.getDirectory();
		int type = newStorageDirectory.getType();
		File newDirectoryFile = new File(newDirectory);
		boolean wr = OsmandSettings.isWritable(newDirectoryFile);
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
		itemsHolder = DataStorageHelper.refreshInfo(app);
		calculateMemoryTask = itemsHolder.calculateMemoryUsedInfo(this);
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
		new ReloadData(activity, getMyApplication()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	@Override
	public void onMemoryInfoUpdate() {
		updateAllSettings();
		app.getSettings().OSMAND_USAGE_SPACE.set(itemsHolder.getTotalUsedBytes());
	}
	
	public static class MoveFilesToDifferentDirectory extends AsyncTask<Void, Void, Boolean> {

		protected WeakReference<OsmandActionBarActivity> activity;
		private WeakReference<Context> context;
		private File from;
		private File to;
		protected ProgressImplementation progress;
		private Runnable runOnSuccess;
		private int movedCount;
		private long movedSize;
		private int copiedCount;
		private long copiedSize;
		private int failedCount;
		private long failedSize;
		private String exceptionMessage;

		public MoveFilesToDifferentDirectory(OsmandActionBarActivity activity, File from, File to) {
			this.activity = new WeakReference<>(activity);
			this.context = new WeakReference<>((Context) activity);
			this.from = from;
			this.to = to;
		}

		public void setRunOnSuccess(Runnable runOnSuccess) {
			this.runOnSuccess = runOnSuccess;
		}

		public int getMovedCount() {
			return movedCount;
		}

		public int getCopiedCount() {
			return copiedCount;
		}

		public int getFailedCount() {
			return failedCount;
		}

		public long getMovedSize() {
			return movedSize;
		}

		public long getCopiedSize() {
			return copiedSize;
		}

		public long getFailedSize() {
			return failedSize;
		}

		@Override
		protected void onPreExecute() {
			Context ctx = context.get();
			if (context == null) {
				return;
			}
			movedCount = 0;
			copiedCount = 0;
			failedCount = 0;
			progress = ProgressImplementation.createProgressDialog(
					ctx, ctx.getString(R.string.copying_osmand_files),
					ctx.getString(R.string.copying_osmand_files_descr, to.getPath()),
					ProgressDialog.STYLE_HORIZONTAL);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Context ctx = context.get();
			if (ctx == null) {
				return;
			}
			if (result != null) {
				if (result.booleanValue() && runOnSuccess != null) {
					runOnSuccess.run();
				} else if (!result.booleanValue()) {
					Toast.makeText(ctx, ctx.getString(R.string.shared_string_io_error) + ": " + exceptionMessage, Toast.LENGTH_LONG).show();
				}
			}
			try {
				if (progress.getDialog().isShowing()) {
					progress.getDialog().dismiss();
				}
			} catch (Exception e) {
				//ignored
			}
		}

		private void movingFiles(File f, File t, int depth) throws IOException {
			Context ctx = context.get();
			if (ctx == null) {
				return;
			}
			if (depth <= 2) {
				progress.startTask(ctx.getString(R.string.copying_osmand_one_file_descr, t.getName()), -1);
			}
			if (f.isDirectory()) {
				t.mkdirs();
				File[] lf = f.listFiles();
				if (lf != null) {
					for (int i = 0; i < lf.length; i++) {
						if (lf[i] != null) {
							movingFiles(lf[i], new File(t, lf[i].getName()), depth + 1);
						}
					}
				}
				f.delete();
			} else if (f.isFile()) {
				if (t.exists()) {
					Algorithms.removeAllFiles(t);
				}
				boolean rnm = false;
				long fileSize = f.length();
				try {
					rnm = f.renameTo(t);
					movedCount++;
					movedSize += fileSize;
				} catch (RuntimeException e) {
				}
				if (!rnm) {
					FileInputStream fin = new FileInputStream(f);
					FileOutputStream fout = new FileOutputStream(t);
					try {
						progress.startTask(ctx.getString(R.string.copying_osmand_one_file_descr, t.getName()), (int) (f.length() / 1024));
						Algorithms.streamCopy(fin, fout, progress, 1024);
						copiedCount++;
						copiedSize += fileSize;
					} catch (IOException e) {
						failedCount++;
						failedSize += fileSize;
					} finally {
						fin.close();
						fout.close();
					}
					f.delete();
				}
			}
			if (depth <= 2) {
				progress.finishTask();
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			to.mkdirs();
			try {
				movingFiles(from, to, 0);
			} catch (IOException e) {
				exceptionMessage = e.getMessage();
				return false;
			}
			return true;
		}

	}

	public static class ReloadData extends AsyncTask<Void, Void, Boolean> {
		private WeakReference<Context> ctx;
		protected ProgressImplementation progress;
		private OsmandApplication app;

		public ReloadData(Context ctx, OsmandApplication app) {
			this.ctx = new WeakReference<>(ctx);
			this.app = app;
		}

		@Override
		protected void onPreExecute() {
			Context c = ctx.get();
			if (c == null) {
				return;
			}
			progress = ProgressImplementation.createProgressDialog(c, c.getString(R.string.loading_data),
					c.getString(R.string.loading_data), ProgressDialog.STYLE_HORIZONTAL);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			try {
				if (progress.getDialog().isShowing()) {
					progress.getDialog().dismiss();
				}
			} catch (Exception e) {
				//ignored
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			app.getResourceManager().reloadIndexes(progress, new ArrayList<String>());
			return true;
		}
	}
}