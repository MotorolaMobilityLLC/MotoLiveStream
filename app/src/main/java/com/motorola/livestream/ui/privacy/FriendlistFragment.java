package com.motorola.livestream.ui.privacy;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.Profile;
import com.motorola.livestream.R;
import com.motorola.livestream.model.fb.FriendList;
import com.motorola.livestream.model.fb.TimelinePrivacy;
import com.motorola.livestream.ui.adapter.BaseRecyclerViewAdapter;
import com.motorola.livestream.ui.adapter.FriendlistAdapter;
import com.motorola.livestream.util.FbUtil;
import com.motorola.livestream.util.Log;
import com.motorola.livestream.viewcache.ViewCacheManager;
import com.motorola.livestream.viewcache.fb.TimelinePrivacyCacheBean;

import java.util.ArrayList;
import java.util.List;

public class FriendlistFragment extends Fragment {
    private static final String LOG_TAG = "FriendlistFragment";

    private View mLoadingLayout;
    private View mEmptyLayout;
    private TextView mEmptyText;
    private RecyclerView mRecyclerView;
    private FriendlistAdapter mAdapter;

    private TimelinePrivacyCacheBean mPrivacyCacheBean = null;
    private SparseArrayCompat<Boolean> mSelectedIndex = new SparseArrayCompat<>();

    private BaseRecyclerViewAdapter.OnItemClickListener mOnItemClickListener =
            (ViewGroup parent, View view, int position) -> {
                onFriendlistItemSelected(position);
            };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FriendlistFragment() {
    }

    public static FriendlistFragment newInstance() {
        return new FriendlistFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friendlist, container, false);

        setHasOptionsMenu(true);

        mLoadingLayout = view.findViewById(R.id.loading_data);
        mEmptyLayout = view.findViewById(R.id.empty_layout);
        mEmptyText = (TextView) mEmptyLayout.findViewById(R.id.empty_textview);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.listFriendlist);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mAdapter = new FriendlistAdapter(mRecyclerView, mOnItemClickListener);
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mPrivacyCacheBean == null) {
            mPrivacyCacheBean = (TimelinePrivacyCacheBean) ViewCacheManager.getCacheFromTag(
                    ViewCacheManager.FB_TIMELINE_PRIVACY);
        }

        mLoadingLayout.setVisibility(View.VISIBLE);

        FbUtil.getFriendList(new FbUtil.OnListRetrievedListener<FriendList>() {
            @Override
            public void onSuccess(List<FriendList> dataList) {
                if (FriendlistFragment.this.getActivity() != null) {
                    mLoadingLayout.setVisibility(View.GONE);
                    if (dataList.size() > 0) {
                        mEmptyLayout.setVisibility(View.GONE);
                        mRecyclerView.setVisibility(View.VISIBLE);
                        initRecyclerViewByData(dataList);
                    } else {
                        mRecyclerView.setVisibility(View.GONE);
                        mEmptyLayout.setVisibility(View.VISIBLE);
                        mEmptyText.setText(R.string.no_friendlist_warning);
                    }
                }
            }

            @Override
            public void onError(Exception exp) {
                Log.d(LOG_TAG, "on error: " + exp);
                if (FriendlistFragment.this.getActivity() != null) {
                    mLoadingLayout.setVisibility(View.GONE);
                    mEmptyLayout.setVisibility(View.VISIBLE);
                }
            }
        }, Profile.getCurrentProfile().getId());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_select_friendlist, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.done);
        if (menuItem != null) {
            menuItem.setEnabled((mAdapter.getItemCount() > 0));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.done) {
            saveSelectedFriendList();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initRecyclerViewByData(List<FriendList> friendList) {
        List<String> selectedFriendlistIds = mPrivacyCacheBean.getPrivacyCustomFriendlist();
        mSelectedIndex.clear();
        for (String friendlisId : selectedFriendlistIds) {
            for (int i = 0, n = friendList.size(); i < n; i++) {
                if (friendlisId.equals(friendList.get(i).getId())) {
                    mSelectedIndex.put(i, Boolean.TRUE);
                    break;
                }
            }
        }

        mAdapter.setFriendlists(friendList, mSelectedIndex);
        // Disable menu item "Done" if no friend list found
        getActivity().invalidateOptionsMenu();
    }

    private void onFriendlistItemSelected(int position) {
        if (mSelectedIndex.get(position, false)) {
            mSelectedIndex.remove(position);
        } else {
            mSelectedIndex.put(position, Boolean.TRUE);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void saveSelectedFriendList() {
        int n = mSelectedIndex.size();
        if (n == 0) {
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
            return;
        }

        List<String> friendlists = new ArrayList<>(mSelectedIndex.size());
        StringBuilder sb = new StringBuilder();
        boolean deleteLastChar = false;
        for (int i = 0; i < n; i++) {
            FriendList tmpFriendlist = mAdapter.getItem(mSelectedIndex.keyAt(i));
            friendlists.add(tmpFriendlist.getId());
            sb.append(tmpFriendlist.getName()).append(",");
            deleteLastChar = true;
        }
        if (deleteLastChar) {
            sb.deleteCharAt(sb.length() - 1);
        }
        mPrivacyCacheBean.setPrivacy(TimelinePrivacy.CUSTOM);
        mPrivacyCacheBean.setPrivacyCustomFriendlist(friendlists);
        mPrivacyCacheBean.setPrivacyCustomFriendlistDisplay(sb.toString());

        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }
}
