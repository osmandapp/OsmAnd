package net.osmand.plus.download.local.dialogs.viewholders;

import static net.osmand.plus.download.local.LocalItemType.TERRAIN_DATA;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.dialogs.LocalItemsAdapter.LocalItemListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class LocalItemHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final LocalItemListener listener;

	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final View options;
	private final CompoundButton compoundButton;
	private final View bottomShadow;
	private final View bottomDivider;

	public LocalItemHolder(@NonNull View itemView, @NonNull LocalItemListener listener, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();
		this.listener = listener;

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

	public void bindView(@NonNull BaseLocalItem item, boolean selectionMode, boolean lastItem, boolean hideDivider) {
		Context context = itemView.getContext();
		title.setText(item.getName(context));
		description.setText(item.getDescription(context));
		icon.setImageDrawable(getIcon(item));

		boolean selected = listener != null && listener.isItemSelected(item);
		compoundButton.setChecked(selected);

		options.setOnClickListener(v -> {
			if (listener != null) {
				listener.onItemOptionsSelected(item, options);
			}
		});
		itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.onItemSelected(item);
			}
		});
		AndroidUiHelper.updateVisibility(options, !selectionMode);
		AndroidUiHelper.updateVisibility(compoundButton, selectionMode);
		AndroidUiHelper.updateVisibility(bottomShadow, lastItem);
		AndroidUiHelper.updateVisibility(bottomDivider, !lastItem && !hideDivider);
	}

	@NonNull
	private Drawable getIcon(@NonNull BaseLocalItem item) {
		int iconId = getIconId(item);
		LocalItemType type = item.getType();
		if (item instanceof LocalItem) {
			LocalItem localItem = (LocalItem) item;
			if (type.isDownloadType() && !localItem.isBackuped(app)) {
				boolean shouldUpdate = listener.itemUpdateAvailable(localItem);
				return uiUtilities.getIcon(iconId, shouldUpdate ? R.color.color_distance : R.color.color_ok);
			}
		}
		return uiUtilities.getThemedIcon(iconId);
	}

	@DrawableRes
	private int getIconId(@NonNull BaseLocalItem item) {
		LocalItemType type = item.getType();
		if (item instanceof LocalItem) {
			LocalItem localItem = (LocalItem) item;
			if (type == TERRAIN_DATA && SrtmDownloadItem.isSrtmFile(localItem.getFileName())) {
				return R.drawable.ic_plugin_srtm;
			}
		}
		return type.getIconId();
	}
}