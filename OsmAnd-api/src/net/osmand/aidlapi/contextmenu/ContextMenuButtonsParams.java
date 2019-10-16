package net.osmand.aidlapi.contextmenu;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;

public class ContextMenuButtonsParams extends AidlParams {

	private AContextMenuButton leftButton;
	private AContextMenuButton rightButton;

	private String id;
	private String appPackage;
	private String layerId;

	private long callbackId = -1L;

	private ArrayList<String> pointsIds = new ArrayList<>();

	public ContextMenuButtonsParams(AContextMenuButton leftButton, AContextMenuButton rightButton, String id, String appPackage, String layerId, long callbackId, List<String> pointsIds) {
		this.leftButton = leftButton;
		this.rightButton = rightButton;
		this.id = id;
		this.appPackage = appPackage;
		this.layerId = layerId;
		this.callbackId = callbackId;
		if (pointsIds != null) {
			this.pointsIds.addAll(pointsIds);
		}
	}

	public ContextMenuButtonsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ContextMenuButtonsParams> CREATOR = new Creator<ContextMenuButtonsParams>() {
		@Override
		public ContextMenuButtonsParams createFromParcel(Parcel in) {
			return new ContextMenuButtonsParams(in);
		}

		@Override
		public ContextMenuButtonsParams[] newArray(int size) {
			return new ContextMenuButtonsParams[size];
		}
	};

	public AContextMenuButton getLeftButton() {
		return leftButton;
	}

	public AContextMenuButton getRightButton() {
		return rightButton;
	}

	public String getId() {
		return id;
	}

	public String getAppPackage() {
		return appPackage;
	}

	public String getLayerId() {
		return layerId;
	}

	public long getCallbackId() {
		return callbackId;
	}

	public void setCallbackId(long callbackId) {
		this.callbackId = callbackId;
	}

	public List<String> getPointsIds() {
		return pointsIds;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("leftButton", leftButton);
		bundle.putParcelable("rightButton", rightButton);
		bundle.putString("id", id);
		bundle.putString("appPackage", appPackage);
		bundle.putString("layerId", layerId);
		bundle.putLong("callbackId", callbackId);
		bundle.putStringArrayList("pointsIds", pointsIds);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AContextMenuButton.class.getClassLoader());
		leftButton = bundle.getParcelable("leftButton");
		rightButton = bundle.getParcelable("rightButton");
		id = bundle.getString("id");
		appPackage = bundle.getString("appPackage");
		layerId = bundle.getString("layerId");
		callbackId = bundle.getLong("callbackId");
		pointsIds = bundle.getStringArrayList("pointsIds");
	}
}