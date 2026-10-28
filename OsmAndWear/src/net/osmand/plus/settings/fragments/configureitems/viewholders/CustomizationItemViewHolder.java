package net.osmand.plus.settings.fragments.configureitems.viewholders;

import static net.osmand.plus.settings.fragments.configureitems.ScreenType.CONFIGURE_MAP;
import static net.osmand.plus.settings.fragments.configureitems.ScreenType.DRAWER;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.configmap.ConfigureMapMenu;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.configureitems.ScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.Iterator;
import java.util.List;

public class CustomizationItemViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final CallbackWithObject<ScreenType> callback;
	private final boolean nightMode;

	private final ImageView icon;
	private final TextView title;
	private final TextView subTitle;

	public CustomizationItemViewHolder(@NonNull View itemView, @Nullable CallbackWithObject<ScreenType> callback, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();
		this.callback = callback;
		this.nightMode = nightMode;

		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
		subTitle = itemView.findViewById(R.id.sub_title);
	}

	public void bindView(@NonNull ScreenType screenType, @NonNull FragmentActivity activity, @NonNull ApplicationMode appMode) {
		Drawable drawable = uiUtilities.getIcon(screenType.iconId, nightMode);
		icon.setImageDrawable(AndroidUtils.getDrawableForDirection(app, drawable));
		title.setText(screenType.titleId);
		subTitle.setText(getSubTitleText(screenType, activity, appMode));
		itemView.setOnClickListener(view -> callback.processResult(screenType));
	}

	@NonNull
	private String getSubTitleText(@NonNull ScreenType screenType, @NonNull FragmentActivity activity, @NonNull ApplicationMode appMode) {
		if (activity instanceof MapActivity) {
			int allCount = getCustomizableItems(screenType, activity).size();
			int hiddenCount = ContextMenuUtils.getSettingForScreen(app, screenType).getModeValue(appMode).getHiddenIds().size();
			String amount = app.getString(R.string.n_items_of_z, String.valueOf(allCount - hiddenCount), String.valueOf(allCount));
			return app.getString(R.string.shared_string_items) + " : " + amount;
		}
		return "";
	}

	@NonNull
	public List<ContextMenuItem> getCustomizableItems(@NonNull ScreenType screenType, @NonNull FragmentActivity activity) {
		ContextMenuAdapter adapter = getContextMenuAdapter(screenType, activity);
		List<ContextMenuItem> items = ContextMenuUtils.getCustomizableItems(adapter);
		if (screenType == DRAWER || screenType == CONFIGURE_MAP) {
			Iterator<ContextMenuItem> iterator = items.iterator();
			while (iterator.hasNext()) {
				String id = iterator.next().getId();
				if (ContextMenuUtils.isCategoryItem(id)) {
					iterator.remove();
				}
			}
		}
		return items;
	}

	@NonNull
	private ContextMenuAdapter getContextMenuAdapter(@NonNull ScreenType screenType, @NonNull FragmentActivity activity) {
		switch (screenType) {
			case DRAWER:
				MapActivityActions actions = new MapActivityActions((MapActivity) activity);
				return actions.createMainOptionsMenu();
			case CONFIGURE_MAP:
				ConfigureMapMenu configureMenu = new ConfigureMapMenu(app);
				return configureMenu.createListAdapter((MapActivity) activity);
			case CONTEXT_MENU_ACTIONS:
				MapContextMenu contextMenu = ((MapActivity) activity).getContextMenu();
				return contextMenu.getActionsContextMenuAdapter(true);
		}
		throw new IllegalArgumentException("Unsupported screenType");
	}
}
