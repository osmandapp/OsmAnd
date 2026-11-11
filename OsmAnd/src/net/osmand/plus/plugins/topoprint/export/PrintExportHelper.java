package net.osmand.plus.plugins.topoprint.export;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.AndroidUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Hands the rendered map to the Android Printing Framework (as a PDF page
 * sized to the selected paper) and provides FileProvider-based sharing of the
 * SVG/PNG artifacts.
 */
public class PrintExportHelper {

	private PrintExportHelper() {
	}

	/**
	 * Opens the system print dialog for the map PNG. The image is scaled to
	 * fill the paper exactly — the map geometry already includes the margins.
	 *
	 * @param paperWidthMils  paper width in 1/1000 inch
	 * @param paperHeightMils paper height in 1/1000 inch
	 */
	public static void printPng(@NonNull Activity activity, @NonNull File pngFile,
	                            @NonNull String jobName, int paperWidthMils, int paperHeightMils) {
		PrintAttributes attributes = new PrintAttributes.Builder()
				.setMediaSize(new PrintAttributes.MediaSize("map", "map", paperWidthMils, paperHeightMils))
				.setColorMode(PrintAttributes.COLOR_MODE_COLOR)
				.setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0))
				.build();
		PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
		if (printManager != null) {
			printManager.print(jobName, new MapPrintAdapter(activity, pngFile, paperWidthMils, paperHeightMils), attributes);
		}
	}

	/** Opens the artifact with a viewer app (e.g. the gallery for the PNG). */
	public static void openFile(@NonNull Activity activity, @NonNull File file, @NonNull String mimeType) {
		Uri uri = AndroidUtils.getUriForFile(activity, file);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(uri, mimeType);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		activity.startActivity(intent);
	}

	/** Standard share sheet for the generated artifact. */
	public static void shareFile(@NonNull Activity activity, @NonNull File file, @NonNull String mimeType) {
		Uri uri = AndroidUtils.getUriForFile(activity, file);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType(mimeType);
		intent.putExtra(Intent.EXTRA_STREAM, uri);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		activity.startActivity(Intent.createChooser(intent, null));
	}

	private static class MapPrintAdapter extends PrintDocumentAdapter {

		private final Context context;
		private final File file;
		private final int widthMils;
		private final int heightMils;

		MapPrintAdapter(@NonNull Context context, @NonNull File file, int widthMils, int heightMils) {
			this.context = context;
			this.file = file;
			this.widthMils = widthMils;
			this.heightMils = heightMils;
		}

		@Override
		public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
		                     CancellationSignal cancellationSignal, LayoutResultCallback callback,
		                     Bundle extras) {
			if (cancellationSignal.isCanceled()) {
				callback.onLayoutCancelled();
				return;
			}
			PrintDocumentInfo info = new PrintDocumentInfo.Builder(file.getName())
					.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
					.setPageCount(1)
					.build();
			callback.onLayoutFinished(info, true);
		}

		@Override
		public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
		                    CancellationSignal cancellationSignal, WriteResultCallback callback) {
			PdfDocument document = new PdfDocument();
			// PDF points = 1/72 inch.
			int pageWidth = widthMils * 72 / 1000;
			int pageHeight = heightMils * 72 / 1000;
			PdfDocument.Page page = document.startPage(
					new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create());

			Bitmap bitmap = decodeBitmap(file);
			if (bitmap == null) {
				// Release the started page and the native document before
				// bailing out — the finally block below only covers writeTo.
				document.finishPage(page);
				document.close();
				callback.onWriteFailed("Cannot decode " + file.getName());
				return;
			}
			Canvas canvas = page.getCanvas();
			canvas.drawBitmap(bitmap, null, new RectF(0, 0, pageWidth, pageHeight), null);
			bitmap.recycle();
			document.finishPage(page);

			try {
				document.writeTo(new FileOutputStream(destination.getFileDescriptor()));
				callback.onWriteFinished(new PageRange[] {PageRange.ALL_PAGES});
			} catch (IOException e) {
				callback.onWriteFailed(e.getMessage());
			} finally {
				document.close();
			}
		}

		@Nullable
		private static Bitmap decodeBitmap(@NonNull File file) {
			BitmapFactory.Options bounds = new BitmapFactory.Options();
			bounds.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
			if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
				return null;
			}
			// Downsample only when the full-size decode would exceed ~350 MB.
			int sampleSize = 1;
			long bytes = (long) bounds.outWidth * bounds.outHeight * 4;
			while (bytes / ((long) sampleSize * sampleSize) > 350L * 1024 * 1024 && sampleSize < 32) {
				sampleSize *= 2;
			}
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = sampleSize;
			return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
		}
	}
}
