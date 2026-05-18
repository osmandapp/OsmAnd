package net.osmand.plus.track.cards;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.Metadata;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.fragments.EditDescriptionFragment;
import net.osmand.plus.track.fragments.ReadGpxDescriptionFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.util.Algorithms;

import java.util.Map;

import static net.osmand.plus.wikivoyage.WikivoyageUtils.ARTICLE_LANG;
import static net.osmand.plus.wikivoyage.WikivoyageUtils.ARTICLE_TITLE;

public class DescriptionCard extends MapBaseCard {

	private final Fragment targetFragment;
	private final GpxFile gpxFile;

	public DescriptionCard(@NonNull MapActivity mapActivity,
	                       @NonNull Fragment targetFragment,
	                       @NonNull GpxFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
		this.targetFragment = targetFragment;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_description_preview_card;
	}

	@Override
	public void updateContent() {
		String title = gpxFile.getMetadata().getArticleTitle();
		String imageUrl = getMetadataImageLink(gpxFile.getMetadata());
		String descriptionHtml = gpxFile.getMetadata().getDescription();

		setupImage(imageUrl);

		if (Algorithms.isBlank(descriptionHtml)) {
			showAddBtn();
		} else {
			showDescription(title, imageUrl, descriptionHtml);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow), gpxFile.isShowCurrentTrack());
	}

	private void showAddBtn() {
		LinearLayout descriptionContainer = view.findViewById(R.id.description_container);
		View addBtn = view.findViewById(R.id.btn_add);

		setupButton(addBtn);
		addBtn.setOnClickListener(v -> {
			EditDescriptionFragment.showInstance(getMapActivity(), "", targetFragment);
		});
		AndroidUiHelper.updateVisibility(descriptionContainer, false);
		AndroidUiHelper.updateVisibility(addBtn, true);
	}

	private void showDescription(String title, String imageUrl, String descriptionHtml) {
		LinearLayout descriptionContainer = view.findViewById(R.id.description_container);
		FrameLayout addBtn = view.findViewById(R.id.btn_add);

		AndroidUiHelper.updateVisibility(descriptionContainer, true);
		AndroidUiHelper.updateVisibility(addBtn, false);

		TextViewEx tvDescription = view.findViewById(R.id.description);
		tvDescription.setText(getFirstParagraph(descriptionHtml));

		View readBtn = view.findViewById(R.id.btn_read_full);
		setupButton(readBtn);
		readBtn.setOnClickListener(v -> {
			Map<String, String> extensions = gpxFile.getMetadata().getExtensionsToRead();
			if (!Algorithms.isEmpty(extensions)) {
				String articleTitle = extensions.get(ARTICLE_TITLE);
				String lang = extensions.get(ARTICLE_LANG);
				if (articleTitle != null && lang != null) {
					TravelArticleIdentifier articleId = app.getTravelHelper().getArticleId(articleTitle, lang);
					if (articleId != null) {
						WikivoyageUtils.openWikivoyageArticle(activity, articleId, lang);
						return;
					}
				}
			}
			ReadGpxDescriptionFragment.showInstance(mapActivity, title, imageUrl, descriptionHtml, targetFragment);
		});

		View editBtn = view.findViewById(R.id.btn_edit);
		setupButton(editBtn);
		editBtn.setOnClickListener(v -> {
			EditDescriptionFragment.showInstance(mapActivity, descriptionHtml, targetFragment);
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

	private void setupButton(View button) {
		Context ctx = button.getContext();
		AndroidUtils.setBackground(ctx, button, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
	}

	private void setupImage(String imageUrl) {
		if (imageUrl == null) {
			return;
		}
		PicassoUtils picasso = PicassoUtils.getPicasso(app);
		RequestCreator rc = Picasso.get().load(imageUrl);
		AppCompatImageView image = view.findViewById(R.id.main_image);
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

	@Nullable
	public static String getMetadataImageLink(@NonNull Metadata metadata) {
		String link = metadata.getLink();
		if (!TextUtils.isEmpty(link)) {
			String lowerCaseLink = link.toLowerCase();
			if (lowerCaseLink.contains(".jpg")
					|| lowerCaseLink.contains(".jpeg")
					|| lowerCaseLink.contains(".png")
					|| lowerCaseLink.contains(".bmp")
					|| lowerCaseLink.contains(".webp")) {
				return link;
			}
		}
		return null;
	}
}