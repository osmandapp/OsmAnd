package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class TracksGroupViewHolder extends RecyclerView.ViewHolder {

	protected final OsmandApplication app;
	protected final UiUtilities uiUtilities;

	protected final TextView title;
	protected final TextView description;
	protected final ImageView icon;
	protected final CompoundButton checkbox;
	protected final View checkboxContainer;
	protected final View menuButton;

	protected final boolean nightMode;

	public TracksGroupViewHolder(@NonNull View view, boolean nightMode, boolean selectionMode) {
		super(view);
		this.nightMode = nightMode;
		app = (OsmandApplication) itemView.getContext().getApplicationContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();

		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);
		checkbox = itemView.findViewById(R.id.checkbox);
		checkboxContainer = view.findViewById(R.id.checkbox_container);
		menuButton = view.findViewById(R.id.menu_button);

		setupSelectionMode(selectionMode);
	}

	private void setupSelectionMode(boolean selectionMode) {
		AndroidUiHelper.updateVisibility(menuButton, false);
		AndroidUiHelper.updateVisibility(checkboxContainer, selectionMode);
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), checkbox);
	}
}