package net.osmand.plus.mapmarkers.adapters;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;

import java.util.List;

public class CoordinateInputAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder> {

	private MapActivity mapActivity;
	private IconsCache iconsCache;
	private List<MapMarker> mapMarkers;

	private boolean nightTheme;

	private LatLon location;
	private Float heading;
	private boolean useCenter;
	private int screenOrientation;

	public CoordinateInputAdapter(MapActivity mapActivity, List<MapMarker> mapMarkers) {
		this.mapActivity = mapActivity;
		iconsCache = mapActivity.getMyApplication().getIconsCache();
		this.mapMarkers = mapMarkers;
		nightTheme = !mapActivity.getMyApplication().getSettings().isLightContent();
	}

	public void setLocation(LatLon location) {
		this.location = location;
	}

	public void setHeading(Float heading) {
		this.heading = heading;
	}

	public void setUseCenter(boolean useCenter) {
		this.useCenter = useCenter;
	}

	public void setScreenOrientation(int screenOrientation) {
		this.screenOrientation = screenOrientation;
	}

	@Override
	public MapMarkerItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.map_marker_item_new, parent, false);
		return new MapMarkerItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(final MapMarkerItemViewHolder holder, int position) {
		final MapMarker mapMarker = getItem(position);

		holder.iconDirection.setVisibility(View.VISIBLE);
		holder.icon.setImageDrawable(getColoredIcon(R.drawable.ic_action_flag_dark, MapMarker.getColorId(mapMarker.colorIndex)));
		holder.mainLayout.setBackgroundColor(getResolvedColor(nightTheme ? R.color.ctx_menu_bg_dark : R.color.bg_color_light));
		holder.title.setTextColor(getResolvedColor(nightTheme ? R.color.ctx_menu_title_color_dark : R.color.color_black));
		holder.divider.setBackgroundColor(getResolvedColor(nightTheme ? R.color.route_info_divider_dark : R.color.dashboard_divider_light));
		holder.optionsBtn.setBackgroundDrawable(getRemoveBtnBgSelector());
		holder.optionsBtn.setImageDrawable(getColoredIcon(R.drawable.ic_action_remove_small, R.color.icon_color));
		holder.iconReorder.setVisibility(View.GONE);
		holder.numberText.setVisibility(View.VISIBLE);
		holder.numberText.setText(String.valueOf(position + 1));
		holder.description.setVisibility(View.GONE);

		holder.optionsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int position = holder.getAdapterPosition();
				if (position != RecyclerView.NO_POSITION) {
					mapMarkers.remove(getItem(position));
					notifyDataSetChanged();
				}
			}
		});

		boolean singleItem = getItemCount() == 1;
		boolean fistItem = position == 0;
		boolean lastItem = position == getItemCount() - 1;

		holder.topDivider.setVisibility(fistItem ? View.VISIBLE : View.GONE);
		holder.bottomShadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);
		holder.divider.setVisibility((!singleItem && !lastItem) ? View.VISIBLE : View.GONE);

		holder.title.setText(mapMarker.getName(mapActivity));

		DashLocationFragment.updateLocationView(useCenter, location,
				heading, holder.iconDirection, R.drawable.ic_direction_arrow,
				holder.distance, new LatLon(mapMarker.getLatitude(), mapMarker.getLongitude()),
				screenOrientation, mapActivity.getMyApplication(), mapActivity, true);
	}

	@Override
	public int getItemCount() {
		return mapMarkers.size();
	}

	public boolean isEmpty() {
		return getItemCount() == 0;
	}

	public MapMarker getItem(int position) {
		return mapMarkers.get(position);
	}

	private Drawable getRemoveBtnBgSelector() {
		if (nightTheme) {
			return AndroidUtils.createStateListDrawable(
					getColoredIcon(R.drawable.marker_circle_background_dark_n_with_inset, R.color.keyboard_item_control_dark_bg),
					ContextCompat.getDrawable(mapActivity, R.drawable.marker_circle_background_p_with_inset)
			);
		}
		return ContextCompat.getDrawable(mapActivity, R.drawable.marker_circle_background_light_with_inset);
	}

	private Drawable getColoredIcon(@DrawableRes int resId, @ColorRes int colorResId) {
		return iconsCache.getIcon(resId, colorResId);
	}

	@ColorInt
	private int getResolvedColor(@ColorRes int colorResId) {
		return ContextCompat.getColor(mapActivity, colorResId);
	}
}
