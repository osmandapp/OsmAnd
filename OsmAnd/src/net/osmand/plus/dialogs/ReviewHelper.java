package net.osmand.plus.dialogs;

import android.os.Debug;
import android.util.Log;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;

import net.osmand.plus.activities.MapActivity;

import androidx.annotation.NonNull;

public class ReviewHelper {

	public static void review(final MapActivity mapActivity) {
		final ReviewManager manager = ReviewManagerFactory.create(mapActivity);
		Task<ReviewInfo> request = manager.requestReviewFlow();
		request.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
			@Override
			public void onComplete(@NonNull Task<ReviewInfo> task) {
				if (task.isSuccessful()) {
					showInAppReview(manager, mapActivity, task.getResult());
				}
			}
		});
	}

	private static void showInAppReview(ReviewManager manager, MapActivity mapActivity, ReviewInfo task) {
		Task<Void> flow = manager.launchReviewFlow(mapActivity, task);
		flow.addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				if (task.isSuccessful()) {
					// TODO: Update
					Log.v("M_ReviewHelper", "Shown");
				}
			}
		});
	}

}
