package com.winsonchiu.reader;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Subreddit;

public class FragmentComments extends Fragment {

    public static final String TAG = FragmentComments.class.getCanonicalName();

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_IS_GRID = "isGrid";
    private static final String ARG_COLOR_LINK = "colorLink";
    private static final String ARG_START_X = "startX";
    private static final String ARG_START_Y = "startY";
    private static final String ARG_ITEM_HEIGHT = "itemHeight";
    private static final String ARG_SPANS_FILLED = "spansFilled";
    private static final String ARG_SPAN_COUNT = "spanCount";
    private static final long DURATION_COMMENTS_FADE = 300;
    private static final long DURATION_ACTIONS_FADE = 150;
    private static final float OFFSET_MODIFIER = 0.25f;

    // TODO: Rename and change types of parameters
    private String subreddit;
    private String linkId;

    private FragmentListenerBase mListener;
    private RecyclerView recyclerCommentList;
    private Activity activity;
    private LinearLayoutManager linearLayoutManager;
    private AdapterCommentList adapterCommentList;
    private SwipeRefreshLayout swipeRefreshCommentList;
    private ControllerComments.CommentClickListener listener;
    private RecyclerView.ViewHolder viewHolder;
    private Toolbar toolbar;
    private LinearLayout layoutActions;
    private FloatingActionButton buttonExpandActions;
    private FloatingActionButton buttonJumpTop;
    private FloatingActionButton buttonCommentPrevious;
    private FloatingActionButton buttonCommentNext;
    private ScrollAwareFloatingActionButtonBehavior behaviorFloatingActionButton;
    private YouTubePlayerView viewYouTube;
    private YouTubePlayer youTubePlayer;
    private RecyclerView.AdapterDataObserver observer;
    private AccelerateInterpolator accelerateInterpolator;
    private DecelerateInterpolator decelerateInterpolator;
    private Fragment fragmentToHide;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentComments.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentComments newInstance(String param1,
            String param2,
            boolean isGrid,
            int colorLink,
            float startX,
            float startY,
            int itemHeight) {
        FragmentComments fragment = new FragmentComments();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putBoolean(ARG_IS_GRID, isGrid);
        args.putInt(ARG_COLOR_LINK, colorLink);
        args.putFloat(ARG_START_X, startX);
        args.putFloat(ARG_START_Y, startY);
        args.putInt(ARG_ITEM_HEIGHT, itemHeight);
        fragment.setArguments(args);
        return fragment;
    }

