package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import net.osmand.AndroidUtils;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectCategoryDialogFragment extends DialogFragment {

	public static final String TAG = SelectCategoryDialogFragment.class.getSimpleName();

	public interface CategorySelectionListener{

		void onCategorySelected(String category, int color);
	}

	private static final String KEY_CTX_SEL_CAT_EDITOR_TAG = "key_ctx_sel_cat_editor_tag";

	private String editorTag;
	private CategorySelectionListener selectionListener;
	private GPXFile gpxFile;
	private Map<String, Integer> gpxCategories;

	public void setGpxFile(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public Map<String, Integer> getGpxCategories() {
		return gpxCategories;
	}

	public void setGpxCategories(Map<String, Integer> gpxCategories) {
		this.gpxCategories = gpxCategories;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}

		final FragmentActivity activity = requireActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.favorite_category_select);
		final View v = activity.getLayoutInflater().inflate(R.layout.favorite_categories_dialog, null, false);
		LinearLayout ll = (LinearLayout) v.findViewById(R.id.list_container);

		final FavouritesDbHelper helper = ((OsmandApplication) activity.getApplication()).getFavorites();
		if (gpxFile != null) {
			if (gpxCategories != null) {
				for (Map.Entry<String, Integer> e : gpxCategories.entrySet()) {
					String categoryName = e.getKey();
					addCategory(activity, ll, categoryName, e.getValue());
				}
			}
		} else {
			List<FavouritesDbHelper.FavoriteGroup> gs = helper.getFavoriteGroups();
			for (final FavouritesDbHelper.FavoriteGroup category : gs) {
				if (!category.personal) {
					addCategory(activity, ll, category.name, category.color);
				}
			}
		}
		View itemView = activity.getLayoutInflater().inflate(R.layout.favorite_category_dialog_item, null);
		Button button = (Button)itemView.findViewById(R.id.button);
		button.setCompoundDrawablesWithIntrinsicBounds(getIcon(activity, R.drawable.map_zoom_in), null, null, null);
		button.setCompoundDrawablePadding(AndroidUtils.dpToPx(activity,15f));
		button.setText(activity.getResources().getText(R.string.favorite_category_add_new));
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				Set<String> categories = gpxCategories != null ? gpxCategories.keySet() : null;
				EditCategoryDialogFragment dialogFragment =
						EditCategoryDialogFragment.createInstance(editorTag, categories,gpxFile != null);
				dialogFragment.show(activity.getSupportFragmentManager(), EditCategoryDialogFragment.TAG);
				dialogFragment.setSelectionListener(selectionListener);
			}
		});
		ll.addView(itemView);

		builder.setView(v);
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		return builder.create();
	}

	private void addCategory(@NonNull final Activity activity, @NonNull LinearLayout ll, final String categoryName, final int categoryColor) {
		View itemView = activity.getLayoutInflater().inflate(R.layout.favorite_category_dialog_item, null);
		Button button = (Button)itemView.findViewById(R.id.button);
		if (categoryColor != 0) {
			button.setCompoundDrawablesWithIntrinsicBounds(
					getIcon(activity, R.drawable.ic_action_folder, categoryColor), null, null, null);
		} else {
			button.setCompoundDrawablesWithIntrinsicBounds(
					getIcon(activity, R.drawable.ic_action_folder, ContextCompat.getColor(activity,
							gpxFile != null ? R.color.gpx_color_point : R.color.color_favorite)), null, null, null);
		}
		button.setCompoundDrawablePadding(AndroidUtils.dpToPx(activity,15f));
		String name = categoryName.length() == 0 ? getString(R.string.shared_string_favorites) : categoryName;
		button.setText(name);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity a = getActivity();
				if (a != null && a instanceof MapActivity) {
					PointEditor pointEditor = ((MapActivity) a).getContextMenu().getPointEditor(editorTag);
					if (pointEditor != null) {
						pointEditor.setCategory(categoryName, categoryColor);
					}
					if (selectionListener != null) {
						selectionListener.onCategorySelected(categoryName, categoryColor);
					}
				}
				dismiss();
			}
		});
		ll.addView(itemView);
	}

	public static SelectCategoryDialogFragment createInstance(String editorTag) {
		SelectCategoryDialogFragment fragment = new SelectCategoryDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(KEY_CTX_SEL_CAT_EDITOR_TAG, editorTag);
		fragment.setArguments(bundle);
		return fragment;
	}

	public void setSelectionListener(CategorySelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	public void saveState(Bundle bundle) {
		bundle.putString(KEY_CTX_SEL_CAT_EDITOR_TAG, editorTag);
	}

	public void restoreState(Bundle bundle) {
		editorTag = bundle.getString(KEY_CTX_SEL_CAT_EDITOR_TAG);
	}

	private static Drawable getIcon(final Activity activity, int iconId) {
		OsmandApplication app = (OsmandApplication)activity.getApplication();
		UiUtilities iconsCache = app.getUIUtilities();
		boolean light = app.getSettings().isLightContent();
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color_default_light : R.color.icon_color_default_dark);
	}

	private static Drawable getIcon(final Activity activity, int resId, int color) {
		OsmandApplication app = (OsmandApplication)activity.getApplication();
		Drawable d = app.getResources().getDrawable(resId).mutate();
		d.clearColorFilter();
		d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		return d;
	}
}
