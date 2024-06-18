package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.utils.OsmAndFormatter.FORMAT_MINUTES;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.LocationConvert;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

import java.util.Iterator;
import java.util.SortedSet;

public class AisObjectMenuController extends MenuController {
    private AisObject aisObject;
    private final OsmandApplication app;
    public AisObjectMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription,
                                   AisObject aisObject) {
        super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
        this.aisObject = aisObject;
        this.app = builder.getApplication();
        builder.setShowTitleIfTruncated(false);
        builder.setShowNearestPoi(false);
        builder.setShowOnlinePhotos(false);
        builder.setShowNearestWiki(false);
        // TODO: show an icon in the menu
    }

    private void addMenuItem(@NonNull String type, @Nullable String value) {
        if (value != null) {
            if (!value.isEmpty()) {
                addPlainMenuItem(0, value, type, false, false, null);
            }
        }
    }
    private void addMenuItem(@NonNull String type, @Nullable String value,
                             @Nullable SortedSet<Integer> msgTypes, Integer selection[]) {
        if (msgTypes != null) {
            for (Integer i : selection) {
                if (msgTypes.contains(i)) {
                    addMenuItem(type, value);
                    break;
                }
            }
        }
    }
    private void addMenuItemDimension() {
        if (((aisObject.getDimensionToBow() != AisObjectConstants.INVALID_DIMENSION) ||
                (aisObject.getDimensionToStern() != AisObjectConstants.INVALID_DIMENSION)) &&
                ((aisObject.getDimensionToPort() != AisObjectConstants.INVALID_DIMENSION) ||
                (aisObject.getDimensionToStarboard() != AisObjectConstants.INVALID_DIMENSION))) {
            addMenuItem("Dimension",
                    Integer.toString(aisObject.getDimensionToBow() + aisObject.getDimensionToStern()) +
                            "m x " +
                            Integer.toString(aisObject.getDimensionToPort() + aisObject.getDimensionToStarboard()) +
                            "m");
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
        SortedSet<Integer> msgTypes = aisObject.getMsgTypes();
        Iterator<Integer> iter = msgTypes.iterator();
        String msgTypesString = "";
        LatLon position = aisObject.getPosition();
        long lastUpdate = (System.currentTimeMillis() - aisObject.getLastUpdate()) / 1000;

        addMenuItem("MMSI",  Integer.toString(aisObject.getMmsi()));
        if (position != null) {
            addMenuItem("Location",
                    LocationConvert.convertLatitude(position.getLatitude(), FORMAT_MINUTES, true) +
                          ", " + LocationConvert.convertLongitude(position.getLongitude(), FORMAT_MINUTES, true) );
            if (this.app != null) {
                float distance = aisObject.getDistanceInNauticalMiles(app.getLocationProvider());
                float bearing = aisObject.getBearing(app.getLocationProvider());
                if (distance >= 0.0f) {
                    try {
                        addMenuItem("Distance",  String.format("%.1f nm", distance));
                    } catch (Exception ignore) { }
                }
                if (bearing >= 0.0f) {
                    try {
                        addMenuItem("Bearing", String.format("%.1f", bearing));
                    } catch (Exception ignore) { }
                }
            }
        }
        if (msgTypes.contains(21)) { // ATON (aid to navigation)
            addMenuItem("ATON Type",  aisObject.getAidTypeString());
            addMenuItemDimension();
        } else if (msgTypes.contains(9)) { // SAR aircraft
            addMenuItem("Object Type", "SAR Aircraft");
            if (aisObject.getCog() != AisObjectConstants.INVALID_COG) {
                addMenuItem("COG", String.valueOf(aisObject.getCog()));
            }
            if (aisObject.getSog() != AisObjectConstants.INVALID_SOG) {
                addMenuItem("SOG", String.valueOf(aisObject.getSog()) + " kt");
            }
            if (aisObject.getAltitude() != AisObjectConstants.INVALID_ALTITUDE) {
                addMenuItem("Altitude", String.valueOf(aisObject.getAltitude()) + " m");
            }
        } else {
            addMenuItem("Callsign", aisObject.getCallSign());
            if (aisObject.getImo() != 0 ) {
                addMenuItem("IMO", Integer.toString(aisObject.getImo()), msgTypes, new Integer[]{5});
            }
            addMenuItem("Shipname", aisObject.getShipName());
            addMenuItem("Shiptype", aisObject.getShipTypeString(), msgTypes, new Integer[]{5, 19, 24});
            if (aisObject.getNavStatus() != AisObjectConstants.INVALID_NAV_STATUS) {
                addMenuItem("Navigation Status", aisObject.getNavStatusString());
            }
            if (aisObject.getCog() != AisObjectConstants.INVALID_COG) {
                addMenuItem("COG", String.valueOf(aisObject.getCog()));
            }
            if (aisObject.getSog() != AisObjectConstants.INVALID_SOG) {
                addMenuItem("SOG", String.valueOf(aisObject.getSog()) + " kt");
            }
            if (aisObject.getHeading() != AisObjectConstants.INVALID_HEADING) {
                addMenuItem("Heading", String.valueOf(aisObject.getHeading()));
            }
            if (aisObject.getRot() != AisObjectConstants.INVALID_ROT) {
                addMenuItem("Rate of Turn", String.valueOf(aisObject.getRot()));
            }
            addMenuItemDimension();
            if (aisObject.getDraught() != AisObjectConstants.INVALID_DRAUGHT) {
                addMenuItem("Draught", String.valueOf(aisObject.getDraught()) + " m");
            }
            addMenuItem("Destination", aisObject.getDestination());
            if ((aisObject.getEtaDay() != AisObjectConstants.INVALID_ETA) &&
                    (aisObject.getEtaHour() != AisObjectConstants.INVALID_ETA_HOUR) &&
                    (aisObject.getEtaMin() != AisObjectConstants.INVALID_ETA_MIN) &&
                    (aisObject.getEtaMon() != AisObjectConstants.INVALID_ETA)) {
                @SuppressLint("DefaultLocale") String eta = new String(aisObject.getEtaDay() + "." +
                        aisObject.getEtaMon() + ". " + String.format("%02d", aisObject.getEtaHour()) + ":" +
                        String.format("%02d", aisObject.getEtaMin()));
                addMenuItem("ETA", eta);
            }
        }
        if (lastUpdate > 60) {
            addMenuItem("Last Update", (lastUpdate / 60) +
                    " min " + (lastUpdate % 60) + " sec");
        } else {
            addMenuItem("Last Update", lastUpdate + " sec");
        }
        boolean hasNext = iter.hasNext();
        while (hasNext) {
            msgTypesString = msgTypesString.concat(Integer.toString(iter.next()));
            hasNext = iter.hasNext();
            if (hasNext) { msgTypesString = msgTypesString.concat(", "); }
        }
        addMenuItem("Message Type(s)", msgTypesString);
    }

    @Override
    protected void setObject(Object object) {
        if (object instanceof AisObject) {
            this.aisObject = (AisObject) object;
        }
    }

    @Override
    protected Object getObject() {
        return aisObject;
    }
    @Override
    public CharSequence getAdditionalInfoStr() { return "Country: " + aisObject.getCountryCode(); }

    @NonNull
    @Override
    public String getTypeStr() {  return "AIS object";  }

    @Override
    public boolean needStreetName() { return false; }
}