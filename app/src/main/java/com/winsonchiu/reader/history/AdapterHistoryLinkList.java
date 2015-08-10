/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;

/**
 * Created by TheKeeperOfPie on 7/9/2015.
 */
public class AdapterHistoryLinkList extends AdapterLinkList {

    public AdapterHistoryLinkList(Activity activity,
            ControllerLinksBase controllerLinks,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderBase.EventListener eventListenerBase,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback) {
        super(activity, controllerLinks, eventListenerHeader, eventListenerBase,
                disallowListener, recyclerCallback);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return super.onCreateViewHolder(viewGroup, viewType);
        }

        return new AdapterLinkList.ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_link, viewGroup, false), eventListenerBase, disallowListener,
                recyclerCallback) {
            @Override
            public boolean isInHistory() {
                return false;
            }

            @Override
            public void addToHistory() {
                // Override to prevent moving to top
            }
        };
    }
}
