package net.osmand.plus.track;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.ActivityResultListener.OnActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.importfiles.GpxImportListener;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GpxDialogs {

	private static final int OPEN_GPX_DOCUMENT_REQUEST = 1005;

	public static void selectGPXFile(@NonNull FragmentActivity activity, boolean showCurrentGpx,
	                                 boolean multipleChoice,
	                                 CallbackWithObject<GPXFile[]> callbackWithObject,
	                                 boolean nightMode) {
		int dialogThemeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> list = GpxUiHelper.getSortedGPXFilesInfo(dir, null, false);
		if (list.isEmpty()) {
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		if (!list.isEmpty() || showCurrentGpx) {
			if (showCurrentGpx) {
				list.add(0, new GPXInfo(activity.getString(R.string.show_current_gpx_title), null));
			}

			ContextMenuAdapter adapter = createGpxContextMenuAdapter(app, list);
			createDialog(activity, showCurrentGpx, multipleChoice, callbackWithObject, list, adapter, dialogThemeRes, nightMode);
		}
	}

	@NonNull
	private static ContextMenuAdapter createGpxContextMenuAdapter(@NonNull OsmandApplication app, @NonNull List<GPXInfo> allGpxList) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		fillGpxContextMenuAdapter(adapter, allGpxList, false);
		return adapter;
	}

	private static void fillGpxContextMenuAdapter(@NonNull ContextMenuAdapter adapter, @NonNull List<GPXInfo> allGpxFiles,
	                                              boolean needSelectItems) {
		for (GPXInfo gpxInfo : allGpxFiles) {
			adapter.addItem(new ContextMenuItem(null)
					.setTitle(GpxUiHelper.getGpxTitle(gpxInfo.getFileName()))
					.setSelected(needSelectItems && gpxInfo.isSelected())
					.setIcon(R.drawable.ic_action_polygom_dark));
		}
	}

	private static void createDialog(@NonNull FragmentActivity activity,
	                                 boolean showCurrentGpx,
	                                 boolean multipleChoice,
	                                 CallbackWithObject<GPXFile[]> callbackWithObject,
	                                 List<GPXInfo> gpxInfoList,
	                                 ContextMenuAdapter adapter,
	                                 int themeRes,
	                                 boolean nightMode) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		final int layout = R.layout.gpx_track_item;
		DialogGpxDataItemCallback gpxDataItemCallback = new DialogGpxDataItemCallback(app);

		List<String> modifiableGpxFileNames = ContextMenuUtils.getNames(adapter.getItems());
		ArrayAdapter<String> alertDialogAdapter = new ArrayAdapter<String>(activity, layout, R.id.title, modifiableGpxFileNames) {

			@Override
			public int getItemViewType(int position) {
				return showCurrentGpx && position == 0 ? 1 : 0;
			}

			@Override
			public int getViewTypeCount() {
				return 2;
			}

			private GpxDataItem getDataItem(GPXInfo info) {
				return app.getGpxDbHelper().getItem(
						new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), info.getFileName()),
						gpxDataItemCallback);
			}

			@Override
			@NonNull
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				boolean checkLayout = getItemViewType(position) == 0;
				if (v == null) {
					v = View.inflate(new ContextThemeWrapper(activity, themeRes), layout, null);
				}

				ContextMenuItem item = adapter.getItem(position);
				GPXInfo info = gpxInfoList.get(position);
				boolean currentlyRecordingTrack = showCurrentGpx && position == 0;

				GPXTrackAnalysis analysis = null;
				if (currentlyRecordingTrack) {
					analysis = app.getSavingTrackHelper().getCurrentTrack().getTrackAnalysis(app);
				} else {
					GpxDataItem dataItem = getDataItem(info);
					if (dataItem != null) {
						analysis = dataItem.getAnalysis();
					}
				}
				GpxUiHelper.updateGpxInfoView(v, item.getTitle(), info, analysis, app);

				if (item.getSelected() == null) {
					v.findViewById(R.id.check_item).setVisibility(View.GONE);
					v.findViewById(R.id.check_local_index).setVisibility(View.GONE);
				} else {
					if (checkLayout) {
						CheckBox ch = v.findViewById(R.id.check_local_index);
						ch.setVisibility(View.VISIBLE);
						v.findViewById(R.id.toggle_item).setVisibility(View.GONE);
						ch.setOnCheckedChangeListener(null);
						ch.setChecked(item.getSelected());
						ch.setOnCheckedChangeListener((buttonView, isChecked) -> {
							item.setSelected(isChecked);
						});
						UiUtilities.setupCompoundButton(ch, nightMode, PROFILE_DEPENDENT);
					} else {
						SwitchCompat ch = v.findViewById(R.id.toggle_item);
						ch.setVisibility(View.VISIBLE);
						v.findViewById(R.id.toggle_checkbox_item).setVisibility(View.GONE);
						ch.setOnCheckedChangeListener(null);
						ch.setChecked(item.getSelected());
						ch.setOnCheckedChangeListener((buttonView, isChecked) -> {
							item.setSelected(isChecked);
						});
						UiUtilities.setupCompoundButton(ch, nightMode, PROFILE_DEPENDENT);
					}
					v.findViewById(R.id.check_item).setVisibility(View.VISIBLE);
				}
				return v;
			}
		};
		gpxDataItemCallback.setListAdapter(alertDialogAdapter);
		builder.setAdapter(alertDialogAdapter, (dialog, position) -> {
		});
		if (multipleChoice) {
			builder.setTitle(R.string.show_gpx);
			builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
				GPXFile currentGPX = null;
				//clear all previously selected files before adding new one
				if (app.getSelectedGpxHelper() != null) {
					app.getSelectedGpxHelper().clearAllGpxFilesToShow(false);
				}
				if (showCurrentGpx && adapter.getItem(0).getSelected()) {
					currentGPX = app.getSavingTrackHelper().getCurrentGpx();
				}
				List<String> selectedGpxNames = new ArrayList<>();
				for (int i = (showCurrentGpx ? 1 : 0); i < adapter.length(); i++) {
					if (adapter.getItem(i).getSelected()) {
						selectedGpxNames.add(gpxInfoList.get(i).getFileName());
					}
				}
				dialog.dismiss();
				GpxUiHelper.loadGPXFileInDifferentThread(activity, callbackWithObject, dir, currentGPX,
						selectedGpxNames.toArray(new String[0]));
			});
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			if (gpxInfoList.size() > 1 || !showCurrentGpx && gpxInfoList.size() > 0) {
				builder.setNeutralButton(R.string.gpx_add_track, null);
			}
		}

		AlertDialog dlg = builder.create();
		dlg.setCanceledOnTouchOutside(true);
		if (gpxInfoList.size() == 0 || showCurrentGpx && gpxInfoList.size() == 1) {
			View footerView = activity.getLayoutInflater().inflate(R.layout.no_gpx_files_list_footer, null);
			TextView descTextView = footerView.findViewById(R.id.descFolder);
			String descPrefix = app.getString(R.string.gpx_no_tracks_title_folder);
			SpannableString spannableDesc = new SpannableString(descPrefix + ": " + dir.getAbsolutePath());
			spannableDesc.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
					descPrefix.length() + 1, spannableDesc.length(), 0);
			descTextView.setText(spannableDesc);
			footerView.findViewById(R.id.button).setOnClickListener(v -> {
				addTrack(activity, alertDialogAdapter, adapter, gpxInfoList);
			});
			dlg.getListView().addFooterView(footerView, null, false);
		}
		dlg.getListView().setOnItemClickListener((parent, view, position, id) -> {
			if (multipleChoice) {
				ContextMenuItem item = adapter.getItem(position);
				item.setSelected(!item.getSelected());
				alertDialogAdapter.notifyDataSetInvalidated();
				if (position == 0 && showCurrentGpx && item.getSelected()) {
					OsmandMonitoringPlugin monitoringPlugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
					if (monitoringPlugin == null) {
						AlertDialog.Builder confirm = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
						confirm.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
							Bundle params = new Bundle();
							params.putBoolean(PluginsFragment.OPEN_PLUGINS, true);
							MapActivity.launchMapActivityMoveToTop(activity, null, null, params);
						});
						confirm.setNegativeButton(R.string.shared_string_cancel, null);
						confirm.setMessage(activity.getString(R.string.enable_plugin_monitoring_services));
						confirm.show();
					} else if (!app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()) {
						monitoringPlugin.askShowTripRecordingDialog(activity);
					}
				}
			} else {
				dlg.dismiss();
				if (showCurrentGpx && position == 0) {
					callbackWithObject.processResult(null);
				} else {
					GPXInfo gpxInfo = gpxInfoList.get(position);
					String filePath = gpxInfo.getFilePath();
					SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(filePath);
					if (selectedGpxFile != null) {
						callbackWithObject.processResult(new GPXFile[] {selectedGpxFile.getGpxFile()});
					} else {
						String fileName = gpxInfo.getFileName();
						GpxUiHelper.loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
					}
				}
			}
		});
		dlg.setOnShowListener(dialog -> {
			Button addTrackButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
			if (addTrackButton != null) {
				addTrackButton.setOnClickListener(v -> {
					addTrack(activity, alertDialogAdapter, adapter, gpxInfoList);
				});
			}
		});
		dlg.setOnDismissListener(dialog -> gpxDataItemCallback.setUpdateEnable(false));
		dlg.show();
		try {
			dlg.getListView().setFastScrollEnabled(true);
		} catch (Exception e) {
			// java.lang.ClassCastException: com.android.internal.widget.RoundCornerListAdapter
			// Unknown reason but on some devices fail
		}
	}

	private static void updateGpxDialogAfterImport(Activity activity,
	                                               ArrayAdapter<String> dialogAdapter,
	                                               ContextMenuAdapter adapter, List<GPXInfo> allGpxFiles,
	                                               String importedGpx) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();

		List<String> selectedGpxFiles = new ArrayList<>();
		selectedGpxFiles.add(importedGpx);
		for (int i = 0; i < allGpxFiles.size(); i++) {
			GPXInfo gpxInfo = allGpxFiles.get(i);
			ContextMenuItem menuItem = adapter.getItem(i);
			if (menuItem.getSelected()) {
				boolean isCurrentTrack = gpxInfo.getFileName().equals(app.getString(R.string.show_current_gpx_title));
				selectedGpxFiles.add(isCurrentTrack ? "" : gpxInfo.getFileName());
			}
		}
		allGpxFiles.clear();
		allGpxFiles.addAll(listGpxInfo(app, selectedGpxFiles));
		adapter.clear();
		fillGpxContextMenuAdapter(adapter, allGpxFiles, true);
		dialogAdapter.clear();
		dialogAdapter.addAll(ContextMenuUtils.getNames(adapter.getItems()));
		dialogAdapter.notifyDataSetInvalidated();
	}

	@NonNull
	private static List<GPXInfo> listGpxInfo(@NonNull OsmandApplication app, @NonNull List<String> selectedGpxFiles) {
		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> allGpxList = GpxUiHelper.getSortedGPXFilesInfo(gpxDir, selectedGpxFiles, false);
		GPXInfo currentTrack = new GPXInfo(app.getString(R.string.show_current_gpx_title), null);
		currentTrack.setSelected(selectedGpxFiles.contains(""));
		allGpxList.add(0, currentTrack);
		return allGpxList;
	}

	private static void addTrack(Activity activity, ArrayAdapter<String> listAdapter,
	                             ContextMenuAdapter contextMenuAdapter, List<GPXInfo> allGpxFiles) {
		if (activity instanceof MapActivity) {
			MapActivity mapActivity = (MapActivity) activity;
			OnActivityResultListener listener = (resultCode, resultData) -> {
				if (resultCode != Activity.RESULT_OK || resultData == null) {
					return;
				}

				ImportHelper importHelper = mapActivity.getImportHelper();
				importHelper.setGpxImportListener(new GpxImportListener() {
					@Override
					public void onSaveComplete(boolean success, GPXFile gpxFile) {
						if (success) {
							OsmandApplication app = (OsmandApplication) activity.getApplication();
							GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
							app.getSelectedGpxHelper().selectGpxFile(gpxFile, params);
							updateGpxDialogAfterImport(activity, listAdapter, contextMenuAdapter, allGpxFiles, gpxFile.path);
						}
						importHelper.setGpxImportListener(null);
					}
				});
				Uri uri = resultData.getData();
				if (uri != null) {
					importHelper.handleGpxImport(uri, null, false);
				}
			};

			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.setType("*/*");
			try {
				mapActivity.startActivityForResult(intent, OPEN_GPX_DOCUMENT_REQUEST);
				mapActivity.registerActivityResultListener(new ActivityResultListener(OPEN_GPX_DOCUMENT_REQUEST, listener));
			} catch (ActivityNotFoundException e) {
				Toast.makeText(mapActivity, R.string.no_activity_for_intent, Toast.LENGTH_LONG).show();
			}
		}
	}

	private static class DialogGpxDataItemCallback implements GpxDataItemCallback {

		private static final int UPDATE_GPX_ITEM_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 6;
		private static final long MIN_UPDATE_INTERVAL = 500;

		private final OsmandApplication app;
		private long lastUpdateTime;
		private boolean updateEnable = true;
		private ArrayAdapter<String> listAdapter;

		DialogGpxDataItemCallback(@NonNull OsmandApplication app) {
			this.app = app;
		}

		public void setUpdateEnable(boolean updateEnable) {
			this.updateEnable = updateEnable;
		}

		public ArrayAdapter<String> getListAdapter() {
			return listAdapter;
		}

		public void setListAdapter(ArrayAdapter<String> listAdapter) {
			this.listAdapter = listAdapter;
		}

		private final Runnable updateItemsProc = new Runnable() {
			@Override
			public void run() {
				if (updateEnable) {
					lastUpdateTime = System.currentTimeMillis();
					listAdapter.notifyDataSetChanged();
				}
			}
		};

		@Override
		public boolean isCancelled() {
			return !updateEnable;
		}

		@Override
		public void onGpxDataItemReady(@NonNull GpxDataItem item) {
			if (System.currentTimeMillis() - lastUpdateTime > MIN_UPDATE_INTERVAL) {
				updateItemsProc.run();
			}
			app.runMessageInUIThreadAndCancelPrevious(UPDATE_GPX_ITEM_MSG_ID, updateItemsProc, MIN_UPDATE_INTERVAL);
		}
	}
}
