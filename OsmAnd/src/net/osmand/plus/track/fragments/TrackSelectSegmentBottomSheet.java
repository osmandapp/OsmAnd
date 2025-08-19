package net.osmand.plus.track.fragments;

import static net.osmand.plus.helpers.TrackSelectSegmentAdapter.GpxItem;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter.RouteItem;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter.SegmentItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.util.Algorithms;

public class TrackSelectSegmentBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = TrackSelectSegmentBottomSheet.class.getSimpleName();

	private GpxFile gpxFile;
	private OnSegmentSelectedListener onSegmentSelectedListener;

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View itemView = inflate(R.layout.bottom_sheet_select_segment);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(itemView).create());

		setupTrackRow(itemView);
		setupItemsList(itemView);
	}

	private void setupTrackRow(@NonNull View view) {
		View routesContainer = view.findViewById(R.id.gpx_track_container);

		String titleGpxTrack = Algorithms.getFileWithoutDirs(gpxFile.getPath());
		String selectSegmentDescription = getString(R.string.select_segments_description, titleGpxTrack);
		SpannableString gpxTrackName = new SpannableString(selectSegmentDescription);
		int startIndex = selectSegmentDescription.indexOf(titleGpxTrack);
		int descriptionColor = ColorUtilities.getSecondaryTextColor(app, nightMode);
		int endIndex = startIndex + titleGpxTrack.length();
		gpxTrackName.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), startIndex, endIndex, 0);
		gpxTrackName.setSpan(new ForegroundColorSpan(descriptionColor), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		TextView description = view.findViewById(R.id.description);
		description.setText(gpxTrackName);

		GpxTrackAnalysis analysis = gpxFile.getAnalysis(0);

		ImageView icon = routesContainer.findViewById(R.id.icon);
		int sidePadding = dpToPx(16f);
		int bottomTopPadding = dpToPx(2f);

		LinearLayout readContainer = routesContainer.findViewById(R.id.read_section);
		readContainer.setPadding(0, bottomTopPadding, 0, bottomTopPadding);
		TextView name = routesContainer.findViewById(R.id.name);
		TextView distance = routesContainer.findViewById(R.id.distance);
		TextView pointsCount = routesContainer.findViewById(R.id.points_count);
		TextView time = routesContainer.findViewById(R.id.time);
		ImageView timeIcon = routesContainer.findViewById(R.id.time_icon);

		LinearLayout container = routesContainer.findViewById(R.id.container);
		LinearLayout containerNameAndReadSection = container.findViewById(R.id.name_and_read_section_container);
		container.setPadding(sidePadding, 0, 0, 0);
		containerNameAndReadSection.setPadding(sidePadding, 0, 0, 0);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_polygom_dark));
		name.setText(titleGpxTrack);
		distance.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		distance.setText(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
		pointsCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		pointsCount.setText(String.valueOf(analysis.getWptPoints()));
		time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		boolean timeSpecified = analysis.isTimeSpecified();
		if (timeSpecified) {
			time.setText(Algorithms.formatDuration(
					analysis.getDurationInSeconds(), app.accessibilityEnabled()));
		}
		AndroidUiHelper.updateVisibility(time, timeSpecified);
		AndroidUiHelper.updateVisibility(timeIcon, timeSpecified);

		routesContainer.setOnClickListener(v -> {
			if (onSegmentSelectedListener != null) {
				onSegmentSelectedListener.onSegmentSelect(gpxFile, -1);
			}
			dismiss();
		});
	}

	private void setupItemsList(@NonNull View view) {
		RecyclerView recyclerView = view.findViewById(R.id.gpx_segment_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setNestedScrollingEnabled(false);

		TrackSelectSegmentAdapter adapter = new TrackSelectSegmentAdapter(view.getContext(), gpxFile);
		adapter.setAdapterListener((item) -> {
			notifyListener(item);
			dismiss();
		});
		recyclerView.setAdapter(adapter);
	}

	private void notifyListener(GpxItem item) {
		if (onSegmentSelectedListener != null) {
			if (item instanceof SegmentItem) {
				onSegmentSelectedListener.onSegmentSelect(gpxFile, item.index);
			} else if (item instanceof RouteItem) {
				onSegmentSelectedListener.onRouteSelected(gpxFile, item.index);
			}
		}
	}

	public interface OnSegmentSelectedListener {
		void onSegmentSelect(@NonNull GpxFile gpxFile, int selectedSegment);

		void onRouteSelected(@NonNull GpxFile gpxFile, int selectedRoute);
	}

	public static boolean shouldShowForGpxFile(@NonNull GpxFile gpxFile) {
		return gpxFile.getNonEmptySegmentsCount() > 1 || gpxFile.getRoutes().size() > 1;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull GpxFile gpxFile,
	                                @Nullable OnSegmentSelectedListener onSegmentSelectedListener) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			TrackSelectSegmentBottomSheet fragment = new TrackSelectSegmentBottomSheet();
			fragment.setRetainInstance(true);
			fragment.onSegmentSelectedListener = onSegmentSelectedListener;
			fragment.gpxFile = gpxFile;
			fragment.show(fragmentManager, TAG);
		}
	}
}