package net.osmand.plus.helpers;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.track.GpxTrackAdapter;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;
import java.util.List;

public class SelectGpxTrackBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectGpxTrackBottomSheet.class.getSimpleName();

	private List<GPXInfo> gpxInfoList;
	private boolean showCurrentGpx;
	private CallbackWithObject<GpxFile[]> callbackWithObject;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		View mainView = inflater.inflate(R.layout.gpx_track_select_dialog, null);

		RecyclerView recyclerView = mainView.findViewById(R.id.gpx_track_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		GpxTrackAdapter adapter = new GpxTrackAdapter(requireContext(), gpxInfoList);
		adapter.setShowCurrentGpx(showCurrentGpx);
		adapter.setShowFolderName(true);
		adapter.setAdapterListener(position -> {
			if (position != RecyclerView.NO_POSITION) {
				onItemClick(position);
			}
		});
		recyclerView.setAdapter(adapter);
		TextView gpxCounter = mainView.findViewById(R.id.counter);
		gpxCounter.setText(String.valueOf(adapter.getItemCount()));
		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
	}

	private void onItemClick(int position) {
		if (position != -1 && position < gpxInfoList.size()) {
			OsmandApplication app = (OsmandApplication) requireActivity().getApplication();
			if (showCurrentGpx && position == 0) {
				callbackWithObject.processResult(null);
				app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.set(null);
			} else {
				GPXInfo gpxInfo = gpxInfoList.get(position);
				String fileName = gpxInfo.getFileName();
				String filePath = gpxInfo.getFilePath();
				app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.set(fileName);
				SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(filePath);
				if (selectedGpxFile != null) {
					callbackWithObject.processResult(new GpxFile[] {selectedGpxFile.getGpxFile()});
				} else {
					File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
					Activity activity = getActivity();
					if (activity != null) {
						GpxUiHelper.loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
					}
				}
			}
		}
		dismiss();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	public static void showInstance(FragmentManager fragmentManager, boolean showCurrentGpx,
	                                CallbackWithObject<GpxFile[]> callbackWithObject, @NonNull List<GPXInfo> gpxInfoList) {
		if (!fragmentManager.isStateSaved()) {
			SelectGpxTrackBottomSheet fragment = new SelectGpxTrackBottomSheet();
			fragment.setUsedOnMap(true);
			fragment.setRetainInstance(true);
			fragment.showCurrentGpx = showCurrentGpx;
			fragment.callbackWithObject = callbackWithObject;
			fragment.gpxInfoList = gpxInfoList;
			fragment.show(fragmentManager, TAG);
		}
	}
}
