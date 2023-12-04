package net.osmand.plus.download.local.dialogs.viewholders;

import static net.osmand.plus.download.local.LocalItemUtils.getFormattedDate;
import static net.osmand.plus.download.local.dialogs.LocalItemsAdapter.*;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.dialogs.livegroup.LiveGroupItemsFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.Date;

public class LiveGroupItemHolder extends RecyclerView.ViewHolder{
	private final UiUtilities uiUtilities;
	@Nullable
	private final LiveGroupItemListener listener;

	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final View options;
	private final CompoundButton compoundButton;
	private final View bottomShadow;
	private final View bottomDivider;

	public LiveGroupItemHolder(@NonNull View itemView, @Nullable LiveGroupItemListener listener, boolean nightMode) {
		super(itemView);
		OsmandApplication app = (OsmandApplication) itemView.getContext().getApplicationContext();
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
		AndroidUtils.setBackground(itemView.findViewById(R.id.selectable_list_item), drawable);
	}

	public void bindView(@NonNull LiveGroupItemsFragment.LiveGroupItem item, boolean lastItem, boolean hideDivider) {
		Context context = itemView.getContext();
		title.setText(item.name);

		String formattedDate = getFormattedDate(new Date(item.getLocalItemCreated()));
		String size = AndroidUtils.formatSize(context, item.getLocalItemSize());
		description.setText(context.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, formattedDate));
		icon.setImageDrawable(getIcon(item.localItems.get(0)));

		AndroidUiHelper.updateVisibility(compoundButton, false);

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
		AndroidUiHelper.updateVisibility(options, true);
		AndroidUiHelper.updateVisibility(bottomShadow, lastItem);
		AndroidUiHelper.updateVisibility(bottomDivider, !lastItem && !hideDivider);
	}

	@NonNull
	private Drawable getIcon(@NonNull LocalItem item) {
		LocalItemType type = item.getType();
			return uiUtilities.getThemedIcon(type.getIconId());
	}
}
