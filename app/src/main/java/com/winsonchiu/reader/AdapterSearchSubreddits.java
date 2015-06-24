package com.winsonchiu.reader;

import android.app.Activity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;

import java.util.Date;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class AdapterSearchSubreddits extends RecyclerView.Adapter<AdapterSearchSubreddits.ViewHolder>
        implements ControllerSearch.ListenerCallback {

    private static final String TAG = AdapterSearchSubreddits.class.getCanonicalName();
    private RecyclerView.LayoutManager layoutManager;
    private Activity activity;
    private ControllerSearch controllerSubreddits;
    private ControllerSearch.Listener listener;

    public AdapterSearchSubreddits(Activity activity,
            ControllerSearch controllerSubreddits,
            ControllerSearch.Listener subredditListener) {
        this.activity = activity;
        this.controllerSubreddits = controllerSubreddits;
        this.listener = subredditListener;
        this.layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        Log.d(TAG, "AdapterSearchSubreddits created");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_subreddit, parent, false), this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.onBind(controllerSubreddits.getSubreddit(position));
    }

    @Override
    public int getItemCount() {
        return controllerSubreddits.getSubredditCount();
    }

    @Override
    public ControllerSearch.Listener getListener() {
        return listener;
    }

    @Override
    public ControllerSearch getController() {
        return controllerSubreddits;
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return layoutManager;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected TextView textName;
        protected TextView textTitle;
        protected TextView textDescription;
        protected TextView textInfo;
        protected ImageButton buttonOpen;
        protected RelativeLayout layoutContainerExpand;
        private ControllerSearch.ListenerCallback callback;

        public ViewHolder(View itemView, final ControllerSearch.ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;

            textName = (TextView) itemView.findViewById(R.id.text_name);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textDescription = (TextView) itemView.findViewById(R.id.text_description);
            textDescription.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(R.id.layout_container_expand);
            buttonOpen = (ImageButton) itemView.findViewById(R.id.button_open);

            buttonOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.getListener().onClickSubreddit(callback.getController().getSubreddit(getAdapterPosition()));
                }
            });
            textDescription.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    MotionEvent newEvent = MotionEvent.obtain(event);
                    newEvent.offsetLocation(v.getLeft(), v.getTop());
                    ViewHolder.this.itemView.onTouchEvent(newEvent);
                    newEvent.recycle();
                    return false;
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AnimationUtils.animateExpand(layoutContainerExpand, 1f, null);
                    Log.d(TAG, "onClick");
                }
            });

        }

        public void onBind(Subreddit subreddit) {

            layoutContainerExpand.setVisibility(View.GONE);

            textName.setText(subreddit.getDisplayName());
            textTitle.setText(Reddit.getTrimmedHtml(subreddit.getTitle()));

            if ("null".equals(subreddit.getPublicDescriptionHtml())) {
                textDescription.setVisibility(View.GONE);
            }
            else {
                textDescription.setText(Reddit.getTrimmedHtml(subreddit.getPublicDescriptionHtml()));
            }

            textInfo.setText(subreddit.getSubscribers() + " subscribers\n" +
                    "created " + new Date(subreddit.getCreatedUtc()));

        }
    }

}
