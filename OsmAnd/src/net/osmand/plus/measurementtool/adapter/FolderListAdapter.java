package net.osmand.plus.measurementtool.adapter;

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
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FolderListAdapter extends RecyclerView.Adapter<FolderListAdapter.GroupsViewHolder> {

	List<String> items = new ArrayList<>();

	String selectedItemName;
	OsmandApplication app;
	boolean nightMode;
	FolderListAdapterListener listener;

	public FolderListAdapter(OsmandApplication app, boolean nightMode, String folderName) {
		this.app = app;
		this.nightMode = nightMode;
		selectedItemName = folderName;
		fillGroups();
	}

	private void fillGroups() {
		items.clear();
		items.addAll(getFolders());
	}

	private Collection<? extends String> getFolders() {
		List<File> dirs = new ArrayList<>();
		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		dirs.add(gpxDir);
		Algorithms.collectDirs(gpxDir, dirs);
		List<String> dirItems = new ArrayList<>();
		for (File dir : dirs) {
			dirItems.add(dir.getName());
		}
		return dirItems;
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
		return new FolderListAdapter.GroupsViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final GroupsViewHolder holder, int position) {

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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, holder.groupButton, nightMode, R.drawable.ripple_solid_light_6dp,
					R.drawable.ripple_solid_dark_6dp);
		}
	}

	@Override
	public int getItemCount() {
		return items == null ? 0 : items.size();
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
	}
}
