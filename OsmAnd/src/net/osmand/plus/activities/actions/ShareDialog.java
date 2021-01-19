package net.osmand.plus.activities.actions;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;

import java.util.ArrayList;
import java.util.List;

public class ShareDialog {

	private Activity a;
	private String title;
	private List<ShareType> share = new ArrayList<ShareType>();
	private static final String ZXING_BARCODE_SCANNER_COMPONENT = "com.google.zxing.client.android"; //$NON-NLS-1$
	private static final String ZXING_BARCODE_SCANNER_ACTIVITY = "com.google.zxing.client.android.ENCODE"; //$NON-NLS-1$
	static final int ACTION = -1;
	static final int VIEW = 0;
	static final int EMAIL = 1;
	static final int SMS = 2;
	static final int CLIPBOARD = 3;
	static final int QR = 4;


	public ShareDialog(Activity a) {
		this.a = a;
	}
	
	public ShareDialog setTitle(String title) {
		this.title = title;
		return this;
	}
	
	
	public ShareDialog viewContent(String content){
		share.add(new ShareType(content, VIEW));
		return this;
	}
	
	public ShareDialog setAction(String content, Runnable r){
		share.add(new ShareType(content, ACTION, r));
		return this;
	}
	
	public ShareDialog shareURLOrText(String url, String shortExplanation, String longExplanation) {
		if (shortExplanation == null) {
			shortExplanation = url;
		}
		if (longExplanation == null) {
			longExplanation = shortExplanation;
		}
		share.add(new ShareType(longExplanation, EMAIL));
		share.add(new ShareType(shortExplanation, SMS));
		if (url != null) {
			share.add(new ShareType(url, CLIPBOARD));
			share.add(new ShareType(url, QR));
		} else {
			share.add(new ShareType(shortExplanation, CLIPBOARD));
		}
		return this;
	}
	
	
	private static class ShareType {
		public String content;
		public int type;
		private Runnable runnable;
		
		public ShareType(String content, int type) {
			this.content = content;
			this.type = type;
		}

		public ShareType(String content, int action, Runnable r) {
			this.content = content;
			this.type = action;
			this.runnable = r;
		}

		public String getShareName(Context ctx) {
			if(type == ACTION) {
				return content;
			} else if(type == VIEW) {
				return ctx.getString(R.string.shared_string_show_details);
			} else if(type == EMAIL) {
				return "Email";
			} else if(type == SMS) {
				return "SMS";
			} else if(type == CLIPBOARD) {
				return "Clipboard";
			} else if(type == QR) {
				return "QR-code";
			}
			return "";
		}
		
		public void execute(Activity a, String title) {
			if(type == ACTION) {
				runnable.run();
			} else if(type == VIEW) {
				AlertDialog.Builder bld = new AlertDialog.Builder(a);
				bld.setTitle(title);
				bld.setMessage(content);
				bld.show();
			} else if(type == EMAIL) {
				sendEmail(a, content, title);
			} else if(type == SMS) {
				sendSms(a, content);
			} else if(type == CLIPBOARD) {
				sendToClipboard(a, content);
			} else if(type == QR) {
				sendQRCode(a, "TEXT_TYPE", null, content);
			}
			
		}
	}
	
	public void showDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setTitle(title);
		String[] shareStrings = new String[share.size()];
		for(int i = 0; i < shareStrings.length; i++) {
			shareStrings[i] = share.get(i).getShareName(a);
		}
		builder.setItems(shareStrings, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ShareType type = share.get(which);
				try {
					type.execute(a, title);
				} catch (RuntimeException e) {
					Toast.makeText(a, R.string.shared_string_io_error, Toast.LENGTH_SHORT).show();
				}				
			}
		});
    	builder.show();

		
	}
	
	public static void sendSms(Activity a, String sms) {
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		sendIntent.putExtra("sms_body", sms); 
		sendIntent.setType("vnd.android-dir/mms-sms");
		a.startActivity(sendIntent);
	}
	
	public static void sendEmail(Activity a, String email, String title) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("vnd.android.cursor.dir/email"); //$NON-NLS-1$
		intent.putExtra(Intent.EXTRA_SUBJECT, "Location"); //$NON-NLS-1$
		intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(email));
		intent.setType("text/html");
		a.startActivity(Intent.createChooser(intent, a.getString(R.string.send_location)));
	}
	
	public static void sendMessage(Activity a, String msg) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, msg);
		intent.setType("text/plain");
		a.startActivity(Intent.createChooser(intent, a.getString(R.string.send_location)));
	}
	
	public static void sendQRCode(final Activity activity, String encodeType, Bundle encodeData, String strEncodeData) {
		Intent intent = new Intent();
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setAction(ZXING_BARCODE_SCANNER_ACTIVITY);
		ResolveInfo resolved = activity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (resolved != null) {
			intent.putExtra("ENCODE_TYPE", encodeType);
			if(strEncodeData != null ) {
				intent.putExtra("ENCODE_DATA", strEncodeData);
			} else {
				intent.putExtra("ENCODE_DATA", encodeData);
			}
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			activity.startActivity(intent);
		} else {
			if (Version.isMarketEnabled()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setMessage(activity.getString(R.string.zxing_barcode_scanner_not_found));
				builder.setPositiveButton(activity.getString(R.string.shared_string_yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.getUrlWithUtmRef((OsmandApplication) activity.getApplication(), ZXING_BARCODE_SCANNER_COMPONENT)));
						try {
							activity.startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
				builder.setNegativeButton(activity.getString(R.string.shared_string_no), null);
				builder.show();
			} else {
				Toast.makeText(activity, R.string.zxing_barcode_scanner_not_found, Toast.LENGTH_LONG).show();
			}
		}
	}

	public static void sendToClipboard(Activity activity, String text) {
		ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
		clipboard.setText(text);
		Toast.makeText(activity, activity.getString(R.string.copied_to_clipboard) + "\n" + text, Toast.LENGTH_LONG)
				.show();
	}
}
