package net.osmand.plus.widgets.ctxmenu;

import static net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem.INVALID_ID;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnIntegerValueChangedListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.util.Algorithms;

import java.util.LinkedHashSet;
import java.util.Set;

public class ViewCreator {

	// Constants to determine profiles list item type (drawer menu items in 'Switch profile' mode)
	public static final int PROFILES_NORMAL_PROFILE_TAG = 0;
	public static final int PROFILES_CHOSEN_PROFILE_TAG = 1;
	public static final int PROFILES_CONTROL_BUTTON_TAG = 2;

	private final Activity ctx;
	private final UiUtilities iconsCache;
	private final boolean nightMode;

	private OnDataChangeUiAdapter uiAdapter;
	@LayoutRes
	private int defLayoutId = R.layout.list_menu_item_native;
	private Integer customControlsColor;

	public ViewCreator(@NonNull Activity ctx, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		this.ctx = ctx;
		this.nightMode = nightMode;
		iconsCache = app.getUIUtilities();
	}

	public void setUiAdapter(OnDataChangeUiAdapter uiAdapter) {
		this.uiAdapter = uiAdapter;
	}

	public void setDefaultLayoutId(int defLayoutId) {
		this.defLayoutId = defLayoutId;
	}

	public void setCustomControlsColor(Integer customControlsColor) {
		this.customControlsColor = customControlsColor;
	}

	public int getDefaultLayoutId() {
		return defLayoutId;
	}

