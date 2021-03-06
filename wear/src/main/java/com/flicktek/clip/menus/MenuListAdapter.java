package com.flicktek.clip.menus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.flicktek.clip.FlicktekSettings;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.R;
import com.flicktek.clip.util.Helpers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Adapter for the menu.
 * <p>
 * You can manually add items using the ArrayAdapter<AppModel> or load JSON files from the resources
 */
public class MenuListAdapter extends ArrayAdapter<AppModel> {
    private static final String TAG = "MenuAdapter";
    public boolean hasHeader = false;
    public boolean disableHeader = false;

    public MenuListAdapter(Context _context, int _resourceId, List<AppModel> _objects) {
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
            throw new Exception("Something went wrong reading JSON " + jsonResourceName);

        // Parse the JSON array and populate the internal adapter with menu items.
        JSONArray menu_items = jsonObj.getJSONArray("menu_items");

        for (int i = 0; i < menu_items.length(); i++) {
            JSONObject item_config = menu_items.getJSONObject(i);
            Drawable drawable = Helpers.getDrawableFromResources(main, "drawable", item_config);

            if (drawable != null) {
                AppModel appModel = new AppModel(null, null, drawable, AppModel.NO_VIEW);
                appModel.setConfiguration(item_config);
                this.add(appModel);

                if (appModel.isHeader)
                    hasHeader = true;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (jsonObj.getBoolean("back_button")) {
                add(new AppModel("Back", null,
                        res.getDrawable(R.drawable.ic_arrow_back_black_48dp, main.getTheme()),
                        AppModel.BACK_APPLICATION));
            }
        }

        return true;
    }

    public View getView(int _position, View _view, ViewGroup _parent) {
        AppModel item = getItem(_position);

        ViewHolder holder;
        ViewHeader header;

        if (_view != null) {
            if (item.isHeader) {
                if (!_view.getClass().isInstance(ViewHeader.class)) {
                    _view = null;
                }
            } else {
                if (!_view.getClass().isInstance(ViewHolder.class))
                    _view = null;
            }
        }

        if (_view == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            MainActivity main = (MainActivity) getContext();

            if (item.isHeader) {
                header = new ViewHeader();
                if (main.isRound)
                    _view = inflater.inflate(R.layout.item_menu_header_round, _parent, false);
                else
                    _view = inflater.inflate(R.layout.item_menu_header_rect, _parent, false);

                _view.setBackgroundResource(R.drawable.bg_dark);
                header.imageView = (ImageView) _view.findViewById(R.id.iv_header);
                header.imageView.setImageDrawable(item.getIcon());

                if (disableHeader) {
                    _view.setVisibility(View.GONE);
                }
                return _view;
            }

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

        holder.textView.setText(item.getName());
        if (item.getIcon() != null) {
            holder.imageView.setImageDrawable(item.getIcon());
        } else {
            holder.imageView.setImageResource(android.R.color.transparent);
        }

        String data = item.getDataKey();
        if (data != null) {
            String info = FlicktekSettings.getInstance().getString(data, "Not Available");
            holder.textViewSmall.setVisibility(View.VISIBLE);
            holder.textViewSmall.setText(info);
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

            //int color = Color.parseColor("#AE6118");
            int color = Color.parseColor("#FFFFFF");
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