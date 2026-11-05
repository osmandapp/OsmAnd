package net.osmand.aidl.contextmenu;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ContextMenuButtonsParams implements Parcelable {

	private AContextMenuButton leftButton;
	private AContextMenuButton rightButton;

	private String id;
	private String appPackage;
	private String layerId;

	private long callbackId = -1L;

	private List<String> pointsIds = new ArrayList<>();

	public ContextMenuButtonsParams(AContextMenuButton leftButton, AContextMenuButton rightButton, String id, String appPackage, String layerId, long callbackId, List<String> pointsIds) {
		this.leftButton = leftButton;
		this.rightButton = rightButton;
		this.id = id;
		this.appPackage = appPackage;
		this.layerId = layerId;
		this.callbackId = callbackId;
		this.pointsIds = pointsIds;
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
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(leftButton, flags);
		dest.writeParcelable(rightButton, flags);
		dest.writeString(id);
		dest.writeString(appPackage);
		dest.writeString(layerId);
		dest.writeLong(callbackId);
		dest.writeStringList(pointsIds);
	}

	private void readFromParcel(Parcel in) {
		leftButton = in.readParcelable(AContextMenuButton.class.getClassLoader());
		rightButton = in.readParcelable(AContextMenuButton.class.getClassLoader());
		id = in.readString();
		appPackage = in.readString();
		layerId = in.readString();
		callbackId = in.readLong();
		in.readStringList(pointsIds);
	}

	@Override
	public int describeContents() {
		return 0;
	}
}