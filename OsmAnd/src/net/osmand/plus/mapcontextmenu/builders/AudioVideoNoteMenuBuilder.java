package net.osmand.plus.mapcontextmenu.builders;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

public class AudioVideoNoteMenuBuilder extends MenuBuilder {

	private final Recording recording;

	public AudioVideoNoteMenuBuilder(OsmandApplication app, final Recording recording) {
		super(app);
		this.recording = recording;
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void build(View view) {
		super.build(view);

		File file = recording.getFile();
		if (file != null) {

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
						vint.setDataAndType(Uri.fromFile(recording.getFile()), "image/*");
						vint.setFlags(0x10000000);
						v.getContext().startActivity(vint);
					}
				});
			}

			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(view.getContext());
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(view.getContext());
			Date date = new Date(recording.getFile().lastModified());
			buildRow(view, R.drawable.ic_action_data, dateFormat.format(date) + " — " + timeFormat.format(date), 0);
		}

		buildPlainMenuItems(view);

		buildButtonRow(view, null, view.getResources().getString(R.string.recording_context_menu_delete), new OnClickListener() {
			@Override
			public void onClick(View v) {
				AccessibleAlertBuilder bld = new AccessibleAlertBuilder(v.getContext());
				bld.setMessage(R.string.recording_delete_confirm);
				final View fView = v;
				bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
						if (plugin != null) {
							plugin.deleteRecording(recording);
							if (fView.getContext() instanceof MapActivity) {
								((MapActivity)fView.getContext()).getContextMenu().close();
							}
						}
					}
				});
				bld.setNegativeButton(R.string.shared_string_no, null);
				bld.show();
			}
		});
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
