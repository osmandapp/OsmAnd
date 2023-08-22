package net.osmand.plus.settings.fragments;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIVIDER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_RENDERING_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OPEN_STREET_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ROUTES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.SHOW_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_CATEGORY_ID;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.ConfigureMapMenu;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.CtxMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ConfigureMenuRootFragment extends BaseOsmAndFragment {

	public static final String TAG = ConfigureMenuRootFragment.class.getName();
	private static final String APP_MODE_KEY = "app_mode_key";
	private static final Log LOG = PlatformUtil.getLog(TAG);

	private LayoutInflater mInflater;
	private ApplicationMode appMode;
	private Activity activity;

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
									   @NonNull ApplicationMode appMode,
									   @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ConfigureMenuRootFragment fragment = new ConfigureMenuRootFragment();
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
		}
		nightMode = !app.getSettings().isLightContentForMode(appMode);
		mInflater = UiUtilities.getInflater(app, nightMode);
		activity = getActivity();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Context ctx = requireContext();
		View root = mInflater.inflate(R.layout.fragment_ui_customization, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		TextView toolbarTitle = root.findViewById(R.id.toolbar_title);
		TextView toolbarSubTitle = root.findViewById(R.id.toolbar_subtitle);
		ImageButton toolbarButton = root.findViewById(R.id.close_button);
		RecyclerView recyclerView = root.findViewById(R.id.list);
		toolbar.setBackgroundColor(ColorUtilities.getListBgColor(ctx, nightMode));
		toolbarTitle.setTextColor(getColor(nightMode ? R.color.text_color_primary_dark : R.color.list_background_color_dark));
		toolbarSubTitle.setTextColor(getColor(R.color.text_color_secondary_light));
		toolbarButton.setImageDrawable(getPaintedContentIcon(
				AndroidUtils.getNavigationIconResId(app),
				getColor(R.color.text_color_secondary_light)));
		toolbarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				FragmentManager fm = getFragmentManager();
				if (fm != null) {
					fm.popBackStack();
				}
			}
		});
		toolbarTitle.setText(R.string.ui_customization);
		toolbarSubTitle.setText(appMode.toHumanString());
		toolbarSubTitle.setVisibility(View.VISIBLE);
		List<Object> items = new ArrayList<>();
		String plugins = getString(R.string.prefs_plugins);
		String description = String.format(getString(R.string.ui_customization_description), plugins);
		items.add(description);
		items.addAll(Arrays.asList(ScreenType.values()));
		CustomizationItemsAdapter adapter = new CustomizationItemsAdapter(items, new OnCustomizationItemClickListener() {
			@Override
			public void onItemClick(ScreenType type) {
				FragmentManager fm = getFragmentManager();
				if (fm != null) {
					ConfigureMenuItemsFragment.showInstance(fm, appMode, type);
				}
			}
		});
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);
		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), root);
		}
		return root;
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(APP_MODE_KEY, getAppMode().getStringKey());
	}

	@Override
	public void onResume() {
		super.onResume();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).disableDrawer();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).enableDrawer();
		}
	}

	public void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public ApplicationMode getAppMode() {
		return appMode != null ? appMode : app.getSettings().getApplicationMode();
	}

	private class CustomizationItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private static final int DESCRIPTION_TYPE = 0;
		private static final int ITEM_TYPE = 1;

		private final List<Object> items;
		private final OnCustomizationItemClickListener listener;

		CustomizationItemsAdapter(List<Object> items, OnCustomizationItemClickListener listener) {
			this.items = items;
			this.listener = listener;
		}

		@Override
		public int getItemViewType(int position) {
			if (items.get(position) instanceof String) {
				return DESCRIPTION_TYPE;
			} else {
				return ITEM_TYPE;
			}
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			if (viewType == DESCRIPTION_TYPE) {
				View view = mInflater.inflate(R.layout.list_item_description_with_image, parent, false);
				return new DescriptionHolder(view);

			} else {
				View view = mInflater.inflate(R.layout.list_item_ui_customization, parent, false);
				return new ItemHolder(view);
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			Object currentItem = items.get(position);
			if (holder instanceof DescriptionHolder) {
				DescriptionHolder descriptionHolder = (DescriptionHolder) holder;
				String plugins = getString(R.string.prefs_plugins);
				setupClickableText(descriptionHolder.description, (String) currentItem, plugins);
				descriptionHolder.image.setVisibility(View.GONE);
			} else {
				ScreenType item = (ScreenType) currentItem;
				ItemHolder itemHolder = (ItemHolder) holder;
				Drawable d = app.getUIUtilities().getIcon(item.iconRes, nightMode);
				itemHolder.icon.setImageDrawable(AndroidUtils.getDrawableForDirection(app, d));
				itemHolder.title.setText(item.titleRes);
				itemHolder.subTitle.setText(getSubTitleText(item));
				itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						listener.onItemClick(item);
					}
				});
			}
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		private void setupClickableText(TextView textView, String text, String clickableText) {
			SpannableString spannableString = new SpannableString(text);
			ClickableSpan clickableSpan = new ClickableSpan() {
				@Override
				public void onClick(@NonNull View view) {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						PluginsFragment.showInstance(activity.getSupportFragmentManager());
					}
				}
			};
			try {
				int startIndex = text.indexOf(clickableText);
				spannableString.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), startIndex, startIndex + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				spannableString.setSpan(clickableSpan, startIndex, startIndex + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				textView.setText(spannableString);
				textView.setMovementMethod(LinkMovementMethod.getInstance());
				textView.setHighlightColor(ColorUtilities.getActiveColor(app, nightMode));
			} catch (RuntimeException e) {
				LOG.error("Error trying to find index of " + clickableText + " " + e);
			}
		}

		private String getSubTitleText(ScreenType type) {
			ContextMenuAdapter contextMenuAdapter = null;
			Activity activity = getActivity();
			if (activity instanceof MapActivity) {
				switch (type) {
					case DRAWER:
						MapActivityActions mapActivityActions = new MapActivityActions((MapActivity) activity);
						contextMenuAdapter = mapActivityActions.createMainOptionsMenu();
						break;
					case CONFIGURE_MAP:
						ConfigureMapMenu configureMapMenu = new ConfigureMapMenu(app);
						contextMenuAdapter = configureMapMenu.createListAdapter((MapActivity) activity);
						break;
					case CONTEXT_MENU_ACTIONS:
						MapContextMenu menu = ((MapActivity) activity).getContextMenu();
						contextMenuAdapter = menu.getActionsContextMenuAdapter(true);
						break;
				}
				int hiddenCount = ConfigureMenuItemsFragment.getSettingForScreen(app, type).getModeValue(appMode).getHiddenIds().size();
				List<ContextMenuItem> allItems = CtxMenuUtils.getCustomizableItems(contextMenuAdapter);
				if (type == ScreenType.DRAWER || type == ScreenType.CONFIGURE_MAP) {
					Iterator<ContextMenuItem> iterator = allItems.iterator();
					while (iterator.hasNext()) {
						String id = iterator.next().getId();
						if (DRAWER_DIVIDER_ID.equals(id)
								|| SHOW_CATEGORY_ID.equals(id)
								|| TERRAIN_CATEGORY_ID.equals(id)
								|| OPEN_STREET_MAP.equals(id)
								|| ROUTES_ID.equals(id)
								|| MAP_RENDERING_CATEGORY_ID.equals(id)) {
							iterator.remove();
						}
					}
				}
				int allCount = allItems.size();
				String amount = getString(R.string.n_items_of_z, String.valueOf(allCount - hiddenCount), String.valueOf(allCount));
				return getString(R.string.shared_string_items) + " : " + amount;
			}
			return "";
		}

		class DescriptionHolder extends RecyclerView.ViewHolder {
			ImageView image;
			TextView description;

			DescriptionHolder(@NonNull View itemView) {
				super(itemView);
				image = itemView.findViewById(R.id.image);
				description = itemView.findViewById(R.id.description);
			}
		}

		class ItemHolder extends RecyclerView.ViewHolder {
			ImageView icon;
			TextView title;
			TextView subTitle;

			ItemHolder(@NonNull View itemView) {
				super(itemView);
				icon = itemView.findViewById(R.id.icon);
				title = itemView.findViewById(R.id.title);
				subTitle = itemView.findViewById(R.id.sub_title);
			}
		}
	}

	public enum ScreenType {
		DRAWER(R.string.shared_string_drawer, R.drawable.ic_action_drawer, R.drawable.img_settings_customize_drawer_day, R.drawable.img_settings_customize_drawer_night),
		CONFIGURE_MAP(R.string.configure_map, R.drawable.ic_action_layers, R.drawable.img_settings_customize_configure_map_day, R.drawable.img_settings_customize_configure_map_night),
		CONTEXT_MENU_ACTIONS(R.string.context_menu_actions, R.drawable.ic_action_context_menu, R.drawable.img_settings_customize_context_menu_day, R.drawable.img_settings_customize_context_menu_night);

		@StringRes
		public int titleRes;
		@DrawableRes
		public int iconRes;
		@DrawableRes
		public int imageDayRes;
		@DrawableRes
		public int imageNightRes;

		ScreenType(int titleRes, int iconRes, int imageDayRes, int imageNightRes) {
			this.titleRes = titleRes;
			this.iconRes = iconRes;
			this.imageDayRes = imageDayRes;
			this.imageNightRes = imageNightRes;
		}
	}

	interface OnCustomizationItemClickListener {
		void onItemClick(ScreenType type);
	}
}
