package net.osmand.plus.measurementtool.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.Location;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

import java.util.Collections;
import java.util.List;

public class MeasurementToolAdapter extends RecyclerView.Adapter<MeasurementToolAdapter.MeasureToolItemVH>
		implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

	private final MapActivity mapActivity;
	private final List<WptPt> points;
	private MeasurementAdapterListener listener;
	private boolean nightMode;
	private static final String BULLET = "   •   ";

	public MeasurementToolAdapter(MapActivity mapActivity, List<WptPt> points) {
		this.mapActivity = mapActivity;
		this.points = points;
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
		int backgroundColor = ColorUtilities.getActivityBgColor(mapActivity, nightMode);
		view.setBackgroundColor(backgroundColor);
		return new MeasureToolItemVH(view);
	}

	@Override
	public void onBindViewHolder(@NonNull MeasureToolItemVH holder, int pos) {
		OsmandApplication app = mapActivity.getMyApplication();
		UiUtilities iconsCache = app.getUIUtilities();
		holder.iconReorder.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_item_move));
		holder.iconReorder.setOnTouchListener((view, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				listener.onDragStarted(holder);
			}
			return false;
		});
		holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_measure_point,
				ColorUtilities.getDefaultIconColorId(nightMode)));
		WptPt pt = points.get(pos);
		String pointTitle = pt.getAmenityOriginName();
		if (!TextUtils.isEmpty(pointTitle)) {
			holder.title.setText(pointTitle);
		} else {
			holder.title.setText(mapActivity.getString(R.string.ltr_or_rtl_combine_via_dash, mapActivity.getString(R.string.plugin_distance_point), String.valueOf(pos + 1)));
		}
		String pointDesc = pt.getDesc();
		if (!TextUtils.isEmpty(pointDesc)) {
			holder.descr.setText(pointDesc);
		} else {
			String text = "";
			Location l1;
			Location l2;
			if (pos < 1) {
				text = mapActivity.getString(R.string.start_point);
				if (app.getLocationProvider().getLastKnownLocation() != null) {
					l1 = app.getLocationProvider().getLastKnownLocation();
					l2 = getLocationFromLL(points.get(0).getLat(), points.get(0).getLon());
					text = text
						+ BULLET + OsmAndFormatter.getFormattedDistance(l1.distanceTo(l2), app)
						+ BULLET + OsmAndFormatter.getFormattedAzimuth(l1.bearingTo(l2), app);
				}
			} else {
				float dist = 0;
				for (int i = 1; i <= pos; i++) {
					l1 = getLocationFromLL(points.get(i - 1).getLat(), points.get(i - 1).getLon());
					l2 = getLocationFromLL(points.get(i).getLat(), points.get(i).getLon());
					dist += l1.distanceTo(l2);
					text = OsmAndFormatter.getFormattedDistance(dist, app)
						+ BULLET + OsmAndFormatter.getFormattedAzimuth(l1.bearingTo(l2), app);
				}
			}
			holder.descr.setText(text);
		}
		double elevation = pt.getEle();
		if (!Double.isNaN(elevation)) {
			String eleStr = (mapActivity.getString(R.string.altitude)).substring(0, 1);
			holder.elevation.setText(mapActivity.getString(R.string.ltr_or_rtl_combine_via_colon,
					eleStr, OsmAndFormatter.getFormattedAlt(elevation, app)));
		} else {
			holder.elevation.setText("");
		}
		float speed = (float) pt.getSpeed();
		if (speed != 0) {
			String speedStr = (mapActivity.getString(R.string.shared_string_speed)).substring(0, 1);
			holder.speed.setText(mapActivity.getString(R.string.ltr_or_rtl_combine_via_colon,
					speedStr, OsmAndFormatter.getFormattedSpeed(speed, app)));
		} else {
			holder.speed.setText("");
		}
		holder.deleteBtn.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_remove_dark,
				ColorUtilities.getDefaultIconColorId(nightMode)));
		holder.deleteBtn.setOnClickListener(view -> listener.onRemoveClick(holder.getAdapterPosition()));
		holder.itemView.setOnClickListener(view -> listener.onItemClick(holder.getAdapterPosition()));
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
	public void onItemDismiss(@NonNull ViewHolder holder) {
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
			iconReorder = view.findViewById(R.id.measure_point_reorder_icon);
			icon = view.findViewById(R.id.measure_point_icon);
			title = view.findViewById(R.id.measure_point_title);
			descr = view.findViewById(R.id.measure_point_descr);
			elevation = view.findViewById(R.id.measure_point_ele);
			speed = view.findViewById(R.id.measure_point_speed);
			deleteBtn = view.findViewById(R.id.measure_point_remove_image_button);
		}
	}

	public interface MeasurementAdapterListener {

		void onRemoveClick(int position);

		void onItemClick(int position);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragEnded(RecyclerView.ViewHolder holder);
	}
}