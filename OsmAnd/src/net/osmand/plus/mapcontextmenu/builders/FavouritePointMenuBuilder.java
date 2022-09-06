package net.osmand.plus.mapcontextmenu.builders;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.myplaces.ui.FavoritesActivity;
import net.osmand.plus.track.fragments.ReadPointDescriptionFragment;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Map;

public class FavouritePointMenuBuilder extends MenuBuilder {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(FavouritePointMenuBuilder.class);

	private final FavouritePoint fav;
	private Object originObject;

	public FavouritePointMenuBuilder(@NonNull MapActivity mapActivity, @NonNull FavouritePoint fav) {
		super(mapActivity);
		this.fav = fav;
		setShowNearestWiki(true);
		acquireOriginObject();
	}

	private void acquireOriginObject() {
		originObject = collectUpdatedPointInfo(fav.getExtensions(), fav.getAmenityOriginName(),
				fav.getLatitude(), fav.getLongitude());
	}

	public Object getOriginObject() {
		return originObject;
	}

	@Override
	protected void buildNearestRow(View view, List<Amenity> nearestAmenities, int iconId, String text, String amenityKey) {
		if (originObject == null || !(originObject instanceof Amenity)) {
			super.buildNearestRow(view, nearestAmenities, iconId, text, amenityKey);
		}
	}

	@Override
	protected void buildTopInternal(View view) {
		super.buildTopInternal(view);
		buildGroupFavouritesView(view);
	}

	@Override
	public void buildInternal(View view) {
		if (fav != null && fav.getTimestamp() != 0) {
			buildDateRow(view, fav.getTimestamp());
		}
		if (!Algorithms.isEmpty(fav.getComment())) {
			View rowc = buildRow(view, R.drawable.ic_action_note_dark, null, fav.getComment(), 0,
					false, null, true, 10, false, null, false);
			rowc.setOnClickListener(v -> POIMapLayer.showPlainDescriptionDialog(rowc.getContext(),
					app, fav.getComment(), rowc.getResources().getString(R.string.poi_dialog_comment)));
		}
		if (originObject != null) {
			Map<String, String> additionalInfo = null;
			if (originObject instanceof Map) {
				additionalInfo = (Map<String, String>)originObject;
			}
			AmenityUIHelper helper = new AmenityUIHelper(this.mapActivity, getPreferredMapAppLang(), additionalInfo);
			helper.setLight(light);
			helper.setLatLon(getLatLon());
			helper.buildInternal(view);
		}
	}

	@Override
	protected void buildDescription(View view) {
		String desc = fav.getDescription();
		if (!Algorithms.isEmpty(desc)) {
			buildDescriptionRow(view, desc);
		}
	}

	@Override
	protected void showDescriptionDialog(@NonNull Context ctx, @NonNull String description, @NonNull String title) {
		ReadPointDescriptionFragment.showInstance(mapActivity, description);
	}

	private void buildGroupFavouritesView(View view) {
		FavoriteGroup favoriteGroup = app.getFavoritesHelper().getGroup(fav);
		if (favoriteGroup != null && !Algorithms.isEmpty(favoriteGroup.getPoints())) {
			int color = favoriteGroup.getColor() == 0 ? getColor(R.color.color_favorite) : favoriteGroup.getColor();
			int disabledColor = ColorUtilities.getSecondaryTextColorId(!light);
			color = favoriteGroup.isVisible() ? (color | 0xff000000) : getColor(disabledColor);
			String name = view.getContext().getString(R.string.context_menu_points_of_group);
			buildRow(view, app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder, color), null, name, 0, null,
					true, getCollapsableFavouritesView(view.getContext(), true, favoriteGroup, fav),
					false, 0, false, null, false);
		}
	}

	private CollapsableView getCollapsableFavouritesView(Context context, boolean collapsed, @NonNull FavoriteGroup group, FavouritePoint selectedPoint) {
		LinearLayout view = buildCollapsableContentView(context, collapsed, true);

		List<FavouritePoint> points = group.getPoints();
		for (int i = 0; i < points.size() && i < 10; i++) {
			FavouritePoint point = points.get(i);
			boolean selected = selectedPoint != null && selectedPoint.equals(point);
			TextViewEx button = buildButtonInCollapsableView(context, selected, false);
			String name = point.getDisplayName(context);
			button.setText(name);

			if (!selected) {
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getDisplayName(context));
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
					FavoritesActivity.openFavoritesGroup(context, group.getName());
				}
			});
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}
}