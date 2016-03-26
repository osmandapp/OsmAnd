package net.osmand.plus.download.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v4.app.FragmentManager;
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

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.dashboard.DashChooseAppDirFragment;
import net.osmand.plus.download.DownloadActivity;

import java.io.File;

public class DataStoragePlaceDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = "DataStoragePlaceDialogFragment";
	private static final String STORAGE_READOLNY_KEY = "storage_readolny_key";

	private File internalStorage;
	private File sharedStorage;
	private File externalStorage;
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		hasExternalStoragePermission = DownloadActivity.hasPermissionToWriteExternalStorage(getActivity());

		internalStorage = getInternalStorageDirectory(getActivity());
		if (hasExternalStoragePermission) {
			sharedStorage = getSharedStorageDirectory(getActivity());
			externalStorage = getMyApplication().getSettings().getSecondaryStorage();
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
				.setImageDrawable(getIcon(R.drawable.ic_action_folder, R.color.map_widget_blue));

		if (storageReadOnly) {
			((TextView) view.findViewById(R.id.description))
					.setText(getString(R.string.storage_directory_readonly_desc));
		}

		View internalStorageRow = view.findViewById(R.id.deviceMemoryRow);
		internalStorageRow.setOnClickListener(internalMemoryOnClickListener);
		ImageView internalStorageImageView = (ImageView) view.findViewById(R.id.deviceMemoryImageView);
		internalStorageImageView.setImageDrawable(getContentIcon(R.drawable.ic_sdcard));
		TextView internalStorageDescription = (TextView) view.findViewById(R.id.deviceMemoryDescription);
		internalStorageDescription.setText(getFreeSpace(internalStorage));

		View sharedMemoryRow = view.findViewById(R.id.sharedMemoryRow);
		if (hasExternalStoragePermission && sharedStorage != null) {
			sharedMemoryRow.setOnClickListener(sharedMemoryOnClickListener);
			ImageView sharedMemoryImageView = (ImageView) view.findViewById(R.id.sharedMemoryImageView);
			sharedMemoryImageView.setImageDrawable(getContentIcon(R.drawable.ic_sdcard));
			TextView sharedMemoryDescription = (TextView) view.findViewById(R.id.sharedMemoryDescription);
			sharedMemoryDescription.setText(getFreeSpace(sharedStorage));
		} else {
			view.findViewById(R.id.divSharedStorage).setVisibility(View.GONE);
			sharedMemoryRow.setVisibility(View.GONE);
		}

		View memoryStickRow = view.findViewById(R.id.memoryStickRow);
		if (hasExternalStoragePermission && externalStorage != null) {
			memoryStickRow.setOnClickListener(externalMemoryOnClickListener);
			ImageView memoryStickImageView = (ImageView) view.findViewById(R.id.memoryStickImageView);
			memoryStickImageView.setImageDrawable(getContentIcon(R.drawable.ic_sdcard));
			TextView memoryStickDescription = (TextView) view.findViewById(R.id.memoryStickDescription);
			memoryStickDescription.setText(getFreeSpace(externalStorage));
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

	public static File getInternalStorageDirectory(Activity activity) {
		return ((OsmandApplication) activity.getApplication()).getSettings()
				.getInternalAppPath();
	}

	public static File getSharedStorageDirectory(Activity activity) {
		return ((OsmandApplication) activity.getApplication()).getSettings()
				.getDefaultInternalStorage();
	}

	private String getFreeSpace(File dir) {
		String sz = "";
		if (dir != null && dir.canRead()) {
			StatFs fs = new StatFs(dir.getAbsolutePath());
			@SuppressWarnings("deprecation")
			float size = (float) fs.getAvailableBlocks() * fs.getBlockSize();
			if (size > 0) {
				if (size > 1 << 20) {
					sz = DownloadActivity.formatGb.format(new Object[]{size / (1 << 30)});
				} else {
					sz = DownloadActivity.formatMb.format(new Object[]{size / (1 << 20)});
				}
			}
		}
		return sz;
	}

	private View.OnClickListener internalMemoryOnClickListener =
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					saveFilesLocation(OsmandSettings.EXTERNAL_STORAGE_TYPE_INTERNAL_FILE,
							internalStorage, getActivity());
					isInterestedInFirstTime = false;
					dismiss();
				}
			};

	private View.OnClickListener sharedMemoryOnClickListener =
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					saveFilesLocation(OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT,
							sharedStorage, getActivity());
					isInterestedInFirstTime = false;
					dismiss();
				}
			};

	private View.OnClickListener externalMemoryOnClickListener =
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean res = saveFilesLocation(OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE,
							externalStorage, getActivity());
					isInterestedInFirstTime = false;
					if (res) {
						dismiss();
					}
				}
			};

	public boolean saveFilesLocation(int type, File selectedFile, Activity context) {
		boolean wr = OsmandSettings.isWritable(selectedFile);
		if (wr) {
			((OsmandApplication) context.getApplication())
					.setExternalStorageDirectory(type, selectedFile.getAbsolutePath());
			reloadData();
		} else {
			AccessibleToast.makeText(context, R.string.specified_directiory_not_writeable,
					Toast.LENGTH_LONG).show();
		}
		return wr;
	}

	private void reloadData() {
		new DashChooseAppDirFragment.ReloadData(getActivity(), getMyApplication()).execute((Void) null);
	}

	public static void showInstance(FragmentManager fragmentManager, boolean storageReadOnly) {
		DataStoragePlaceDialogFragment f = new DataStoragePlaceDialogFragment();
		Bundle args = new Bundle();
		args.putBoolean(STORAGE_READOLNY_KEY, storageReadOnly);
		f.setArguments(args);
		f.show(fragmentManager, DataStoragePlaceDialogFragment.TAG);
	}
}
