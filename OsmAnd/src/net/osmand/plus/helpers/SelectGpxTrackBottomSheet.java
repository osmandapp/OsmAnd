package net.osmand.plus.helpers;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;

import java.io.File;
import java.util.List;

public class SelectGpxTrackBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectGpxTrackBottomSheet.class.getSimpleName();

	protected View mainView;
	protected GpxTrackAdapter adapter;
	private List<GpxUiHelper.GPXInfo> gpxInfoList;
	private boolean showCurrentGpx;
	private CallbackWithObject<GPXUtilities.GPXFile[]> callbackWithObject;

	private void setGpxInfoList(List<GpxUiHelper.GPXInfo> gpxInfoList) {
		this.gpxInfoList = gpxInfoList;
	}

	private void setShowCurrentGpx(boolean showCurrentGpx) {
		this.showCurrentGpx = showCurrentGpx;
	}

	private void setCallbackWithObject(CallbackWithObject<GPXUtilities.GPXFile[]> callbackWithObject) {
		this.callbackWithObject = callbackWithObject;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.gpx_track_select_dialog, null);

		final RecyclerView recyclerView = mainView.findViewById(R.id.gpx_track_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter = new GpxTrackAdapter(requireContext(), gpxInfoList, showCurrentGpx, true);
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

	private void onItemClick(int position) {
		if (position != -1 && position < gpxInfoList.size()) {
			OsmandApplication app = (OsmandApplication) requireActivity().getApplication();
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
					Activity activity = getActivity();
					if (activity != null) {
						GpxUiHelper.loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
					}
				}
			}
		}
		dismiss();
	}

	public static void showInstance(FragmentManager fragmentManager, boolean showCurrentGpx,
	                                CallbackWithObject<GPXUtilities.GPXFile[]> callbackWithObject, List<GpxUiHelper.GPXInfo> gpxInfoList) {
		if (!fragmentManager.isStateSaved()) {
			SelectGpxTrackBottomSheet fragment = new SelectGpxTrackBottomSheet();
			fragment.setUsedOnMap(true);
			fragment.setRetainInstance(true);
			fragment.setShowCurrentGpx(showCurrentGpx);
			fragment.setCallbackWithObject(callbackWithObject);
			fragment.setGpxInfoList(gpxInfoList);
			fragment.show(fragmentManager, SelectGpxTrackBottomSheet.TAG);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}
}
