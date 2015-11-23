package net.osmand.plus.osmedit;

import android.view.View;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.render.RenderingIcons;

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
				if (EditPoiData.POI_TYPE_TAG.equals(e.getKey())) {
					String poiTranslation = e.getValue();
					Map<String, PoiType> poiTypeMap = MapPoiTypes.getDefault().getAllTranslatedNames(false);
					PoiType poiType = poiTypeMap.get(poiTranslation.toLowerCase());
					int resId = 0;
					if (poiType != null) {
						String id = null;
						if (RenderingIcons.containsBigIcon(poiType.getIconKeyName())) {
							id = poiType.getIconKeyName();
						} else if (RenderingIcons.containsBigIcon(poiType.getOsmTag() + "_" + poiType.getOsmValue())) {
							id = poiType.getOsmTag() + "_" + poiType.getOsmValue();
						}
						if (id != null) {
							resId = RenderingIcons.getBigIconResourceId(id);
						}
					}
					if (resId == 0) {
						resId = R.drawable.ic_action_folder_stroke;
					}
					buildRow(view, resId, poiTranslation, 0, false, 0);
					break;
				}
			}

			for (Map.Entry<String, String> e : point.getEntity().getTags().entrySet()) {
				if (EditPoiData.POI_TYPE_TAG.equals(e.getKey())) {
					continue;
				}
				String text = e.getKey() + "=" + e.getValue();
				buildRow(view, R.drawable.ic_action_info_dark, text, 0, false, 0);
			}
		}
	}
}
