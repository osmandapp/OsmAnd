package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class NavigationStatus implements Parcelable {

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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(currentRouteSegmentIndex);
        dest.writeFloat(currentMaxSpeed);
        dest.writeInt(leftDistance);
        dest.writeInt(leftTime);
        dest.writeInt(leftDistanceToNextIntermediate);
        dest.writeInt(leftTimeToNextIntermediate);
        dest.writeByte((byte) (isRouteCalculated ? 1 : 0));
        dest.writeByte((byte) (isRouteFinished ? 1 : 0));
        dest.writeByte((byte) (isPauseNavigation ? 1 : 0));
        dest.writeByte((byte) (isDeviatedFromRoute ? 1 : 0));
    }

    public void readFromParcel(Parcel in) {
        currentRouteSegmentIndex = in.readInt();
        currentMaxSpeed = in.readFloat();
        leftDistance = in.readInt();
        leftTime = in.readInt();
        leftDistanceToNextIntermediate = in.readInt();
        leftTimeToNextIntermediate = in.readInt();
        isRouteCalculated = in.readByte() != 0;
        isRouteFinished = in.readByte() != 0;
        isPauseNavigation = in.readByte() != 0;
        isDeviatedFromRoute = in.readByte() != 0;
    }
}
