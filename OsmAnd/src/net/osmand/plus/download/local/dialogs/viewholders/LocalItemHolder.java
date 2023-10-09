package net.osmand.plus.download.local.dialogs.viewholders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.dialogs.LocalItemsFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class LocalItemHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final LocalItemsFragment fragment;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final View options;
	private final CompoundButton compoundButton;
	private final View bottomShadow;
	private final View bottomDivider;

	public LocalItemHolder(@NonNull View itemView, @NonNull LocalItemsFragment fragment) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();
		this.fragment = fragment;
		this.nightMode = fragment.isNightMode();

		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		options = itemView.findViewById(R.id.options);
		compoundButton = itemView.findViewById(R.id.compound_button);
		bottomShadow = itemView.findViewById(R.id.bottom_shadow);
		bottomDivider = itemView.findViewById(R.id.bottom_divider);

		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		UiUtilities.setupCompoundButton(nightMode, activeColor, compoundButton);
		AndroidUtils.setBackground(itemView.findViewById(R.id.selectable_list_item), drawable);
	}

	public void bindView(@NonNull LocalItem item, boolean selectionMode, boolean lastItem, boolean hideDivider) {
		Context context = itemView.getContext();
		title.setText(item.getName(context));
		description.setText(item.getDescription(context));
		icon.setImageDrawable(getIcon(item));

		boolean selected = fragment != null && fragment.isItemSelected(item);
		compoundButton.setChecked(selected);

		options.setOnClickListener(v -> {
			if (fragment != null) {
				fragment.onItemOptionsSelected(item, options);
			}
		});
		itemView.setOnClickListener(v -> {
			if (fragment != null) {
				fragment.onItemSelected(item);
			}
		});
		AndroidUiHelper.updateVisibility(options, !selectionMode);
		AndroidUiHelper.updateVisibility(compoundButton, selectionMode);
		AndroidUiHelper.updateVisibility(bottomShadow, lastItem);
		AndroidUiHelper.updateVisibility(bottomDivider, !lastItem && !hideDivider);
	}

	@NonNull
	private Drawable getIcon(@NonNull LocalItem item) {
		LocalItemType type = item.getType();
		if (type.isDownloadType() && !item.isBackuped()) {
			boolean shouldUpdate = fragment.getItemsToUpdate().containsKey(item.getFile().getName());
			return uiUtilities.getIcon(type.getIconId(), shouldUpdate ? R.color.color_distance : R.color.color_ok);
		} else {
			return uiUtilities.getThemedIcon(type.getIconId());
		}
	}
}