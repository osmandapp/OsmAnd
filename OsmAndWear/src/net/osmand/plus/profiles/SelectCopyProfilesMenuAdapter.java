package net.osmand.plus.profiles;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class SelectCopyProfilesMenuAdapter extends AbstractProfileMenuAdapter<SelectCopyProfilesMenuAdapter.SelectProfileViewHolder> {

	private final OsmandApplication app;

	private ApplicationMode selectedAppMode;
	private final List<ApplicationMode> items = new ArrayList<>();

	private final boolean nightMode;

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
	public void onBindViewHolder(@NonNull SelectProfileViewHolder holder, int position) {
		ApplicationMode appMode = items.get(position);
		boolean selected = appMode == selectedAppMode;

		holder.title.setText(appMode.toHumanString());
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
		int colorNoAlpha = appMode.getProfileColor(nightMode);

		holder.icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconRes, colorNoAlpha));
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, colorNoAlpha, 0.3f);

		if (selected) {
			Drawable[] layers = {new ColorDrawable(ColorUtilities.getColorWithAlpha(colorNoAlpha, 0.15f)), drawable};
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