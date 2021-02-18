package net.osmand.plus.track;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter.OnItemClickListener;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.util.List;

public class TrackSelectSegmentBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = TrackSelectSegmentBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private GPXFile gpxFile;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		Context context = requireContext();

		LayoutInflater inflater = UiUtilities.getInflater(context, nightMode);
		View itemView = inflater.inflate(R.layout.bottom_sheet_select_segment, null, false);

		String titleGpxTrack = Algorithms.getFileWithoutDirs(gpxFile.path);
		Typeface typeface = FontCache.getRobotoMedium(app);
		String selectSegmentDescription = getString(R.string.select_segments_description, titleGpxTrack);
		SpannableString gpxTrackName = new SpannableString(selectSegmentDescription);
		int startIndex = selectSegmentDescription.indexOf(titleGpxTrack);
		int descriptionColor = getResolvedColor(nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light);
		int endIndex = startIndex + titleGpxTrack.length();
		gpxTrackName.setSpan(new CustomTypefaceSpan(typeface), startIndex, endIndex, 0);
		gpxTrackName.setSpan(new ForegroundColorSpan(descriptionColor), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.create());

		LinearLayout gpxTrackContainer = itemView.findViewById(R.id.gpx_track_container);
		GPXUtilities.GPXTrackAnalysis analysis = gpxFile.getAnalysis(0);

		ImageView icon = gpxTrackContainer.findViewById(R.id.icon);
		int sidePadding = AndroidUtils.dpToPx(context, 16f);
		int bottomTopPadding = AndroidUtils.dpToPx(context, 2f);

		LinearLayout readContainer = gpxTrackContainer.findViewById(R.id.read_section);
		readContainer.setPadding(0, bottomTopPadding, 0, bottomTopPadding);
		TextView name = gpxTrackContainer.findViewById(R.id.name);
		TextView description = itemView.findViewById(R.id.description);
		TextView distance = gpxTrackContainer.findViewById(R.id.distance);
		TextView pointsCount = gpxTrackContainer.findViewById(R.id.points_count);
		TextView time = gpxTrackContainer.findViewById(R.id.time);
		ImageView timeIcon = gpxTrackContainer.findViewById(R.id.time_icon);
		LinearLayout container = gpxTrackContainer.findViewById(R.id.container);
		LinearLayout containerNameAndReadSection = gpxTrackContainer.findViewById(R.id.name_and_read_section_container);
		container.setPadding(sidePadding, 0, 0, 0);
		containerNameAndReadSection.setPadding(sidePadding, 0, 0, 0);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_polygom_dark));
		name.setText(titleGpxTrack);
		description.setText(gpxTrackName);
		distance.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		pointsCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		pointsCount.setText(String.valueOf(analysis.wptPoints));
		time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		boolean timeSpecified = analysis.isTimeSpecified();
		if (timeSpecified) {
			time.setText(Algorithms.formatDuration((int) (analysis.timeSpan / 1000),
					app.accessibilityEnabled()));
		}
		AndroidUiHelper.updateVisibility(time, timeSpecified);
		AndroidUiHelper.updateVisibility(timeIcon, timeSpecified);

		RecyclerView recyclerView = itemView.findViewById(R.id.gpx_segment_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setNestedScrollingEnabled(false);

		List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
		TrackSelectSegmentAdapter adapterSegments = new TrackSelectSegmentAdapter(context, segments);
		adapterSegments.setAdapterListener(new OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				Fragment fragment = getTargetFragment();
				if (fragment instanceof OnSegmentSelectedListener) {
					((OnSegmentSelectedListener) fragment).onSegmentSelect(gpxFile, position);
				}
				dismiss();
			}
		});
		recyclerView.setAdapter(adapterSegments);

		gpxTrackContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = getTargetFragment();
				if (fragment instanceof OnSegmentSelectedListener) {
					((OnSegmentSelectedListener) fragment).onSegmentSelect(gpxFile, -1);
				}
				dismiss();
			}
		});

	}

	public interface OnSegmentSelectedListener {
		void onSegmentSelect(GPXFile gpxFile, int selectedSegment);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull GPXFile gpxFile, @Nullable Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			TrackSelectSegmentBottomSheet fragment = new TrackSelectSegmentBottomSheet();
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.gpxFile = gpxFile;
			fragment.show(fragmentManager, TAG);
		}
	}
}