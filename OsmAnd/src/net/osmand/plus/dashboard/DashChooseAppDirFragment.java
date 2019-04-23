package net.osmand.plus.dashboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import gnu.trove.list.array.TIntArrayList;

public class DashChooseAppDirFragment {

	
	public static class ChooseAppDirFragment {
		public static final int VERSION_DEFAULTLOCATION_CHANGED = 19;
		private TextView locationPath;
		private TextView locationDesc;
		MessageFormat formatGb = new MessageFormat("{0, number,#.##} GB", Locale.US);
		private View copyMapsBtn;
		private ImageView editBtn;
		private View dontCopyMapsBtn;
		private View cancelBtn;
		private boolean mapsCopied = false;
		private TextView warningReadonly;
		private int type = -1;
		private File selectedFile = new File("/");
		private File currentAppFile;
		private OsmandSettings settings;
		private Activity activity;
		private Fragment fragment;
		private Dialog dlg;

		private static int typeTemp = -1;
		private static String selectePathTemp;

		public ChooseAppDirFragment(Activity activity, Fragment f) {
			this.activity = activity;
			this.fragment = f;
		}

		public ChooseAppDirFragment(Activity activity, Dialog dlg) {
			this.activity = activity;
			this.dlg = dlg;
		}

		public void setPermissionDenied() {
			typeTemp = -1;
			selectePathTemp = null;
		}

		private String getFreeSpace(File dir) {
			if (dir.canRead()) {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				return formatGb
						.format(new Object[] { (float) (fs.getAvailableBlocks()) * fs.getBlockSize() / (1 << 30) });
			}
			return "";
		}

