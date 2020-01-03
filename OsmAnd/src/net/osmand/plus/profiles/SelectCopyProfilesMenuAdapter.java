package net.osmand.plus.profiles;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class SelectCopyProfilesMenuAdapter extends AbstractProfileMenuAdapter<SelectCopyProfilesMenuAdapter.SelectProfileViewHolder> {

	private OsmandApplication app;

	private ApplicationMode selectedAppMode;
	private List<ApplicationMode> items = new ArrayList<>();

	private boolean nightMode;

	public SelectCopyProfilesMenuAdapter(List<ApplicationMode> items, @NonNull OsmandApplication app,
	                                     boolean nightMode, @Nullable ApplicationMode selectedAppMode) {
		this.items.addAll(items);
		this.app = app;
		this.selectedAppMode = selectedAppMode;
		this.nightMode = nightMode;
	}

	@NonNull
	@Override
	public SelectProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		View itemView = inflater.inflate(R.layout.bottom_sheet_item_with_radio_btn, parent, false);
		return new SelectProfileViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull final SelectProfileViewHolder holder, int position) {
		ApplicationMode appMode = items.get(position);
		boolean selected = appMode == selectedAppMode;

		holder.title.setText(appMode.toHumanString(app));
		holder.compoundButton.setChecked(selected);

		updateViewHolder(holder, appMode, selected);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	private void updateViewHolder(SelectProfileViewHolder holder, ApplicationMode appMode, boolean selected) {
		int iconRes = appMode.getIconRes();
		if (iconRes == 0 || iconRes == -1) {
			iconRes = R.drawable.ic_action_world_globe;
		}
		int iconColor = appMode.getIconColorInfo().getColor(nightMode);
		holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, iconColor));

		int colorNoAlpha = ContextCompat.getColor(app, iconColor);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, colorNoAlpha, 0.3f);

		if (selected) {
			Drawable[] layers = {new ColorDrawable(UiUtilities.getColorWithAlpha(colorNoAlpha, 0.15f)), drawable};
			drawable = new LayerDrawable(layers);
		}
		AndroidUtils.setBackground(holder.itemView, drawable);
	}

	class SelectProfileViewHolder extends ProfileAbstractViewHolder {

		SelectProfileViewHolder(View itemView) {
			super(itemView);
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int pos = getAdapterPosition();
					if (pos != RecyclerView.NO_POSITION) {
						selectedAppMode = items.get(pos);
						if (profilePressedListener != null) {
							profilePressedListener.onProfilePressed(selectedAppMode);
							notifyDataSetChanged();
						}
					}
				}
			});
		}
	}
}