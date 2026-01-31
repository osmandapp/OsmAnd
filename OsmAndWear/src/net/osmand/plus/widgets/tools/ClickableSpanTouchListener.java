package net.osmand.plus.widgets.tools;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class ClickableSpanTouchListener implements View.OnTouchListener {

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();

		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
			TextView widget = (TextView) v;

			int x = (int) event.getX();
			int y = (int) event.getY();

			x -= widget.getTotalPaddingLeft();
			y -= widget.getTotalPaddingTop();

			x += widget.getScrollX();
			y += widget.getScrollY();

			Layout layout = widget.getLayout();
			int line = layout.getLineForVertical(y);
			int off = layout.getOffsetForHorizontal(line, x);

			Spannable spannable = new SpannableString(widget.getText());
			ClickableSpan[] links = spannable.getSpans(off, off, ClickableSpan.class);

			if (links.length != 0) {
				if (action == MotionEvent.ACTION_UP) {
					links[0].onClick(widget);
				} else {
					Selection.setSelection(spannable, spannable.getSpanStart(links[0]), spannable.getSpanEnd(links[0]));
				}
				return true;
			} else {
				Selection.removeSelection(spannable);
			}
		}

		return false;
	}

}