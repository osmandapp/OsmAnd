package net.osmand.plus.track.cards;

import static net.osmand.plus.track.cards.AuthorCard.NO_ICON;
import static net.osmand.plus.track.cards.AuthorCard.fillCardItems;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.util.Algorithms;

import java.util.Map;

public class ExtensionsCard extends MapBaseCard {
	private final GPXFile gpxFile;
	private final boolean nightMode;

	public ExtensionsCard(@NonNull MapActivity mapActivity, @NonNull GPXFile gpxFile, boolean nightMode) {
		super(mapActivity);
		this.gpxFile = gpxFile;
		this.nightMode = nightMode;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.extensions_card;
	}

	@Override
	public void updateContent() {
		Map<String, String> extensions = gpxFile.metadata.extensions;
		updateVisibility(!Algorithms.isEmpty(extensions));
		ViewGroup container = view.findViewById(R.id.extensions_container);
		OsmandApplication app = mapActivity.getMyApplication();

		for (String key : extensions.keySet()) {
			LinearLayout extensionView = (LinearLayout) LayoutInflater.from(view.getContext()).inflate(R.layout.item_with_title_desc, null);
			container.addView(extensionView);
			fillCardItems(app, view, nightMode, extensionView, NO_ICON, extensions.get(key), key, false, true);
		}
	}
}