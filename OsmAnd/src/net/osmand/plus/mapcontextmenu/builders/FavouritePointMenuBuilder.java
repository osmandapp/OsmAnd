package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.plus.myplaces.MyPlacesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.track.fragments.ReadPointDescriptionFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavouritePointMenuBuilder extends MenuBuilder {

	private static final Log LOG = PlatformUtil.getLog(FavouritePointMenuBuilder.class);

	private final FavouritePoint point;
	private final Map<String, String> amenityExtensions = new HashMap<>();

	public FavouritePointMenuBuilder(@NonNull MapActivity activity, @NonNull FavouritePoint point, @Nullable Amenity amenity) {
		super(activity);
		this.point = point;
		setAmenity(amenity);
		setShowNearestWiki(true);
		acquireAmenityExtensions();
	}

	private void acquireAmenityExtensions() {
		AmenityExtensionsHelper helper = new AmenityExtensionsHelper(app);
		if (amenity == null) {
			String originName = point.getAmenityOriginName();
			if (!Algorithms.isEmpty(originName)) {
				amenity = helper.findAmenity(originName, point.getLatitude(), point.getLongitude());
			}
		}
		amenityExtensions.putAll(helper.getUpdatedAmenityExtensions(point.getAmenityExtensions(), amenity));
	}

	@Nullable
	public Amenity getAmenity() {
		return amenity;
	}

	@Override
	protected void buildNearestRow(View view, List<Amenity> nearestAmenities, int iconId,
			String text, String amenityKey) {
		if (amenity == null) {
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
		boolean light = isLightContent();
		buildDateRow(view, point.getTimestamp());
		buildCommentRow(view, point.getComment());

		if (!Algorithms.isEmpty(amenityExtensions)) {
			AdditionalInfoBundle bundle = new AdditionalInfoBundle(app, amenityExtensions);
			AmenityUIHelper helper = new AmenityUIHelper(mapActivity, getPreferredMapAppLang(), bundle);
			helper.setLight(light);
			helper.setLatLon(getLatLon());
			helper.setCollapseExpandListener(getCollapseExpandListener());
			helper.buildInternal(view);
		}
	}

	@Override
	protected void buildDescription(View view) {
		String desc = point.getDescription();
		if (!Algorithms.isEmpty(desc)) {
			buildDescriptionRow(view, desc);
		}
	}

	@Override
	protected void showDescriptionDialog(@NonNull Context ctx, @NonNull String description,
			@NonNull String title) {
		ReadPointDescriptionFragment.showInstance(mapActivity, description);
	}

	private void buildGroupFavouritesView(@NonNull View view) {
		boolean light = isLightContent();
		FavoriteGroup favoriteGroup = app.getFavoritesHelper().getGroup(point);
		if (favoriteGroup != null && !Algorithms.isEmpty(favoriteGroup.getPoints())) {
			int color = favoriteGroup.getColor() == 0 ? getColor(R.color.color_favorite) : favoriteGroup.getColor();
			int disabledColor = ColorUtilities.getSecondaryTextColorId(!light);
			color = favoriteGroup.isVisible() ? (color | 0xff000000) : getColor(disabledColor);
			String name = view.getContext().getString(R.string.context_menu_points_of_group);
			buildRow(view, app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder, color), null, name, 0, null,
					true, getCollapsableFavouritesView(view.getContext(), true, favoriteGroup, point),
					false, 0, false, null, false);
		}
	}

	private CollapsableView getCollapsableFavouritesView(Context context, boolean collapsed,
			@NonNull FavoriteGroup group, FavouritePoint selectedPoint) {
		LinearLayout view = buildCollapsableContentView(context, collapsed, true);

		List<FavouritePoint> points = group.getPoints();
		for (int i = 0; i < points.size() && i < 10; i++) {
			FavouritePoint point = points.get(i);
			boolean selected = selectedPoint != null && selectedPoint.equals(point);
			TextViewEx button = buildButtonInCollapsableView(context, selected, false);
			String name = point.getDisplayName(context);
			button.setText(name);

			if (!selected) {
				button.setOnClickListener(v -> {
					LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
					PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getDisplayName(context));
					mapActivity.getContextMenu().show(latLon, pointDescription, point);
				});
			}
			view.addView(button);
		}

		if (points.size() > 10) {
			TextViewEx button = buildButtonInCollapsableView(context, false, true);
			button.setText(context.getString(R.string.shared_string_show_all));
			button.setOnClickListener(v -> openFavoritesGroup(context, group.getName()));
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}

	private void openFavoritesGroup(Context context, String groupName) {
		OsmAndAppCustomization customization = ((OsmandApplication) context.getApplicationContext()).getAppCustomization();
		Intent intent = new Intent(context, customization.getMyPlacesActivity());
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, FAV_TAB);
		bundle.putString(FragmentStateHolder.GROUP_NAME_TO_SHOW, groupName);
		intent.putExtra(MapActivity.INTENT_PARAMS, bundle);
		AndroidUtils.startActivityIfSafe(context, intent);
	}
}