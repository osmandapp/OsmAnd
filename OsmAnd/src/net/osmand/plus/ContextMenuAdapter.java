package net.osmand.plus;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
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

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.slider.Slider;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.activities.HelpActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.dialogs.HelpArticleDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.ContextMenuItemsPreference;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SWITCH_PROFILE_ID;

public class ContextMenuAdapter {
	private static final Log LOG = PlatformUtil.getLog(ContextMenuAdapter.class);

	// Constants to determine profiles list item type (drawer menu items in 'Switch profile' mode)
	public static final int PROFILES_NORMAL_PROFILE_TAG = 0;
	public static final int PROFILES_CHOSEN_PROFILE_TAG = 1;
	public static final int PROFILES_CONTROL_BUTTON_TAG = 2;
	private static final int ITEMS_ORDER_STEP = 10;

	@LayoutRes
	private int DEFAULT_LAYOUT_ID = R.layout.list_menu_item_native;
	List<ContextMenuItem> items = new ArrayList<>();
	private boolean profileDependent = false;
	private boolean nightMode;
	private ConfigureMapMenu.OnClickListener changeAppModeListener = null;
	private OsmandApplication app;

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

	public void clearAdapter() { items.clear(); }

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
		private OsmandApplication app;
		private boolean lightTheme;
		@LayoutRes
		private int layoutId;
		private final ConfigureMapMenu.OnClickListener changeAppModeListener;
		private final UiUtilities mIconsCache;

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
				final Set<ApplicationMode> selected = new LinkedHashSet<>();
				return AppModeDialog.prepareAppModeDrawerView((Activity) getContext(),
						selected, true, new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								if (selected.size() > 0) {
									app.getSettings().setApplicationMode(selected.iterator().next());
									notifyDataSetChanged();
								}
								if (changeAppModeListener != null) {
									changeAppModeListener.onClick();
								}
							}
						});
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
				int colorNoAlpha = item.getColor();
				TextView title = convertView.findViewById(R.id.title);
				title.setText(item.getTitle());

				if (layoutId == R.layout.main_menu_drawer_btn_switch_profile) {
					ImageView icon = convertView.findViewById(R.id.icon);
					icon.setImageDrawable(mIconsCache.getPaintedIcon(item.getIcon(), colorNoAlpha));
					ImageView icArrow = convertView.findViewById(R.id.ic_expand_list);
					icArrow.setImageDrawable(mIconsCache.getIcon(item.getSecondaryIcon()));
					TextView desc = convertView.findViewById(R.id.description);
					desc.setText(item.getDescription());
				}
				if (layoutId == R.layout.main_menu_drawer_btn_configure_profile) {
					View fatDivider = convertView.findViewById(R.id.fatDivider);
					fatDivider.setBackgroundColor(colorNoAlpha);
				}

				Drawable selectableBg = UiUtilities.getColoredSelectableDrawable(app, colorNoAlpha, 0.3f);
				Drawable[] layers = {new ColorDrawable(UiUtilities.getColorWithAlpha(colorNoAlpha, 0.15f)), selectableBg};
				LayerDrawable layerDrawable = new LayerDrawable(layers);

				AndroidUtils.setBackground(convertView, layerDrawable);

				return convertView;
			}
			if (layoutId == R.layout.profile_list_item) {
				int tag = item.getTag();
				int colorNoAlpha = item.getColor();
				TextView title = convertView.findViewById(R.id.title);
				TextView desc = convertView.findViewById(R.id.description);
				ImageView icon = convertView.findViewById(R.id.icon);
				title.setText(item.getTitle());

				convertView.findViewById(R.id.divider_up).setVisibility(View.INVISIBLE);
				convertView.findViewById(R.id.divider_bottom).setVisibility(View.INVISIBLE);
				convertView.findViewById(R.id.menu_image).setVisibility(View.GONE);
				convertView.findViewById(R.id.compound_button).setVisibility(View.GONE);

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
						Drawable[] layers = {new ColorDrawable(UiUtilities.getColorWithAlpha(colorNoAlpha, 0.15f)), drawable};
						drawable = new LayerDrawable(layers);
					}
				}
				
				AndroidUtils.setBackground(convertView, drawable);
				
				return convertView;
			}
			if (layoutId == R.layout.help_to_improve_item) {
				TextView feedbackButton = (TextView) convertView.findViewById(R.id.feedbackButton);
				Drawable pollIcon = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_big_poll);
				feedbackButton.setCompoundDrawablesWithIntrinsicBounds(null, pollIcon, null, null);
				feedbackButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						HelpArticleDialogFragment
								.instantiateWithUrl(HelpActivity.OSMAND_POLL_HTML, app.getString(R.string.feedback))
								.show(((FragmentActivity) getContext()).getSupportFragmentManager(), null);
					}
				});
				TextView contactUsButton = (TextView) convertView.findViewById(R.id.contactUsButton);
				Drawable contactUsIcon =
						app.getUIUtilities().getThemedIcon(R.drawable.ic_action_big_feedback);
				contactUsButton.setCompoundDrawablesWithIntrinsicBounds(null, contactUsIcon, null,
						null);
				final String email = app.getString(R.string.support_email);
				contactUsButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(Intent.ACTION_SENDTO);
						intent.setData(Uri.parse("mailto:")); // only email apps should handle this
						intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
						if (intent.resolveActivity(app.getPackageManager()) != null) {
							getContext().startActivity(intent);
						}
					}
				});
				File logFile = app.getAppPath(OsmandApplication.EXCEPTION_PATH);
				View sendLogButtonDiv = convertView.findViewById(R.id.sendLogButtonDiv);
				TextView sendLogButton = (TextView) convertView.findViewById(R.id.sendLogButton);
				if (logFile.exists()) {
					Drawable sendLogIcon =
							app.getUIUtilities().getThemedIcon(R.drawable.ic_crashlog);
					sendLogButton.setCompoundDrawablesWithIntrinsicBounds(null, sendLogIcon, null, null);
					sendLogButton.setCompoundDrawablePadding(AndroidUtils.dpToPx(app, 8f));
					sendLogButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							app.sendCrashLog();
						}
					});
					sendLogButtonDiv.setVisibility(View.VISIBLE);
					sendLogButton.setVisibility(View.VISIBLE);
				} else {
					sendLogButtonDiv.setVisibility(View.GONE);
					sendLogButton.setVisibility(View.GONE);
				}
				return convertView;
			}

			TextView tv = (TextView) convertView.findViewById(R.id.title);
			if (tv != null) {
				tv.setText(item.getTitle());
			}

			if (this.layoutId == R.layout.simple_list_menu_item) {
				@ColorRes
				int color = lightTheme ? R.color.icon_color_default_light : R.color.icon_color_default_dark;
				Drawable drawable = item.getIcon() != ContextMenuItem.INVALID_ID
						? mIconsCache.getIcon(item.getIcon(), color) : null;
				if (drawable != null && tv != null) {
					int paddingInPixels = (int) getContext().getResources().getDimension(R.dimen.bottom_sheet_icon_margin);
					int drawableSizeInPixels = (int) getContext().getResources().getDimension(R.dimen.standard_icon_size);
					drawable.setBounds(0, 0, drawableSizeInPixels, drawableSizeInPixels);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
						tv.setCompoundDrawablesRelative(drawable, null, null, null);
					} else {
						tv.setCompoundDrawables(drawable, null, null, null);
					}
					tv.setCompoundDrawablePadding(paddingInPixels);
				}
			} else {
				if (item.getIcon() != ContextMenuItem.INVALID_ID) {
					Integer color = item.getColor();
					Drawable drawable;
					if (color == null) {
						int colorRes = lightTheme ? R.color.icon_color_default_light : R.color.icon_color_default_dark;
						colorRes = item.shouldSkipPainting() ? 0 : colorRes;
						drawable = mIconsCache.getIcon(item.getIcon(), colorRes);
					} else if (profileDependent) {
						drawable = mIconsCache.getPaintedIcon(item.getIcon(), currentModeColor);
					} else {
						drawable = mIconsCache.getPaintedIcon(item.getIcon(), color);
					}

					((AppCompatImageView) convertView.findViewById(R.id.icon)).setImageDrawable(drawable);
					convertView.findViewById(R.id.icon).setVisibility(View.VISIBLE);
				} else if (convertView.findViewById(R.id.icon) != null) {
					convertView.findViewById(R.id.icon).setVisibility(View.GONE);
				}
			}
			@DrawableRes
			int secondaryDrawable = item.getSecondaryIcon();
			if (secondaryDrawable != ContextMenuItem.INVALID_ID) {
				@ColorRes
				int colorRes;
				if (secondaryDrawable == R.drawable.ic_action_additional_option) {
					colorRes = lightTheme ? R.color.icon_color_default_light : R.color.icon_color_default_dark;
				} else {
					colorRes = lightTheme ? R.color.icon_color_default_light : R.color.icon_color_default_dark;
				}
				Drawable drawable = mIconsCache.getIcon(item.getSecondaryIcon(), colorRes);
				ImageView imageView = (ImageView) convertView.findViewById(R.id.secondary_icon);
				imageView.setImageDrawable(drawable);
				imageView.setVisibility(View.VISIBLE);
			} else {
				ImageView imageView = (ImageView) convertView.findViewById(R.id.secondary_icon);
				if (imageView != null) {
					imageView.setVisibility(View.GONE);
				}
			}

			if (convertView.findViewById(R.id.toggle_item) != null && !item.isCategory()) {
				final CompoundButton ch = (CompoundButton) convertView.findViewById(R.id.toggle_item);
				if (item.getSelected() != null) {
					ch.setOnCheckedChangeListener(null);
					ch.setVisibility(View.VISIBLE);
					ch.setChecked(item.getSelected());
					final ArrayAdapter<ContextMenuItem> la = this;
					final OnCheckedChangeListener listener = new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							ItemClickListener ca = item.getItemClickListener();
							item.setSelected(isChecked);
							if (ca != null) {
								ca.onContextMenuClick(la, item.getTitleId(), position, isChecked, null);
							}
						}
					};
					ch.setOnCheckedChangeListener(listener);
					ch.setVisibility(item.shouldHideCompoundButton() ? View.GONE : View.VISIBLE);
				} else if (ch != null) {
					ch.setVisibility(View.GONE);
				}
				if (profileDependent) {
					UiUtilities.setupCompoundButton(nightMode, currentModeColor, ch);
				}
			}

			Slider slider = (Slider) convertView.findViewById(R.id.slider);
			if (slider != null) {
				if (item.getProgress() != ContextMenuItem.INVALID_ID) {
					slider.setValue(item.getProgress());
					slider.addOnChangeListener(new Slider.OnChangeListener() {
						@Override
						public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
							OnIntegerValueChangedListener listener = item.getIntegerListener();
							int progress = (int) value;
							item.setProgress(progress);
							if (listener != null && fromUser) {
								listener.onIntegerValueChangedListener(progress);
							}
						}
					});
					slider.setVisibility(View.VISIBLE);
				} else {
					slider.setVisibility(View.GONE);
				}
				UiUtilities.setupSlider(slider, nightMode, currentModeColor);
			}

			View progressBar = convertView.findViewById(R.id.ProgressBar);
			if (progressBar != null) {
				ProgressBar bar = (ProgressBar) progressBar;
				if (item.isLoading()) {
					int progress = item.getProgress();
					if (progress == ContextMenuItem.INVALID_ID) {
						bar.setIndeterminate(true);
					} else {
						bar.setIndeterminate(false);
						bar.setProgress(progress);
					}
					bar.setVisibility(View.VISIBLE);
				} else {
					bar.setVisibility(View.GONE);
				}
			}

			View descriptionTextView = convertView.findViewById(R.id.description);
			if (descriptionTextView != null) {
				String itemDescr = item.getDescription();
				if (itemDescr != null && (progressBar == null || !item.isLoading())) {
					((TextView) descriptionTextView).setText(itemDescr);
					descriptionTextView.setVisibility(View.VISIBLE);
				} else {
					descriptionTextView.setVisibility(View.GONE);
				}
			}

			View dividerView = convertView.findViewById(R.id.divider);
			if (dividerView != null) {
				if (getCount() - 1 == position || getItem(position + 1).isCategory()
						|| item.shouldHideDivider()) {
					dividerView.setVisibility(View.GONE);
				} else {
					dividerView.setVisibility(View.VISIBLE);
				}
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
	}

	public interface ItemClickListener {
		//boolean return type needed to desribe if drawer needed to be close or not
		boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter,
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
			CompoundButton btn = (CompoundButton) view.findViewById(R.id.toggle_item);
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

	public static OnItemDeleteAction makeDeleteAction(final OsmandPreference... prefs) {
		return new OnItemDeleteAction() {
			@Override
			public void itemWasDeleted(ApplicationMode appMode, boolean profileOnly) {
				for (OsmandPreference pref : prefs) {
					resetSetting(appMode, pref, profileOnly);
				}
			}
		};
	}

	public static OnItemDeleteAction makeDeleteAction(final List<? extends OsmandPreference> prefs) {
		return makeDeleteAction(prefs.toArray(new OsmandPreference[0]));
	}

	private static void resetSetting(ApplicationMode appMode, OsmandPreference preference, boolean profileOnly) {
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