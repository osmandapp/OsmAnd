package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

/**
 * Next turn info sent with navigation updates.
 * All fields after isLeftSide are optional: older OsmAnd versions do not send them,
 * so a client reads 0 / null for them.
 */
public class ADirectionInfo extends AidlParams {

	private int distanceTo; //distance to next turn
	private int turnType; //turn type
	private boolean isLeftSide; //is movement left-sided

	private int exitNumber; // roundabout exit number, 0 if the turn is not a roundabout
	private float turnAngle; // turn angle in degrees, 0 if unknown
	@Nullable
	private int[] lanes; // lanes encoded as in net.osmand.router.TurnType, null if no lanes data
	@Nullable
	private String streetName; // street name / ref / destination after the turn, null if unknown
	private int leftTime; // time left to the destination, seconds
	private int leftDistance; // distance left to the destination, meters

	public ADirectionInfo(int distanceTo, int turnType, boolean isLeftSide) {
		this.distanceTo = distanceTo;
		this.turnType = turnType;
		this.isLeftSide = isLeftSide;
	}

	protected ADirectionInfo(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ADirectionInfo> CREATOR = new Creator<ADirectionInfo>() {
		@Override
		public ADirectionInfo createFromParcel(Parcel in) {
			return new ADirectionInfo(in);
		}

		@Override
		public ADirectionInfo[] newArray(int size) {
			return new ADirectionInfo[size];
		}
	};

	public int getDistanceTo() {
		return distanceTo;
	}

	public int getTurnType() {
		return turnType;
	}

	public boolean isLeftSide() {
		return isLeftSide;
	}

	public int getExitNumber() {
		return exitNumber;
	}

	public float getTurnAngle() {
		return turnAngle;
	}

	@Nullable
	public int[] getLanes() {
		return lanes;
	}

	@Nullable
	public String getStreetName() {
		return streetName;
	}

	public int getLeftTime() {
		return leftTime;
	}

	public int getLeftDistance() {
		return leftDistance;
	}

	public void setDistanceTo(int distanceTo) {
		this.distanceTo = distanceTo;
	}

	public void setTurnType(int turnType) {
		this.turnType = turnType;
	}

	public void setLeftSide(boolean leftSide) {
		isLeftSide = leftSide;
	}

	public void setExitNumber(int exitNumber) {
		this.exitNumber = exitNumber;
	}

	public void setTurnAngle(float turnAngle) {
		this.turnAngle = turnAngle;
	}

	public void setLanes(@Nullable int[] lanes) {
		this.lanes = lanes;
	}

	public void setStreetName(@Nullable String streetName) {
		this.streetName = streetName;
	}

	public void setLeftTime(int leftTime) {
		this.leftTime = leftTime;
	}

	public void setLeftDistance(int leftDistance) {
		this.leftDistance = leftDistance;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		distanceTo = bundle.getInt("distanceTo");
		turnType = bundle.getInt("turnType");
		isLeftSide = bundle.getBoolean("isLeftSide");
		exitNumber = bundle.getInt("exitNumber");
		turnAngle = bundle.getFloat("turnAngle");
		lanes = bundle.getIntArray("lanes");
		streetName = bundle.getString("streetName");
		leftTime = bundle.getInt("leftTime");
		leftDistance = bundle.getInt("leftDistance");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putInt("distanceTo", distanceTo);
		bundle.putInt("turnType", turnType);
		bundle.putBoolean("isLeftSide", isLeftSide);
		bundle.putInt("exitNumber", exitNumber);
		bundle.putFloat("turnAngle", turnAngle);
		bundle.putIntArray("lanes", lanes);
		bundle.putString("streetName", streetName);
		bundle.putInt("leftTime", leftTime);
		bundle.putInt("leftDistance", leftDistance);
	}
}
