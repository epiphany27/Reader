/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.YouTubeListener;

import javax.inject.Inject;

/**
 * Created by TheKeeperOfPie on 12/29/2015.
 */
public class AdapterLinkHeader extends RecyclerView.Adapter<AdapterLink.ViewHolderBase> {

    private final int thumbnailSize;
    private boolean isGrid;
    private int colorLink;
    private boolean actionsExpanded;
    private AdapterLink.ViewHolderBase viewHolderLink;
    private final AdapterLink.ViewHolderBase.EventListener eventListenerBase;
    private final DisallowListener disallowListener;
    private final RecyclerCallback recyclerCallback;
    private final YouTubeListener youTubeListener;
    private boolean animationFinished;

    @Inject ControllerComments controllerComments;

    public AdapterLinkHeader(Activity activity,
                             AdapterLink.ViewHolderBase.EventListener eventListenerBase,
                             DisallowListener disallowListener, RecyclerCallback recyclerCallback,
                             YouTubeListener youTubeListener,
                             boolean isGrid,
                             int colorLink,
                             boolean actionsExpanded) {
        this.eventListenerBase = eventListenerBase;
        this.disallowListener = disallowListener;
        this.recyclerCallback = recyclerCallback;
        this.youTubeListener = youTubeListener;
        this.isGrid = isGrid;
        this.colorLink = colorLink;
        this.actionsExpanded = actionsExpanded;
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        thumbnailSize = displayMetrics.widthPixels / 2;

        ((ActivityMain) activity).getComponentActivity().inject(this);
    }

    @Override
    public AdapterLink.ViewHolderBase onCreateViewHolder(ViewGroup parent, int viewType) {
        if (isGrid) {
            viewHolderLink = new AdapterLinkGrid.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_link, parent, false),
                    eventListenerBase,
                    disallowListener,
                    recyclerCallback,
                    thumbnailSize) {

                @Override
                public Intent getShareIntent() {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
                    shareIntent
                            .putExtra(Intent.EXTRA_TEXT, Reddit.BASE_URL + link.getPermalink());
                    return shareIntent;
                }

                @Override
                public boolean isInHistory() {

                    return false;
                }

                @Override
                public void loadBackgroundColor() {
                    if (colorLink != 0) {
                        link.setBackgroundColor(colorLink);
                        itemView.setBackgroundColor(colorLink);
                        setTextColors(colorLink);
                    } else {
                        super.loadBackgroundColor();
                    }
                }

                @Override
                public void onBind(Link link,
                                   boolean showSubreddit) {
                    super.onBind(link, showSubreddit);
                    if (actionsExpanded) {
                        setToolbarMenuVisibility();
                        showToolbarActionsInstant();
                    }
                    if (animationFinished) {
                        if (!TextUtils.isEmpty(link.getSelfText())) {
                            loadSelfText();
                        }
                    }
                }

                @Override
                public void onRecycle() {
                    super.onRecycle();
                    actionsExpanded = false;
                }

                @Override
                public void loadComments() {
                    controllerComments.loadLinkComments();
                }

                @Override
                public void onClickThumbnail() {
                    if (youTubeListener.hideYouTube()) {
                        super.onClickThumbnail();
                    }
                }
            };

            if (colorLink != 0) {
                viewHolderLink.itemView.setBackgroundColor(colorLink);
            }
        }
        else {
            viewHolderLink = new AdapterLinkList.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_link, parent, false),
                    eventListenerBase,
                    disallowListener,
                    recyclerCallback) {

                @Override
                public Intent getShareIntent() {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
                    shareIntent
                            .putExtra(Intent.EXTRA_TEXT, Reddit.BASE_URL + link.getPermalink());
                    return shareIntent;
                }

                @Override
                public boolean isInHistory() {
                    return false;
                }

                @Override
                public void onBind(Link link,
                                   boolean showSubreddit) {
                    super.onBind(link, showSubreddit);
                    if (actionsExpanded) {
                        setToolbarMenuVisibility();
                        showToolbarActionsInstant();
                    }
                    if (animationFinished) {
                        if (!TextUtils.isEmpty(link.getSelfText())) {
                            loadSelfText();
                        }
                    }
                }

                @Override
                public void onRecycle() {
                    super.onRecycle();
                    actionsExpanded = false;
                }

                @Override
                public void loadComments() {
                    controllerComments.loadLinkComments();
                }

                @Override
                public void onClickThumbnail() {
                    if (youTubeListener.hideYouTube()) {
                        super.onClickThumbnail();
                    }
                }
            };
        }

        viewHolderLink.setYouTubeListener(youTubeListener);

        return viewHolderLink;
    }

    @Override
    public void onBindViewHolder(AdapterLink.ViewHolderBase holder, int position) {
        holder.onBind(controllerComments.getLink(), true);
    }

    @Override
    public void onViewRecycled(AdapterLink.ViewHolderBase holder) {
        holder.onRecycle();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public void setAnimationFinished(boolean animationFinished) {
        this.animationFinished = animationFinished;
        notifyDataSetChanged();
    }

    public void collapseViewHolderLink(boolean expandActions) {
        if (controllerComments.getLink().isSelf()) {
            viewHolderLink.textThreadSelf.setVisibility(View.GONE);
        }
        else {
            viewHolderLink.destroyWebViews();
            viewHolderLink.onRecycle();
            viewHolderLink.onBind(controllerComments.getLink(),
                    controllerComments.showSubreddit());
        }

        if (expandActions) {
            actionsExpanded = true;
            viewHolderLink.showToolbarActionsInstant();
        } else {
            viewHolderLink.hideToolbarActionsInstant();
        }
    }

    public void recycle() {
        if (viewHolderLink != null) {
            viewHolderLink.onRecycle();
        }
    }

    public void destroyViewHolderLink() {
        if (viewHolderLink != null) {
            viewHolderLink.destroyWebViews();
        }
    }

    public void onTouchEvent(MotionEvent event) {
        viewHolderLink.itemView.dispatchTouchEvent(event);
    }

    public void onBind() {
        viewHolderLink.onBind(controllerComments.getLink(), true);
    }
}
