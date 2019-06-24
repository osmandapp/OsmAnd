package net.osmand.aidl.customization;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidl.navdrawer.NavDrawerFooterParams;
import net.osmand.aidl.navdrawer.NavDrawerHeaderParams;
import net.osmand.aidl.navdrawer.SetNavDrawerItemsParams;
import net.osmand.aidl.plugins.PluginParams;

import java.util.ArrayList;
import java.util.List;

public class CustomizationInfoParams implements Parcelable {

	private OsmandSettingsParams settingsParams;

	private NavDrawerHeaderParams navDrawerHeaderParams;
	private NavDrawerFooterParams navDrawerFooterParams;
	private SetNavDrawerItemsParams navDrawerItemsParams;

	private ArrayList<SetWidgetsParams> visibilityWidgetsParams = new ArrayList<>();
	private ArrayList<SetWidgetsParams> availabilityWidgetsParams = new ArrayList<>();

	private ArrayList<PluginParams> pluginsParams = new ArrayList<>();

	private List<String> featuresEnabledIds = new ArrayList<>();
	private List<String> featuresDisabledIds = new ArrayList<>();
	private List<String> featuresEnabledPatterns = new ArrayList<>();
	private List<String> featuresDisabledPatterns = new ArrayList<>();

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
	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(settingsParams, flags);

		out.writeParcelable(navDrawerHeaderParams, flags);
		out.writeParcelable(navDrawerFooterParams, flags);
		out.writeParcelable(navDrawerItemsParams, flags);

		out.writeTypedList(visibilityWidgetsParams);
		out.writeTypedList(availabilityWidgetsParams);
		out.writeTypedList(pluginsParams);

		out.writeStringList(featuresEnabledIds);
		out.writeStringList(featuresDisabledIds);
		out.writeStringList(featuresEnabledPatterns);
		out.writeStringList(featuresDisabledPatterns);
	}

	@SuppressLint("ParcelClassLoader")
	private void readFromParcel(Parcel in) {
		settingsParams = in.readParcelable(OsmandSettingsParams.class.getClassLoader());

		navDrawerHeaderParams = in.readParcelable(NavDrawerHeaderParams.class.getClassLoader());
		navDrawerFooterParams = in.readParcelable(NavDrawerFooterParams.class.getClassLoader());
		navDrawerItemsParams = in.readParcelable(SetNavDrawerItemsParams.class.getClassLoader());

		in.readTypedList(visibilityWidgetsParams, SetWidgetsParams.CREATOR);
		in.readTypedList(availabilityWidgetsParams, SetWidgetsParams.CREATOR);
		in.readTypedList(pluginsParams, PluginParams.CREATOR);

		in.readStringList(featuresEnabledIds);
		in.readStringList(featuresDisabledIds);
		in.readStringList(featuresEnabledPatterns);
		in.readStringList(featuresDisabledPatterns);
	}

	@Override
	public int describeContents() {
		return 0;
	}
}