package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.GPXDatabase;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper;
import net.osmand.plus.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.settings.backend.backup.GpxAppearanceInfo;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.GradientScaleType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class GpxSettingsItem extends FileSettingsItem {

	private GpxAppearanceInfo appearanceInfo;

	public GpxSettingsItem(@NonNull OsmandApplication app, @NonNull File file) throws IllegalArgumentException {
		super(app, file);
		createGpxAppearanceInfo();
	}

	public GpxSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Nullable
	public GpxAppearanceInfo getAppearanceInfo() {
		return appearanceInfo;
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.GPX;
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return GpxUiHelper.getGpxTitle(file.getName());
	}

	@Override
	void readFromJson(@NonNull JSONObject json) throws JSONException {
		subtype = FileSubtype.GPX;
		super.readFromJson(json);
		appearanceInfo = GpxAppearanceInfo.fromJson(json);
	}

	@Override
	void writeToJson(@NonNull JSONObject json) throws JSONException {
		super.writeToJson(json);
		if (appearanceInfo != null) {
			appearanceInfo.toJson(json);
		}
	}

	@Override
	public void applyAdditionalParams() {
		if (appearanceInfo != null) {
			GpxDataItem dataItem = app.getGpxDbHelper().getItem(savedFile, new GpxDataItemCallback() {
				@Override
				public boolean isCancelled() {
					return false;
				}

				@Override
				public void onGpxDataItemReady(GpxDataItem item) {
					updateGpxParams(item);
				}
			});
			if (dataItem != null) {
				updateGpxParams(dataItem);
			}
		}
	}

	private void updateGpxParams(@NonNull GPXDatabase.GpxDataItem dataItem) {
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		GpxSplitType splitType = GpxSplitType.getSplitTypeByTypeId(appearanceInfo.splitType);
		gpxDbHelper.updateColor(dataItem, appearanceInfo.color);
		gpxDbHelper.updateWidth(dataItem, appearanceInfo.width);
		gpxDbHelper.updateShowArrows(dataItem, appearanceInfo.showArrows);
		gpxDbHelper.updateShowStartFinish(dataItem, appearanceInfo.showStartFinish);
		gpxDbHelper.updateSplit(dataItem, splitType, appearanceInfo.splitInterval);
		gpxDbHelper.updateGradientScaleType(dataItem, appearanceInfo.scaleType);
		gpxDbHelper.updateGradientScalePalette(dataItem, GradientScaleType.SPEED, appearanceInfo.gradientSpeedPalette);
		gpxDbHelper.updateGradientScalePalette(dataItem, GradientScaleType.ALTITUDE, appearanceInfo.gradientAltitudePalette);
		gpxDbHelper.updateGradientScalePalette(dataItem, GradientScaleType.SLOPE, appearanceInfo.gradientSlopePalette);
	}

	private void createGpxAppearanceInfo() {
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(file, new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void onGpxDataItemReady(GPXDatabase.GpxDataItem item) {
				appearanceInfo = new GpxAppearanceInfo(item);
			}
		});
		if (dataItem != null) {
			appearanceInfo = new GpxAppearanceInfo(dataItem);
		}
	}
}
