package net.osmand.plus.mapcontextmenu.editors;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public class PointIconScreenAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	public static final int CATEGORY_SELECTOR = 1;
	public static final int CATEGORY_ICONS = 2;
	public static final int SEARCH_PROGRESS = 3;
	public static final int ICON_SEARCH_RESULT = 4;
	public static final int NO_ICONS_FOUND = 5;

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return null;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

	}

	@Override
	public int getItemCount() {
		return 0;
	}
}
