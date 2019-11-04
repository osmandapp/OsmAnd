package net.osmand.plus.mapcontextmenu.editors;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;
import net.osmand.util.Algorithms;

public abstract class PointEditorFragment extends BaseOsmAndFragment {

	private View view;
	private EditText nameEdit;
	private boolean cancelled;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		boolean nightMode = !getMyApplication().getSettings().isLightContent();
		view = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.point_editor_fragment, container, false);

		PointEditor editor = getEditor();
		if (editor == null) {
			return view;
		}

		editor.updateLandscapePortrait(requireActivity());
		editor.updateNightMode();

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), !editor.isLight() ? R.color.app_bar_color_dark : R.color.app_bar_color_light));
		toolbar.setTitle(getToolbarTitle());

		OsmandApplication app = requireMyApplication();
		Drawable icBack = app.getUIUtilities().getIcon(R.drawable.ic_arrow_back, !editor.isLight() ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light);
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setTitleTextColor(getResources().getColor(getResIdFromAttribute(getMapActivity(), R.attr.pstsTextColor)));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		int activeColorResId = !editor.isLight() ? R.color.active_color_primary_dark : R.color.active_color_primary_light;

		Button saveButton = (Button) view.findViewById(R.id.save_button);
		saveButton.setTextColor(getResources().getColor(activeColorResId));
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				savePressed();
			}
		});

		Button cancelButton = (Button) view.findViewById(R.id.cancel_button);
		cancelButton.setTextColor(getResources().getColor(activeColorResId));
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelled = true;
				dismiss();
			}
		});

		Button deleteButton = (Button) view.findViewById(R.id.delete_button);
		deleteButton.setTextColor(getResources().getColor(activeColorResId));
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deletePressed();
			}
		});

		if (editor.isNew()) {
			deleteButton.setVisibility(View.GONE);
		} else {
			deleteButton.setVisibility(View.VISIBLE);
		}

		view.findViewById(R.id.background_layout).setBackgroundResource(!editor.isLight() ? R.color.activity_background_color_dark : R.color.activity_background_color_light);
		view.findViewById(R.id.buttons_layout).setBackgroundResource(!editor.isLight() ? R.color.activity_background_color_dark : R.color.activity_background_color_light);
		view.findViewById(R.id.title_view).setBackgroundResource(!editor.isLight() ? R.color.list_background_color_dark : R.color.list_background_color_light);
		view.findViewById(R.id.description_info_view).setBackgroundResource(!editor.isLight() ? R.color.activity_background_color_dark : R.color.activity_background_color_light);

		TextView nameCaption = (TextView) view.findViewById(R.id.name_caption);
		AndroidUtils.setTextSecondaryColor(view.getContext(), nameCaption, !editor.isLight());
		nameCaption.setText(getNameCaption());
		TextView categoryCaption = (TextView) view.findViewById(R.id.category_caption);
		AndroidUtils.setTextSecondaryColor(view.getContext(), categoryCaption, !editor.isLight());
		categoryCaption.setText(getCategoryCaption());

		nameEdit = (EditText) view.findViewById(R.id.name_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), nameEdit, !editor.isLight());
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), nameEdit, !editor.isLight());
		nameEdit.setText(getNameInitValue());
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) view.findViewById(R.id.category_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), categoryEdit, !editor.isLight());
		categoryEdit.setText(getCategoryInitValue());
		categoryEdit.setFocusable(false);
		categoryEdit.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					DialogFragment dialogFragment = createSelectCategoryDialog();
					if (dialogFragment != null) {
						dialogFragment.show(getChildFragmentManager(), SelectCategoryDialogFragment.TAG);
					}
					return true;
				}
				return false;
			}
		});

		final EditText descriptionEdit = (EditText) view.findViewById(R.id.description_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), descriptionEdit, !editor.isLight());
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), descriptionEdit, !editor.isLight());
		if (getDescriptionInitValue() != null) {
			descriptionEdit.setText(getDescriptionInitValue());
		}

		ImageView nameImage = (ImageView) view.findViewById(R.id.name_image);
		nameImage.setImageDrawable(getNameIcon());
		ImageView categoryImage = (ImageView) view.findViewById(R.id.category_image);
		categoryImage.setImageDrawable(getCategoryIcon());

		ImageView descriptionImage = (ImageView) view.findViewById(R.id.description_image);
		descriptionImage.setImageDrawable(getRowIcon(R.drawable.ic_action_note_dark));

		if (app.accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			categoryCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
			categoryEdit.setHint(R.string.access_hint_enter_category);
			descriptionEdit.setHint(R.string.access_hint_enter_description);
		}

		return view;
	}

	protected EditText getNameEdit() {
		return nameEdit;
	}

	@Nullable
	protected DialogFragment createSelectCategoryDialog() {
		PointEditor editor = getEditor();
		if (editor != null) {
			return SelectCategoryDialogFragment.createInstance(editor.getFragmentTag());
		} else {
			return null;
		}
	}

	public Drawable getRowIcon(int iconId) {
		PointEditor editor = getEditor();
		boolean light = editor == null || editor.isLight();
		return getIcon(iconId, light ? R.color.icon_color_default_light : R.color.icon_color_default_dark);
	}

	@Override
	public void onStart() {
		super.onStart();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getContextMenu().setBaseFragmentVisibility(false);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		PointEditor editor = getEditor();
		if (editor != null && editor.isNew()) {
			nameEdit.selectAll();
			nameEdit.requestFocus();
			AndroidUtils.softKeyboardDelayed(nameEdit);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		hideKeyboard();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getContextMenu().setBaseFragmentVisibility(true);
		}
	}

	@Override
	public void onDestroyView() {
		PointEditor editor = getEditor();
		if (!wasSaved() && editor != null && !editor.isNew() && !cancelled) {
			save(false);
		}
		super.onDestroyView();
	}

	@Override
	public int getStatusBarColorId() {
		return R.color.status_bar_color_light;
	}

	@Override
	protected boolean isFullScreenAllowed() {
		return false;
	}

	private void hideKeyboard() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
			if (inputMethodManager != null) {
				View currentFocus = activity.getCurrentFocus();
				if (currentFocus != null) {
					IBinder windowToken = currentFocus.getWindowToken();
					if (windowToken != null) {
						inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
					}
				}
			}
		}
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
		if (attr == 0) {
			return 0;
		}
		final TypedValue typedvalueattr = new TypedValue();
		ctx.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	@Nullable
	public abstract PointEditor getEditor();

	public abstract String getToolbarTitle();

	public void setCategory(String name, int color) {
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) view.findViewById(R.id.category_edit);
		String n = name.length() == 0 ? getDefaultCategoryName() : name;
		categoryEdit.setText(n);
		ImageView categoryImage = (ImageView) view.findViewById(R.id.category_image);
		categoryImage.setImageDrawable(getCategoryIcon());
		ImageView nameImage = (ImageView) view.findViewById(R.id.name_image);
		nameImage.setImageDrawable(getNameIcon());
	}

	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_none);
	}

	@Nullable
	protected MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Nullable
	@Override
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (includingMenu) {
				mapActivity.getSupportFragmentManager().popBackStack();
				mapActivity.getContextMenu().close();
			} else {
				mapActivity.getSupportFragmentManager().popBackStack();
			}
		}
	}

	public abstract String getHeaderCaption();

	public String getNameCaption() {
		return getString(R.string.shared_string_name);
	}

	public String getCategoryCaption() {
		return getString(R.string.favourites_edit_dialog_category);
	}

	public abstract String getNameInitValue();

	public abstract String getCategoryInitValue();

	public abstract String getDescriptionInitValue();

	public abstract Drawable getNameIcon();

	public abstract Drawable getCategoryIcon();

	public abstract int getPointColor();

	public String getNameTextValue() {
		EditText nameEdit = (EditText) view.findViewById(R.id.name_edit);
		return nameEdit.getText().toString().trim();
	}

	public String getCategoryTextValue() {
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) view.findViewById(R.id.category_edit);
		String name = categoryEdit.getText().toString().trim();
		return name.equals(getDefaultCategoryName()) ? "" : name;
	}

	public String getDescriptionTextValue() {
		EditText descriptionEdit = (EditText) view.findViewById(R.id.description_edit);
		String res = descriptionEdit.getText().toString().trim();
		return Algorithms.isEmpty(res) ? null : res;
	}

	protected Drawable getPaintedIcon(int iconId, int color) {
		return getPaintedContentIcon(iconId, color);
	}
}
