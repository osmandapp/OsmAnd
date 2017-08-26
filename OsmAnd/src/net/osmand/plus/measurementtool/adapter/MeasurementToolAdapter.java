package net.osmand.plus.measurementtool.adapter;

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

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.NewGpxData;
import net.osmand.util.MapUtils;

import java.util.Collections;
import java.util.List;

public class MeasurementToolAdapter extends RecyclerView.Adapter<MeasurementToolAdapter.MeasureToolItemVH>
		implements MeasurementToolItemTouchHelperCallback.ItemTouchHelperAdapter {

	private final MapActivity mapActivity;
	private final List<WptPt> points;
	private MeasurementAdapterListener listener;
	private boolean nightMode;
	private NewGpxData.ActionType actionType;

	public MeasurementToolAdapter(MapActivity mapActivity, List<WptPt> points, NewGpxData.ActionType actionType) {
		this.mapActivity = mapActivity;
		this.points = points;
		this.actionType = actionType;
	}

	public void setAdapterListener(MeasurementAdapterListener listener) {
		this.listener = listener;
	}

	@Override
	public MeasureToolItemVH onCreateViewHolder(ViewGroup viewGroup, int i) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.measure_points_list_item, viewGroup, false);
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		if (!nightMode) {
			view.findViewById(R.id.points_divider).setBackgroundResource(R.drawable.divider);
		}
		final int backgroundColor = ContextCompat.getColor(mapActivity,
				nightMode ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);
		view.setBackgroundColor(backgroundColor);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onItemClick(view);
			}
		});
		return new MeasureToolItemVH(view);
	}

	@Override
	public void onBindViewHolder(final MeasureToolItemVH holder, int pos) {
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
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
				nightMode ? R.color.ctx_menu_info_text_dark : R.color.icon_color));
		if (nightMode) {
			holder.title.setTextColor(mapActivity.getMyApplication().getResources().getColor(R.color.primary_text_dark));
		}
		WptPt pt = points.get(pos);
		String pointTitle = pt.name;
		if (!TextUtils.isEmpty(pointTitle)) {
			holder.title.setText(pointTitle);
		} else {
			if (actionType == NewGpxData.ActionType.ADD_ROUTE_POINTS) {
				holder.title.setText(mapActivity.getString(R.string.route_point) + " - " + (pos + 1));
			} else {
				holder.title.setText(mapActivity.getString(R.string.plugin_distance_point) + " - " + (pos + 1));
			}
		}
		String pointDesc = pt.desc;
		if (!TextUtils.isEmpty(pointDesc)) {
			holder.descr.setText(pointDesc);
		} else {
			if (pos < 1) {
				holder.descr.setText(mapActivity.getString(R.string.shared_string_control_start));
			} else {
				float dist = 0;
				for (int i = 1; i <= pos; i++) {
					dist += MapUtils.getDistance(points.get(i - 1).lat, points.get(i - 1).lon,
							points.get(i).lat, points.get(i).lon);
				}
				holder.descr.setText(OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication()));
			}
		}
		if (actionType == NewGpxData.ActionType.EDIT_SEGMENT) {
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
				nightMode ? R.color.ctx_menu_info_text_dark : R.color.icon_color));
		holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onRemoveClick(holder.getAdapterPosition());
			}
		});
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

		void onItemClick(View view);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragEnded(RecyclerView.ViewHolder holder);
	}
}