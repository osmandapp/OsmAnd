package net.osmand.plus.quickaction;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

import static net.osmand.plus.quickaction.AddQuickActionsAdapter.CATEGORY_MODE;
import static net.osmand.plus.quickaction.AddQuickActionsAdapter.SEARCH_MODE;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.AddQuickActionsAdapter.QuickActionAdapterMode;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;


public class QuickActionViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final boolean nightMode;

	private final ImageView iconView;
	private final TextView title;
	private final TextView description;
	private final TextView itemsCountView;
	private final View shortDivider;

	public QuickActionViewHolder(@NonNull View itemView, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		settings = app.getSettings();
		this.nightMode = nightMode;

		iconView = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		itemsCountView = itemView.findViewById(R.id.items_count_descr);
		shortDivider = itemView.findViewById(R.id.short_divider);
		itemView.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
	}

	public void bindView(@NonNull QuickActionType type, int itemsCount, boolean lastItem, @QuickActionAdapterMode int mode) {
		boolean showActionNameRes = mode == SEARCH_MODE || mode == CATEGORY_MODE;
		if (showActionNameRes) {
			int actionNameRes = type.getActionNameRes();
			if (actionNameRes != 0) {
				title.setText(app.getString(type.getActionNameRes()));
				title.setTextSize(COMPLEX_UNIT_SP, 14);
				title.setTextColor(ColorUtilities.getActiveColor(app, nightMode));
			} else {
				showActionNameRes = false;
			}

			description.setText(app.getString(type.getNameRes()));
			description.setTextSize(COMPLEX_UNIT_SP, 16);
			description.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		} else {
			title.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
			title.setTextSize(COMPLEX_UNIT_SP, 16);
			title.setText(type.getFullName(app));
		}

		ApplicationMode appMode = settings.getApplicationMode();
		int iconRes = type.getIconRes();
		if (iconRes != 0) {
			Drawable icon = UiUtilities.createTintedDrawable(app, type.getIconRes(), ColorUtilities.getDefaultIconColor(app, nightMode));
			iconView.setImageDrawable(icon);
		}
		itemsCountView.setText(String.valueOf(itemsCount));

		setupListItemBackground(appMode, nightMode);
		AndroidUiHelper.updateVisibility(itemsCountView, itemsCount != 0);
		AndroidUiHelper.updateVisibility(description, showActionNameRes);
		AndroidUiHelper.updateVisibility(shortDivider, !lastItem);
	}

	private void setupListItemBackground(@NonNull ApplicationMode mode, boolean nightMode) {
		int color = mode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(itemView.findViewById(R.id.button_container), background);
	}
}