package net.osmand.core.samples.android.sample1.search;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.core.samples.android.sample1.R;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.SampleFormatter;
import net.osmand.core.samples.android.sample1.view.DirectionDrawable;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.search.core.SearchPhrase;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class QuickSearchListAdapter extends ArrayAdapter<QuickSearchListItem> {

	private SampleApplication app;
	private Activity activity;

	private LatLon location;
	private Float heading;
	private boolean useMapCenter;

	private int searchMoreItemPosition;

	private int screenOrientation;
	private int dp56;
	private int dp1;

	private static final int ITEM_TYPE_REGULAR = 0;
	private static final int ITEM_TYPE_SEARCH_MORE = 1;

	public interface OnSelectionListener {

		void onUpdateSelectionMode(List<QuickSearchListItem> selectedItems);

		void reloadData();
	}

	public QuickSearchListAdapter(SampleApplication app, Activity activity) {
		super(app, R.layout.search_list_item);
		this.app = app;
		this.activity = activity;
		dp56 = AndroidUtils.dpToPx(app, 56f);
		dp1 = AndroidUtils.dpToPx(app, 1f);
	}

	public int getScreenOrientation() {
		return screenOrientation;
	}

	public void setScreenOrientation(int screenOrientation) {
		this.screenOrientation = screenOrientation;
	}

	public LatLon getLocation() {
		return location;
	}

	public void setLocation(LatLon location) {
		this.location = location;
	}

	public Float getHeading() {
		return heading;
	}

	public void setHeading(Float heading) {
		this.heading = heading;
	}

	public boolean isUseMapCenter() {
		return useMapCenter;
	}

	public void setUseMapCenter(boolean useMapCenter) {
		this.useMapCenter = useMapCenter;
	}

	public void setListItems(List<QuickSearchListItem> items) {
		setNotifyOnChange(false);
		clear();
		for (QuickSearchListItem item : items) {
			add(item);
		}
		acquireAdditionalItemsPositions();
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	public void addListItem(QuickSearchListItem item) {
		if (searchMoreItemPosition != -1 && item instanceof QuickSearchMoreListItem) {
			return;
		}
		setNotifyOnChange(false);
		add(item);
		acquireAdditionalItemsPositions();
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	public void insertListItem(QuickSearchListItem item, int index) {
		setNotifyOnChange(false);
		insert(item, index);
		acquireAdditionalItemsPositions();
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	private void acquireAdditionalItemsPositions() {
		searchMoreItemPosition = -1;
		if (getCount() > 0) {
			QuickSearchListItem first = getItem(0);
			QuickSearchListItem last = getItem(getCount() - 1);
			searchMoreItemPosition = last instanceof QuickSearchMoreListItem ? getCount() - 1 : -1;
		}
	}

	@Override
	public QuickSearchListItem getItem(int position) {
		return super.getItem(position);
	}

	@Override
	public int getItemViewType(int position) {
		if (position == searchMoreItemPosition) {
			return ITEM_TYPE_SEARCH_MORE;
		} else {
			return ITEM_TYPE_REGULAR;
		}
	}

	@Override
	public int getViewTypeCount() {
		return 4;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final QuickSearchListItem listItem = getItem(position);
		int viewType = getItemViewType(position);
		LinearLayout view;
		if (viewType == ITEM_TYPE_SEARCH_MORE) {
			if (convertView == null) {
				LayoutInflater inflater = activity.getLayoutInflater();
				view = (LinearLayout) inflater.inflate(R.layout.search_more_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}

			((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
		} else {
			if (convertView == null) {
				LayoutInflater inflater = activity.getLayoutInflater();
				view = (LinearLayout) inflater.inflate(R.layout.search_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}

			ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView subtitle = (TextView) view.findViewById(R.id.subtitle);

			imageView.setImageDrawable(listItem.getIcon());
			String name = listItem.getName();
			title.setText(name);

			String desc = listItem.getTypeName();
			boolean hasDesc = false;
			if (!Algorithms.isEmpty(desc) && !desc.equals(name)) {
				subtitle.setText(desc);
				subtitle.setVisibility(View.VISIBLE);
				hasDesc = true;
			} else {
				subtitle.setVisibility(View.GONE);
			}

			view.findViewById(R.id.type_name_icon).setVisibility(View.GONE);

			TextView timeText = (TextView) view.findViewById(R.id.time);
			ImageView timeIcon = (ImageView) view.findViewById(R.id.time_icon);
			if (listItem.getSearchResult().object instanceof Amenity
					&& ((Amenity) listItem.getSearchResult().object).getOpeningHours() != null) {
				Amenity amenity = (Amenity) listItem.getSearchResult().object;
				OpeningHoursParser.OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				if (rs != null) {
					Calendar inst = Calendar.getInstance();
					inst.setTimeInMillis(System.currentTimeMillis());
					boolean worksNow = rs.isOpenedForTime(inst);
					inst.setTimeInMillis(System.currentTimeMillis() + 30 * 60 * 1000); // 30 minutes later
					boolean worksLater = rs.isOpenedForTime(inst);
					int colorId = worksNow ? worksLater ? R.color.color_ok : R.color.color_intermediate : R.color.color_warning;

					timeIcon.setVisibility(View.VISIBLE);
					timeText.setVisibility(View.VISIBLE);
					timeIcon.setImageDrawable(app.getIconsCache().getIcon("ic_action_time_16", colorId));
					timeText.setTextColor(app.getResources().getColor(colorId));
					String rt = rs.getCurrentRuleTime(inst);
					timeText.setText(rt == null ? "" : rt);
				} else {
					timeIcon.setVisibility(View.GONE);
					timeText.setVisibility(View.GONE);
				}
			} else {
				timeIcon.setVisibility(View.GONE);
				timeText.setVisibility(View.GONE);
			}

			updateCompassVisibility(view, listItem);
		}
		view.setBackgroundColor(app.getResources().getColor(R.color.bg_color_light));
		View divider = view.findViewById(R.id.divider);
		if (divider != null) {
			if (position == getCount() - 1) {
				divider.setVisibility(View.GONE);
			} else {
				divider.setVisibility(View.VISIBLE);
				if (position + 1 == searchMoreItemPosition) {
					LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp1);
					p.setMargins(0, 0, 0 ,0);
					divider.setLayoutParams(p);
				} else {
					LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp1);
					p.setMargins(dp56, 0, 0 ,0);
					divider.setLayoutParams(p);
				}
			}
		}
		return view;
	}

	private void updateCompassVisibility(View view, QuickSearchListItem listItem) {
		View compassView = view.findViewById(R.id.compass_layout);
		Location ll = app.getLocationProvider().getLastKnownLocation();
		boolean showCompass = location != null && listItem.getSearchResult().location != null;
		boolean gpsFixed = ll != null && System.currentTimeMillis() - ll.getTime() < 1000 * 60 * 60 * 20;
		if ((gpsFixed || useMapCenter) && showCompass) {
			updateDistanceDirection(view, listItem);
			compassView.setVisibility(View.VISIBLE);
		} else {
			if (!showCompass) {
				compassView.setVisibility(View.GONE);
			} else {
				compassView.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateDistanceDirection(View view, QuickSearchListItem listItem) {
		TextView distanceText = (TextView) view.findViewById(R.id.distance);
		ImageView direction = (ImageView) view.findViewById(R.id.direction);
		SearchPhrase phrase = listItem.getSearchResult().requiredSearchPhrase;
		LatLon loc = location;
		if(phrase != null && useMapCenter) {
			LatLon ol = phrase.getSettings().getOriginalLocation();
			if(ol != null) {
				loc = ol;
			}
		}
		updateLocationView(useMapCenter, loc,
				heading, direction, distanceText,
				listItem.getSearchResult().location.getLatitude(),
				listItem.getSearchResult().location.getLongitude(),
				screenOrientation, app, activity);
	}

	public static void updateLocationView(boolean useCenter, LatLon fromLoc, Float h,
										  ImageView arrow, TextView txt, double toLat, double toLon,
										  int screenOrientation, SampleApplication app, Context ctx) {
		updateLocationView(useCenter, fromLoc, h, arrow, 0, txt, new LatLon(toLat, toLon), screenOrientation, app, ctx, true);
	}

	public static void updateLocationView(boolean useCenter, LatLon fromLoc, Float h,
										  ImageView arrow, int arrowResId, TextView txt, LatLon toLoc,
										  int screenOrientation, SampleApplication app, Context ctx, boolean paint) {
		float[] mes = new float[2];
		if (fromLoc != null && toLoc != null) {
			Location.distanceBetween(toLoc.getLatitude(), toLoc.getLongitude(), fromLoc.getLatitude(), fromLoc.getLongitude(), mes);
		}
		if (arrow != null) {
			boolean newImage = false;
			if (arrowResId == 0) {
				arrowResId = R.drawable.ic_direction_arrow;
			}
			DirectionDrawable dd;
			if(!(arrow.getDrawable() instanceof DirectionDrawable)) {
				newImage = true;
				dd = new DirectionDrawable(ctx, arrow.getWidth(), arrow.getHeight());
			} else {
				dd = (DirectionDrawable) arrow.getDrawable();
			}
			dd.setImage(arrowResId, useCenter ? R.color.color_distance : R.color.color_myloc_distance);
			if (fromLoc == null || h == null || toLoc == null) {
				dd.setAngle(0);
			} else {
				dd.setAngle(mes[1] - h + 180 + screenOrientation);
			}
			if (newImage) {
				arrow.setImageDrawable(dd);
			}
			arrow.invalidate();
		}
		if (txt != null) {
			if (fromLoc != null && toLoc != null) {
				if (paint) {
					txt.setTextColor(app.getResources().getColor(
							useCenter ? R.color.color_distance : R.color.color_myloc_distance));
				}
				txt.setText(SampleFormatter.getFormattedDistance(mes[0], app));
			} else {
				txt.setText("");
			}
		}
	}
}
