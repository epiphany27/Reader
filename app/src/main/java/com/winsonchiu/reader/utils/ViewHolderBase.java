/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.res.Resources;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by TheKeeperOfPie on 3/30/2016.
 */
public abstract class ViewHolderBase extends RecyclerView.ViewHolder {

    public static final String TAG = ViewHolderBase.class.getCanonicalName();

    protected Resources resources;

    public ViewHolderBase(View itemView) {
        super(itemView);
        this.resources = itemView.getResources();
    }

    public void onPause() {

    }

    public void onRecycle() {

    }

    public int getColor(@ColorRes int colorRes) {
        return ContextCompat.getColor(itemView.getContext(), colorRes);
    }

}
