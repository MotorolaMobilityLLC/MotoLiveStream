package com.motorola.livestream.ui.adapter;

import android.content.Context;
import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRecyclerViewAdapter<T> extends
        RecyclerView.Adapter<BaseRecyclerViewAdapter.BaseRecyclerViewHolder> {

    final Context mContext;
    private List<T> mData;
    private int mItemLayoutId;
    final RecyclerView mRecyclerView;
    private OnItemClickListener mOnItemClickListener;

    private BaseRecyclerViewAdapter(RecyclerView recyclerView) {
        mContext = recyclerView.getContext();
        mRecyclerView = recyclerView;
        mData = new ArrayList<>();
    }

    BaseRecyclerViewAdapter(RecyclerView recyclerView, int layoutId, OnItemClickListener listener) {
        this(recyclerView);
        mItemLayoutId = layoutId;
        mOnItemClickListener = listener;
    }

    @Override
    public BaseRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BaseRecyclerViewHolder(
                mRecyclerView,
                LayoutInflater.from(mContext).inflate(mItemLayoutId, parent, false),
                mOnItemClickListener);
    }

    @Override
    public void onBindViewHolder(BaseRecyclerViewHolder holder, int position) {
        bindData(holder, position, getItem(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public T getItem(int position) {
        return mData.get(position);
    }

    void addData(List<T> data){
        if (data == null) {
            return;
        }
        int start = mData.size();
        mData.addAll(start, data);
    }

    public void clearData() {
        mData.clear();
    }

    protected abstract void bindData(BaseRecyclerViewHolder holder, int position, T item);

    public interface OnItemClickListener {
        void onItemClick(ViewGroup parent, View view, int position);
    }

    public static class BaseRecyclerViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        final RecyclerView mRecyclerView;
        final SparseArrayCompat<View> mViews = new SparseArrayCompat<>();
        final OnItemClickListener mOnItemClickListener;
        int mPosition;

        public BaseRecyclerViewHolder(RecyclerView recyclerView, View itemView, OnItemClickListener listener) {
            super(itemView);
            mRecyclerView = recyclerView;
            mOnItemClickListener = listener;

            itemView.setOnClickListener(this);
        }

        ImageView getImageView(int viewId) {
            return (ImageView) getView(viewId);
        }

        TextView getTextView(int viewId) {
            return (TextView) getView(viewId);
        }

        ToggleButton getCheckableView(int viewId) {
            return (ToggleButton) getView(viewId);
        }

        private View getView(int viewId) {
            View view = mViews.get(viewId);
            if (view == null) {
                view = itemView.findViewById(viewId);
                mViews.put(viewId, view);
            }
            return view;
        }

        void setPosition(int position) {
            mPosition = position;
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(mRecyclerView, itemView, mPosition);
            }
        }
    }
}
