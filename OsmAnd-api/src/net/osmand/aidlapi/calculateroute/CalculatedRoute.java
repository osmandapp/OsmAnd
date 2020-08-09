package net.osmand.aidlapi.calculateroute;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.map.ALatLon;

import java.util.ArrayList;

public class CalculatedRoute extends AidlParams {

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
    protected void readFromBundle(Bundle bundle) {
        bundle.setClassLoader(ALatLon.class.getClassLoader());
        isRouteCalculated = bundle.getBoolean("isRouteCalculated");
        appMode = bundle.getString("appMode");
        routePoints = bundle.getParcelableArrayList("routePoints");
        routeDistance = bundle.getInt("routeDistance");
        routingTime = bundle.getFloat("routingTime");
        creationTime = bundle.getLong("creationTime");
        calculationTime = bundle.getFloat("calculationTime");
    }

    @Override
    protected void writeToBundle(Bundle bundle) {
        bundle.putBoolean("isRouteCalculated", isRouteCalculated);
        bundle.putString("appMode", appMode);
        bundle.putParcelableArrayList("routePoints", routePoints);
        bundle.putInt("routeDistance", routeDistance);
        bundle.putFloat("routingTime", routingTime);
        bundle.putLong("creationTime", creationTime);
        bundle.putFloat("calculationTime", calculationTime);
    }
}
