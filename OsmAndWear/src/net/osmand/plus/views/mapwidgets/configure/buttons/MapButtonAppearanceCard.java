package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.MapButtonAppearanceFragment;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class MapButtonAppearanceCard extends MapBaseCard {

	private final MapButtonState buttonState;

	public MapButtonAppearanceCard(@NonNull MapActivity mapActivity, @NonNull MapButtonState buttonState) {
		super(mapActivity, false);
		this.buttonState = buttonState;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_screen_list_item;
	}

	@Override
	protected void updateContent() {
		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(R.string.shared_string_appearance);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_appearance));
		description.setText(buttonState.hasCustomAppearance() ? R.string.shared_string_custom : R.string.shared_string_default);

		View container = view.findViewById(R.id.button_container);
		container.setOnClickListener(v -> showAppearanceDialog());

		AndroidUiHelper.updateVisibility(description, true);
		view.setBackgroundColor(ColorUtilities.getCardAndListBackgroundColor(app, nightMode));
		UiUtilities.setupListItemBackground(view.getContext(), container, settings.getApplicationMode().getProfileColor(nightMode));
	}

	private void showAppearanceDialog() {
		FragmentManager manager = activity.getSupportFragmentManager();
		MapButtonAppearanceFragment.showInstance(manager, buttonState);
	}
}