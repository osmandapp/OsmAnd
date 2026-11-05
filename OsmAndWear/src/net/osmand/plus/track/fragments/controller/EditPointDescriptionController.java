package net.osmand.plus.track.fragments.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnDescriptionSavedCallback;

public abstract class EditPointDescriptionController extends EditDescriptionController {

	protected EditPointDescriptionController(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public void saveEditedDescription(@NonNull String editedText, @NonNull OnDescriptionSavedCallback callback) {
		saveEditedDescriptionImpl(editedText);
		callback.onDescriptionSaved();
	}

	protected abstract void saveEditedDescriptionImpl(@NonNull String editedText);


	@NonNull
	public String getTitle() {
		return activity.getString(R.string.shared_string_description);
	}

	@Nullable
	public String getImageUrl() {
		return null;
	}

	protected void updateContextMenu(@NonNull LatLon latLon, @NonNull PointDescription pointDescription, @NonNull Object object) {
		MapContextMenu menu = activity.getContextMenu();
		if (menu.getLatLon() != null && menu.getLatLon().equals(latLon)) {
			menu.update(latLon, pointDescription, object);
		}
	}

	@Nullable
	protected Object getContextMenuObject() {
		return getContextMenuObject(activity);
	}

	public static EditPointDescriptionController getInstance(@NonNull MapActivity mapActivity) {
		Object object = getContextMenuObject(mapActivity);
		if (object instanceof FavouritePoint) {
			return new EditFavoriteDescriptionController(mapActivity);
		} else {
			return new EditWptDescriptionController(mapActivity);
		}
	}

	protected static Object getContextMenuObject(@NonNull MapActivity mapActivity) {
		return mapActivity.getContextMenu().getObject();
	}

}
