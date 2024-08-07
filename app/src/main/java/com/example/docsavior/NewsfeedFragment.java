package com.example.docsavior;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewsfeedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewsfeedFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public NewsfeedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NewFeedFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NewsfeedFragment newInstance(String param1, String param2) {
        NewsfeedFragment fragment = new NewsfeedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_newsfeed, container, false);
    }

    // MAIN THINGS FROM HERE
    private ImageButton btnCreatePost;
    private ImageButton btnLookup;
    private ImageButton btnProfile;
    private RecyclerView lvPost;
    private NewsfeedAdapter newsFeedAdapter;
    private ArrayList<Newsfeed> newsfeedArrayList;
    private TextView tvNothing;
    private View loadingPanel;
    private NewsfeedLoader newsfeedLoader;

    private SwipeRefreshLayout srlPost;

    // this function is the same as onCreate() in Activity
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        findViewByIds();

        initVariables();

        setOnClickListeners();

        newsfeedLoader.start();
    }

    @SuppressLint("WrongViewCast")
    private void findViewByIds() {
        btnCreatePost = getView().findViewById(R.id.btnCreatePost);
        btnLookup = getView().findViewById(R.id.btnLookup);
        btnProfile = getView().findViewById(R.id.btnProfile);
        lvPost = getView().findViewById(R.id.lvPost);
        tvNothing = getView().findViewById(R.id.tvNothing);
        loadingPanel = getView().findViewById(R.id.loadingPanel);
        srlPost = getView().findViewById(R.id.srlPost);
    }

    private void setOnClickListeners() {
        btnCreatePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // open the CreatePostActivity when user click the button
                Intent myIntent = new Intent(getActivity(), CreatePostActivity.class);
                startActivity(myIntent);
                requireActivity().overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
            }
        });

        btnLookup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(getActivity(), LookUpPostUserActivity.class);
                myIntent.putExtra(ApplicationInfo.KEY_TO_LOOK_UP_POST_USER_ACTIVITY, ApplicationInfo.LOOK_UP_TYPE_POST);
                startActivity(myIntent);
                requireActivity().overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(getActivity(), ProfileActivity.class);
                myIntent.putExtra(ApplicationInfo.KEY_TO_PROFILE_ACTIVITY, ApplicationInfo.username);
                startActivity(myIntent);
                requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        srlPost.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadingPanel.setVisibility(View.VISIBLE);
                newsfeedArrayList.clear();
                newsFeedAdapter.notifyDataSetChanged();
                newsfeedLoader.interrupt();
                newsfeedLoader = new NewsfeedLoader(getActivity(), lvPost, newsfeedArrayList, newsFeedAdapter, tvNothing, loadingPanel, 0, "");
                newsfeedLoader.start();

                srlPost.setRefreshing(false);
            }
        });
    }

    private void initVariables() {
        loadingPanel.setVisibility(View.VISIBLE);
        lvPost.setVisibility(View.GONE);

        newsfeedArrayList = new ArrayList<>();
        newsFeedAdapter = new NewsfeedAdapter(getActivity(), newsfeedArrayList);

        lvPost.setHasFixedSize(true);
        lvPost.setItemViewCacheSize(20);
        newsFeedAdapter.setHasStableIds(true);

        lvPost.setAdapter(newsFeedAdapter);

        NewsfeedPreCachingLayoutManager newsfeedPreCachingLayoutManager = new NewsfeedPreCachingLayoutManager(getContext());
        lvPost.setLayoutManager(newsfeedPreCachingLayoutManager);

        newsfeedLoader = new NewsfeedLoader(getActivity(), lvPost, newsfeedArrayList, newsFeedAdapter, tvNothing, loadingPanel, 0, "");
    }
}