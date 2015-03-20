package com.winsonchiu.reader.data;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Comment extends Thing {

    private static final String TAG = Comment.class.getCanonicalName();

    private Comment parent;

    private String approvedBy;
    private String author;
    private String authorFlairCssClass;
    private String authorFlairText;
    private String bannedBy;
    private String body;
    private String bodyHtml;
    private Reddit.Distinguished distinguished;
    private long edited;
    private int gilded;
    private Reddit.Vote likes;
    private String linkId;
    private int numReports; // May be "null"
    private String parentId;
    private boolean saved;
    private int score;
    private boolean scoreHidden;
    private String subreddit;
    private String subredditId;

    // May not be present
    private String linkAuthor;
    private String linkTitle;
    private String linkUrl;

    private int level;
    private boolean isMore;
    private List<Comment> replies;

    public static void addAllFromJson(List<Comment> comments, JSONObject rootJsonObject, int level) throws JSONException {

        comments.add(Comment.fromJson(rootJsonObject, level));

        if (rootJsonObject.getJSONObject("data").has("replies") && !TextUtils.isEmpty(rootJsonObject.getJSONObject("data").getString("replies"))) {
            JSONArray arrayComments = rootJsonObject.getJSONObject("data")
                    .getJSONObject("replies")
                    .getJSONObject("data")
                    .getJSONArray("children");

            for (int index = 0; index < arrayComments.length(); index++) {
                Comment.addAllFromJson(comments, arrayComments.getJSONObject(index), level + 1);
            }
        }

    }

    public static Comment fromJson(JSONObject rootJsonObject, int level) throws JSONException {

        Comment comment = new Comment();
        comment.setLevel(level);
        comment.setKind(rootJsonObject.getString("kind"));

        JSONObject jsonObject = rootJsonObject.getJSONObject("data");

        comment.setId(jsonObject.getString("id"));
        comment.setName(jsonObject.getString("name"));
        comment.setParentId(jsonObject.getString("parent_id"));

        if (comment.getKind().equals("more")) {
            comment.setIsMore(true);
            return comment;
        }

        comment.setApprovedBy(jsonObject.getString("approved_by"));
        comment.setAuthor(jsonObject.getString("author"));
        comment.setAuthorFlairCssClass(jsonObject.getString("author_flair_css_class"));
        comment.setAuthorFlairText(jsonObject.getString("author_flair_text"));
        comment.setBannedBy(jsonObject.getString("banned_by"));
        comment.setBody(jsonObject.getString("body"));
        comment.setBodyHtml(jsonObject.getString("body_html"));


        switch (jsonObject.getString("distinguished")) {
            case "null":
                comment.setDistinguished(Reddit.Distinguished.NOT_DISTINGUISHED);
                break;
            case "moderator":
                comment.setDistinguished(Reddit.Distinguished.MODERATOR);
                break;
            case "admin":
                comment.setDistinguished(Reddit.Distinguished.ADMIN);
                break;
            case "special":
                comment.setDistinguished(Reddit.Distinguished.SPECIAL);
                break;
        }

        String edited = jsonObject.getString("edited");
        switch (edited) {
            case "true":
                comment.setEdited(1);
                break;
            case "false":
                comment.setEdited(0);
                break;
            default:
                comment.setEdited(jsonObject.getLong("edited"));
                break;
        }

        comment.setGilded(jsonObject.getInt("gilded"));

        switch (jsonObject.getString("likes")) {
            case "null":
                comment.setLikes(Reddit.Vote.NOT_VOTED);
                break;
            case "true":
                comment.setLikes(Reddit.Vote.UPVOTED);
                break;
            case "false":
                comment.setLikes(Reddit.Vote.DOWNVOTED);
                break;
        }

        comment.setLinkId(jsonObject.getString("link_id"));

        if (jsonObject.getString("num_reports").equals("null")) {
            comment.setNumReports(0);
        }
        else {
            comment.setNumReports(jsonObject.getInt("num_reports"));
        }
        comment.setSaved(jsonObject.getBoolean("saved"));
        comment.setScore(jsonObject.getInt("score"));
        comment.setScoreHidden(jsonObject.getBoolean("score_hidden"));
        comment.setSubreddit(jsonObject.getString("subreddit"));
        comment.setSubredditId(jsonObject.getString("subreddit_id"));

        comment.setLinkAuthor(jsonObject.has("link_author") ? jsonObject.getString("link_author") :
                "");
        comment.setLinkTitle(jsonObject.has("link_title") ? jsonObject.getString("link_title") :
                "");
        comment.setLinkUrl(jsonObject.has("link_url") ? jsonObject.getString("link_url") :
                "");

//        JSONArray arrayReplies = jsonObject.getJSONArray("replies");
//        ArrayList<Comment> listReplies = new ArrayList<>(arrayReplies.length());
//        for (int index = 0; index < arrayReplies.length(); index++) {
//            listReplies.add(Comment.fromJson(arrayReplies.getJSONObject(index)));
//        }
//
//        comment.setReplies(listReplies);

        return comment;
    }

    public Comment() {
        super();
    }

    public Comment(Comment parent) {
        super();
        this.parent = parent;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
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

    public String getBannedBy() {
        return bannedBy;
    }

    public void setBannedBy(String bannedBy) {
        this.bannedBy = bannedBy;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public Reddit.Distinguished getDistinguished() {
        return distinguished;
    }

    public void setDistinguished(Reddit.Distinguished distinguished) {
        this.distinguished = distinguished;
    }

    public long getEdited() {
        return edited;
    }

    public void setEdited(long edited) {
        this.edited = edited;
    }

    public int getGilded() {
        return gilded;
    }

    public void setGilded(int gilded) {
        this.gilded = gilded;
    }

    public Reddit.Vote isLikes() {
        return likes;
    }

    public void setLikes(Reddit.Vote likes) {
        this.likes = likes;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public int getNumReports() {
        return numReports;
    }

    public void setNumReports(int numReports) {
        this.numReports = numReports;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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

    public boolean isScoreHidden() {
        return scoreHidden;
    }

    public void setScoreHidden(boolean scoreHidden) {
        this.scoreHidden = scoreHidden;
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

    public String getLinkAuthor() {
        return linkAuthor;
    }

    public void setLinkAuthor(String linkAuthor) {
        this.linkAuthor = linkAuthor;
    }

    public String getLinkTitle() {
        return linkTitle;
    }

    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }

    public boolean isMore() {
        return isMore;
    }

    public void setIsMore(boolean isMore) {
        this.isMore = isMore;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}