package net.osmand.plus.mapcontextmenu.editors;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;

public class EditCategoryDialogFragment extends DialogFragment {

	public static final String TAG = EditCategoryDialogFragment.class.getSimpleName();

	private static final String KEY_CTX_EDIT_CAT_EDITOR_TAG = "key_ctx_edit_cat_editor_tag";
	private static final String KEY_CTX_EDIT_CAT_NEW = "key_ctx_edit_cat_new";
	private static final String KEY_CTX_EDIT_CAT_NAME = "key_ctx_edit_cat_name";
	private static final String KEY_CTX_EDIT_CAT_COLOR = "key_ctx_edit_cat_color";
	private static final String KEY_CTX_EDIT_GPX_FILE = "key_ctx_edit_gpx_file";
	private static final String KEY_CTX_EDIT_GPX_CATEGORIES = "key_ctx_edit_gpx_categories";

	private String editorTag;
	private boolean isNew = true;
	private String name = "";
	private int color;
	private boolean isGpx;
	private ArrayList<String> gpxCategories;

	private EditText nameEdit;
	private Spinner colorSpinner;

	FavouritesDbHelper favoritesHelper;

	private SelectCategoryDialogFragment.CategorySelectionListener selectionListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		FragmentActivity activity = requireActivity();
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		favoritesHelper = app.getFavorites();

		color = ColorDialogs.pallette[0];

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.favorite_category_add_new_title);
		final View v = activity.getLayoutInflater().inflate(R.layout.favorite_category_edit_dialog, null, false);

		nameEdit = (EditText)v.findViewById(R.id.edit_name);
		nameEdit.setText(name);

		colorSpinner = (Spinner)v.findViewById(R.id.edit_color);
		final TIntArrayList colors = new TIntArrayList();
		final int intColor = color;
		ColorDialogs.setupColorSpinnerEx(activity, intColor, colorSpinner, colors, new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				color = colors.get(position);
				colorSpinner.invalidate();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		builder.setView(v);
		builder.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { }
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		return builder.create();
	}

	public void setSelectionListener(SelectCategoryDialogFragment.CategorySelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	@Override
	public void onStart()
	{
		super.onStart();
		final AlertDialog d = (AlertDialog)getDialog();
		if(d != null)
		{
			Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
			positiveButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					name = nameEdit.getText().toString().trim();
					FragmentActivity activity = getActivity();
					if (activity != null) {
						boolean exists = isGpx ? isGpxCategoryExists(name) : favoritesHelper.groupExists(name);
						if (exists) {
							AlertDialog.Builder b = new AlertDialog.Builder(activity);
							b.setMessage(getString(R.string.favorite_category_dublicate_message));
							b.setNegativeButton(R.string.shared_string_ok, null);
							b.show();
						} else {
							if (activity instanceof MapActivity) {
								if (!isGpx) {
									favoritesHelper.addEmptyCategory(name, color);
								}
								PointEditor editor = ((MapActivity) activity).getContextMenu().getPointEditor(editorTag);

								if (editor != null) {
									editor.setCategory(name, color);
								}

								if (selectionListener != null) {
									selectionListener.onCategorySelected(name, color);
								}
							}
							d.dismiss();
						}
					}
				}
			});
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		saveState(outState);
		super.onSaveInstanceState(outState);
	}

	private boolean isGpxCategoryExists(@NonNull String name) {
		boolean res = false;
		if (gpxCategories != null) {
			String nameLC = name.toLowerCase();
			for (String category : gpxCategories) {
				if (category.toLowerCase().equals(nameLC)) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	public static EditCategoryDialogFragment createInstance(@NonNull String editorTag, @Nullable Set<String> gpxCategories, boolean isGpx) {
		EditCategoryDialogFragment fragment = new EditCategoryDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		bundle.putBoolean(KEY_CTX_EDIT_GPX_FILE, isGpx);
		if (gpxCategories != null) {
			bundle.putStringArrayList(KEY_CTX_EDIT_GPX_CATEGORIES, new ArrayList<>(gpxCategories));
		}
		fragment.setArguments(bundle);
		return fragment;
	}

	public static EditCategoryDialogFragment createInstance(String editorTag, FavoriteGroup group) {
		EditCategoryDialogFragment fragment = new EditCategoryDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		bundle.putString(KEY_CTX_EDIT_CAT_NEW, Boolean.valueOf(false).toString());
		bundle.putString(KEY_CTX_EDIT_CAT_NAME, group.getName());
		bundle.putString(KEY_CTX_EDIT_CAT_COLOR, "" + group.getColor());
		fragment.setArguments(bundle);
		return fragment;
	}

	public void saveState(Bundle bundle) {
		bundle.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		bundle.putString(KEY_CTX_EDIT_CAT_NEW, Boolean.valueOf(isNew).toString());
		bundle.putString(KEY_CTX_EDIT_CAT_NAME, nameEdit.getText().toString().trim());
		bundle.putString(KEY_CTX_EDIT_CAT_COLOR, "" + color);
		bundle.putBoolean(KEY_CTX_EDIT_GPX_FILE, isGpx);
		if (gpxCategories != null) {
			bundle.putStringArrayList(KEY_CTX_EDIT_GPX_CATEGORIES, gpxCategories);
		}
	}

	public void restoreState(Bundle bundle) {
		editorTag = bundle.getString(KEY_CTX_EDIT_CAT_EDITOR_TAG);
		String isNewStr = bundle.getString(KEY_CTX_EDIT_CAT_NEW);
		if (isNewStr != null) {
			isNew = Boolean.valueOf(isNewStr);
		}
		name = bundle.getString(KEY_CTX_EDIT_CAT_NAME);
		if (name == null) {
			name = "";
		}
		String colorStr = bundle.getString(KEY_CTX_EDIT_CAT_COLOR);
		if (!Algorithms.isEmpty(colorStr)) {
			color = Integer.parseInt(colorStr);
		}
		isGpx = bundle.getBoolean(KEY_CTX_EDIT_GPX_FILE, false);
		gpxCategories = bundle.getStringArrayList(KEY_CTX_EDIT_GPX_CATEGORIES);
	}
}
