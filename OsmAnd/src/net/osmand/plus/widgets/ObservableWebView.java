package net.osmand.plus.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class ObservableWebView extends WebView {
	private OnScrollChangeListener onScrollChangeListener;

	public ObservableWebView(Context context) {
		super(context);
	}

	public ObservableWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ObservableWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		if (onScrollChangeListener != null) {
			onScrollChangeListener.onScrollChange(this, l, t, oldl, oldt);
		}
	}

	public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
		this.onScrollChangeListener = onScrollChangeListener;
	}

	public OnScrollChangeListener getOnScrollChangeListener() {
		return onScrollChangeListener;
	}

	public interface OnScrollChangeListener {
		/**
		 * Called when the scroll position of a view changes.
		 *
		 * @param v          The view whose scroll position has changed.
		 * @param scrollX    Current horizontal scroll origin.
		 * @param scrollY    Current vertical scroll origin.
		 * @param oldScrollX Previous horizontal scroll origin.
		 * @param oldScrollY Previous vertical scroll origin.
		 */
		void onScrollChange(WebView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY);
	}
}