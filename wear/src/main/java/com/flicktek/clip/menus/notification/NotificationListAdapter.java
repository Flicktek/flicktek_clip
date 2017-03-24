package com.flicktek.clip.menus.notification;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.flicktek.clip.MainActivity;
import com.flicktek.clip.R;
import com.flicktek.clip.wearable.common.NotificationModel;

import java.util.List;

/**
 * Adapter for the menu.
 * <p>
 * You can manually add items using the ArrayAdapter<NotificationModel> or load JSON files from the resources
 */
public class NotificationListAdapter extends ArrayAdapter<NotificationModel> {
	private static final String TAG = "NotificationListAdapter";
	public boolean hasHeader = false;
	public boolean disableHeader = false;

	public NotificationListAdapter(Context _context, int _resourceId, List<NotificationModel> _objects) {
		super(_context, _resourceId, _objects);
	}

	public NotificationModel getNotificationModel(int position) {
		return getItem(position);
	}
	
	public View getView(int _position, View _view, ViewGroup _parent) {
		NotificationModel item = getItem(_position);

		ViewHolder holder;
		ViewHeader header;

		if (_view != null) {
			if (!_view.getClass().isInstance(ViewHolder.class))
				_view = null;
		}

		if (_view == null) {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			MainActivity main = (MainActivity) getContext();
			holder = new ViewHolder();

			if (main.isRound) {
				_view = inflater.inflate(R.layout.item_menu_round, _parent, false);
			} else {
				_view = inflater.inflate(R.layout.item_menu_rect, _parent, false);
			}

			Typeface mainFont = Typeface.createFromAsset(getContext().getAssets(), getContext().getString(R.string.main_font));
			holder.textView = (TextView) _view.findViewById(R.id.tv_item_label);
			holder.textView.setTypeface(mainFont);
			holder.textView.setTextColor(Color.WHITE);

			holder.textViewSmall = (TextView) _view.findViewById(R.id.tv_item_label_small);
			holder.textViewSmall.setTypeface(mainFont);
			holder.textViewSmall.setTextColor(Color.WHITE);

			holder.imageView = (ImageView) _view.findViewById(R.id.iv_item_icon);

			_view.setTag(holder);
		} else {
			holder = (ViewHolder) _view.getTag();
		}

		holder.textView.setText(item.getTitle());
		if (item.getIcon() != null) {
			//holder.imageView.setImageDrawable(item.getIcon());
		} else {
			holder.imageView.setImageResource(android.R.color.transparent);
		}

		ImageView imageIcon = (ImageView) _view.findViewById(R.id.iv_item_icon);
		if (item.isSelected()) {
			_view.setBackgroundResource(R.drawable.bg_light);
			holder.textView.setTextColor(Color.BLACK);
			holder.textViewSmall.setTextColor(Color.BLACK);

			int color = Color.parseColor("#000000");
			imageIcon.setColorFilter(color);
		} else {
			_view.setBackgroundResource(R.drawable.bg_dark);
			holder.textView.setTextColor(Color.WHITE);
			holder.textViewSmall.setTextColor(Color.WHITE);

			int color = Color.parseColor("#AE6118");
			imageIcon.setColorFilter(color);
		}

		return _view;
	}

	public void disable_header() {
		disableHeader = true;
	}

	private static class ViewHolder {
		TextView textView;
		TextView textViewSmall;
		ImageView imageView;
	}

	private static class ViewHeader {
		ImageView imageView;
	}

}