package net.osmand.aidl;

import net.osmand.aidl.contextmenu.AContextMenuButton;
import net.osmand.aidl.contextmenu.ContextMenuButtonsParams;

import java.util.List;

public class AidlContextMenuButtonsWrapper {

	private AidlContextMenuButtonWrapper leftButton;
	private AidlContextMenuButtonWrapper rightButton;

	private String id;
	private String appPackage;
	private String layerId;

	private long callbackId;

	private List<String> pointsIds;

	public AidlContextMenuButtonsWrapper(ContextMenuButtonsParams params) {
		id = params.getId();
		appPackage = params.getAppPackage();
		layerId = params.getLayerId();
		callbackId = params.getCallbackId();
		pointsIds = params.getPointsIds();

		AContextMenuButton leftButton = params.getLeftButton();
		AContextMenuButton rightButton = params.getRightButton();
		if (leftButton != null) {
			this.leftButton = new AidlContextMenuButtonWrapper(leftButton);
		}
		if (rightButton != null) {
			this.rightButton = new AidlContextMenuButtonWrapper(rightButton);
		}
	}

	public AidlContextMenuButtonsWrapper(net.osmand.aidlapi.contextmenu.ContextMenuButtonsParams params) {
		id = params.getId();
		appPackage = params.getAppPackage();
		layerId = params.getLayerId();
		callbackId = params.getCallbackId();
		pointsIds = params.getPointsIds();

		net.osmand.aidlapi.contextmenu.AContextMenuButton leftButton = params.getLeftButton();
		net.osmand.aidlapi.contextmenu.AContextMenuButton rightButton = params.getRightButton();
		if (leftButton != null) {
			this.leftButton = new AidlContextMenuButtonWrapper(leftButton);
		}
		if (rightButton != null) {
			this.leftButton = new AidlContextMenuButtonWrapper(rightButton);
		}
	}

	public AidlContextMenuButtonWrapper getLeftButton() {
		return leftButton;
	}

	public AidlContextMenuButtonWrapper getRightButton() {
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
}