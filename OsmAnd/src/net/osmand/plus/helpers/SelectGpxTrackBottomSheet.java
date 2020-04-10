package net.osmand.plus.helpers;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;

import java.io.File;
import java.util.List;

public class SelectGpxTrackBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SelectGpxTrackBottomSheet";

	protected View mainView;
	protected GpxTrackAdapter adapter;

	private List<GpxUiHelper.GPXInfo> gpxInfoList;
	private OsmandApplication app;
	private boolean showCurrentGpx;
	private CallbackWithObject<GPXUtilities.GPXFile[]> callbackWithObject;
	private Activity activity;

	private SelectGpxTrackBottomSheet(Activity activity, boolean showCurrentGpx, CallbackWithObject<GPXUtilities.GPXFile[]> callbackWithObject,
	                                  List<GpxUiHelper.GPXInfo> gpxInfoList) {
		super();
		app = (OsmandApplication) activity.getApplication();
		this.activity = activity;
		this.showCurrentGpx = showCurrentGpx;
		this.gpxInfoList = gpxInfoList;
		this.callbackWithObject = callbackWithObject;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.gpx_track_select_dialog, null);

		final RecyclerView recyclerView = mainView.findViewById(R.id.gpx_track_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter = createAdapter();
		adapter.setAdapterListener(new GpxTrackAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				if (position != RecyclerView.NO_POSITION) {
					SelectGpxTrackBottomSheet.this.onItemClick(position);
				}
			}
		});
		recyclerView.setAdapter(adapter);
		TextView gpxCounter = mainView.findViewById(R.id.counter);
		gpxCounter.setText(String.valueOf(adapter.getItemCount()));
		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
	}

	private GpxTrackAdapter createAdapter() {
		return new GpxTrackAdapter(activity, gpxInfoList, showCurrentGpx);
	}

	private void onItemClick(int position) {
		if (position != -1 && position < gpxInfoList.size()) {
			if (showCurrentGpx && position == 0) {
				callbackWithObject.processResult(null);
				app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.set(null);
			} else {
				String fileName = gpxInfoList.get(position).getFileName();
				app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.set(fileName);
				GpxSelectionHelper.SelectedGpxFile selectedGpxFile =
						app.getSelectedGpxHelper().getSelectedFileByName(fileName);
				if (selectedGpxFile != null) {
					callbackWithObject.processResult(new GPXUtilities.GPXFile[]{selectedGpxFile.getGpxFile()});
				} else {
					File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
					GpxUiHelper.loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
				}
			}
		}
		dismiss();
	}

	public static void showInstance(MapActivity mapActivity, boolean showCurrentGpx,
	                                CallbackWithObject<GPXUtilities.GPXFile[]> callbackWithObject, List<GpxUiHelper.GPXInfo> gpxInfoList) {
		SelectGpxTrackBottomSheet fragment = new SelectGpxTrackBottomSheet(mapActivity, showCurrentGpx, callbackWithObject, gpxInfoList);
		fragment.setUsedOnMap(true);
		fragment.setRetainInstance(true);
		fragment.show(mapActivity.getSupportFragmentManager(), SelectGpxTrackBottomSheet.TAG);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

}
