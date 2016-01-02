package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public abstract class MenuTitleController {

	protected int leftIconId;
	protected Drawable leftIcon;
	protected String nameStr = "";
	protected String typeStr = "";
	protected String commonTypeStr = "";
	protected Drawable secondLineTypeIcon;
	protected String streetStr = "";
	protected String addressNotKnownStr;
	protected boolean searchingAddress;
	protected boolean cancelSearch;

	public abstract MapActivity getMapActivity();

	public abstract LatLon getLatLon();

	public abstract PointDescription getPointDescription();

	public abstract Object getObject();

	public abstract MenuController getMenuController();

	public String getTitleStr() {
		//if (Algorithms.isEmpty(nameStr) && searchingAddress) {
		// searchingAddress did not work here once search was interrupted by a new search
		if (Algorithms.isEmpty(nameStr) && needStreetName()) {
			return addressNotKnownStr;
		} else {
			return nameStr;
		}
	}

	public int getLeftIconId() {
		return leftIconId;
	}

	public Drawable getLeftIcon() {
		return leftIcon;
	}

	public Drawable getTypeIcon() {
		return secondLineTypeIcon;
	}

	public String getTypeStr() {
		MenuController menuController = getMenuController();
		if (menuController != null && menuController.needTypeStr()) {
			return typeStr;
		} else {
			return "";
		}
	}

	public String getCommonTypeStr() {
		return commonTypeStr;
	}

	public String getStreetStr() {
		MenuController menuController = getMenuController();
		if (menuController != null && menuController.needStreetName()) {
			// Display "Looking up address..." status
			if (searchingAddress) {
				return addressNotKnownStr;
			} else {
				return streetStr;
			}
		} else {
			return "";
		}
	}

	protected void initTitle() {
		addressNotKnownStr = getMapActivity().getString(R.string.looking_up_address) + getMapActivity().getString(R.string.shared_string_ellipsis);
		acquireIcons();
		acquireNameAndType();
		if (needStreetName()) {
			acquireStreetName();
		}
	}

	protected boolean needStreetName() {
		MenuController menuController = getMenuController();
		boolean res = getObject() != null || Algorithms.isEmpty(getPointDescription().getName());
		if (res && menuController != null) {
			res = menuController.needStreetName();
		}
		return res;
	}

	protected void acquireIcons() {
		MenuController menuController = getMenuController();

		leftIconId = 0;
		leftIcon = null;
		secondLineTypeIcon = null;

		if (menuController != null) {
			leftIconId = menuController.getLeftIconId();
			leftIcon = menuController.getLeftIcon();
			secondLineTypeIcon = menuController.getSecondLineTypeIcon();
		}
	}

	protected void acquireNameAndType() {
		nameStr = "";
		typeStr = "";
		commonTypeStr = "";
		streetStr = "";

		MenuController menuController = getMenuController();
		if (menuController != null) {
			nameStr = menuController.getNameStr();
			typeStr = menuController.getTypeStr();
			commonTypeStr = menuController.getCommonTypeStr();
		}

		if (Algorithms.isEmpty(nameStr)) {
			nameStr = typeStr;
			typeStr = commonTypeStr;
		} else if (Algorithms.isEmpty(typeStr)) {
			typeStr = commonTypeStr;
		}
	}

	protected void acquireStreetName() {
		if (searchingAddress) {
			cancelSearch = true;
			getMapActivity().getMyApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					acquireStreetName();
				}
			}, 50);
			return;
		}

		searchingAddress = true;
		cancelSearch = false;
		Location ll = new Location("");
		ll.setLatitude(getLatLon().getLatitude());
		ll.setLongitude(getLatLon().getLongitude());
		getMapActivity().getMyApplication().getLocationProvider()
				.getGeocodingResult(ll, new ResultMatcher<GeocodingResult>() {

					@Override
					public boolean publish(GeocodingResult object) {
						if (object != null) {
							OsmandSettings settings = getMapActivity().getMyApplication().getSettings();
							String lang = settings.MAP_PREFERRED_LOCALE.get();
							String geocodingResult = "";
							if (object.building != null) {
								String bldName = object.building.getName(lang);
								if (!Algorithms.isEmpty(object.buildingInterpolation)) {
									bldName = object.buildingInterpolation;
								}
								geocodingResult = object.street.getName(lang) + " " + bldName + ", "
										+ object.city.getName(lang);
							} else if (object.street != null) {
								geocodingResult = object.street.getName(lang) + ", " + object.city.getName(lang);
							} else if (object.city != null) {
								geocodingResult = object.city.getName(lang);
							} else if (object.point != null) {
								RouteDataObject rd = object.point.getRoad();
								String sname = rd.getName(lang);
								if (Algorithms.isEmpty(sname)) {
									sname = "";
								}
								String ref = rd.getRef();
								if (!Algorithms.isEmpty(ref)) {
									if (!Algorithms.isEmpty(sname)) {
										sname += ", ";
									}
									sname += ref;
								}
								geocodingResult = sname;
							}

							streetStr = geocodingResult;

							if (!Algorithms.isEmpty(streetStr) && object.getDistance() > 100) {
								streetStr = getMapActivity().getString(R.string.shared_string_near) + " " + streetStr;
							} else if (Algorithms.isEmpty(streetStr)) {
								streetStr = getMapActivity().getString(R.string.no_address_found);
							}

							MenuController menuController = getMenuController();
							if (menuController == null || menuController.displayStreetNameInTitle()) {
								nameStr = streetStr;
								getPointDescription().setName(nameStr);
							}
						}

						searchingAddress = false;
						getMapActivity().runOnUiThread(new Runnable() {
							public void run() {
								onSearchAddressDone();
							}
						});
						return true;
					}

					@Override
					public boolean isCancelled() {
						return cancelSearch;
					}

				});
	}

	protected void onSearchAddressDone() {
	}

}
