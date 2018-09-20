package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.helpers.SearchHistoryHelper.PointHistoryEntry;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class HistoryMenuController extends MenuController {

	private PointHistoryEntry point;
	private boolean hasTypeInDescription;

	public HistoryMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, final @NonNull PointHistoryEntry point) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.point = point;
		builder.setShowNearestWiki(true);
		initData();
	}

	private void initData() {
		hasTypeInDescription = !Algorithms.isEmpty(point.getName().getTypeName());
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof PointHistoryEntry) {
			this.point = (PointHistoryEntry) object;
			initData();
		}
	}

	@Override
	protected Object getObject() {
		return point;
	}

	@Override
	public boolean displayStreetNameInTitle() {
		return point.getName().isLocation();
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getRightIcon() {
		return getIcon(SearchHistoryFragment.getItemIcon(point.getName()));
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		if (hasTypeInDescription) {
			return getIcon(R.drawable.ic_small_group);
		} else {
			return null;
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		if (hasTypeInDescription) {
			return point.getName().getTypeName();
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
		return !point.getName().isAddress();
	}
}
