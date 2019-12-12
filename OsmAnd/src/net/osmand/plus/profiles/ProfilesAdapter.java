package net.osmand.plus.profiles;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.BaseSettingsFragment;

import java.util.Collections;
import java.util.List;

public class ProfilesAdapter extends AbstractProfileMenuAdapter<ProfilesAdapter.SelectProfileViewHolder>
		implements ProfilesItemTouchHelperCallback.ItemTouchHelperAdapter {

	private OsmandApplication app;
	private List<ApplicationMode> applicationModes;
	private ProfilesAdapterListener listener;

	private boolean nightMode;

	public ProfilesAdapter(MapActivity mapActivity, List<ApplicationMode> applicationModes) {
		setHasStableIds(true);
		app = mapActivity.getMyApplication();
		this.applicationModes = applicationModes;
		nightMode = !mapActivity.getMyApplication().getSettings().isLightContent();
	}

	public void setAdapterListener(ProfilesAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public SelectProfileViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		View itemView = UiUtilities.getInflater(viewGroup.getContext(), nightMode).inflate(R.layout.profile_list_item, null);
		itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listener.onItemClick(view);
			}
		});
		return new SelectProfileViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(final SelectProfileViewHolder holder, final int pos) {
		ApplicationMode mode = applicationModes.get(pos);

		holder.icon.setVisibility(View.VISIBLE);
		holder.descr.setVisibility(View.VISIBLE);
		holder.switcher.setVisibility(View.GONE);
		holder.menuIcon.setVisibility(View.VISIBLE);

		holder.title.setText(mode.toHumanString(app));
		holder.descr.setText(BaseSettingsFragment.getAppModeDescription(app, mode));

		//set up cell color
		int profileColorResId = mode.getIconColorInfo().getColor(nightMode);
		int colorNoAlpha = ContextCompat.getColor(app, profileColorResId);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, colorNoAlpha, 0.3f);

		AndroidUtils.setBackground(holder.profileOptions, drawable);

		updateViewHolder(holder, mode);

		holder.menuIcon.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
					listener.onDragStarted(holder);
				}
				return false;
			}
		});
	}

	@Override
	public int getItemCount() {
		return applicationModes.size();
	}

	@Override
	public boolean onItemMove(int from, int to) {
		Collections.swap(applicationModes, from, to);
		notifyItemMoved(from, to);
		return true;
	}

	@Override
	public long getItemId(int position) {
		return applicationModes.get(position).hashCode();
	}

	@Override
	public void onItemDismiss(RecyclerView.ViewHolder holder) {
		listener.onDragOrSwipeEnded(holder);
	}

	private void updateViewHolder(SelectProfileViewHolder holder, ApplicationMode mode) {
		int iconRes = mode.getIconRes();
		if (iconRes == 0 || iconRes == -1) {
			iconRes = R.drawable.ic_action_world_globe;
		}
		int selectedIconColorRes = mode.getIconColorInfo().getColor(nightMode);
		holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, selectedIconColorRes));
	}

	public ApplicationMode getItem(int position) {
		return applicationModes.get(position);
	}

	class SelectProfileViewHolder extends ProfileAbstractViewHolder {
		SelectProfileViewHolder(View itemView) {
			super(itemView);
		}
	}

	public interface ProfilesAdapterListener {

		void onItemClick(View view);

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragOrSwipeEnded(RecyclerView.ViewHolder holder);
	}
}