    public static FragmentComments newInstance(RecyclerView.ViewHolder viewHolder, int colorLink, int spanCount) {
        FragmentComments fragment = new FragmentComments();
        Bundle args = new Bundle();
        if (viewHolder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            int spans = ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).isFullSpan() ? spanCount : 1;
            args.putBoolean(ARG_IS_GRID, true);
            args.putInt(ARG_SPANS_FILLED, spans);
            args.putInt(ARG_SPAN_COUNT, spanCount);
        }
        args.putInt(ARG_COLOR_LINK, colorLink);
        args.putFloat(ARG_START_X, viewHolder.itemView.getX());
        args.putFloat(ARG_START_Y, viewHolder.itemView.getY());
        args.putInt(ARG_ITEM_HEIGHT, viewHolder.itemView.getHeight());
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentComments() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            subreddit = getArguments().getString(ARG_PARAM1);
            linkId = getArguments().getString(ARG_PARAM2);
        }
        accelerateInterpolator = new AccelerateInterpolator();
        decelerateInterpolator = new DecelerateInterpolator();
        setHasOptionsMenu(true);
    }

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_comments);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_comments, container, false);

        listener = new ControllerComments.CommentClickListener() {
            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerCommentList.requestDisallowInterceptTouchEvent(disallow);
                swipeRefreshCommentList.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }

            @Override
            public void loadUrl(String url) {
                getFragmentManager().beginTransaction()
                        .hide(FragmentComments.this)
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(url, ""), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshCommentList.setRefreshing(refreshing);
            }

            @Override
            public AdapterCommentList getAdapter() {
                return adapterCommentList;
            }

            @Override
            public void notifyDataSetChanged() {
                if (adapterCommentList.isAnimationFinished()) {
                    adapterCommentList.notifyDataSetChanged();
                }
            }

            @Override
            public int getRecyclerHeight() {
                return recyclerCommentList.getHeight();
            }

            @Override
            public int getRecyclerWidth() {
                return recyclerCommentList.getWidth();
            }

            @Override
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
            }

            @Override
            public void loadYouTube(final Link link, final String id, final AdapterLink.ViewHolderBase viewHolderBase) {
                if (youTubePlayer != null) {
                    viewYouTube.setVisibility(View.VISIBLE);
                    return;
                }

                viewYouTube.initialize(ApiKeys.YOUTUBE_API_KEY,
                        new YouTubePlayer.OnInitializedListener() {
                            @Override
                            public void onInitializationSuccess(YouTubePlayer.Provider provider,
                                    YouTubePlayer player,
                                    boolean b) {
                                FragmentComments.this.youTubePlayer = player;
                                youTubePlayer.setManageAudioFocus(false);
                                youTubePlayer.setFullscreenControlFlags(YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI);

                                DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();

                                if (displayMetrics.widthPixels > displayMetrics.heightPixels){
                                    youTubePlayer.setOnFullscreenListener(
                                            new YouTubePlayer.OnFullscreenListener() {
                                                @Override
                                                public void onFullscreen(boolean fullscreen) {
                                                    Log.d(TAG, "fullscreen: " + fullscreen);
                                                    if (!fullscreen) {
                                                        youTubePlayer.pause();
                                                        youTubePlayer.release();
                                                        youTubePlayer = null;
                                                        viewYouTube.setVisibility(View.GONE);
                                                    }
                                                }
                                            });
                                    youTubePlayer.setPlayerStateChangeListener(
                                            new YouTubePlayer.PlayerStateChangeListener() {
                                                @Override
                                                public void onLoading() {

                                                }

                                                @Override
                                                public void onLoaded(String s) {
                                                }

                                                @Override
                                                public void onAdStarted() {

                                                }

                                                @Override
                                                public void onVideoStarted() {
                                                    youTubePlayer.setFullscreen(true);

                                                }

                                                @Override
                                                public void onVideoEnded() {

                                                }

                                                @Override
                                                public void onError(YouTubePlayer.ErrorReason errorReason) {

                                                }
                                            });
                                }
                                viewYouTube.setVisibility(View.VISIBLE);
                                youTubePlayer.loadVideo(id);
                            }

                            @Override
                            public void onInitializationFailure(YouTubePlayer.Provider provider,
                                    YouTubeInitializationResult youTubeInitializationResult) {
                                viewHolderBase.attemptLoadImage(link);
                            }
                        });
            }

            @Override
            public boolean hideYouTube() {
                if (viewYouTube.isShown()) {
                    if (youTubePlayer != null) {
                        youTubePlayer.pause();
                    }
                    viewYouTube.setVisibility(View.GONE);
                    return false;
                }

                return true;
            }

        };

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayoutManager.scrollToPositionWithOffset(0, 0);
            }
        });
        toolbar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity, "Return to top", Toast.LENGTH_LONG).show();
                return true;
            }
        });
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNavigationBackClick();
            }
        });
        setUpOptionsMenu();

        layoutActions = (LinearLayout) view.findViewById(R.id.layout_actions);
        buttonExpandActions = (FloatingActionButton) view.findViewById(R.id.button_expand_actions);
        buttonExpandActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonExpandActions.setImageResource(buttonCommentPrevious.isShown() ? R.drawable.ic_unfold_more_white_24dp : android.R.color.transparent);
                toggleLayoutActions();
            }
        });

        behaviorFloatingActionButton = new ScrollAwareFloatingActionButtonBehavior(activity, null,
                new ScrollAwareFloatingActionButtonBehavior.OnVisibilityChangeListener() {
                    @Override
                    public void onStartHideFromScroll() {
                        hideLayoutActions(0);
                    }

                    @Override
                    public void onEndHideFromScroll() {
                        buttonExpandActions.setImageResource(R.drawable.ic_unfold_more_white_24dp);
                    }

                });
        ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams()).setBehavior(behaviorFloatingActionButton);

        buttonJumpTop = (FloatingActionButton) view.findViewById(R.id.button_jump_top);
        buttonJumpTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayoutManager.scrollToPositionWithOffset(0, 0);
            }
        });

        buttonCommentPrevious = (FloatingActionButton) view.findViewById(R.id.button_comment_previous);
        buttonCommentPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = linearLayoutManager.findFirstVisibleItemPosition();
                if (position == 1) {
                    linearLayoutManager.scrollToPositionWithOffset(0, 0);
                    return;
                }
                linearLayoutManager.scrollToPositionWithOffset(mListener.getControllerComments().getPreviousCommentPosition(
                        position - 1) + 1, 0);
            }
        });

        buttonCommentNext = (FloatingActionButton) view.findViewById(R.id.button_comment_next);
        buttonCommentNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = linearLayoutManager.findFirstVisibleItemPosition();
                if (position == 0) {
                    if (adapterCommentList.getItemCount() > 0) {
                        linearLayoutManager.scrollToPositionWithOffset(1, 0);
                    }
                    return;
                }
                linearLayoutManager.scrollToPositionWithOffset(mListener.getControllerComments()
                        .getNextCommentPosition(position - 1) + 1, 0);
            }
        });

        viewYouTube = (YouTubePlayerView) view.findViewById(R.id.youtube);

        swipeRefreshCommentList = (SwipeRefreshLayout) view.findViewById(
                R.id.swipe_refresh_comment_list);
        swipeRefreshCommentList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.getControllerComments().reloadAllComments();
            }
        });

        linearLayoutManager = new LinearLayoutManager(activity);
        recyclerCommentList = (RecyclerView) view.findViewById(R.id.recycler_comment_list);
        recyclerCommentList.setLayoutManager(linearLayoutManager);
        recyclerCommentList.setItemAnimator(null);

        if (adapterCommentList == null) {
            adapterCommentList = new AdapterCommentList(activity, mListener.getControllerComments(), listener,
                    getArguments().getBoolean(ARG_IS_GRID, false), getArguments().getInt(ARG_COLOR_LINK),
                    new ControllerLinks.LinkClickListener() {
                        @Override
                        public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                            recyclerCommentList.requestDisallowInterceptTouchEvent(disallow);
                            swipeRefreshCommentList.requestDisallowInterceptTouchEvent(disallow);
                        }

                        @Override
                        public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

                        }

                        @Override
                        public void onClickComments(Link link, RecyclerView.ViewHolder viewHolder) {

                        }

                        @Override
                        public void loadUrl(String url) {
                            listener.loadUrl(url);
                        }

                        @Override
                        public void onFullLoaded(int position) {

                        }

                        @Override
                        public void setRefreshing(boolean refreshing) {

                        }

                        @Override
                        public void setToolbarTitle(String title) {

                        }

                        @Override
                        public AdapterLink getAdapter() {
                            return null;
                        }

                        @Override
                        public int getRecyclerHeight() {
                            return recyclerCommentList.getHeight();
                        }

                        @Override
                        public void loadSideBar(Subreddit listingSubreddits) {

                        }

                        @Override
                        public void setEmptyView(boolean visible) {

                        }

                        @Override
                        public int getRecyclerWidth() {
                            return recyclerCommentList.getWidth();
                        }

                        @Override
                        public void onClickSubmit(String postType) {

                        }

                        @Override
                        public ControllerCommentsBase getControllerComments() {
                            return mListener.getControllerComments();
                        }

                        @Override
                        public void setSort(Sort sort) {

                        }

                        @Override
                        public void loadVideoLandscape(int position) {

                        }

                        @Override
                        public int getRequestedOrientation() {
                            return mListener.getRequestedOrientation();
                        }

                        @Override
                        public void showSidebar() {

                        }

                    });
        }

        observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                if (positionStart == 0 && youTubePlayer != null) {
                    youTubePlayer.release();
                    youTubePlayer = null;
                }
            }
        };

        adapterCommentList.registerAdapterDataObserver(observer);

        recyclerCommentList.setAdapter(adapterCommentList);
        mListener.getControllerComments().addListener(listener);


        final int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int viewHolderWidth = screenWidth / getArguments().getInt(ARG_SPAN_COUNT, 1) * getArguments().getInt(ARG_SPANS_FILLED, 1);
        float speed = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.1f, getResources().getDisplayMetrics());
        final float startX = getArguments().getFloat(ARG_START_X, 0);
        final float startY = getArguments().getFloat(ARG_START_Y, 0);
        final int startMarginEnd = (int) (screenWidth - startX - viewHolderWidth);
        long duration = 250;//(long) Math.abs(startY / speed);

        Log.d(TAG, "startX: " + startX);
        Log.d(TAG, "startY: " + startY);

        final View viewBackground = view.findViewById(R.id.view_background);

        viewBackground.setScaleY(0f);
        viewBackground.setPivotY(startY + getArguments().getInt(ARG_ITEM_HEIGHT, 0));
        final ViewPropertyAnimator viewPropertyAnimatorBackground = viewBackground.animate()
                .scaleY(2f)
                .setDuration(duration)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewBackground.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                viewBackground.setVisibility(View.GONE);
                            }
                        }, 250);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });

        final Animation animation = new Animation() {
            @Override
            public boolean willChangeBounds() {
                return true;
            }

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) swipeRefreshCommentList.getLayoutParams();
                float reverseInterpolation = 1.0f - interpolatedTime;
                layoutParams.topMargin = (int) (startY * reverseInterpolation);
                layoutParams.setMarginStart((int) (startX * reverseInterpolation));
                layoutParams.setMarginEnd((int) (startMarginEnd * reverseInterpolation));
                swipeRefreshCommentList.setLayoutParams(layoutParams);
            }
        };
        animation.setDuration(duration);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                    if (fragmentToHide != null) {
                        getFragmentManager().beginTransaction().hide(fragmentToHide).commit();
                        fragmentToHide = null;
                    }
                    swipeRefreshCommentList.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            adapterCommentList.setAnimationFinished(true);
                            adapterCommentList.notifyDataSetChanged();
                        }
                    }, 150);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        swipeRefreshCommentList.setVisibility(View.GONE);

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) swipeRefreshCommentList.getLayoutParams();
        layoutParams.topMargin = (int) startY;
        layoutParams.setMarginStart((int) startX);
        layoutParams.setMarginEnd(startMarginEnd);
        swipeRefreshCommentList.setLayoutParams(layoutParams);

        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);

                swipeRefreshCommentList.setVisibility(View.VISIBLE);
                view.startAnimation(animation);

                viewPropertyAnimatorBackground.start();
                return true;
            }
        });

        return view;
    }

    private void toggleLayoutActions() {
        if (buttonCommentPrevious.isShown()) {
            hideLayoutActions(DURATION_ACTIONS_FADE);
        }
        else {
            showLayoutActions();
        }
    }

    private void showLayoutActions() {

        for (int index = layoutActions.getChildCount() - 1; index >= 0; index--) {
            final View view = layoutActions.getChildAt(index);
            AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            alphaAnimation.setInterpolator(decelerateInterpolator);
            alphaAnimation.setDuration(DURATION_ACTIONS_FADE);
            alphaAnimation.setStartOffset(
                    (long) ((layoutActions.getChildCount() - 1 - index) * DURATION_ACTIONS_FADE * OFFSET_MODIFIER));
            view.startAnimation(alphaAnimation);
        }

    }

    private void hideLayoutActions(long offset) {
        for (int index = 0; index < layoutActions.getChildCount(); index++) {
            final View view = layoutActions.getChildAt(index);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1f, 0f);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            alphaAnimation.setInterpolator(accelerateInterpolator);
            alphaAnimation.setDuration(DURATION_ACTIONS_FADE);
            alphaAnimation.setStartOffset((long) (index * offset * OFFSET_MODIFIER));
            view.startAnimation(alphaAnimation);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");

        swipeRefreshCommentList.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshCommentList.setRefreshing(true);
                mListener.getControllerComments()
                        .reloadAllComments();

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        mListener.getControllerComments().addListener(listener);
    }

    @Override
    public void onStop() {
        adapterCommentList.destroyViewHolderLink();
        mListener.getControllerComments().removeListener(listener);
        super.onStop();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
        this.activity = activity;
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
        mListener = null;
        activity = null;
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        adapterCommentList.unregisterAdapterDataObserver(observer);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (youTubePlayer != null) {
            youTubePlayer.release();
            youTubePlayer = null;
        }
        super.onDestroy();
//        CustomApplication.getRefWatcher(getActivity())
//                .watch(this);
    }

    public void setFragmentToHide(Fragment fragmentToHide) {
        this.fragmentToHide = fragmentToHide;
    }
}