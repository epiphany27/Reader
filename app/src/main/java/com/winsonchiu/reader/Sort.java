package com.winsonchiu.reader;

/**
 * Created by TheKeeperOfPie on 6/5/2015.
 */
public enum Sort {

    HOT(R.id.item_sort_hot),
    NEW(R.id.item_sort_new),
    TOP(R.id.item_sort_top),
    CONTROVERSIAL(R.id.item_sort_controversial),
    RELEVANCE(R.id.item_sort_relevance),
    ACTIVITY(R.id.item_sort_activity);

    private int menuId;

    Sort(int menuId) {
        this.menuId = menuId;
    }

    public int getMenuId() {
        return menuId;
    }


    // String returned is lowercase for use in URL and formatting
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}