package net.osmand.plus.measurementtool;

import static net.osmand.plus.importfiles.OnSuccessfulGpxImport.OPEN_PLAN_ROUTE_FRAGMENT;
import static net.osmand.plus.track.helpers.GpxUiHelper.getSortedGPXFilesInfo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetBehaviourDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.importfiles.GpxImportListener;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.track.GpxTrackAdapter;
import net.osmand.plus.track.SelectTrackTabsFragment;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class StartPlanRouteBottomSheet extends BottomSheetBehaviourDialogFragment implements CallbackWithObject<String> {

	public static final String TAG = StartPlanRouteBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(StartPlanRouteBottomSheet.class);
	public static final int BOTTOM_SHEET_HEIGHT_DP = 427;
	private static final int OPEN_GPX_DOCUMENT_REQUEST = 1001;

	private View mainView;
	private GpxTrackAdapter adapter;
	private ImportHelper importHelper;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		importHelper = app.getImportHelper();
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.bottom_sheet_plan_route_start, null);

		items.add(new TitleItem(getString(R.string.plan_route)));

		BaseBottomSheetItem createNewRouteItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_notification_track))
				.setTitle(getString(R.string.plan_route_create_new_route))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						MeasurementToolFragment.showInstance(activity.getSupportFragmentManager());
					}
					dismiss();
				})
				.create();
		items.add(createNewRouteItem);

		BaseBottomSheetItem openExistingTrackItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_folder))
				.setTitle(getString(R.string.plan_route_open_existing_track))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					MapActivity mapActivity = (MapActivity) getActivity();
					if (mapActivity != null) {
						hideBottomSheet();
						SelectTrackTabsFragment.showInstance(mapActivity.getSupportFragmentManager(), this);
					}
				})
				.create();
		items.add(openExistingTrackItem);

		BaseBottomSheetItem importTrackItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_import_to))
				.setTitle(getString(R.string.plan_route_import_track))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> importTrack())
				.create();
		items.add(importTrackItem);

		items.add(new DividerItem(getContext()));

		RecyclerView recyclerView = mainView.findViewById(R.id.gpx_track_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> gpxList = getSortedGPXFilesInfo(gpxDir, null, false);
		Collections.sort(gpxList, (lhs, rhs) -> Long.compare(rhs.getLastModified(), lhs.getLastModified()));
		List<GPXInfo> gpxTopList = gpxList.subList(0, Math.min(5, gpxList.size()));
		adapter = new GpxTrackAdapter(requireContext(), gpxTopList);
		adapter.setShowCurrentGpx(false);
		adapter.setShowFolderName(true);
		adapter.setAdapterListener(position -> onItemClick(position, gpxTopList));
		recyclerView.setAdapter(adapter);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
	}

	@Override
	protected int getPeekHeight() {
		return AndroidUtils.dpToPx(requiredMyApplication(), BOTTOM_SHEET_HEIGHT_DP);
	}

	private void onItemClick(int position, List<GPXInfo> gpxInfoList) {
		if (position != RecyclerView.NO_POSITION && position < gpxInfoList.size()) {
			String filePath = gpxInfoList.get(position).getFilePath();
			FragmentActivity activity = getActivity();
			if (activity != null) {
				MeasurementToolFragment.showInstance(activity.getSupportFragmentManager(), filePath, true);
			}
		}
		dismiss();
	}

	private void importTrack() {
		Intent intent = ImportHelper.getImportFileIntent();
		AndroidUtils.startActivityForResultIfSafe(this, intent, OPEN_GPX_DOCUMENT_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == OPEN_GPX_DOCUMENT_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				if (uri != null) {
					importHelper.setGpxImportListener(new GpxImportListener() {
						@Override
						public void onImportComplete(boolean success) {
							finishImport(success);
							importHelper.setGpxImportListener(null);
						}
					});
					importHelper.handleGpxImport(uri, OPEN_PLAN_ROUTE_FRAGMENT, false);
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	void finishImport(boolean success) {
		if (success) {
			dismissAllowingStateLoss();
		}
	}

	public static void showInstance(FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			StartPlanRouteBottomSheet fragment = new StartPlanRouteBottomSheet();
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}

	protected void hideBottomSheet() {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			manager.beginTransaction()
					.hide(this)
					.commitAllowingStateLoss();
		}
	}

	protected void showBottomSheet() {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			manager.beginTransaction()
					.show(this)
					.commitAllowingStateLoss();
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	public boolean processResult(String filePath) {
		dismiss();
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(), filePath, true);
		}
		return true;
	}
}
