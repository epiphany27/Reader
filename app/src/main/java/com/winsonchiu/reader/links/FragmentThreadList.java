/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.RequestManager;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.FragmentNewPost;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.search.ControllerSearch;
import com.winsonchiu.reader.search.FragmentSearch;
import com.winsonchiu.reader.theme.ThemeWrapper;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.CustomItemTouchHelper;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.utils.ItemDecorationDivider;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.ScrollAwareFloatingActionButtonBehavior;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.utils.UtilsReddit;

import javax.inject.Inject;

public class FragmentThreadList extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentThreadList.class.getCanonicalName();
    private static final long DURATION_TRANSITION = 150;
    private static final long DURATION_ACTIONS_FADE = 150;
    private static final float OFFSET_MODIFIER = 0.5f;
    private FragmentListenerBase mListener;

    private SharedPreferences preferences;
    private RecyclerView recyclerThreadList;
    private AdapterLink adapterLink;
    private SwipeRefreshLayout swipeRefreshThreadList;
    private RecyclerView.LayoutManager layoutManager;
    private MenuItem itemInterface;

    private MenuItem itemSearch;
    private TextView textSidebar;
    private DrawerLayout drawerLayout;
    private TextView textEmpty;
    private Menu menu;
    private MenuItem itemSortTime;
    private Toolbar toolbar;
    private AdapterLinkList adapterLinkList;
    private AdapterLinkGrid adapterLinkGrid;
    private Button buttonSubscribe;
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private AdapterLink.ViewHolderHeader.EventListener eventListenerHeader;
    private DisallowListener disallowListener;
    private RecyclerCallback recyclerCallback;
    private ControllerLinks.Listener listenerLinks;
    private ControllerUser.Listener listenerUser;
    private Snackbar snackbar;
    private CustomItemTouchHelper itemTouchHelper;
    private FloatingActionButton buttonExpandActions;
    private FastOutSlowInInterpolator fastOutSlowInInterpolator = new FastOutSlowInInterpolator();
    private LinearLayout layoutActions;
    private FloatingActionButton buttonClearViewed;
    private FloatingActionButton buttonJumpTop;
    private ScrollAwareFloatingActionButtonBehavior behaviorButtonExpandActions;
    private View view;
    private CustomColorFilter colorFilterPrimary;
    private CustomColorFilter colorFilterAccent;
    private ItemDecorationDivider itemDecorationDivider;
    private boolean isFinished;

    @Inject Historian historian;
    @Inject ControllerLinks controllerLinks;
    @Inject Picasso picasso;
    @Inject ControllerUser controllerUser;
    @Inject ControllerSearch controllerSearch;

    public static FragmentThreadList newInstance() {
        FragmentThreadList fragment = new FragmentThreadList();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentThreadList() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void setUpToolbar() {

        if (getFragmentManager().getBackStackEntryCount() <= 1) {
            toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.openDrawer();
                }
            });
        }
        else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onNavigationBackClick();
                }
            });
        }
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);
        toolbar.inflateMenu(R.menu.menu_thread_list);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        itemInterface = menu.findItem(R.id.item_interface);
        switch (preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)) {
            case AppSettings.MODE_LIST:
                itemInterface.setIcon(R.drawable.ic_view_module_white_24dp);
                break;
            case AppSettings.MODE_GRID:
                itemInterface.setIcon(R.drawable.ic_view_list_white_24dp);
                break;
        }

        itemSortTime = menu.findItem(R.id.item_sort_time);
        itemSearch = menu.findItem(R.id.item_search);

        menu.findItem(controllerLinks.getSort().getMenuId()).setChecked(true);
        menu.findItem(controllerLinks.getTime().getMenuId()).setChecked(true);
        itemSortTime.setTitle(
                getString(R.string.time) + Reddit.TIME_SEPARATOR + menu
                        .findItem(controllerLinks.getTime().getMenuId()).toString());

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
        }

    }

    private void resetAdapter(AdapterLink newAdapter) {

        RecyclerView.LayoutManager layoutManagerCurrent = recyclerThreadList.getLayoutManager();

        int size = layoutManagerCurrent instanceof StaggeredGridLayoutManager ? ((StaggeredGridLayoutManager) layoutManagerCurrent).getSpanCount() : 1;

        int[] currentPosition = new int[size];
        if (layoutManagerCurrent instanceof LinearLayoutManager) {
            currentPosition[0] = ((LinearLayoutManager) layoutManagerCurrent)
                    .findFirstVisibleItemPosition();
        }
        else if (layoutManagerCurrent instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManagerCurrent).findFirstCompletelyVisibleItemPositions(
                    currentPosition);
        }

        adapterLink = newAdapter;
        layoutManager = adapterLink.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            recyclerThreadList.setPadding(0, 0, 0, 0);
        }
        else {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    getResources().getDisplayMetrics());
            recyclerThreadList.setPadding(padding, 0, padding, 0);
        }

        /*
            Note that we must call setAdapter before setLayoutManager or the ViewHolders
            will not be properly recycled, leading to memory leaks.
         */
        recyclerThreadList.setAdapter(adapterLink);
        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.scrollToPosition(currentPosition[0]);
        if (layoutManager instanceof LinearLayoutManager) {
            recyclerThreadList.addItemDecoration(itemDecorationDivider);
        }
        else {
            recyclerThreadList.removeItemDecoration(itemDecorationDivider);
        }
    }

    @Override
    protected void inject() {
        ((ActivityMain) getActivity()).getComponentActivity().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initialize();

        view = inflater.inflate(R.layout.fragment_thread_list, container, false);

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);

        TypedArray typedArray = getActivity().getTheme().obtainStyledAttributes(
                new int[] {R.attr.colorPrimary, R.attr.colorAccent});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        int colorAccent = typedArray.getColor(1, getResources().getColor(R.color.colorAccent));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.showOnWhite(colorPrimary) ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;
        int colorResourceAccent = UtilsColor.showOnWhite(colorAccent) ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        colorFilterPrimary = new CustomColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);
        colorFilterAccent = new CustomColorFilter(getResources().getColor(colorResourceAccent), PorterDuff.Mode.MULTIPLY);

        int styleColorBackground = AppSettings.THEME_DARK.equals(mListener.getThemeBackground()) ? R.style.MenuDark : R.style.MenuLight;

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(new ThemeWrapper(getActivity(), UtilsColor.getThemeForColor(getResources(), colorPrimary, mListener)), styleColorBackground);

        toolbar = (Toolbar) getActivity().getLayoutInflater().cloneInContext(contextThemeWrapper).inflate(R.layout.toolbar, layoutAppBar, false);
        layoutAppBar.addView(toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction()
                        .hide(FragmentThreadList.this)
                        .add(R.id.frame_fragment, FragmentSearch.newInstance(true),
                                FragmentSearch.TAG)
                        .addToBackStack(null)
                        .commit();
            }
        });
        setUpToolbar();

        layoutActions = (LinearLayout) view.findViewById(R.id.layout_actions);

        buttonExpandActions = (FloatingActionButton) view.findViewById(R.id.button_expand_actions);
        buttonExpandActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLayoutActions();
            }
        });

        behaviorButtonExpandActions = new ScrollAwareFloatingActionButtonBehavior(
                getActivity(), null,
                new ScrollAwareFloatingActionButtonBehavior.OnVisibilityChangeListener() {
                    @Override
                    public void onStartHideFromScroll() {
                        hideLayoutActions(0);
                    }

                    @Override
                    public void onEndHideFromScroll() {
                        buttonExpandActions.setImageResource(R.drawable.ic_unfold_more_white_24dp);
                        buttonExpandActions.setColorFilter(colorFilterAccent);
                    }

                });
        ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams())
                .setBehavior(behaviorButtonExpandActions);


        buttonJumpTop = (FloatingActionButton) view.findViewById(R.id.button_jump_top);
        buttonJumpTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollToPositionWithOffset(0, 0);
            }
        });
        buttonJumpTop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(), getString(R.string.content_description_button_jump_top),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        buttonClearViewed = (FloatingActionButton) view.findViewById(R.id.button_clear_viewed);
        buttonClearViewed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controllerLinks.clearViewed(historian);
            }
        });
        buttonClearViewed.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(),
                        getString(R.string.content_description_button_clear_viewed),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        // Margin is included within shadow margin on pre-Lollipop, so remove all regular margin
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams())
                    .setMargins(0, 0, 0, 0);

            int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                    getResources().getDisplayMetrics());

            LinearLayout.LayoutParams layoutParamsJumpTop = (LinearLayout.LayoutParams) buttonJumpTop
                    .getLayoutParams();
            layoutParamsJumpTop.setMargins(0, 0, 0, 0);
            buttonJumpTop.setLayoutParams(layoutParamsJumpTop);

            LinearLayout.LayoutParams layoutParamsClearViewed = (LinearLayout.LayoutParams) buttonClearViewed
                    .getLayoutParams();
            layoutParamsClearViewed.setMargins(0, 0, 0, 0);
            buttonClearViewed.setLayoutParams(layoutParamsClearViewed);

            RelativeLayout.LayoutParams layoutParamsActions = (RelativeLayout.LayoutParams) layoutActions
                    .getLayoutParams();
            layoutParamsActions.setMarginStart(margin);
            layoutParamsActions.setMarginEnd(margin);
            layoutActions.setLayoutParams(layoutParamsActions);
        }

        buttonExpandActions.setColorFilter(colorFilterAccent);
        buttonJumpTop.setColorFilter(colorFilterAccent);
        buttonClearViewed.setColorFilter(colorFilterAccent);

        swipeRefreshThreadList = (SwipeRefreshLayout) view.findViewById(
                R.id.swipe_refresh_thread_list);
        swipeRefreshThreadList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                controllerLinks.reloadSubreddit();
            }
        });
        if (adapterLinkList == null) {
            adapterLinkList = new AdapterLinkList(getActivity(),
                    controllerLinks,
                    eventListenerHeader,
                    mListener.getEventListenerBase(),
                    disallowListener,
                    recyclerCallback);
        }
        if (adapterLinkGrid == null) {
            adapterLinkGrid = new AdapterLinkGrid(getActivity(),
                    controllerLinks,
                    eventListenerHeader,
                    mListener.getEventListenerBase(),
                    disallowListener,
                    recyclerCallback);
        }

        if (AppSettings.MODE_LIST.equals(preferences.getString(AppSettings.INTERFACE_MODE,
                AppSettings.MODE_GRID))) {
            adapterLink = adapterLinkList;
        }
        else {
            adapterLink = adapterLinkGrid;
        }

        adapterLinkList.setActivity(getActivity());
        adapterLinkGrid.setActivity(getActivity());

        itemDecorationDivider = new ItemDecorationDivider(getActivity(), ItemDecorationDivider.VERTICAL_LIST);

        recyclerThreadList = (RecyclerView) view.findViewById(R.id.recycler_thread_list);
        recyclerThreadList.setItemAnimator(null);
        resetAdapter(adapterLink);

        recyclerThreadList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        picasso.resumeTag(AdapterLink.TAG_PICASSO);
                        break;
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        picasso.pauseTag(AdapterLink.TAG_PICASSO);
                        break;
                }
            }
        });

        itemTouchHelper = new CustomItemTouchHelper(
                new CustomItemTouchHelper.SimpleCallback(getActivity(),
                        R.drawable.ic_visibility_off_white_24dp,
                        ItemTouchHelper.START | ItemTouchHelper.END,
                        ItemTouchHelper.START | ItemTouchHelper.END) {

                    @Override
                    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {

                        if (layoutManager instanceof StaggeredGridLayoutManager) {
                            return 1f / ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
                        }

                        return 0.5f;
                    }

                    @Override
                    public int getSwipeDirs(RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder) {

                        if (viewHolder.getAdapterPosition() == 0) {
                            return 0;
                        }

                        ViewGroup.LayoutParams layoutParams = viewHolder.itemView.getLayoutParams();

                        if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams &&
                                !((StaggeredGridLayoutManager.LayoutParams) layoutParams)
                                        .isFullSpan()) {

                            int spanCount = layoutManager instanceof StaggeredGridLayoutManager ?
                                    ((StaggeredGridLayoutManager) layoutManager).getSpanCount() : 2;
                            int spanIndex = ((StaggeredGridLayoutManager.LayoutParams) layoutParams)
                                    .getSpanIndex() % spanCount;
                            if (spanIndex == 0) {
                                return ItemTouchHelper.END;
                            }
                            else if (spanIndex == spanCount - 1) {
                                return ItemTouchHelper.START;
                            }

                        }

                        return super.getSwipeDirs(recyclerView, viewHolder);
                    }

                    @Override
                    public boolean isLongPressDragEnabled() {
                        return false;
                    }

                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                        // Offset by 1 due to subreddit header
                        final int position = viewHolder.getAdapterPosition() - 1;
                        final Link link = controllerLinks.remove(position);
                        mListener.getEventListenerBase().hide(link);

                        if (snackbar != null) {
                            snackbar.dismiss();
                        }

                        SpannableString text = new SpannableString(link.isHidden() ? getString(R.string.link_hidden) : getString(R.string.link_shown));
                        text.setSpan(new ForegroundColorSpan(colorFilterPrimary.getColor()), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                        //noinspection ResourceType
                        snackbar = Snackbar.make(recyclerThreadList, text,
                                UtilsAnimation.SNACKBAR_DURATION)
                                .setActionTextColor(colorFilterPrimary.getColor())
                                .setAction(
                                        R.string.undo, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                mListener.getEventListenerBase().hide(link);
                                                controllerLinks.add(position, link);
                                                recyclerThreadList.invalidate();
                                            }
                                        });
                        snackbar.getView().setBackgroundColor(colorPrimary);
                        snackbar.show();
                    }
                });
        itemTouchHelper.attachToRecyclerView(recyclerThreadList);

        recyclerThreadList.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
