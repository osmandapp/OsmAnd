package net.osmand.plus.track;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.helpers.GpxTrackAdapter;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.List;

public class TrackSelectSegmentBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = TrackSelectSegmentBottomSheet.class.getSimpleName();
	protected TrackSelectSegmentAdapter adapterSegments;
	private GpxSelectionHelper.SelectedGpxFile file;

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull GpxSelectionHelper.SelectedGpxFile file) {
		if (!fragmentManager.isStateSaved()) {
			TrackSelectSegmentBottomSheet fragment = new TrackSelectSegmentBottomSheet();
			fragment.file = file;
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();

		LayoutInflater inflater = UiUtilities.getInflater(context, nightMode);
		View itemView = inflater.inflate(R.layout.bottom_sheet_select_segment, null, false);
		String titleGpxTrack = Algorithms.getFileWithoutDirs(file.getGpxFile().path);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.select_segments_description, titleGpxTrack))
				.setCustomView(itemView)
				.create());

		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		TextViewEx titleGpxTrackView = itemView.findViewById(R.id.title_gpx_track);
		titleGpxTrackView.setText(titleGpxTrack);

		final RecyclerView recyclerView = itemView.findViewById(R.id.gpx_segment_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setNestedScrollingEnabled(false);
		List<TrkSegment> segments =  file.getPointsToDisplay();
		adapterSegments = new TrackSelectSegmentAdapter(requireContext(), segments);
		adapterSegments.setAdapterListener(new GpxTrackAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
			}
		});
		recyclerView.setAdapter(adapterSegments);

	}
}
