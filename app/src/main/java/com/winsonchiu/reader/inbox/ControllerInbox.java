/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.inbox;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Likes;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.rx.ObserverEmpty;
import com.winsonchiu.reader.utils.ControllerListener;
import com.winsonchiu.reader.utils.UtilsRx;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class ControllerInbox {

    public static final int VIEW_TYPE_MESSAGE = 0;
    public static final int VIEW_TYPE_COMMENT = 1;
    public static final String INBOX = "inbox";
    public static final String UNREAD = "unread";
    public static final String SENT = "sent";
    public static final String COMMENTS = "comments";
    public static final String SELF_REPLY = "selfreply";
    public static final String MENTIONS = "mentions";
    public static final String MODERATOR = "moderator";
    public static final String MODERATOR_UNREAD = "moderator/unread";

    private static final String TAG = ControllerInbox.class.getCanonicalName();

    private Set<Listener> listeners;
    private Listing data;
    private Link link;
    private Page page;
    private boolean isLoading;

    @Inject Reddit reddit;

    public ControllerInbox(Context context) {
        CustomApplication.getComponentMain().inject(this);
        data = new Listing();
        listeners = new HashSet<>();
        link = new Link();
        page = new Page(INBOX, context.getString(R.string.inbox_page_inbox));
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        setTitle();
        listener.getAdapter().notifyDataSetChanged();
        listener.setRefreshing(isLoading());
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setTitle() {
        for (Listener listener : listeners) {
            listener.setToolbarTitle("Inbox");
        }
    }

    public int getViewType(int position) {

        Thing thing = data.getChildren().get(position);

        if (thing instanceof Message) {
            return VIEW_TYPE_MESSAGE;
        }
        else if (thing instanceof Comment) {
            return VIEW_TYPE_COMMENT;
        }

        throw new IllegalStateException(thing + " is not a valid view type");
    }


    public int getItemCount() {
        return data.getChildren().size();
    }

    public Link getLink(int position) {
        return link;
    }

    public Message getMessage(int position) {
        return (Message) data.getChildren().get(position);
    }

    public Comment getComment(int position) {
        return (Comment) data.getChildren().get(position);
    }

    public void setPage(Page page) {
        if (!this.page.equals(page)) {
            this.page = page;
            reload();
        }
    }

    public Page getPage() {
        return page;
    }

    public void reload() {
        Log.d(TAG, "Page: " + page.getPage());

        reddit.message(page.getPage(), null)
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoading(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        setData(listing);
                        for (Listener listener : listeners) {
                            listener.setPage(page);
                            listener.getAdapter().notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void finish() {
                        setLoading(false);
                    }
                });
    }

    public void setData(Listing data) {
        this.data = data;
    }

    public boolean hasChildren(Comment comment) {
        return false;
    }

    public void editComment(String name, final int level, String text) {

        reddit.editUserText(name, text)
                .flatMap(UtilsRx.flatMapWrapError(response ->
                        Comment.fromJson(ComponentStatic.getObjectMapper()
                                .readValue(response, JsonNode.class)
                                .get("json")
                                .get("data")
                                .get("things")
                                .get(0), level)))
                .subscribe(new Observer<Comment>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Comment newComment) {
                        int commentIndex = data.getChildren()
                                .indexOf(newComment);

                        if (commentIndex > -1) {
                            Comment comment = (Comment) data.getChildren().get(commentIndex);
                            comment.setBodyHtml(newComment.getBodyHtml());
                            comment.setEdited(newComment.getEdited());
                            for (Listener listener : listeners) {
                                listener.getAdapter().notifyItemChanged(commentIndex);
                            }
                        }
                    }
                });
    }

    public Reddit getReddit() {
        return reddit;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public Subreddit getSubreddit() {
        return new Subreddit();
    }

    public boolean showSubreddit() {
        return true;
    }

    public void insertMessage(Message message) {

        Message parentMessage = new Message();
        parentMessage.setId(message.getParentId());

        int messageIndex = data.getChildren().indexOf(parentMessage);
        if (messageIndex > -1) {
            data.getChildren()
                    .add(messageIndex + 1, message);
        }

        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemInserted(messageIndex + 1);
        }
    }

    public void insertComment(Comment comment) {

        // Placeholder to use ArrayList.indexOfLink() properly
        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());

        int commentIndex = data.getChildren().indexOf(parentComment);
        if (commentIndex > -1) {
            // Level and context are set as they are not provided by the send API
            parentComment = (Comment) data.getChildren().get(commentIndex);
            comment.setLevel(parentComment.getLevel() + 1);
            comment.setContext(parentComment.getContext());
            data.getChildren()
                    .add(commentIndex + 1, comment);

            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemInserted(commentIndex + 1);
            }
        }
    }

    public Observable<String> deleteComment(Comment comment) {
        int commentIndex = data.getChildren().indexOf(comment);
        if (commentIndex > -1) {
            data.getChildren()
                    .remove(commentIndex);
        }

        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemRemoved(commentIndex);
        }

        Observable<String> observable = reddit.delete(comment);
        observable.subscribe(new ObserverEmpty<>());
        return observable;
    }

    public Observable<String> voteComment(final AdapterCommentList.ViewHolderComment viewHolder,
                                          final Comment comment,
                                          Likes vote) {
        final int position = viewHolder.getAdapterPosition();

        final Likes oldVote = comment.getLikes();
        Likes newVote = Likes.NONE;

        if (comment.getLikes() != vote) {
            newVote = vote;
        }

        comment.setScore(comment.getScore() + newVote.getScoreValue() - comment.getLikes().getScoreValue());
        comment.setLikes(newVote);

        if (position == viewHolder.getAdapterPosition()) {
            viewHolder.setVoteColors();
        }

        final Likes finalNewVote = newVote;

        Observable<String> observable = reddit.voteComment(comment, newVote);
        observable.subscribe(new FinalizingSubscriber<String>() {
                    @Override
                    public void error(Throwable e) {
                        comment.setScore(comment.getScore() - finalNewVote.getScoreValue());
                        comment.setLikes(oldVote);
                        if (position == viewHolder.getAdapterPosition()) {
                            viewHolder.setVoteColors();
                        }
                    }
                });
        return observable;
    }

    public void loadNestedComments(Comment moreComment) {
        // Not implemented
    }

    public boolean isCommentExpanded(int position) {
        // Not implemented
        return true;
    }

    public void sendComment(String name, String text) {
        reddit.sendComment(name, text)
                .flatMap(UtilsRx.flatMapWrapError(response ->
                        Comment.fromJson(ComponentStatic.getObjectMapper()
                                .readValue(response, JsonNode.class).get("json")
                                .get("data")
                                .get("things")
                                .get(0), 0)))
                .subscribe(new FinalizingSubscriber<Comment>() {
                    @Override
                    public void next(Comment next) {
                        insertComment(next);
                    }
                });
    }

    public void loadMore() {
        if (isLoading || TextUtils.isEmpty(data.getAfter())) {
            return;
        }

        reddit.message(page.getPage(), data.getAfter())
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoading(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        int startSize = data.getChildren().size();
                        data.addChildren(listing.getChildren());
                        data.setAfter(listing.getAfter());
                        for (Listener listener : listeners) {
                            listener.getAdapter().notifyItemRangeInserted(startSize,
                                    data.getChildren().size() - startSize);
                        }
                    }

                    @Override
                    public void finish() {
                        setLoading(false);
                    }
                });
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        for (Listener listener : listeners) {
            listener.setRefreshing(isLoading());
        }
    }

    public boolean setReplyText(String name, String text, boolean collapsed) {

        for (int index = 0; index < data.getChildren().size(); index++) {
            Thing thing = data.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index);
                }
                return true;
            }
        }
        return false;
    }

    public Observable<String> markAllRead() {
        Observable<String> observable = reddit.readAllMessages();
        observable.subscribe(new ObserverEmpty<String>());
        return observable;
    }

    public interface Listener extends ControllerListener {
        void setPage(Page page);
    }

}
