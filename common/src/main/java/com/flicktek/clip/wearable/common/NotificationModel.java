package com.flicktek.clip.wearable.common;

import android.graphics.Bitmap;

public class NotificationModel {
    private String title;
    private String text;
    private String key;
    private Bitmap icon;

    // In case we don't have the icon, this a google wearable asset so we can query for it.
    private Object asset;
    private boolean selected;

    public NotificationModel(String title, String text, String key, Bitmap icon, Object asset) {
        this.title = title;
        this.text = text;
        this.key = key;
        this.icon = icon;
        this.asset = asset;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getKeyId() {
        return key;
    }

    public void setKey(String id) {
        this.key = id;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Object getAsset() {
        return asset;
    }
}
