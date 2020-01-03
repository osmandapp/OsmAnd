package net.osmand.plus.profiles;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class ConfigureProfileMenuAdapter extends AbstractProfileMenuAdapter<ConfigureProfileMenuAdapter.ConfigureProfileViewHolder> {

	private static final Log LOG = PlatformUtil.getLog(ConfigureProfileMenuAdapter.class);

	private List<Object> items = new ArrayList<>();
	private Set<ApplicationMode> selectedItems;

	@Nullable
	private ProfileSelectedListener profileSelectedListener;
	private final OsmandApplication app;
	@ColorRes
	private int selectedIconColorRes;
	private boolean bottomButton;
	private String bottomButtonText;
	private static final String BUTTON_ITEM = "button_item";

	private boolean nightMode;

	public ConfigureProfileMenuAdapter(List<ApplicationMode> items, Set<ApplicationMode> selectedItems,
	                                   OsmandApplication app, String bottomButtonText, boolean nightMode) {
		this.items.addAll(items);
		if (bottomButton) {
			this.items.add(BUTTON_ITEM);
		}
		this.app = app;
		this.selectedItems = selectedItems;
		this.bottomButton = !Algorithms.isEmpty(bottomButtonText);
		this.bottomButtonText = bottomButtonText;
		this.nightMode = nightMode;
		selectedIconColorRes = nightMode
				? R.color.active_color_primary_dark
				: R.color.active_color_primary_light;
	}

	public List<Object> getItems() {
		return items;
	}

	public void setProfileSelectedListener(@Nullable ProfileSelectedListener profileSelectedListener) {
		this.profileSelectedListener = profileSelectedListener;
	}

	public void updateItemsList(List<ApplicationMode> newList, Set<ApplicationMode> selectedItems) {
		this.items.clear();
		this.selectedItems.clear();
		this.items.addAll(newList);
		if (bottomButton) {
			items.add(BUTTON_ITEM);
		}
		this.selectedItems.addAll(selectedItems);
		notifyDataSetChanged();
	}

	@Override
	public int getItemViewType(int position) {
		return super.getItemViewType(position);
	}

	@NonNull
	@Override
	public ConfigureProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		int themeResId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View itemView = 
				View.inflate(new ContextThemeWrapper(parent.getContext(), themeResId), R.layout.profile_list_item, null);
		return new ConfigureProfileViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull final ConfigureProfileViewHolder holder, int position) {
		Object obj = items.get(position);
		holder.dividerUp.setVisibility(View.INVISIBLE);
		if (obj instanceof ApplicationMode) {
			holder.dividerBottom.setVisibility(View.VISIBLE);
			holder.icon.setVisibility(View.VISIBLE);
			holder.descr.setVisibility(View.VISIBLE);
			holder.compoundButton.setVisibility(View.VISIBLE);
			holder.menuIcon.setVisibility(View.VISIBLE);
			final ApplicationMode item = (ApplicationMode) obj;
			holder.title.setText(item.toHumanString(app));
			holder.descr.setText(BaseSettingsFragment.getAppModeDescription(app, item));

			holder.initSwitcher = true;
			holder.compoundButton.setChecked(selectedItems.contains(item));
			holder.initSwitcher = false;
			updateViewHolder(holder, item);
		} else {
			final String title = (String) obj;
			if (title.equals(BUTTON_ITEM)) {
				holder.dividerBottom.setVisibility(View.INVISIBLE);
			}
			holder.icon.setVisibility(View.INVISIBLE);
			holder.descr.setVisibility(View.GONE);
			holder.compoundButton.setVisibility(View.GONE);
			holder.menuIcon.setVisibility(View.GONE);
			holder.title.setTextColor(app.getResources().getColor(
				nightMode
				? R.color.active_color_primary_dark
				: R.color.active_color_primary_light));
			holder.title.setText(bottomButtonText);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	private void updateViewHolder(ConfigureProfileViewHolder holder, ApplicationMode mode) {
		int iconRes = mode.getIconRes();
		if (iconRes == 0 || iconRes == -1) {
			iconRes = R.drawable.ic_action_world_globe;
		}
		selectedIconColorRes = mode.getIconColorInfo().getColor(nightMode);
		if (selectedItems.contains(mode)) {
			holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, selectedIconColorRes));
		} else {
			holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, R.color.profile_icon_color_inactive));
		}
	}

	class ConfigureProfileViewHolder extends ProfileAbstractViewHolder {

		boolean initSwitcher;

		ConfigureProfileViewHolder(View itemView) {
			super(itemView);

			profileOptions.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					int pos = getAdapterPosition();
					if (pos != RecyclerView.NO_POSITION) {
						Object o = items.get(pos);
						if (o instanceof ApplicationMode && profilePressedListener != null) {
							profilePressedListener.onProfilePressed((ApplicationMode) o);
						} else if (buttonPressedListener != null) {
							buttonPressedListener.onButtonPressed();
						}
					}
				}
			});
			compoundButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					int pos = getAdapterPosition();
					if (pos != RecyclerView.NO_POSITION && profileSelectedListener != null && !initSwitcher) {
						Object o = items.get(pos);
						if (o instanceof ApplicationMode) {
							final ApplicationMode item = (ApplicationMode) o;
							if (isChecked) {
								selectedItems.add(item);
							} else {
								selectedItems.remove(item);
							}
							updateViewHolder(ConfigureProfileViewHolder.this, item);
							profileSelectedListener.onProfileSelected(item, isChecked);
						}
					}
				}
			});
		}
	}

	public interface ProfileSelectedListener {
		void onProfileSelected(ApplicationMode item, boolean isChecked);
	}
}
