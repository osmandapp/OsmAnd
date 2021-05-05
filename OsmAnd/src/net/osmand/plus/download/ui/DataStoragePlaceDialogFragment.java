package net.osmand.plus.download.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.FileUtils;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.OnDismissDialogFragmentListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.dashboard.DashChooseAppDirFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

import java.io.File;

import static android.app.Activity.RESULT_OK;

public class DataStoragePlaceDialogFragment extends BottomSheetDialogFragment {
	private static final Log LOG = PlatformUtil.getLog(DataStoragePlaceDialogFragment.class);

	public static final String TAG = "DataStoragePlaceDialogFragment";
	private static final String STORAGE_READOLNY_KEY = "storage_readolny_key";
	public static final int DATA_STORAGE_DIALOG_SCOPED_STORAGE_REQUEST_CODE = 600;

	private OsmandApplication app;

	private File deviceStorage;
	private int deviceStorageType;

	private File sharedStorage;
	private File cardStorage;
	private String scopedStorageDir;

	boolean storageReadOnly;
	boolean hasExternalStoragePermission;

	@Override
	public void onStart() {
		super.onStart();

		Dialog dialog = getDialog();
		if (dialog != null) {
			Window window = dialog.getWindow();
			if (window != null) {
				WindowManager.LayoutParams params = window.getAttributes();
				params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
				params.gravity = Gravity.BOTTOM;
				params.width = ViewGroup.LayoutParams.MATCH_PARENT;
				window.setAttributes(params);
			}
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		Activity activity = requireActivity();
		app = (OsmandApplication) activity.getApplication();
		OsmandSettings settings = app.getSettings();

		hasExternalStoragePermission = DownloadActivity.hasPermissionToWriteExternalStorage(activity);

		File internalStorage = getInternalStorageDirectory(activity);
		File external1Storage = getExternal1StorageDirectory(activity);
		String deviceStorageName;
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
			cardStorage = settings.getSecondaryStorage();
		}
		if (settings.isScopedStorageSupported()) {
			scopedStorageDir = settings.getScopedStorageDirectory();
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

		View scopedStorageRow = view.findViewById(R.id.scopedStorageRow);
		if (settings.isScopedStorageSupported()) {
			ImageView scopedStorageImageView = (ImageView) view.findViewById(R.id.scopedStorageImageView);
			scopedStorageImageView.setImageDrawable(getContentIcon(R.drawable.ic_action_phone));
			TextView scopedStorageDescription = (TextView) view.findViewById(R.id.scopedStorageDescription);
			if (false) {//scopedStorageDir != null) {
				scopedStorageRow.setOnClickListener(scopedStorageOnClickListener);
				scopedStorageDescription.setText(AndroidUtils.getFreeSpace(activity, new File(scopedStorageDir)));
			} else {
				scopedStorageRow.setOnClickListener(chooseScopedStorageOnClickListener);
				scopedStorageDescription.setText(R.string.choose_scoped_storage);
			}
		} else {
			view.findViewById(R.id.divScopedStorage).setVisibility(View.GONE);
			scopedStorageRow.setVisibility(View.GONE);
		}

		final ImageButton closeImageButton = (ImageButton) view.findViewById(R.id.closeImageButton);
		closeImageButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		closeImageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STORAGE_READOLNY_KEY, storageReadOnly);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
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
		requiredMyApplication().getResourceManager().checkAssets(IProgress.EMPTY_PROGRESS, true);
	}

	private void updateDownloadIndexes() {
		DownloadIndexesThread downloadIndexesThread = requiredMyApplication().getDownloadThread();
		downloadIndexesThread.runReloadIndexFilesSilent();
	}

	private final OnClickListener deviceMemoryOnClickListener =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					saveFilesLocation(deviceStorageType, deviceStorage, getActivity());
					checkAssets();
					updateDownloadIndexes();
					dismiss();
				}
			};

	private final OnClickListener sharedMemoryOnClickListener =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					int sharedStorageType = OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT;
					saveFilesLocation(sharedStorageType, sharedStorage, getActivity());
					checkAssets();
					updateDownloadIndexes();
					dismiss();
				}
			};

	private final OnClickListener memoryStickOnClickListener =
			new OnClickListener() {
				@Override
				public void onClick(View v) {
					int cardStorageType = OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE;
					boolean res = saveFilesLocation(cardStorageType, cardStorage, getActivity());
					checkAssets();
					updateDownloadIndexes();
					if (res) {
						dismiss();
					}
				}
			};

	private final OnClickListener scopedStorageOnClickListener =
			new OnClickListener() {
				@RequiresApi(api = Build.VERSION_CODES.KITKAT)
				@Override
				public void onClick(View v) {
					boolean res = saveFilesScopedLocation(scopedStorageDir, app);
					checkAssets();
					updateDownloadIndexes();
					if (res) {
						dismiss();
					}
				}
			};

	private final OnClickListener chooseScopedStorageOnClickListener =
			new OnClickListener() {
				@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
				@Override
				public void onClick(View v) {
					final FragmentActivity activity = getActivity();
					if (activity instanceof MapActivity) {
						final MapActivity mapActivity = (MapActivity) activity;
						mapActivity.registerActivityResultListener(new ActivityResultListener(DATA_STORAGE_DIALOG_SCOPED_STORAGE_REQUEST_CODE, new ActivityResultListener.OnActivityResultListener() {
							@Override
							public void onResult(int resultCode, Intent data) {
								if (resultCode == RESULT_OK) {
									if (data != null) {
										Uri treeUri = data.getData();
										if (treeUri != null) {
											// Do not use root uri
											if (Uri.decode(treeUri.toString()).endsWith(":")) {
												app.showToastMessage(mapActivity.getString(R.string.choose_scoped_storage_root_error));
												return;
											}
											// Persist the permissions
											int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
											app.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);

											saveFilesScopedLocation(treeUri.toString(), app);
											checkAssets();
											updateDownloadIndexes();
										}
									}
								}
							}
						}));
						app.getSettings().askScopedStoragePermission(activity, DATA_STORAGE_DIALOG_SCOPED_STORAGE_REQUEST_CODE);
					}
					dismiss();
				}
			};

	public boolean saveFilesLocation(int type, File selectedFile, Activity context) {
		boolean wr = FileUtils.isWritable(selectedFile);
		if (wr) {
			app.setExternalStorageDirectory(type, selectedFile.getAbsolutePath());
			reloadData();
		} else {
			Toast.makeText(context, R.string.specified_directiory_not_writeable, Toast.LENGTH_LONG).show();
		}
		return wr;
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	public boolean saveFilesScopedLocation(@NonNull String selectedPath, @NonNull Context context) {
		boolean permissionsGranted = app.getSettings().areScopedStoragePermissionsGranted(context, selectedPath);
		if (permissionsGranted) {
			app.getSettings().setScopedStorageDirectory(selectedPath);
			app.setExternalStorageDirectory(OsmandSettings.EXTERNAL_STORAGE_TYPE_SCOPED, selectedPath);
			//reloadData(); TODO activity null
		} else {
			Toast.makeText(context, R.string.specified_directiory_not_writeable, Toast.LENGTH_LONG).show();
		}
		return permissionsGranted;
	}

	private void reloadData() {
		new DashChooseAppDirFragment.ReloadData(getActivity(), getMyApplication())
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
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
