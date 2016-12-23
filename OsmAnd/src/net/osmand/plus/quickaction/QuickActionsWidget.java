package net.osmand.plus.quickaction;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import net.osmand.plus.R;

import java.util.List;


public class QuickActionsWidget extends LinearLayout {

    private List<QuickAction> actions;

    private ImageButton next;
    private ImageButton prev;

    private ViewPager viewPager;

    public QuickActionsWidget(Context context) {
        super(context);
        setup();
    }

    public QuickActionsWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public QuickActionsWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public void setActions(List<QuickAction> actions){
        this.actions = actions;
    }

    private void setup(){
        inflate(getContext(), R.layout.quick_action_widget, this);

//        viewPager = (ViewPager) findViewById(R.id.viewPager);
//        viewPager.setAdapter(new ViewsPagerAdapter());
//
//        next = (ImageButton) findViewById(R.id.btnNext);
//        prev = (ImageButton) findViewById(R.id.btnPrev);
//
//        next.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                if (viewPager.getAdapter().getCount() > viewPager.getCurrentItem() + 1) {
//                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
//                }
//            }
//        });
//
//        prev.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                if (viewPager.getCurrentItem() - 1 >= 0) {
//                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
//                }
//            }
//        });
    }

    private View createPageView(ViewGroup container){
        //TODO setup it
        return getLayoutInflater().inflate(R.layout.quick_action_widget_item, container, false);
    }

    private class ViewsPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return (int) Math.ceil(actions.size() / 6);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            View view = createPageView(container);
            container.addView(view, 0);

            return view;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    private LayoutInflater getLayoutInflater(){
        return (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
}
