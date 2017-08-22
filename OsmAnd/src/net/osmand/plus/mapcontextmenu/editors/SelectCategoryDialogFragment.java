package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionFactory;
import net.osmand.plus.quickaction.QuickActionRegistry;

import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class SelectCategoryDialogFragment extends DialogFragment {

	public static final String TAG = "SelectCategoryDialogFragment";

	public interface CategorySelectionListener{

		void onCategorySelected(String category, int color);
	}

	private static final String KEY_CTX_SEL_CAT_EDITOR_TAG = "key_ctx_sel_cat_editor_tag";

	private String editorTag;
	private CategorySelectionListener selectionListener;
	private GPXFile gpxFile;

	public void setGpxFile(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.favorite_category_select);
		final View v = getActivity().getLayoutInflater().inflate(R.layout.favorite_categories_dialog, null, false);

		LinearLayout ll = (LinearLayout) v.findViewById(R.id.list_container);

		final FavouritesDbHelper helper = ((OsmandApplication) getActivity().getApplication()).getFavorites();
		if (gpxFile != null) {
			List<String> categories = gpxFile.getWaypointCategories();
			for (final String category : categories) {
				addCategory(ll, category, 0);
			}
		} else {
			List<FavouritesDbHelper.FavoriteGroup> gs = helper.getFavoriteGroups();
			for (final FavouritesDbHelper.FavoriteGroup category : gs) {
				addCategory(ll, category.name, category.color);
			}
		}
		View itemView = getActivity().getLayoutInflater().inflate(R.layout.favorite_category_dialog_item, null);
		Button button = (Button)itemView.findViewById(R.id.button);
		button.setCompoundDrawablesWithIntrinsicBounds(getIcon(getActivity(), R.drawable.map_zoom_in), null, null, null);
		button.setCompoundDrawablePadding(dpToPx(15f));
		button.setText(getActivity().getResources().getText(R.string.favorite_category_add_new));
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				EditCategoryDialogFragment dialogFragment = EditCategoryDialogFragment.createInstance(editorTag);
				dialogFragment.show(getActivity().getSupportFragmentManager(), EditCategoryDialogFragment.TAG);
				dialogFragment.setSelectionListener(selectionListener);
			}
		});
		ll.addView(itemView);

		builder.setView(v);
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		return builder.create();
	}

	private void addCategory(LinearLayout ll, final String categoryName, final int categoryColor) {
		View itemView = getActivity().getLayoutInflater().inflate(R.layout.favorite_category_dialog_item, null);
		Button button = (Button)itemView.findViewById(R.id.button);
		if (categoryColor != 0) {
			button.setCompoundDrawablesWithIntrinsicBounds(getIcon(getActivity(), R.drawable.ic_action_folder, categoryColor), null, null, null);
		} else {
			button.setCompoundDrawablesWithIntrinsicBounds(getIcon(getActivity(), R.drawable.ic_action_folder, getResources().getColor(R.color.color_favorite)), null, null, null);
		}
		button.setCompoundDrawablePadding(dpToPx(15f));
		String name = categoryName.length() == 0 ? getString(R.string.shared_string_favorites) : categoryName;
		button.setText(name);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				PointEditor editor = ((MapActivity) getActivity()).getContextMenu().getPointEditor(editorTag);

				if (editor != null) {
					editor.setCategory(categoryName);
					if (gpxFile != null && editor instanceof WptPtEditor) {
						((WptPtEditor) editor).getWptPt().category = categoryName;
					}
				}

				if (selectionListener != null) {
					selectionListener.onCategorySelected(categoryName, categoryColor);
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

	private int dpToPx(float dp) {
		Resources r = getActivity().getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}
}
