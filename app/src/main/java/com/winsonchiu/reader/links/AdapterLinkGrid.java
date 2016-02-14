/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.SpanLeadingMargin;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.utils.UtilsImage;

import java.util.ArrayList;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();
    private static final int ALPHA_OVERLAY = 140;
    private static final int ALPHA_OVERLAY_IMAGE = 200;
    protected int thumbnailSize;

    public AdapterLinkGrid(FragmentActivity activity,
            ControllerLinksBase controllerLinks,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderBase.EventListener eventListenerBase,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback) {
        super(activity, eventListenerHeader, eventListenerBase, disallowListener, recyclerCallback);
        setController(controllerLinks);
    }

    @Override
    public void setActivity(FragmentActivity activity) {
        super.setActivity(activity);

        Resources resources = activity.getResources();

        int spanCount = 0;

        try {
            spanCount = Integer.parseInt(preferences.getString(AppSettings.PREF_GRID_COLUMNS, String.valueOf(0)));
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (spanCount <= 0) {
            int columnThreshold = resources.getDimensionPixelSize(R.dimen.grid_column_width_threshold);
            int width = resources.getDisplayMetrics().widthPixels;
            int columns = width / columnThreshold;
            spanCount = Math.max(1, columns);
        }

        layoutManager = new StaggeredGridLayoutManager(spanCount,
                StaggeredGridLayoutManager.VERTICAL);
//        ((StaggeredGridLayoutManager) this.layoutManager).setGapStrategy(
//                StaggeredGridLayoutManager.GAP_HANDLING_NONE);

        DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        this.thumbnailSize = displayMetrics.widthPixels / 2;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.header_subreddit, viewGroup, false), eventListenerHeader);
        }

        return new ViewHolder(activity,
                LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cell_link, viewGroup, false),
                eventListenerBase,
                Source.LINKS,
                disallowListener,
                recyclerCallback,
                this,
                thumbnailSize);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        super.onBindViewHolder(holder, position);

        switch (getItemViewType(position)) {
            case VIEW_LINK_HEADER:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(controllerLinks.getSubreddit());
                break;
            case VIEW_LINK:
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.onBind(controllerLinks.getLink(position), controllerLinks.showSubreddit());
                break;
        }
    }

    public static class ViewHolder extends AdapterLink.ViewHolderBase {

        private final int thumbnailSize;
        protected ImageView imageFull;
        private int colorBackgroundDefault;
        private ValueAnimator valueAnimatorBackground;
        private float heightFlair;
        private float marginThumbnailHeight;
        private float marginThumbnailWidth;

        public ViewHolder(FragmentActivity activity,
                View itemView,
                EventListener eventListener,
                Source source,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback,
                CallbackYouTubeDestruction callbackYouTubeDestruction,
                int thumbnailSize) {
            super(activity, itemView, eventListener, source, disallowListener, recyclerCallback, callbackYouTubeDestruction);
            this.thumbnailSize = thumbnailSize;

        }

        @Override
        protected void initialize() {
            super.initialize();
            imageFull = (ImageView) itemView.findViewById(R.id.image_full);
            if (itemView.getBackground() instanceof ColorDrawable) {
                colorBackgroundDefault = ((ColorDrawable) itemView.getBackground()).getColor();
            }

            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            heightFlair = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, displayMetrics);
            marginThumbnailWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 52, displayMetrics);
            marginThumbnailHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, displayMetrics);
        }

        @Override
        protected void initializeListeners() {
            super.initializeListeners();
            imageFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imageFull.setVisibility(View.GONE);
                    progressImage.setVisibility(View.GONE);
                    imagePlay.setVisibility(View.GONE);

                    loadFull();
                }
            });
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.view_margin:
                case R.id.text_thread_flair:
                case R.id.layout_title:
                case R.id.text_thread_title:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        destroySurfaceView();
                        imageFull.setVisibility(View.VISIBLE);
                        imagePlay.setVisibility(View.VISIBLE);
                    }
                    loadComments();
                    break;
                default:
                    super.onClick(v);
            }
        }

        @Override
        public void onBind(Link link, boolean showSubreddit) {

            super.onBind(link, showSubreddit);

            int position = getAdapterPosition();

            if (link.getBackgroundColor() == 0) {
                link.setBackgroundColor(colorBackgroundDefault);
            }

            if (viewOverlay.getVisibility() == View.GONE) {
                itemView.setBackgroundColor(link.getBackgroundColor());
                viewOverlay.setBackgroundColor(0x00000000);
            }
            else {
                itemView.setBackgroundColor(0x00000000);
                viewOverlay.setBackgroundColor(ColorUtils.setAlphaComponent(link.getBackgroundColor(), ALPHA_OVERLAY));
            }

            imagePlay.setColorFilter(colorFilterIconDefault);
            textThreadInfo.setTextColor(colorTextSecondaryDefault);
            textHidden.setTextColor(colorTextSecondaryDefault);

            imagePlay.setVisibility(View.GONE);

            Drawable drawable = UtilsImage.getDrawableForLink(itemView.getContext(), link);
            if (drawable != null) {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawable);
                showThumbnail(true);
            }
            else if (!preferences.getBoolean(AppSettings.PREF_SHOW_THUMBNAILS, true) ||
                    (link.isOver18() && !preferences
                            .getBoolean(AppSettings.PREF_NSFW_THUMBNAILS, true))) {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawableDefault);
                showThumbnail(true);
            }
            else if (UtilsImage.showThumbnail(link)) {
                loadThumbnail(link, position);
            }
            else {
                String thumbnail = UtilsImage.parseThumbnail(link);
                if (URLUtil.isNetworkUrl(thumbnail)) {
                    imageFull.setVisibility(View.GONE);
                    imageThumbnail.clearColorFilter();
                    showThumbnail(true);
                    picasso.load(thumbnail)
                            .tag(TAG_PICASSO)
                            .into(imageThumbnail);
                }
                else {
                    imageFull.setVisibility(View.GONE);
                    imageThumbnail.setColorFilter(colorFilterIconDefault);
                    imageThumbnail.setImageDrawable(drawableDefault);
                    showThumbnail(true);
                }
            }
        }

        @Override
        public void expandFull(boolean expand) {
            super.expandFull(expand);

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams())
                        .setFullSpan(expand);
            }
            if (expand) {
                if (recyclerCallback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                    ((StaggeredGridLayoutManager) recyclerCallback.getLayoutManager())
                            .invalidateSpanAssignments();
                }
                scrollToSelf();
            }
        }

        @Override
        protected void expandToolbarActions() {
            super.expandToolbarActions();
            setOverflowColorFilter();
        }

        private int getAdjustedThumbnailSize() {
            float modifier = Float.parseFloat(
                    preferences.getString(AppSettings.PREF_GRID_THUMBNAIL_SIZE, "0.75"));
            if (modifier > 0) {
                return (int) (thumbnailSize * modifier);
            }

            return itemView.getResources().getDisplayMetrics().widthPixels;
        }

        private void loadThumbnail(final Link link, final int position) {

            // TODO: Improve thumbnail loading logic

            imageFull.setVisibility(View.VISIBLE);
            showThumbnail(false);
            progressImage.setVisibility(View.VISIBLE);

            picasso.cancelRequest(imageFull);
            imageFull.setImageDrawable(null);

            final int size = getAdjustedThumbnailSize();

            String thumbnail = UtilsImage.parseThumbnail(link);

            Log.d(TAG, "loadThumbnail() called with: " + "thumbnail = [" + thumbnail + "], title = [" + link.getTitle() + "]");

            if (URLUtil.isNetworkUrl(thumbnail)) {
                picasso.load(thumbnail)
                        .tag(TAG_PICASSO)
                        .into(imageFull,
                                new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        loadBackgroundColor();

                                        if (position == getAdapterPosition()) {
                                            if (UtilsImage.placeImageUrl(link)) {
                                                picasso.load(link.getUrl())
                                                        .tag(TAG_PICASSO)
                                                        .resize(size, size)
                                                        .centerCrop()
                                                        .into(imageFull, new Callback() {
                                                            @Override
                                                            public void onSuccess() {
                                                                progressImage.setVisibility(
                                                                        View.GONE);
                                                            }

                                                            @Override
                                                            public void onError() {
                                                                progressImage.setVisibility(
                                                                        View.GONE);
                                                            }
                                                        });

                                            } else {
                                                if (link.getDomain().contains("imgur") && (link
                                                        .getUrl()
                                                        .contains(Reddit.IMGUR_PREFIX_ALBUM) || link
                                                        .getUrl()
                                                        .contains(Reddit.IMGUR_PREFIX_GALLERY))) {
                                                    imagePlay.setImageResource(
                                                            R.drawable.ic_photo_album_white_48dp);
                                                } else {
                                                    imagePlay.setImageResource(
                                                            R.drawable.ic_play_circle_outline_white_48dp);
                                                }

                                                imagePlay.setColorFilter(colorFilterMenuItem);
                                                imagePlay.setVisibility(View.VISIBLE);
                                                progressImage.setVisibility(View.GONE);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onError() {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                });
            }
            else if (UtilsImage.placeImageUrl(link)) {
                Log.d(TAG, "loadThumbnail() called with: " + "url = [" + link.getUrl() + "], title = [" + link.getTitle() + "]");
                picasso.load(link.getUrl())
                        .tag(TAG_PICASSO)
                        .resize(size, size)
                        .centerCrop()
                        .into(imageFull,
                                new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        loadBackgroundColor();

                                        if (position == getAdapterPosition()) {
                                            if (link.getDomain().contains("imgur") && (link
                                                    .getUrl()
                                                    .contains(Reddit.IMGUR_PREFIX_ALBUM) || link
                                                    .getUrl()
                                                    .contains(Reddit.IMGUR_PREFIX_GALLERY))) {
                                                imagePlay.setImageResource(
                                                        R.drawable.ic_photo_album_white_48dp);
                                                imagePlay.setColorFilter(colorFilterMenuItem);
                                                imagePlay.setVisibility(View.VISIBLE);
                                            }
                                        }

                                        progressImage.setVisibility(View.GONE);
                                    }

                                    @Override
                                    public void onError() {
                                        imageFull.setVisibility(View.GONE);
                                        showThumbnail(true);
                                        imageThumbnail.setColorFilter(colorFilterIconDefault);
                                        imageThumbnail.setImageDrawable(drawableDefault);
                                        progressImage.setVisibility(View.GONE);
                                    }
                                });
            }
            else {
                imageFull.setVisibility(View.GONE);
                showThumbnail(true);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawableDefault);
                progressImage.setVisibility(View.GONE);
            }

        }

        public void loadBackgroundColor() {
            if (link.getBackgroundColor() != colorBackgroundDefault) {
                syncBackgroundColor();
                return;
            }

            final Link linkSaved = link;

            final int position = getAdapterPosition();
            Drawable drawable = imageFull.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                Palette.from(((BitmapDrawable) drawable).getBitmap())
                        .generate(
                                new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette palette) {
                                        if (position == getAdapterPosition()) {
                                            linkSaved.setBackgroundColor(palette.getDarkVibrantColor(
                                                    palette.getMutedColor(colorBackgroundDefault)));
                                            syncBackgroundColor();
                                        }
                                    }
                                });
            }
        }

        public void syncBackgroundColor() {
            int color = link.getBackgroundColor();

            if (viewOverlay.getVisibility() == View.GONE) {
                int viewBackgroundColor = ((ColorDrawable) itemView.getBackground())
                        .getColor();
                if (link.getBackgroundColor() != viewBackgroundColor) {
                    valueAnimatorBackground = UtilsAnimation.animateBackgroundColor(
                            itemView,
                            viewBackgroundColor,
                            color);
                }

                setTextColors(color);
            }
            else {
                color = ColorUtils.setAlphaComponent(color, ALPHA_OVERLAY_IMAGE);
                int overlayBackgroundColor = ((ColorDrawable) viewOverlay.getBackground())
                        .getColor();

                itemView.setBackgroundColor(0x00000000);
                if (link.getBackgroundColor() != overlayBackgroundColor) {
                    valueAnimatorBackground = UtilsAnimation.animateBackgroundColor(
                            viewOverlay,
                            overlayBackgroundColor,
                            color);
                }

                titleTextColor = colorTextPrimaryDefault;
                colorTextSecondary = colorTextSecondaryDefault;
                syncTitleColor();
            }
        }

        public double calculateLuminance(int color) {
            return Math.sqrt(0.299f * Math.pow(Color.red(color) / 255f, 2) + 0.587f * Math.pow(Color.green(color) / 255f, 2) + 0.114f * Math.pow(Color.blue(color) / 255f, 2));
        }

        public void setTextColors(int color) {

            Menu menu = toolbarActions.getMenu();

            double contrast = UtilsColor.computeContrast(color, Color.WHITE);

            if (contrast > 3f) {
                imagePlay.setColorFilter(colorFilterIconLight);
                textThreadInfo.setTextColor(resources.getColor(R.color.darkThemeTextColorMuted));
                textHidden.setTextColor(resources.getColor(R.color.darkThemeTextColorMuted));
                colorTextSecondary = resources.getColor(R.color.darkThemeTextColorMuted);
                titleTextColorAlert = resources.getColor(R.color.textColorAlert);
                titleTextColor = resources.getColor(R.color.darkThemeTextColor);
                colorFilterMenuItem = colorFilterIconLight;

            }
            else {
                imagePlay.setColorFilter(colorFilterIconDark);
                textThreadInfo.setTextColor(resources.getColor(R.color.lightThemeTextColorMuted));
                textHidden.setTextColor(resources.getColor(R.color.lightThemeTextColorMuted));
                colorTextSecondary = resources.getColor(R.color.lightThemeTextColorMuted);
                titleTextColorAlert = resources.getColor(R.color.textColorAlertMuted);
                titleTextColor = resources.getColor(R.color.lightThemeTextColor);
                colorFilterMenuItem = colorFilterIconDark;
            }
            syncTitleColor();

            setOverflowColorFilter();

            for (int index = 0; index < menu.size(); index++) {
                menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterMenuItem);
            }
        }

        public void setOverflowColorFilter() {

            ArrayList<View> views = new ArrayList<>();
            toolbarActions.findViewsWithText(views, "toolbar_overflow_access",
                    View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);

            if (!views.isEmpty()) {
                ImageView imageOverflow = (ImageView) views.get(0);
                imageOverflow.setColorFilter(colorFilterMenuItem);
            }
        }

        @Override
        public float getRatio() {
            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                float width = itemView.getResources().getDisplayMetrics().widthPixels;

                return ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams())
                        .isFullSpan() ?
                        1f : itemView.getWidth() / width;
            }

            return 1f;
        }

        @Override
        public void setTextValues(Link link) {
            super.setTextValues(link);

            showThumbnail(imageThumbnail.getVisibility() == View.VISIBLE);

            textThreadInfo.setText(TextUtils
                    .concat(getSubredditString(), showSubreddit ? "\n" : "", getSpannableScore(),
                            "by ", link.getAuthor(), " ", getFlairString()));

            Linkify.addLinks(textThreadInfo, Linkify.WEB_URLS);

            textHidden.setText(getTimestamp() + ", " + link.getNumComments() + " comments");
        }

        @Override
        public void setAlbum(Link link, Album album) {
            super.setAlbum(link, album);
            showThumbnail(false);
        }

        private void showThumbnail(boolean show) {
            imageThumbnail.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show) {
                float heightLine = textThreadTitle.getPaint().getTextSize();
                float heightThumbnail = marginThumbnailHeight - (textThreadFlair.getVisibility() == View.VISIBLE ? heightFlair : 0);
                int lines = Math.max(1, (int) Math.ceil(heightThumbnail / heightLine));

                SpannableString text = new SpannableString(link.getTitle());
                text.setSpan(new SpanLeadingMargin(lines, marginThumbnailWidth), 0, lines, 0);
                textThreadTitle.setText(text);

                Log.d(TAG, "showThumbnail() called with: " + "lines = [" + lines + "], marginThumbnailWidth = [" + marginThumbnailWidth + "], title = [" + link.getTitle() + "]");

            }
            else {
                textThreadTitle.setText(link.getTitle());
            }
        }

        @Override
        public void onRecycle() {
            super.onRecycle();
            expandFull(false);
            if (valueAnimatorBackground != null) {
                valueAnimatorBackground.cancel();
            }
        }

        @Override
        public void clearOverlay() {
            itemView.setBackgroundColor(link.getBackgroundColor());
            setTextColors(link.getBackgroundColor());
            viewOverlay.setVisibility(View.GONE);
        }

        @Override
        public int[] getScreenAnchor() {
            int[] location = new int[2];
            if (imageFull.isShown()) {
                imageFull.getLocationOnScreen(location);
            }
            else {
                frameFull.getLocationOnScreen(location);
                location[1] += frameFull.getHeight();
                if (!imageThumbnail.isShown()) {
                    location[1] -= itemView.getWidth();
                }
            }

            return location;
        }
    }

}