//                rv.getChildViewHolder(rv.findChildViewUnder(e.getX(), e.getY()));
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        if (layoutManager instanceof LinearLayoutManager) {
            recyclerThreadList.setPadding(0, 0, 0, 0);
        }
        else {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    getResources().getDisplayMetrics());
            recyclerThreadList.setPadding(padding, 0, padding, 0);
        }

        drawerLayout = (DrawerLayout) view.findViewById(R.id.layout_drawer);

        textSidebar = (TextView) view.findViewById(R.id.text_sidebar);
        textSidebar.setMovementMethod(LinkMovementMethod.getInstance());

        buttonSubscribe = (Button) view.findViewById(R.id.button_subscribe);
        buttonSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSubscribe.setText(
                        controllerLinks.getSubreddit().isUserIsSubscriber() ?
                                R.string.subscribe : R.string.unsubscribe);
                controllerLinks.subscribe();
                if (controllerLinks.getSubreddit().isUserIsSubscriber()) {
                    controllerSearch.addSubreddit(controllerLinks.getSubreddit());
                }
            }
        });

        textEmpty = (TextView) view.findViewById(R.id.text_empty);

        return view;
    }

    private void initialize() {
        eventListenerHeader = new AdapterLink.ViewHolderHeader.EventListener() {
            @Override
            public void onClickSubmit(Reddit.PostType postType) {

                if (TextUtils.isEmpty(controllerUser.getUser().getName())) {
                    Toast.makeText(getActivity(), getString(R.string.must_be_logged_in),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                FragmentNewPost fragmentNewPost = FragmentNewPost.newInstance(
                        controllerUser.getUser().getName(),
                        controllerLinks.getSubreddit().getUrl(),
                        postType,
                        controllerLinks.getSubreddit().getSubmitTextHtml());

                getFragmentManager().beginTransaction()
                        .hide(FragmentThreadList.this)
                        .add(R.id.frame_fragment, fragmentNewPost, FragmentNewPost.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void showSidebar() {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        };

        disallowListener = new DisallowListener() {
            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                Log.d(TAG, "requestDisallowInterceptTouchEventVertical() called with: " + "disallow = [" + disallow + "]");
                recyclerThreadList.requestDisallowInterceptTouchEvent(disallow);
                swipeRefreshThreadList.requestDisallowInterceptTouchEvent(disallow);
                itemTouchHelper.select(null, CustomItemTouchHelper.ACTION_STATE_IDLE);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {
                itemTouchHelper.setDisallow(disallow);
            }
        };

        recyclerCallback = new RecyclerCallback() {

            @Override
            public int getRecyclerHeight() {
                return recyclerThreadList.getHeight();
            }

            @Override
            public RecyclerView.LayoutManager getLayoutManager() {
                return layoutManager;
            }

            @Override
            public void scrollTo(final int position) {
                recyclerThreadList.requestLayout();
                UtilsAnimation.scrollToPositionWithCentering(position, recyclerThreadList, layoutManager, false);
            }

            @Override
            public void scrollAndCenter(int position, int height) {

            }

            @Override
            public void hideToolbar() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void onReplyShown() {
                behaviorButtonExpandActions.animateOut(buttonExpandActions);
            }

            @Override
            public RequestManager getRequestManager() {
                return getGlideRequestManager();
            }
        };

        listenerLinks = new ControllerLinks.Listener() {

            @Override
            public void setSortAndTime(Sort sort, Time time) {
                menu.findItem(sort.getMenuId()).setChecked(true);
                menu.findItem(time.getMenuId()).setChecked(true);
                itemSortTime.setTitle(
                        getString(R.string.time) + Reddit.TIME_SEPARATOR + menu
                                .findItem(time.getMenuId()).toString());
            }

            @Override
            public void showEmptyView(boolean isEmpty) {
                textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void loadSideBar(Subreddit subreddit) {
                if (subreddit.getUrl().equals("/") || "/r/all/"
                        .equalsIgnoreCase(subreddit.getUrl())) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                            GravityCompat.END);
                    return;
                }

                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
                textSidebar.setText(UtilsReddit.getFormattedHtml(subreddit.getDescriptionHtml()));
                drawerLayout.setDrawerLockMode(
                        DrawerLayout.LOCK_MODE_UNLOCKED);
                if (subreddit.isUserIsSubscriber()) {
                    buttonSubscribe.setText(R.string.unsubscribe);
                }
                else {
                    buttonSubscribe.setText(R.string.subscribe);
                }
                buttonSubscribe.setVisibility(controllerUser.hasUser() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void scrollTo(int position) {
                scrollToPositionWithOffset(position, 0);
            }

            @Override
            public void setSubscribed(boolean subscribed) {
                if (subscribed) {
                    buttonSubscribe.setText(R.string.unsubscribe);
                }
                else {
                    buttonSubscribe.setText(R.string.subscribe);
                }
            }

            @Override
            public void post(Runnable runnable) {
                recyclerThreadList.post(runnable);
            }

            @Override
            public RecyclerView.Adapter getAdapter() {
                return adapterLink;
            }

            @Override
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshThreadList.setRefreshing(refreshing);
            }
        };

        listenerUser = new ControllerUser.Listener() {
            @Override
            public void onUserLoaded(@Nullable User user) {
                if (user == null) {
                    buttonSubscribe.setVisibility(View.GONE);
                }
                else {
                    controllerLinks.reloadSubredditOnly()
                            .subscribe(new FinalizingSubscriber<Subreddit>() {
                                @Override
                                public void next(Subreddit next) {
                                    if (next.isUserIsSubscriber()) {
                                        buttonSubscribe.setText(R.string.unsubscribe);
                                    } else {
                                        buttonSubscribe.setText(R.string.subscribe);
                                    }
                                    buttonSubscribe.setVisibility(controllerUser.hasUser() ? View.VISIBLE : View.GONE);
                                }
                            });
                }
            }
        };
    }

    private void toggleLayoutActions() {
        // TODO: Move to a Utils class
        if (buttonJumpTop.isShown()) {
            hideLayoutActions(DURATION_ACTIONS_FADE);
        }
        else {
            showLayoutActions();
        }
    }

    private void showLayoutActions() {

        for (int index = layoutActions.getChildCount() - 1; index >= 0; index--) {
            final View view = layoutActions.getChildAt(index);
            view.setScaleX(0f);
            view.setScaleY(0f);
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            final int finalIndex = index;
            ViewCompat.animate(view)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(fastOutSlowInInterpolator)
                    .setDuration(DURATION_ACTIONS_FADE)
                    .setStartDelay((long) ((layoutActions
                            .getChildCount() - 1 - index) * DURATION_ACTIONS_FADE * OFFSET_MODIFIER))
                    .setListener(new ViewPropertyAnimatorListener() {
                        @Override
                        public void onAnimationStart(View view) {
                            if (finalIndex == 0) {
                                buttonExpandActions.setImageResource(android.R.color.transparent);
                            }
                        }

                        @Override
                        public void onAnimationEnd(View view) {

                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .start();
        }

    }

    private void hideLayoutActions(long offset) {
        for (int index = 0; index < layoutActions.getChildCount(); index++) {
            final View view = layoutActions.getChildAt(index);
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setAlpha(1f);
            final int finalIndex = index;
            ViewCompat.animate(view)
                    .alpha(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setInterpolator(fastOutSlowInInterpolator)
                    .setDuration(DURATION_ACTIONS_FADE)
                    .setStartDelay((long) (index * offset * OFFSET_MODIFIER))
                    .setListener(new ViewPropertyAnimatorListener() {
                        @Override
                        public void onAnimationStart(View view) {

                        }

                        @Override
                        public void onAnimationEnd(View view) {
                            view.setVisibility(View.GONE);
                            if (finalIndex == layoutActions.getChildCount() - 1) {
                                buttonExpandActions
                                        .setImageResource(R.drawable.ic_unfold_more_white_24dp);
                                buttonExpandActions.setColorFilter(colorFilterAccent);
                            }
                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .start();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        controllerLinks.addListener(listenerLinks);
        controllerUser.addListener(listenerUser);
    }

    @Override
    public void onPause() {
        controllerLinks.removeListener(listenerLinks);
        controllerUser.removeListener(listenerUser);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        adapterLink.destroyViewHolders();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        item.setChecked(true);
        switch (item.getItemId()) {
            case R.id.item_search:
                getFragmentManager().beginTransaction()
                        .hide(FragmentThreadList.this)
                        .add(R.id.frame_fragment, FragmentSearch.newInstance(false),
                                FragmentSearch.TAG)
                        .addToBackStack(null)
                        .commit();
                return true;
            case R.id.item_interface:
                if (AppSettings.MODE_LIST.equals(
                        preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID))) {
                    resetAdapter(adapterLinkGrid);
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_list_white_24dp));
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)
                            .apply();
                }
                else {
                    resetAdapter(adapterLinkList);
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_module_white_24dp));
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST)
                            .apply();
                }
                item.getIcon().setColorFilter(colorFilterPrimary);
                return true;

        }


        Sort sort = Sort.fromMenuId(item.getItemId());
        if (sort != null) {
            controllerLinks.setSort(sort);
            scrollToPositionWithOffset(0, 0);
            return true;
        }

        Time time = Time.fromMenuId(item.getItemId());
        if (time != null) {
            controllerLinks.setTime(time);
            itemSortTime.setTitle(
                    getString(R.string.time) + Reddit.TIME_SEPARATOR + item.toString());
            scrollToPositionWithOffset(0, 0);
            return true;
        }

        return false;
    }

    /**
     * Helper method to scroll without if statement sprinkled everywhere, as
     * scrollToPositionWithOffset is not abstracted into the upper LayoutManager
     *
     * @param position to scroll to
     * @param offset   from top of view
     */
    private void scrollToPositionWithOffset(int position, int offset) {
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
        }
        else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager)
                    .scrollToPositionWithOffset(position, offset);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            adapterLink.pauseViewHolders();
            view.setVisibility(View.INVISIBLE);
        }
        else {
            view.setAlpha(1f);
            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setVisibilityOfThing(int visibility, Thing thing) {
        super.setVisibilityOfThing(visibility, thing);
        adapterLink.setVisibility(visibility, thing);
    }

    @Override
    public void navigateBack() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return;
        }

        isFinished = adapterLink.navigateBack();
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void onShown() {
        adapterLink.setVisibility(View.VISIBLE);
        ViewCompat.animate(buttonExpandActions)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .withLayer()
                .setDuration(DURATION_TRANSITION)
                .setInterpolator(ScrollAwareFloatingActionButtonBehavior.INTERPOLATOR)
                .setListener(null);
    }

    @Override
    public void onWindowTransitionStart() {
        super.onWindowTransitionStart();
        ViewCompat.animate(buttonExpandActions)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .withLayer()
                .setDuration(DURATION_TRANSITION)
                .setInterpolator(ScrollAwareFloatingActionButtonBehavior.INTERPOLATOR)
                .setListener(null);
    }

}