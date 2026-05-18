package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public abstract class MenuTitleController {

	protected int rightIconId;
	protected Drawable rightIcon;
	protected boolean bigRightIcon;
	protected String nameStr = "";
	protected String typeStr = "";
	protected String commonTypeStr = "";
	protected Drawable secondLineTypeIcon;
	protected String streetStr = "";

	private AddressLookupRequest addressLookupRequest;

	protected String searchAddressStr = "";
	protected String addressNotFoundStr = "";

	@Nullable
	public abstract MapActivity getMapActivity();

	public abstract LatLon getLatLon();

	public abstract PointDescription getPointDescription();

	@Nullable
	public abstract Object getObject();

	@Nullable
	public abstract MenuController getMenuController();

	@Nullable
	public OsmandApplication getMyApplication() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			return activity.getMyApplication();
		} else {
			return null;
		}
	}

	public String getTitleStr() {
		if (displayStreetNameInTitle() && searchingAddress()) {
			return searchAddressStr;
		} else {
			return nameStr;
		}
	}

	public boolean searchingAddress() {
		return addressLookupRequest != null;
	}

	public void cancelSearchAddress() {
		OsmandApplication app = getMyApplication();
		if (addressLookupRequest != null && app != null) {
			app.getGeocodingLookupService().cancel(addressLookupRequest);
			addressLookupRequest = null;
			onSearchAddressDone();
		}
	}

	public boolean displayStreetNameInTitle() {
		MenuController menuController = getMenuController();
		return menuController != null && menuController.displayStreetNameInTitle();
	}

	// Has title which does not equal to "Looking up address" and "No address determined"
	public boolean hasValidTitle() {
		String title = getTitleStr();
		return !addressNotFoundStr.equals(title) && !searchAddressStr.equals(title);
	}

	public int getRightIconId() {
		return rightIconId;
	}

	public Drawable getRightIcon() {
		return rightIcon;
	}

	public boolean isBigRightIcon() {
		return bigRightIcon;
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

	public String getStreetStr() {
		if (needStreetName()) {
			if (searchingAddress()) {
				return searchAddressStr;
			} else {
				return streetStr;
			}
		} else {
			return "";
		}
	}

	protected void initTitle() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			searchAddressStr = PointDescription.getSearchAddressStr(mapActivity);
			addressNotFoundStr = PointDescription.getAddressNotFoundStr(mapActivity);

			if (searchingAddress()) {
				cancelSearchAddress();
			}

			acquireIcons();
			acquireNameAndType();
			if (needStreetName()) {
				acquireStreetName();
			}
		}
	}

	public void setNameStr(@Nullable String nameStr) {
		this.nameStr = nameStr != null ? nameStr : "";
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

		rightIconId = 0;
		rightIcon = null;
		bigRightIcon = false;
		secondLineTypeIcon = null;

		if (menuController != null) {
			rightIconId = menuController.getRightIconId();
			rightIcon = menuController.getRightIcon();
			bigRightIcon = menuController.isBigRightIcon();
			secondLineTypeIcon = menuController.getSecondLineTypeIcon();
		}
	}

	protected void acquireNameAndType() {
		String firstNameStr = "";
		typeStr = "";
		commonTypeStr = "";
		streetStr = "";
		setNameStr("");

		MenuController menuController = getMenuController();
		if (menuController != null) {
			firstNameStr = menuController.getFirstNameStr();
			setNameStr(menuController.getNameStr());
			typeStr = menuController.getTypeStr();
			commonTypeStr = menuController.getCommonTypeStr();
		}

		if (Algorithms.isEmpty(nameStr)) {
			setNameStr(typeStr);
			typeStr = commonTypeStr;
		} else if (Algorithms.isEmpty(typeStr)) {
			typeStr = commonTypeStr;
		}

		if (!Algorithms.isEmpty(firstNameStr)) {
			setNameStr(firstNameStr + " (" + nameStr + ")");
		}
	}

	protected void acquireStreetName() {
		addressLookupRequest = new AddressLookupRequest(getLatLon(), new GeocodingLookupService.OnAddressLookupResult() {
			@Override
			public void geocodingDone(String address) {
				MapActivity mapActivity = getMapActivity();
				if (addressLookupRequest != null && mapActivity != null) {
					addressLookupRequest = null;
					if (Algorithms.isEmpty(address)) {
						streetStr = PointDescription.getAddressNotFoundStr(mapActivity);
					} else {
						streetStr = address;
					}

					if (displayStreetNameInTitle()) {
						setNameStr(streetStr);
						getPointDescription().setName(nameStr);
					}
					onSearchAddressDone();
				}
			}
		}, new GeocodingLookupService.OnAddressLookupProgress() {
			@Override
			public void geocodingInProgress() {
				// animate three dots
			}
		});

		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getGeocodingLookupService().lookupAddress(addressLookupRequest);
		}
	}

	protected void onSearchAddressDone() {
	}
}
