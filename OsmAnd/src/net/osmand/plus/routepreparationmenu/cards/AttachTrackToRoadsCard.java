package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;


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
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_snap_to_road));

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardPressed(AttachTrackToRoadsCard.this);
				}
			}
		});
	}
}