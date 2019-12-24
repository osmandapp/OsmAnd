package net.osmand.plus.measurementtool.adapter;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.NewGpxData.ActionType;
import net.osmand.plus.profiles.ReorderItemTouchHelperCallback;

import java.util.Collections;
import java.util.List;

public class MeasurementToolAdapter extends RecyclerView.Adapter<MeasurementToolAdapter.MeasureToolItemVH>
		implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

	private final MapActivity mapActivity;
	private final List<WptPt> points;
	private MeasurementAdapterListener listener;
	private boolean nightMode;
	private final ActionType actionType;
	private final static String BULLET = "   •   ";

	public MeasurementToolAdapter(MapActivity mapActivity, List<WptPt> points, ActionType actionType) {
		this.mapActivity = mapActivity;
		this.points = points;
		this.actionType = actionType;
	}

	public void setAdapterListener(MeasurementAdapterListener listener) {
		this.listener = listener;
	}

	@Override
	public MeasureToolItemVH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		View view = inflater.inflate(R.layout.measure_points_list_item, viewGroup, false);
		if (!nightMode) {
			view.findViewById(R.id.points_divider).setBackgroundResource(R.drawable.divider);
		}
		final int backgroundColor = ContextCompat.getColor(mapActivity,
				nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light);
		view.setBackgroundColor(backgroundColor);
		return new MeasureToolItemVH(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final MeasureToolItemVH holder, int pos) {
		UiUtilities iconsCache = mapActivity.getMyApplication().getUIUtilities();
		holder.iconReorder.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_reorder));
		holder.iconReorder.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
					listener.onDragStarted(holder);
				}
				return false;
			}
		});
		holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_measure_point,
				nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light));
		WptPt pt = points.get(pos);
		String pointTitle = pt.name;
		if (!TextUtils.isEmpty(pointTitle)) {
			holder.title.setText(pointTitle);
		} else {
			if (actionType == ActionType.ADD_ROUTE_POINTS) {
				holder.title.setText(mapActivity.getString(R.string.route_point) + " - " + (pos + 1));
			} else {
				holder.title.setText(mapActivity.getString(R.string.plugin_distance_point) + " - " + (pos + 1));
			}
		}
		String pointDesc = pt.desc;
		if (!TextUtils.isEmpty(pointDesc)) {
			holder.descr.setText(pointDesc);
		} else {
			String text = "";
			Location l1;
			Location l2;
			if (pos < 1) {
				text = mapActivity.getString(R.string.shared_string_control_start);
				if (mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation() != null) {
					l1 = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
					l2 = getLocationFromLL(points.get(0).lat, points.get(0).lon);
					text = text
						+ BULLET + OsmAndFormatter.getFormattedDistance(l1.distanceTo(l2), mapActivity.getMyApplication())
						+ BULLET + OsmAndFormatter.getFormattedAzimuth(l1.bearingTo(l2), mapActivity.getMyApplication());
				}
				holder.descr.setText(text);
			} else {
				float dist = 0;
				for (int i = 1; i <= pos; i++) {
					l1 = getLocationFromLL(points.get(i - 1).lat, points.get(i - 1).lon);
					l2 = getLocationFromLL(points.get(i).lat, points.get(i).lon);
					dist += l1.distanceTo(l2);
					text = OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication())
						+ BULLET + OsmAndFormatter.getFormattedAzimuth(l1.bearingTo(l2), mapActivity.getMyApplication());
				}
				holder.descr.setText(text);
			}
		}
		if (actionType == ActionType.EDIT_SEGMENT) {
			double elevation = pt.ele;
			if (!Double.isNaN(elevation)) {
				String eleStr = (mapActivity.getString(R.string.altitude)).substring(0, 1);
				holder.elevation.setText(eleStr + ": " + OsmAndFormatter.getFormattedAlt(elevation, mapActivity.getMyApplication()));
			} else {
				holder.elevation.setText("");
			}
			float speed = (float) pt.speed;
			if (speed != 0) {
				String speedStr = (mapActivity.getString(R.string.map_widget_speed)).substring(0, 1);
				holder.speed.setText(speedStr + ": " + OsmAndFormatter.getFormattedSpeed(speed, mapActivity.getMyApplication()));
			} else {
				holder.speed.setText("");
			}
		}
		holder.deleteBtn.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_remove_dark,
				nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light));
		holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onRemoveClick(holder.getAdapterPosition());
			}
		});
		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onItemClick(holder.getAdapterPosition());
			}
		});
	}

	private Location getLocationFromLL(double lat, double lon) {
		Location l = new Location("");
		l.setLatitude(lat);
		l.setLongitude(lon);
		return l;
	}

	@Override
	public int getItemCount() {
		return points.size();
	}

	@Override
	public boolean onItemMove(int from, int to) {
		Collections.swap(points, from, to);
		notifyItemMoved(from, to);
		return true;
	}

	@Override
	public void onItemDismiss(RecyclerView.ViewHolder holder) {
		listener.onDragEnded(holder);
	}

	static class MeasureToolItemVH extends RecyclerView.ViewHolder {

		final ImageView iconReorder;
		final ImageView icon;
		final TextView title;
		final TextView descr;
		final TextView elevation;
		final TextView speed;
		final ImageButton deleteBtn;

		MeasureToolItemVH(View view) {
			super(view);
			iconReorder = (ImageView) view.findViewById(R.id.measure_point_reorder_icon);
			icon = (ImageView) view.findViewById(R.id.measure_point_icon);
			title = (TextView) view.findViewById(R.id.measure_point_title);
			descr = (TextView) view.findViewById(R.id.measure_point_descr);
			elevation = (TextView) view.findViewById(R.id.measure_point_ele);
			speed = (TextView) view.findViewById(R.id.measure_point_speed);
			deleteBtn = (ImageButton) view.findViewById(R.id.measure_point_remove_image_button);
		}
	}

	public interface MeasurementAdapterListener {

		void onRemoveClick(int position);

		void onItemClick(int position);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragEnded(RecyclerView.ViewHolder holder);
	}
}