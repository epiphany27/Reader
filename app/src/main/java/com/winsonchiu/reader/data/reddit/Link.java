/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.text.Html;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.rjeschke.txtmark.Processor;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.utils.UtilsJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Link extends Replyable {

    private static final String TAG = Link.class.getCanonicalName();

    private String author = "";
    private String authorFlairCssClass = "";
    private String authorFlairText = "";
    private boolean clicked;
    private String domain = "";
    private boolean hidden;
    private boolean isSelf;
    private int likes;
    private String linkFlairCssClass = "";
    private String linkFlairText = "";
    private String media = "";
    private String mediaEmbed = "";
    private int numComments;
    private boolean over18;
    private String permalink = "";
    private boolean saved;
    private int score;
    private CharSequence selfText = "";
    private CharSequence selfTextHtml = "";
    private String subreddit = "";
    private String subredditId = "";
    private String thumbnail = "";
    private String title = "";
    private String url = "";
    private long edited;
    private Reddit.Distinguished distinguished;
    private boolean stickied;
    private long created;
    private long createdUtc;

    private Listing comments = new Listing();
    private Album album;
    private boolean commentsClicked;
    private int backgroundColor;


    public static Link fromJson(JsonNode nodeRoot) {

        // TODO: Move parsing of HTML to asynchronous thread

        Link link = new Link();
        link.setKind(UtilsJson.getString(nodeRoot.get("kind")));

        JsonNode nodeData = nodeRoot.get("data");

        link.setId(UtilsJson.getString(nodeData.get("id")));
        link.setName(UtilsJson.getString(nodeData.get("name")));

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        link.setCreated(UtilsJson.getLong(nodeData.get("created")) * 1000);
        link.setCreatedUtc(UtilsJson.getLong(nodeData.get("created_utc")) * 1000);

        link.setAuthor(UtilsJson.getString(nodeData.get("author")));
        link.setAuthorFlairCssClass(UtilsJson.getString(
                nodeData.get("author_flair_css_class")));
        link.setAuthorFlairText(UtilsJson.getString(nodeData.get("author_flair_text")));
        link.setClicked(UtilsJson.getBoolean(nodeData.get("clicked")));
        link.setDomain(UtilsJson.getString(nodeData.get("domain")));
        link.setHidden(UtilsJson.getBoolean(nodeData.get("hidden")));
        link.setSelf(UtilsJson.getBoolean(nodeData.get("is_self")));

        switch (UtilsJson.getString(nodeData.get("likes"))) {
            case "null":
                link.setLikes(0);
                break;
            case "true":
                link.setLikes(1);
                break;
            case "false":
                link.setLikes(-1);
                break;
        }

        link.setLinkFlairCssClass(UtilsJson.getString(nodeData.get("link_flair_css_class")));
        link.setLinkFlairText(Html.fromHtml(
                UtilsJson.getString(nodeData.get("link_flair_text"))).toString());
        if (link.getLinkFlairText().equals("null")) {
            link.setLinkFlairText("");
        }
        link.setMedia(UtilsJson.getString(nodeData.get("media")));
        link.setMediaEmbed(UtilsJson.getString(nodeData.get("media_embed")));
        link.setNumComments(UtilsJson.getInt(nodeData.get("num_comments")));
        link.setOver18(UtilsJson.getBoolean(nodeData.get("over_18")));
        link.setPermalink(UtilsJson.getString(nodeData.get("permalink")));
        link.setSaved(UtilsJson.getBoolean(nodeData.get("saved")));
        link.setScore(UtilsJson.getInt(nodeData.get("score")));
        link.setSelfText(Html.fromHtml(UtilsJson.getString(
                nodeData.get("selftext")).replaceAll("\n", "<br>")));
        link.setSelfTextHtml(Reddit.getFormattedHtml(UtilsJson.getString(
                nodeData.get("selftext_html"))));
        link.setSubreddit(UtilsJson.getString(nodeData.get("subreddit")));
        link.setSubredditId(UtilsJson.getString(nodeData.get("subreddit_id")));
        link.setThumbnail(UtilsJson.getString(nodeData.get("thumbnail")));
        link.setTitle(Html.fromHtml(UtilsJson.getString(nodeData.get("title"))).toString());
        link.setUrl(String.valueOf(Html.fromHtml(UtilsJson.getString(nodeData.get("url")))));

        String edited = UtilsJson.getString(nodeData.get("edited"));
        switch (edited) {
            case "true":
                link.setEdited(1);
                break;
            case "false":
                link.setEdited(0);
                break;
            default:
                link.setEdited(UtilsJson.getLong(nodeData.get("edited")) * 1000);
                break;
        }

        switch (UtilsJson.getString(nodeData.get("distinguished"))) {
            case "null":
                link.setDistinguished(Reddit.Distinguished.NOT_DISTINGUISHED);
                break;
            case "moderator":
                link.setDistinguished(Reddit.Distinguished.MODERATOR);
                break;
            case "admin":
                link.setDistinguished(Reddit.Distinguished.ADMIN);
                break;
            case "special":
                link.setDistinguished(Reddit.Distinguished.SPECIAL);
                break;
        }

        link.setStickied(UtilsJson.getBoolean(nodeData.get("stickied")));

        return link;
    }

    public static Link fromJsonWithComments(JsonNode nodeRoot) throws IOException {

        Link link = fromJson(nodeRoot.get(0)
                .get("data")
                .get("children")
                .get(0));

        link.setComments(Listing.fromJson(nodeRoot.get(1)));

        return link;
    }

    public Link() {
        super();
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorFlairCssClass() {
        return authorFlairCssClass;
    }

    public void setAuthorFlairCssClass(String authorFlairCssClass) {
        this.authorFlairCssClass = authorFlairCssClass;
    }

    public String getAuthorFlairText() {
        return authorFlairText;
    }

    public void setAuthorFlairText(String authorFlairText) {
        this.authorFlairText = authorFlairText;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public void setSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getLinkFlairCssClass() {
        return linkFlairCssClass;
    }

    public void setLinkFlairCssClass(String linkFlairCssClass) {
        this.linkFlairCssClass = linkFlairCssClass;
    }

    public String getLinkFlairText() {
        return linkFlairText;
    }

    public void setLinkFlairText(String linkFlairText) {
        this.linkFlairText = linkFlairText;
    }

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public String getMediaEmbed() {
        return mediaEmbed;
    }

    public void setMediaEmbed(String mediaEmbed) {
        this.mediaEmbed = mediaEmbed;
    }

    public int getNumComments() {
        return numComments;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    public boolean isOver18() {
        return over18;
    }

    public void setOver18(boolean over18) {
        this.over18 = over18;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public CharSequence getSelfText() {
        return selfText;
    }

    public void setSelfText(CharSequence selfText) {
        if (Reddit.NULL.equals(selfText)) {
            selfText = "";
        }
        this.selfText = selfText;
    }

    public CharSequence getSelfTextHtml() {
        return selfTextHtml;
    }

    public void setSelfTextHtml(CharSequence selfTextHtml) {
        if (Reddit.NULL.equals(selfText)) {
            selfText = "";
        }
        this.selfTextHtml = selfTextHtml;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public String getSubredditId() {
        return subredditId;
    }

    public void setSubredditId(String subredditId) {
        this.subredditId = subredditId;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getEdited() {
        return edited;
    }

    public void setEdited(long edited) {
        this.edited = edited;
    }

    public Reddit.Distinguished getDistinguished() {
        return distinguished;
    }

    public void setDistinguished(Reddit.Distinguished distinguished) {
        this.distinguished = distinguished;
    }

    public boolean isStickied() {
        return stickied;
    }

    public void setStickied(boolean stickied) {
        this.stickied = stickied;
    }

    public Listing getComments() {
        return comments;
    }

    public void setComments(Listing comments) {
        this.comments = comments;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getCreatedUtc() {
        return createdUtc;
    }

    public void setCreatedUtc(long createdUtc) {
        this.createdUtc = createdUtc;
    }

    public boolean isCommentsClicked() {
        return commentsClicked;
    }

    public void setCommentsClicked(boolean commentsClicked) {
        this.commentsClicked = commentsClicked;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public CharSequence getParentHtml() {
        return getSelfTextHtml();
    }

}
