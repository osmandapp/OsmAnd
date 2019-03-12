package net.osmand.plus.mapcontextmenu.builders;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class FavouritePointMenuBuilder extends MenuBuilder {

	private final FavouritePoint fav;
	private Object originObject;

	public FavouritePointMenuBuilder(@NonNull MapActivity mapActivity, final @NonNull FavouritePoint fav) {
		super(mapActivity);
		this.fav = fav;
		setShowNearestWiki(true);
		acquireOriginObject();
	}

	private void acquireOriginObject() {
		String originObjectName = fav.getOriginObjectName();
		if (originObjectName.length() > 0) {
			if (originObjectName.startsWith(Amenity.class.getSimpleName())) {
				originObject = findAmenity(originObjectName, fav.getLatitude(), fav.getLongitude());
			} else if (originObjectName.startsWith(TransportStop.class.getSimpleName())) {
				originObject = findTransportStop(originObjectName, fav.getLatitude(), fav.getLongitude());
			}
		}
	}

	public Object getOriginObject() {
		return originObject;
	}

	@Override
	protected void buildNearestWikiRow(View view) {
		if (originObject == null || !(originObject instanceof Amenity)) {
			super.buildNearestWikiRow(view);
		}
	}

	@Override
	protected void buildTopInternal(View view) {
		buildDescription(view);
		super.buildTopInternal(view);
		buildGroupFavouritesView(view);
	}

	@Override
	public void buildInternal(View view) {
		if (originObject != null && originObject instanceof Amenity) {
			AmenityMenuBuilder builder = new AmenityMenuBuilder(mapActivity, (Amenity) originObject);
			builder.setLatLon(getLatLon());
			builder.setLight(light);
			builder.buildInternal(view);
		}
	}

	private void buildGroupFavouritesView(View view) {
		FavoriteGroup favoriteGroup = app.getFavorites().getGroup(fav);
		List<FavouritePoint> groupFavourites = favoriteGroup.points;
		if (groupFavourites.size() > 0) {
			int color = favoriteGroup.color == 0 || favoriteGroup.color == Color.BLACK ? view.getResources().getColor(R.color.color_favorite) : favoriteGroup.color;
			int disabledColor = light ? R.color.secondary_text_light : R.color.secondary_text_dark;
			color = favoriteGroup.visible ? (color | 0xff000000) : view.getResources().getColor(disabledColor);
			String name = view.getContext().getString(R.string.context_menu_points_of_group);
			buildRow(view, app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder, color), null, name, 0, null,
					true, getCollapsableFavouritesView(view.getContext(), true, favoriteGroup, fav),
					false, 0, false, null, false);
		}
	}

	private void buildDescriptionRow(final View view, final String description, int textColor,
									 int textLinesLimit, boolean matchWidthDivider) {
		if (!isFirstRow()) {
			buildRowDivider(view);
		}
		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llBaseViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		baseView.setLayoutParams(llBaseViewParams);

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		ll.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(description, view.getContext());
				return true;
			}
		});

		baseView.addView(ll);

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textPrefixView = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(dpToPx(16f), dpToPx(8f), 0, 0);
		textPrefixView.setLayoutParams(llTextParams);
		textPrefixView.setTextSize(12);
		textPrefixView.setTextColor(app.getResources().getColor(R.color.ctx_menu_buttons_text_color));
		textPrefixView.setEllipsize(TextUtils.TruncateAt.END);
		textPrefixView.setMinLines(1);
		textPrefixView.setMaxLines(1);
		textPrefixView.setText(app.getResources().getString(R.string.shared_string_description));
		llText.addView(textPrefixView);

		TextView textView = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams2.setMargins(dpToPx(16f), dpToPx(2f), 0, dpToPx(8f));
		textView.setLayoutParams(llTextParams2);
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_bottom_view_text_color_light : R.color.ctx_menu_bottom_view_text_color_dark));
		textView.setText(description);

		textView.setEllipsize(TextUtils.TruncateAt.END);
		if (textLinesLimit > 0) {
			textView.setMinLines(1);
			textView.setMaxLines(textLinesLimit);
		}
		if (textColor > 0) {
			textView.setTextColor(view.getResources().getColor(textColor));
		}
		llText.addView(textView);

		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.weight = 1f;
		llTextViewParams.setMargins(0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);

		((LinearLayout) view).addView(baseView);
		rowBuilt();
		setDividerWidth(matchWidthDivider);
	}

	private void buildDescription(View view) {
		String desc = fav.getDescription();
		if (!Algorithms.isEmpty(desc)) {
			buildDescriptionRow(view, desc, 0, 0, true);
		}
	}

	private Amenity findAmenity(String nameStringEn, double lat, double lon) {
		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 15);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(
				new BinaryMapIndexReader.SearchPoiTypeFilter() {
					@Override
					public boolean accept(PoiCategory type, String subcategory) {
						return true;
					}

					@Override
					public boolean isEmpty() {
						return false;
					}
				}, rect.top, rect.left, rect.bottom, rect.right, -1, null);

		for (Amenity amenity : amenities) {
			String stringEn = amenity.toStringEn();
			if (stringEn.equals(nameStringEn)) {
				return amenity;
			}
		}
		return null;
	}

	private TransportStop findTransportStop(String nameStringEn, double lat, double lon) {

		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 15);
		List<TransportStop> res = app.getResourceManager().searchTransportSync(rect.top, rect.left,
				rect.bottom, rect.right, new ResultMatcher<TransportStop>() {

					@Override
					public boolean publish(TransportStop object) {
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});

		for (TransportStop stop : res) {
			String stringEn = stop.toStringEn();
			if (stringEn.equals(nameStringEn)) {
				return stop;
			}
		}
		return null;
	}

	private CollapsableView getCollapsableFavouritesView(final Context context, boolean collapsed, @NonNull final FavoriteGroup group, FavouritePoint selectedPoint) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, collapsed, true);

		List<FavouritePoint> points = group.points;
		for (int i = 0; i < points.size() && i < 10; i++) {
			final FavouritePoint point = points.get(i);
			boolean selected = selectedPoint != null && selectedPoint.equals(point);
			TextViewEx button = buildButtonInCollapsableView(context, selected, false);
			String name = point.getName();
			button.setText(name);

			if (!selected) {
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getName());
						mapActivity.getContextMenu().show(latLon, pointDescription, point);
					}
				});
			}
			view.addView(button);
		}

		if (points.size() > 10) {
			TextViewEx button = buildButtonInCollapsableView(context, false, true);
			button.setText(context.getString(R.string.shared_string_show_all));
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					OsmAndAppCustomization appCustomization = app.getAppCustomization();
					final Intent intent = new Intent(context, appCustomization.getFavoritesActivity());
					intent.putExtra(FavoritesActivity.OPEN_FAVOURITES_TAB, true);
					intent.putExtra(FavoritesActivity.GROUP_NAME_TO_SHOW, group.name);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					context.startActivity(intent);
				}
			});
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}
}