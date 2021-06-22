package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.OtherLocalRoutingParameter;

public class ReverseTrackCard extends MapBaseCard {

	private LocalRoutingParameter parameter;

	public ReverseTrackCard(@NonNull MapActivity mapActivity, boolean isReverse) {
		super(mapActivity);
		int textId = R.string.gpx_option_reverse_route;
		String title = app.getString(textId);
		this.parameter = new OtherLocalRoutingParameter(textId, title, isReverse);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_with_switch;
	}

	@Override
	protected void updateContent() {
		TextView titleTv = view.findViewById(R.id.title);
		titleTv.setText(R.string.gpx_option_reverse_route);

		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_change_navigation_points));

		final CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(parameter.isSelected(app.getSettings()));
		UiUtilities.setupCompoundButton(nightMode, getActiveColor(), compoundButton);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean selected = !parameter.isSelected(app.getSettings());
				compoundButton.setChecked(selected);
				app.getRoutingOptionsHelper().applyRoutingParameter(parameter, selected);

				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardPressed(ReverseTrackCard.this);
				}
			}
		});
	}
}