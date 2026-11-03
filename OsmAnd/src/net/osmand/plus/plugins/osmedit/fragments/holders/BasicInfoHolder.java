package net.osmand.plus.plugins.osmedit.fragments.holders;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Location;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GeocodingLookupService.RoadsLookup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.EditPoiListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BasicInfoHolder extends RecyclerView.ViewHolder {

	// other streets this close to the point are offered too, e.g. for a house on a corner
	private static final double ALTERNATIVE_STREET_RADIUS = 25;

	private final OsmandApplication app;
	private final boolean nightMode;
	private final EditText streetEditText;
	private final View streetFromMapButton;
	// the number the button put in, replaced or cleared with the next street; a typed one is kept
	@Nullable
	private String numberFromMap;
	private final EditText houseNumberEditText;
	private final EditText phoneEditText;
	private final EditText webSiteEditText;
	private final EditText descriptionEditText;

	public BasicInfoHolder(@NonNull View itemView, @NonNull OsmandApplication app, boolean nightMode) {
		super(itemView);
		this.app = app;
		this.nightMode = nightMode;
		streetEditText = itemView.findViewById(R.id.streetEditText);
		streetFromMapButton = itemView.findViewById(R.id.streetFromMapButton);
		TooltipCompat.setTooltipText(streetFromMapButton, streetFromMapButton.getContentDescription());
		houseNumberEditText = itemView.findViewById(R.id.houseNumberEditText);
		phoneEditText = itemView.findViewById(R.id.phoneEditText);
		webSiteEditText = itemView.findViewById(R.id.webSiteEditText);
		descriptionEditText = itemView.findViewById(R.id.descriptionEditText);
	}

	protected void addTextWatcher(String tag, EditText e, EditPoiData data, @NonNull EditPoiListener editPoiListener) {
		e.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (data != null && !data.isInEdit()) {
					if (!TextUtils.isEmpty(s)) {
						data.putTag(tag, s.toString());
					} else if (editPoiListener.isBasicTagsInitialized() && editPoiListener.isFragmentResumed()) {
						data.removeTag(tag);
					}
				}
			}
		});
	}

	public void bindView(EditPoiData data, @NonNull EditPoiListener editPoiListener) {
		addTextWatcher(OSMSettings.OSMTagKey.ADDR_STREET.getValue(), streetEditText, data, editPoiListener);
		addTextWatcher(OSMSettings.OSMTagKey.WEBSITE.getValue(), webSiteEditText, data, editPoiListener);
		addTextWatcher(OSMSettings.OSMTagKey.PHONE.getValue(), phoneEditText, data, editPoiListener);
		addTextWatcher(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue(), houseNumberEditText, data, editPoiListener);
		addTextWatcher(OSMSettings.OSMTagKey.DESCRIPTION.getValue(), descriptionEditText, data, editPoiListener);
		InputFilter[] lengthLimit = editPoiListener.getLengthLimit();
		streetEditText.setFilters(lengthLimit);
		houseNumberEditText.setFilters(lengthLimit);
		phoneEditText.setFilters(lengthLimit);
		webSiteEditText.setFilters(lengthLimit);
		descriptionEditText.setFilters(lengthLimit);

		AndroidUtils.setTextHorizontalGravity(streetEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(houseNumberEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(phoneEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(webSiteEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(descriptionEditText, Gravity.START);

		streetFromMapButton.setOnClickListener(v -> fillStreetFromMap(data));

		if (data == null) {
			return;
		}
		Map<String, String> tagValues = data.getTagValues();
		streetEditText.setText(tagValues.get(OSMSettings.OSMTagKey.ADDR_STREET.getValue()));
		houseNumberEditText.setText(tagValues.get(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue()));
		phoneEditText.setText(tagValues.get(OSMSettings.OSMTagKey.PHONE.getValue()));
		webSiteEditText.setText(tagValues.get(OSMSettings.OSMTagKey.WEBSITE.getValue()));
		descriptionEditText.setText(tagValues.get(OSMSettings.OSMTagKey.DESCRIPTION.getValue()));
	}

	private void fillStreetFromMap(@Nullable EditPoiData data) {
		LatLon latLon = data != null ? getLatLon(data.getEntity()) : null;
		if (latLon == null) {
			return;
		}
		Location location = new Location("");
		location.setLatitude(latLon.getLatitude());
		location.setLongitude(latLon.getLongitude());
		streetFromMapButton.setEnabled(false);
		GeocodingLookupService service = app.getGeocodingLookupService();
		service.findRoads(location, RoadsLookup.ADDRESS, false, null, roads -> {
			Map<String, Double> streets = getNearestStreets(roads);
			Map<String, String> buildings = streets.isEmpty()
					? new HashMap<>() : getNearestBuildings(service.findAddresses(roads));
			app.runInUIThread(() -> {
				streetFromMapButton.setEnabled(true);
				onStreetsFound(streets, buildings);
			});
		});
	}

	@Nullable
	private static LatLon getLatLon(@Nullable Entity entity) {
		if (entity == null) {
			return null;
		}
		LatLon latLon = entity.getLatLon();
		return latLon != null ? latLon : new LatLon(entity.getLatitude(), entity.getLongitude());
	}

	// the name tag of the nearest named road, plus other street names close enough to be the one
	// meant; roads come nearest first. A road with only a ref is not a street for addr:street.
	@NonNull
	private static Map<String, Double> getNearestStreets(@Nullable List<GeocodingResult> roads) {
		Map<String, Double> streets = new LinkedHashMap<>();
		if (roads == null) {
			return streets;
		}
		for (GeocodingResult road : roads) {
			RouteDataObject object = road.point != null ? road.point.getRoad() : null;
			String name = object != null ? object.getName() : null;
			if (Algorithms.isEmpty(name) || streets.containsKey(name)) {
				continue;
			}
			double distance = road.getDistance();
			if (!streets.isEmpty() && distance > ALTERNATIVE_STREET_RADIUS) {
				break;
			}
			streets.put(name, distance);
		}
		return streets;
	}

	// the number of the nearest building on every street; addresses come nearest first
	@NonNull
	private static Map<String, String> getNearestBuildings(@NonNull List<GeocodingResult> addresses) {
		Map<String, String> buildings = new HashMap<>();
		for (GeocodingResult address : addresses) {
			RouteDataObject road = address.point != null ? address.point.getRoad() : null;
			String street = road != null ? road.getName() : null;
			String number = address.getBuildingString();
			if (!Algorithms.isEmpty(street) && !Algorithms.isEmpty(number) && !buildings.containsKey(street)) {
				buildings.put(street, number);
			}
		}
		return buildings;
	}

	private void onStreetsFound(@NonNull Map<String, Double> streets, @NonNull Map<String, String> buildings) {
		if (streets.isEmpty()) {
			app.showShortToastMessage(R.string.street_name_not_found_nearby);
		} else if (streets.size() == 1) {
			String street = streets.keySet().iterator().next();
			setAddress(street, buildings.get(street));
		} else {
			List<PopUpMenuItem> items = new ArrayList<>();
			for (Map.Entry<String, Double> street : streets.entrySet()) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitle(street.getKey())
						.setTrailingText(OsmAndFormatter.getFormattedDistance(street.getValue().floatValue(), app))
						.setOnClickListener(item -> setAddress(street.getKey(), buildings.get(street.getKey())))
						.create());
			}
			PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
			displayData.anchorView = streetEditText;
			displayData.menuItems = items;
			displayData.nightMode = nightMode;
			PopUpMenu.show(displayData);
		}
	}

	// the number is a guess from the nearest building on the street, the user corrects it if needed
	private void setAddress(@NonNull String street, @Nullable String number) {
		streetEditText.setText(street);
		streetEditText.setSelection(streetEditText.length());
		String current = houseNumberEditText.getText().toString();
		if (!Algorithms.isEmpty(number)) {
			houseNumberEditText.setText(number);
		} else if (numberFromMap != null && numberFromMap.equals(current)) {
			houseNumberEditText.setText(null);
		}
		numberFromMap = number;
	}
}
