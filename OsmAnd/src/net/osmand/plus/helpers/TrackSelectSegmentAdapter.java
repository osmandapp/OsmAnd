package net.osmand.plus.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter.TrackViewHolder;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TrackSelectSegmentAdapter extends RecyclerView.Adapter<TrackViewHolder> {

	private final OsmandApplication app;
	private final LayoutInflater themedInflater;
	private final UiUtilities iconsCache;
	private final List<TitledSegment> titledSegments;
	private OnItemClickListener onItemClickListener;

	public TrackSelectSegmentAdapter(Context ctx, GPXFile gpxFile) {
		app = (OsmandApplication) ctx.getApplicationContext();
		themedInflater = UiUtilities.getInflater(ctx, app.getDaynightHelper().isNightModeForMapControls());
		iconsCache = app.getUIUtilities();
		this.titledSegments = getTitledSegments(gpxFile);
	}

	@NonNull
	private List<TitledSegment> getTitledSegments(GPXFile gpxFile) {
		List<TitledSegment> titledSegments = new ArrayList<>();
		for (Track track : gpxFile.tracks) {
			if (track.generalTrack) {
				continue;
			}
			for (TrkSegment segment : track.segments) {
				String trackSegmentTitle = GpxSelectionHelper.buildTrackSegmentName(gpxFile, track, segment, app);
				titledSegments.add(new TitledSegment(segment, trackSegmentTitle));
			}
		}
		return titledSegments;
	}

	@NonNull
	@Override
	public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = themedInflater.inflate(R.layout.gpx_segment_list_item, parent, false);
		ImageView distanceIcon = view.findViewById(R.id.distance_icon);
		distanceIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_split_interval));
		ImageView timeIcon = view.findViewById(R.id.time_icon);
		timeIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_moving_16));
		return new TrackViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final TrackViewHolder holder, int position) {
		holder.iconSegment.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_split_interval));

		TitledSegment titledSegment = titledSegments.get(position);

		holder.name.setText(titledSegment.name);

		double distance = getDistance(titledSegment.segment);
		long time = getSegmentTime(titledSegment.segment);
		if (time != 1) {
			holder.timeIcon.setVisibility(View.VISIBLE);
			holder.time.setText(OsmAndFormatter.getFormattedDurationShort((int) (time / 1000)));
		} else {
			holder.timeIcon.setVisibility(View.GONE);
			holder.time.setText("");
		}
		holder.distance.setText(OsmAndFormatter.getFormattedDistance((float) distance, app));
		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onItemClickListener != null) {
					onItemClickListener.onItemClick(holder.getAdapterPosition());
				}
			}
		});
	}

	@Override
	public int getItemCount() {
		return titledSegments.size();
	}

	public static long getSegmentTime(TrkSegment segment) {
		long startTime = Long.MAX_VALUE;
		long endTime = Long.MIN_VALUE;
		for (int i = 0; i < segment.points.size(); i++) {
			WptPt point = segment.points.get(i);
			long time = point.time;
			if (time != 0) {
				startTime = Math.min(startTime, time);
				endTime = Math.max(endTime, time);
			}
		}
		return endTime - startTime;
	}

	public static double getDistance(TrkSegment segment) {
		double distance = 0;
		WptPt prevPoint = null;
		for (int i = 0; i < segment.points.size(); i++) {
			WptPt point = segment.points.get(i);
			if (prevPoint != null) {
				distance += MapUtils.getDistance(prevPoint.getLatitude(), prevPoint.getLongitude(), point.getLatitude(), point.getLongitude());
			}
			prevPoint = point;
		}
		return distance;
	}

	public void setAdapterListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	public interface OnItemClickListener {

		void onItemClick(int position);

	}

	static class TrackViewHolder extends RecyclerView.ViewHolder {

		ImageView iconSegment;
		ImageView timeIcon;
		TextView name;
		TextView distance;
		TextView time;

		TrackViewHolder(View itemView) {
			super(itemView);
			iconSegment = itemView.findViewById(R.id.icon);
			timeIcon = itemView.findViewById(R.id.time_icon);
			name = itemView.findViewById(R.id.name);
			distance = itemView.findViewById(R.id.distance);
			time = itemView.findViewById(R.id.time_interval);
		}
	}

	static class TitledSegment {

		TrkSegment segment;
		String name;

		TitledSegment(TrkSegment segment, String name) {
			this.segment = segment;
			this.name = name;
		}
	}
}