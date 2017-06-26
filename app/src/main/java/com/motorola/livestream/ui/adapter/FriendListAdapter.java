package com.motorola.livestream.ui.adapter;

import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;

import com.motorola.livestream.R;
import com.motorola.livestream.model.fb.FriendList;

import java.util.List;

public class FriendListAdapter extends BaseRecyclerViewAdapter<FriendList> {

    private SparseArrayCompat<Boolean> mSelectedIndex;

    public FriendListAdapter(RecyclerView recyclerView, OnItemClickListener listener) {
        super(recyclerView, R.layout.fragment_friendlist_item, listener);
        mSelectedIndex = new SparseArrayCompat<>();
    }

    @Override
    protected void bindData(BaseRecyclerViewHolder holder, int position, FriendList item) {
        holder.setPosition(position);
        holder.getCheckableView(R.id.checkbox_is_picked)
                .setChecked(mSelectedIndex.get(position, false));
        holder.getTextView(R.id.friend_list_title).setText(item.getName());
    }

    public void setFriendLists(List<FriendList> friendLists,
                               SparseArrayCompat<Boolean> selectedValues) {
        addData(friendLists);

        mSelectedIndex.clear();
        mSelectedIndex = selectedValues;

        notifyDataSetChanged();
    }
}
