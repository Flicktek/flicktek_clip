package com.flicktek.android.clip;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.flicktek.android.clip.util.Helpers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Adapter for the menu.
 *
 * You can manually add items using the ArrayAdapter<AppModel> or load JSON files from the resources
 */
public class MenuAdapter extends ArrayAdapter<AppModel> {
    private static final String TAG = "MenuAdapter";

    public MenuAdapter(Context _context, int _resourceId, List<AppModel> _objects) {
        super(_context, _resourceId, _objects);
    }

    public AppModel getAppModel(int position) {
        return getItem(position);
    }

    public boolean populateFromJson(String jsonResourceName) throws Exception {
        MainActivity main = (MainActivity) getContext();
        Resources res = main.getResources();

        JSONObject jsonObj = Helpers.getJsonFromResources(main, jsonResourceName);

        if (jsonObj == null)
            throw new Exception("Something went wrong reading JSON");

        // Parse the JSON array and populate the internal adapter with menu items.
        JSONArray menu_items = jsonObj.getJSONArray("menu_items");

        for (int i = 0; i < menu_items.length(); i++) {
            JSONObject item_config = menu_items.getJSONObject(i);
            Drawable drawable = Helpers.getDrawableFromResources(main, "drawable", item_config);
            if (drawable!=null) {
                String text = "";
                try {
                    text = item_config.getString("text");
                } catch (Exception e) {
                }

                AppModel appModel = new AppModel(text, null, drawable, AppModel.NO_VIEW);
                appModel.setConfiguration(item_config);
                this.add(appModel);
            }
        }
        return true;
    }

    public View getView(int _position, View _view, ViewGroup _parent) {
        AppModel item = getItem(_position);

        ViewHolder holder;

        if (_view == null) {
            holder = new ViewHolder();

            LayoutInflater inflater = LayoutInflater.from(getContext());

            MainActivity main = (MainActivity) getContext();
            _view = inflater.inflate(R.layout.item_menu_rect, _parent, false);

            Typeface mainFont = Typeface.createFromAsset(getContext().getAssets(),
                    getContext().getString(R.string.main_font));

            holder.textView = (TextView) _view.findViewById(R.id.tv_item_label);
            holder.textView.setTypeface(mainFont);
            holder.textView.setTextColor(Color.WHITE);

            holder.imageView = (ImageView) _view.findViewById(R.id.iv_item_icon);

            _view.setTag(holder);
        } else {
            holder = (ViewHolder) _view.getTag();
        }

        holder.textView.setText(item.getName());
        if (item.getIcon() != null) {
            holder.imageView.setImageDrawable(item.getIcon());
        } else {
            holder.imageView.setImageResource(android.R.color.transparent);
        }

        ImageView imageIcon = (ImageView) _view.findViewById(R.id.iv_item_icon);
        if (item.isSelected()) {
            _view.setBackgroundResource(R.drawable.bg_light);
            holder.textView.setTextColor(Color.BLACK);

            int color = Color.parseColor("#000000");
            imageIcon.setColorFilter(color);
        } else {
            _view.setBackgroundResource(R.drawable.bg_dark);
            holder.textView.setTextColor(Color.WHITE);

            int color = Color.parseColor("#AE6118");
            imageIcon.setColorFilter(color);
        }

        return _view;
    }

    private static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }

}