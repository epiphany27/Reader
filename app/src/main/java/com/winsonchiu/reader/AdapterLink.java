package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.AnimationUtils;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public abstract class AdapterLink extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterLink.class.getCanonicalName();
    protected Activity activity;
    protected LayoutManager layoutManager;
    protected ControllerLinks controllerLinks;
    protected int colorPositive;
    protected int colorNegative;
    protected ControllerLinks.LinkClickListener listener;

    public void setActivity(Activity activity) {
        Resources resources = activity.getResources();
        this.activity = activity;
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
    }

    public void setControllerLinks(ControllerLinks controllerLinks, ControllerLinks.LinkClickListener listener) {
        this.controllerLinks = controllerLinks;
        this.listener = listener;
    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    public abstract RecyclerView.ItemDecoration getItemDecoration();

    protected class ViewHolderBase extends RecyclerView.ViewHolder {

        protected MediaController mediaController;
        protected ProgressBar progressImage;
        protected ViewPager viewPagerFull;
        protected ImageView imagePlay;
        protected ImageView imagePreview;
        protected VideoView videoFull;
        protected WebViewFixed webFull;
        protected Toolbar toolbarActionsFull;
        protected TextView textThreadTitle;
        protected TextView textThreadSelf;
        protected TextView textThreadInfo;
        protected ImageButton buttonComments;
        protected Toolbar toolbarActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolderBase(final View itemView) {
            super(itemView);

            progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            imagePlay = (ImageView) itemView.findViewById(R.id.image_play);
            mediaController = new MediaController(itemView.getContext());
            videoFull = (VideoView) itemView.findViewById(R.id.video_full);
            videoFull.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaController.hide();
                }
            });
            mediaController.setAnchorView(videoFull);
            videoFull.setMediaController(mediaController);
            webFull = (WebViewFixed) itemView.findViewById(R.id.web_full);
            webFull.getSettings().setUseWideViewPort(true);
            webFull.getSettings().setBuiltInZoomControls(true);
            webFull.getSettings().setDisplayZoomControls(false);
            webFull.getSettings().setDomStorageEnabled(true);
            webFull.setBackgroundColor(0x000000);
            webFull.setWebViewClient(new WebViewClient() {
                @Override
                public void onScaleChanged(WebView view, float oldScale, float newScale) {
                    webFull.lockHeight();
                    super.onScaleChanged(view, oldScale, newScale);
                }
            });
            webFull.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {

                        if ((webFull.canScrollVertically(1) && webFull.canScrollVertically(-1))) {
                            listener
                                    .requestDisallowInterceptTouchEvent(true);
                        }
                        else {
                            listener
                                    .requestDisallowInterceptTouchEvent(false);
                            if (webFull.getScrollY() == 0) {
                                webFull.setScrollY(1);
                            }
                            else {
                                webFull.setScrollY(webFull.getScrollY() - 1);
                            }
                        }
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        listener
                                .requestDisallowInterceptTouchEvent(false);
                    }

                    return false;
                }
            });
            viewPagerFull = (ViewPager) itemView.findViewById(R.id.view_pager_full);
            toolbarActionsFull = (Toolbar) itemView.findViewById(R.id.toolbar_actions_full);
            toolbarActionsFull.inflateMenu(R.menu.menu_link_full);
            toolbarActionsFull.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.item_web:
                            listener.loadUrl(controllerLinks.getLink(getPosition()).getUrl());
                            break;
                    }
                    return true;
                }
            });
            imagePreview = (ImageView) itemView.findViewById(R.id.image_preview);
            textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            textThreadSelf = (TextView) itemView.findViewById(R.id.text_thread_self);
            textThreadSelf.setMovementMethod(LinkMovementMethod.getInstance());
            buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickComments(controllerLinks.getLink(getPosition()));
                }
            });
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_link);
            toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.item_upvote:
                            controllerLinks.vote(ViewHolderBase.this, 1);
                            break;
                        case R.id.item_downvote:
                            controllerLinks.vote(ViewHolderBase.this, -1);
                            break;
                        case R.id.item_share:
                            break;
                    }
                    return true;
                }
            });
        }

        public void loadFull(Link link ) {

            Log.d(TAG, "loadFull: " + link.getUrl());

            String url = link.getUrl();
            if (!TextUtils.isEmpty(url)) {
                if (link.getDomain()
                        .contains("imgur")) {
                    int startIndex;
                    int lastIndex;
                    if (url.contains("imgur.com/a/")) {
                        startIndex = url.indexOf("imgur.com/a/") + 12;
                        int slashIndex = url.substring(startIndex)
                                .indexOf("/") + startIndex;
                        lastIndex = slashIndex > startIndex ? slashIndex : url.length();
                        String imgurId = url.substring(startIndex, lastIndex);
                        loadAlbum(imgurId);
                    }
                    else if (url.contains(Reddit.GIFV)) {
                        startIndex = url.indexOf("imgur.com/") + 10;
                        int dotIndex = url.substring(startIndex)
                                .indexOf(".") + startIndex;
                        lastIndex = dotIndex > startIndex ? dotIndex : url.length();
                        String imgurId = url.substring(startIndex, lastIndex);
                        loadGifv(imgurId);
                    }
                    else {
                        attemptLoadImage(link);
                    }
                }
                else if (link.getDomain()
                        .contains("gfycat")) {
                    int startIndex = url.indexOf("gfycat.com/") + 11;
                    int dotIndex = url.substring(startIndex)
                            .indexOf(".");
                    int lastIndex = dotIndex > startIndex ? dotIndex : url.length();
                    String gfycatId = url.substring(startIndex, lastIndex);
                    progressImage.setVisibility(View.VISIBLE);
                    controllerLinks.getReddit()
                            .loadGet(Reddit.GFYCAT_URL + gfycatId,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(
                                                        response).getJSONObject(Reddit.GFYCAT_ITEM);
                                                loadVideo(jsonObject.getString(Reddit.GFYCAT_WEBM),
                                                        (float) jsonObject.getInt(
                                                                "height") / jsonObject.getInt(
                                                                "width"));
                                            }
                                            catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            finally {
                                                progressImage.setVisibility(View.GONE);
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            progressImage.setVisibility(View.GONE);
                                        }
                                    }, 0);
                }
                else {
                    attemptLoadImage(link);
                }
            }
        }

        public void setVoteColors() {

            Link link = controllerLinks.getLink(getPosition());
            switch (link.isLikes()) {
                case 1:
                    toolbarActions.getMenu().findItem(R.id.item_upvote).getIcon().setColorFilter(
                            colorPositive, PorterDuff.Mode.MULTIPLY);
                    toolbarActions.getMenu().findItem(R.id.item_downvote).getIcon().clearColorFilter();
                    break;
                case -1:
                    toolbarActions.getMenu().findItem(R.id.item_downvote).getIcon().setColorFilter(
                            colorNegative, PorterDuff.Mode.MULTIPLY);
                    toolbarActions.getMenu().findItem(R.id.item_upvote).getIcon().clearColorFilter();
                    break;
                case 0:
                    toolbarActions.getMenu().findItem(R.id.item_upvote).getIcon().clearColorFilter();
                    toolbarActions.getMenu().findItem(R.id.item_downvote).getIcon().clearColorFilter();
                    break;
            }
        }

        public void setTextInfo() {
            Link link = controllerLinks.getLink(getPosition());

            String subreddit = "/r/" + link.getSubreddit();
            Spannable spannableInfo = new SpannableString(subreddit + "\n" + link.getScore() + " by " + link.getAuthor());
            spannableInfo.setSpan(
                    new ForegroundColorSpan(link.getScore() > 0 ? colorPositive : colorNegative),
                    subreddit.length() + 1,
                    subreddit.length() + 1 + String.valueOf(link.getScore())
                            .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textThreadInfo.setText(spannableInfo);
        }


        private void attemptLoadImage(Link link) {

            if (Reddit.placeImageUrl(link)) {
                webFull.onResume();
                webFull.resetMaxHeight();
                webFull.loadData(Reddit.getImageHtml(
                        controllerLinks.getLink(getPosition())
                                .getUrl()), "text/html", "UTF-8");
                webFull.setVisibility(View.VISIBLE);
                toolbarActionsFull.setVisibility(View.VISIBLE);
                listener
                        .onFullLoaded(getPosition());
            }
            else {
                listener.loadUrl(link.getUrl());
            }
        }


        private void loadAlbum(String id) {
            progressImage.setVisibility(View.VISIBLE);
            imagePreview.setTag(controllerLinks.getReddit()
                    .loadImgurAlbum(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Log.d(TAG, "loadAlbum: " + response);
                                        Album album = Album.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        toolbarActionsFull.setVisibility(View.VISIBLE);
                                        viewPagerFull.setAdapter(
                                                new AdapterAlbum(activity, album,
                                                        listener));
                                        viewPagerFull.getLayoutParams().height = listener
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
                                        viewPagerFull.requestLayout();
                                        listener.onFullLoaded(getPosition());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } finally {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    progressImage.setVisibility(View.GONE);
                                }
                            }, 0));
        }


        private void loadGifv(String id) {
            Log.d(TAG, "loadGifv: " + id);
            imagePreview.setTag(controllerLinks.getReddit()
                    .loadImgurImage(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Image image = Image.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        if (!TextUtils.isEmpty(image.getMp4())) {
                                            loadVideo(image.getMp4(), (float) image.getHeight() / image.getWidth());
                                        }
                                        else if (!TextUtils.isEmpty(image.getWebm())) {
                                            loadVideo(image.getWebm(), (float) image.getHeight() / image.getWidth());
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } finally {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    try {
                                        Log.d(TAG, "onErrorResponse");
                                        Log.d(TAG, "" + error.networkResponse.statusCode);
                                        Log.d(TAG, error.networkResponse.headers.toString());
                                        Log.d(TAG, new String(error.networkResponse.data));
                                    }
                                    catch (Throwable e) {

                                    }
                                    Log.d(TAG, "error on loadGifv");
                                    progressImage.setVisibility(View.GONE);
                                }
                            }, 0));
        }


        private void loadVideo(String url, float heightRatio) {
            Log.d(TAG, "loadVideo: " + url + " : " + heightRatio);
            Uri uri = Uri.parse(url);
            toolbarActionsFull.setVisibility(View.VISIBLE);
            videoFull.setVideoURI(uri);
            videoFull.getLayoutParams().height = (int) (ViewHolderBase.this.itemView.getWidth() * heightRatio);
            videoFull.setVisibility(View.VISIBLE);
            videoFull.invalidate();
            videoFull.start();
            videoFull.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            videoFull.start();
                        }
                    });
            listener
                    .onFullLoaded(getPosition());
        }
    }

}
