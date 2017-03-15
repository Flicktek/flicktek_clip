package com.flicktek.clip.menus.notification;

import android.app.Activity;
import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.flicktek.clip.R;

import java.util.List;

public class NotificationListAdapter extends WearableListView.Adapter {

	private final Activity activity;
	private final List<NotificationModel> items;

	public NotificationListAdapter(Activity activity, List<NotificationModel> items) {
		this.activity = activity;
		this.items = items;
	}

	@Override
	public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		return new WearableListView.ViewHolder(new MyItemView(activity));
	}

	@Override
	public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int position) {
		MyItemView itemView = (MyItemView) viewHolder.itemView;
		final NotificationModel item = items.get(position);

		TextView txtView = (TextView) itemView.findViewById(R.id.iv_notification_item_label);
		txtView.setText(item.getTitle());

		ImageView imgView = (ImageView) itemView.findViewById(R.id.iv_notification_item_icon);
		imgView.setImageBitmap(item.getIcon());

		if(item.isSelected()){
			itemView.setBackgroundResource(R.color.bg2);
		}else{
			itemView.setBackgroundResource(R.color.dark_grey);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public void refresh() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});
	}

	private final class MyItemView extends FrameLayout implements WearableListView.OnCenterProximityListener {

		final ImageView imgView;
		final TextView txtView;

		public MyItemView(Context context) {
			super(context);
			View.inflate(context, R.layout.notification_item, this);
			imgView = (ImageView) findViewById(R.id.iv_notification_item_icon);
			txtView = (TextView) findViewById(R.id.iv_notification_item_label);
		}

		@Override
		public void onCenterPosition(boolean b) {
			//Animation example to be ran when the view becomes the centered one
			imgView.animate().scaleX(1f).scaleY(1f).alpha(1);
			txtView.animate().scaleX(1f).scaleY(1f).alpha(1);
		}

		@Override
		public void onNonCenterPosition(boolean b) {
			//Animation example to be ran when the view is not the centered one anymore
			imgView.animate().scaleX(0.7f).scaleY(0.7f).alpha(0.5f);
			txtView.animate().scaleX(0.7f).scaleY(0.7f).alpha(0.5f);
		}
	}
}
