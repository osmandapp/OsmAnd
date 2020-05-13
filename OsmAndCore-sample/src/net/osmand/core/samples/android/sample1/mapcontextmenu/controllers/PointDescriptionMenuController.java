package net.osmand.core.samples.android.sample1.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuBuilder;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class PointDescriptionMenuController extends MenuController {

	private boolean hasTypeInDescription;

	public PointDescriptionMenuController(MainActivity mainActivity, final PointDescription pointDescription) {
		super(new MenuBuilder(mainActivity), pointDescription, mainActivity);
		builder.setShowNearestWiki(true);
		initData();
	}

	private void initData() {
		hasTypeInDescription = !Algorithms.isEmpty(getPointDescription().getTypeName());
	}

	@Override
	protected void setObject(Object object) {
		initData();
	}

	@Override
	protected Object getObject() {
		return null;
	}

	@Override
	public boolean displayStreetNameInTitle() {
		return true;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(getPointDescription().getIconId());
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		if (hasTypeInDescription) {
			return getIcon(OsmandResources.getDrawableId("ic_action_group_name_16"));
		} else {
			return null;
		}
	}

	@Override
	public String getTypeStr() {
		if (hasTypeInDescription) {
			return getPointDescription().getTypeName();
		} else {
			return "";
		}
	}

	@Override
	public String getCommonTypeStr() {
		return getMainActivity().getMyApplication().getString("shared_string_location");
	}

	@Override
	public boolean needStreetName() {
		return !getPointDescription().isAddress();
	}
}
