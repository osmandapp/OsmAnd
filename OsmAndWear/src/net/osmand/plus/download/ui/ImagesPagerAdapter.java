package net.osmand.plus.download.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.util.List;

public class ImagesPagerAdapter extends PagerAdapter {

	private final OsmandApplication app;
	private final PicassoUtils picassoUtils;

	private final List<String> imageUrls;

	public ImagesPagerAdapter(@NonNull OsmandApplication app, List<String> imageUrls) {
		this.app = app;
		this.imageUrls = imageUrls;
		picassoUtils = PicassoUtils.getPicasso(app);
	}

	@Override
	public int getCount() {
		return imageUrls.size();
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		View view = createImageView(position);
		container.addView(view, 0);

		return view;
	}

	@Override
	public void destroyItem(ViewGroup collection, int position, @NonNull Object view) {
		collection.removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
		return view == object;
	}

	private View createImageView(int position) {
		ImageView imageView = new ImageView(app);
		imageView.setScaleType(ImageView.ScaleType.FIT_XY);

		String imageUrl = imageUrls.get(position);
		if (!Algorithms.isEmpty(imageUrl)) {
			Picasso.get().load(imageUrl).into(imageView, new Callback() {
				@Override
				public void onSuccess() {
					imageView.setVisibility(View.VISIBLE);
					picassoUtils.setResultLoaded(imageUrl, true);
				}

				@Override
				public void onError(Exception e) {
					imageView.setVisibility(View.INVISIBLE);
					picassoUtils.setResultLoaded(imageUrl, false);
				}
			});
		} else {
			imageView.setVisibility(View.INVISIBLE);
		}

		return imageView;
	}
}