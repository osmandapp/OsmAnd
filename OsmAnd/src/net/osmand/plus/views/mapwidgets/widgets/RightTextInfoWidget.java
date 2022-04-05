package net.osmand.plus.views.mapwidgets.widgets;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;

public class RightTextInfoWidget extends TextInfoWidget {

	private final View bottomDivider;

	public RightTextInfoWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		bottomDivider = view.findViewById(R.id.bottom_divider);
	}

	@Override
	public void updateColors(@NonNull TextState textState) {
		super.updateColors(textState);
		view.setBackgroundResource(textState.rightRes);
		bottomDivider.setBackgroundResource(textState.rightDividerColorId);
	}
}