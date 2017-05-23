package net.osmand.plus.osmedit;

import android.view.View;

import net.osmand.data.PointDescription;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
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

			buildRow(view, R.drawable.ic_action_note_dark, notes.getText(), 0, false, null, false, 0, false, null);
			buildRow(view, R.drawable.ic_group, notes.getAuthor(), 0, false, null, false, 0, false, null);

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
					buildRow(view, resId, poiTranslation, 0, false, null, false, 0, false, null);
					break;
				}
			}

			for (Map.Entry<String, String> e : point.getEntity().getTags().entrySet()) {
				if (EditPoiData.POI_TYPE_TAG.equals(e.getKey()) || 
						e.getKey().startsWith(EditPoiData.REMOVE_TAG_PREFIX)) {
					continue;
				}
				String text = e.getKey() + "=" + e.getValue();
				buildRow(view, R.drawable.ic_action_info_dark, text, 0, false, null, false, 0, false, null);
			}
		}

		OsmandSettings st = ((OsmandApplication) mapActivity.getApplicationContext()).getSettings();
		buildRow(view, R.drawable.ic_action_get_my_location, PointDescription.getLocationName(app,
				osmPoint.getLatitude(), osmPoint.getLongitude(), true)
				.replaceAll("\n", " "), 0, false, null, false, 0, false, null);
		//if (st.COORDINATES_FORMAT.get() != PointDescription.OLC_FORMAT)
		//	buildRow(view, R.drawable.ic_action_get_my_location, PointDescription.getLocationOlcName(
		//			osmPoint.getLatitude(), osmPoint.getLongitude())
		//			.replaceAll("\n", " "), 0, false, null, false, 0, false, null);
	}
}
