package net.osmand.plus.myplaces;

import net.osmand.plus.GpxSelectionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 04.03.2015.
 */
public class TrackSegmentFragment extends SelectedGPXFragment {

	@Override
	public void setContent() {
		List<GpxSelectionHelper.GpxDisplayGroup> groups = filterGroups();
		lightContent = app.getSettings().isLightContent();


		List<GpxSelectionHelper.GpxDisplayItem> items = new ArrayList<>();
		for (GpxSelectionHelper.GpxDisplayGroup group : groups) {
			if (group.getType() != GpxSelectionHelper.GpxDisplayItemType.TRACK_SEGMENT){
				continue;
			}
			for (GpxSelectionHelper.GpxDisplayItem item : group.getModifiableList()) {
				items.add(item);
			}
		}
		adapter = new SelectedGPXAdapter(items);
		setListAdapter(adapter);
	}
}
