package net.osmand.plus.measurementtool.adapter;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class FolderListAdapter extends RecyclerView.Adapter<FolderListAdapter.GroupsViewHolder> {

	private static final int VIEW_TYPE_FOOTER = 1;
	private static final int VIEW_TYPE_CELL = 0;
	private final List<String> items = new ArrayList<>();

	private String selectedItemName;
	private final OsmandApplication app;
	private final boolean nightMode;
	private FolderListAdapterListener listener;

	public FolderListAdapter(OsmandApplication app, boolean nightMode, String folderName) {
		this.app = app;
		this.nightMode = nightMode;
		selectedItemName = folderName;
	}

	public void setSelectedFolderName(String folderName) {
		this.selectedItemName = folderName;
	}

	public void setFolders(List<String> folders) {
		items.clear();
		items.addAll(folders);
	}

	@NonNull
	@Override
	public GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		int activeColorRes = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.point_editor_group_select_item,
				parent, false);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		params.width = app.getResources().getDimensionPixelSize(R.dimen.measurement_tool_folder_select_width);
		params.height = app.getResources().getDimensionPixelSize(R.dimen.measurement_tool_folder_select_height);
		TextView groupName = view.findViewById(R.id.groupName);
		groupName.setMaxLines(1);
		groupName.setEllipsize(TextUtils.TruncateAt.END);
		groupName.setTextColor(ContextCompat.getColor(app, activeColorRes));
		if (viewType != VIEW_TYPE_CELL) {
			groupName.setText(R.string.add_new_folder);
			int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
			Drawable iconAdd = app.getUIUtilities().getIcon(R.drawable.ic_action_add, activeColorResId);
			ImageView groupIcon = view.findViewById(R.id.groupIcon);
			groupIcon.setImageDrawable(iconAdd);
			GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app,
					R.drawable.bg_select_group_button_outline);
			if (rectContourDrawable != null) {
				int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.stroked_buttons_and_links_outline_dark
						: R.color.stroked_buttons_and_links_outline_light);
				rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 1), strokeColor);
				((ImageView) view.findViewById(R.id.outlineRect)).setImageDrawable(rectContourDrawable);
			}
			((TextView) view.findViewById(R.id.groupName)).setTextColor(app.getResources().getColor(activeColorResId));
		}
		return new FolderListAdapter.GroupsViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final GroupsViewHolder holder, int position) {
		if (position == items.size()) {
			holder.groupButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onAddNewItemSelected();
					}
				}
			});
		} else {
			holder.groupButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int previousSelectedPosition = getItemPosition(selectedItemName);
					selectedItemName = items.get(holder.getAdapterPosition());
					notifyItemChanged(holder.getAdapterPosition());
					notifyItemChanged(previousSelectedPosition);
					if (listener != null) {
						listener.onItemSelected(selectedItemName);
					}
				}
			});
			final String group = Algorithms.capitalizeFirstLetter(items.get(position));
			holder.groupName.setText(group);
			int activeColorRes = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
			int strokeColor;
			int strokeWidth;
			if (selectedItemName != null && selectedItemName.equals(items.get(position))) {
				strokeColor = activeColorRes;
				strokeWidth = 2;
			} else {
				strokeColor = nightMode ? R.color.stroked_buttons_and_links_outline_dark
						: R.color.stroked_buttons_and_links_outline_light;
				strokeWidth = 1;
			}
			GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app,
					R.drawable.bg_select_group_button_outline);
			if (rectContourDrawable != null) {
				rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, strokeWidth), ContextCompat.getColor(app, strokeColor));
				holder.groupButton.setImageDrawable(rectContourDrawable);
			}
			int iconID;
			iconID = R.drawable.ic_action_folder;
			holder.groupIcon.setImageDrawable(app.getUIUtilities().getIcon(iconID, activeColorRes));
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, holder.groupButton, nightMode, R.drawable.ripple_solid_light_6dp,
					R.drawable.ripple_solid_dark_6dp);
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

	int getItemPosition(String name) {
		return items.indexOf(name);
	}

	public void setListener(FolderListAdapterListener listener) {
		this.listener = listener;
	}

	static class GroupsViewHolder extends RecyclerView.ViewHolder {

		final TextView groupName;
		final ImageView groupIcon;
		final ImageView groupButton;

		GroupsViewHolder(View itemView) {
			super(itemView);
			groupName = itemView.findViewById(R.id.groupName);
			groupIcon = itemView.findViewById(R.id.groupIcon);
			groupButton = itemView.findViewById(R.id.outlineRect);
		}
	}

	public interface FolderListAdapterListener {

		void onItemSelected(String item);

		void onAddNewItemSelected();
	}
}
