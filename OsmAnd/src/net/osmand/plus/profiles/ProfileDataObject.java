package net.osmand.plus.profiles;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ProfileDataObject implements Comparable<ProfileDataObject> {

	private String name;
	private String description;
	private int iconRes;
	private String stringKey;
	private boolean isSelected;
	private boolean isEnabled;
	private ProfileIconColors iconColor;

	public ProfileDataObject(String name, String description, String stringKey, int iconRes,  boolean isSelected, ProfileIconColors iconColor) {
		this.name = name;
		this.iconRes = iconRes;
		this.description = description;
		this.isSelected = isSelected;
		this.stringKey = stringKey;
		this.iconColor = iconColor;
	}

	public String getName() {
		return name;
	}

	public int getIconRes() {
		return iconRes;
	}

	public String getDescription() {
		return description;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean selected) {
		isSelected = selected;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

	public String getStringKey() {
		return stringKey;
	}

	@ColorRes  public int getIconColor(boolean isNightMode) {
		return iconColor.getColor(isNightMode);
	}

	@Override
	public int compareTo(@NonNull ProfileDataObject another) {
		return this.name.compareToIgnoreCase(another.name);
	}

	public static List<ProfileDataObject> getDataObjects(OsmandApplication app,
	                                                     List<ApplicationMode> appModes) {
		List<ProfileDataObject> profiles = new ArrayList<>();
		for (ApplicationMode mode : appModes) {
			String description = mode.getDescription();
			if (Algorithms.isEmpty(description)) {
				description = getAppModeDescription(app, mode);
			}
			profiles.add(new ProfileDataObject(mode.toHumanString(), description,
					mode.getStringKey(), mode.getIconRes(), false, mode.getIconColorInfo()));
		}
		return profiles;
	}

	public static String getAppModeDescription(Context ctx, ApplicationMode mode) {
		String description;
		if (mode.isCustomProfile()) {
			description = ctx.getString(R.string.profile_type_user_string);
		} else {
			description = ctx.getString(R.string.profile_type_osmand_string);
		}

		return description;
	}
}
