package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.Thing;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 6/3/2015.
 */
public class ControllerSearch {

    public static final int PAGE_SUBREDDITS = 0;
    public static final int PAGE_LINKS_SUBREDDIT = 1;
    public static final int PAGE_LINKS = 2;

    private static final String TAG = ControllerSearch.class.getCanonicalName();

    private ControllerLinks controllerLinks;
    private Set<Listener> listeners;
    private Activity activity;
    private SharedPreferences preferences;
    private Reddit reddit;
    private Listing subredditsSubscribed;
    private Listing subreddits;
    private Listing links;
    private Listing linksSubreddit;
    private String query;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Sort sort;
    private Time time;
    private volatile int currentPage;
    private Request<String> requestSubreddits;
    private Request<String> requestLinks;
    private Request<String> requestLinksSubreddit;
    private User user;
    private boolean isLoadingLinks;
    private boolean isLoadingLinksSubreddit;

    public ControllerSearch(Activity activity) {
        setActivity(activity);
        sort = Sort.RELEVANCE;
        time = Time.ALL;
        listeners = new HashSet<>();
        query = "";
        subredditsSubscribed = new Listing();
        subreddits = new Listing();
        links = new Listing();
        linksSubreddit = new Listing();
        reloadSubscriptionList();
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
        this.user = new User();

        if (!TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            try {
                this.user = User.fromJson(
                        new JSONObject(preferences.getString(AppSettings.ACCOUNT_JSON, "")));
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        setTitle();
        listener.getAdapterSearchSubreddits().notifyDataSetChanged();
        listener.setSort(sort);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Reddit getReddit() {
        return reddit;
    }

    public void setQuery(String query) {
        this.query = query;
        if ((TextUtils.isEmpty(query) || query.length() < 2) && currentPage == PAGE_SUBREDDITS) {
            subreddits = subredditsSubscribed;
            for (Listener listener : listeners) {
                listener.getAdapterSearchSubreddits()
                        .notifyDataSetChanged();
            }
            return;
        }
        setTitle();
        reloadCurrentPage();
    }

    public void setTitle() {
        for (Listener listener : listeners) {
            listener.setToolbarTitle(query);
        }
    }

    public void reloadSubscriptionList() {
        String url;

        if (TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            url = Reddit.OAUTH_URL + "/subreddits/default?show=all&limit=100";
        }
        else {
            url = Reddit.OAUTH_URL + "/subreddits/mine/subscriber?show=all&limit=100";
        }

        reddit.loadGet(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Listing listing = Listing.fromJson(new JSONObject(response));
                            subredditsSubscribed = listing;
                            Collections.sort(subredditsSubscribed.getChildren(), new Comparator<Thing>() {
                                @Override
                                public int compare(Thing lhs, Thing rhs) {
                                    return ((Subreddit) lhs).getDisplayName().compareToIgnoreCase(((Subreddit) rhs).getDisplayName());
                                }
                            });
                            if (TextUtils.isEmpty(query)) {
                                subreddits = subredditsSubscribed;
                                for (Listener listener : listeners) {
                                    listener.getAdapterSearchSubreddits()
                                            .notifyDataSetChanged();
                                }
                            }
                            if (listing.getChildren().size() == 100) {
                                loadMoreSubscriptions();
                            }

                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    private void loadMoreSubscriptions() {
        String url;

        if (TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            url = Reddit.OAUTH_URL + "/subreddits/default?show=all&limit=100&after=" + subredditsSubscribed.getAfter();
        }
        else {
            url = Reddit.OAUTH_URL + "/subreddits/mine/subscriber?show=all&limit=100&after=" + subredditsSubscribed.getAfter();
        }

        reddit.loadGet(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Listing listing = Listing.fromJson(new JSONObject(response));
                            subredditsSubscribed.addChildren(listing.getChildren());
                            Collections.sort(subredditsSubscribed.getChildren(), new Comparator<Thing>() {
                                @Override
                                public int compare(Thing lhs, Thing rhs) {
                                    return ((Subreddit) lhs).getDisplayName().compareToIgnoreCase(((Subreddit) rhs).getDisplayName());
                                }
                            });
                            if (TextUtils.isEmpty(query)) {
                                subreddits = subredditsSubscribed;
                                for (Listener listener : listeners) {
                                    listener.getAdapterSearchSubreddits()
                                            .notifyDataSetChanged();
                                }
                            }
                            if (listing.getChildren().size() == 100) {
                                loadMoreSubscriptions();
                            }

                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    public void reloadCurrentPage() {
        Log.d(TAG, "reloadCurrentPage");
        switch (currentPage) {
            case PAGE_SUBREDDITS:
                if (TextUtils.isEmpty(query)) {
                    for (Listener listener : listeners) {
                        listener.getAdapterSearchSubreddits()
                                .notifyDataSetChanged();
                    }
                }
                else {
                    reloadSubreddits();
                }
                break;
            case PAGE_LINKS:
                reloadLinks();
                break;
            case PAGE_LINKS_SUBREDDIT:
                reloadLinksSubreddit();
                break;
        }
    }

    public void reloadSubreddits() {

        if (requestSubreddits != null) {
            requestSubreddits.cancel();
        }

        try {
            requestSubreddits = reddit.loadGet(Reddit.OAUTH_URL + "/subreddits/search?show=all&q=" + URLEncoder.encode(query, Reddit.UTF_8).replaceAll("\\s", "") + "&sort=" + sort.toString(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                Listing listing = Listing.fromJson(new JSONObject(response));
                                Iterator<Thing> iterator = listing.getChildren().iterator();
                                while (iterator.hasNext()) {
                                    Subreddit subreddit = (Subreddit) iterator.next();
                                    if (subreddit.getSubredditType()
                                            .equalsIgnoreCase(Subreddit.PRIVATE) && !subreddit.isUserIsContributor()) {
                                        iterator.remove();
                                    }
                                }

                                subreddits = listing;
                                for (Listener listener : listeners) {
                                    listener.getAdapterSearchSubreddits().notifyDataSetChanged();
                                }
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    }, 0);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public void reloadLinks() {
        setLoadingLinks(true);

        if (requestLinks != null) {
            requestLinks.cancel();
        }

        try {
            String sortString = sort.toString();
            if (sort == Sort.ACTIVITY) {
                sortString = Sort.HOT.name();
            }
            requestLinks = reddit.loadGet(Reddit.OAUTH_URL + "/search?q=" + URLEncoder.encode(query, Reddit.UTF_8) + "&sort=" + sortString + "&t=" + time.toString(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Response: " + response);
                            try {
                                links = Listing.fromJson(new JSONObject(response));
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinks().notifyDataSetChanged();
                                }
                                setLoadingLinks(false);
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                                setLoadingLinks(false);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            setLoadingLinks(false);
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, 0);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setLoadingLinks(false);
        }
    }

    public void reloadLinksSubreddit() {
        setLoadingLinksSubreddit(true);

        Subreddit subreddit = controllerLinks.getSubreddit();
        String url = Reddit.OAUTH_URL;
        if (TextUtils.isEmpty(subreddit.getUrl())) {
            url += "/";
        }
        else {
            url += subreddit.getUrl();
        }

        if (requestLinksSubreddit != null) {
            requestLinksSubreddit.cancel();
        }

        try {
            requestLinksSubreddit = reddit.loadGet(url + "search?restrict_sr=on&q=" + URLEncoder.encode(query, Reddit.UTF_8) + "&sort=" + sort.toString() + "&t=" + time.toString(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                linksSubreddit = Listing.fromJson(new JSONObject(response));
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinksSubreddit().notifyDataSetChanged();
                                }
                                setLoadingLinksSubreddit(false);
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                                setLoadingLinksSubreddit(false);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            setLoadingLinksSubreddit(false);
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, 0);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setLoadingLinksSubreddit(false);
        }
    }

    public Subreddit getSubreddit(int position) {
        return (Subreddit) subreddits.getChildren()
                .get(position);
    }

    public int getSubredditCount() {
        return subreddits.getChildren()
                .size();
    }

    public Drawable getDrawableForLink(Link link) {
        String thumbnail = link.getThumbnail();

        if (link.isSelf()) {
            return drawableSelf;
        }

        if (Reddit.DEFAULT.equals(thumbnail) || Reddit.NSFW.equals(thumbnail)) {
            return drawableDefault;
        }

        return null;
    }

    public Link getLink(int position) {
        return (Link) links.getChildren().get(position - 1);
    }

    public void voteLink(final RecyclerView.ViewHolder viewHolder, final Link link, int vote) {
        reddit.voteLink(viewHolder, link, vote, new Reddit.VoteResponseListener() {
            @Override
            public void onVoteFailed() {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    public int sizeLinks() {
        return links.getChildren().size();
    }

    public boolean isLoadingLinks() {
        return isLoadingLinks;
    }

    public void loadMoreLinks() {
        if (isLoadingLinks()) {
            return;
        }
        setLoadingLinks(true);

        if (requestLinks != null) {
            requestLinks.cancel();
        }

        try {
            String sortString = sort.toString();
            if (sort == Sort.ACTIVITY) {
                sortString = Sort.HOT.name();
            }
            requestLinks = reddit.loadGet(Reddit.OAUTH_URL + "/search?q=" + URLEncoder.encode(query, Reddit.UTF_8) + "&sort=" + sortString + "&t=" + time.toString() + "&after=" + links.getAfter(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {

                                int positionStart = links.getChildren()
                                        .size() + 1;
                                int startSize = links.getChildren().size();
                                Listing listing = Listing.fromJson(new JSONObject(response));
                                links.addChildren(listing.getChildren());
                                links.setAfter(listing.getAfter());
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinks().notifyItemRangeInserted(positionStart, links.getChildren().size() - startSize);
                                }
                                setLoadingLinks(false);
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                                setLoadingLinks(false);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            setLoadingLinks(false);
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, 0);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setLoadingLinks(false);
        }
    }

    private void setLoadingLinks(boolean loading) {
        isLoadingLinks = loading;
    }

    public Link getLinkSubreddit(int position) {
        return (Link) linksSubreddit.getChildren().get(position - 1);
    }

    public void voteLinkSubreddit(RecyclerView.ViewHolder viewHolder, final Link link, int vote) {

    }

    public int sizeLinksSubreddit() {
        return linksSubreddit.getChildren().size();
    }

    public boolean isLoadingLinksSubreddit() {
        return isLoadingLinksSubreddit;
    }

    public void loadMoreLinksSubreddit() {

        if (isLoadingLinksSubreddit()) {
            return;
        }
        setLoadingLinksSubreddit(true);

        Subreddit subreddit = controllerLinks.getSubreddit();
        String url = Reddit.OAUTH_URL;
        if (TextUtils.isEmpty(subreddit.getUrl())) {
            url += "/";
        }
        else {
            url += subreddit.getUrl();
        }

        if (requestLinksSubreddit != null) {
            requestLinksSubreddit.cancel();
        }

        try {
            requestLinksSubreddit = reddit.loadGet(url + "search?restrict_sr=on&q=" + URLEncoder.encode(query, Reddit.UTF_8) + "&sort=" + sort.toString() + "&t=" + time.toString() + "&after=" + linksSubreddit.getAfter(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                Listing listing = Listing.fromJson(new JSONObject(response));
                                if (listing.getChildren().isEmpty() || listing.getChildren().get(0) instanceof Subreddit) {
                                    setLoadingLinksSubreddit(false);
                                    return;
                                }
                                int startSize = linksSubreddit.getChildren().size();
                                int positionStart = startSize + 1;

                                linksSubreddit.addChildren(listing.getChildren());
                                linksSubreddit.setAfter(listing.getAfter());
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinksSubreddit().notifyItemRangeInserted(positionStart, linksSubreddit.getChildren().size() - startSize);
                                }
                                setLoadingLinksSubreddit(false);
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                                setLoadingLinksSubreddit(false);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            setLoadingLinksSubreddit(false);
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, 0);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setLoadingLinksSubreddit(false);
        }
    }

    private void setLoadingLinksSubreddit(boolean loading) {
        isLoadingLinksSubreddit = loading;
    }

    public Activity getActivity() {
        return activity;
    }

    public Subreddit getSubreddit() {
        return new Subreddit();
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            reloadCurrentPage();
        }
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            reloadCurrentPage();
        }
    }

    public String getQuery() {
        return query;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        reloadCurrentPage();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void clearResults() {
        if (subreddits == subredditsSubscribed) {
            subreddits = new Listing();
        }
        else {
            subreddits.getChildren()
                    .clear();
            subreddits = subredditsSubscribed;
        }
        links.getChildren().clear();
        linksSubreddit.getChildren().clear();
        query = "";
        sort = Sort.RELEVANCE;
        for (Listener listener : listeners) {
            listener.setSort(sort);
            listener.getAdapterSearchSubreddits().notifyDataSetChanged();
            listener.getAdapterLinks().notifyDataSetChanged();
            listener.getAdapterLinksSubreddit().notifyDataSetChanged();
        }
    }

    public void setControllerLinks(ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
    }

    public interface Listener {
        void onClickSubreddit(Subreddit subreddit);
        AdapterSearchSubreddits getAdapterSearchSubreddits();
        AdapterLink getAdapterLinks();
        AdapterLink getAdapterLinksSubreddit();
        void setToolbarTitle(CharSequence title);
        void setSort(Sort sort);
    }

    public interface ListenerCallback {
        ControllerSearch.Listener getListener();
        ControllerSearch getController();
        Activity getActivity();
    }
}
