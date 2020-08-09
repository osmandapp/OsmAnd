package net.osmand.aidl.calculateroute;

import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidl.map.ALatLon;

import java.util.ArrayList;

public class CalculatedRoute implements Parcelable {

    private boolean isRouteCalculated;

    private String appMode;

    private ArrayList<ALatLon> routePoints;
    private int routeDistance;

    private float routingTime;
    private long creationTime;

    private float calculationTime;

    public CalculatedRoute() {

    }

    public CalculatedRoute(boolean isRouteCalculated, String appMode, ArrayList<ALatLon> routePoints, int routeDistance, float routingTime, long creationTime, float calculationTime) {
        this.isRouteCalculated = isRouteCalculated;
        this.appMode = appMode;
        this.routePoints = routePoints;
        this.routeDistance = routeDistance;
        this.routingTime = routingTime;
        this.creationTime = creationTime;
        this.calculationTime = calculationTime;
    }

    protected CalculatedRoute(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<CalculatedRoute> CREATOR = new Creator<CalculatedRoute>() {
        @Override
        public CalculatedRoute createFromParcel(Parcel in) {
            return new CalculatedRoute(in);
        }

        @Override
        public CalculatedRoute[] newArray(int size) {
            return new CalculatedRoute[size];
        }
    };

    public boolean isRouteCalculated() {
        return isRouteCalculated;
    }

    public String getAppMode() {
        return appMode;
    }

    public ArrayList<ALatLon> getRoutePoints() {
        return routePoints;
    }

    public int getRouteDistance() {
        return routeDistance;
    }

    public float getRoutingTime() {
        return routingTime;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public float getCalculationTime() {
        return calculationTime;
    }

    public void setRouteCalculated(boolean routeCalculated) {
        isRouteCalculated = routeCalculated;
    }

    public void setAppMode(String appMode) {
        this.appMode = appMode;
    }

    public void setRoutePoints(ArrayList<ALatLon> routePoints) {
        this.routePoints = routePoints;
    }

    public void setRouteDistance(int routeDistance) {
        this.routeDistance = routeDistance;
    }

    public void setRoutingTime(float routingTime) {
        this.routingTime = routingTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setCalculationTime(float calculationTime) {
        this.calculationTime = calculationTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isRouteCalculated ? 1 : 0));
        dest.writeString(appMode);
        dest.writeTypedList(routePoints);
        dest.writeInt(routeDistance);
        dest.writeFloat(routingTime);
        dest.writeLong(creationTime);
        dest.writeFloat(calculationTime);
    }

    public void readFromParcel(Parcel in) {
        isRouteCalculated = in.readByte() != 0;
        appMode = in.readString();
        routePoints = in.createTypedArrayList(ALatLon.CREATOR);
        routeDistance = in.readInt();
        routingTime = in.readFloat();
        creationTime = in.readLong();
        calculationTime = in.readFloat();
    }
}
