package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder.*;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder;
import net.osmand.plus.mapcontextmenu.gallery.holders.MapillaryContributeHolder;
import net.osmand.plus.utils.AndroidUtils;

public class GalleryGridItemDecorator extends RecyclerView.ItemDecoration {
	public static final int GRID_SCREEN_ITEM_SPACE_DP = 3;

	private final OsmandApplication app;
	private final int standardItemOffsetInPx;

	public GalleryGridItemDecorator(@NonNull OsmandApplication app) {
		this.app = app;
		standardItemOffsetInPx = AndroidUtils.dpToPx(app, 6);
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);
		int position = parent.getChildAdapterPosition(view);
		RecyclerView.ViewHolder viewHolder = parent.getChildViewHolder(view);
		if (viewHolder instanceof GalleryImageHolder holder) {
			ImageHolderType type = holder.getHolderType();
			switch (type) {
				case SPAN_RESIZABLE -> {
					int gridSpace = AndroidUtils.dpToPx(app, GRID_SCREEN_ITEM_SPACE_DP);
					outRect.right = gridSpace;
					outRect.bottom = gridSpace;
					outRect.left = gridSpace;
					outRect.top = gridSpace;
				}
				case MAIN -> {
					outRect.right = standardItemOffsetInPx;
					outRect.left = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
				}
				case STANDARD -> {
					outRect.right = standardItemOffsetInPx;
					outRect.left = standardItemOffsetInPx;
					if (position % 2 == 0) {
						outRect.top = standardItemOffsetInPx;
						outRect.bottom = 0;
					} else {
						outRect.bottom = standardItemOffsetInPx;
						outRect.top = 0;
					}
				}
			}
		} else if (viewHolder instanceof MapillaryContributeHolder) {
			outRect.right = AndroidUtils.dpToPx(app, 16);
		}
	}
}
