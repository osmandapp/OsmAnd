package net.osmand.plus.osmedit;

import android.view.View;

import net.osmand.data.PointDescription;
import net.osmand.osm.PoiType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.render.RenderingIcons;

import java.util.Map;

public class EditPOIMenuBuilder extends MenuBuilder {

	private final OsmPoint osmPoint;

	public EditPOIMenuBuilder(MapActivity mapActivity, final OsmPoint osmPoint) {
		super(mapActivity);
		this.osmPoint = osmPoint;
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void buildInternal(View view) {
		if (osmPoint instanceof OsmNotesPoint) {
			OsmNotesPoint notes = (OsmNotesPoint) osmPoint;

			buildRow(view, R.drawable.ic_action_note_dark, null, notes.getText(), 0, false, null, false, 0, false, null, false);
			buildRow(view, R.drawable.ic_group, null, notes.getAuthor(), 0, false, null, false, 0, false, null, false);

		} else if (osmPoint instanceof OpenstreetmapPoint) {
			OpenstreetmapPoint point = (OpenstreetmapPoint) osmPoint;

			for (Map.Entry<String, String> e : point.getEntity().getTags().entrySet()) {
				if (EditPoiData.POI_TYPE_TAG.equals(e.getKey())) {
					String poiTranslation = e.getValue();
					Map<String, PoiType> poiTypeMap = app.getPoiTypes().getAllTranslatedNames(false);
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
					buildRow(view, resId, null, poiTranslation, 0, false, null, false, 0, false, null, false);
					break;
				}
			}

			for (Map.Entry<String, String> e : point.getEntity().getTags().entrySet()) {
				if (EditPoiData.POI_TYPE_TAG.equals(e.getKey()) || 
						e.getKey().startsWith(EditPoiData.REMOVE_TAG_PREFIX)) {
					continue;
				}
				String text = e.getKey() + "=" + e.getValue();
				buildRow(view, R.drawable.ic_action_info_dark, null, text, 0, false, null, false, 0, false, null, false);
			}
		}

		buildRow(view, R.drawable.ic_action_get_my_location, null, PointDescription.getLocationName(app,
				osmPoint.getLatitude(), osmPoint.getLongitude(), true)
				.replaceAll("\n", " "), 0, false, null, false, 0, false, null, false);
	}
}
