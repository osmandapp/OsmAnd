package net.osmand.plus.profiles;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class ConfigureProfileMenuAdapter extends AbstractProfileMenuAdapter<ConfigureProfileMenuAdapter.ConfigureProfileViewHolder> {

	private static final Log LOG = PlatformUtil.getLog(ConfigureProfileMenuAdapter.class);

	private final List<Object> items = new ArrayList<>();
	private final Set<ApplicationMode> selectedItems;

	@Nullable
	private ProfileSelectedListener profileSelectedListener;
	private final OsmandApplication app;
	@ColorInt
	private int selectedIconColor;
	private boolean bottomButton;
	private final String bottomButtonText;
	private static final String BUTTON_ITEM = "button_item";

	private final boolean nightMode;

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
		int selectedIconColorRes = ColorUtilities.getActiveColorId(nightMode);
		selectedIconColor = ContextCompat.getColor(app, selectedIconColorRes);
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

	@NonNull
	@Override
	public ConfigureProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		int themeResId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View itemView = 
				View.inflate(new ContextThemeWrapper(parent.getContext(), themeResId), R.layout.profile_list_item, null);
		return new ConfigureProfileViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull ConfigureProfileViewHolder holder, int position) {
		Object obj = items.get(position);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		holder.dividerUp.setVisibility(View.INVISIBLE);
		if (obj instanceof ApplicationMode) {
			holder.dividerBottom.setVisibility(View.VISIBLE);
			holder.icon.setVisibility(View.VISIBLE);
			holder.descr.setVisibility(View.VISIBLE);
			holder.compoundButton.setVisibility(View.VISIBLE);
			holder.menuIcon.setVisibility(View.VISIBLE);
			ApplicationMode item = (ApplicationMode) obj;
			holder.title.setText(item.toHumanString());
			holder.descr.setText(ProfileDataUtils.getAppModeDescription(app, item));

			holder.initSwitcher = true;
			holder.compoundButton.setChecked(selectedItems.contains(item));
			holder.initSwitcher = false;
			updateViewHolder(holder, item);
		} else {
			String title = (String) obj;
			if (title.equals(BUTTON_ITEM)) {
				holder.dividerBottom.setVisibility(View.INVISIBLE);
			}
			holder.icon.setVisibility(View.INVISIBLE);
			holder.descr.setVisibility(View.GONE);
			holder.compoundButton.setVisibility(View.GONE);
			holder.menuIcon.setVisibility(View.GONE);
			holder.title.setTextColor(activeColor);
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
		selectedIconColor = mode.getProfileColor(nightMode);
		if (selectedItems.contains(mode)) {
			holder.icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconRes, selectedIconColor));
		} else {
			holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, R.color.icon_color_default_light));
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
							ApplicationMode item = (ApplicationMode) o;
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
