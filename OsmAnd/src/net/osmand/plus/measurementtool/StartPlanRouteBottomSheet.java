package net.osmand.plus.measurementtool;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
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
import net.osmand.plus.helpers.GpxTrackAdapter;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportHelper.OnGpxImportCompleteListener;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static net.osmand.plus.helpers.GpxUiHelper.getSortedGPXFilesInfo;
import static net.osmand.plus.measurementtool.SelectFileBottomSheet.Mode.OPEN_TRACK;

public class StartPlanRouteBottomSheet extends BottomSheetBehaviourDialogFragment {

	public static final String TAG = StartPlanRouteBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(StartPlanRouteBottomSheet.class);
	public static final int BOTTOM_SHEET_HEIGHT_DP = 427;
	private static final int OPEN_GPX_DOCUMENT_REQUEST = 1001;

	protected View mainView;
	protected GpxTrackAdapter adapter;
	private ImportHelper importHelper;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		importHelper = new ImportHelper((AppCompatActivity) getActivity(), getMyApplication(), null);
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.bottom_sheet_plan_route_start, null);

		items.add(new TitleItem(getString(R.string.plan_route)));

		BaseBottomSheetItem createNewRouteItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_notification_track))
				.setTitle(getString(R.string.plan_route_create_new_route))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							MeasurementToolFragment.showInstance(activity.getSupportFragmentManager());
						}
						dismiss();
					}
				})
				.create();
		items.add(createNewRouteItem);

		BaseBottomSheetItem openExistingTrackItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_folder))
				.setTitle(getString(R.string.plan_route_open_existing_track))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MapActivity mapActivity = (MapActivity) getActivity();
						if (mapActivity != null) {
							hideBottomSheet();
							SelectFileBottomSheet.showInstance(mapActivity.getSupportFragmentManager(),
									createSelectFileListener(), OPEN_TRACK);
						}
					}
				})
				.create();
		items.add(openExistingTrackItem);

		BaseBottomSheetItem importTrackItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_import_to))
				.setTitle(getString(R.string.plan_route_import_track))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						importTrack();
					}
				})
				.create();
		items.add(importTrackItem);

		items.add(new DividerItem(getContext()));

		final RecyclerView recyclerView = mainView.findViewById(R.id.gpx_track_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> gpxList = getSortedGPXFilesInfo(gpxDir, null, false);
		Collections.sort(gpxList, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo lhs, GPXInfo rhs) {
				return lhs.getLastModified() > rhs.getLastModified()
						? -1 : (lhs.getLastModified() == rhs.getLastModified() ? 0 : 1);
			}
		});
		final List<GPXInfo> gpxTopList = gpxList.subList(0, Math.min(5, gpxList.size()));
		adapter = new GpxTrackAdapter(requireContext(), gpxTopList, false, true);
		adapter.setAdapterListener(new GpxTrackAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				StartPlanRouteBottomSheet.this.onItemClick(position, gpxTopList);
			}
		});
		recyclerView.setAdapter(adapter);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
	}

	@Override
	protected int getPeekHeight() {
		return AndroidUtils.dpToPx(requiredMyApplication(), BOTTOM_SHEET_HEIGHT_DP);
	}

	private void onItemClick(int position, List<GPXInfo> gpxInfoList) {
		if (position != RecyclerView.NO_POSITION && position < gpxInfoList.size()) {
			String fileName = gpxInfoList.get(position).getFileName();
			FragmentActivity activity = getActivity();
			if (activity != null) {
				MeasurementToolFragment.showInstance(activity.getSupportFragmentManager(), fileName);
			}
		}
		dismiss();
	}

	private void importTrack() {
		Intent intent = ImportHelper.getImportTrackIntent();
		try {
			startActivityForResult(intent, OPEN_GPX_DOCUMENT_REQUEST);
		} catch (ActivityNotFoundException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == OPEN_GPX_DOCUMENT_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				importHelper.setGpxImportCompleteListener(new OnGpxImportCompleteListener() {
					@Override
					public void onImportComplete(boolean success) {
						finishImport(success);
						importHelper.setGpxImportCompleteListener(null);
					}

					@Override
					public void onSaveComplete(boolean success, GPXUtilities.GPXFile result) {

					}
				});
				importHelper.handleGpxImport(uri, ImportHelper.OnSuccessfulGpxImport.OPEN_PLAN_ROUTE_FRAGMENT, false);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	void finishImport(boolean success) {
		if (success) {
			dismiss();
		}
	}

	private SelectFileBottomSheet.SelectFileListener createSelectFileListener() {
		return new SelectFileBottomSheet.SelectFileListener() {
			@Override
			public void selectFileOnCLick(String gpxFileName) {
				dismiss();
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(), gpxFileName);
				}
			}

			@Override
			public void dismissButtonOnClick() {
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					showBottomSheet();
				}
			}
		};
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
					.hide(this).commit();
		}
	}

	protected void showBottomSheet() {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			manager.beginTransaction()
					.show(this).commit();
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}
}
