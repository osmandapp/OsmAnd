package net.osmand.plus.osmedit;

import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MenuBuilder;

import java.util.Map;

public class EditPOIMenuBuilder extends MenuBuilder {

	private final OsmPoint osmPoint;

	public EditPOIMenuBuilder(OsmandApplication app, final OsmPoint osmPoint) {
		super(app);
		this.osmPoint = osmPoint;
	}

	@Override
	public void buildInternal(View view) {
		if (osmPoint instanceof OsmNotesPoint) {
			OsmNotesPoint notes = (OsmNotesPoint) osmPoint;

			buildRow(view, R.drawable.ic_action_note_dark, notes.getText(), 0, false, 0);
			buildRow(view, R.drawable.ic_group, notes.getAuthor(), 0, false, 0);

		} else if (osmPoint instanceof OpenstreetmapPoint) {
			OpenstreetmapPoint point = (OpenstreetmapPoint) osmPoint;

			for (Map.Entry<String, String> e : point.getEntity().getTags().entrySet()) {
				String text = e.getKey() + "=" + e.getValue();
				buildRow(view, R.drawable.ic_action_info_dark, text, 0, false, 0);
			}
		}
	}
}