		public void updateView() {
			if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_INTERNAL_FILE) {
				locationPath.setText(R.string.storage_directory_internal_app);
			} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT) {
				locationPath.setText(R.string.storage_directory_shared);
			} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE) {
				locationPath.setText(R.string.storage_directory_external);
			} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_OBB) {
				locationPath.setText(R.string.storage_directory_multiuser);
			} else if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_SPECIFIED) {
				locationPath.setText(R.string.storage_directory_manual);
			}
			locationDesc.setText(selectedFile.getAbsolutePath() + " \u2022 " + getFreeSpace(selectedFile));
			boolean copyFiles = !currentAppFile.getAbsolutePath().equals(selectedFile.getAbsolutePath()) && !mapsCopied;
			warningReadonly.setVisibility(copyFiles ? View.VISIBLE : View.GONE);
			if (copyFiles) {
				if (!OsmandSettings.isWritable(currentAppFile)) {
					warningReadonly.setText(activity.getString(R.string.android_19_location_disabled,
							currentAppFile.getAbsolutePath()));
				} else {
					warningReadonly.setText(getString(R.string.application_dir_change_warning3));
				}
			}

			copyMapsBtn.setVisibility(copyFiles ? View.VISIBLE : View.GONE);
			dontCopyMapsBtn.setVisibility(copyFiles ? View.VISIBLE : View.GONE);
		}

		public View initView(LayoutInflater inflater, ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.dash_storage_type_fragment, container, false);
			settings = getMyApplication().getSettings();
			locationPath = (TextView) view.findViewById(R.id.location_path);
			locationDesc = (TextView) view.findViewById(R.id.location_desc);
			warningReadonly = (TextView) view.findViewById(R.id.android_19_location_changed);
			currentAppFile = settings.getExternalStorageDirectory();
			selectedFile = currentAppFile;
			if (settings.getExternalStorageDirectoryTypeV19() >= 0) {
				type = settings.getExternalStorageDirectoryTypeV19();
			} else {
				ValueHolder<Integer> vh = new ValueHolder<Integer>();
				settings.getExternalStorageDirectory(vh);
				if (vh.value != null && vh.value >= 0) {
					type = vh.value;
				} else {
					type = 0;
				}
			}
			editBtn = (ImageView) view.findViewById(R.id.edit_icon);
			copyMapsBtn = view.findViewById(R.id.copy_maps);
			dontCopyMapsBtn = view.findViewById(R.id.dont_copy_maps);
			cancelBtn = view.findViewById(R.id.cancel);
			addListeners();
			processPermissionGranted();
			updateView();
			return view;
		}
		
		public String getString(int string) {
			return activity.getString(string);
		}

		@TargetApi(Build.VERSION_CODES.KITKAT)
		protected void showSelectDialog19() {
			AlertDialog.Builder editalert = new AlertDialog.Builder(activity);
			editalert.setTitle(R.string.application_dir);
			final List<String> items = new ArrayList<String>();
			final List<String> paths = new ArrayList<String>();
			final TIntArrayList types = new TIntArrayList();
			int selected = -1;
			if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_SPECIFIED) {
				items.add(getString(R.string.storage_directory_manual));
				paths.add(selectedFile.getAbsolutePath());
				types.add(OsmandSettings.EXTERNAL_STORAGE_TYPE_SPECIFIED);
			}
			File df = settings.getDefaultInternalStorage();
			if (type == OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT) {
				selected = items.size();
			}
			items.add(getString(R.string.storage_directory_shared));
			paths.add(df.getAbsolutePath());
			types.add(OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT);

			File[] externals = getMyApplication().getExternalFilesDirs(null);
			if (externals != null) {
				int i = 1;
				for (File external : externals) {
					if (external != null) {
						if (selectedFile.getAbsolutePath().equals(external.getAbsolutePath())) {
							selected = items.size();
						}
						items.add(getString(R.string.storage_directory_external) + " " + (i++));
						paths.add(external.getAbsolutePath());
						types.add(OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE);
					}
				}
			}

			File[] obbDirs = getMyApplication().getObbDirs();
			if (obbDirs != null) {
				int i = 1;
				for (File obb : obbDirs) {
					if (obb != null) {
						if (selectedFile.getAbsolutePath().equals(obb.getAbsolutePath())) {
							selected = items.size();
						}
						items.add(getString(R.string.storage_directory_multiuser) + " " + (i++));
						paths.add(obb.getAbsolutePath());
						types.add(OsmandSettings.EXTERNAL_STORAGE_TYPE_OBB);
					}
				}
			}

			String pth = settings.getInternalAppPath().getAbsolutePath();
			if (selectedFile.getAbsolutePath().equals(pth)) {
				selected = items.size();
			}
			items.add(getString(R.string.storage_directory_internal_app));
			paths.add(pth);
			types.add(OsmandSettings.EXTERNAL_STORAGE_TYPE_INTERNAL_FILE);

			items.add(getString(R.string.storage_directory_manual) + getString(R.string.shared_string_ellipsis));
			paths.add("");
			types.add(OsmandSettings.EXTERNAL_STORAGE_TYPE_SPECIFIED);

			editalert.setSingleChoiceItems(items.toArray(new String[items.size()]), selected,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == items.size() - 1) {
								dialog.dismiss();
								showOtherDialog();
							} else {

								if (types.get(which) == OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT
										&& !DownloadActivity.hasPermissionToWriteExternalStorage(activity)) {

									typeTemp = types.get(which);
									selectePathTemp = paths.get(which);
									dialog.dismiss();
									if (dlg != null) {
										dlg.dismiss();
									}

									ActivityCompat.requestPermissions(activity,
											new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
											DownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

								} else {
									mapsCopied = false;
									type = types.get(which);
									selectedFile = new File(paths.get(which));
									dialog.dismiss();
									updateView();
								}
							}
						}
					});
			editalert.setNegativeButton(R.string.shared_string_dismiss, null);
			editalert.show();
		}

		private void processPermissionGranted() {
			if (typeTemp != -1 && selectePathTemp != null) {
				mapsCopied = false;
				type = typeTemp;
				selectedFile = new File(selectePathTemp);

				typeTemp = -1;
				selectePathTemp = null;
			}
		}

		public void showOtherDialog() {
			AlertDialog.Builder editalert = new AlertDialog.Builder(activity);
			editalert.setTitle(R.string.application_dir);
			final EditText input = new EditText(activity);
			input.setText(selectedFile.getAbsolutePath());
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT);
			lp.leftMargin = lp.rightMargin = 5;
			lp.bottomMargin = lp.topMargin = 5;
			input.setLayoutParams(lp);
			settings.getExternalStorageDirectory().getAbsolutePath();
			editalert.setView(input);
			editalert.setNegativeButton(R.string.shared_string_cancel, null);
			editalert.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					selectedFile = new File(input.getText().toString());
					mapsCopied = false;
					updateView();
				}
			});
			editalert.show();
		}

		private void addListeners() {
			editBtn.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
						showOtherDialog();
					} else {
						showSelectDialog19();
					}
				}
			});
			copyMapsBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					@SuppressLint("StaticFieldLeak")
					MoveFilesToDifferentDirectory task = new MoveFilesToDifferentDirectory(activity, currentAppFile, selectedFile) {

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
							int moved = getMovedCount();
							int copied = getCopiedCount();
							int failed = getFailedCount();
							sb.append(activity.getString(R.string.files_moved, moved, getFormattedSize(getMovedSize()))).append("\n");
							if (copied > 0) {
								sb.append(activity.getString(R.string.files_copied, copied, getFormattedSize(getCopiedSize()))).append("\n");
							}
							if (failed > 0) {
								sb.append(activity.getString(R.string.files_failed, failed, getFormattedSize(getFailedSize()))).append("\n");
							}
							if (copied > 0 || failed > 0) {
								int count = copied + failed;
								sb.append(activity.getString(R.string.files_present, count, getFormattedSize(getCopiedSize() + getFailedSize()), selectedFile.getAbsolutePath()));
							}
							AlertDialog.Builder bld = new AlertDialog.Builder(activity);
							bld.setMessage(sb.toString());
							bld.setPositiveButton(R.string.shared_string_restart, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									getConfirmListener(true).onClick(v);
								}
							});
							bld.show();
						}

						@Override
						protected void onPostExecute(Boolean result) {
							super.onPostExecute(result);
							if (result) {
								mapsCopied = true;
								getMyApplication().getResourceManager().resetStoreDirectory();
								// immediately proceed with change (to not loose where maps are currently located)
								if (getCopiedCount() > 0 || getFailedCount() > 0) {
									showResultsDialog();
								} else {
									getConfirmListener(false).onClick(v);
								}
							} else {
								showResultsDialog();
								Toast.makeText(activity, R.string.copying_osmand_file_failed,
										Toast.LENGTH_SHORT).show();
								updateView();
							}
							
						}
					};
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			});
			dontCopyMapsBtn.setOnClickListener(getConfirmListener(false));
			cancelBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (dlg != null) {
						dlg.dismiss();
					}
				}
			});
		}

		public OnClickListener getConfirmListener(final boolean silentRestart) {
			return new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean wr = OsmandSettings.isWritable(selectedFile);
					if (wr) {
						boolean changed = !currentAppFile.getAbsolutePath().equals(selectedFile.getAbsolutePath());
						getMyApplication().setExternalStorageDirectory(type, selectedFile.getAbsolutePath());
						if (changed) {
							successCallback();
							reloadData();
						}
						if (fragment != null && activity instanceof FragmentActivity) {
							((FragmentActivity) activity).getSupportFragmentManager().beginTransaction()
									.remove(fragment).commit();
						}
						if (silentRestart) {
							android.os.Process.killProcess(android.os.Process.myPid());
						} else {
							getMyApplication().restartApp(activity);
						}
					} else {
						Toast.makeText(activity, R.string.specified_directiory_not_writeable,
								Toast.LENGTH_LONG).show();
					}
					if (dlg != null) {
						dlg.dismiss();
					}
				}
			};
		}

		// To be implemented by subclass
		protected void successCallback() {}

		protected void reloadData() {
			new ReloadData(activity, getMyApplication()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
		}

		public OsmandApplication getMyApplication() {
			if (activity == null) {
				return null;
			}
			return (OsmandApplication) activity.getApplication();
		}


		

		public static HashSet<String> getExternalMounts() {
			final HashSet<String> out = new HashSet<String>();
			String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
			String s = "";
			try {
				final Process process = new ProcessBuilder().command("mount").redirectErrorStream(true).start();
				process.waitFor();
				final InputStream is = process.getInputStream();
				final byte[] buffer = new byte[1024];
				while (is.read(buffer) != -1) {
					s = s + new String(buffer);
				}
				is.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}

			// parse output
			final String[] lines = s.split("\n");
			for (String line : lines) {
				if (!line.toLowerCase(Locale.US).contains("asec")) {
					if (line.matches(reg)) {
						String[] parts = line.split(" ");
						for (String part : parts) {
							if (part.startsWith("/"))
								if (!part.toLowerCase(Locale.US).contains("vold"))
									out.add(part);
						}
					}
				}
			}
			return out;
		}

		public void setDialog(Dialog dlg) {
			this.dlg = dlg;
		}

	}
	
	public static class MoveFilesToDifferentDirectory extends AsyncTask<Void, Void, Boolean> {

		private File to;
		private Context ctx;
		private File from;
		protected ProgressImplementation progress;
		private Runnable runOnSuccess;
		private int movedCount;
		private long movedSize;
		private int copiedCount;
		private long copiedSize;
		private int failedCount;
		private long failedSize;

		public MoveFilesToDifferentDirectory(Context ctx, File from, File to) {
			this.ctx = ctx;
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
			if (result != null) {
				if (result.booleanValue() && runOnSuccess != null) {
					runOnSuccess.run();
				} else if (!result.booleanValue()) {
					Toast.makeText(ctx, R.string.shared_string_io_error, Toast.LENGTH_LONG).show();
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
			if(depth <= 2) {
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
				} catch(RuntimeException e) {
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
			if(depth <= 2) {
				progress.finishTask();
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			to.mkdirs();
			try {
				movingFiles(from, to, 0);
			} catch (IOException e) {
				return false;
			}
			return true;
		}
		
	}
	
	public static class ReloadData extends AsyncTask<Void, Void, Boolean> {
		private Context ctx;
		protected ProgressImplementation progress;
		private OsmandApplication app;

		public ReloadData(Context ctx, OsmandApplication app) {
			this.ctx = ctx;
			this.app = app;
		}

		@Override
		protected void onPreExecute() {
			progress = ProgressImplementation.createProgressDialog(ctx, ctx.getString(R.string.loading_data),
					ctx.getString(R.string.loading_data), ProgressDialog.STYLE_HORIZONTAL);
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
