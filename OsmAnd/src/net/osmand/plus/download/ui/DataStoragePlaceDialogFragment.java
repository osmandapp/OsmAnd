package net.osmand.plus.download.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.FileUtils;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.OnDismissDialogFragmentListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.dashboard.DashChooseAppDirFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

import java.io.File;

public class DataStoragePlaceDialogFragment extends BottomSheetDialogFragment {
	private static final Log LOG = PlatformUtil.getLog(DataStoragePlaceDialogFragment.class);

	public static final String TAG = "DataStoragePlaceDialogFragment";
	private static final String STORAGE_READOLNY_KEY = "storage_readolny_key";

	private File deviceStorage;
	private int deviceStorageType;
	private String deviceStorageName;
	private File sharedStorage;
	private int sharedStorageType = OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT;
	private File cardStorage;
	private int cardStorageType = OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE;
	public static boolean isInterestedInFirstTime = true;
	boolean storageReadOnly;
	boolean hasExternalStoragePermission;

	@Override
	public void onStart() {
		super.onStart();

		final Window window = getDialog().getWindow();
		WindowManager.LayoutParams params = window.getAttributes();
		params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params.gravity = Gravity.BOTTOM;
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		window.setAttributes(params);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		Activity activity = getActivity();

		hasExternalStoragePermission = DownloadActivity.hasPermissionToWriteExternalStorage(activity);

		File internalStorage = getInternalStorageDirectory(activity);
		File external1Storage = getExternal1StorageDirectory(activity);
		if (external1Storage != null && external1Storage.exists() && FileUtils.isWritable(external1Storage)) {
			deviceStorage = external1Storage;
			deviceStorageType = OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE;
			deviceStorageName = getString(R.string.storage_directory_external);
		} else {
			deviceStorage = internalStorage;
			deviceStorageType = OsmandSettings.EXTERNAL_STORAGE_TYPE_INTERNAL_FILE;
			deviceStorageName = getString(R.string.storage_directory_default);
		}
		if (hasExternalStoragePermission) {
			sharedStorage = getSharedStorageDirectory(activity);
			cardStorage = getMyApplication().getSettings().getSecondaryStorage();
		}

		Bundle args = null;
		if (savedInstanceState != null) {
			args = savedInstanceState;
		} else if (getArguments() != null) {
			args = getArguments();
		}

		if (args != null) {
			storageReadOnly = args.getBoolean(STORAGE_READOLNY_KEY);
		}

		final View view = inflater.inflate(R.layout.fragment_data_storage_place_dialog, container,
				false);
		((ImageView) view.findViewById(R.id.folderIconImageView))
				.setImageDrawable(getIcon(R.drawable.ic_action_folder, R.color.osmand_orange));

		if (storageReadOnly) {
			((TextView) view.findViewById(R.id.description))
					.setText(getString(R.string.storage_directory_readonly_desc));
		}

		View deviceStorageRow = view.findViewById(R.id.deviceMemoryRow);
		deviceStorageRow.setOnClickListener(deviceMemoryOnClickListener);
		ImageView deviceStorageImageView = (ImageView) view.findViewById(R.id.deviceMemoryImageView);
		deviceStorageImageView.setImageDrawable(getContentIcon(R.drawable.ic_action_phone));
		TextView deviceStorageDescription = (TextView) view.findViewById(R.id.deviceMemoryDescription);
		deviceStorageDescription.setText(deviceStorageName);
		deviceStorageDescription.setText(AndroidUtils.getFreeSpace(activity, deviceStorage));

		View sharedMemoryRow = view.findViewById(R.id.sharedMemoryRow);
		if (hasExternalStoragePermission && sharedStorage != null) {
			sharedMemoryRow.setOnClickListener(sharedMemoryOnClickListener);
			ImageView sharedMemoryImageView = (ImageView) view.findViewById(R.id.sharedMemoryImageView);
			sharedMemoryImageView.setImageDrawable(getContentIcon(R.drawable.ic_action_phone));
			TextView sharedMemoryDescription = (TextView) view.findViewById(R.id.sharedMemoryDescription);
			sharedMemoryDescription.setText(AndroidUtils.getFreeSpace(activity, sharedStorage));
		} else {
			view.findViewById(R.id.divSharedStorage).setVisibility(View.GONE);
			sharedMemoryRow.setVisibility(View.GONE);
		}

		View memoryStickRow = view.findViewById(R.id.memoryStickRow);
		if (hasExternalStoragePermission && cardStorage != null) {
			memoryStickRow.setOnClickListener(memoryStickOnClickListener);
			ImageView memoryStickImageView = (ImageView) view.findViewById(R.id.memoryStickImageView);
			memoryStickImageView.setImageDrawable(getContentIcon(R.drawable.ic_sdcard));
			TextView memoryStickDescription = (TextView) view.findViewById(R.id.memoryStickDescription);
			memoryStickDescription.setText(AndroidUtils.getFreeSpace(activity, cardStorage));
		} else {
			view.findViewById(R.id.divExtStorage).setVisibility(View.GONE);
			memoryStickRow.setVisibility(View.GONE);
		}

		final ImageButton closeImageButton = (ImageButton) view.findViewById(R.id.closeImageButton);
		closeImageButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		closeImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isInterestedInFirstTime = false;
				dismiss();
			}
		});
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STORAGE_READOLNY_KEY, storageReadOnly);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		Activity activity = getActivity();
		if (activity instanceof OnDismissDialogFragmentListener) {
			((OnDismissDialogFragmentListener) activity).onDismissDialogFragment(this);
		}

	}

	public static File getInternalStorageDirectory(Activity activity) {
		return ((OsmandApplication) activity.getApplication()).getSettings()
				.getInternalAppPath();
	}

	public static File getExternal1StorageDirectory(Activity activity) {
		if (Build.VERSION.SDK_INT < 19) {
			return null;
		} else {
			return ((OsmandApplication) activity.getApplication()).getSettings()
					.getExternal1AppPath();
		}
	}

	public static File getSharedStorageDirectory(Activity activity) {
		return ((OsmandApplication) activity.getApplication()).getSettings()
				.getDefaultInternalStorage();
	}

	private void checkAssets() {
		getMyApplication().getResourceManager().checkAssets(IProgress.EMPTY_PROGRESS, true);
	}

	private void updateDownloadIndexes() {
		DownloadIndexesThread downloadIndexesThread = getMyApplication().getDownloadThread();
		downloadIndexesThread.runReloadIndexFilesSilent();
	}

	private View.OnClickListener deviceMemoryOnClickListener =
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					saveFilesLocation(deviceStorageType, deviceStorage, getActivity());
					checkAssets();
					updateDownloadIndexes();
					isInterestedInFirstTime = false;
					dismiss();
				}
			};

	private View.OnClickListener sharedMemoryOnClickListener =
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					saveFilesLocation(sharedStorageType, sharedStorage, getActivity());
					checkAssets();
					updateDownloadIndexes();
					isInterestedInFirstTime = false;
					dismiss();
				}
			};

	private View.OnClickListener memoryStickOnClickListener =
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean res = saveFilesLocation(cardStorageType, cardStorage, getActivity());
					checkAssets();
					updateDownloadIndexes();
					isInterestedInFirstTime = false;
					if (res) {
						dismiss();
					}
				}
			};

	public boolean saveFilesLocation(int type, File selectedFile, Activity context) {
		boolean wr = FileUtils.isWritable(selectedFile);
		if (wr) {
			((OsmandApplication) context.getApplication())
					.setExternalStorageDirectory(type, selectedFile.getAbsolutePath());
			reloadData();
		} else {
			Toast.makeText(context, R.string.specified_directiory_not_writeable,
					Toast.LENGTH_LONG).show();
		}
		return wr;
	}

	private void reloadData() {
		new DashChooseAppDirFragment.ReloadData(getActivity(), getMyApplication()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public static void showInstance(FragmentManager fragmentManager, boolean storageReadOnly) {
		DataStoragePlaceDialogFragment f = new DataStoragePlaceDialogFragment();
		Bundle args = new Bundle();
		args.putBoolean(STORAGE_READOLNY_KEY, storageReadOnly);
		f.setArguments(args);
		fragmentManager.beginTransaction()
				.add(f, DataStoragePlaceDialogFragment.TAG)
				.commitAllowingStateLoss();
	}
}
