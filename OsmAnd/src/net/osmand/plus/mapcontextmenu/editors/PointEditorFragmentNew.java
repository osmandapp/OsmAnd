package net.osmand.plus.mapcontextmenu.editors;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
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
import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.widgets.AutoCompleteTextViewEx;
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

import static net.osmand.plus.FavouritesDbHelper.FavoriteGroup.PERSONAL_CATEGORY;
import static net.osmand.plus.FavouritesDbHelper.FavoriteGroup.isPersonalCategoryDisplayName;

public abstract class PointEditorFragmentNew extends BaseOsmAndFragment {

	private View view;
	private EditText nameEdit;
	private Button addDelDescription;
	private boolean cancelled;
	private boolean nightMode;
	private String selectedIcon;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		nightMode = !requireMyApplication().getSettings().isLightContent();
		view = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.point_editor_fragment_new, container, false);

		final PointEditor editor = getEditor();
		if (editor == null) {
			return view;
		}

		editor.updateLandscapePortrait(requireActivity());
		editor.updateNightMode();

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(getToolbarTitle());

		final OsmandApplication app = requireMyApplication();
		Drawable icBack = app.getUIUtilities().getIcon(R.drawable.ic_arrow_back,
				nightMode ? R.color.active_buttons_and_links_text_dark : R.color.description_font_and_bottom_sheet_icons);
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		int activeColorResId = !editor.isLight() ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		ImageView toolbarAction = (ImageView) view.findViewById(R.id.toolbar_action);


		ImageView groupListIcon = (ImageView) view.findViewById(R.id.group_list_button_icon);
		groupListIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_group_select_all, activeColorResId));

		ImageView replaceIcon = (ImageView) view.findViewById(R.id.replace_action_icon);
		replaceIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_replace, activeColorResId));

		ImageView deleteIcon = (ImageView) view.findViewById(R.id.delete_action_icon);
		deleteIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_delete_dark, activeColorResId));

//		((TextView)view.findViewById(R.id.replace_action_title)).setTextColor(ContextCompat.getColor(app,activeColorResId));
//		((TextView)view.findViewById(R.id.replace_action_description)).setTextColor(ContextCompat.getColor(app,activeColorResId));
//		((TextView)view.findViewById(R.id.delete_action_title)).setTextColor(ContextCompat.getColor(app,activeColorResId));

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

//		Button deleteButton = (Button) view.findViewById(R.id.delete_button);
//		deleteButton.setTextColor(getResources().getColor(activeColorResId));
//		deleteButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				deletePressed();
//			}
//		});

		UiUtilities.setupDialogButton(nightMode, cancelButton, UiUtilities.DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		UiUtilities.setupDialogButton(nightMode, saveButton, UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_save);

		TextInputLayout nameCaption = (TextInputLayout) view.findViewById(R.id.name_caption);
		nameCaption.setHint(getString(R.string.shared_string_name));

		nameEdit = (EditText) view.findViewById(R.id.name_edit);
		nameEdit.setText(getNameInitValue());
		ImageView nameIcon = (ImageView) view.findViewById(R.id.name_icon);
		TextView categoryEdit = view.findViewById(R.id.groupName);
		if (categoryEdit != null) {
			AndroidUtils.setTextPrimaryColor(view.getContext(), categoryEdit, !editor.isLight());
			categoryEdit.setText(getCategoryInitValue());
		}

		final EditText descriptionEdit = (EditText) view.findViewById(R.id.description_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), descriptionEdit, !editor.isLight());
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), descriptionEdit, !editor.isLight());
		if (getDescriptionInitValue() != null) {
			descriptionEdit.setText(getDescriptionInitValue());
		}

		final TextInputLayout descriptionCaption = (TextInputLayout) view.findViewById(R.id.description_caption);
		addDelDescription = (Button) view.findViewById(R.id.description_button);
		addDelDescription.setTextColor(getResources().getColor(activeColorResId));
		addDelDescription.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (descriptionCaption.getVisibility() != View.VISIBLE) {
					descriptionCaption.setVisibility(View.VISIBLE);
					addDelDescription.setText(view.getResources().getString(R.string.delete_description));
				} else {
					descriptionCaption.setVisibility(View.GONE);
					addDelDescription.setText(view.getResources().getString(R.string.add_description));
				}
			}
		});
		nameIcon.setImageDrawable(getNameIcon());


		if (app.accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
			descriptionEdit.setHint(R.string.access_hint_enter_description);
		}

		if (editor.isNew()) {
			toolbarAction.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_replace, activeColorResId));
//			deleteButton.setVisibility(View.GONE);
			descriptionCaption.setVisibility(View.GONE);
			deleteIcon.setVisibility(View.GONE);
		} else {
			toolbarAction.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_delete_dark, activeColorResId));
