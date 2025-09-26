package net.osmand.plus.plugins.osmedit.menu;

import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BuildRowAttrs;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.render.RenderingIcons;

import java.util.Map;

public class EditPOIMenuBuilder extends MenuBuilder {

	private final OsmPoint osmPoint;

	public EditPOIMenuBuilder(@NonNull MapActivity mapActivity, @NonNull OsmPoint osmPoint) {
		super(mapActivity);
		this.osmPoint = osmPoint;
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void buildInternal(View view) {
		if (osmPoint instanceof OsmNotesPoint notes) {
			buildRow(view, new BuildRowAttrs.Builder().setIconId(R.drawable.ic_action_note_dark).setText(notes.getText()).markLabelUndefined().build());
			buildRow(view, new BuildRowAttrs.Builder().setIconId(R.drawable.ic_group).setText(notes.getAuthor()).markLabelUndefined().build());

		} else if (osmPoint instanceof OpenstreetmapPoint point) {
			for (Map.Entry<String, String> e : point.getEntity().getTags().entrySet()) {
				if (POI_TYPE_TAG.equals(e.getKey())) {
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
					buildRow(view, new BuildRowAttrs.Builder().setIconId(resId).setText(poiTranslation).markLabelUndefined().build());
					break;
				}
			}

			for (Map.Entry<String, String> e : point.getEntity().getTags().entrySet()) {
				if (POI_TYPE_TAG.equals(e.getKey()) ||
						e.getKey().startsWith(Entity.REMOVE_TAG_PREFIX)) {
					continue;
				}
				String text = e.getKey() + "=" + e.getValue();
				buildRow(view, new BuildRowAttrs.Builder().setIconId(R.drawable.ic_action_info_dark).setText(text).markLabelUndefined().build());
			}
		}
	}
}
