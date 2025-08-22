package net.osmand.plus.mapcontextmenu.editors.icon;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.RequestMapThemeParams;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconSearchResult;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.FlowLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EditorIconScreenAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	public static final int CATEGORY_ICONS = 1;
	public static final int ICON_SEARCH_RESULT = 2;

	private final OsmandApplication app;
	private final UiUtilities iconsCache;
	private final ApplicationMode appMode;
	private final IconsPaletteElements<String> paletteElements;
	private ViewGroup parent;
	private Context context;

	private List<ScreenItem> screenItems = new ArrayList<>();
	private final boolean usedOnMap;
	private final EditorIconScreenController controller;

	public EditorIconScreenAdapter(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode,
	                               @NonNull EditorIconScreenController controller, boolean usedOnMap) {
		this.app = mapActivity.getMyApplication();
		this.iconsCache = app.getUIUtilities();
		this.appMode = appMode;
		this.usedOnMap = usedOnMap;
		this.controller = controller;
		this.paletteElements = controller.getPaletteElements(mapActivity, isNightMode());
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		this.parent = parent;
		context = parent.getContext();
		switch (viewType) {
			case CATEGORY_ICONS:
				return new IconsCategoryViewHolder(inflate(R.layout.card_icons_by_category));
			case ICON_SEARCH_RESULT:
				return new IconSearchResultViewHolder(inflate(R.layout.list_item_icon_search_result));
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		int itemType = getItemViewType(position);
		boolean lastItem = position == getItemCount() - 1;
		ScreenItem screenItem = screenItems.get(position);
		if (itemType == CATEGORY_ICONS) {
			IconsCategoryViewHolder h = (IconsCategoryViewHolder) holder;
			IconsCategory category = (IconsCategory) screenItem.getValue();
			h.title.setText(category.getTranslation());
			h.iconsContainer.removeAllViews();
			h.iconsContainer.setHorizontalAutoSpacing(true);
			for (String icon : category.getIconKeys()) {
				h.iconsContainer.addView(createIconItemView(icon, h.iconsContainer, category.getKey()));
			}
			AndroidUiHelper.updateVisibility(h.bottomDivider, !lastItem);
		} else if (itemType == ICON_SEARCH_RESULT) {
			IconSearchResultViewHolder h = (IconSearchResultViewHolder) holder;
			IconSearchResult searchResult = (IconSearchResult) screenItem.getValue();
			h.icon.setImageDrawable(iconsCache.getThemedIcon(searchResult.getIconId()));
			h.title.setText(searchResult.getIconName());
			h.description.setText(searchResult.getCategoryName());
			h.itemView.setOnClickListener(v -> controller.onIconSelectedFromPalette(searchResult.getIconKey(), searchResult.getCategoryKey()));
			AndroidUiHelper.updateVisibility(h.bottomDivider, !lastItem);
		}
	}

	@NonNull
	private View createIconItemView(@NonNull String iconKey,
	                                @NonNull FlowLayout rootView,
	                                @NonNull String categoryKey) {
		View view = paletteElements.createView(rootView);
		boolean isSelected = controller.isSelectedIcon(iconKey);
		int controlsColor = controller.getControlsAccentColor();
		paletteElements.bindView(view, iconKey, controlsColor, isSelected);

		view.setOnClickListener(v -> {
			controller.onIconSelectedFromPalette(iconKey, categoryKey);
		});
		view.setTag(iconKey);
		return view;
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setScreenData(@NonNull List<ScreenItem> screenItems) {
		this.screenItems = screenItems;
		notifyDataSetChanged();
	}

	public int positionOfValue(@NonNull Object value) {
		for (int i = 0; i < screenItems.size(); i++) {
			if (Objects.equals(screenItems.get(i).value, value)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public int getItemCount() {
		return screenItems.size();
	}

	@Override
	public int getItemViewType(int position) {
		return screenItems.get(position).getType();
	}

	@Override
	public long getItemId(int position) {
		return screenItems.get(position).getId();
	}

	private View inflate(@LayoutRes int layoutResId) {
		LayoutInflater inflater = UiUtilities.getInflater(context, isNightMode());
		return inflater.inflate(layoutResId, parent, false);
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap, new RequestMapThemeParams().setAppMode(appMode));
	}

	static class IconsCategoryViewHolder extends ViewHolder {
		private final TextView title;
		private final FlowLayout iconsContainer;
		private final View bottomDivider;

		public IconsCategoryViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.category_title);
			iconsContainer = itemView.findViewById(R.id.icons_selector);
			bottomDivider = itemView.findViewById(R.id.bottom_divider);
		}
	}

	static class IconSearchResultViewHolder extends ViewHolder {
		private final ImageView icon;
		private final TextView title;
		private final TextView description;
		private final View bottomDivider;

		public IconSearchResultViewHolder(@NonNull View itemView) {
			super(itemView);
			icon = itemView.findViewById(R.id.icon);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			bottomDivider = itemView.findViewById(R.id.bottom_divider);
		}
	}
}