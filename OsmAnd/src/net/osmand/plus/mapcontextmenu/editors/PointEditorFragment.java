package net.osmand.plus.mapcontextmenu.editors;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;
import net.osmand.util.Algorithms;

public abstract class PointEditorFragment extends Fragment {

	private View view;
	private EditText nameEdit;
	private boolean cancelled;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.point_editor_fragment, container, false);

		getEditor().updateLandscapePortrait();
		getEditor().updateNightMode();

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(getToolbarTitle());
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setTitleTextColor(getResources().getColor(getResIdFromAttribute(getMapActivity(), R.attr.pstsTextColor)));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		Button saveButton = (Button) view.findViewById(R.id.save_button);
		saveButton.setTextColor(getResources().getColor(!getEditor().isLight() ? R.color.osmand_orange : R.color.map_widget_blue));
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				savePressed();
			}
		});

		Button cancelButton = (Button) view.findViewById(R.id.cancel_button);
		cancelButton.setTextColor(getResources().getColor(!getEditor().isLight() ? R.color.osmand_orange : R.color.map_widget_blue));
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelled = true;
				dismiss();
			}
		});

		Button deleteButton = (Button) view.findViewById(R.id.delete_button);
		deleteButton.setTextColor(getResources().getColor(!getEditor().isLight() ? R.color.osmand_orange : R.color.map_widget_blue));
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deletePressed();
			}
		});

		if (getEditor().isNew()) {
			deleteButton.setVisibility(View.GONE);
		} else {
			deleteButton.setVisibility(View.VISIBLE);
		}

		view.findViewById(R.id.background_layout).setBackgroundResource(!getEditor().isLight() ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);
		view.findViewById(R.id.buttons_layout).setBackgroundResource(!getEditor().isLight() ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);
		view.findViewById(R.id.title_view).setBackgroundResource(!getEditor().isLight() ? R.color.bg_color_dark : R.color.bg_color_light);
		view.findViewById(R.id.description_info_view).setBackgroundResource(!getEditor().isLight() ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);

		TextView nameCaption = (TextView) view.findViewById(R.id.name_caption);
		AndroidUtils.setTextSecondaryColor(view.getContext(), nameCaption, !getEditor().isLight());
		nameCaption.setText(getNameCaption());
		TextView categoryCaption = (TextView) view.findViewById(R.id.category_caption);
		AndroidUtils.setTextSecondaryColor(view.getContext(), categoryCaption, !getEditor().isLight());
		categoryCaption.setText(getCategoryCaption());

		nameEdit = (EditText) view.findViewById(R.id.name_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), nameEdit, !getEditor().isLight());
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), nameEdit, !getEditor().isLight());
		nameEdit.setText(getNameInitValue());
		AutoCompleteTextViewEx categoryEdit = (AutoCompleteTextViewEx) view.findViewById(R.id.category_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), categoryEdit, !getEditor().isLight());
		categoryEdit.setText(getCategoryInitValue());
		categoryEdit.setFocusable(false);
		categoryEdit.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					DialogFragment dialogFragment =
							SelectCategoryDialogFragment.createInstance(getEditor().getFragmentTag());
					dialogFragment.show(getChildFragmentManager(), SelectCategoryDialogFragment.TAG);
					return true;
				}
				return false;
			}
		});

		final EditText descriptionEdit = (EditText) view.findViewById(R.id.description_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), descriptionEdit, !getEditor().isLight());
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), descriptionEdit, !getEditor().isLight());
		if (getDescriptionInitValue() != null) {
			descriptionEdit.setText(getDescriptionInitValue());
		}

		if (Build.VERSION.SDK_INT >= 11) {
			view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
					if (descriptionEdit.isFocused()) {
						ScrollView scrollView = (ScrollView) view.findViewById(R.id.editor_scroll_view);
						scrollView.scrollTo(0, bottom);
					}
				}
			});
		} else {
			ViewTreeObserver vto = view.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					if (descriptionEdit.isFocused()) {
						ScrollView scrollView = (ScrollView) view.findViewById(R.id.editor_scroll_view);
						scrollView.scrollTo(0, view.getBottom());
					}
				}
			});
		}

		ImageView nameImage = (ImageView) view.findViewById(R.id.name_image);
		nameImage.setImageDrawable(getNameIcon());
		ImageView categoryImage = (ImageView) view.findViewById(R.id.category_image);
		categoryImage.setImageDrawable(getCategoryIcon());

		ImageView descriptionImage = (ImageView) view.findViewById(R.id.description_image);
		descriptionImage.setImageDrawable(getRowIcon(R.drawable.ic_action_note_dark));

		if (getMyApplication().accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			categoryCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
			categoryEdit.setHint(R.string.access_hint_enter_category);
			descriptionEdit.setHint(R.string.access_hint_enter_description);
		}

		return view;
	}

	public Drawable getRowIcon(int iconId) {
		IconsCache iconsCache = getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId,
				getEditor().isLight() ? R.color.icon_color : R.color.icon_color_light);
	}

	@Override
	public void onStart() {
		super.onStart();
		getMapActivity().getContextMenu().setBaseFragmentVisibility(false);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getEditor().isNew()) {
			nameEdit.selectAll();
			nameEdit.requestFocus();
			AndroidUtils.softKeyboardDelayed(nameEdit);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		hideKeyboard();
		getMapActivity().getContextMenu().setBaseFragmentVisibility(true);
	}

	@Override
	public void onDestroyView() {
		if (!wasSaved() && !getEditor().isNew() && !cancelled) {
			save(false);
		}
		super.onDestroyView();
	}

	private void hideKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null) {
			View currentFocus = getActivity().getCurrentFocus();
			if (currentFocus != null) {
				IBinder windowToken = currentFocus.getWindowToken();
				if (windowToken != null) {
					inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
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
		if (attr == 0)
			return 0;
		final TypedValue typedvalueattr = new TypedValue();
		ctx.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	public abstract PointEditor getEditor();
	public abstract String getToolbarTitle();

	public void setCategory(String name) {
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
			getMapActivity().getSupportFragmentManager().popBackStack();
			getMapActivity().getContextMenu().close();
		} else {
			getMapActivity().getSupportFragmentManager().popBackStack();
		}
	}

	public abstract String getHeaderCaption();

	public String getNameCaption() {
		return getMapActivity().getResources().getString(R.string.shared_string_name);
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
		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();
		return iconsCache.getPaintedIcon(iconId, color);
	}
}
