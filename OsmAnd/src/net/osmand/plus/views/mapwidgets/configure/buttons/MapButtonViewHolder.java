package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class MapButtonViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final UiUtilities uiUtilities;
	private final boolean nightMode;

	private final ImageView icon;
	private final TextView title;
	private final TextView description;
	private final View shortDivider;

	public MapButtonViewHolder(@NonNull View itemView, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
		this.nightMode = nightMode;

		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		shortDivider = itemView.findViewById(R.id.short_divider);
	}

	public void bindView(@NonNull MapButtonState buttonState, boolean lastItem) {
		boolean enabled = buttonState.isEnabled();
		title.setText(buttonState.getName());
		description.setText(getDescription(buttonState, enabled));

		ApplicationMode appMode = settings.getApplicationMode();
		int color = enabled ? appMode.getProfileColor(nightMode) : ColorUtilities.getDefaultIconColor(app, nightMode);
		icon.setImageDrawable(buttonState.getIcon(nightMode, false, color));

		setupListItemBackground(appMode, nightMode);
		AndroidUiHelper.updateVisibility(description, true);
		AndroidUiHelper.updateVisibility(shortDivider, !lastItem);
	}

	@NonNull
	private String getDescription(@NonNull MapButtonState buttonState, boolean enabled) {
		if (buttonState instanceof Map3DButtonState) {
			Map3DButtonState map3DButtonState = (Map3DButtonState) buttonState;
			return app.getString(map3DButtonState.getVisibility().getTitleId());
		} else if (buttonState instanceof CompassButtonState) {
			CompassButtonState compassButtonState = (CompassButtonState) buttonState;
			return app.getString(compassButtonState.getVisibility().getTitleId());
		}
		return app.getString(enabled ? R.string.shared_string_on : R.string.shared_string_off);
	}

	private void setupListItemBackground(@NonNull ApplicationMode mode, boolean nightMode) {
		int color = mode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(itemView.findViewById(R.id.button_container), background);
	}
}