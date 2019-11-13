package net.osmand.plus.profiles;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class SelectProfileMenuAdapter extends AbstractProfileMenuAdapter<SelectProfileMenuAdapter.SelectProfileViewHolder> {

	private static final Log LOG = PlatformUtil.getLog(SelectProfileMenuAdapter.class);

	private List<Object> items = new ArrayList<>();
	private final OsmandApplication app;
	private ApplicationMode appMode;
	@ColorRes
	private int selectedIconColorRes;
	private boolean bottomButton;
	private String bottomButtonText;
	private static final String BUTTON_ITEM = "button_item";

	private boolean nightMode;

	public SelectProfileMenuAdapter(List<ApplicationMode> items, @NonNull OsmandApplication app,
	                                String bottomButtonText, boolean nightMode,
									@Nullable ApplicationMode appMode) {
		this.items.addAll(items);
		if (bottomButton) {
			this.items.add(BUTTON_ITEM);
		}
		this.app = app;
		this.appMode = appMode != null ? appMode : app.getSettings().getApplicationMode();
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
	

	public void updateItemsList(List<ApplicationMode> newList) {
		this.items.clear();
		this.items.addAll(newList);
		if (bottomButton) {
			items.add(BUTTON_ITEM);
		}
		notifyDataSetChanged();
	}

	@Override
	public int getItemViewType(int position) {
		return super.getItemViewType(position);
	}

	@NonNull
	@Override
	public SelectProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		int themeResId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View itemView =
				View.inflate(new ContextThemeWrapper(parent.getContext(), themeResId), R.layout.profile_list_item, null);
		return new SelectProfileViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull final SelectProfileViewHolder holder, int position) {
		Object obj = items.get(position);
		if (obj instanceof ApplicationMode) {
			holder.dividerBottom.setVisibility(View.INVISIBLE);
			holder.dividerUp.setVisibility(View.INVISIBLE);
			holder.icon.setVisibility(View.VISIBLE);
			holder.descr.setVisibility(View.VISIBLE);
			holder.switcher.setVisibility(View.GONE);
			holder.menuIcon.setVisibility(View.GONE);
			final ApplicationMode item = (ApplicationMode) obj;
			holder.title.setText(item.toHumanString(app));
			holder.descr.setText(BaseSettingsFragment.getAppModeDescription(app, item));

			int profileColorResId = item.getIconColorInfo().getColor(nightMode);
			holder.icon.setImageDrawable(app.getUIUtilities().getIcon(profileColorResId, selectedIconColorRes));
			
			//set up cell color
			int colorNoAlpha = ContextCompat.getColor(app, profileColorResId);
			boolean selectedMode = appMode == item;
			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, colorNoAlpha, 0.3f);

			if (selectedMode) {
				Drawable[] layers = {new ColorDrawable(UiUtilities.getColorWithAlpha(colorNoAlpha, 0.15f)), drawable};
				drawable = new LayerDrawable(layers);
			}
			AndroidUtils.setBackground(holder.profileOptions, drawable);

			updateViewHolder(holder, item);
		} else {
			final String title = (String) obj;
			if (title.equals(BUTTON_ITEM)) {
				holder.dividerBottom.setVisibility(View.INVISIBLE);
				holder.dividerUp.setVisibility(View.VISIBLE);
			}
			holder.icon.setVisibility(View.INVISIBLE);
			holder.descr.setVisibility(View.GONE);
			holder.switcher.setVisibility(View.GONE);
			holder.menuIcon.setVisibility(View.GONE);
			int color = ContextCompat.getColor(app, nightMode
					? R.color.active_color_primary_dark
					: R.color.active_color_primary_light);
			holder.title.setTextColor(color);
			holder.title.setText(bottomButtonText);

			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
			AndroidUtils.setBackground(holder.profileOptions, drawable);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	private void updateViewHolder(SelectProfileViewHolder holder, ApplicationMode mode) {
		int iconRes = mode.getIconRes();
		if (iconRes == 0 || iconRes == -1) {
			iconRes = R.drawable.ic_action_world_globe;
		}
		selectedIconColorRes = mode.getIconColorInfo().getColor(nightMode);
		holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, selectedIconColorRes));
	}

	class SelectProfileViewHolder extends ProfileAbstractViewHolder {

		SelectProfileViewHolder(View itemView) {
			super(itemView);

			profileOptions.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int pos = getAdapterPosition();
					if (pos != RecyclerView.NO_POSITION) {
						Object o = items.get(pos);
						if (o instanceof ApplicationMode && profilePressedListener != null) {
							profilePressedListener.onProfilePressed((ApplicationMode) o);
							notifyDataSetChanged();
						} else if (buttonPressedListener != null) {
							buttonPressedListener.onButtonPressed();
						}
					}
				}
			});
		}
	}
}