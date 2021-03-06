/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AppSettings {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            THEME_DARK,
            THEME_LIGHT,
            THEME_BLACK,
    })
    public @interface ThemeBackground {}

    public static final String THEME_DARK = "Dark";
    public static final String THEME_LIGHT = "Light";
    public static final String THEME_BLACK = "Black";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            THEME_RED,
            THEME_PINK,
            THEME_PURPLE,
            THEME_DEEP_PURPLE,
            THEME_INDIGO,
            THEME_BLUE,
            THEME_LIGHT_BLUE,
            THEME_CYAN,
            THEME_TEAL,
            THEME_GREEN,
            THEME_LIGHT_GREEN,
            THEME_LIME,
            THEME_YELLOW,
            THEME_AMBER,
            THEME_ORANGE,
            THEME_DEEP_ORANGE,
            THEME_BROWN,
            THEME_GREY,
            THEME_BLUE_GREY,
    })
    public @interface ThemeColor {}

    public static final String THEME_RED = "Red";
    public static final String THEME_PINK = "Pink";
    public static final String THEME_PURPLE = "Purple";
    public static final String THEME_DEEP_PURPLE = "Deep Purple";
    public static final String THEME_INDIGO = "Indigo";
    public static final String THEME_BLUE = "Blue";
    public static final String THEME_LIGHT_BLUE = "Light Blue";
    public static final String THEME_CYAN = "Cyan";
    public static final String THEME_TEAL = "Teal";
    public static final String THEME_GREEN = "Green";
    public static final String THEME_LIGHT_GREEN = "Light Green";
    public static final String THEME_LIME = "Lime";
    public static final String THEME_YELLOW = "Yellow";
    public static final String THEME_AMBER = "Amber";
    public static final String THEME_ORANGE = "Orange";
    public static final String THEME_DEEP_ORANGE = "Deep Orange";
    public static final String THEME_BROWN = "Brown";
    public static final String THEME_GREY = "Grey";
    public static final String THEME_BLUE_GREY = "Blue Grey";

    // List of all possible keys
    public static final String INITIALIZED = "initialized";
    public static final String DEVICE_ID = "deviceId";

    public static final String INTERFACE_MODE = "interfaceMode";
    public static final String MODE_LIST = "List";
    public static final String MODE_GRID = "Grid";

    public static final String ACCOUNT_NAME = "accountName";
    public static final String SECRET = "secret";

    public static final String HEADER_FILE_NAME = "image_header";
    public static final String HEADER_NAME = "headerName";
    public static final String HEADER_PERMALINK = "headerPermalink";
    public static final String HEADER_EXPIRATION = "headerExpiration";
    public static final String HEADER_INTERVAL = "headerInterval";

    // Preferences

    public static final String PREF_FULL_TIMESTAMPS = "pref_full_timestamps";
    public static final String PREF_SHOW_THUMBNAILS = "pref_show_thumbnails";
    public static final String PREF_NSFW_THUMBNAILS = "pref_nsfw_thumbnails";
    public static final String PREF_GRID_THUMBNAIL_SIZE = "pref_grid_thumbnail_size";
    public static final String PREF_GRID_COLUMNS = "pref_grid_columns";
    public static final String PREF_INBOX_CHECK_INTERVAL = "pref_inbox_check_interval";
    public static final String PREF_SAVE_HISTORY = "pref_save_history";
    public static final String PREF_CLEAR_HISTORY = "pref_clear_history";
    public static final String PREF_DIM_POSTS = "pref_dim_posts";
    public static final String PREF_EXTERNAL_BROWSER = "pref_external_browser";
    public static final String PREF_HISTORY_SIZE = "pref_history_size";
    public static final String BETA_NOTICE_0 = "betaNotice0";
    public static final String SWIPE_EXIT_COMMENTS = "swipe_exit_comments";
    public static final String PREF_THEME_BACKGROUND = "pref_theme_background";
    public static final String PREF_THEME_PRIMARY = "pref_theme_primary";
    public static final String PREF_THEME_PRIMARY_DARK = "pref_theme_primary_dark";
    public static final String PREF_THEME_ACCENT = "pref_theme_accent";
    public static final String PREF_VERSION = "pref_version";
    public static final String PREF_HEADER_SUBREDDIT = "pref_header_subreddit";
    public static final String PREF_HOME_SUBREDDIT = "pref_home_subreddit";
    public static final String PREF_COLLAPSE_COMMENT_THREADS = "pref_collapse_comment_threads";

    private static final String TAG = AppSettings.class.getCanonicalName();
    public static final String SUBSCRIPTIONS = "subscriptions";;

    public static boolean initPrefs(Context context) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean initialized = preferences.getBoolean(INITIALIZED, false);

        if (!initialized) {
            // TODO: Set initial values

            SharedPreferences.Editor editor = preferences.edit();

            editor.putBoolean(INITIALIZED, true);
            editor.apply();

        }

        return initialized;
    }
}