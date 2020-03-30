package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static net.osmand.data.FavouritePoint.*;
import static net.osmand.plus.FavouritesDbHelper.FavoriteGroup.PERSONAL_CATEGORY;
import static net.osmand.plus.FavouritesDbHelper.FavoriteGroup.isPersonalCategoryDisplayName;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;

public abstract class PointEditorFragmentNew extends BaseOsmAndFragment {

	private View view;
	private EditText nameEdit;
	private TextView addDelDescription;
	private boolean cancelled;
	private boolean nightMode;
	@DrawableRes
	private int selectedIcon;
	@ColorInt
	private int selectedColor;
	private BackgroundType selectedShape = BackgroundType.CIRCLE;
	private ImageView nameIcon;
	private GroupAdapter groupListAdapter;
	private int scrollViewY;
	private RecyclerView groupRecyclerView;
	private String selectedIconCategory;
	private LinkedHashMap<String, JSONArray> iconCategories;
	private OsmandApplication app;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		app = requireMyApplication();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		view = UiUtilities.getMaterialInflater(getContext(), nightMode)
				.inflate(R.layout.point_editor_fragment_new, container, false);

		final PointEditor editor = getEditor();
		if (editor == null) {
			return view;
		}

		editor.updateLandscapePortrait(requireActivity());
		editor.updateNightMode();

