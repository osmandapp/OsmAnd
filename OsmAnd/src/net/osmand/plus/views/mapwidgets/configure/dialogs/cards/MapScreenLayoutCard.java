package net.osmand.plus.views.mapwidgets.configure.dialogs.cards;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class MapScreenLayoutCard extends MapBaseCard {

	@Override
	public int getCardLayoutId() {
		return R.layout.free_backup_card;
	}

	public MapScreenLayoutCard(@NonNull MapActivity mapActivity) {
		super(mapActivity, false);
	}

	@Override
	protected void updateContent() {
		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(R.string.map_screen_layout);
		description.setText(R.string.map_screen_layout_descr);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_map_screen_layout_portrait_colored));

		ImageView closeButton = view.findViewById(R.id.btn_close);
		closeButton.setImageDrawable(getContentIcon(R.drawable.ic_action_cancel));
		closeButton.setOnClickListener(v -> dismiss());

		DialogButton actionButton = view.findViewById(R.id.dismiss_button_container);
		actionButton.setTitle(getString(R.string.use_separate_layouts));
		actionButton.setOnClickListener(v -> {
			settings.USE_SEPARATE_LAYOUTS.set(true);
			dismiss();
		});
	}

	private void dismiss() {
		settings.MAP_SCREEN_LAYOUT_CARD_DISMISSED.set(true);
		notifyCardPressed();
	}
}