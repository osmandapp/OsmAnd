package net.osmand.plus.views.mapwidgets.configure.dialogs.cards;

import static net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.showResetSettingsDialog;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListFragment;

public class ConfigureActionsCard extends MapBaseCard {

	private final Fragment target;
	@StringRes
	private final int screenTitleId;

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_widgets_actions_card;
	}

	public ConfigureActionsCard(@NonNull MapActivity mapActivity, @NonNull Fragment target, @StringRes int screenTitleId) {
		super(mapActivity, false);
		this.target = target;
		this.screenTitleId = screenTitleId;
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
		button.setOnClickListener(v -> {
			FragmentManager manager = target.getFragmentManager();
			if (manager != null) {
				ApplicationMode appMode = settings.getApplicationMode();
				SelectCopyAppModeBottomSheet.showInstance(manager, target, appMode);
			}
		});
	}

	private void setupResetButton() {
		View button = view.findViewById(R.id.reset_button);
		setupAction(button, R.drawable.ic_action_reset, R.string.reset_to_default);
		button.setOnClickListener(v -> {
			FragmentManager manager = target.getFragmentManager();
			if (manager != null) {
				showResetSettingsDialog(manager, target, screenTitleId);
			}
		});
	}

	private void setupAction(@NonNull View view, @DrawableRes int iconId, @StringRes int titleId) {
		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);

		title.setText(titleId);
		icon.setImageDrawable(getIcon(iconId));

		ApplicationMode appMode = settings.getApplicationMode();
		View container = view.findViewById(R.id.container);
		WidgetsListFragment.setupListItemBackground(app, container, appMode.getProfileColor(nightMode));
	}
}