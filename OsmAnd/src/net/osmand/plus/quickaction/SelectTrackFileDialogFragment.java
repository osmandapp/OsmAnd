package net.osmand.plus.quickaction;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.track.GpxTrackAdapter;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;
import java.util.List;

public class SelectTrackFileDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = SelectTrackFileDialogFragment.class.getSimpleName();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), isNightMode());
		View view = themedInflater.inflate(R.layout.select_track_file_dialog_fragment, container, false);
		setupToolbar(view);
		setupRecyclerView(view);
		return view;
	}

	private void setupToolbar(@NonNull View root) {
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		int closeIconColor = ColorUtilities.getActiveButtonsAndLinksTextColorId(isNightMode());
		Drawable closeIcon = getIcon(R.drawable.ic_action_close, closeIconColor);
		toolbar.setNavigationIcon(closeIcon);
		toolbar.setNavigationOnClickListener(v -> dismiss());
	}

	private void setupRecyclerView(@NonNull View root) {
		OsmandApplication app = getMyApplication();
		Context context = root.getContext();

		File gpxRootDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> gpxInfoList = GpxUiHelper.getSortedGPXFilesInfo(gpxRootDir, null, false);
		boolean showCurrentGpx = OsmandPlugin.isActive(OsmandMonitoringPlugin.class);
		if (showCurrentGpx) {
			gpxInfoList.add(0, new GPXInfo(getString(R.string.current_track), 0, 0));
		}
		GpxTrackAdapter adapter = new GpxTrackAdapter(context, gpxInfoList, showCurrentGpx, true);
		adapter.setAdapterListener(position -> {
			Fragment target = getTargetFragment();
			CallbackWithObject<Object> listener = target instanceof CallbackWithObject<?>
					? ((CallbackWithObject<Object>) target)
					: null;
			if (listener != null) {
				boolean currentTrack = position == 0 && showCurrentGpx;
				if (currentTrack) {
					listener.processResult("");
				} else {
					GPXInfo selectedGpxInfo = gpxInfoList.get(position);
					String fileName = selectedGpxInfo.getFileName();
					String gpxFilePath = gpxRootDir.getAbsolutePath() + "/" + fileName;
					listener.processResult(gpxFilePath);
				}
			}
			dismiss();
		});

		RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));
		recyclerView.setAdapter(adapter);
	}

	private boolean isNightMode() {
		return isNightMode(true);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectTrackFileDialogFragment fragment = new SelectTrackFileDialogFragment();
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}