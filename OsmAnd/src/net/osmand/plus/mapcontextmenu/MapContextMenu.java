package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.details.AmenityMenuController;
import net.osmand.plus.mapcontextmenu.details.FavouritePointMenuController;
import net.osmand.plus.mapcontextmenu.details.MenuController;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.util.Algorithms;

public class MapContextMenu {

	private OsmandApplication app;
	private OsmandSettings settings;
	private final MapActivity mapActivity;

	private PointDescription pointDescription;
	private Object object;

	private int leftIconId;
	private Drawable leftIcon;
	private String nameStr;
	private String typeStr;
	private Drawable secondLineIcon;
	private String streetStr;

	private static final String KEY_CTX_MENU_OBJECT = "key_ctx_menu_object";
	private static final String KEY_CTX_MENU_POINT_DESC = "key_ctx_menu_point_desc";
	private static final String KEY_CTX_MENU_NAME_STR = "key_ctx_menu_name_str";
	private static final String KEY_CTX_MENU_TYPE_STR = "key_ctx_menu_type_str";
	private static final String KEY_CTX_MENU_STREET_STR = "key_ctx_menu_street_str";

	public boolean isMenuVisible() {
		return findMenuFragment() != null;
	}

	public PointDescription getPointDescription() {
		return pointDescription;
	}

	public Object getObject() {
		return object;
	}

	public MapContextMenu(OsmandApplication app, MapActivity mapActivity) {
		this.app = app;
		this.mapActivity = mapActivity;
		settings = app.getSettings();
	}

	public boolean init(PointDescription pointDescription, Object object) {
		if (isMenuVisible()) {
			if (this.object == null || !this.object.equals(object)) {
				hide();
			} else {
				return false;
			}
		}

		this.pointDescription = pointDescription;
		this.object = object;
		leftIconId = 0;
		nameStr = null;
		typeStr = null;
		streetStr = null;

		acquireIcons();
		acquireNameAndType();
		if (needStreetName()) {
			acquireStreetName(new LatLon(pointDescription.getLat(), pointDescription.getLon()));
		}
		return true;
	}

	public void show() {
		if (!isMenuVisible()) {
			MapContextMenuFragment.showInstance(mapActivity);
		}
	}

	public void show(PointDescription pointDescription, Object object) {
		if (init(pointDescription, object)) {
			MapContextMenuFragment.showInstance(mapActivity);
		}
	}

	public void hide() {
		MapContextMenuFragment fragment = findMenuFragment();
		if (fragment != null) {
			fragment.dismissMenu();
		}
	}

	private boolean needStreetName() {
		boolean res = object != null || Algorithms.isEmpty(pointDescription.getName());
		if (res) {
			if (object != null) {
				if (object instanceof Amenity) {
					Amenity a = (Amenity) object;
					if (a.getSubType() != null && a.getType() != null) {
						PoiType pt = a.getType().getPoiTypeByKeyName(a.getSubType());
						if (pt != null && pt.getOsmTag() != null && pt.getOsmTag().equals("place")) {
							res = false;
						}
					}
				} else if (object instanceof FavouritePoint) {
					res = false;
				}
			}
		}
		return res;
	}

	public void refreshMenuTitle() {
		MapContextMenuFragment fragment = findMenuFragment();
		if (fragment != null)
			fragment.refreshTitle();
	}

	private MapContextMenuFragment findMenuFragment() {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(MapContextMenuFragment.TAG);
		if (fragment != null) {
			return (MapContextMenuFragment) fragment;
		} else {
			return null;
		}
	}

	public int getLeftIconId() {
		return leftIconId;
	}

	public Drawable getLeftIcon() {
		return leftIcon;
	}

	public Drawable getSecondLineIcon() {
		return secondLineIcon;
	}

	public String getTitleStr() {
		return nameStr;
	}

	public String getLocationStr() {
		if (object != null && object instanceof FavouritePoint) {
			return typeStr;
		} else {
			if (Algorithms.isEmpty(streetStr)) {
				return pointDescription.getLocationName(mapActivity, true).replaceAll("\n", "");
			} else {
				return streetStr;
			}
		}
	}

	private void acquireIcons() {
		leftIconId = 0;
		leftIcon = null;
		secondLineIcon = null;

		if (object != null) {
			if (object instanceof Amenity) {
				String id = null;
				Amenity o = (Amenity) object;
				PoiType st = o.getType().getPoiTypeByKeyName(o.getSubType());
				if (st != null) {
					if (RenderingIcons.containsBigIcon(st.getIconKeyName())) {
						id = st.getIconKeyName();
					} else if (RenderingIcons.containsBigIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
						id = st.getOsmTag() + "_" + st.getOsmValue();
					}
				}
				if (id != null) {
					leftIconId = RenderingIcons.getBigIconResourceId(id);
				}
			} else if (object instanceof FavouritePoint) {
				FavouritePoint fav = (FavouritePoint)object;
				leftIcon = FavoriteImageDrawable.getOrCreate(mapActivity, fav.getColor(), mapActivity.getMapView().getCurrentRotatedTileBox().getDensity());
				secondLineIcon = getIcon(R.drawable.ic_small_group);
			}
		}
	}

