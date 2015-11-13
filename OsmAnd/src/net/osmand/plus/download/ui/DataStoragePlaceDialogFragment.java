package net.osmand.plus.download.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.StatFs;
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
import net.osmand.plus.download.DownloadActivity;

import java.io.File;

public class DataStoragePlaceDialogFragment extends BottomSheetDialogFragment {

	private File internalStorage;
	private File externalStorage;
	public static boolean isInterestedInFirstTime = true;

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
		internalStorage = getInternalStorageDirectory(getActivity());
		externalStorage = getMyApplication().getSettings().getSecondaryStorage();

		final View view = inflater.inflate(R.layout.fragment_data_storage_place_dialog, container,
				false);
		((ImageView) view.findViewById(R.id.folderIconImageView))
				.setImageDrawable(getIcon(R.drawable.ic_action_folder, R.color.map_widget_blue));

		ImageView internalStorageImageView = (ImageView) view.findViewById(R.id.deviceMemoryImageView);
		internalStorageImageView.setImageDrawable(getContentIcon(R.drawable.ic_sdcard));
		internalStorageImageView.setOnClickListener(internalMemoryOnClickListener);

		View internalStorageTitle = view.findViewById(R.id.deviceMemoryTitle);
		internalStorageTitle.setOnClickListener(internalMemoryOnClickListener);

		TextView internalStorageDescription = (TextView) view.findViewById(R.id.deviceMemoryDescription);
		internalStorageDescription.setOnClickListener(internalMemoryOnClickListener);
		internalStorageDescription.setText(getFreeSpace(internalStorage));

		ImageView externalStorageImageView = ((ImageView) view.findViewById(R.id.memoryStickImageView));
		externalStorageImageView.setImageDrawable(getContentIcon(R.drawable.ic_sdcard));
		externalStorageImageView.setOnClickListener(externalMemoryOnClickListener);

		View externalStorageTitle = view.findViewById(R.id.memoryStickTitle);
		externalStorageTitle.setOnClickListener(externalMemoryOnClickListener);

		TextView externalStorageDescription = (TextView) view.findViewById(R.id.memoryStickDescription);
		externalStorageDescription.setOnClickListener(externalMemoryOnClickListener);
		externalStorageDescription.setText(getFreeSpace(externalStorage));

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

	public static File getInternalStorageDirectory(Activity activity) {
		return ((OsmandApplication) activity.getApplication()).getSettings()
				.getDefaultInternalStorage();
	}

	private String getFreeSpace(File dir) {
		String sz = "";
		if (dir.canRead()) {
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
					saveFilesLocation(OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT,
							internalStorage, getActivity());
					isInterestedInFirstTime = false;
					dismiss();
				}
			};

	private View.OnClickListener externalMemoryOnClickListener =
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					saveFilesLocation(OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE,
							externalStorage, getActivity());
					isInterestedInFirstTime = false;
					dismiss();
				}
			};

	public static void saveFilesLocation(int type, File selectedFile, Activity context) {
		boolean wr = OsmandSettings.isWritable(selectedFile);
		if (wr) {
			((OsmandApplication) context.getApplication())
					.setExternalStorageDirectory(type, selectedFile.getAbsolutePath());
		} else {
			AccessibleToast.makeText(context, R.string.specified_directiory_not_writeable,
					Toast.LENGTH_LONG).show();
		}
	}
}