		selectedColor = 0xb4FFFFFF & getPointColor();
		selectedShape = getBackgroundType();
		selectedIcon = getIconId();

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ContextCompat.getColor(requireContext(),
				nightMode ? R.color.app_bar_color_dark : R.color.list_background_color_light));
		toolbar.setTitle(getToolbarTitle());
		Drawable icBack = app.getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(app),
				nightMode ? R.color.active_buttons_and_links_text_dark : R.color.description_font_and_bottom_sheet_icons);
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		final ScrollView scrollView = (ScrollView) view.findViewById(R.id.editor_scroll_view);
		scrollViewY = scrollView.getScrollY();
		scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
			@Override
			public void onScrollChanged() {
				if (scrollViewY != scrollView.getScrollY()) {
					scrollViewY = scrollView.getScrollY();
					Activity activity = getActivity();
					if (activity != null) {
						AndroidUtils.hideSoftKeyboard(activity, view);
					}
				}
			}
		});

		int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		ImageView toolbarAction = (ImageView) view.findViewById(R.id.toolbar_action);
		view.findViewById(R.id.background_layout).setBackgroundResource(nightMode
				? R.color.app_bar_color_dark : R.color.list_background_color_light);
		ImageView groupListIcon = (ImageView) view.findViewById(R.id.group_list_button_icon);
		groupListIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_group_select_all, activeColorResId));
		ImageView replaceIcon = (ImageView) view.findViewById(R.id.replace_action_icon);
		replaceIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_replace, activeColorResId));
		ImageView deleteIcon = (ImageView) view.findViewById(R.id.delete_action_icon);
		deleteIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_delete_dark, activeColorResId));
		View groupList = view.findViewById(R.id.group_list_button);
		groupList.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment dialogFragment = createSelectCategoryDialog();
				if (dialogFragment != null) {

					dialogFragment.show(getChildFragmentManager(), SelectCategoryDialogFragment.TAG);
				}
			}
		});
		view.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);
		View saveButton = view.findViewById(R.id.right_bottom_button);
		saveButton.setVisibility(View.VISIBLE);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				savePressed();
			}
		});

		View cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelled = true;
				dismiss();
			}
		});

		UiUtilities.setupDialogButton(nightMode, cancelButton, UiUtilities.DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		UiUtilities.setupDialogButton(nightMode, saveButton, UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_save);

		TextInputLayout nameCaption = (TextInputLayout) view.findViewById(R.id.name_caption);
		nameCaption.setHint(getString(R.string.shared_string_name));

		nameEdit = (EditText) view.findViewById(R.id.name_edit);
		nameEdit.setText(getNameInitValue());
		nameIcon = (ImageView) view.findViewById(R.id.name_icon);
		TextView categoryEdit = view.findViewById(R.id.groupName);
		if (categoryEdit != null) {
			AndroidUtils.setTextPrimaryColor(view.getContext(), categoryEdit, nightMode);
			categoryEdit.setText(getCategoryInitValue());
		}

		final EditText descriptionEdit = (EditText) view.findViewById(R.id.description_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), descriptionEdit, nightMode);
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), descriptionEdit, nightMode);
		if (getDescriptionInitValue() != null) {
			descriptionEdit.setText(getDescriptionInitValue());
		}

		final View descriptionCaption = view.findViewById(R.id.description);
		addDelDescription = (TextView) view.findViewById(R.id.description_button);
		addDelDescription.setTextColor(getResources().getColor(activeColorResId));
		addDelDescription.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (descriptionCaption.getVisibility() != View.VISIBLE) {
					descriptionCaption.setVisibility(View.VISIBLE);
					addDelDescription.setText(view.getResources().getString(R.string.delete_description));
					View descriptionEdit = view.findViewById(R.id.description_edit);
					descriptionEdit.requestFocus();
					AndroidUtils.softKeyboardDelayed(descriptionEdit);
				} else {
					descriptionCaption.setVisibility(View.GONE);
					addDelDescription.setText(view.getResources().getString(R.string.add_description));
					AndroidUtils.hideSoftKeyboard(requireActivity(), descriptionEdit);
				}
			}
		});
		nameIcon.setImageDrawable(getNameIcon());

		if (app.accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
			descriptionEdit.setHint(R.string.access_hint_enter_description);
		}

		View deleteButton = view.findViewById(R.id.button_delete_container);
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deletePressed();
			}
		});

		if (editor.isNew()) {
			toolbarAction.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_replace, activeColorResId));
			deleteButton.setVisibility(View.GONE);
			descriptionCaption.setVisibility(View.GONE);
			deleteIcon.setVisibility(View.GONE);
		} else {
			toolbarAction.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_delete_dark, activeColorResId));
			deleteButton.setVisibility(View.VISIBLE);
			deleteIcon.setVisibility(View.VISIBLE);
			if (!descriptionEdit.getText().toString().isEmpty() || descriptionCaption.getVisibility() != View.VISIBLE) {
				descriptionCaption.setVisibility(View.VISIBLE);
				addDelDescription.setText(app.getString(R.string.delete_description));
			} else {
				descriptionCaption.setVisibility(View.GONE);
				addDelDescription.setText(app.getString(R.string.add_description));
			}
		}

		toolbarAction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!editor.isNew) {
					deletePressed();
				}
			}
		});
		createGroupSelector();
		createIconSelector();
		createColorSelector();
		createShapeSelector();
		updateColorSelector(selectedColor, view);
		updateShapeSelector(selectedShape, view);

		return view;
	}

	private void createGroupSelector() {
		groupListAdapter = new GroupAdapter();
		groupRecyclerView = view.findViewById(R.id.group_recycler_view);
		groupRecyclerView.setAdapter(groupListAdapter);
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		setSelectedItemWithScroll(getCategoryInitValue());
	}

	private void createColorSelector() {
		FlowLayout selectColor = view.findViewById(R.id.select_color);
		for (int color : ColorDialogs.pallette) {
			selectColor.addView(createColorItemView(color, selectColor), new FlowLayout.LayoutParams(0, 0));
		}
	}

	private View createColorItemView(@ColorRes final int color, final FlowLayout rootView) {
		FrameLayout colorItemView = (FrameLayout) UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.point_editor_button, rootView, false);
		ImageView outline = colorItemView.findViewById(R.id.outline);
		outline.setImageDrawable(
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.bg_point_circle_contour),
						ContextCompat.getColor(app,
								nightMode ? R.color.stroked_buttons_and_links_outline_dark
										: R.color.stroked_buttons_and_links_outline_light)));
		ImageView backgroundCircle = colorItemView.findViewById(R.id.background);
		AndroidUtils.setBackground(backgroundCircle,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.bg_point_circle), color));
		backgroundCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateColorSelector(color, rootView);
			}
		});
		colorItemView.setTag(color);
		return colorItemView;
	}

	private void updateColorSelector(int color, View rootView) {
		View oldColor = rootView.findViewWithTag(selectedColor);
		if (oldColor != null) {
			oldColor.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView icon = oldColor.findViewById(R.id.icon);
			icon.setImageDrawable(UiUtilities.tintDrawable(icon.getDrawable(), R.color.icon_color_default_light));
		}
		View newColor = rootView.findViewWithTag(color);
		if (newColor != null) {
			newColor.findViewById(R.id.outline).setVisibility(View.VISIBLE);
		}
		((TextView) view.findViewById(R.id.color_name)).setText(ColorDialogs.getColorName(color));
		selectedColor = color;
		setColor(color);
		updateNameIcon();
		updateShapeSelector(selectedShape, view);
		updateIconSelector(selectedIcon, view);
	}

	private void createShapeSelector() {
		FlowLayout selectShape = view.findViewById(R.id.select_shape);
		for (BackgroundType backgroundType : BackgroundType.values()) {
			selectShape.addView(createShapeItemView(backgroundType, selectShape), new FlowLayout.LayoutParams(0, 0));
		}
	}

	private View createShapeItemView(final BackgroundType backgroundType, final FlowLayout rootView) {
		FrameLayout shapeItemView = (FrameLayout) UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.point_editor_button, rootView, false);
		ImageView background = shapeItemView.findViewById(R.id.background);
		setShapeSelectorBackground(backgroundType, background);
		ImageView outline = shapeItemView.findViewById(R.id.outline);
		outline.setImageDrawable(getOutlineDrawable(backgroundType.getIconId()));
		background.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateShapeSelector(backgroundType, view);
			}
		});
		shapeItemView.setTag(backgroundType);
		return shapeItemView;
	}

	private Drawable getOutlineDrawable(int iconId) {
		String iconName = app.getResources().getResourceName(iconId);
		int iconRes = app.getResources().getIdentifier(iconName + "_contour", "drawable", app.getPackageName());
		return app.getUIUtilities().getIcon(iconRes,
				nightMode ? R.color.stroked_buttons_and_links_outline_dark : R.color.stroked_buttons_and_links_outline_light);
	}

	private void updateShapeSelector(BackgroundType backgroundType, View rootView) {
		View oldShape = rootView.findViewWithTag(selectedShape);
		if (oldShape != null) {
			oldShape.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView background = oldShape.findViewById(R.id.background);
			setShapeSelectorBackground(selectedShape, background);
		}
		View newShape = rootView.findViewWithTag(backgroundType);
		newShape.findViewById(R.id.outline).setVisibility(View.VISIBLE);
		((TextView) rootView.findViewById(R.id.shape_name)).setText(backgroundType.getNameId());
		ImageView background = newShape.findViewById(R.id.background);
		AndroidUtils.setBackground(background,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, backgroundType.getIconId()),
						selectedColor));
		selectedShape = backgroundType;
		setBackgroundType(backgroundType);
		updateNameIcon();
	}

	private void setShapeSelectorBackground(BackgroundType backgroundType, ImageView background) {
		AndroidUtils.setBackground(background,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, backgroundType.getIconId()),
						ContextCompat.getColor(app,
								nightMode ? R.color.inactive_buttons_and_links_bg_dark
										: R.color.inactive_buttons_and_links_bg_light)));
	}

	private void createIconSelector() {
		iconCategories = new LinkedHashMap<>();
		try {
			JSONObject obj = new JSONObject(loadJSONFromAsset());
			JSONObject categories = obj.getJSONObject("categories");
			for (int i = 0; i < categories.length(); i++) {
				JSONArray names = categories.names();
				JSONObject icons = categories.getJSONObject(names.get(i).toString());
				iconCategories.put(names.get(i).toString(), icons.getJSONArray("icons"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		selectedIconCategory = getInitCategory();
		createIconForCategory();
	}

	private String getInitCategory() {
		for (int j = 0; j < iconCategories.values().size(); j++) {
			JSONArray iconJsonArray = (JSONArray) iconCategories.values().toArray()[j];
			for (int i = 0; i < iconJsonArray.length(); i++) {
				try {
					if (iconJsonArray.getString(i).equals(getNameFromIconId(getIconId()))) {
						return (String) iconCategories.keySet().toArray()[j];
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return iconCategories.keySet().iterator().next();
	}

	private String getNameFromIconId(int iconId) {
		return app.getResources().getResourceEntryName(iconId).replaceFirst("mx_", "");
	}

	private void createIconForCategory() {
		FlowLayout selectIcon = view.findViewById(R.id.select_icon);
		selectIcon.removeAllViews();
		JSONArray iconJsonArray = iconCategories.get(selectedIconCategory);
		if (iconJsonArray != null) {
			List<String> iconNameList = new ArrayList<>();
			for (int i = 0; i < iconJsonArray.length(); i++) {
				try {
					iconNameList.add(iconJsonArray.getString(i));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			IconCategoriesAdapter iconCategoriesAdapter = new IconCategoriesAdapter();
			iconCategoriesAdapter.setItems(new ArrayList<>(iconCategories.keySet()));
			RecyclerView groupNameRecyclerView = view.findViewById(R.id.group_name_recycler_view);
			groupNameRecyclerView.setAdapter(iconCategoriesAdapter);
			groupNameRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
			iconCategoriesAdapter.notifyDataSetChanged();
			for (String name : iconNameList) {
				selectIcon.addView(createIconItemView(name, selectIcon), new FlowLayout.LayoutParams(0, 0));
			}
		}
	}

	private View createIconItemView(final String iconName, final ViewGroup rootView) {
		FrameLayout iconItemView = (FrameLayout) UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.point_editor_button, rootView, false);
		ImageView outline = iconItemView.findViewById(R.id.outline);
		outline.setImageDrawable(
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.bg_point_circle_contour),
						ContextCompat.getColor(app,
								nightMode ? R.color.stroked_buttons_and_links_outline_dark
										: R.color.stroked_buttons_and_links_outline_light)));
		ImageView backgroundCircle = iconItemView.findViewById(R.id.background);
		setIconSelectorBackground(backgroundCircle);
		ImageView icon = iconItemView.findViewById(R.id.icon);
		icon.setVisibility(View.VISIBLE);
		final int iconRes = app.getResources().getIdentifier("mx_" + iconName, "drawable", app.getPackageName());
		icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, R.color.icon_color_default_light));
		backgroundCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateIconSelector(iconRes, rootView);
			}
		});
		iconItemView.setTag(iconRes);
		return iconItemView;
	}

	private void updateIconSelector(int iconRes, View rootView) {
		View oldIcon = rootView.findViewWithTag(selectedIcon);
		if (oldIcon != null) {
			oldIcon.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView background = oldIcon.findViewById(R.id.background);
			setIconSelectorBackground(background);
			ImageView iconView = oldIcon.findViewById(R.id.icon);
			iconView.setImageDrawable(UiUtilities.tintDrawable(ContextCompat.getDrawable(app, selectedIcon),
					ContextCompat.getColor(app, R.color.icon_color_default_light)));
		}
		View icon = rootView.findViewWithTag(iconRes);
		if (icon != null) {
			ImageView iconView = icon.findViewById(R.id.icon);
			iconView.setImageDrawable(UiUtilities.tintDrawable(ContextCompat.getDrawable(app, iconRes),
					ContextCompat.getColor(app, R.color.color_white)));
			icon.findViewById(R.id.outline).setVisibility(View.VISIBLE);
			ImageView backgroundCircle = icon.findViewById(R.id.background);
			AndroidUtils.setBackground(backgroundCircle,
					UiUtilities.tintDrawable(ContextCompat.getDrawable(view.getContext(), R.drawable.bg_point_circle), selectedColor));
		}
		selectedIcon = iconRes;
		setIcon(iconRes);
		updateNameIcon();
	}

	private void setIconSelectorBackground(ImageView backgroundCircle) {
		AndroidUtils.setBackground(backgroundCircle,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.bg_point_circle),
						ContextCompat.getColor(app, nightMode
								? R.color.inactive_buttons_and_links_bg_dark
								: R.color.inactive_buttons_and_links_bg_light)));
	}

	private void updateNameIcon() {
		if (nameIcon != null) {
			nameIcon.setImageDrawable(getNameIcon());
		}
	}

	private String loadJSONFromAsset() {
		String json;
		try {
			InputStream is = app.getAssets().open("poi_categories.json");
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			json = new String(buffer, "UTF-8");
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
		return json;
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
		View view = getView();
		if (view != null && Build.VERSION.SDK_INT >= 23 && !nightMode) {
			view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
		return nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light;
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

	private void savePressed() {
		save(true);
	}

	private void deletePressed() {
		delete(true);
	}

	public void setCategory(String name, int color) {
		setSelectedItemWithScroll(name);
	}

	private void setSelectedItemWithScroll(String name) {
		groupListAdapter.fillGroups();
		groupListAdapter.setSelectedItemName(name);
		groupListAdapter.notifyDataSetChanged();
		int position;
		if (getEditor().isNew()) {
			String lastCategory = app.getSettings().LAST_FAV_CATEGORY_ENTERED.get();
			if (!Algorithms.isEmpty(lastCategory) && !app.getFavorites().groupExists(lastCategory)) {
				lastCategory = "";
			}
			position = groupListAdapter.getItemPosition(lastCategory);
		} else {
			position = groupListAdapter.items.size() == groupListAdapter.getItemPosition(name) + 1
					? groupListAdapter.getItemPosition(name) + 1
					: groupListAdapter.getItemPosition(name);
		}
		groupRecyclerView.scrollToPosition(position);
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

	protected abstract boolean wasSaved();

	protected abstract void save(boolean needDismiss);

	protected abstract void delete(boolean needDismiss);

	@Nullable
	public abstract PointEditor getEditor();

	public abstract String getToolbarTitle();

	public abstract int getCategoryColor(String category);

	public abstract int getCategoryPointsCount(String category);

	public abstract void setColor(int color);

	public abstract void setBackgroundType(BackgroundType backgroundType);

	public abstract void setIcon(int iconId);

	public abstract String getNameInitValue();

	public abstract String getCategoryInitValue();

	public abstract String getDescriptionInitValue();

	public abstract Drawable getNameIcon();

	public abstract Drawable getCategoryIcon();

	public abstract int getDefaultColor();

	public abstract int getPointColor();

	public abstract BackgroundType getBackgroundType();

	public abstract int getIconId();

	public abstract Set<String> getCategories();

	String getNameTextValue() {
		EditText nameEdit = view.findViewById(R.id.name_edit);
		return nameEdit.getText().toString().trim();
	}

	String getCategoryTextValue() {
		RecyclerView recyclerView = view.findViewById(R.id.group_recycler_view);
		if (recyclerView.getAdapter() != null) {
			String name = ((GroupAdapter) recyclerView.getAdapter()).getSelectedItem();
			if (isPersonalCategoryDisplayName(requireContext(), name)) {
				return PERSONAL_CATEGORY;
			}
			if (name.equals(getDefaultCategoryName())) {
				return "";
			}
			return name;
		}
		return "";
	}

	String getDescriptionTextValue() {
		EditText descriptionEdit = view.findViewById(R.id.description_edit);
		String res = descriptionEdit.getText().toString().trim();
		return Algorithms.isEmpty(res) ? null : res;
	}

	protected Drawable getPaintedIcon(int iconId, int color) {
		return getPaintedContentIcon(iconId, color);
	}

	class GroupAdapter extends RecyclerView.Adapter<GroupsViewHolder> {

		private static final int VIEW_TYPE_FOOTER = 1;
		private static final int VIEW_TYPE_CELL = 0;
		List<String> items = new ArrayList<>();

		void setSelectedItemName(String selectedItemName) {
			this.selectedItemName = selectedItemName;
		}

		String selectedItemName;

		GroupAdapter() {
			fillGroups();
		}

		private void fillGroups() {
			items.clear();
			items.addAll(getCategories());
		}

		@NonNull
		@Override
		public GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view;
			int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
			view = LayoutInflater.from(parent.getContext()).inflate(R.layout.point_editor_group_select_item, parent, false);
			if (viewType != VIEW_TYPE_CELL) {
				Drawable iconAdd = app.getUIUtilities().getIcon(R.drawable.ic_action_add, activeColorResId);
				((ImageView) view.findViewById(R.id.groupIcon)).setImageDrawable(iconAdd);
				((TextView) view.findViewById(R.id.groupName)).setText(requireMyApplication().getString(R.string.add_group));
				GradientDrawable rectContourDrawable = (GradientDrawable) ContextCompat.getDrawable(app,
						R.drawable.bg_select_group_button_outline);
				if (rectContourDrawable != null) {
					int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 1), strokeColor);
					((ImageView) view.findViewById(R.id.outlineRect)).setImageDrawable(rectContourDrawable);
				}
			}
			((TextView) view.findViewById(R.id.groupName)).setTextColor(getResources().getColor(activeColorResId));
			return new GroupsViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull final GroupsViewHolder holder, int position) {
			if (position == items.size()) {
				holder.groupButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						PointEditor editor = getEditor();
						if (editor != null) {
							EditCategoryDialogFragment dialogFragment =
									EditCategoryDialogFragment.createInstance(editor.getFragmentTag(), getCategories(), false);
							dialogFragment.show(requireActivity().getSupportFragmentManager(), EditCategoryDialogFragment.TAG);
						}
					}
				});
			} else {
				holder.groupButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						int previousSelectedPosition = getItemPosition(selectedItemName);
						selectedItemName = items.get(holder.getAdapterPosition());
						notifyItemChanged(holder.getAdapterPosition());
						notifyItemChanged(previousSelectedPosition);
					}
				});
				final String group = items.get(position);
				holder.groupName.setText(group);
				int categoryColor = getCategoryColor(group);
				int color = categoryColor == 0 ? getDefaultColor() : categoryColor;
				holder.groupIcon.setImageDrawable(UiUtilities.tintDrawable(
						ContextCompat.getDrawable(app, R.drawable.ic_action_folder), color));
				holder.pointsCounter.setText(String.valueOf(getCategoryPointsCount(group)));
				int strokeColor;
				int strokeWidth;
				if (selectedItemName != null && selectedItemName.equals(items.get(position))) {
					strokeColor = ContextCompat.getColor(app, nightMode ?
							R.color.active_color_primary_dark : R.color.active_color_primary_light);
					strokeWidth = 2;
				} else {
					strokeColor = ContextCompat.getColor(app, nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light);
					strokeWidth = 1;
				}
				GradientDrawable rectContourDrawable = (GradientDrawable) ContextCompat.getDrawable(app,
						R.drawable.bg_select_group_button_outline);
				if (rectContourDrawable != null) {
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, strokeWidth), strokeColor);
					holder.groupButton.setImageDrawable(rectContourDrawable);
				}
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, holder.groupButton, nightMode, R.drawable.ripple_solid_light_6dp,
						R.drawable.ripple_solid_dark_6dp);
			}
		}

		@Override
		public int getItemViewType(int position) {
			return (position == items.size()) ? VIEW_TYPE_FOOTER : VIEW_TYPE_CELL;
		}

		@Override
		public int getItemCount() {
			return items == null ? 0 : items.size() + 1;
		}

		String getSelectedItem() {
			return selectedItemName;
		}

		int getItemPosition(String name) {
			return items.indexOf(name);
		}
	}

	static class GroupsViewHolder extends RecyclerView.ViewHolder {

		final TextView pointsCounter;
		final TextView groupName;
		final ImageView groupIcon;
		final ImageView groupButton;

		GroupsViewHolder(View itemView) {
			super(itemView);
			pointsCounter = itemView.findViewById(R.id.counter);
			groupName = itemView.findViewById(R.id.groupName);
			groupIcon = itemView.findViewById(R.id.groupIcon);
			groupButton = itemView.findViewById(R.id.outlineRect);
		}
	}

	class IconCategoriesAdapter extends RecyclerView.Adapter<NameViewHolder> {

		List<String> items;

		public void setItems(List<String> items) {
			this.items = items;
		}

		@NonNull
		@Override
		public NameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view;
			view = LayoutInflater.from(parent.getContext()).inflate(R.layout.point_editor_icon_category_item, parent, false);
			return new NameViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull NameViewHolder holder, final int position) {
			final String category = items.get(position);
			TextView textView = holder.buttonText;
			int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
			if (category.equals(selectedIconCategory)) {
				AndroidUtils.setBackground(holder.button, app.getUIUtilities().getPaintedIcon(R.drawable.bg_select_icon_group_button,
						ContextCompat.getColor(app, activeColorResId)));
				textView.setTextColor(ContextCompat.getColor(app, R.color.color_white));
			} else {
				textView.setTextColor(ContextCompat.getColor(app, R.color.preference_category_title));
				GradientDrawable buttonBackground = (GradientDrawable) ContextCompat.getDrawable(app,
						R.drawable.bg_select_icon_group_button).mutate();
				buttonBackground.setStroke(AndroidUtils.dpToPx(app, 1), ContextCompat.getColor(app,
						nightMode ? R.color.stroked_buttons_and_links_outline_dark
								: R.color.stroked_buttons_and_links_outline_light));
				buttonBackground.setColor(ContextCompat.getColor(app, R.color.color_transparent));
				AndroidUtils.setBackground(holder.button, buttonBackground);
			}
			textView.setText(capitalizeFirstLetter(category));
			holder.button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectedIconCategory = category;
					createIconForCategory();
					updateIconSelector(selectedIcon, PointEditorFragmentNew.this.view);
				}
			});
		}

		@Override
		public int getItemCount() {
			return items.size();
		}
	}

	static class NameViewHolder extends RecyclerView.ViewHolder {
		final TextView buttonText;
		final LinearLayout button;

		NameViewHolder(@NonNull View itemView) {
			super(itemView);
			buttonText = itemView.findViewById(R.id.button_text);
			button = itemView.findViewById(R.id.button_container);
		}
	}
}
