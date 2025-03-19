package net.osmand.plus.search;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.dialogs.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchWikiItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;

public class WikiItemViewHolder extends RecyclerView.ViewHolder {

	public final OsmandApplication app;
	public final UpdateLocationViewCache locationViewCache;

	public final TextView title;
	public final TextView description;
	public final TextView type;
	public final ImageView icon;
	public final ImageView image;

	public final boolean nightMode;

	public WikiItemViewHolder(@NonNull View view,
			@NonNull UpdateLocationViewCache locationViewCache, boolean nightMode) {
		super(view);

		this.app = AndroidUtils.getApp(view.getContext());
		this.locationViewCache = locationViewCache;
		this.nightMode = nightMode;

		title = view.findViewById(R.id.item_title);
		description = view.findViewById(R.id.item_description);
		type = view.findViewById(R.id.item_type);
		icon = view.findViewById(R.id.item_icon);
		image = view.findViewById(R.id.item_image);
	}

	public void bindItem(@NonNull QuickSearchWikiItem item, @Nullable PoiUIFilter poiUIFilter,
			boolean useMapCenter) {
		title.setText(item.getName());
		description.setText(item.getDescription());
		type.setText(item.getTypeName());

		Drawable drawable = item.getIcon();
		icon.setImageDrawable(drawable);

		boolean shouldLayoutWithImages = poiUIFilter != null && poiUIFilter.showLayoutWithImages();
		AndroidUiHelper.updateVisibility(image, shouldLayoutWithImages);
		if (shouldLayoutWithImages) {
			String wikiImageUrl = item.getImage();
			if (wikiImageUrl != null) {
				RequestCreator creator = Picasso.get().load(wikiImageUrl);
				creator.error(drawable);
				creator.into(image, new Callback() {
					@Override
					public void onSuccess() {
						PicassoUtils.getPicasso(app).setResultLoaded(wikiImageUrl, true);
					}

					@Override
					public void onError(Exception e) {
						PicassoUtils.getPicasso(app).setResultLoaded(wikiImageUrl, true);
					}
				});
			}
		}
		QuickSearchListAdapter.updateCompass(itemView, item, locationViewCache, useMapCenter);
	}
}
