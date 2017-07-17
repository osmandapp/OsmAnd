package net.osmand.plus.quickaction;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.actions.NewAction;

import java.util.List;

import static net.osmand.plus.R.id.imageView;


public class QuickActionsWidget extends LinearLayout {

    private static final int ELEMENT_PER_PAGE = 6;

    private QuickAction.QuickActionSelectionListener selectionListener;

    private List<QuickAction> actions;

    private ImageButton next;
    private ImageButton prev;

    private ViewPager viewPager;
    private LinearLayout dots;
    private LinearLayout controls;
    private View container;

    public QuickActionsWidget(Context context) {
        super(context);
    }

    public QuickActionsWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QuickActionsWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setActions(List<QuickAction> actions){

        this.actions = actions;
        this.actions.add(new NewAction());

        removeAllViews();
        setupLayout(getContext(), countPage());
    }

    public void setSelectionListener(QuickAction.QuickActionSelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    private void setupLayout(Context context, int pageCount){

        OsmandApplication application = ((OsmandApplication) getContext().getApplicationContext());
        boolean light = application.getSettings().isLightContent() && !application.getDaynightHelper().isNightMode();

        inflate(new ContextThemeWrapper(context, light
                ? R.style.OsmandLightTheme
                : R.style.OsmandDarkTheme), R.layout.quick_action_widget, this);

        container = findViewById(R.id.container);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewsPagerAdapter());

        container.setBackgroundResource(light
                ? R.drawable.bg_card_light
                : R.drawable.bg_card_dark);

        viewPager.getLayoutParams().height = actions.size() > ELEMENT_PER_PAGE / 2
                ? (int) getResources().getDimension(R.dimen.quick_action_widget_height_big)
                : (int) getResources().getDimension(R.dimen.quick_action_widget_height_small);

        viewPager.requestLayout();

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                updateControls(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        next = (ImageButton) findViewById(R.id.btnNext);
        prev = (ImageButton) findViewById(R.id.btnPrev);

        next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if (viewPager.getAdapter().getCount() > viewPager.getCurrentItem() + 1) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                }
            }
        });

        prev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if (viewPager.getCurrentItem() - 1 >= 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                }
            }
        });

        dots = (LinearLayout) findViewById(R.id.dots);
        dots.removeAllViews();

        if (pageCount > 1) {

            int color = light ?  R.color.icon_color_light : R.color.white_50_transparent;

            for (int i = 0; i < pageCount; i++) {

                ImageView dot = (ImageView) getLayoutInflater()
                        .inflate(R.layout.quick_action_widget_dot, dots, false);

                dot.setImageDrawable(i == 0
                        ? getIconsCache().getIcon(R.drawable.ic_dot_position, R.color.dashboard_blue)
                        : getIconsCache().getIcon(R.drawable.ic_dot_position, color));

                dots.addView(dot);
            }
        }

        controls = (LinearLayout) findViewById(R.id.controls);
        controls.setVisibility(pageCount > 1 ? VISIBLE : GONE);

        Drawable background = controls.getBackground();
        int backgroundColor = ContextCompat.getColor(context, light ? R.color.dashboard_divider_light : R.color.dashboard_divider_dark);
        if (background instanceof ShapeDrawable) {
            ((ShapeDrawable)background).getPaint().setColor(backgroundColor);
        } else if (background instanceof GradientDrawable) {
            ((GradientDrawable)background).setColor(backgroundColor);
        } else if (background instanceof ColorDrawable) {
            ((ColorDrawable)background).setColor(backgroundColor);
        }

        updateControls(viewPager.getCurrentItem());
    }

    private void updateControls(int position) {

        OsmandApplication application = ((OsmandApplication) getContext().getApplicationContext());
        boolean light = application.getSettings().isLightContent() && !application.getDaynightHelper().isNightMode();

        int colorEnabled = light ?  R.color.icon_color : R.color.color_white;
        int colorDisabled = light ?  R.color.icon_color_light : R.color.white_50_transparent;

        next.setEnabled(viewPager.getAdapter().getCount() > position + 1);
        next.setImageDrawable(next.isEnabled()
                ? getIconsCache().getIcon(R.drawable.ic_arrow_forward, colorEnabled)
                : getIconsCache().getIcon(R.drawable.ic_arrow_forward, colorDisabled));

        prev.setEnabled(position > 0);
        prev.setImageDrawable(prev.isEnabled()
                ? getIconsCache().getIcon(R.drawable.ic_arrow_back, colorEnabled)
                : getIconsCache().getIcon(R.drawable.ic_arrow_back, colorDisabled));

        for (int i = 0; i < dots.getChildCount(); i++){

            ((ImageView) dots.getChildAt(i)).setImageDrawable(i == position
                    ? getIconsCache().getIcon(R.drawable.ic_dot_position, R.color.dashboard_blue)
                    : getIconsCache().getIcon(R.drawable.ic_dot_position, colorDisabled));
        }
    }

    private View createPageView(ViewGroup container, int position){

        OsmandApplication application = ((OsmandApplication) getContext().getApplicationContext());
        boolean light = application.getSettings().isLightContent() && !application.getDaynightHelper().isNightMode();

        LayoutInflater li = getLayoutInflater(light
                ? R.style.OsmandLightTheme
                : R.style.OsmandDarkTheme);

        View page = li.inflate(R.layout.quick_action_widget_page, container, false);
        GridLayout gridLayout = (GridLayout) page.findViewById(R.id.grid);

        final boolean land = !AndroidUiHelper.isOrientationPortrait((Activity) getContext());
        final int maxItems = actions.size() == 1 ? 1 : ELEMENT_PER_PAGE;

        for (int i = 0; i < maxItems; i++){

            View view = li.inflate(R.layout.quick_action_widget_item, gridLayout, false);

            if (i + (position * ELEMENT_PER_PAGE) < actions.size()) {

                final QuickAction action = QuickActionFactory.produceAction(
                        actions.get(i + (position * ELEMENT_PER_PAGE)));

                ((ImageView) view.findViewById(imageView))
                        .setImageResource(action.getIconRes(getContext()));

                ((TextView) view.findViewById(R.id.title))
                        .setText(action.getActionText(application));

                if (action.isActionWithSlash(application)) {

                    ((ImageView) view.findViewById(R.id.imageSlash))
                            .setImageResource(light
                                    ? R.drawable.ic_action_icon_hide_white
                                    : R.drawable.ic_action_icon_hide_dark);
                }

                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (selectionListener != null) selectionListener.onActionSelected(action);
                    }
                });
				if (action.isActionEditable()) {
					view.setOnLongClickListener(new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							CreateEditActionDialog dialog = CreateEditActionDialog.newInstance(action.id);
							dialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), AddQuickActionDialog.TAG);
							return true;
						}
					});
				}
                if (!action.isActionEnable(application)) {
                    view.setEnabled(false);
                    view.setAlpha(0.5f);
                }
            }

            if (land) {
                view.findViewById(R.id.dividerBot).setVisibility(GONE);
                view.findViewById(R.id.dividerRight).setVisibility(VISIBLE);
            } else {
                view.findViewById(R.id.dividerBot).setVisibility(i < ELEMENT_PER_PAGE / 2 ? VISIBLE : GONE);
                view.findViewById(R.id.dividerRight).setVisibility(((i + 1) % 3) == 0 ? GONE : VISIBLE);
            }

            gridLayout.addView(view);
        }

        return gridLayout;
    }

    private class ViewsPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return countPage();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            View view = createPageView(container, position);
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

    private int countPage(){
        return (int) Math.ceil((actions.size()) / (double) 6);
    }

    private LayoutInflater getLayoutInflater(){
        return (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private LayoutInflater getLayoutInflater(@StyleRes int style){
        return (LayoutInflater) new ContextThemeWrapper(getContext(), style).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private OsmandApplication getApplication(){
        return (OsmandApplication)(getContext().getApplicationContext());
    }

    private IconsCache getIconsCache(){
        return getApplication().getIconsCache();
    }
}
