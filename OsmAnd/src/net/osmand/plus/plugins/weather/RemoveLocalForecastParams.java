package net.osmand.plus.plugins.weather;

import net.osmand.OnCompleteCallback;

public class RemoveLocalForecastParams {

	private String[] regionIds;
	private OnCompleteCallback onSettingsRemovedCallback;
	private OnCompleteCallback onDataRemovedCallback;
	private boolean shouldRefreshMap = false;

	private RemoveLocalForecastParams() { }

	public RemoveLocalForecastParams setRegionIds(String ... regionIds) {
		this.regionIds = regionIds;
		return this;
	}

	public RemoveLocalForecastParams setOnSettingsRemovedCallback(OnCompleteCallback settingsRemovedCallback) {
		this.onSettingsRemovedCallback = settingsRemovedCallback;
		return this;
	}

	public void setOnDataRemovedCallback(OnCompleteCallback onDataRemovedCallback) {
		this.onDataRemovedCallback = onDataRemovedCallback;
	}

	public RemoveLocalForecastParams setRefreshMap() {
		this.shouldRefreshMap = true;
		return this;
	}

	public String[] getRegionIds() {
		return regionIds;
	}

	public OnCompleteCallback getOnSettingsRemovedCallback() {
		return onSettingsRemovedCallback;
	}

	public OnCompleteCallback getOnDataRemovedCallback() {
		return onDataRemovedCallback;
	}

	public boolean shouldRefreshMap() {
		return shouldRefreshMap;
	}

	public static RemoveLocalForecastParams newInstance() {
		return new RemoveLocalForecastParams();
	}
}
