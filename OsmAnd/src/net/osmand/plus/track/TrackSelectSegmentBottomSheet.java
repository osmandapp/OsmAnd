package net.osmand.plus.track;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmAndFormatter;
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
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.util.List;

public class TrackSelectSegmentBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = TrackSelectSegmentBottomSheet.class.getSimpleName();
	protected TrackSelectSegmentAdapter adapterSegments;
	private GpxSelectionHelper.SelectedGpxFile selectedFile;
	private MapActivity mapActivity;
	private GPXUtilities.GPXFile gpxFile;
	private OsmandApplication app;

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull GpxSelectionHelper.SelectedGpxFile selectedFile) {
		if (!fragmentManager.isStateSaved()) {
			TrackSelectSegmentBottomSheet fragment = new TrackSelectSegmentBottomSheet();
			fragment.selectedFile = selectedFile;
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();

		LayoutInflater inflater = UiUtilities.getInflater(context, nightMode);
		View itemView = inflater.inflate(R.layout.bottom_sheet_select_segment, null, false);

		app = getMyApplication();
		if (app == null) {
			return;
		}

		mapActivity = (MapActivity) getActivity();
		gpxFile = selectedFile.getGpxFile();
		String titleGpxTrack = Algorithms.getFileWithoutDirs(gpxFile.path);
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

		LinearLayout gpxTrackContainer = itemView.findViewById(R.id.gpx_track_container);
		GPXUtilities.GPXTrackAnalysis analysis = gpxFile.getAnalysis(0);

		ImageView icon = gpxTrackContainer.findViewById(R.id.icon);
		icon.setId(R.id.icon1);
		TextView name = gpxTrackContainer.findViewById(R.id.name);
		TextView distance = gpxTrackContainer.findViewById(R.id.distance);
		TextView pointsCount = gpxTrackContainer.findViewById(R.id.points_count);
		TextView time = gpxTrackContainer.findViewById(R.id.time);

		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_polygom_dark));
		name.setText(titleGpxTrack);
		distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		pointsCount.setText(String.valueOf(analysis.wptPoints));
		time.setText(analysis.isTimeSpecified() ? Algorithms.formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()) : "");

		final RecyclerView recyclerView = itemView.findViewById(R.id.gpx_segment_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setNestedScrollingEnabled(false);
		final List<TrkSegment> segments = selectedFile.getPointsToDisplay();
		adapterSegments = new TrackSelectSegmentAdapter(requireContext(), segments);
		adapterSegments.setAdapterListener(new GpxTrackAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				//select segment
				dismiss();
			}
		});
		recyclerView.setAdapter(adapterSegments);

		gpxTrackContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectSegmentToFollow(gpxFile);
				dismiss();
			}
		});

	}

	private void selectSegmentToFollow(GPXUtilities.GPXFile gpxFile) {
		if (mapActivity != null) {
			this.gpxFile = gpxFile;
			TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
			RoutingHelper routingHelper = app.getRoutingHelper();
			List<GPXUtilities.WptPt> points = gpxFile.getRoutePoints();
			if (!points.isEmpty()) {
				ApplicationMode mode = ApplicationMode.valueOfStringKey(points.get(0).getProfileType(), null);
				if (mode != null) {
					routingHelper.setAppMode(mode);
					app.initVoiceCommandPlayer(mapActivity, mode, true, null, false, false, true);
				}
			}
			mapActivity.getMapActions().setGPXRouteParams(gpxFile);
			targetPointsHelper.updateRouteAndRefresh(true);
			routingHelper.onSettingsChanged(true);
		}
	}
}

