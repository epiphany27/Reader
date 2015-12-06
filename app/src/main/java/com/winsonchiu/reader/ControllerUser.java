/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.User;

import java.io.IOException;

import rx.Subscriber;

/**
 * Created by TheKeeperOfPie on 6/24/2015.
 */
public class ControllerUser {

    private static final String TAG = ControllerUser.class.getCanonicalName();
    private SharedPreferences preferences;
    private User user;
    private Reddit reddit;
    private AccountManager accountManager;
    private Account account;

    public ControllerUser(Activity activity) {
        super();
        setActivity(activity);
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        user = new User();
    }

    public User getUser() {
        return user;
    }

    public void setActivity(Activity activity) {
        reddit = Reddit.getInstance(activity);
        accountManager = AccountManager.get(activity.getApplicationContext());
    }

    public void reloadUser() {
        reddit.me()
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String response) {
                        try {
                            user = User.fromJson(Reddit.getObjectMapper().readValue(response, JsonNode.class));

                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public boolean hasUser() {
        return account != null;//user != null && !TextUtils.isEmpty(user.getName());
    }

    public void clearAccount() {
        account = null;
        user = new User();
    }

    public void setAccount(Account accountUser) {
        boolean accountFound = false;
        Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equals(accountUser.name)) {
                this.account = account;
                accountFound = true;
                reloadUser();
                break;
            }
        }

        user = new User();

        if (!accountFound) {
            account = null;
        }
        else {
            user.setName(account.name);
        }
    }
}
