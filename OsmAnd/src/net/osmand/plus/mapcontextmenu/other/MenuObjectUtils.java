package net.osmand.plus.mapcontextmenu.other;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.controllers.FavouritePointMenuController;
import net.osmand.plus.myplaces.favorites.FavoriteFolderFormatter;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MenuObjectUtils {

	public static List<MenuObject> createMenuObjectsList(@NonNull MapActivity mapActivity,
	                                                     @NonNull List<RenderedObject> objects,
	                                                     @NonNull LatLon latLon) {
		List<MenuObject> result = new ArrayList<>();
		OsmandApplication app = mapActivity.getApp();
		IContextMenuProvider contextObject = app.getOsmandMap().getMapLayers().getPoiMapLayer();
		for (RenderedObject object : objects) {
			result.add(createMenuObject(object, contextObject, latLon, mapActivity));
		}
		return result;
	}

	@NonNull
	public static MenuObject createMenuObject(@NonNull Object object,
			@Nullable IContextMenuProvider provider, @NonNull LatLon latLon,
			@Nullable MapActivity activity) {
		LatLon ll = null;
		PointDescription pointDescription = null;
		if (provider != null) {
			ll = provider.getObjectLocation(object);
			pointDescription = provider.getObjectName(object);
		}
		if (ll == null) {
			ll = latLon;
		}
		if (pointDescription == null) {
			pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
		}
		return new MenuObject(ll, pointDescription, object, activity);
	}

	@NonNull
	public static String getSecondLineText(MenuObject item) {
		StringBuilder line2Str = new StringBuilder(item.getTypeStr());
		if (item.getObject() instanceof Pair<?, ?> pair) {
			if (pair.first instanceof RouteKey key) {
				OsmandApplication app = item.getMyApplication();
				if (app != null) {
					String routeType = AndroidUtils.getActivityTypeTitle(app, key.type);
					line2Str.append(" - ").append(routeType);
				}
			}
		}
		String streetStr = item.getStreetStr();
		if (!Algorithms.isEmpty(streetStr) && !item.displayStreetNameInTitle()) {
			if (line2Str.length() > 0) {
				line2Str.append(", ");
			}
			line2Str.append(streetStr);
		}
		return line2Str.toString();
	}

	public static void setSecondLineText(@NonNull MenuObject item, @NonNull TextView line2, boolean nightMode) {
		resetSecondLineTextStyle(line2);
		if (item.getMenuController() instanceof FavouritePointMenuController favoriteController) {
			setFavoriteFolderPathText(line2, favoriteController.getFavoriteCategory(), nightMode);
		} else {
			line2.setText(getSecondLineText(item));
		}
	}

	public static void setFavoriteFolderPathText(@NonNull TextView textView,
			@Nullable String fullPath, boolean nightMode) {
		Context context = textView.getContext();
		textView.setSingleLine(true);
		textView.setMaxLines(1);
		textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		Object token = new Object();
		textView.setTag(R.id.context_menu_line2, token);
		textView.setText(FavoriteFolderFormatter.getStyledBreadcrumb(context, fullPath, nightMode));
		textView.post(() -> {
			if (textView.getTag(R.id.context_menu_line2) == token) {
				textView.setText(FavoriteFolderFormatter.getStyledBreadcrumb(context, fullPath, nightMode,
						textView.getPaint(), getTextAvailableWidth(textView)));
			}
		});
	}

	public static void resetSecondLineTextStyle(@NonNull TextView textView) {
		textView.setTag(R.id.context_menu_line2, null);
		textView.setSingleLine(false);
		textView.setMaxLines(Integer.MAX_VALUE);
		textView.setEllipsize(null);
	}

	public static int getTextAvailableWidth(@NonNull TextView textView) {
		int availableWidth = textView.getWidth();
		View parent = textView;
		while (parent.getParent() instanceof View) {
			parent = (View) parent.getParent();
			if (parent.getWidth() > 0) {
				availableWidth = availableWidth > 0 ? Math.min(availableWidth, parent.getWidth()) : parent.getWidth();
			}
		}
		if (availableWidth > 0) {
			availableWidth -= textView.getCompoundPaddingLeft() + textView.getCompoundPaddingRight();
		}
		return Math.max(0, availableWidth);
	}

	@NonNull
	public static String getMenuObjectsNamesByComma(@NonNull List<MenuObject> menuObjects) {
		List<String> names = new ArrayList<>();
		for (MenuObject menuObject : menuObjects) {
			names.add(menuObject.getTitleStr());
		}
		return TextUtils.join(", ", names);
	}
}
