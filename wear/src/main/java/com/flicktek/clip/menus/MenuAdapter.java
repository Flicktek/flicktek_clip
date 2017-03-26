package com.flicktek.clip.menus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
    private static final String TAG = "MenuAdapter";
    private List<AppModel> mDataset;
    MainActivity main;
    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;

    public boolean hasHeader = false;
    public boolean disableHeader = false;
    private int selectedItem = 0;

    public static Typeface mainFont;

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        RelativeLayout mRelativeLayout;
        TextView mTextView;
        TextView mTextViewSmall;
        ImageView mImageView;

        RelativeLayout mRelativeLayoutSelected;
        TextView mTextViewSelected;
        TextView mTextViewSmallSelected;
        ImageView mImageViewSelected;

        public ViewHolder(View view) {
            super(view);
            mRelativeLayout = (RelativeLayout) view.findViewById(R.id.rl_item_menu);

            mTextView = (TextView) view.findViewById(R.id.tv_item_label);
            mTextView.setTypeface(mainFont);
            mTextView.setTextColor(Color.WHITE);

            mTextViewSmall = (TextView) view.findViewById(R.id.tv_item_label_small);
            mTextViewSmall.setTypeface(mainFont);
            mTextViewSmall.setTextColor(Color.WHITE);

            mImageView = (ImageView) view.findViewById(R.id.iv_item_icon);

            mRelativeLayout.setBackgroundResource(R.drawable.bg_dark);
            mTextView.setTextColor(Color.WHITE);
            mTextViewSmall.setTextColor(Color.WHITE);

            int color = Color.parseColor("#AE6118");
            mImageView.setColorFilter(color);

            //---- SELECTED ----

            mRelativeLayoutSelected = (RelativeLayout) view.findViewById(R.id.rl_item_menu_selected);

            mTextViewSelected = (TextView) view.findViewById(R.id.tv_item_label_selected);
            mTextViewSelected.setTypeface(mainFont);
            mTextViewSelected.setTextColor(Color.WHITE);

            mTextViewSmallSelected = (TextView) view.findViewById(R.id.tv_item_label_small_selected);
            mTextViewSmallSelected.setTypeface(mainFont);
            mTextViewSmallSelected.setTextColor(Color.WHITE);

            mImageViewSelected = (ImageView) view.findViewById(R.id.iv_item_icon_selected);

            mRelativeLayoutSelected.setBackgroundResource(R.drawable.bg_light);
            mTextViewSelected.setTextColor(Color.BLACK);
            mTextViewSmallSelected.setTextColor(Color.BLACK);

            color = Color.parseColor("#000000");
            mImageViewSelected.setColorFilter(color);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Redraw the old selection and the new
                    notifyItemChanged(selectedItem);
                    selectedItem = getLayoutPosition();
                    notifyItemChanged(selectedItem);
                }
            });
        }
    }

    private boolean tryMoveSelection(RecyclerView.LayoutManager lm, int direction) {
        int nextSelectItem = selectedItem + direction;

        // If still within valid bounds, move the selection, notify to redraw, and scroll
        if (nextSelectItem > 0 && nextSelectItem < getItemCount()) {
            notifyItemChanged(selectedItem);
            selectedItem = nextSelectItem;
            notifyItemChanged(selectedItem);
            lm.scrollToPosition(selectedItem);
            return true;
        }

        return false;
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        // Handle key up and key down and attempt to move selection
        recyclerView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();

                // Return false if scrolled to the bounds and allow focus to move off the list
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return tryMoveSelection(lm, 1);
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return tryMoveSelection(lm, -1);
                    }
                }

                return false;
            }
        });
    }

    private static class ViewHeader {
        ImageView imageView;
    }

    public MenuAdapter(List<AppModel> _objects) {
        mDataset = _objects;
    }

    public AppModel getAppModel(int position) {
        return mDataset.get(position);
    }

    public void setSelected(int pos) {
        if (pos < 0)
            pos = 0;

        int size = mDataset.size();
        if (pos >= size)
            pos = 0;

        try {
            if (size > 1) {
                int old_pos = mPref.getInt("position", 0);
                if (old_pos < mDataset.size()) {
                    mDataset.get(old_pos).setSelected(false);
                }

                mEditor.putInt("position", pos);
                mEditor.commit();
            }

            mDataset.get(pos).setSelected(true);
            notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean populateFromJson(MainActivity main, String jsonResourceName) throws Exception {
        Resources res = main.getResources();

        String font = main.getString(R.string.main_font);
        mainFont = Typeface.createFromAsset(main.getAssets(), font);

        mPref = main.getSharedPreferences(jsonResourceName, Context.MODE_PRIVATE);
        mEditor = mPref.edit();

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
                mDataset.add(appModel);

                if (appModel.isHeader)
                    hasHeader = true;
            }
        }

        if (jsonObj.getBoolean("back_button")) {
            mDataset.add(new AppModel("Back", null,
                    res.getDrawable(R.drawable.ic_arrow_back_black_48dp, main.getTheme()),
                    AppModel.BACK_APPLICATION));
        }

        notifyDataSetChanged();
        return true;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MenuAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        View view;
        int resource = R.layout.item_menu_rect;
        if (MainActivity.isRound)
            resource = R.layout.item_menu_round;

        view = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);

        ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppModel item = mDataset.get(position);
        holder.itemView.setSelected(selectedItem == position);

        String data = item.getDataKey();
        if (data != null) {
            String info = FlicktekSettings.getInstance().getString(data, "Not Available");
            holder.mTextViewSmall.setVisibility(View.VISIBLE);
            holder.mTextViewSmall.setText(info);
        }

        if (item.isSelected()) {
            holder.mRelativeLayout.setVisibility(View.GONE);
            holder.mRelativeLayoutSelected.setVisibility(View.VISIBLE);
            holder.mTextViewSelected.setText(item.getName());

            if (item.getIcon() != null) {
                holder.mImageViewSelected.setImageDrawable(item.getIcon());
            } else {
                holder.mImageViewSelected.setImageResource(android.R.color.transparent);
            }

            int color = Color.parseColor("#000000");
            holder.mImageViewSelected.setColorFilter(0);

        } else {
            holder.mRelativeLayout.setVisibility(View.VISIBLE);
            holder.mRelativeLayoutSelected.setVisibility(View.GONE);
            holder.mTextView.setText(item.getName());

            if (item.getIcon() != null) {
                holder.mImageView.setImageDrawable(item.getIcon());
            } else {
                holder.mImageView.setImageResource(android.R.color.transparent);
            }

            int color = Color.parseColor("#AE6118");
            holder.mImageView.setColorFilter(color);
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void disable_header() {
        disableHeader = true;
    }

}