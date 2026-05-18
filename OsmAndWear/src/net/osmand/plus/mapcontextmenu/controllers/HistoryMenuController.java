package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class HistoryMenuController extends MenuController {

	private HistoryEntry entry;
	private boolean hasTypeInDescription;

	public HistoryMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull HistoryEntry entry) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.entry = entry;
		builder.setShowNearestWiki(true);
		initData();
	}

	private void initData() {
		hasTypeInDescription = !Algorithms.isEmpty(entry.getName().getTypeName());
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof HistoryEntry) {
			this.entry = (HistoryEntry) object;
			initData();
		}
	}

	@Override
	protected Object getObject() {
		return entry;
	}

	@Override
	public boolean displayStreetNameInTitle() {
		return entry.getName().isLocation();
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getRightIcon() {
		return getIcon(entry.getName().getItemIcon());
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		if (hasTypeInDescription) {
			return getIcon(R.drawable.ic_action_group_name_16);
		} else {
			return null;
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		if (hasTypeInDescription) {
			return entry.getName().getTypeName();
		} else {
			return "";
		}
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.shared_string_history);
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return !entry.getName().isAddress();
	}
}
