package net.osmand.plus.mapcontextmenu.editors.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.editors.PointEditorFragment;

import java.util.List;

public class SelectCategoryDialogFragment extends DialogFragment {

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.favorite_category_select);
		final View v = getActivity().getLayoutInflater().inflate(R.layout.favorite_categories_dialog, null, false);

		LinearLayout ll = (LinearLayout) v.findViewById(R.id.list_container);

		final FavouritesDbHelper helper = ((OsmandApplication) getActivity().getApplication()).getFavorites();
		List<FavouritesDbHelper.FavoriteGroup> gs = helper.getFavoriteGroups();
		for (final FavouritesDbHelper.FavoriteGroup category : gs) {
			View itemView = getActivity().getLayoutInflater().inflate(R.layout.favorite_category_dialog_item, null);
			ImageView icon = (ImageView)itemView.findViewById(R.id.image_view);
			if (category.color != 0) {
				icon.setImageDrawable(getIcon(getActivity(), R.drawable.ic_action_folder, category.color));
			} else {
				icon.setImageDrawable(getIcon(getActivity(), R.drawable.ic_action_folder));
			}
			Button button = (Button)itemView.findViewById(R.id.button);
			button.setText(category.name);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					((PointEditorFragment) getParentFragment()).setCategory(category.name);
					dismiss();
				}
			});
			ll.addView(itemView);
		}
		View itemView = getActivity().getLayoutInflater().inflate(R.layout.favorite_category_dialog_item, null);
		ImageView icon = (ImageView)itemView.findViewById(R.id.image_view);
		icon.setImageDrawable(getIcon(getActivity(), R.drawable.map_zoom_in));
		Button button = (Button)itemView.findViewById(R.id.button);
		button.setText(getActivity().getResources().getText(R.string.favorite_category_add_new));
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//((PointEditorFragment) getParentFragment()).setCategory(null);
				//dismiss();
			}
		});
		ll.addView(itemView);

		builder.setView(v);
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		return builder.create();
	}

	public static SelectCategoryDialogFragment createInstance() {
		return new SelectCategoryDialogFragment();
	}

	private static Drawable getIcon(final Activity activity, int iconId) {
		OsmandApplication app = (OsmandApplication)activity.getApplication();
		IconsCache iconsCache = app.getIconsCache();
		boolean light = app.getSettings().isLightContent();
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color : R.color.icon_color_light);
	}

	private static Drawable getIcon(final Activity activity, int resId, int color) {
		OsmandApplication app = (OsmandApplication)activity.getApplication();
		Drawable d = app.getResources().getDrawable(resId).mutate();
		d.clearColorFilter();
		d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		return d;
	}
}
