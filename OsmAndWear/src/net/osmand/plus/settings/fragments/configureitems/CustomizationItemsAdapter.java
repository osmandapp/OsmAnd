package net.osmand.plus.settings.fragments.configureitems;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.configureitems.viewholders.CustomizationDescriptionViewHolder;
import net.osmand.plus.settings.fragments.configureitems.viewholders.CustomizationItemViewHolder;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

class CustomizationItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


	private static final int ITEM_TYPE = 0;
	private static final int DESCRIPTION_TYPE = 1;

	private final FragmentActivity activity;

	private final List<Object> items;
	private final ApplicationMode appMode;
	private final CallbackWithObject<ScreenType> callback;

	private final LayoutInflater themedInflater;
	private final boolean nightMode;

	CustomizationItemsAdapter(@NonNull FragmentActivity activity, @NonNull List<Object> items,
	                          @NonNull ApplicationMode appMode, boolean nightMode,
	                          @Nullable CallbackWithObject<ScreenType> callback) {
		this.activity = activity;
		this.items = items;
		this.appMode = appMode;
		this.callback = callback;
		this.nightMode = nightMode;
		this.themedInflater = UiUtilities.getInflater(activity, nightMode);
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		if (viewType == DESCRIPTION_TYPE) {
			View view = themedInflater.inflate(R.layout.list_item_description_with_image, parent, false);
			return new CustomizationDescriptionViewHolder(view, activity);

		} else {
			View view = themedInflater.inflate(R.layout.list_item_ui_customization, parent, false);
			return new CustomizationItemViewHolder(view, callback, nightMode);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object item = items.get(position);
		if (holder instanceof CustomizationDescriptionViewHolder) {
			CustomizationDescriptionViewHolder viewHolder = (CustomizationDescriptionViewHolder) holder;
			viewHolder.bindView((String) item, nightMode);
		} else {
			ScreenType screenType = (ScreenType) item;
			CustomizationItemViewHolder viewHolder = (CustomizationItemViewHolder) holder;
			viewHolder.bindView(screenType, activity, appMode);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof String) {
			return DESCRIPTION_TYPE;
		} else if (object instanceof ScreenType) {
			return ITEM_TYPE;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}
}