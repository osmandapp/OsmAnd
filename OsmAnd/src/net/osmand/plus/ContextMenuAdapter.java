package net.osmand.plus;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.slider.Slider;

import net.osmand.PlatformUtil;
import net.osmand.plus.activities.HelpActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.dialogs.HelpArticleDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.preferences.ContextMenuItemsPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SWITCH_PROFILE_ID;

public class ContextMenuAdapter {
	private static final Log LOG = PlatformUtil.getLog(ContextMenuAdapter.class);

	// Constants to determine profiles list item type (drawer menu items in 'Switch profile' mode)
	public static final int PROFILES_NORMAL_PROFILE_TAG = 0;
	public static final int PROFILES_CHOSEN_PROFILE_TAG = 1;
	public static final int PROFILES_CONTROL_BUTTON_TAG = 2;
	private static final int ITEMS_ORDER_STEP = 10;

	private final OsmandApplication app;
	private final List<ContextMenuItem> items = new ArrayList<>();

	@LayoutRes
	private int DEFAULT_LAYOUT_ID = R.layout.list_menu_item_native;
	private boolean profileDependent = false;
	private boolean nightMode;
	private ConfigureMapMenu.OnClickListener changeAppModeListener = null;

	public ContextMenuAdapter(OsmandApplication app) {
		this.app = app;
	}

	public int length() {
		return items.size();
	}

	public String[] getItemNames() {
		String[] itemNames = new String[items.size()];
		for (int i = 0; i < items.size(); i++) {
			itemNames[i] = items.get(i).getTitle();
		}
		return itemNames;
	}

	public void addItem(ContextMenuItem item) {
		String id = item.getId();
		if (id != null) {
			item.setHidden(isItemHidden(id));
			item.setOrder(getItemOrder(id, item.getOrder()));
		}
		items.add(item);
		sortItemsByOrder();
	}

	public ContextMenuItem getItem(int position) {
		return items.get(position);
	}

	public List<ContextMenuItem> getItems() {
		return items;
	}

	public void removeItem(int position) {
		items.remove(position);
	}

	public void clearAdapter() {
		items.clear();
	}

	public boolean isProfileDependent() {
		return profileDependent;
	}