	private Drawable getIcon(int iconId) {
		IconsCache iconsCache = app.getIconsCache();
		boolean light = app.getSettings().isLightContent();
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color : R.color.icon_color_light);
	}

	private void acquireNameAndType() {
		if (object != null) {
			if (object instanceof Amenity) {
				Amenity amenity = (Amenity) object;

				PoiCategory pc = amenity.getType();
				PoiType pt = pc.getPoiTypeByKeyName(amenity.getSubType());
				typeStr = amenity.getSubType();
				if (pt != null) {
					typeStr = pt.getTranslation();
				} else if(typeStr != null){
					typeStr = Algorithms.capitalizeFirstLetterAndLowercase(typeStr.replace('_', ' '));
				}
				nameStr = amenity.getName(settings.MAP_PREFERRED_LOCALE.get());
			} else if (object instanceof FavouritePoint) {
				FavouritePoint fav = (FavouritePoint)object;
				nameStr = fav.getName();
				typeStr = fav.getCategory();
			}
		}

		if (Algorithms.isEmpty(nameStr)) {
			nameStr = pointDescription.getName();
		}
		if (Algorithms.isEmpty(typeStr)) {
			typeStr = pointDescription.getTypeName();
		}

		if (Algorithms.isEmpty(nameStr)) {
			if (!Algorithms.isEmpty(typeStr)) {
				nameStr = typeStr;
				typeStr = null;
			} else {
				nameStr = app.getResources().getString(R.string.address_unknown);
			}
		}
	}

	private void acquireStreetName(final LatLon loc) {
		Location ll = new Location("");
		ll.setLatitude(loc.getLatitude());
		ll.setLongitude(loc.getLongitude());
		app.getLocationProvider().getRouteSegment(ll, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object != null) {
					streetStr = RoutingHelper.formatStreetName(object.getName(settings.MAP_PREFERRED_LOCALE.get()),
							object.getRef(), object.getDestinationName(settings.MAP_PREFERRED_LOCALE.get()));
					if (streetStr != null && streetStr.trim().length() == 0) {
						streetStr = null;
					}

					if (streetStr != null) {
						if (getObject() == null) {
							nameStr = streetStr;
							streetStr = null;
						}
						mapActivity.runOnUiThread(new Runnable() {
							public void run() {
								refreshMenuTitle();
							}
						});
					}
				} else {
					streetStr = null;
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

		});
	}

	public MenuController getMenuController(Activity activity) {
		MenuController menuController = null;
		if (object != null) {
			if (object instanceof Amenity) {
				menuController = new AmenityMenuController(app, activity, (Amenity)object);
				if (!Algorithms.isEmpty(typeStr)) {
					menuController.addPlainMenuItem(R.drawable.ic_action_info_dark, typeStr);
				}
				menuController.addPlainMenuItem(R.drawable.map_my_location, pointDescription.getLocationName(activity, true).replaceAll("\n", ""));
			} else if (object instanceof FavouritePoint) {
				menuController = new FavouritePointMenuController(app, activity, (FavouritePoint)object);
				menuController.addPlainMenuItem(R.drawable.map_my_location, pointDescription.getLocationName(activity, true).replaceAll("\n", ""));
			}
		}

		return menuController;
	}

	public void buttonNavigatePressed() {
		mapActivity.getMapActions().showNavigationContextMenuPoint(pointDescription.getLat(), pointDescription.getLon());
	}

	public void buttonFavoritePressed() {
		if (object != null && object instanceof FavouritePoint) {
			mapActivity.getFavoritePointEditor().edit((FavouritePoint)object);
			//mapActivity.getMapActions().editFavoritePoint((FavouritePoint) object);
		} else {
			mapActivity.getFavoritePointEditor().add(pointDescription);
			//mapActivity.getMapActions().addFavouritePoint(pointDescription.getLat(), pointDescription.getLon());
		}
	}

	public void buttonSharePressed() {
		mapActivity.getMapActions().shareLocation(pointDescription.getLat(), pointDescription.getLon());
	}

	public void buttonMorePressed() {
		final ContextMenuAdapter menuAdapter = new ContextMenuAdapter(mapActivity);
		if (object != null) {
			for (OsmandMapLayer layer : mapActivity.getMapView().getLayers()) {
				layer.populateObjectContextMenu(object, menuAdapter);
			}
		}

		mapActivity.getMapActions().contextMenuPoint(pointDescription.getLat(), pointDescription.getLon(), menuAdapter, object);
	}

	public void saveMenuState(Bundle bundle) {
		if (object != null) {
			if (object instanceof Amenity) {
				bundle.putSerializable(KEY_CTX_MENU_OBJECT, (Amenity) object);
			} else if (object instanceof FavouritePoint) {
				bundle.putSerializable(KEY_CTX_MENU_OBJECT, (FavouritePoint) object);
			}
		}
		bundle.putSerializable(KEY_CTX_MENU_POINT_DESC, pointDescription);
		bundle.putSerializable(KEY_CTX_MENU_NAME_STR, nameStr);
		bundle.putSerializable(KEY_CTX_MENU_TYPE_STR, typeStr);
		bundle.putSerializable(KEY_CTX_MENU_STREET_STR, streetStr);
	}

	public void restoreMenuState(Bundle bundle) {
		object = bundle.getSerializable(KEY_CTX_MENU_OBJECT);
		Object pDescObj = bundle.getSerializable(KEY_CTX_MENU_POINT_DESC);
		if (pDescObj != null)
			pointDescription = (PointDescription)pDescObj;
		Object nameStrObj = bundle.getSerializable(KEY_CTX_MENU_NAME_STR);
		if (nameStrObj != null) {
			nameStr = nameStrObj.toString();
		}
		Object typeStrObj = bundle.getSerializable(KEY_CTX_MENU_TYPE_STR);
		if (typeStrObj != null) {
			typeStr = typeStrObj.toString();
		}
		Object streetStrObj = bundle.getSerializable(KEY_CTX_MENU_STREET_STR);
		if (streetStrObj != null) {
			streetStr = streetStrObj.toString();
		}
		acquireIcons();
	}
}
