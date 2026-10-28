package net.osmand.plus.routepreparationmenu.cards;

import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import androidx.annotation.NonNull;


public class AttachTrackToRoadsCard extends MapBaseCard {

	public AttachTrackToRoadsCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_simple;
	}

	@Override
	protected void updateContent() {
		TextView titleTv = view.findViewById(R.id.title);
		titleTv.setText(R.string.attach_to_the_roads);

		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_attach_track));

		view.setOnClickListener(v -> notifyCardPressed());
	}
}