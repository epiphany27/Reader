/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.FragmentNewPost;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.data.reddit.Likes;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Report;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.rx.ActionLog;
import com.winsonchiu.reader.rx.ObserverEmpty;
import com.winsonchiu.reader.rx.ObserverError;
import com.winsonchiu.reader.search.FragmentSearch;
import com.winsonchiu.reader.utils.CustomItemTouchHelper;
import com.winsonchiu.reader.utils.ItemDecorationDivider;
import com.winsonchiu.reader.utils.ScrollAwareFloatingActionButtonBehavior;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.utils.UtilsReddit;
import com.winsonchiu.reader.utils.UtilsRx;
import com.winsonchiu.reader.utils.UtilsTheme;

import javax.inject.Inject;

import butterknife.BindView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class FragmentThreadList extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentThreadList.class.getCanonicalName();

    private static final long DURATION_TRANSITION = 150;
    private static final long DURATION_ACTIONS_FADE = 150;
    private static final float OFFSET_MODIFIER = 0.5f;

    private FragmentListenerBase mListener;

    private Toolbar toolbar;
    private Menu menu;
    private MenuItem itemSearch;
    private MenuItem itemSortTime;
    private MenuItem itemInterface;

    private AdapterLinkList adapterLinkList;
    private AdapterLinkGrid adapterLinkGrid;
    private AdapterLink.ViewHolderHeader.EventListener eventListenerHeader;
    private ControllerUser.Listener listenerUser;

    private Snackbar snackbar;
    private CustomItemTouchHelper itemTouchHelper;
    private FastOutSlowInInterpolator fastOutSlowInInterpolator = new FastOutSlowInInterpolator();

    private ScrollAwareFloatingActionButtonBehavior behaviorButtonExpandActions;
    private ItemDecorationDivider itemDecorationDivider;

    private View view;
    private AdapterLink adapterLink;
    private RecyclerView.LayoutManager layoutManager;

    private boolean isFinished;
    private Subreddit subreddit = new Subreddit();

    private Subscription subscriptionData;
    private Subscription subscriptionLoading;
    private Subscription subscriptionSort;
    private Subscription subscriptionTime;
    private Subscription subscriptionErrors;

    private Report reportSelected;

    @BindView(R.id.layout_coordinator) CoordinatorLayout layoutCoordinator;
    @BindView(R.id.layout_app_bar) AppBarLayout layoutAppBar;
    @BindView(R.id.recycler_thread_list) RecyclerView recyclerThreadList;
    @BindView(R.id.layout_actions) ViewGroup layoutActions;
    @BindView(R.id.button_expand_actions) FloatingActionButton buttonExpandActions;
    @BindView(R.id.button_clear_viewed) FloatingActionButton buttonClearViewed;
    @BindView(R.id.button_jump_top) FloatingActionButton buttonJumpTop;
    @BindView(R.id.swipe_refresh_thread_list) SwipeRefreshLayout swipeRefreshThreadList;
    @BindView(R.id.layout_drawer) DrawerLayout layoutDrawer;
    @BindView(R.id.text_sidebar) TextView textSidebar;
    @BindView(R.id.text_empty) TextView textEmpty;
    @BindView(R.id.button_subscribe) Button buttonSubscribe;

    @Inject SharedPreferences preferences;
    @Inject Historian historian;
    @Inject ControllerLinks controllerLinks;
    @Inject Picasso picasso;
    @Inject ControllerUser controllerUser;

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initialize();

        view = bind(inflater.inflate(R.layout.fragment_thread_list, container, false));

        setUpToolbar();

        buttonExpandActions.setOnClickListener(v -> toggleLayoutActions());

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
                        buttonExpandActions.setColorFilter(themer.getColorFilterAccent());
                    }

                });
        ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams())
                .setBehavior(behaviorButtonExpandActions);


        buttonJumpTop.setOnClickListener(v -> scrollToPositionWithOffset(0, 0));
        buttonJumpTop.setOnLongClickListener(v -> {
            Toast.makeText(getActivity(), getString(R.string.content_description_button_jump_top),
                    Toast.LENGTH_SHORT).show();
            return false;
        });

        buttonClearViewed.setOnClickListener(v -> controllerLinks.clearViewed(historian));
        buttonClearViewed.setOnLongClickListener(v -> {
            Toast.makeText(getActivity(),
                    getString(R.string.content_description_button_clear_viewed),
                    Toast.LENGTH_SHORT).show();
            return false;
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

        buttonExpandActions.setColorFilter(themer.getColorFilterAccent());
        buttonJumpTop.setColorFilter(themer.getColorFilterAccent());
        buttonClearViewed.setColorFilter(themer.getColorFilterAccent());

        swipeRefreshThreadList.setOnRefreshListener(() -> {
            controllerLinks.reload();
        });

        AdapterListener adapterListener = new AdapterListener() {

            @Override
            public void requestMore() {
                controllerLinks.loadMore();
            }

            @Override
            public void scrollAndCenter(int position, int height) {
                UtilsAnimation.scrollToPositionWithCentering(position, recyclerThreadList, height, 0, 0, false);
            }

            @Override
            public void hideToolbar() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void clearDecoration() {
                behaviorButtonExpandActions.animateOut(buttonExpandActions);
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerThreadList.requestDisallowInterceptTouchEvent(disallow);
                swipeRefreshThreadList.requestDisallowInterceptTouchEvent(disallow);
                itemTouchHelper.select(null, CustomItemTouchHelper.ACTION_STATE_IDLE);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {
                itemTouchHelper.setDisallow(disallow);
            }
        };

        AdapterLink.ViewHolderLink.Listener listener = new LinksListenerBase(mListener.getEventListenerBase()) {
            @Override
            public void onVote(Link link, AdapterLink.ViewHolderLink viewHolderLink, Likes vote) {
                mListener.getEventListenerBase()
                        .onVote(link, vote)
                        .subscribe(new ObserverEmpty<Link>() {
                            @Override
                            public void onError(Throwable e) {
                                controllerLinks.getEventHolder().getErrors().call(LinksError.VOTE);
                            }

                            @Override
                            public void onNext(Link link) {
                                controllerLinks.update(link);
                            }
                        });
            }

            @Override
            public void onDelete(Link link) {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.delete_post)
                        .setMessage(link.getTitle())
                        .setPositiveButton(R.string.yes,
                                (dialog, which) -> {
                                    mListener.getEventListenerBase()
                                            .onDelete(link)
                                            .subscribe(new ObserverEmpty<Link>() {
                                                @Override
                                                public void onError(Throwable e) {
                                                    controllerLinks.getEventHolder().getErrors().call(LinksError.DELETE);
                                                }

                                                @Override
                                                public void onNext(Link link) {
                                                    controllerLinks.remove(link);
                                                }
                                            });
                                })
                        .setNegativeButton(R.string.no, null)
                        .show();
            }

            @Override
            public void onReport(Link link) {
                // TODO: Add link title
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.report_title)
                        .setSingleChoiceItems(Report.getDisplayReasons(getResources()), -1, (dialog, which) -> {
                            reportSelected = Report.values()[which];
                        })
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            if (reportSelected == Report.OTHER) {
                                View viewDialog = LayoutInflater.from(getContext()).inflate(R.layout.dialog_text_input, null, false);
                                final EditText editText = (EditText) viewDialog.findViewById(R.id.edit_text);
                                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
                                new AlertDialog.Builder(getContext())
                                        .setView(viewDialog)
                                        .setTitle(R.string.item_report)
                                        .setPositiveButton(R.string.ok, (dialog1, which1) -> {
                                            mListener.getEventListenerBase()
                                                    .onReport(link, editText.getText().toString())
                                                    .subscribe(new ObserverError<String>() {
                                                        @Override
                                                        public void onError(Throwable e) {
                                                            controllerLinks.getEventHolder().getErrors().call(LinksError.REPORT);
                                                        }
                                                    });
                                        })
                                        .setNegativeButton(R.string.cancel, (dialog1, which1) -> {
                                            dialog1.dismiss();
                                        })
                                        .show();
                            }
                            else if (reportSelected != null) {
                                mListener.getEventListenerBase()
                                        .onReport(link, reportSelected.getReason())
                                        .subscribe(new ObserverError<String>() {
                                            @Override
                                            public void onError(Throwable e) {
                                                controllerLinks.getEventHolder().getErrors().call(LinksError.REPORT);
                                            }
                                        });
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }

            @Override
            public void onSave(Link link) {
                if (link.isSaved()) {
                    mListener.getEventListenerBase()
                            .onUnsave(link)
                            .subscribe(new ObserverEmpty<Link>() {
                                @Override
                                public void onError(Throwable e) {
                                    controllerLinks.getEventHolder().getErrors().call(LinksError.UNSAVE);
                                }

                                @Override
                                public void onNext(Link link) {
                                    controllerLinks.update(link);
                                }
                            });
                }
                else {
                    mListener.getEventListenerBase()
                            .onSave(link)
                            .subscribe(new ObserverEmpty<Link>() {
                                @Override
                                public void onError(Throwable e) {
                                    controllerLinks.getEventHolder().getErrors().call(LinksError.SAVE);
                                }

                                @Override
                                public void onNext(Link link) {
                                    controllerLinks.update(link);
                                }
                            });
                }
            }

            @Override
            public void onMarkNsfw(Link link) {
                if (link.isOver18()) {
                    mListener.getEventListenerBase()
                            .onUnmarkNsfw(link)
                            .subscribe(new ObserverEmpty<Link>() {
                                @Override
                                public void onError(Throwable e) {
                                    controllerLinks.getEventHolder().getErrors().call(LinksError.UNMARK_NSFW);
                                }

                                @Override
                                public void onNext(Link link) {
                                    controllerLinks.update(link);
                                }
                            });
                }
                else {
                    mListener.getEventListenerBase()
                            .onMarkNsfw(link)
                            .subscribe(new ObserverEmpty<Link>() {
                                @Override
                                public void onError(Throwable e) {
                                    controllerLinks.getEventHolder().getErrors().call(LinksError.MARK_NSFW);
                                }

                                @Override
                                public void onNext(Link link) {
                                    controllerLinks.update(link);
                                }
                            });
                }
            }
        };

        adapterLinkList = new AdapterLinkList(getActivity(),
                adapterListener,
                eventListenerHeader,
                listener);

        adapterLinkGrid = new AdapterLinkGrid(getActivity(),
                adapterListener,
                eventListenerHeader,
                listener);

        adapterLink = AppSettings.MODE_GRID.equals(preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID))
                ? adapterLinkGrid
                : adapterLinkList;

        itemDecorationDivider = new ItemDecorationDivider(getActivity(), ItemDecorationDivider.VERTICAL_LIST);

//        recyclerThreadList.setItemAnimator(null);
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
                        Link link = controllerLinks.hideLink(viewHolder.getAdapterPosition());
                        mListener.getEventListenerBase().hide(link);

                        if (snackbar != null) {
                            snackbar.dismiss();
                        }

                        SpannableString text = new SpannableString(link.isHidden() ? getString(R.string.link_hidden) : getString(R.string.link_shown));
                        text.setSpan(new ForegroundColorSpan(themer.getColorFilterPrimary().getColor()), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                        //noinspection ResourceType
                        snackbar = Snackbar.make(recyclerThreadList, text,
                                UtilsAnimation.SNACKBAR_DURATION)
                                .setActionTextColor(themer.getColorFilterPrimary().getColor())
                                .setAction(R.string.undo, v -> {
                                            mListener.getEventListenerBase().hide(link);
                                            controllerLinks.reshowLastHiddenLink();
                                        });
                        snackbar.getView().setBackgroundColor(themer.getColorPrimary());
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

        textSidebar.setMovementMethod(LinkMovementMethod.getInstance());

        buttonSubscribe.setOnClickListener(v -> controllerLinks.subscribe());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        ControllerLinks.EventHolder eventHolder = controllerLinks.getEventHolder();
        subscriptionData = eventHolder.getData()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new ActionLog<>(TAG))
                .doOnNext(linksModel -> {
                    adapterLinkGrid.setData(linksModel);
                    adapterLinkList.setData(linksModel);
                })
                .doOnNext(linksModel -> textEmpty.setVisibility(linksModel.getLinks().isEmpty() && TextUtils.isEmpty(linksModel.getSubreddit().getName()) ? View.VISIBLE : View.GONE))
                .map(LinksModel::getSubreddit)
                .subscribe(subreddit -> {
                    this.subreddit = subreddit;

                    layoutDrawer.setDrawerLockMode(TextUtils.isEmpty(subreddit.getDescription())
                            ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                            : DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);

                    toolbar.setTitle(TextUtils.isEmpty(subreddit.getDisplayName()) ? Reddit.FRONT_PAGE : "/r/" + subreddit.getDisplayName());
                    textSidebar.setText(UtilsReddit.getFormattedHtml(subreddit.getDescriptionHtml()));
                    buttonSubscribe.setText(subreddit.isUserIsSubscriber() ? R.string.subscribe : R.string.unsubscribe);
                    buttonSubscribe.setVisibility(controllerUser.hasUser() ? View.VISIBLE : View.GONE);
                });

        subscriptionLoading = eventHolder.getLoading()
                .doOnEach(notification -> {
                    Log.d(TAG, "call() called with: notification = [" + notification + "]");
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(swipeRefreshThreadList::setRefreshing);

        subscriptionSort = eventHolder.getSort()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sort -> menu.findItem(sort.getMenuId()).setChecked(true));

        subscriptionTime = eventHolder.getTime()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(time -> {
                    menu.findItem(time.getMenuId()).setChecked(true);
                    itemSortTime.setTitle(getString(R.string.time_description, menu.findItem(time.getMenuId()).toString()));
                });

        subscriptionErrors = eventHolder.getErrors()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(linksError -> {
                    switch (linksError) {
                        case REPORT:
                            break;
                        case SAVE:
                            Toast.makeText(getContext(), R.string.error_saving_post, Toast.LENGTH_SHORT).show();
                            break;
                        case UNSAVE:
                            Toast.makeText(getContext(), R.string.error_unsaving_post, Toast.LENGTH_SHORT).show();
                            break;
                        case MARK_NSFW:
                            Toast.makeText(getContext(), R.string.error_marking_nsfw, Toast.LENGTH_SHORT).show();
                            break;
                        case UNMARK_NSFW:
                            Toast.makeText(getContext(), R.string.error_unmarking_nsfw, Toast.LENGTH_SHORT).show();
                            break;
                    }
                });

        controllerUser.addListener(listenerUser);
    }

    @Override
    public void onStop() {
        UtilsRx.unsubscribe(subscriptionData);
        UtilsRx.unsubscribe(subscriptionLoading);
        UtilsRx.unsubscribe(subscriptionSort);
        UtilsRx.unsubscribe(subscriptionTime);
        UtilsRx.unsubscribe(subscriptionErrors);
        controllerUser.removeListener(listenerUser);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        adapterLink.destroyViewHolders();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void setUpToolbar() {
        toolbar = UtilsTheme.generateToolbar(getContext(), layoutAppBar, themer, mListener);
        toolbar.setOnClickListener(v ->
                getFragmentManager().beginTransaction()
                        .hide(FragmentThreadList.this)
                        .add(R.id.frame_fragment, FragmentSearch.newInstance(true),
                                FragmentSearch.TAG)
                        .addToBackStack(null)
                        .commit());

        if (getFragmentManager().getBackStackEntryCount() <= 1) {
            toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
            toolbar.setNavigationOnClickListener(v -> mListener.openDrawer());
        }
        else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            toolbar.setNavigationOnClickListener(v -> mListener.onNavigationBackClick());
        }

        toolbar.getNavigationIcon().mutate().setColorFilter(themer.getColorFilterPrimary());
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

        UtilsColor.tintMenu(menu, themer.getColorFilterPrimary());
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
                        subreddit.getTitle(),
                        postType,
                        subreddit.getSubmitTextHtml());

                getFragmentManager().beginTransaction()
                        .hide(FragmentThreadList.this)
                        .add(R.id.frame_fragment, fragmentNewPost, FragmentNewPost.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void showSidebar() {
                layoutDrawer.openDrawer(GravityCompat.END);
            }
        };

        listenerUser = new ControllerUser.Listener() {
            @Override
            public void onUserLoaded(@Nullable User user) {
                if (user == null) {
                    buttonSubscribe.setVisibility(View.GONE);
                }
                else {
                    controllerLinks.reload();
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
                                buttonExpandActions.setColorFilter(themer.getColorFilterAccent());
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
                item.getIcon().setColorFilter(themer.getColorFilterPrimary());
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
            itemSortTime.setTitle(getString(R.string.time_description, item.toString()));
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
        Log.d(TAG, "setVisibilityOfThing() called with: visibility = [" + visibility + "], thing = [" + thing + "]");
        adapterLink.setVisibility(visibility, thing);
    }

    @Override
    public void navigateBack() {
        if (layoutDrawer.isDrawerOpen(GravityCompat.END)) {
            layoutDrawer.closeDrawer(GravityCompat.END);
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