	public void setProfileDependent(boolean profileDependent) {
		this.profileDependent = profileDependent;
	}

	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
	}

	public void setDefaultLayoutId(int defaultLayoutId) {
		this.DEFAULT_LAYOUT_ID = defaultLayoutId;
	}

	public void setChangeAppModeListener(ConfigureMapMenu.OnClickListener changeAppModeListener) {
		this.changeAppModeListener = changeAppModeListener;
	}

	private void sortItemsByOrder() {
		Collections.sort(items, new Comparator<ContextMenuItem>() {
			@Override
			public int compare(ContextMenuItem item1, ContextMenuItem item2) {
				if (DRAWER_CONFIGURE_PROFILE_ID.equals(item1.getId())
						&& DRAWER_SWITCH_PROFILE_ID.equals(item2.getId())) {
					return 1;
				} else if (DRAWER_SWITCH_PROFILE_ID.equals(item1.getId())
						&& DRAWER_CONFIGURE_PROFILE_ID.equals(item2.getId())) {
					return -1;
				} else if (DRAWER_SWITCH_PROFILE_ID.equals(item1.getId())
						|| DRAWER_CONFIGURE_PROFILE_ID.equals(item1.getId())) {
					return -1;
				} else if (DRAWER_SWITCH_PROFILE_ID.equals(item2.getId())
						|| DRAWER_CONFIGURE_PROFILE_ID.equals(item2.getId())) {
					return 1;
				}
				int order1 = item1.getOrder();
				int order2 = item2.getOrder();
				if (order1 < order2) {
					return -1;
				} else if (order1 == order2) {
					return 0;
				}
				return 1;
			}
		});
	}

	private boolean isItemHidden(@NonNull String id) {
		OsmAndAppCustomization customization = app.getAppCustomization();
		if (!customization.isFeatureEnabled(id)) {
			return true;
		}
		ContextMenuItemsPreference contextMenuItemsPreference = app.getSettings().getContextMenuItemsPreference(id);
		if (contextMenuItemsPreference == null) {
			return false;
		}
		List<String> hiddenIds = contextMenuItemsPreference.get().getHiddenIds();
		if (!Algorithms.isEmpty(hiddenIds)) {
			return hiddenIds.contains(id);
		}
		return false;
	}

	private int getItemOrder(@NonNull String id, int defaultOrder) {
		ContextMenuItemsPreference contextMenuItemsPreference = app.getSettings().getContextMenuItemsPreference(id);
		if (contextMenuItemsPreference != null) {
			List<String> orderIds = contextMenuItemsPreference.get().getOrderIds();
			if (!Algorithms.isEmpty(orderIds)) {
				int index = orderIds.indexOf(id);
				if (index != -1) {
					return index;
				}
			}
		}
		return getDefaultOrder(defaultOrder);
	}

	private int getDefaultOrder(int defaultOrder) {
		if (defaultOrder == 0 && !items.isEmpty()) {
			return items.get(items.size() - 1).getOrder() + ITEMS_ORDER_STEP;
		} else {
			return defaultOrder;
		}
	}

	public ArrayAdapter<ContextMenuItem> createListAdapter(final Activity activity, final boolean lightTheme) {
		final int layoutId = DEFAULT_LAYOUT_ID;
		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		final OsmAndAppCustomization customization = app.getAppCustomization();
		List<ContextMenuItem> itemsToRemove = new ArrayList<>();
		for (ContextMenuItem item : items) {
			String id = item.getId();
			if (item.isHidden() || !TextUtils.isEmpty(id) && !customization.isFeatureEnabled(id)) {
				itemsToRemove.add(item);
			}
		}
		items.removeAll(itemsToRemove);
		return new ContextMenuArrayAdapter(activity, layoutId, R.id.title,
				items.toArray(new ContextMenuItem[0]), app, lightTheme, changeAppModeListener);
	}

	public class ContextMenuArrayAdapter extends ArrayAdapter<ContextMenuItem> {

		private final OsmandApplication app;
		private final UiUtilities mIconsCache;
		private final boolean lightTheme;
		@LayoutRes
		private final int layoutId;
		private final ConfigureMapMenu.OnClickListener changeAppModeListener;

		public ContextMenuArrayAdapter(Activity context,
									   @LayoutRes int layoutRes,
									   @IdRes int textViewResourceId,
									   ContextMenuItem[] objects,
									   OsmandApplication app,
									   boolean lightTheme,
									   ConfigureMapMenu.OnClickListener changeAppModeListener) {
			super(context, layoutRes, textViewResourceId, objects);
			this.app = app;
			this.lightTheme = lightTheme;
			this.layoutId = layoutRes;
			this.changeAppModeListener = changeAppModeListener;
			mIconsCache = app.getUIUtilities();
		}

		@Override
		public boolean isEnabled(int position) {
			final ContextMenuItem item = getItem(position);
			if (item != null) {
				return !item.isCategory() && item.isClickable() && item.getLayout() != R.layout.drawer_divider;
			}
			return true;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			// User super class to create the View
			final ContextMenuItem item = getItem(position);
			int layoutId = item.getLayout();
			layoutId = layoutId != ContextMenuItem.INVALID_ID ? layoutId : DEFAULT_LAYOUT_ID;
			int currentModeColor = app.getSettings().getApplicationMode().getProfileColor(nightMode);
			if (layoutId == R.layout.mode_toggles) {
				return getAppModeToggleView();
			}
			if (convertView == null || !(convertView.getTag() instanceof Integer)
					|| (layoutId != (Integer) convertView.getTag())) {
				int themeRes = lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
				convertView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), layoutId, null);
				convertView.setTag(layoutId);
			}
			if (item.getMinHeight() > 0) {
				convertView.setMinimumHeight(item.getMinHeight());
			}
			if (layoutId == R.layout.main_menu_drawer_btn_switch_profile || 
					layoutId == R.layout.main_menu_drawer_btn_configure_profile) {
				return getDrawerProfileView(convertView, layoutId, item);
			} else if (layoutId == R.layout.profile_list_item) {
				return getProfileListItemView(convertView, item);
			} else if (layoutId == R.layout.help_to_improve_item) {
				return getHelpToImproveItemView(convertView);
			} else if (layoutId == R.layout.main_menu_drawer_osmand_version) {
				return getOsmAndVersionView(convertView, item);
			}

			TextView title = convertView.findViewById(R.id.title);
			if (title != null) {
				title.setText(item.getTitle());
			}

			if (this.layoutId == R.layout.simple_list_menu_item) {
				if (title != null) {
					setupTitle(title, item);
				}
			} else {
				AppCompatImageView icon = convertView.findViewById(R.id.icon);
				if (icon != null) {
					setupIcon(icon, item, currentModeColor);
				}
			}

			ImageView secondaryIcon = convertView.findViewById(R.id.secondary_icon);
			if (secondaryIcon != null) {
				setupSecondaryIcon(secondaryIcon, item.getSecondaryIcon());
			}

			CompoundButton toggle = convertView.findViewById(R.id.toggle_item);
			if (toggle != null && !item.isCategory()) {
				setupToggle(toggle, item, position, currentModeColor);
			}

			Slider slider = convertView.findViewById(R.id.slider);
			if (slider != null) {
				setupSlider(slider, item, currentModeColor);
			}

			ProgressBar progressBar = convertView.findViewById(R.id.ProgressBar);
			if (progressBar != null) {
				setupProgressBar(progressBar, item);
			}

			TextView description = convertView.findViewById(R.id.description);
			if (description != null) {
				boolean noProgress = progressBar == null || !item.isLoading();
				setupDescription(description, item.getDescription(), noProgress);
			}

			View dividerView = convertView.findViewById(R.id.divider);
			if (dividerView != null) {
				showHideDivider(dividerView, item, position);
			}

			if (item.isCategory()) {
				convertView.setFocusable(false);
				convertView.setClickable(false);
			}

			if (!item.isClickable()) {
				convertView.setFocusable(false);
				convertView.setClickable(false);
			}

			return convertView;
		}

		@NonNull
		private View getAppModeToggleView() {
			Set<ApplicationMode> selected = new LinkedHashSet<>();
			return AppModeDialog.prepareAppModeDrawerView((Activity) getContext(),
					selected, true, view -> {
						if (selected.size() > 0) {
							app.getSettings().setApplicationMode(selected.iterator().next());
							notifyDataSetChanged();
						}
						if (changeAppModeListener != null) {
							changeAppModeListener.onClick();
						}
					});
		}

		@NonNull
		private View getDrawerProfileView(@NonNull View view, @LayoutRes int layoutId, @NonNull ContextMenuItem item) {
			int color = item.getColor();
			TextView title = view.findViewById(R.id.title);
			title.setText(item.getTitle());

			if (layoutId == R.layout.main_menu_drawer_btn_switch_profile) {
				ImageView icon = view.findViewById(R.id.icon);
				icon.setImageDrawable(mIconsCache.getPaintedIcon(item.getIcon(), color));
				ImageView icArrow = view.findViewById(R.id.ic_expand_list);
				icArrow.setImageDrawable(mIconsCache.getIcon(item.getSecondaryIcon()));
				TextView desc = view.findViewById(R.id.description);
				desc.setText(item.getDescription());
			} else if (layoutId == R.layout.main_menu_drawer_btn_configure_profile) {
				View fatDivider = view.findViewById(R.id.fatDivider);
				fatDivider.setBackgroundColor(color);
			}

			Drawable selectableBg = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
			Drawable[] layers = {new ColorDrawable(ColorUtilities.getColorWithAlpha(color, 0.15f)), selectableBg};
			LayerDrawable layerDrawable = new LayerDrawable(layers);

			AndroidUtils.setBackground(view, layerDrawable);

			return view;
		}

		@NonNull
		private View getProfileListItemView(@NonNull View view, @NonNull ContextMenuItem item) {
			int tag = item.getTag();
			int colorNoAlpha = item.getColor();
			TextView title = view.findViewById(R.id.title);
			TextView desc = view.findViewById(R.id.description);
			ImageView icon = view.findViewById(R.id.icon);
			title.setText(item.getTitle());

			view.findViewById(R.id.divider_up).setVisibility(View.INVISIBLE);
			view.findViewById(R.id.divider_bottom).setVisibility(View.INVISIBLE);
			view.findViewById(R.id.menu_image).setVisibility(View.GONE);
			view.findViewById(R.id.compound_button).setVisibility(View.GONE);

			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, colorNoAlpha, 0.3f);

			if (tag == PROFILES_CONTROL_BUTTON_TAG) {
				title.setTextColor(colorNoAlpha);
				icon.setVisibility(View.INVISIBLE);
				desc.setVisibility(View.GONE);
			} else {
				AndroidUiHelper.updateVisibility(icon, true);
				AndroidUiHelper.updateVisibility(desc, true);
				AndroidUtils.setTextPrimaryColor(app, title, nightMode);
				icon.setImageDrawable(mIconsCache.getPaintedIcon(item.getIcon(), colorNoAlpha));
				desc.setText(item.getDescription());
				boolean selectedMode = tag == PROFILES_CHOSEN_PROFILE_TAG;
				if (selectedMode) {
					int colorWithAlpha = ColorUtilities.getColorWithAlpha(colorNoAlpha, 0.15f);
					Drawable[] layers = {new ColorDrawable(colorWithAlpha), drawable};
					drawable = new LayerDrawable(layers);
				}
			}

			AndroidUtils.setBackground(view, drawable);

			return view;
		}

		@NonNull
		private View getHelpToImproveItemView(@NonNull View view) {
			TextView feedbackButton = view.findViewById(R.id.feedbackButton);
			Drawable pollIcon = mIconsCache.getThemedIcon(R.drawable.ic_action_big_poll);
			feedbackButton.setCompoundDrawablesWithIntrinsicBounds(null, pollIcon, null, null);
			feedbackButton.setOnClickListener(v -> HelpArticleDialogFragment
							.instantiateWithUrl(HelpActivity.OSMAND_POLL_HTML, app.getString(R.string.feedback))
							.show(((FragmentActivity) getContext()).getSupportFragmentManager(), null));
			TextView contactUsButton = view.findViewById(R.id.contactUsButton);
			Drawable contactUsIcon = mIconsCache.getThemedIcon(R.drawable.ic_action_big_feedback);
			contactUsButton.setCompoundDrawablesWithIntrinsicBounds(null, contactUsIcon, null,
					null);
			String email = app.getString(R.string.support_email);
			contactUsButton.setOnClickListener(v -> {
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setData(Uri.parse("mailto:"));
				intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
				AndroidUtils.startActivityIfSafe(getContext(), intent);
			});
			return view;
		}

		@NonNull
		private View getOsmAndVersionView(@NonNull View view, @NonNull ContextMenuItem item) {
			TextView osmAndVersionText = view.findViewById(R.id.osmand_version);
			osmAndVersionText.setText(item.getTitle());

			String releaseDate = item.getDescription();
			if (!Algorithms.isEmpty(releaseDate)) {
				TextView releaseDateText = view.findViewById(R.id.release_date);
				releaseDateText.setText(releaseDate);
				AndroidUiHelper.updateVisibility(releaseDateText, true);
			}

			return view;
		}

		private void setupTitle(@NonNull TextView title, @NonNull ContextMenuItem item) {
			int colorId = ColorUtilities.getDefaultIconColorId(!lightTheme);
			Drawable drawable = item.getIcon() != ContextMenuItem.INVALID_ID
					? mIconsCache.getIcon(item.getIcon(), colorId)
					: null;
			if (drawable != null) {
				int paddingInPixels = (int) getContext().getResources().getDimension(R.dimen.bottom_sheet_icon_margin);
				int drawableSizeInPixels = (int) getContext().getResources().getDimension(R.dimen.standard_icon_size);
				drawable.setBounds(0, 0, drawableSizeInPixels, drawableSizeInPixels);
				title.setCompoundDrawablesRelative(drawable, null, null, null);
				title.setCompoundDrawablePadding(paddingInPixels);
			}
		}

		private void setupIcon(@NonNull AppCompatImageView icon, @NonNull ContextMenuItem item, @ColorInt int profileColor) {
			int iconId = item.getIcon();
			if (iconId != ContextMenuItem.INVALID_ID) {
				Integer color = item.getColor();
				Drawable drawable;
				if (color == null) {
					int colorId = ColorUtilities.getDefaultIconColorId(!lightTheme);
					colorId = item.shouldSkipPainting() ? 0 : colorId;
					drawable = mIconsCache.getIcon(iconId, colorId);
				} else if (profileDependent) {
					drawable = mIconsCache.getPaintedIcon(iconId, profileColor);
				} else {
					drawable = mIconsCache.getPaintedIcon(iconId, color);
				}

				icon.setImageDrawable(drawable);
				AndroidUiHelper.updateVisibility(icon, true);
			} else {
				AndroidUiHelper.updateVisibility(icon, false);
			}
		}

		private void setupSecondaryIcon(@NonNull ImageView secondaryIcon, @DrawableRes int secondaryIconId) {
			if (secondaryIconId != ContextMenuItem.INVALID_ID) {
				int colorId = ColorUtilities.getDefaultIconColorId(!lightTheme);
				Drawable drawable = mIconsCache.getIcon(secondaryIconId, colorId);
				secondaryIcon.setImageDrawable(drawable);
				if (secondaryIconId == R.drawable.ic_action_additional_option) {
					UiUtilities.rotateImageByLayoutDirection(secondaryIcon);
				}
				AndroidUiHelper.updateVisibility(secondaryIcon, true);
			} else {
				AndroidUiHelper.updateVisibility(secondaryIcon, false);
			}
		}

		private void setupToggle(@NonNull CompoundButton toggle, @NonNull ContextMenuItem item, int position,
		                         @ColorInt int profileColor) {
			Boolean selected = item.getSelected();
			if (selected != null) {
				toggle.setChecked(selected);
				ArrayAdapter<ContextMenuItem> adapter = this;
				OnCheckedChangeListener listener = (buttonView, isChecked) -> {
					item.setSelected(isChecked);
					ItemClickListener clickListener = item.getItemClickListener();
					if (clickListener != null) {
						clickListener.onContextMenuClick(adapter, item.getTitleId(), position, isChecked, null);
					}
				};
				toggle.setOnCheckedChangeListener(listener);
				AndroidUiHelper.updateVisibility(toggle, !item.shouldHideCompoundButton());
			} else {
				AndroidUiHelper.updateVisibility(toggle, false);
			}
			if (profileDependent) {
				UiUtilities.setupCompoundButton(nightMode, profileColor, toggle);
			}
		}

		private void setupSlider(@NonNull Slider slider, @NonNull ContextMenuItem item, @ColorInt int profileColor) {
			int selectedValue = item.getProgress();
			if (selectedValue != ContextMenuItem.INVALID_ID) {
				slider.setValue(selectedValue);
				slider.clearOnChangeListeners();
				slider.addOnChangeListener((slider1, value, fromUser) -> {
					OnIntegerValueChangedListener listener = item.getIntegerListener();
					int progress = (int) value;
					item.setProgress(progress);
					if (listener != null && fromUser) {
						listener.onIntegerValueChangedListener(progress);
					}
				});
				AndroidUiHelper.updateVisibility(slider, true);
			} else {
				AndroidUiHelper.updateVisibility(slider, false);
			}
			UiUtilities.setupSlider(slider, nightMode, profileColor);
		}

		private void setupProgressBar(@NonNull ProgressBar progressBar, @NonNull ContextMenuItem item) {
			if (item.isLoading()) {
				int progress = item.getProgress();
				if (progress == ContextMenuItem.INVALID_ID) {
					progressBar.setIndeterminate(true);
				} else {
					progressBar.setIndeterminate(false);
					progressBar.setProgress(progress);
				}
				AndroidUiHelper.updateVisibility(progressBar, true);
			} else {
				AndroidUiHelper.updateVisibility(progressBar, false);
			}
		}

		private void setupDescription(@NonNull TextView descriptionText, @Nullable String description, boolean noProgress) {
			if (description != null && noProgress) {
				descriptionText.setText(description);
				AndroidUiHelper.updateVisibility(descriptionText, true);
			} else {
				AndroidUiHelper.updateVisibility(descriptionText, false);
			}
		}

		private void showHideDivider(@NonNull View divider, @NonNull ContextMenuItem item, int position) {
			boolean hideDivider = position == getCount() - 1
					|| getItem(position + 1).isCategory()
					|| item.shouldHideDivider();
			AndroidUiHelper.updateVisibility(divider, !hideDivider);
		}
	}

	public interface ItemClickListener {

		/**
		 * @return true if drawer should be closed
		 */
		boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter,
								   int itemId,
								   int position,
								   boolean isChecked,
								   int[] viewCoordinates);
	}

	public interface ItemLongClickListener {

		/**
		 * @return true if drawer should be closed
		 */
		boolean onContextMenuLongClick(ArrayAdapter<ContextMenuItem> adapter,
									   int itemId,
									   int position,
									   boolean isChecked,
									   int[] viewCoordinates);
	}

	public interface ProgressListener {
		boolean onProgressChanged(Object progressObject,
								  int progress,
								  ArrayAdapter<ContextMenuItem> adapter,
								  int itemId,
								  int position);
	}

	public interface OnIntegerValueChangedListener {
		boolean onIntegerValueChangedListener(int newValue);
	}


	public static abstract class OnRowItemClick implements ItemClickListener {

		//boolean return type needed to describe if drawer needed to be close or not
		public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
			CompoundButton btn = view.findViewById(R.id.toggle_item);
			if (btn != null && btn.getVisibility() == View.VISIBLE) {
				btn.setChecked(!btn.isChecked());
				return false;
			} else {
				return onContextMenuClick(adapter, itemId, position, false, null);
			}
		}
	}

	public List<ContextMenuItem> getDefaultItems() {
		String idScheme = getIdScheme();
		List<ContextMenuItem> items = new ArrayList<>();
		for (ContextMenuItem item : this.items) {
			String id = item.getId();
			if (id != null && (id.startsWith(idScheme))) {
				items.add(item);
			}
		}
		return items;
	}

	private String getIdScheme() {
		String idScheme = "";
		for (ContextMenuItem item : items) {
			String id = item.getId();
			if (id != null) {
				ContextMenuItemsPreference pref = app.getSettings().getContextMenuItemsPreference(id);
				if (pref != null) {
					return pref.getIdScheme();
				}
			}
		}
		return idScheme;
	}

	public List<ContextMenuItem> getVisibleItems() {
		List<ContextMenuItem> visible = new ArrayList<>();
		for (ContextMenuItem item : items) {
			if (!item.isHidden()) {
				visible.add(item);
			}
		}
		return visible;
	}

	public static OnItemDeleteAction makeDeleteAction(OsmandPreference<?>... prefs) {
		return (appMode, profileOnly) -> {
			for (OsmandPreference<?> pref : prefs) {
				resetSetting(appMode, pref, profileOnly);
			}
		};
	}

	public static OnItemDeleteAction makeDeleteAction(List<? extends OsmandPreference<?>> prefs) {
		return makeDeleteAction(prefs.toArray(new OsmandPreference[0]));
	}

	private static void resetSetting(ApplicationMode appMode, OsmandPreference<?> preference, boolean profileOnly) {
		if (profileOnly) {
			preference.resetModeToDefault(appMode);
		} else {
			for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
				preference.resetModeToDefault(mode);
			}
		}
	}

	// when action is deleted or reset
	public interface OnItemDeleteAction {
		void itemWasDeleted(ApplicationMode appMode, boolean profileOnly);
	}
}