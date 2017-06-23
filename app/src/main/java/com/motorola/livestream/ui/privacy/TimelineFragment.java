package com.motorola.livestream.ui.privacy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.motorola.livestream.R;
import com.motorola.livestream.model.fb.TimelinePrivacy;
import com.motorola.livestream.ui.adapter.BaseRecyclerViewAdapter;
import com.motorola.livestream.ui.adapter.TimelineAdapter;
import com.motorola.livestream.util.Log;
import com.motorola.livestream.viewcache.ViewCacheManager;
import com.motorola.livestream.viewcache.fb.TimelinePrivacyCacheBean;

import static android.app.Activity.RESULT_OK;

public class TimelineFragment extends Fragment {

    private static final String LOG_TAG = "TimelineFragment";

    private static final int REQUEST_CUSTOM_FRIEND_LIST = 0x101;

    private TimelinePrivacyCacheBean mTimelinePrivacyCacheBean;
    private TimelineAdapter mAdapter;

    private TimelineFragment.OnListFragmentDoneListener mListener;

    private final BaseRecyclerViewAdapter.OnItemClickListener mOnItemClickListener =
            (ViewGroup parent, View view, int position) -> {

                mAdapter.setSelectedIndex(position);

                TimelinePrivacy newPrivacy = mAdapter.getItem(position);
                if (TimelinePrivacy.CUSTOM == newPrivacy) {
                    startActivityForResult(new Intent(getActivity(), FriendListActivity.class),
                            REQUEST_CUSTOM_FRIEND_LIST);
                } else {
                    mTimelinePrivacyCacheBean.setPrivacy(newPrivacy);
                    mListener.onListFragmentDone();
                }
            };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TimelineFragment() {
    }

    public static Fragment newInstance() {
        return new TimelineFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        RecyclerView mRecyclerView = (RecyclerView) view.findViewById(R.id.listTimeline);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        if (mTimelinePrivacyCacheBean == null) {
            mTimelinePrivacyCacheBean = (TimelinePrivacyCacheBean) ViewCacheManager
                    .getCacheFromTag(ViewCacheManager.FB_TIMELINE_PRIVACY);
        }
        mAdapter = new TimelineAdapter(mRecyclerView,
                mTimelinePrivacyCacheBean.getCurrentPrivacyIndex(), mOnItemClickListener);
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TimelineFragment.OnListFragmentDoneListener) {
            mListener = (TimelineFragment.OnListFragmentDoneListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CUSTOM_FRIEND_LIST == requestCode) {
            if (resultCode == RESULT_OK) {
                Log.d(LOG_TAG, "Custom friend list set to: "
                        + mTimelinePrivacyCacheBean.getCustomFriendListDisplay());
                mListener.onListFragmentDone();
            } else {
                mAdapter.setSelectedIndex(mTimelinePrivacyCacheBean.getCurrentPrivacyIndex());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentDoneListener {
        void onListFragmentDone();
    }
}
