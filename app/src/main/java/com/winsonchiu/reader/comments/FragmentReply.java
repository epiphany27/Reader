/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.animation.Animator;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.rjeschke.txtmark.Processor;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;

public class FragmentReply extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    private static final int PAGE_REPLY = 0;
    private static final int PAGE_PREVIEW = 1;
    private static final int PAGE_COUNT = 2;
    public static final String TAG = FragmentReply.class.getCanonicalName();

    private FragmentListenerBase mListener;
    private Activity activity;

    private EditText editReply;
    private TextView textPreview;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Toolbar toolbar;
    private Toolbar toolbarActions;
    private PorterDuffColorFilter colorFilterIcon;
    private View viewDivider;
    private Menu menu;

    private int editMarginDefault;
    private int editMarginWithActions;

    public static FragmentReply newInstance() {
        FragmentReply fragment = new FragmentReply();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentReply() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view =  inflater.inflate(R.layout.fragment_reply, container, false);

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorIconFilter});
        int colorIconFilter = typedArray.getColor(0, 0xFFFFFFFF);
        typedArray.recycle();

        colorFilterIcon = new PorterDuffColorFilter(colorIconFilter,
                PorterDuff.Mode.MULTIPLY);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.item_reply));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNavigationBackClick();
            }
        });
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterIcon);
        setUpOptionsMenu();

        editMarginDefault = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        editMarginWithActions = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());

        editReply = (EditText) view.findViewById(R.id.edit_reply);
        textPreview = (TextView) view.findViewById(R.id.text_preview);
        viewDivider = view.findViewById(R.id.view_divider);

        toolbarActions = (Toolbar) view.findViewById(R.id.toolbar_actions);
        toolbarActions.inflateMenu(R.menu.menu_reply_actions);
        toolbarActions.setOnMenuItemClickListener(this);

        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Menu menu = toolbarActions.getMenu();

                        int maxNum = (int) (view.getWidth() / TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 48,
                                getResources().getDisplayMetrics()));
                        int numShown = 0;

                        for (int index = 0; index < menu.size(); index++) {

                            MenuItem menuItem = menu.getItem(index);
                            menuItem.getIcon().setColorFilter(colorFilterIcon);

                            if (numShown++ < maxNum - 1) {
                                menuItem
                                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                            }
                            else {
                                menuItem
                                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                            }
                        }
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new PagerAdapter() {

            @Override
            public CharSequence getPageTitle(int position) {

                switch (position) {
                    case PAGE_REPLY:
                        return getString(R.string.page_reply);
                    case PAGE_PREVIEW:
                        return getString(R.string.page_preview);
                }

                return super.getPageTitle(position);
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                return viewPager.getChildAt(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {

            }

            @Override
            public int getCount() {
                return PAGE_COUNT;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position,
                    float positionOffset,
                    int positionOffsetPixels) {

                if (position == PAGE_REPLY && toolbarActions.getVisibility() == View.VISIBLE) {
                    float translationY = positionOffset * (toolbarActions.getHeight() + viewDivider
                            .getHeight());
                    viewDivider.setTranslationY(translationY);
                    toolbarActions.setTranslationY(translationY);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == PAGE_PREVIEW) {
                    textPreview.setText(
                            Html.fromHtml(Processor.process(editReply.getText().toString())));
                }
                menu.findItem(R.id.item_hide_actions).setVisible(position == PAGE_REPLY);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout = (TabLayout) view.findViewById(R.id.layout_tab);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        editReply.requestFocus();

        return view;
    }

    private void setUpOptionsMenu() {

        toolbar.inflateMenu(R.menu.menu_reply);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().setColorFilter(colorFilterIcon);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement FragmentListenerBase");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
        mListener = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        int selectionStart = editReply.getSelectionStart();

        switch (item.getItemId()) {
            case R.id.item_hide_actions:
                final int margin;
                float translationY = toolbarActions.getHeight() + viewDivider
                        .getHeight();
                if (toolbarActions.isShown()) {
                    margin = editMarginDefault;
                    viewDivider.animate().translationY(translationY);
                    toolbarActions.animate().translationY(translationY).setListener(
                            new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    toolbarActions.setVisibility(View.GONE);

                                    ((RelativeLayout.LayoutParams) editReply.getLayoutParams()).bottomMargin = margin;
                                    editReply.requestLayout();
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            });
                }
                else {
                    margin = editMarginWithActions;
                    viewDivider.animate().translationY(0);
                    toolbarActions.animate().translationY(0).setListener(
                            new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    toolbarActions.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    ((RelativeLayout.LayoutParams) editReply.getLayoutParams()).bottomMargin = margin;
                                    editReply.requestLayout();
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            });
                }
                break;
            case R.id.item_reply_italicize:
                editReply.getText().insert(selectionStart, "**");
                editReply.setSelection(selectionStart + 1);
                break;
            case R.id.item_reply_bold:
                editReply.getText().insert(selectionStart, "****");
                editReply.setSelection(selectionStart + 2);
                break;
            case R.id.item_reply_strikethrough:
                editReply.getText().insert(selectionStart, "~~~~");
                editReply.setSelection(selectionStart + 2);
                break;
            case R.id.item_reply_quote:
                editReply.getText().insert(selectionStart, "\n> ");
                editReply.setSelection(selectionStart + 2);
                break;
            case R.id.item_reply_link:
                String labelText = getString(R.string.reply_label_text);
                String labelLink = getString(R.string.reply_label_link);
                int indexStart = selectionStart + 1;
                int indexEnd = indexStart + labelText.length();
                editReply.getText().insert(selectionStart, "[" + labelText + "](" + labelLink + ")");
                editReply.setSelection(indexStart, indexEnd);
                break;
            case R.id.item_reply_list_bulleted:
                editReply.getText().insert(selectionStart, "\n\n* \n* \n* ");
                editReply.setSelection(selectionStart + 4);
                break;
            case R.id.item_reply_list_numbered:
                editReply.getText().insert(selectionStart, "\n\n1. \n2. \n3. ");
                editReply.setSelection(selectionStart + 5);
                break;
        }

        return true;
    }

    @Override
    public boolean navigateBack() {
        return true;
    }
}