package net.osmand.plus.mapmarkers.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.PointImageUtils;


public class CoordinateInputAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder> {

	public static final String ADAPTER_POSITION_KEY = "adapter_position_key";
	private GpxFile gpx;

	private final OsmandApplication app;

	private final UiUtilities uiUtilities;
	private final UpdateLocationViewCache updateViewCache;

	private final boolean nightMode;

	private View.OnClickListener listener;
	private View.OnClickListener actionsListener;

	public void setOnClickListener(View.OnClickListener listener) {
		this.listener = listener;
	}
	
	public void setOnActionsClickListener(View.OnClickListener actionsListener) {
		this.actionsListener = actionsListener;
	}
	
	public CoordinateInputAdapter(@NonNull Context context, GpxFile gpx) {
		this.app = (OsmandApplication) context.getApplicationContext();
		this.gpx = gpx;

		uiUtilities = app.getUIUtilities();
		updateViewCache = UpdateLocationUtils.getUpdateLocationViewCache(context);
		nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
	}

	@NonNull
	@Override
	public MapMarkerItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.map_marker_item_new, parent, false);
		view.setOnClickListener(listener);
		return new MapMarkerItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull MapMarkerItemViewHolder holder, int position) {
		WptPt wpt = getItem(position);

		holder.iconDirection.setVisibility(View.VISIBLE);
		holder.icon.setImageDrawable(PointImageUtils.getFromPoint(app, wpt.getColor(), false, wpt));
		holder.mainLayout.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
		holder.title.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		holder.divider.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.divider_color_dark : R.color.divider_color_light));
		holder.iconReorder.setVisibility(View.GONE);
		holder.numberText.setVisibility(View.VISIBLE);
		holder.numberText.setText(String.valueOf(position + 1));
		holder.description.setVisibility(View.GONE);
		holder.optionsBtn.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
		holder.optionsBtn.setOnClickListener(actionsListener);
		AndroidUtils.setDashButtonBackground(app, holder.optionsBtn, nightMode);

		boolean singleItem = getItemCount() == 1;
		boolean fistItem = position == 0;
		boolean lastItem = position == getItemCount() - 1;

		holder.topDivider.setVisibility(fistItem ? View.VISIBLE : View.GONE);
		holder.bottomShadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);
		holder.divider.setVisibility((!singleItem && !lastItem) ? View.VISIBLE : View.GONE);

		holder.title.setText(wpt.getName());
		UpdateLocationUtils.updateLocationView(app, updateViewCache, holder.iconDirection, holder.distance, wpt.getLat(), wpt.getLon());
	}

	@Override
	public int getItemCount() {
		return gpx.getPointsSize();
	}

	public boolean isEmpty() {
		return getItemCount() == 0;
	}

	public WptPt getItem(int position) {
		return gpx.getPointsList().get(position);
	}

	public int getItemPosition(WptPt wptPt) {
		return gpx.getPointsList().indexOf(wptPt);
	}

	public void removeItem(int position) {
		if (position != RecyclerView.NO_POSITION) {
			gpx.deleteWptPt(getItem(position));
			notifyDataSetChanged();
		}
	}
	
	public void setGpx(GpxFile gpx) {
		this.gpx = gpx;
		notifyDataSetChanged();
	}
}
