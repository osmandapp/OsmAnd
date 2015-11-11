package net.osmand.plus.mapcontextmenu.editors;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
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

import gnu.trove.list.array.TIntArrayList;

public class EditCategoryDialogFragment extends DialogFragment {

	public static final String TAG = "EditCategoryDialogFragment";

	private static final String KEY_CTX_EDIT_CAT_EDITOR_TAG = "key_ctx_edit_cat_editor_tag";
	private static final String KEY_CTX_EDIT_CAT_NEW = "key_ctx_edit_cat_new";
	private static final String KEY_CTX_EDIT_CAT_NAME = "key_ctx_edit_cat_name";
	private static final String KEY_CTX_EDIT_CAT_COLOR = "key_ctx_edit_cat_color";

	private String editorTag;
	private boolean isNew = true;
	private String name = "";
	private int color;

	private EditText nameEdit;
	private Spinner colorSpinner;

	FavouritesDbHelper helper;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		helper = ((OsmandApplication) getActivity().getApplication()).getFavorites();

		color = ColorDialogs.pallette[0];

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.favorite_category_add_new_title);
		final View v = getActivity().getLayoutInflater().inflate(R.layout.favorite_category_edit_dialog, null, false);

		nameEdit = (EditText)v.findViewById(R.id.edit_name);
		nameEdit.setText(name);

		colorSpinner = (Spinner)v.findViewById(R.id.edit_color);
		final TIntArrayList colors = new TIntArrayList();
		final int intColor = color;
		ColorDialogs.setupColorSpinnerEx(getActivity(), intColor, colorSpinner, colors, new AdapterView.OnItemSelectedListener() {
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
					if (!helper.groupExists(name)) {
						helper.addEmptyCategory(name, color);
						PointEditor editor = ((MapActivity) getActivity()).getContextMenu().getPointEditor(editorTag);
						if (editor != null) {
							editor.setCategory(name);
						}
						d.dismiss();
					} else {
						AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
						b.setMessage(getString(R.string.favorite_category_dublicate_message));
						b.setNegativeButton(R.string.shared_string_ok, null);
						b.show();
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

	public static EditCategoryDialogFragment createInstance(String editorTag) {
		EditCategoryDialogFragment fragment = new EditCategoryDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		fragment.setArguments(bundle);
		return fragment;
	}

	public static EditCategoryDialogFragment createInstance(String editorTag, FavoriteGroup group) {
		EditCategoryDialogFragment fragment = new EditCategoryDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		bundle.putString(KEY_CTX_EDIT_CAT_NEW, Boolean.valueOf(false).toString());
		bundle.putString(KEY_CTX_EDIT_CAT_NAME, group.name);
		bundle.putString(KEY_CTX_EDIT_CAT_COLOR, "" + group.color);
		fragment.setArguments(bundle);
		return fragment;
	}

	public void saveState(Bundle bundle) {
		bundle.putString(KEY_CTX_EDIT_CAT_EDITOR_TAG, editorTag);
		bundle.putString(KEY_CTX_EDIT_CAT_NEW, Boolean.valueOf(isNew).toString());
		bundle.putString(KEY_CTX_EDIT_CAT_NAME, nameEdit.getText().toString().trim());
		bundle.putString(KEY_CTX_EDIT_CAT_COLOR, "" + color);
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
	}
}
