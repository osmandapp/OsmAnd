package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;

public class LeftTextInfoWidget extends TextInfoWidget {

	public LeftTextInfoWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public void updateColors(@NonNull TextState textState) {
		super.updateColors(textState);
		view.setBackgroundResource(textState.leftRes);
	}
}