package net.osmand.plus.download.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.IProgress;
import net.osmand.plus.OnDismissDialogFragmentListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.resources.ResourceManager.ReloadIndexesListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.DataStorageHelper;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class DataStoragePlaceDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = "DataStoragePlaceDialogFragment";
	private static final String STORAGE_READONLY_KEY = "storage_readonly_key";

	private OsmandApplication app;
	private DataStorageHelper storageHelper;

	boolean storageReadOnly;
	boolean hasExternalStoragePermission;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		storageHelper = new DataStorageHelper(app);
	}

	@Override
	public void onStart() {
		super.onStart();
		Dialog dialog = getDialog();
		if (dialog != null && dialog.getWindow() != null) {
			Window window = dialog.getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			params.gravity = Gravity.BOTTOM;
			params.width = ViewGroup.LayoutParams.MATCH_PARENT;
			window.setAttributes(params);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Activity activity = requireActivity();
		hasExternalStoragePermission = DownloadActivity.hasPermissionToWriteExternalStorage(activity);

		Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
		if (args != null) {
			storageReadOnly = args.getBoolean(STORAGE_READONLY_KEY);
		}

		View view = inflater.inflate(R.layout.fragment_data_storage_place_dialog, container, false);
		((ImageView) view.findViewById(R.id.folderIconImageView))
				.setImageDrawable(getIcon(R.drawable.ic_action_folder, R.color.osmand_orange));

		TextView description = view.findViewById(R.id.description);
		description.setText(storageReadOnly ? R.string.storage_directory_readonly_desc : R.string.application_dir_description);

		setupStorageItems(view, inflater);

		ImageButton closeImageButton = view.findViewById(R.id.closeImageButton);
		closeImageButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		closeImageButton.setOnClickListener(v -> dismiss());
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STORAGE_READONLY_KEY, storageReadOnly);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		Activity activity = getActivity();
		if (activity instanceof OnDismissDialogFragmentListener) {
			((OnDismissDialogFragmentListener) activity).onDismissDialogFragment(this);
		}
		Fragment target = getTargetFragment();
		if (target instanceof OnDismissDialogFragmentListener) {
			((OnDismissDialogFragmentListener) target).onDismissDialogFragment(this);
		}
	}

	private void setupStorageItems(@NonNull View view, @NonNull LayoutInflater inflater) {
		LinearLayout itemsContainer = view.findViewById(R.id.storage_items);
		for (StorageItem item : storageHelper.getStorageItems()) {
			String key = item.getKey();
			if (DataStorageHelper.MANUALLY_SPECIFIED.equals(key)
//					|| !hasExternalStoragePermission && key.startsWith(DataStorageHelper.EXTERNAL_STORAGE)
//					|| !hasExternalStoragePermission && key.startsWith(DataStorageHelper.MULTIUSER_STORAGE)
					|| DataStorageHelper.SHARED_STORAGE.equals(key) && (!hasExternalStoragePermission || Build.VERSION.SDK_INT >= 30)) {
				continue;
			}
			View itemView = inflater.inflate(R.layout.data_storage_dialog_row, itemsContainer, false);

			ImageView icon = itemView.findViewById(R.id.icon);
			TextView title = itemView.findViewById(R.id.title);
			TextView description = itemView.findViewById(R.id.description);

			File dir = new File(item.getDirectory());

			title.setText(item.getTitle());
			description.setText(AndroidUtils.getFreeSpace(app, dir));
			icon.setImageDrawable(getContentIcon(item.getNotSelectedIconResId()));

			itemView.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					int type = item.getType();
					boolean res = saveFilesLocation(app, activity, type, dir);
					checkAssets(app);
					updateDownloadIndexes(app);
					if (res || OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE != type) {
						dismiss();
					}
				}
			});
			itemsContainer.addView(itemView);
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

	public static void checkAssets(@NonNull OsmandApplication app) {
		app.getResourceManager().checkAssets(IProgress.EMPTY_PROGRESS, true, false);
	}

	public static void updateDownloadIndexes(@NonNull OsmandApplication app) {
		app.getDownloadThread().runReloadIndexFilesSilent();
	}

	public static boolean saveFilesLocation(@NonNull OsmandApplication app, @NonNull FragmentActivity activity, int type, @NonNull File selectedFile) {
		boolean writable = FileUtils.isWritable(selectedFile);
		if (writable) {
			app.setExternalStorageDirectory(type, selectedFile.getAbsolutePath());
			reloadData(app, activity);
		} else {
			app.showToastMessage(R.string.specified_directiory_not_writeable);
		}
		return writable;
	}

	private static void reloadData(@NonNull OsmandApplication app, @NonNull FragmentActivity activity) {
		final WeakReference<FragmentActivity> activityRef = new WeakReference<>((FragmentActivity) activity);
		app.getResourceManager().reloadIndexesAsync(IProgress.EMPTY_PROGRESS, new ReloadIndexesListener() {

			private ProgressImplementation progress;

			@Override
			public void reloadIndexesStarted() {
				FragmentActivity activity = activityRef.get();
				if (activity != null) {
					progress = ProgressImplementation.createProgressDialog(activity, app.getString(R.string.loading_data),
							app.getString(R.string.loading_data), ProgressDialog.STYLE_HORIZONTAL);
				}
			}

			@Override
			public void reloadIndexesFinished(List<String> warnings) {
				try {
					if (progress != null && progress.getDialog().isShowing()) {
						progress.getDialog().dismiss();
					}
				} catch (Exception e) {
					//ignored
				}
			}
		});
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target, boolean storageReadOnly) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			DataStoragePlaceDialogFragment fragment = new DataStoragePlaceDialogFragment();
			Bundle args = new Bundle();
			args.putBoolean(STORAGE_READONLY_KEY, storageReadOnly);
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);
			fragmentManager.beginTransaction()
					.add(fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}