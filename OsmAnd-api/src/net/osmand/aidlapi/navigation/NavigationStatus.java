package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class NavigationStatus extends AidlParams {

    private int currentRouteSegmentIndex;

    private float currentMaxSpeed;

    private int leftDistance;
    private int leftTime;

    private int leftDistanceToNextIntermediate;
    private int leftTimeToNextIntermediate;

    private boolean isRouteCalculated;
    private boolean isRouteFinished;

    private boolean isPauseNavigation;
    private boolean isDeviatedFromRoute;

    public NavigationStatus() {

    }

    public NavigationStatus(int currentRouteSegmentIndex, float currentMaxSpeed, int leftDistance, int leftTime, int leftDistanceToNextIntermediate, int leftTimeToNextIntermediate, boolean isRouteCalculated, boolean isRouteFinished, boolean isPauseNavigation, boolean isDeviatedFromRoute) {
        this.currentRouteSegmentIndex = currentRouteSegmentIndex;
        this.currentMaxSpeed = currentMaxSpeed;
        this.leftDistance = leftDistance;
        this.leftTime = leftTime;
        this.leftDistanceToNextIntermediate = leftDistanceToNextIntermediate;
        this.leftTimeToNextIntermediate = leftTimeToNextIntermediate;
        this.isRouteCalculated = isRouteCalculated;
        this.isRouteFinished = isRouteFinished;
        this.isPauseNavigation = isPauseNavigation;
        this.isDeviatedFromRoute = isDeviatedFromRoute;
    }

    protected NavigationStatus(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<NavigationStatus> CREATOR = new Creator<NavigationStatus>() {
        @Override
        public NavigationStatus createFromParcel(Parcel in) {
            return new NavigationStatus(in);
        }

        @Override
        public NavigationStatus[] newArray(int size) {
            return new NavigationStatus[size];
        }
    };

    public int getCurrentRouteSegmentIndex() {
        return currentRouteSegmentIndex;
    }

    public float getCurrentMaxSpeed() {
        return currentMaxSpeed;
    }

    public int getLeftDistance() {
        return leftDistance;
    }

    public int getLeftTime() {
        return leftTime;
    }

    public int getLeftDistanceToNextIntermediate() {
        return leftDistanceToNextIntermediate;
    }

    public int getLeftTimeToNextIntermediate() {
        return leftTimeToNextIntermediate;
    }

    public boolean isRouteCalculated() {
        return isRouteCalculated;
    }

    public boolean isRouteFinished() {
        return isRouteFinished;
    }

    public boolean isPauseNavigation() {
        return isPauseNavigation;
    }

    public boolean isDeviatedFromRoute() {
        return isDeviatedFromRoute;
    }

    public void setCurrentRouteSegmentIndex(int currentRouteSegmentIndex) {
        this.currentRouteSegmentIndex = currentRouteSegmentIndex;
    }

    public void setCurrentMaxSpeed(float currentMaxSpeed) {
        this.currentMaxSpeed = currentMaxSpeed;
    }

    public void setLeftDistance(int leftDistance) {
        this.leftDistance = leftDistance;
    }

    public void setLeftTime(int leftTime) {
        this.leftTime = leftTime;
    }

    public void setLeftDistanceToNextIntermediate(int leftDistanceToNextIntermediate) {
        this.leftDistanceToNextIntermediate = leftDistanceToNextIntermediate;
    }

    public void setLeftTimeToNextIntermediate(int leftTimeToNextIntermediate) {
        this.leftTimeToNextIntermediate = leftTimeToNextIntermediate;
    }

    public void setRouteCalculated(boolean routeCalculated) {
        isRouteCalculated = routeCalculated;
    }

    public void setRouteFinished(boolean routeFinished) {
        isRouteFinished = routeFinished;
    }

    public void setPauseNavigation(boolean pauseNavigation) {
        isPauseNavigation = pauseNavigation;
    }

    public void setDeviatedFromRoute(boolean deviatedFromRoute) {
        isDeviatedFromRoute = deviatedFromRoute;
    }

    @Override
    protected void readFromBundle(Bundle bundle) {
        currentRouteSegmentIndex = bundle.getInt("currentRouteSegmentIndex");
        currentMaxSpeed = bundle.getFloat("currentMaxSpeed");
        leftDistance = bundle.getInt("leftDistance");
        leftTime = bundle.getInt("leftTime");
        leftDistanceToNextIntermediate = bundle.getInt("leftDistanceToNextIntermediate");
        leftTimeToNextIntermediate = bundle.getInt("leftTimeToNextIntermediate");
        isRouteCalculated = bundle.getBoolean("isRouteCalculated");
        isRouteFinished = bundle.getBoolean("isRouteFinished");
        isPauseNavigation = bundle.getBoolean("isPauseNavigation");
        isDeviatedFromRoute = bundle.getBoolean("isDeviatedFromRoute");
    }

    @Override
    protected void writeToBundle(Bundle bundle) {
        bundle.putInt("currentRouteSegmentIndex", currentRouteSegmentIndex);
        bundle.putFloat("currentMaxSpeed", currentMaxSpeed);
        bundle.putInt("leftDistance", leftDistance);
        bundle.putInt("leftTime", leftTime);
        bundle.putInt("leftDistanceToNextIntermediate", leftDistanceToNextIntermediate);
        bundle.putInt("leftTimeToNextIntermediate", leftTimeToNextIntermediate);
        bundle.putBoolean("isRouteCalculated", isRouteCalculated);
        bundle.putBoolean("isRouteFinished", isRouteFinished);
        bundle.putBoolean("isPauseNavigation", isPauseNavigation);
        bundle.putBoolean("isDeviatedFromRoute", isDeviatedFromRoute);
    }
}
