package net.osmand.plus.audionotes;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.mapcontextmenu.MenuBuilder;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

public class AudioVideoNoteMenuBuilder extends MenuBuilder {

	private final Recording recording;

	public AudioVideoNoteMenuBuilder(@NonNull MapActivity mapActivity, final @NonNull Recording recording) {
		super(mapActivity);
		this.recording = recording;
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void buildInternal(View view) {
		File file = recording.getFile();
		if (file != null) {

			buildDateRow(view, recording.getFile().lastModified());
			buildPlainMenuItems(view);

			if (recording.isPhoto()) {
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inSampleSize = 4;
				int rot = recording.getBitmapRotation();
				Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
				if (rot != 0) {
					Matrix matrix = new Matrix();
					matrix.postRotate(rot);
					Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
					bmp.recycle();
					bmp = resizedBitmap;
				}

				buildImageRow(view, bmp, new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent vint = new Intent(Intent.ACTION_VIEW);
						vint.setDataAndType(AndroidUtils.getUriForFile(getApplication(),recording.getFile()) , "image/*");
						vint.setFlags(0x10000000);
						vint.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						v.getContext().startActivity(vint);
					}
				});
			}
		} else {
			buildPlainMenuItems(view);
		}
	}

	protected void buildImageRow(final View view, Bitmap bitmap, OnClickListener onClickListener) {
		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);

		// Image
		LinearLayout llImage = new LinearLayout(view.getContext());
		LinearLayout.LayoutParams llILParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llImage.setLayoutParams(llILParams);
		llImage.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llImage);

		ImageView imageView = new ImageView(view.getContext());
		LinearLayout.LayoutParams llImgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(208f));
		imageView.setLayoutParams(llImgParams);
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		imageView.setImageBitmap(bitmap);

		imageView.setOnClickListener(onClickListener);
		llImage.addView(imageView);

		((LinearLayout) view).addView(ll);

		rowBuilt();
	}
}
