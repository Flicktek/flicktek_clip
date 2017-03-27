package com.flicktek.clip.menus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
public class MenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "MenuAdapter";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<AppModel> mDataset;
    MainActivity main;
    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;

    public boolean hasHeader = false;
    public boolean disableHeader = false;
    private int selectedItem = 0;

    public static Typeface mainFont;

    public class ViewOptimized  {
        public RelativeLayout mRelativeLayout;
        public TextView mTextView;
        public TextView mTextViewSmall;
        public ImageView mImageView;
    }

    public class ViewHeader extends RecyclerView.ViewHolder {
        public ImageView mImageViewHeader;
        public ViewHeader(View view) {
            super(view);
            mImageViewHeader = (ImageView) view.findViewById(R.id.iv_header);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case

        ViewOptimized mSelected;
        ViewOptimized mNotSelected;

        public ViewHolder(View view) {
            super(view);
            int color;

            ViewOptimized n_sel = new ViewOptimized();
            n_sel.mRelativeLayout = (RelativeLayout) view.findViewById(R.id.rl_item_menu);
            n_sel.mRelativeLayout.setBackgroundResource(R.drawable.bg_dark);

            n_sel.mTextView = (TextView) view.findViewById(R.id.tv_item_label);
            n_sel.mTextView.setTypeface(mainFont);
            n_sel.mTextView.setTextColor(Color.WHITE);

            n_sel.mTextViewSmall = (TextView) view.findViewById(R.id.tv_item_label_small);
            n_sel.mTextViewSmall.setTypeface(mainFont);
            n_sel.mTextViewSmall.setTextColor(Color.WHITE);

            color = Color.parseColor("#AE6118");
            n_sel.mImageView = (ImageView) view.findViewById(R.id.iv_item_icon);
            n_sel.mImageView.setColorFilter(color);

            mNotSelected = n_sel;
            
            //---- SELECTED ----
            ViewOptimized sel = new ViewOptimized();

            sel.mRelativeLayout = (RelativeLayout) view.findViewById(R.id.rl_item_menu_selected);
            sel.mRelativeLayout.setBackgroundResource(R.drawable.bg_light);

            sel.mTextView = (TextView) view.findViewById(R.id.tv_item_label_selected);
            sel.mTextView.setTypeface(mainFont);
            sel.mTextView.setTextColor(Color.BLACK);

            sel.mTextViewSmall = (TextView) view.findViewById(R.id.tv_item_label_small_selected);
            sel.mTextViewSmall.setTypeface(mainFont);
            sel.mTextViewSmall.setTextColor(Color.BLACK);

            color = Color.parseColor("#000000");
            sel.mImageView = (ImageView) view.findViewById(R.id.iv_item_icon_selected);
            sel.mImageView.setColorFilter(color);

            mSelected = sel;

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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        View view;
        if (viewType == TYPE_HEADER) {
            //inflate your layout and pass it to view holder
            int res_header = R.layout.item_menu_header_rect;
            if (MainActivity.isRound)
                res_header = R.layout.item_menu_header_round;

            view = LayoutInflater.from(parent.getContext()).inflate(res_header, parent, false);
            ViewHeader vh = new ViewHeader(view);
            return vh;
        }

        if (viewType != TYPE_ITEM)
            Log.e(TAG, "Not the right type");

        int resource = R.layout.item_menu_rect;
        if (MainActivity.isRound)
            resource = R.layout.item_menu_round;

        view = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);

        ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    private boolean isPositionHeader(int position) {
        // Header only on 0 at the moment. We can check on the object if it is a header
        if (hasHeader && position == 0)
            return true;

        return false;
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder_item, int position) {
        AppModel item = mDataset.get(position);
        if (holder_item instanceof ViewHeader) {
            ViewHeader header = (ViewHeader) holder_item;
            header.mImageViewHeader.setImageDrawable(item.getIcon());
            return;
        }

        ViewHolder holder = (ViewHolder) holder_item;

        ViewOptimized selected = holder.mSelected;
        ViewOptimized n_active = holder.mNotSelected;

        holder.itemView.setSelected(selectedItem == position);

        String data = item.getDataKey();
        if (data != null) {
            String info = FlicktekSettings.getInstance().getString(data, "Not Available");
            n_active.mTextViewSmall.setVisibility(View.VISIBLE);
            n_active.mTextViewSmall.setText(info);
        }

        if (item.isSelected()) {
            n_active.mRelativeLayout.setVisibility(View.GONE);

            selected.mRelativeLayout.setVisibility(View.VISIBLE);
            selected.mTextView.setText(item.getName());

            if (item.getIcon() != null) {
                selected.mImageView.setImageDrawable(item.getIcon());
            } else {
                selected.mImageView.setImageResource(android.R.color.transparent);
            }

            int color = Color.parseColor("#000000");
            selected.mImageView.setColorFilter(color);

        } else {
            selected.mRelativeLayout.setVisibility(View.GONE);

            n_active.mRelativeLayout.setVisibility(View.VISIBLE);
            n_active.mTextView.setText(item.getName());

            if (item.getIcon() != null) {
                n_active.mImageView.setImageDrawable(item.getIcon());
            } else {
                n_active.mImageView.setImageResource(android.R.color.transparent);
            }

            int color = Color.parseColor("#AE6118");
            n_active.mImageView.setColorFilter(color);
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