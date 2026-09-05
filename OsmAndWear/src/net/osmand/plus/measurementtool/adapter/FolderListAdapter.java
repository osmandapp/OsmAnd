package net.osmand.plus.measurementtool.adapter;

import static net.osmand.plus.utils.ColorUtilities.getStrokedButtonsOutlineColor;
import static net.osmand.plus.utils.ColorUtilities.getStrokedButtonsOutlineColorId;

import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.measurementtool.adapter.FolderListAdapter.FolderViewHolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderListAdapter extends RecyclerView.Adapter<FolderViewHolder> {

	public static final int VIEW_TYPE_ADD = 0;
	protected static final int VIEW_TYPE_FOLDER = 1;

	protected final OsmandApplication app;
	protected final UiUtilities uiUtilities;

	protected List<Object> items = new ArrayList<>();
	protected String selectedItem;
	protected FolderListAdapterListener listener;
	protected final boolean nightMode;

	public FolderListAdapter(@NonNull OsmandApplication app, @Nullable String selectedItem, boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
		this.selectedItem = selectedItem;
		this.uiUtilities = app.getUIUtilities();
	}

	@NonNull
	public List<Object> getItems() {
		return items;
	}

	public void setItems(@NonNull List<Object> items) {
		this.items = items;
	}

	public void setSelectedItem(@Nullable String folderName) {
		this.selectedItem = folderName;
	}

	public void setListener(@Nullable FolderListAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		View view = inflater.inflate(R.layout.point_editor_group_select_item, parent, false);

		FolderViewHolder holder;
		if (VIEW_TYPE_FOLDER == viewType) {
			holder = new FolderViewHolder(view);
		} else if (VIEW_TYPE_ADD == viewType) {
			holder = new AddFolderViewHolder(view);
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
		Resources resources = app.getResources();
		ViewGroup.LayoutParams params = view.getLayoutParams();
		params.width = resources.getDimensionPixelSize(R.dimen.measurement_tool_folder_select_width);
		params.height = resources.getDimensionPixelSize(R.dimen.measurement_tool_folder_select_height);

		holder.groupName.setMaxLines(1);
		holder.groupName.setEllipsize(TruncateAt.END);
		holder.groupName.setTextColor(ColorUtilities.getActiveColor(app, nightMode));

		return holder;
	}

	@Override
	public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
		if (holder instanceof AddFolderViewHolder) {
			bindAddItem(holder);
		} else {
			String item = (String) getItem(position);
			boolean selected = Algorithms.objectEquals(selectedItem, item);
			bindFolderItem(holder, item, selected);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, holder.groupButton, nightMode,
					R.drawable.ripple_solid_light_6dp, R.drawable.ripple_solid_dark_6dp);
		}
	}

	protected void bindFolderItem(@NonNull FolderViewHolder holder, @NonNull String item, boolean selected) {
		holder.groupButton.setOnClickListener(view -> {
			int previousSelectedPosition = getItemPosition(selectedItem);
			selectedItem = (String) getItem(holder.getAdapterPosition());
			notifyItemChanged(holder.getAdapterPosition());
			notifyItemChanged(previousSelectedPosition);
			if (listener != null) {
				listener.onItemSelected(selectedItem);
			}
		});
		String name = Algorithms.getFileWithoutDirs(item);
		holder.groupName.setText(Algorithms.capitalizeFirstLetter(name));

		int activeColorId = ColorUtilities.getActiveColorId(nightMode);
		GradientDrawable drawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_group_button_outline);
		if (drawable != null) {
			int strokeWidth = selected ? 2 : 1;
			int strokeColor = selected ? activeColorId : getStrokedButtonsOutlineColorId(nightMode);
			drawable.setStroke(AndroidUtils.dpToPx(app, strokeWidth), ContextCompat.getColor(app, strokeColor));
			holder.groupButton.setImageDrawable(drawable);
		}
		holder.groupIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_folder, activeColorId));
	}

	protected void bindAddItem(@NonNull FolderViewHolder holder) {
		holder.groupButton.setOnClickListener(view -> {
			if (listener != null) {
				listener.onAddNewItemSelected();
			}
		});
		GradientDrawable drawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_group_button_outline);
		if (drawable != null) {
			drawable.setStroke(AndroidUtils.dpToPx(app, 1), getStrokedButtonsOutlineColor(app, nightMode));
		}
		holder.groupButton.setImageDrawable(drawable);
		holder.groupName.setText(R.string.add_new_folder);
		holder.groupName.setTextColor(ColorUtilities.getActiveColor(app, nightMode));
		holder.groupIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_add, ColorUtilities.getActiveColorId(nightMode)));
	}

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);
		if (item instanceof String) {
			return VIEW_TYPE_FOLDER;
		} else if (item instanceof Integer) {
			return VIEW_TYPE_ADD;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	protected int getItemPosition(Object name) {
		return items.indexOf(name);
	}

	protected Object getItem(int position) {
		return items.get(position);
	}

	@NonNull
	public static List<String> getFolders(@NonNull File parentDir) {
		List<File> folders = new ArrayList<>();
		folders.add(parentDir);
		Algorithms.collectDirs(parentDir, folders);

		List<String> folderPaths = new ArrayList<>();
		for (File dir : folders) {
			folderPaths.add(dir.getAbsolutePath());
		}
		return folderPaths;
	}

	public static class FolderViewHolder extends RecyclerView.ViewHolder {

		public final TextView groupName;
		public final ImageView groupIcon;
		public final ImageView groupButton;

		FolderViewHolder(@NonNull View itemView) {
			super(itemView);
			groupName = itemView.findViewById(R.id.groupName);
			groupIcon = itemView.findViewById(R.id.groupIcon);
			groupButton = itemView.findViewById(R.id.outlineRect);
		}
	}

	public static class AddFolderViewHolder extends FolderViewHolder {

		AddFolderViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	public interface FolderListAdapterListener {

		void onItemSelected(String item);

		void onAddNewItemSelected();
	}
}
