package net.osmand.plus.myplaces.favorites.dialogs;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class FavoriteEmptyFolderVHolder extends RecyclerView.ViewHolder {

	protected final OsmandApplication app;
	protected final TextView title;
	protected final TextView description;
	protected final ImageView icon;
	protected final DialogButton button;

	public FavoriteEmptyFolderVHolder(@NonNull View view, @Nullable FavoriteAdapterListener listener) {
		super(view);
		app = (OsmandApplication) itemView.getContext().getApplicationContext().getApplicationContext();

		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);
		button = view.findViewById(R.id.action_button);
		button.setOnClickListener(v -> {
			if (listener != null) {
				listener.onEmptyStateClick();
			}
		});
	}

	public void bindView() {
		title.setText(R.string.tracks_empty_folder);
		description.setText(R.string.favorites_empty_folder_description);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_folder_open));
		button.setTitleId(R.string.shared_string_import);
	}
}