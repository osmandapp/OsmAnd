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
import net.osmand.plus.myplaces.favorites.FavoriteFolder;
import net.osmand.plus.myplaces.favorites.FavoriteFolderFormatter;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;

public class FavoriteFolderViewHolder extends RecyclerView.ViewHolder {

	protected final OsmandApplication app;
	protected final UiUtilities uiUtilities;

	protected final TextView title;
	protected final TextView description;
	protected final ImageView icon;
	protected final CompoundButton checkbox;
	protected final View checkboxContainer;
	protected final View menuButton;
	protected final View divider;
	protected final View fullDivider;

	protected final boolean nightMode;

	public FavoriteFolderViewHolder(@NonNull View view, boolean nightMode) {
		super(view);
		this.nightMode = nightMode;
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

	public void bindView(@NonNull FavoriteGroup group, boolean showDivider, boolean showFullDivider, boolean selectionMode, FavoriteAdapterListener listener) {
		itemView.setOnLongClickListener(v -> {
			listener.onItemLongClick(group);
			return true;
		});
		itemView.setOnClickListener(v -> listener.onItemSingleClick(group));
		menuButton.setOnClickListener(v -> listener.onActionButtonClick(group, menuButton));

		title.setText(FavoriteFolderFormatter.getDisplayName(app, group.getName()));
		description.setText(GpxUiHelper.getFavoriteFolderDescription(app, group));
		bindAppearance(group);
		AndroidUiHelper.updateVisibility(divider, showDivider);
		AndroidUiHelper.updateVisibility(fullDivider, showFullDivider);

		bindSelectionMode(selectionMode, listener, group);
	}

	public void bindView(@NonNull FavoriteFolder folder, boolean showDivider, boolean showFullDivider,
	                     boolean selectionMode, FavoriteAdapterListener listener) {
		itemView.setOnLongClickListener(v -> {
			listener.onItemLongClick(folder);
			return true;
		});
		itemView.setOnClickListener(v -> listener.onItemSingleClick(folder));
		menuButton.setOnClickListener(v -> listener.onActionButtonClick(folder, menuButton));

		title.setText(FavoriteFolderFormatter.getDisplayName(app, folder.getFullPath()));
		description.setText(getFolderDescription(folder));
		bindAppearance(folder.getGroup());
		AndroidUiHelper.updateVisibility(divider, showDivider);
		AndroidUiHelper.updateVisibility(fullDivider, showFullDivider);

		bindSelectionMode(selectionMode, listener, folder);
	}

	private void bindAppearance(@Nullable FavoriteGroup group) {
		boolean visible = group == null || group.isVisible();
		title.setMaxLines(2);
		if (visible) {
			title.setTypeface(FontCache.getNormalFont());
		} else {
			title.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
		}
		int color = group == null || group.getColor() == 0 ? getColor(app, R.color.color_favorite) : group.getColor();
		int hiddenColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		if (group != null && group.isPinned()) {
			icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder_pin, group.isVisible() ? color : hiddenColor));
		} else if (visible) {
			icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder, color));
		} else {
			icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder_hidden, hiddenColor));
		}
	}

	@NonNull
	private String getFolderDescription(@NonNull FavoriteFolder folder) {
		FavoriteGroup group = folder.getGroup();
		long lastModified = folder.getSubtreeLastModified();
		int pointsCount = folder.getSubtreePointsCount();

		String empty = app.getString(R.string.shared_string_empty);
		String numberOfPoints = pointsCount > 0
				? app.getString(R.string.gpx_selection_number_of_points, String.valueOf(pointsCount))
				: empty;
		String description;
		if (lastModified > 0) {
			String formattedDate = OsmAndFormatter.getFormattedDate(app, lastModified);
			description = app.getString(R.string.ltr_or_rtl_combine_via_comma, formattedDate, numberOfPoints);
		} else {
			description = numberOfPoints;
		}
		if (group != null && !group.isVisible()) {
			String hidden = app.getString(R.string.shared_string_hidden);
			return app.getString(R.string.ltr_or_rtl_combine_via_bold_point, hidden, description);
		}
		return description;
	}

	public void bindSelectionMode(boolean selectionMode, @NonNull FavoriteAdapterListener listener, @NonNull FavoriteGroup favoriteFolder) {
		AndroidUiHelper.updateVisibility(checkboxContainer, selectionMode);
		AndroidUiHelper.updateVisibility(menuButton, !selectionMode);

		checkbox.setChecked(listener.isItemSelected(favoriteFolder));
	}

	public void bindSelectionToggle(boolean selectionMode, @NonNull FavoriteAdapterListener listener, @NonNull FavoriteGroup favoriteFolder) {
		if (selectionMode) {
			checkbox.setChecked(listener.isItemSelected(favoriteFolder));
		}
	}

	public void bindSelectionMode(boolean selectionMode, @NonNull FavoriteAdapterListener listener, @NonNull FavoriteFolder folder) {
		AndroidUiHelper.updateVisibility(checkboxContainer, selectionMode);
		AndroidUiHelper.updateVisibility(menuButton, !selectionMode);

		checkbox.setChecked(listener.isItemSelected(folder));
	}

	public void bindSelectionToggle(boolean selectionMode, @NonNull FavoriteAdapterListener listener, @NonNull FavoriteFolder folder) {
		if (selectionMode) {
			checkbox.setChecked(listener.isItemSelected(folder));
		}
	}
}
