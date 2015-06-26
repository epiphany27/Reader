package com.winsonchiu.reader;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Subreddit;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public interface ControllerLinksBase {

    // TODO: Include default implementations

    Link getLink(int position);
    int sizeLinks();
    boolean isLoading();
    void loadMoreLinks();
    Subreddit getSubreddit();
    boolean showSubreddit();
}