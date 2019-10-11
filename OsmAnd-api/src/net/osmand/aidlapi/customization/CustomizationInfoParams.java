package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.navdrawer.NavDrawerFooterParams;
import net.osmand.aidlapi.navdrawer.NavDrawerHeaderParams;
import net.osmand.aidlapi.navdrawer.SetNavDrawerItemsParams;
import net.osmand.aidlapi.plugins.PluginParams;

import java.util.ArrayList;
import java.util.List;

public class CustomizationInfoParams extends AidlParams {

	private OsmandSettingsParams settingsParams;

	private NavDrawerHeaderParams navDrawerHeaderParams;
	private NavDrawerFooterParams navDrawerFooterParams;
	private SetNavDrawerItemsParams navDrawerItemsParams;

	private ArrayList<SetWidgetsParams> visibilityWidgetsParams = new ArrayList<>();
	private ArrayList<SetWidgetsParams> availabilityWidgetsParams = new ArrayList<>();

	private ArrayList<PluginParams> pluginsParams = new ArrayList<>();

	private ArrayList<String> featuresEnabledIds = new ArrayList<>();
	private ArrayList<String> featuresDisabledIds = new ArrayList<>();
	private ArrayList<String> featuresEnabledPatterns = new ArrayList<>();
	private ArrayList<String> featuresDisabledPatterns = new ArrayList<>();

	public CustomizationInfoParams(OsmandSettingsParams settingsParams,
	                               NavDrawerHeaderParams navDrawerHeaderParams,
	                               NavDrawerFooterParams navDrawerFooterParams,
	                               SetNavDrawerItemsParams navDrawerItemsParams,
	                               ArrayList<SetWidgetsParams> visibilityWidgetsParams,
	                               ArrayList<SetWidgetsParams> availabilityWidgetsParams,
	                               ArrayList<PluginParams> pluginsParams,
	                               List<String> featuresEnabledIds,
	                               List<String> featuresDisabledIds,
	                               List<String> featuresEnabledPatterns,
	                               List<String> featuresDisabledPatterns) {
		this.settingsParams = settingsParams;
		this.navDrawerHeaderParams = navDrawerHeaderParams;
		this.navDrawerFooterParams = navDrawerFooterParams;
		this.navDrawerItemsParams = navDrawerItemsParams;

		if (visibilityWidgetsParams != null) {
			this.visibilityWidgetsParams.addAll(visibilityWidgetsParams);
		}
		if (availabilityWidgetsParams != null) {
			this.availabilityWidgetsParams.addAll(availabilityWidgetsParams);
		}
		if (pluginsParams != null) {
			this.pluginsParams.addAll(pluginsParams);
		}
		if (featuresEnabledIds != null) {
			this.featuresEnabledIds.addAll(featuresEnabledIds);
		}
		if (featuresDisabledIds != null) {
			this.featuresDisabledIds.addAll(featuresDisabledIds);
		}
		if (featuresEnabledPatterns != null) {
			this.featuresEnabledPatterns.addAll(featuresEnabledPatterns);
		}
		if (featuresDisabledPatterns != null) {
			this.featuresDisabledPatterns.addAll(featuresDisabledPatterns);
		}
	}

	public CustomizationInfoParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<CustomizationInfoParams> CREATOR = new Creator<CustomizationInfoParams>() {
		@Override
		public CustomizationInfoParams createFromParcel(Parcel in) {
			return new CustomizationInfoParams(in);
		}

		@Override
		public CustomizationInfoParams[] newArray(int size) {
			return new CustomizationInfoParams[size];
		}
	};

	public OsmandSettingsParams getSettingsParams() {
		return settingsParams;
	}

	public NavDrawerHeaderParams getNavDrawerHeaderParams() {
		return navDrawerHeaderParams;
	}

	public NavDrawerFooterParams getNavDrawerFooterParams() {
		return navDrawerFooterParams;
	}

	public SetNavDrawerItemsParams getNavDrawerItemsParams() {
		return navDrawerItemsParams;
	}

	public ArrayList<SetWidgetsParams> getVisibilityWidgetsParams() {
		return visibilityWidgetsParams;
	}

	public ArrayList<SetWidgetsParams> getAvailabilityWidgetsParams() {
		return availabilityWidgetsParams;
	}

	public ArrayList<PluginParams> getPluginsParams() {
		return pluginsParams;
	}

	public List<String> getFeaturesEnabledIds() {
		return featuresEnabledIds;
	}

	public List<String> getFeaturesDisabledIds() {
		return featuresDisabledIds;
	}

	public List<String> getFeaturesEnabledPatterns() {
		return featuresEnabledPatterns;
	}

	public List<String> getFeaturesDisabledPatterns() {
		return featuresDisabledPatterns;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("settingsParams", settingsParams);

		bundle.putParcelable("navDrawerHeaderParams", navDrawerHeaderParams);
		bundle.putParcelable("navDrawerFooterParams", navDrawerFooterParams);
		bundle.putParcelable("navDrawerItemsParams", navDrawerItemsParams);

		bundle.putParcelableArrayList("visibilityWidgetsParams", visibilityWidgetsParams);
		bundle.putParcelableArrayList("availabilityWidgetsParams", availabilityWidgetsParams);
		bundle.putParcelableArrayList("pluginsParams", pluginsParams);

		bundle.putStringArrayList("featuresEnabledIds", featuresEnabledIds);
		bundle.putStringArrayList("featuresDisabledIds", featuresDisabledIds);
		bundle.putStringArrayList("featuresEnabledPatterns", featuresEnabledPatterns);
		bundle.putStringArrayList("featuresDisabledPatterns", featuresDisabledPatterns);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(OsmandSettingsParams.class.getClassLoader());
		settingsParams = bundle.getParcelable("settingsParams");

		bundle.setClassLoader(NavDrawerHeaderParams.class.getClassLoader());
		navDrawerHeaderParams = bundle.getParcelable("navDrawerHeaderParams");
		bundle.setClassLoader(NavDrawerFooterParams.class.getClassLoader());
		navDrawerFooterParams = bundle.getParcelable("navDrawerFooterParams");
		bundle.setClassLoader(SetNavDrawerItemsParams.class.getClassLoader());
		navDrawerItemsParams = bundle.getParcelable("navDrawerItemsParams");

		bundle.setClassLoader(SetWidgetsParams.class.getClassLoader());
		visibilityWidgetsParams = bundle.getParcelableArrayList("visibilityWidgetsParams");
		availabilityWidgetsParams = bundle.getParcelableArrayList("availabilityWidgetsParams");
		bundle.setClassLoader(PluginParams.class.getClassLoader());
		pluginsParams = bundle.getParcelableArrayList("pluginsParams");

		featuresEnabledIds = bundle.getStringArrayList("featuresEnabledIds");
		featuresDisabledIds = bundle.getStringArrayList("featuresDisabledIds");
		featuresEnabledPatterns = bundle.getStringArrayList("featuresEnabledPatterns");
		featuresDisabledPatterns = bundle.getStringArrayList("featuresDisabledPatterns");
	}
}