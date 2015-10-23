package net.osmand.plus.mapcontextmenu.details;

import android.view.View;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapcontextmenu.MenuBuilder;

public class PointDescriptionMenuBuilder extends MenuBuilder {
	private final PointDescription pointDescription;

	public PointDescriptionMenuBuilder(OsmandApplication app, final PointDescription pointDescription) {
		super(app);
		this.pointDescription = pointDescription;
	}

	private void buildRow(View view, int iconId, String text, int textColor) {
		buildRow(view, getRowIcon(iconId), text, textColor);
	}

	@Override
	public void build(View view) {
		super.build(view);

		for (MenuBuilder.PlainMenuItem item : plainMenuItems) {
			buildRow(view, item.getIconId(), item.getText(), 0);
		}
	}
}
