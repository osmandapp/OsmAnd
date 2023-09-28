package net.osmand.plus.views.mapwidgets.configure;

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
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListFragment;

public class ConfigureScreenActionsCard extends MapBaseCard {

	private final Fragment target;
	private final ApplicationMode appMode;
	@StringRes
	private final int screenTitleId;

	public ConfigureScreenActionsCard(@NonNull MapActivity mapActivity,
	                                  @NonNull Fragment target,
	                                  @NonNull ApplicationMode appMode,
	                                  @StringRes int screenTitleId) {
		super(mapActivity, false);
		this.target = target;
		this.appMode = appMode;
		this.screenTitleId = screenTitleId;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_widgets_actions_card;
	}

	@Override
	protected void updateContent() {
		View resetToDefaultContainer = view.findViewById(R.id.reset_to_default);
		setupAction(resetToDefaultContainer, R.drawable.ic_action_reset, R.string.reset_to_default);
		resetToDefaultContainer.setOnClickListener(v -> showResetToDefaultConfirmationDialog());

		View copyFromProfileContainer = view.findViewById(R.id.copy_from_another_profile);
		setupAction(copyFromProfileContainer, R.drawable.ic_action_copy, R.string.copy_from_other_profile);
		copyFromProfileContainer.setOnClickListener(v -> showCopyFromProfileDialog());
	}

	private void setupAction(@NonNull View view, @DrawableRes int iconId, @StringRes int titleId) {
		ImageView resetIcon = view.findViewById(R.id.icon);
		TextView resetTitle = view.findViewById(R.id.title);

		resetIcon.setImageDrawable(app.getUIUtilities().getIcon(iconId));
		resetTitle.setText(titleId);
		View container = view.findViewById(R.id.container);
		WidgetsListFragment.setupListItemBackground(app, container, appMode.getProfileColor(nightMode));
	}

	private void showResetToDefaultConfirmationDialog() {
		FragmentManager fragmentManager = target.getFragmentManager();
		if (fragmentManager != null) {
			ConfirmResetToDefaultBottomSheetDialog.showInstance(fragmentManager, target, screenTitleId);
		}
	}

	private void showCopyFromProfileDialog() {
		FragmentManager fragmentManager = target.getFragmentManager();
		if (fragmentManager != null) {
			SelectCopyAppModeBottomSheet.showInstance(fragmentManager, target, appMode);
		}
	}
}