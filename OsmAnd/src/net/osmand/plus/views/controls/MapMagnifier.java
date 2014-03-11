package net.osmand.plus.views.controls;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;

public class MapMagnifier {

	public static View.OnLongClickListener getOnClickMagnifierListener(final OsmandMapTileView view) {
		final View.OnLongClickListener listener = new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View notUseCouldBeNull) {
				final OsmandSettings.OsmandPreference<Float> zoomScale = view.getSettings().MAP_ZOOM_SCALE_BY_DENSITY;
				final AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
				float scale = view.getZoomScale();
				int p = (int) ((scale > 0 ? 1 : -1) * Math.round(scale * scale * 100)) + 100;
				final TIntArrayList tlist = new TIntArrayList(new int[] { 75, 100, 150, 200, 300, 400, 500 });
				final List<String> values = new ArrayList<String>();
				int i = -1;
				for (int k = 0; k <= tlist.size(); k++) {
					final boolean end = k == tlist.size();
					if (i == -1) {
						if ((end || p < tlist.get(k))) {
							values.add(p + "%");
							i = k;
						} else if (p == tlist.get(k)) {
							i = k;
						}

					}
					if (k < tlist.size()) {
						values.add(tlist.get(k) + "%");
					}
				}
				if (values.size() != tlist.size()) {
					tlist.insert(i, p);
				}

				bld.setTitle(R.string.map_magnifier);
				bld.setSingleChoiceItems(values.toArray(new String[values.size()]), i,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								int p = tlist.get(which);
								float newScale;
								if (p >= 100) {
									newScale = (float) Math.sqrt((tlist.get(which) - 100f) / 100f);
								} else {
									newScale = -(float) Math.sqrt((100f - tlist.get(which)) / 100f);
								}
								zoomScale.set(newScale - (float) Math.sqrt(Math.max(view.getDensity() - 1, 0)));
								view.getAnimatedDraggingThread().startZooming(view.getZoom(),
										view.getSettingsZoomScale(), false);
								dialog.dismiss();
							}
						});
				bld.show();
				return true;
			}
		};
		return listener;
	}
}
