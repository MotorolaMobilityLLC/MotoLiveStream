package com.motorola.livestream.ui.privacy;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.motorola.livestream.ui.adapter.FriendListsAdapter;
import com.motorola.livestream.util.FbUtil;
import com.motorola.livestream.util.Log;
import com.motorola.livestream.viewcache.ViewCacheManager;
import com.motorola.livestream.viewcache.fb.TimelinePrivacyCacheBean;

import java.util.ArrayList;
import java.util.List;

public class FriendListsFragment extends Fragment {
    private static final String LOG_TAG = "FriendListsFragment";

    private View mLoadingLayout;
    private View mEmptyLayout;
    private TextView mEmptyText;
    private RecyclerView mRecyclerView;
    private FriendListsAdapter mAdapter;

    private TimelinePrivacyCacheBean mPrivacyCacheBean = null;
    private final SparseArrayCompat<Boolean> mSelectedIndex = new SparseArrayCompat<>();

    private final BaseRecyclerViewAdapter.OnItemClickListener mOnItemClickListener =
            new BaseRecyclerViewAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(ViewGroup parent, View view, int position) {
                    onFriendListItemSelected(position);
                }
            };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FriendListsFragment() {
    }

    public static FriendListsFragment newInstance() {
        return new FriendListsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friendlist, container, false);

        setHasOptionsMenu(true);

        mLoadingLayout = view.findViewById(R.id.loading_data);
        mEmptyLayout = view.findViewById(R.id.empty_layout);
        mEmptyText = (TextView) mEmptyLayout.findViewById(R.id.empty_text_view);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.listFriendList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mAdapter = new FriendListsAdapter(mRecyclerView, mOnItemClickListener);
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
                if (FriendListsFragment.this.getActivity() != null) {
                    mLoadingLayout.setVisibility(View.GONE);
                    if (dataList.size() > 0) {
                        mEmptyLayout.setVisibility(View.GONE);
                        mRecyclerView.setVisibility(View.VISIBLE);
                        initRecyclerViewByData(dataList);
                    } else {
                        mRecyclerView.setVisibility(View.GONE);
                        mEmptyLayout.setVisibility(View.VISIBLE);
                        mEmptyText.setText(R.string.no_friend_list_warning);
                    }
                }
            }

            @Override
            public void onError(Exception exp) {
                Log.d(LOG_TAG, "on error: " + exp);
                if (FriendListsFragment.this.getActivity() != null) {
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
        List<String> selectedFriendListIds = mPrivacyCacheBean.getCustomFriendList();
        mSelectedIndex.clear();
        for (String friendListId : selectedFriendListIds) {
            for (int i = 0, n = friendList.size(); i < n; i++) {
                if (friendListId.equals(friendList.get(i).getId())) {
                    mSelectedIndex.put(i, Boolean.TRUE);
                    break;
                }
            }
        }

        mAdapter.setFriendLists(friendList, mSelectedIndex);
        // Disable menu item "Done" if no friend list found
        getActivity().invalidateOptionsMenu();
    }

    private void onFriendListItemSelected(int position) {
        if (mSelectedIndex.get(position, false)) {
            mSelectedIndex.remove(position);
        } else {
            mSelectedIndex.put(position, Boolean.TRUE);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void saveSelectedFriendList() {
        int selectedSize = mSelectedIndex.size();
        if (selectedSize == 0) {
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
            return;
        }

        List<String> friendLists = new ArrayList<>(selectedSize);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedSize; i++) {
            FriendList tmpFriendList = mAdapter.getItem(mSelectedIndex.keyAt(i));
            friendLists.add(tmpFriendList.getId());
            sb.append(tmpFriendList.getName())
                    .append(",");
        }
        if (selectedSize > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        mPrivacyCacheBean.setPrivacy(TimelinePrivacy.CUSTOM);
        mPrivacyCacheBean.setCustomFriendList(friendLists);
        mPrivacyCacheBean.setCustomFriendListDisplay(sb.toString());

        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }
}
