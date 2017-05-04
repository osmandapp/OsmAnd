package net.osmand.plus.mapillary;

import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;

import org.json.JSONObject;

public class MapillaryImageCard extends ImageCard {

	public static class MapillaryImageCardFactory implements ImageCardFactory<MapillaryImageCard> {
		@Override
		public MapillaryImageCard createCard(OsmandApplication app, JSONObject imageObject) {
			return new MapillaryImageCard(app, imageObject);
		}
	}

	public MapillaryImageCard(OsmandApplication app, JSONObject imageObject) {
		super(app, imageObject);
		this.icon = app.getIconsCache().getIcon(R.drawable.ic_logo_mapillary);
		this.onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// todo
			}
		};
	}
}
