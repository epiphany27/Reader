/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.modules;

import android.content.Context;

import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.comments.ControllerComments;
import com.winsonchiu.reader.comments.ControllerCommentsTop;
import com.winsonchiu.reader.dagger.ScopeActivity;
import com.winsonchiu.reader.history.ControllerHistory;
import com.winsonchiu.reader.inbox.ControllerInbox;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.search.ControllerSearch;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by TheKeeperOfPie on 12/13/2015.
 */
@Module
public class ModuleReddit {

    @ScopeActivity
    @Provides
    public ControllerLinks provideControllerLinks() {
        return new ControllerLinks();
    }

    @ScopeActivity
    @Provides
    public ControllerUser provideControllerUser() {
        return new ControllerUser();
    }

    @ScopeActivity
    @Provides
    public ControllerCommentsTop provideControllerCommentsTop() {
        return new ControllerCommentsTop();
    }

    @ScopeActivity
    @Provides
    public ControllerComments provideControllerComments() {
        return new ControllerComments();
    }

    @Provides
    @Named("instance")
    public ControllerComments provideControllerCommentsInstance() {
        return new ControllerComments();
    }

    @ScopeActivity
    @Provides
    public ControllerProfile provideControllerProflie(Context context, ControllerUser controllerUser) {
        return new ControllerProfile(context, controllerUser);
    }

    @ScopeActivity
    @Provides
    public ControllerInbox provideControllerInbox(Context context) {
        return new ControllerInbox(context);
    }

    @ScopeActivity
    @Provides
    public ControllerSearch provideControllerSearch(Context context, ControllerLinks controllerLinks, ControllerUser controllerUser) {
        return new ControllerSearch(context, controllerLinks, controllerUser);
    }

    @ScopeActivity
    @Provides
    public ControllerHistory provideControllerHistory() {
        return new ControllerHistory();
    }

}
