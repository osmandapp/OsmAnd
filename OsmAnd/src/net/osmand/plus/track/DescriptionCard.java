package net.osmand.plus.track;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.PicassoUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.util.Algorithms;

import static net.osmand.plus.myplaces.TrackActivityFragmentAdapter.getMetadataImageLink;

public class DescriptionCard extends BaseCard {

	private final GPXFile gpxFile;

	public DescriptionCard(@NonNull MapActivity mapActivity, @NonNull GPXFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_description_preview_card;
	}

	@Override
	protected void updateContent() {
		if (gpxFile.metadata == null || gpxFile.metadata.getDescription() == null) {
			AndroidUiHelper.updateVisibility(view, false);
			return;
		} else {
			AndroidUiHelper.updateVisibility(view, true);
		}

		final String title = gpxFile.metadata.getArticleTitle();
		final String imageUrl = getMetadataImageLink(gpxFile.metadata);
		final String descriptionHtml = gpxFile.metadata.getDescription();

		setupImage(imageUrl);

		TextViewEx tvDescription = view.findViewById(R.id.description);
		tvDescription.setText(getFirstParagraph(descriptionHtml));

		TextViewEx readBtn = view.findViewById(R.id.btn_read_full);
		readBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				GpxReadDescriptionDialogFragment.showInstance(mapActivity, title, imageUrl, descriptionHtml);
			}
		});
		TextViewEx editBtn = view.findViewById(R.id.btn_edit);
		editBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				GpxEditDescriptionDialogFragment.showInstance(mapActivity, descriptionHtml, null);
			}
		});
	}

	private String getFirstParagraph(String descriptionHtml) {
		if (descriptionHtml != null) {
			String firstParagraph = WikiArticleHelper.getPartialContent(descriptionHtml);
			if (!Algorithms.isEmpty(firstParagraph)) {
				return firstParagraph;
			}
		}
		return descriptionHtml;
	}

	private void setupImage(final String imageUrl) {
		if (imageUrl == null) {
			return;
		}
		final PicassoUtils picasso = PicassoUtils.getPicasso(app);
		RequestCreator rc = Picasso.get().load(imageUrl);
		final AppCompatImageView image = view.findViewById(R.id.main_image);
		rc.into(image, new Callback() {
			@Override
			public void onSuccess() {
				picasso.setResultLoaded(imageUrl, true);
				AndroidUiHelper.updateVisibility(image, true);
			}

			@Override
			public void onError(Exception e) {
				picasso.setResultLoaded(imageUrl, false);
			}
		});
	}
}