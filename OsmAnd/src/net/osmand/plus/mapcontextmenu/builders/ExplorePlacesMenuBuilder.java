package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.PlatformUtil;
import net.osmand.data.ExploreTopPlacePoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class ExplorePlacesMenuBuilder extends MenuBuilder {

	private static final Log LOG = PlatformUtil.getLog(ExplorePlacesMenuBuilder.class);

	private final ExploreTopPlacePoint point;

	public ExplorePlacesMenuBuilder(@NonNull MapActivity mapActivity, @NonNull ExploreTopPlacePoint point) {
		super(mapActivity);
		this.point = point;
		setShowNearestWiki(true);
	}

	protected void buildMainImage(View view) {
		if (point.getImageStubUrl() != null) {
			AppCompatImageView imageView = inflateAndGetMainImageView(view);
			PicassoUtils.setupImageViewByUrl(app, imageView, point.getImageStubUrl(), true);
		}
	}

	@Override
	protected void buildDescription(View view) {
		String desc = point.getDescription();
		if (!Algorithms.isEmpty(desc)) {
			buildDescriptionRow(view, desc);
		}
	}
}