	@NonNull
	public View getView(@NonNull ContextMenuItem item, @Nullable View convertView) {
		int layoutId = item.getLayout() != INVALID_ID ? item.getLayout() : defLayoutId;
		if (convertView == null || !(convertView.getTag() instanceof Integer)
				|| (layoutId != (Integer) convertView.getTag())) {
			int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
			convertView = View.inflate(new ContextThemeWrapper(ctx, themeRes), layoutId, null);
			convertView.setTag(layoutId);
		}
		if (item.getMinHeight() > 0) {
			convertView.setMinimumHeight(item.getMinHeight());
		}

		View specialView = getSpecialView(layoutId, item, convertView);
		if (specialView != null) {
			return specialView;
		}

		TextView title = convertView.findViewById(R.id.title);
		if (title != null) {
			title.setText(item.getTitle());
		}

		if (this.defLayoutId == R.layout.simple_list_menu_item) {
			if (title != null) {
				setupTitle(title, item);
			}
		} else {
			AppCompatImageView icon = convertView.findViewById(R.id.icon);
			if (icon != null) {
				setupIcon(icon, item);
			}
		}

		ImageView secondaryIcon = convertView.findViewById(R.id.secondary_icon);
		if (secondaryIcon != null) {
			setupSecondaryIcon(secondaryIcon, item);
		}

		CompoundButton toggle = convertView.findViewById(R.id.toggle_item);
		if (toggle != null && !item.isCategory()) {
			setupToggle(toggle, convertView, item);
		}

		Slider slider = convertView.findViewById(R.id.slider);
		if (slider != null) {
			setupSlider(slider, item);
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

		TextView secondaryDescription = convertView.findViewById(R.id.secondary_description);
		if (secondaryDescription != null) {
			setupSecondaryDescription(secondaryDescription, item.getSecondaryDescription());
		}

		View dividerView = convertView.findViewById(R.id.divider);
		if (dividerView != null) {
			boolean hideDivider = item.shouldHideDivider();
			AndroidUiHelper.updateVisibility(dividerView, !hideDivider);
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

	@Nullable
	private View getSpecialView(@LayoutRes int layoutId, @NonNull ContextMenuItem item, @NonNull View convertView) {
		if (layoutId == R.layout.mode_toggles) {
			return getAppModeToggleView();
		} else if (layoutId == R.layout.main_menu_drawer_btn_switch_profile ||
				layoutId == R.layout.main_menu_drawer_btn_configure_profile) {
			return getDrawerProfileView(convertView, layoutId, item);
		} else if (layoutId == R.layout.profile_list_item) {
			return getProfileListItemView(convertView, item);
		} else if (layoutId == R.layout.main_menu_drawer_osmand_version) {
			return getOsmAndVersionView(convertView, item);
		} else if (layoutId == R.layout.list_item_terrain_description) {
			return getTerrainDescriptionView(convertView, item);
		}
		return null;
	}

	@NonNull
	private View getAppModeToggleView() {
		Set<ApplicationMode> selected = new LinkedHashSet<>();
		return AppModeDialog.prepareAppModeDrawerView(ctx, selected, true,
				view -> {
					if (selected.size() > 0) {
						OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
						OsmandSettings settings = app.getSettings();
						settings.setApplicationMode(selected.iterator().next());
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
			icon.setImageDrawable(iconsCache.getPaintedIcon(item.getIcon(), color));
			ImageView icArrow = view.findViewById(R.id.ic_expand_list);
			icArrow.setImageDrawable(iconsCache.getIcon(item.getSecondaryIcon()));
			TextView desc = view.findViewById(R.id.description);
			desc.setText(item.getDescription());
		} else if (layoutId == R.layout.main_menu_drawer_btn_configure_profile) {
			View fatDivider = view.findViewById(R.id.fatDivider);
			fatDivider.setBackgroundColor(color);
		}

		Drawable selectableBg = UiUtilities.getColoredSelectableDrawable(ctx, color, 0.3f);
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

		Drawable drawable = UiUtilities.getColoredSelectableDrawable(ctx, colorNoAlpha, 0.3f);

		if (tag == PROFILES_CONTROL_BUTTON_TAG) {
			title.setTextColor(colorNoAlpha);
			icon.setVisibility(View.INVISIBLE);
			desc.setVisibility(View.GONE);
		} else {
			AndroidUiHelper.updateVisibility(icon, true);
			AndroidUiHelper.updateVisibility(desc, true);
			AndroidUtils.setTextPrimaryColor(ctx, title, nightMode);
			icon.setImageDrawable(iconsCache.getPaintedIcon(item.getIcon(), colorNoAlpha));
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
	private View getTerrainDescriptionView(@NonNull View view, @NonNull ContextMenuItem item) {
		View clickableButton = view.findViewById(R.id.button_get_clickable);
		clickableButton.setOnClickListener(v -> {
			ItemClickListener listener = item.getItemClickListener();
			if (listener != null) {
				listener.onContextMenuClick(uiAdapter, view, item, false);
			}
		});
		View button = view.findViewById(R.id.button_get);
		UiUtilities.setupDialogButton(nightMode, button, DialogButtonType.SECONDARY_ACTIVE, R.string.shared_string_get);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), PluginsHelper.isEnabled(NauticalMapsPlugin.class));
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

	private void setupTitle(@NonNull TextView tvTitle, @NonNull ContextMenuItem item) {
		int colorId = ColorUtilities.getDefaultIconColorId(nightMode);
		Drawable drawable = item.getIcon() != INVALID_ID
				? iconsCache.getIcon(item.getIcon(), colorId)
				: null;
		if (drawable != null) {
			int paddingInPixels = (int) ctx.getResources().getDimension(R.dimen.bottom_sheet_icon_margin);
			int drawableSizeInPixels = (int) ctx.getResources().getDimension(R.dimen.standard_icon_size);
			drawable.setBounds(0, 0, drawableSizeInPixels, drawableSizeInPixels);
			tvTitle.setCompoundDrawablesRelative(drawable, null, null, null);
			tvTitle.setCompoundDrawablePadding(paddingInPixels);
		}
	}

	private void setupIcon(@NonNull AppCompatImageView icon, @NonNull ContextMenuItem item) {
		int iconId = item.getIcon();
		if (iconId != INVALID_ID) {
			Integer color = item.getColor();
			Drawable drawable;
			if (color == null) {
				int colorId = ColorUtilities.getDefaultIconColorId(nightMode);
				colorId = item.useNaturalIconColor() ? 0 : colorId;
				drawable = iconsCache.getIcon(iconId, colorId);
			} else if (customControlsColor != null) {
				drawable = iconsCache.getPaintedIcon(iconId, customControlsColor);
			} else {
				drawable = iconsCache.getPaintedIcon(iconId, color);
			}

			icon.setImageDrawable(drawable);
			AndroidUiHelper.updateVisibility(icon, true);
		} else {
			AndroidUiHelper.updateVisibility(icon, false);
		}
	}

	private void setupSecondaryIcon(@NonNull ImageView secondaryIcon, @NonNull ContextMenuItem item) {
		int secondaryIconId = item.getSecondaryIcon();
		if (secondaryIconId != INVALID_ID) {
			int colorId = ColorUtilities.getDefaultIconColorId(nightMode);
			colorId = item.useNaturalSecondIconColor() ? 0 : colorId;
			Drawable drawable = iconsCache.getIcon(secondaryIconId, colorId);
			secondaryIcon.setImageDrawable(drawable);
			if (secondaryIconId == R.drawable.ic_action_additional_option) {
				UiUtilities.rotateImageByLayoutDirection(secondaryIcon);
			}
			AndroidUiHelper.updateVisibility(secondaryIcon, true);
		} else {
			AndroidUiHelper.updateVisibility(secondaryIcon, false);
		}
	}

	private void setupToggle(@NonNull CompoundButton toggle, @NonNull View convertView, @NonNull ContextMenuItem item) {
		Boolean selected = item.getSelected();
		if (selected != null) {
			toggle.setOnCheckedChangeListener(null); // Removing listener required before checking/unchecking
			toggle.setChecked(selected);
			OnCheckedChangeListener listener = (buttonView, isChecked) -> {
				item.setSelected(isChecked);
				ItemClickListener clickListener = item.getItemClickListener();
				if (clickListener != null) {
					clickListener.onContextMenuClick(uiAdapter, convertView, item, isChecked);
				}
			};
			toggle.setOnCheckedChangeListener(listener);
			AndroidUiHelper.updateVisibility(toggle, !item.shouldHideCompoundButton());
		} else {
			AndroidUiHelper.updateVisibility(toggle, false);
		}
		if (customControlsColor != null) {
			UiUtilities.setupCompoundButton(nightMode, customControlsColor, toggle);
		}
		toggle.setSaveEnabled(false);
	}

	private void setupSlider(@NonNull Slider slider, @NonNull ContextMenuItem item) {
		int selectedValue = item.getProgress();
		if (selectedValue != INVALID_ID) {
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
		if (customControlsColor != null) {
			UiUtilities.setupSlider(slider, nightMode, customControlsColor);
		} else {
			int activeColor = ColorUtilities.getActiveColor(ctx, nightMode);
			UiUtilities.setupSlider(slider, nightMode, activeColor);
		}
	}

	private void setupProgressBar(@NonNull ProgressBar progressBar, @NonNull ContextMenuItem item) {
		if (item.isLoading()) {
			int progress = item.getProgress();
			if (progress == INVALID_ID) {
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

	private void setupDescription(@NonNull TextView tvDesc, @Nullable String description, boolean noProgress) {
		if (description != null && noProgress) {
			tvDesc.setText(description);
			AndroidUiHelper.updateVisibility(tvDesc, true);
		} else {
			AndroidUiHelper.updateVisibility(tvDesc, false);
		}
	}

	private void setupSecondaryDescription(@NonNull TextView tvDesc, @Nullable String description) {
		if (description != null) {
			tvDesc.setText(description);
			AndroidUiHelper.updateVisibility(tvDesc, true);
		} else {
			AndroidUiHelper.updateVisibility(tvDesc, false);
		}
	}

}
