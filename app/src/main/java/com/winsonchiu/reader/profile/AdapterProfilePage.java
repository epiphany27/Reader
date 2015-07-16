/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.profile;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.winsonchiu.reader.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterProfilePage extends BaseAdapter {

    private List<String> pages;
    private boolean isUser;

    public AdapterProfilePage(Activity activity) {
        super();

        // TODO: IMPORTANT FOR TRANSLATIONS, decouple UI title of page from actual page value
        pages = new ArrayList<>();
        pages.add(activity.getString(R.string.profile_page_overview));
        pages.add(activity.getString(R.string.profile_page_submitted));
        pages.add(activity.getString(R.string.profile_page_comments));
        pages.add(activity.getString(R.string.profile_page_gilded));
        pages.add(activity.getString(R.string.profile_page_upvoted));
        pages.add(activity.getString(R.string.profile_page_downvoted));
        pages.add(activity.getString(R.string.profile_page_hidden));
        pages.add(activity.getString(R.string.profile_page_saved));
    }

    @Override
    public int getCount() {
        return isUser ? pages.size() : 4;
    }

    @Override
    public String getItem(int position) {
        return pages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_page, parent, false);
        }

        TextView textPage = (TextView) convertView.findViewById(R.id.text_page);
        textPage.setText(pages.get(position));

        return convertView;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setIsUser(boolean isUser) {
        this.isUser = isUser;
        notifyDataSetChanged();
    }

    public List<String> getPages() {
        return pages;
    }
}