package net.osmand.plus.profiles;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.BaseSettingsFragment;

import java.util.Collections;
import java.util.List;

public class ProfilesAdapter extends RecyclerView.Adapter<ProfilesAdapter.ProfileViewHolder>
		implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

	private OsmandApplication app;
	private UiUtilities uiUtilities;
	private List<ApplicationMode> applicationModes;
	private ProfilesAdapterListener listener;

	private boolean nightMode;

	public ProfilesAdapter(MapActivity mapActivity, List<ApplicationMode> appModes) {
		setHasStableIds(true);
		app = mapActivity.getMyApplication();
		uiUtilities = app.getUIUtilities();
		applicationModes = appModes;
		nightMode = !mapActivity.getMyApplication().getSettings().isLightContent();
	}

	public void setAdapterListener(ProfilesAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public ProfileViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		LayoutInflater themedInflater = UiUtilities.getInflater(viewGroup.getContext(), nightMode);
		View itemView = themedInflater.inflate(R.layout.profile_edit_list_item, viewGroup, false);
		return new ProfileViewHolder(itemView);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onBindViewHolder(final ProfileViewHolder holder, final int pos) {
		ApplicationMode mode = applicationModes.get(pos);

		holder.title.setText(mode.toHumanString(app));
		holder.description.setText(BaseSettingsFragment.getAppModeDescription(app, mode));

		updateViewHolder(holder, mode);

		holder.moveIcon.setOnTouchListener(new View.OnTouchListener() {
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

	private void updateViewHolder(ProfileViewHolder holder, ApplicationMode mode) {
		int iconRes = mode.getIconRes();
		if (iconRes == 0 || iconRes == -1) {
			iconRes = R.drawable.ic_action_world_globe;
		}
		int profileColorResId = mode.getIconColorInfo().getColor(nightMode);
		int colorNoAlpha = ContextCompat.getColor(app, profileColorResId);
		int removeIconColor = mode.isCustomProfile() ? R.color.color_osm_edit_delete : R.color.icon_color_default_light;

		holder.icon.setImageDrawable(uiUtilities.getIcon(iconRes, profileColorResId));
		holder.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_remove, removeIconColor));

		//set up cell color
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, colorNoAlpha, 0.3f);
		AndroidUtils.setBackground(holder.itemsContainer, drawable);
	}

	public ApplicationMode getItem(int position) {
		return applicationModes.get(position);
	}

	public interface ProfilesAdapterListener {

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragOrSwipeEnded(RecyclerView.ViewHolder holder);
	}

	class ProfileViewHolder extends RecyclerView.ViewHolder {

		TextView title;
		TextView description;
		ImageView icon;
		ImageView moveIcon;
		ImageButton actionIcon;
		View itemsContainer;

		public ProfileViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			actionIcon = itemView.findViewById(R.id.action_icon);
			icon = itemView.findViewById(R.id.icon);
			moveIcon = itemView.findViewById(R.id.move_icon);
			itemsContainer = itemView.findViewById(R.id.selectable_list_item);
		}
	}
}