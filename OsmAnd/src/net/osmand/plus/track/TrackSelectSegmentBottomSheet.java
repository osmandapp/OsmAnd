package net.osmand.plus.track;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.GpxTrackAdapter;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
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

		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		String titleGpxTrack = Algorithms.getFileWithoutDirs(file.getGpxFile().path);
		Typeface typeface = FontCache.getRobotoMedium(app);
		String selectSegmentDescription = getString(R.string.select_segments_description, titleGpxTrack);
		SpannableString gpxTrackName = new SpannableString(selectSegmentDescription);
		int startIndex = selectSegmentDescription.indexOf(titleGpxTrack);
		int descriptionColor = getResolvedColor(nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light);
		int endIndex = startIndex + titleGpxTrack.length();
		gpxTrackName.setSpan(new CustomTypefaceSpan(typeface), startIndex, endIndex, 0);
		gpxTrackName.setSpan(new ForegroundColorSpan(descriptionColor), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(gpxTrackName)
				.setCustomView(itemView)
				.create());

		TextViewEx titleGpxTrackView = itemView.findViewById(R.id.title_gpx_track);
		titleGpxTrackView.setText(titleGpxTrack);

		final RecyclerView recyclerView = itemView.findViewById(R.id.gpx_segment_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setNestedScrollingEnabled(false);
		final List<TrkSegment> segments = file.getPointsToDisplay();
		adapterSegments = new TrackSelectSegmentAdapter(requireContext(), segments);
		adapterSegments.setAdapterListener(new GpxTrackAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				selectSegmentToFollow(segments.get(position), file);
				dismiss();
			}
		});
		recyclerView.setAdapter(adapterSegments);

	}

	private void selectSegmentToFollow(TrkSegment segment, GpxSelectionHelper.SelectedGpxFile file) {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			TargetPointsHelper targetPointsHelper = mapActivity.getMyApplication().getTargetPointsHelper();
			RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
			List<GPXUtilities.WptPt> points = segment.points;
			if (!points.isEmpty()) {
				ApplicationMode mode = ApplicationMode.valueOfStringKey(points.get(0).getProfileType(), null);
				if (mode != null) {
					routingHelper.setAppMode(mode);
					mapActivity.getMyApplication().initVoiceCommandPlayer(mapActivity, mode, true, null, false, false, true);
				}
			}
			mapActivity.getMapActions().setGPXRouteParams(file.getGpxFile());
			targetPointsHelper.updateRouteAndRefresh(true);
			routingHelper.onSettingsChanged(true);
		}
	}
}

