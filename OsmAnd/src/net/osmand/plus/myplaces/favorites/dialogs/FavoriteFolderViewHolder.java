package net.osmand.plus.myplaces.favorites.dialogs;

import static net.osmand.plus.utils.ColorUtilities.getColor;

import android.graphics.Typeface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TracksGroupViewHolder.TrackGroupsListener;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;

public class FavoriteFolderViewHolder extends RecyclerView.ViewHolder {

	protected final OsmandApplication app;
	protected final UiUtilities uiUtilities;
	protected final TrackGroupsListener listener;

	protected final TextView title;
	protected final TextView description;
	protected final ImageView icon;
	protected final CompoundButton checkbox;
	protected final View checkboxContainer;
	protected final View menuButton;
	protected final View divider;
	protected final View fullDivider;

	protected final boolean nightMode;
	protected final boolean selectionMode;

	public FavoriteFolderViewHolder(@NonNull View view, @Nullable TrackGroupsListener listener,
	                                boolean nightMode, boolean selectionMode) {
		super(view);
		this.listener = listener;
		this.nightMode = nightMode;
		this.selectionMode = selectionMode;
		app = (OsmandApplication) view.getContext().getApplicationContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();

		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);
		checkbox = view.findViewById(R.id.checkbox);
		checkboxContainer = view.findViewById(R.id.checkbox_container);
		menuButton = view.findViewById(R.id.menu_button);
		divider = view.findViewById(R.id.divider);
		fullDivider = view.findViewById(R.id.full_divider);

		setupSelectionMode();
	}

	private void setupSelectionMode() {
		AndroidUiHelper.updateVisibility(menuButton, true);
		AndroidUiHelper.updateVisibility(checkboxContainer, false);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.direction_icon), false);
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), checkbox);
	}

	public void bindView(@NonNull FavoriteGroup group, boolean showDivider, boolean showFullDivider, FavoriteAdapterListener listener) {
		itemView.setOnLongClickListener(v -> {
			listener.onItemLongClick(group);
			return true;
		});
		itemView.setOnClickListener(v -> listener.onItemSingleClick(group));
		menuButton.setOnClickListener(v -> listener.onActionButtonClick(group, menuButton));

		boolean visible = group.isVisible();

		title.setText(group.getDisplayName(app));
		title.setMaxLines(2);
		if (visible) {
			title.setTypeface(FontCache.getNormalFont());
		} else {
			title.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
		}

		description.setText(GpxUiHelper.getFavoriteFolderDescription(app, group));

		int color = group.getColor() == 0 ? getColor(app, R.color.color_favorite) : group.getColor();
		int hiddenColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		if (!group.isPersonal()) {
			if (group.isPinned()) {
				icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder_pin, group.isVisible() ? color : hiddenColor));
			} else if (group.isVisible()) {
				icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder, color));
			} else {
				icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder_hidden, hiddenColor));
			}
		}
		AndroidUiHelper.updateVisibility(divider, showDivider);
		AndroidUiHelper.updateVisibility(fullDivider, showFullDivider);
	}

	public void bindSelectionMode(boolean selectionMode, @NonNull FavoriteAdapterListener listener, @NonNull FavoriteGroup trackFolder) {
		AndroidUiHelper.updateVisibility(checkboxContainer, selectionMode);
		AndroidUiHelper.updateVisibility(menuButton, !selectionMode);

		checkbox.setChecked(listener.isItemSelected(trackFolder));
	}

	public void bindSelectionToggle(boolean selectionMode, @NonNull FavoriteAdapterListener listener, @NonNull FavoriteGroup trackFolder) {
		if (selectionMode) {
			checkbox.setChecked(listener.isItemSelected(trackFolder));
		}
	}
}
