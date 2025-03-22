package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.panel.SearchWidgetListener;
import net.osmand.plus.views.mapwidgets.configure.panel.SearchWidgetsFragment.GroupItem;

public class SearchWidgetViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final TextView count;
	private final ImageView icon;
	private final AppCompatImageView proIcon;
	private final View selectableBackground;
	private final View divider;
	private final OsmandApplication app;

	public SearchWidgetViewHolder(@NonNull View itemView, @NonNull OsmandApplication app) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		count = itemView.findViewById(R.id.count);
		icon = itemView.findViewById(R.id.icon);
		proIcon = itemView.findViewById(R.id.pro_icon);
		selectableBackground = itemView.findViewById(R.id.selectable_item);
		divider = itemView.findViewById(R.id.bottom_divider);
		this.app = app;
	}

	public void bind(@NonNull ApplicationMode selectedAppMode, @NonNull SearchWidgetListener listener, WidgetType widgetType, boolean nightMode, boolean showDivider) {
		title.setText(widgetType.titleId);
		icon.setImageResource(widgetType.getIconId(nightMode));
		AndroidUiHelper.updateVisibility(count, false);
		AndroidUiHelper.updateVisibility(proIcon, !widgetType.isPurchased(app));
		divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);

		setupSelectableBackground(selectedAppMode, nightMode);
		itemView.setOnClickListener(view -> listener.widgetSelected(widgetType));
	}

	public void bind(@NonNull ApplicationMode selectedAppMode, @NonNull SearchWidgetListener listener, GroupItem groupItem, boolean nightMode, boolean showDivider) {
		title.setText(groupItem.group().titleId);
		count.setText(String.valueOf(groupItem.count()));
		AndroidUiHelper.updateVisibility(count, true);
		AndroidUiHelper.updateVisibility(proIcon, false);
		divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);

		icon.setImageResource(groupItem.group().getIconId(nightMode));

		setupSelectableBackground(selectedAppMode, nightMode);
		itemView.setOnClickListener(view -> listener.groupSelected(groupItem.group()));
	}

	public void bind(@NonNull ApplicationMode selectedAppMode, @NonNull WidgetIconsHelper iconsHelper, @NonNull SearchWidgetListener listener, @NonNull MapWidgetInfo widgetInfo, boolean nightMode, boolean showDivider) {
		title.setText(widgetInfo.getTitle(app));
		AndroidUiHelper.updateVisibility(count, false);
		AndroidUiHelper.updateVisibility(proIcon, false);
		divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);

		iconsHelper.updateWidgetIcon(icon, widgetInfo);

		setupSelectableBackground(selectedAppMode, nightMode);
		itemView.setOnClickListener(view -> listener.externalWidgetSelected(widgetInfo));
	}

	private void setupSelectableBackground(@NonNull ApplicationMode selectedAppMode, boolean nightMode) {
		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(selectableBackground, drawable);
	}

	public void updateDivider(boolean showDivider) {
		if (divider.getVisibility() != (showDivider ? View.VISIBLE : View.INVISIBLE)) {
			divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);
		}
	}
}