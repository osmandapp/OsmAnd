package net.osmand.plus.views.mapwidgets.configure.dialogs.cards;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;

public class ConfigureActionsCard extends MapBaseCard {

	public static final int COPY_BUTTON_INDEX = 0;
	public static final int RESET_BUTTON_INDEX = 1;

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_widgets_actions_card;
	}

	public ConfigureActionsCard(@NonNull MapActivity mapActivity) {
		super(mapActivity, false);
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.shared_string_actions);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);

		setupResetButton();
		setupCopyButton();
	}

	private void setupCopyButton() {
		View button = view.findViewById(R.id.copy_button);
		setupAction(button, R.drawable.ic_action_copy, R.string.copy_from_other_profile);
		button.setOnClickListener(v -> notifyButtonPressed(COPY_BUTTON_INDEX));
	}

	private void setupResetButton() {
		View button = view.findViewById(R.id.reset_button);
		setupAction(button, R.drawable.ic_action_reset, R.string.reset_to_default);
		button.setOnClickListener(v -> notifyButtonPressed(RESET_BUTTON_INDEX));
	}

	private void setupAction(@NonNull View view, @DrawableRes int iconId, @StringRes int titleId) {
		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);

		title.setText(titleId);
		icon.setImageDrawable(getIcon(iconId));

		ApplicationMode appMode = settings.getApplicationMode();
		View container = view.findViewById(R.id.container);
		UiUtilities.setupListItemBackground(app, container, appMode.getProfileColor(nightMode));
	}
}