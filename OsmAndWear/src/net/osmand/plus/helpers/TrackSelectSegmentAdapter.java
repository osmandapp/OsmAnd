package net.osmand.plus.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.Route;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter.ItemViewHolder;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class TrackSelectSegmentAdapter extends RecyclerView.Adapter<ItemViewHolder> {

	private static final int ROUTE_TYPE = 0;
	private static final int SEGMENT_TYPE = 1;

	private final OsmandApplication app;
	private final UiUtilities iconsCache;
	private final LayoutInflater themedInflater;
	private final List<GpxItem> gpxItems;

	private OnItemClickListener listener;

	public TrackSelectSegmentAdapter(@NonNull Context ctx, @NonNull GpxFile gpxFile) {
		app = (OsmandApplication) ctx.getApplicationContext();
		themedInflater = UiUtilities.getInflater(ctx, app.getDaynightHelper().isNightModeForMapControls());
		iconsCache = app.getUIUtilities();
		this.gpxItems = getGpxItems(gpxFile);
	}

	public void setAdapterListener(@NonNull OnItemClickListener listener) {
		this.listener = listener;
	}

	@NonNull
	private List<GpxItem> getGpxItems(@NonNull GpxFile gpxFile) {
		List<GpxItem> gpxItems = new ArrayList<>();

		int segmentIndex = 0;
		for (Track track : gpxFile.getTracks(false)) {
			for (TrkSegment segment : track.getSegments()) {
				String trackSegmentTitle = GpxDisplayHelper.buildTrackSegmentName(gpxFile, track, segment, app);
				gpxItems.add(new SegmentItem(segment, trackSegmentTitle, segmentIndex));
				segmentIndex++;
			}
		}

		for (int i = 0; i < gpxFile.getRoutes().size(); i++) {
			Route route = gpxFile.getRoutes().get(i);
			String title = GpxDisplayHelper.getRouteTitle(route, i, app);
			gpxItems.add(new RouteItem(route, title, i));
		}
		return gpxItems;
	}

	@NonNull
	@Override
	public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = themedInflater.inflate(R.layout.gpx_segment_list_item, parent, false);
		ImageView distanceIcon = view.findViewById(R.id.distance_icon);
		distanceIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_split_interval));
		ImageView timeIcon = view.findViewById(R.id.time_icon);
		timeIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_time_moving_16));

		switch (viewType) {
			case SEGMENT_TYPE:
				return new SegmentViewHolder(view);
			case ROUTE_TYPE:
				return new RouteViewHolder(view);
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
		holder.iconSegment.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_split_interval));

		GpxItem gpxItem = gpxItems.get(position);
		holder.name.setText(gpxItem.name);

		long time = 0;
		double distance = 0;
		if (holder instanceof SegmentViewHolder) {
			SegmentItem item = (SegmentItem) gpxItem;
			time = getSegmentTime(item.segment.getPoints());
			distance = getDistance(item.segment.getPoints());
		} else if (holder instanceof RouteViewHolder) {
			RouteItem item = (RouteItem) gpxItem;
			time = getSegmentTime(item.route.getPoints());
			distance = getDistance(item.route.getPoints());
		}
		String formattedTime = time > 0 ? OsmAndFormatter.getFormattedDurationShort((int) (time / 1000)) : "";
		holder.time.setText(formattedTime);
		holder.distance.setText(OsmAndFormatter.getFormattedDistance((float) distance, app));
		AndroidUiHelper.updateVisibility(holder.timeIcon, time > 0);

		holder.itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.onItemClick(gpxItems.get(holder.getAdapterPosition()));
			}
		});
	}

	@Override
	public int getItemViewType(int position) {
		GpxItem gpxItem = gpxItems.get(position);
		if (gpxItem instanceof SegmentItem) {
			return SEGMENT_TYPE;
		} else if (gpxItem instanceof RouteItem) {
			return ROUTE_TYPE;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemCount() {
		return gpxItems.size();
	}

	public static long getSegmentTime(@NonNull List<WptPt> points) {
		long startTime = Long.MAX_VALUE;
		long endTime = Long.MIN_VALUE;
		for (WptPt point : points) {
			long time = point.getTime();
			if (time != 0) {
				startTime = Math.min(startTime, time);
				endTime = Math.max(endTime, time);
			}
		}
		return endTime - startTime;
	}

	public static double getDistance(@NonNull List<WptPt> points) {
		double distance = 0;
		WptPt prevPoint = null;
		for (WptPt point : points) {
			if (prevPoint != null) {
				distance += MapUtils.getDistance(prevPoint.getLatitude(), prevPoint.getLongitude(),
						point.getLatitude(), point.getLongitude());
			}
			prevPoint = point;
		}
		return distance;
	}

	public interface OnItemClickListener {

		void onItemClick(@NonNull GpxItem item);

	}

	abstract static class ItemViewHolder extends ViewHolder {

		ImageView iconSegment;
		ImageView timeIcon;
		TextView name;
		TextView distance;
		TextView time;

		ItemViewHolder(View itemView) {
			super(itemView);
			iconSegment = itemView.findViewById(R.id.icon);
			timeIcon = itemView.findViewById(R.id.time_icon);
			name = itemView.findViewById(R.id.name);
			distance = itemView.findViewById(R.id.distance);
			time = itemView.findViewById(R.id.time_interval);
		}
	}

	static class SegmentViewHolder extends ItemViewHolder {

		SegmentViewHolder(View itemView) {
			super(itemView);
		}
	}


	static class RouteViewHolder extends ItemViewHolder {

		RouteViewHolder(View itemView) {
			super(itemView);
		}
	}

	public abstract static class GpxItem {

		final String name;
		public final int index;

		GpxItem(@NonNull String name, int index) {
			this.name = name;
			this.index = index;
		}
	}

	public static class SegmentItem extends GpxItem {

		final TrkSegment segment;

		SegmentItem(@NonNull TrkSegment segment, @NonNull String name, int index) {
			super(name, index);
			this.segment = segment;
		}
	}

	public static class RouteItem extends GpxItem {

		final Route route;

		RouteItem(@NonNull Route route, @NonNull String name, int index) {
			super(name, index);
			this.route = route;
		}
	}
}