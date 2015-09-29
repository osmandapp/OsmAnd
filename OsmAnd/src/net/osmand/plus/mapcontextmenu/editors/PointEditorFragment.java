package net.osmand.plus.mapcontextmenu.editors;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.editors.dialogs.SelectCategoryDialogFragment;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;
import net.osmand.util.Algorithms;

import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public abstract class PointEditorFragment extends Fragment {

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getEditor().saveState(outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null)
			getEditor().restoreState(savedInstanceState);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		getActivity().findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);

		View view;
		if (getEditor().isLandscapeLayout()) {
			view = inflater.inflate(R.layout.point_editor_fragment_land, container, false);
		} else {
			view = inflater.inflate(R.layout.point_editor_fragment, container, false);
		}
		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(getToolbarTitle());
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setTitleTextColor(getResources().getColor(getResIdFromAttribute(getMapActivity(), R.attr.pstsTextColor)));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		Button saveButton = (Button)toolbar.findViewById(R.id.save_button);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				savePressed();
			}
		});

		ImageButton deleteButton = (ImageButton)toolbar.findViewById(R.id.delete_button);
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deletePressed();
			}
		});

		if (getEditor().isNew()) {
			deleteButton.setVisibility(View.GONE);
		} else {
			saveButton.setVisibility(View.GONE);
		}

		TextView headerCaption = (TextView) view.findViewById(R.id.header_caption);
		headerCaption.setText(getHeaderCaption());
		TextView nameCaption = (TextView) view.findViewById(R.id.name_caption);
		nameCaption.setText(getNameCaption());
		TextView categoryCaption = (TextView) view.findViewById(R.id.category_caption);
		categoryCaption.setText(getCategoryCaption());

		EditText nameEdit = (EditText) view.findViewById(R.id.name_edit);
		nameEdit.setText(getNameInitValue());
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) view.findViewById(R.id.category_edit);
		categoryEdit.setText(getCategoryInitValue());
		categoryEdit.setThreshold(1);
		final FavouritesDbHelper helper = getMyApplication().getFavorites();
		List<FavouritesDbHelper.FavoriteGroup> gs = helper.getFavoriteGroups();
		String[] list = new String[gs.size()];
		for(int i = 0; i < list.length; i++) {
			list[i] =gs.get(i).name;
		}
		categoryEdit.setAdapter(new ArrayAdapter<>(getMapActivity(), R.layout.list_textview, list));
		categoryEdit.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, MotionEvent event) {
				final EditText editText = (EditText) v;
				final int DRAWABLE_RIGHT = 2;
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (event.getX() >= (editText.getRight()
							- editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()
							- editText.getPaddingRight())) {

						DialogFragment dialogFragment =
								SelectCategoryDialogFragment.createInstance();
						dialogFragment.show(getChildFragmentManager(), "SelectCategoryDialogFragment");
						return true;
					}
				}
				return false;
			}
		});

		EditText descriptionEdit = (EditText) view.findViewById(R.id.description_edit);
		if (getDescriptionInitValue() != null) {
			descriptionEdit.setText(getDescriptionInitValue());
		}

		ImageView nameImage = (ImageView) view.findViewById(R.id.name_image);
		nameImage.setImageDrawable(getNameIcon());
		ImageView categoryImage = (ImageView) view.findViewById(R.id.category_image);
		categoryImage.setImageDrawable(getCategoryIcon());

		ImageView descriptionImage = (ImageView) view.findViewById(R.id.description_image);
		descriptionImage.setImageDrawable(getRowIcon(R.drawable.ic_action_note_dark));

		return view;
	}

	public Drawable getRowIcon(int iconId) {
		IconsCache iconsCache = getMyApplication().getIconsCache();
		boolean light = getMyApplication().getSettings().isLightContent();
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color : R.color.icon_color_light);
	}

	@Override
	public void onDestroyView() {
		if (!wasSaved() && !getEditor().isNew()) {
			save(false);
		}
		super.onDestroyView();

		getActivity().findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
	}

	protected void savePressed() {
		save(true);
	}

	protected void deletePressed() {
		delete(true);
	}

	protected abstract boolean wasSaved();
	protected abstract void save(boolean needDismiss);
	protected abstract void delete(boolean needDismiss);

	static int getResIdFromAttribute(final Context ctx, final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedvalueattr = new TypedValue();
		ctx.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	public abstract PointEditor getEditor();
	public abstract String getToolbarTitle();

	public void setCategory(String name) {
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) getView().findViewById(R.id.category_edit);
		categoryEdit.setText(name);
		ImageView categoryImage = (ImageView) getView().findViewById(R.id.category_image);
		categoryImage.setImageDrawable(getCategoryIcon());
	}

	protected MapActivity getMapActivity() {
		return (MapActivity)getActivity();
	}

	protected OsmandApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}

	public void dismiss() {
		dismiss(false);
	}

	public void dismiss(boolean includingMenu) {
		if (includingMenu) {
			//getMapActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			getMapActivity().getSupportFragmentManager().popBackStack();
			getMapActivity().getMapLayers().getContextMenuLayer().hideMapContextMenuMarker();
			getMapActivity().getContextMenu().hide();
		} else {
			getMapActivity().getSupportFragmentManager().popBackStack();
		}
	}

	public abstract String getHeaderCaption();

	public String getNameCaption() {
		return getMapActivity().getResources().getString(R.string.favourites_edit_dialog_name);
	}
	public String getCategoryCaption() {
		return getMapActivity().getResources().getString(R.string.favourites_edit_dialog_category);
	}

	public abstract String getNameInitValue();
	public abstract String getCategoryInitValue();
	public abstract String getDescriptionInitValue();

	public abstract Drawable getNameIcon();
	public abstract Drawable getCategoryIcon();

	public String getNameTextValue() {
		EditText nameEdit = (EditText) getView().findViewById(R.id.name_edit);
		return nameEdit.getText().toString().trim();
	}

	public String getCategoryTextValue() {
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) getView().findViewById(R.id.category_edit);
		return categoryEdit.getText().toString().trim();
	}

	public String getDescriptionTextValue() {
		EditText descriptionEdit = (EditText) getView().findViewById(R.id.description_edit);
		String res = descriptionEdit.getText().toString().trim();
		return Algorithms.isEmpty(res) ? null : res;
	}

	// Utils
	private int getScreenHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
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
