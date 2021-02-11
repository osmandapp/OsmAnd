package net.osmand.plus.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.util.MapUtils;

import java.util.List;

public class TrackSelectSegmentAdapter extends RecyclerView.Adapter<TrackSelectSegmentAdapter.TrackViewHolder> {
	private final OsmandApplication app;
	private final LayoutInflater themedInflater;
	private final UiUtilities iconsCache;
	private final List<TrkSegment> segments;
	private GpxTrackAdapter.OnItemClickListener onItemClickListener;

	public TrackSelectSegmentAdapter(Context ctx, List<TrkSegment> segments) {
		app = (OsmandApplication) ctx.getApplicationContext();
		themedInflater = UiUtilities.getInflater(ctx, app.getDaynightHelper().isNightModeForMapControls());
		iconsCache = app.getUIUtilities();
		this.segments = segments;
	}

	@NonNull
	@Override
	public TrackSelectSegmentAdapter.TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = themedInflater.inflate(R.layout.gpx_segment_list_item, parent, false);
		ImageView distanceIcon = view.findViewById(R.id.distance_icon);
		distanceIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_split_interval));
		ImageView timeIcon = view.findViewById(R.id.time_icon);
		timeIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_moving_16));
		return new TrackSelectSegmentAdapter.TrackViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final TrackSelectSegmentAdapter.TrackViewHolder holder, final int position) {
		holder.icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_split_interval));

		TrkSegment segment = segments.get(position);

		String segmentTitle = app.getResources().getString(R.string.segments_count, position + 1);
		holder.name.setText(segmentTitle);

		double distance = getDistance(segment);
		long time = getSegmentTime(segment);
		holder.time.setText(OsmAndFormatter.getFormattedDurationShort((int) (time / 1000)));
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

	private long getSegmentTime(TrkSegment segment) {
		long segmentTime;
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
		segmentTime = endTime - startTime;

		return segmentTime;
	}

	private double getDistance(TrkSegment segment) {
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

	@Override
	public int getItemCount() {
		return segments.size();
	}


	public void setAdapterListener(GpxTrackAdapter.OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	static class TrackViewHolder extends RecyclerView.ViewHolder {

		ImageView icon;
		TextView name;
		TextView distance;
		TextView time;

		TrackViewHolder(View itemView) {
			super(itemView);
			icon = itemView.findViewById(R.id.icon);
			name = itemView.findViewById(R.id.name);
			distance = itemView.findViewById(R.id.distance);
			time = itemView.findViewById(R.id.time_interval);
		}
	}
}