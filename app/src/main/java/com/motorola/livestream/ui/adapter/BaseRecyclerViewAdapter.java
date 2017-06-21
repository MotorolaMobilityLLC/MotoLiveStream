package com.motorola.livestream.ui.adapter;

import android.content.Context;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;
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

    protected Context mContext;
    protected List<T> mData;
    protected int mItemLayoutId;
    protected RecyclerView mRecyclerView;
    private OnItemClickListener mOnItemClickListener;

    private BaseRecyclerViewAdapter(RecyclerView recyclerView) {
        mContext = recyclerView.getContext();
        mRecyclerView = recyclerView;
        mData = new ArrayList<>();
    }

    public BaseRecyclerViewAdapter(RecyclerView recyclerView, int layoutId, OnItemClickListener listener) {
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

    protected void addData(List<T> data){
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

        protected RecyclerView mRecyclerView;
        protected final SparseArrayCompat<View> mViews = new SparseArrayCompat<View>();
        protected OnItemClickListener mOnItemClickListener;
        protected int mPosition;

        public BaseRecyclerViewHolder(RecyclerView recylerView, View itemView, OnItemClickListener listener) {
            super(itemView);
            mRecyclerView = recylerView;
            mOnItemClickListener = listener;

            itemView.setOnClickListener(this);
        }

        protected ImageView getImageView(int viewId) {
            return (ImageView) getView(viewId);
        }

        protected TextView getTextView(int viewId) {
            return (TextView) getView(viewId);
        }

        protected ToggleButton getCheckableView(int viewId) {
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

        protected void setPosition(int position) {
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