//			deleteButton.setVisibility(View.VISIBLE);
			deleteIcon.setVisibility(View.VISIBLE);
			if (!descriptionEdit.getText().toString().isEmpty() || descriptionCaption.getVisibility() != View.VISIBLE) {
				descriptionCaption.setVisibility(View.VISIBLE);
				addDelDescription.setText(app.getString(R.string.delete_description));
			} else {
				descriptionCaption.setVisibility(View.GONE);
				addDelDescription.setText(app.getString(R.string.add_description));
			}
		}

		createGroupSelector(app);
		createIconSelector();
		createColorSelector();
		createShapeSelector();

		return view;
	}

	private void createGroupSelector(OsmandApplication app) {
		GroupAdapter groupListAdapter = new GroupAdapter(app);
		groupListAdapter.setItemClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				int position = groupListAdapter.getChildAdapterPosition(v);
//				if (position == RecyclerView.NO_POSITION) {
//					return;
//				}
//				selectFavorite(favouritePoints.get(position));
			}
		});
		groupListAdapter.setSelectedItemName(getCategoryInitValue());
		RecyclerView groupRecyclerView = view.findViewById(R.id.group_recycler_view);
		groupRecyclerView.setAdapter(groupListAdapter);
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		groupListAdapter.notifyDataSetChanged();
	}

	private void createColorSelector() {
		FlowLayout selectColor = view.findViewById(R.id.select_color);
		for (int color : ColorDialogs.pallette) {
			selectColor.addView(createColorItemView(color, selectColor), new FlowLayout.LayoutParams(0, 0));
		}
	}

	private View createColorItemView(@ColorRes int color, FlowLayout rootView) {
		OsmandApplication app = requireMyApplication();
		FrameLayout colorItemView = (FrameLayout) UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.point_editor_button, rootView, false);
		ImageView backgroundCircle = colorItemView.findViewById(R.id.background);
		AndroidUtils.setBackground(backgroundCircle,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.bg_point_circle),
						color));

		return colorItemView;
	}

	private void createShapeSelector() {
		FlowLayout selectShape = view.findViewById(R.id.select_shape);
		for (FavouritePoint.BackgroundType backgroundType : FavouritePoint.BackgroundType.values()) {
			selectShape.addView(createShapeItemView(backgroundType, selectShape), new FlowLayout.LayoutParams(0, 0));
		}
	}

	private View createShapeItemView(FavouritePoint.BackgroundType backgroundType, FlowLayout rootView) {
		OsmandApplication app = requireMyApplication();
		FrameLayout shapeItemView = (FrameLayout) UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.point_editor_button, rootView, false);
		ImageView backgroundCircle = shapeItemView.findViewById(R.id.background);
		AndroidUtils.setBackground(backgroundCircle,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, backgroundType.getIconId()),
						ContextCompat.getColor(app, R.color.divider_color_light)));
		ImageView icon = shapeItemView.findViewById(R.id.icon);
