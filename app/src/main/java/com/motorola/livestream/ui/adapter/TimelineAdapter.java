package com.motorola.livestream.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.motorola.livestream.R;
import com.motorola.livestream.model.fb.TimelinePrivacy;
import com.motorola.livestream.viewcache.fb.TimelinePrivacyCacheBean;

import java.util.ArrayList;
import java.util.List;

public class TimelineAdapter extends BaseRecyclerViewAdapter<TimelinePrivacy> {

    private int mSelectedIndex = -1;

    public TimelineAdapter(RecyclerView recyclerView,
                           int currentSelectedIndex, OnItemClickListener listener) {
        super(recyclerView, R.layout.fragment_timeline_listitem, listener);

        mSelectedIndex = currentSelectedIndex;

        List<TimelinePrivacy> data = new ArrayList<>();
        data.add(TimelinePrivacy.PUBLIC);
        data.add(TimelinePrivacy.FRIENDS);
        data.add(TimelinePrivacy.CUSTOM);
        data.add(TimelinePrivacy.SELF);
        addData(data);
    }

    @Override
    protected void bindData(BaseRecyclerViewHolder holder, int position, TimelinePrivacy item) {
        holder.setPosition(position);

        holder.getCheckableView(R.id.checkbox_is_picked).setChecked((mSelectedIndex == position));
        holder.getImageView(R.id.item_privacy_icon)
                .setImageResource(TimelinePrivacyCacheBean.getPrivacyIcon(item));
        holder.getTextView(R.id.timeline_title)
                .setText(TimelinePrivacyCacheBean.getPrivacyTitle(item));
        holder.getTextView(R.id.timeline_subtitle)
                .setText(TimelinePrivacyCacheBean.getPrivacyDescription(item));

        ImageView rightArrow = holder.getImageView(R.id.right_arrow);
        rightArrow.setVisibility((TimelinePrivacy.CUSTOM == item)
                ? View.VISIBLE : View.GONE);
    }

    public void setSelectedIndex(int selectedIndex) {
        mSelectedIndex = selectedIndex;
        notifyDataSetChanged();
    }
}
