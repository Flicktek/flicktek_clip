package com.flicktek.android.clip.menus.notification;


import android.graphics.Bitmap;


public class NotificationModel {
	private String title;
	private String text;
	private Integer id;
	private Bitmap icon;
	private boolean selected;

	public NotificationModel(String title, String text, Integer id, Bitmap icon) {
		this.title = title;
		this.text = text;
		this.id = id;
		this.icon = icon;
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

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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
}