//		final int iconRes = getMyApplication().getResources().getIdentifier("mx_" + iconName, "drawable", app.getPackageName());
//		icon.setImageDrawable(getMyApplication().getUIUtilities().getIcon(iconRes, R.color.icon_color_default_light));
//		backgroundCircle.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				updateIconSelector(iconRes,rootView);
//			}
//		});
//		shapeItemView.setTag(String.valueOf(iconRes));
		return shapeItemView;
	}

	private void createIconSelector() {
		LinkedHashMap<String, JSONArray> group = new LinkedHashMap<>();
		try {
			JSONObject obj = new JSONObject(loadJSONFromAsset());
			JSONObject categories = obj.getJSONObject("categories");
			for (int i = 0; i < categories.length(); i++) {
				JSONArray names = categories.names();
				JSONObject icons = categories.getJSONObject(names.get(i).toString());
				group.put(names.get(i).toString(), icons.getJSONArray("icons"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		String selectedIconCategory = group.keySet().iterator().next();
		FlowLayout selectIcon = view.findViewById(R.id.select_icon);
		JSONArray iconJsonArray = group.get(selectedIconCategory);
		if (iconJsonArray != null) {
			List<String> iconNameList = new ArrayList<>();
			for (int i = 0; i < iconJsonArray.length(); i++) {
				try {
					iconNameList.add(iconJsonArray.getString(i));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			GroupNameAdapter groupNameListAdapter = new GroupNameAdapter();
			groupNameListAdapter.setItems(new ArrayList<>(group.keySet()));
			RecyclerView groupNameRecyclerView = view.findViewById(R.id.group_name_recycler_view);
			groupNameRecyclerView.setAdapter(groupNameListAdapter);
			groupNameRecyclerView.setLayoutManager(new LinearLayoutManager(requireMyApplication(), RecyclerView.HORIZONTAL, false));
			groupNameListAdapter.notifyDataSetChanged();

			for (String name : iconNameList) {
				selectIcon.addView(createIconItemView(name, selectIcon), new FlowLayout.LayoutParams(0, 0));
			}
		}
	}

	private View createIconItemView(final String iconName, final ViewGroup rootView) {
		OsmandApplication app = requireMyApplication();
		FrameLayout iconItemView = (FrameLayout) UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.point_editor_button, rootView, false);
		ImageView backgroundCircle = iconItemView.findViewById(R.id.background);
		AndroidUtils.setBackground(backgroundCircle,
				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.bg_point_circle),
						ContextCompat.getColor(app, R.color.divider_color_light)));
		GradientDrawable rectContourDrawable = (GradientDrawable) ContextCompat.getDrawable(app, R.drawable.circle_contour_bg_light);
		if (rectContourDrawable != null) {
			rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), ContextCompat.getColor(app, R.color.divider_color_light));
			ImageView outline = iconItemView.findViewById(R.id.outline);
			outline.setImageDrawable(rectContourDrawable);
		}
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
		iconItemView.setTag(String.valueOf(iconRes));
		return iconItemView;
	}

	private void updateIconSelector(int iconRes, ViewGroup rootView) {
//		setIconNewColor(iconRes);
		View oldIcon = rootView.findViewWithTag(selectedIcon);
		if (oldIcon != null) {
			oldIcon.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
		}
		rootView.findViewWithTag(String.valueOf(iconRes)).findViewById(R.id.outline).setVisibility(View.VISIBLE);

//		checkMark.setImageDrawable(app.getUIUtilities().getIcon(changedProfile.iconRes, R.color.icon_color_default_light));
//		AndroidUtils.setBackground(oldIcon.findViewById(R.id.backgroundCircle),
//				UiUtilities.tintDrawable(ContextCompat.getDrawable(app, R.drawable.circle_background_light),
//						UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, R.color.icon_color_default_light), 0.1f)));
		selectedIcon = String.valueOf(iconRes);
//		updateProfileButton();
	}

	private String loadJSONFromAsset() {
		String json = null;
		try {
			InputStream is = requireMyApplication().getAssets().open("poi_categories.json");
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
		//todo renew category list
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

	public abstract String getNameInitValue();

	public abstract String getCategoryInitValue();

	public abstract String getDescriptionInitValue();

	public abstract Drawable getNameIcon();

	public abstract Drawable getCategoryIcon();

	public abstract int getPointColor();

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
		View.OnClickListener listener;
		OsmandApplication app;
		List<FavouritesDbHelper.FavoriteGroup> items;

		void setSelectedItemName(String selectedItemName) {
			this.selectedItemName = selectedItemName;
		}

		String selectedItemName;

		GroupAdapter(OsmandApplication app) {
			this.app = app;
			items = app.getFavorites().getFavoriteGroups();
		}

		@NonNull
		@Override
		public GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view;
			int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
			if (viewType == VIEW_TYPE_CELL) {
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.point_group_select_item, parent, false);
				view.setOnClickListener(listener);
			} else {
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.point_group_select_item, parent, false);
				((ImageView) view.findViewById(R.id.groupIcon)).setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_add));
				((TextView) view.findViewById(R.id.groupName)).setText(requireMyApplication().getString(R.string.add_group));
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
						EditCategoryDialogFragment dialogFragment =
								EditCategoryDialogFragment.createInstance("editorTag", getCategories(), false);
						dialogFragment.show(requireActivity().getSupportFragmentManager(), EditCategoryDialogFragment.TAG);
					}
				});
			} else {
				holder.groupButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {

						selectedItemName = items.get(holder.getAdapterPosition()).getDisplayName(app);
						notifyDataSetChanged();
					}
				});
				final FavouritesDbHelper.FavoriteGroup group = items.get(position);
				holder.groupName.setText(group.getDisplayName(app));

				int color = group.getColor() == 0 ? getResources().getColor(R.color.color_favorite) : group.getColor();
				holder.groupIcon.setImageDrawable(UiUtilities.tintDrawable(
						ContextCompat.getDrawable(app, R.drawable.ic_action_folder), color));
				holder.pointsCounter.setText(String.valueOf(group.getPoints().size()));
				int strokeColor;
				int strokeWidth;
				if (selectedItemName != null && selectedItemName.equals(items.get(position).getDisplayName(app))) {
					strokeColor = ContextCompat.getColor(app, nightMode ?
							R.color.active_color_primary_dark : R.color.active_color_primary_light);
					strokeWidth = 2;
				} else {
					strokeColor = ContextCompat.getColor(app, R.color.divider_color_light);
					strokeWidth = 1;
				}
				GradientDrawable rectContourDrawable = (GradientDrawable) ContextCompat.getDrawable(app, R.drawable.bg_select_group_button_outline);
				if (rectContourDrawable != null) {
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, strokeWidth), strokeColor);
					holder.groupButton.setImageDrawable(rectContourDrawable);
				}
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

		void setItemClickListener(View.OnClickListener listener) {
			this.listener = listener;
		}

		String getSelectedItem() {
			return selectedItemName;
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

	class GroupNameAdapter extends RecyclerView.Adapter<NameViewHolder> {

		List<String> items;

		public void setItems(List<String> items) {
			this.items = items;
		}

		@NonNull
		@Override
		public NameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Button view;
			int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
			view = new Button(requireMyApplication());
			return new NameViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull NameViewHolder holder, int position) {
			((Button) holder.itemView).setText(items.get(position));
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

	}

	static class NameViewHolder extends RecyclerView.ViewHolder {
		NameViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}
}
