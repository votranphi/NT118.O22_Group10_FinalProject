package com.example.docsavior;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NewsfeedLoader extends Thread {

    private Context context;
    private RecyclerView lvPost;
    private ArrayList<NewsFeed> newsFeedArrayList;
    private NewsFeedAdapter newsFeedAdapter;
    private TextView tvNothing;
    private Map<Integer, NewsFeed> postCache = new HashMap<>();
    private int numberOfPost;
    private int page; // current page (start from 0)
    private final int PAGE_SIZE = 1; // page size (load PAGE_SIZE post after scroll to the bottom of the ListView)
    private final int NUMBER_OF_POST_LOADED_FIRST = 3; // the number of posts will be loaded to the screen first time user enters the newsfeed screen

    public NewsfeedLoader(Context context, RecyclerView lvPost, ArrayList<NewsFeed> newsFeedArrayList, NewsFeedAdapter newsFeedAdapter, TextView tvNothing) {
        super();
        this.context = context;
        this.lvPost = lvPost;
        this.newsFeedArrayList = newsFeedArrayList;
        this.newsFeedAdapter = newsFeedAdapter;
        this.tvNothing = tvNothing;
        this.page = NUMBER_OF_POST_LOADED_FIRST;
    }

    @Override
    public void run() {
        getNumberOfPosts();

        loadPostForTheFirstTime();

        setOnClickListeners();
    }

    private void loadPostForTheFirstTime() {
        for (int i = 0; i < NUMBER_OF_POST_LOADED_FIRST; i++) {
            getAPost(i, PAGE_SIZE);
        }
    }

    private void getNumberOfPosts()
    {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Integer> call = apiService.getNumberOfPosts();
        call.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                try{
                    if(response.isSuccessful())
                    {
                        numberOfPost = response.body().intValue();
                    }
                }
                catch (Exception t)
                {
                    Log.d("Error: ", t.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int lastScrollY = 0;
    private boolean isScrollingUp = false;

    private void setOnClickListeners() {
        lvPost.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Update the scroll direction
                isScrollingUp = dy < 0;
                lastScrollY += dy;
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && isScrollingUp && page < numberOfPost) {
                    getAPost(page, PAGE_SIZE);
                }
            }
        });
    }


    private void getAPost(int number, int pageSize) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<List<NewsFeed>> call = apiService.getSequenceOfPost(number, pageSize);

        call.enqueue(new Callback<List<NewsFeed>>() {
            @Override
            public void onResponse(Call<List<NewsFeed>> call, Response<List<NewsFeed>> response) {
                try {
                    if (response.isSuccessful()) {
                        List<NewsFeed> responseList = response.body();

                        // add the elements in responseList to newsFeedArrayList
                        for (NewsFeed i : responseList) {
                            if(!postCache.containsKey(i.getId()))
                            {
                                newsFeedArrayList.add(i);
                                // update the ListView every one post
                                newsFeedAdapter.notifyDataSetChanged();
                                page++;
                                postCache.put(i.getId(), i);
                            }
                        }

                        // set the visibility of "NOTHING TO SHOW" to GONE
                        tvNothing.setVisibility(View.GONE);
                    } else if (response.code() == 600) {
                        // set the visibility of "NOTHING TO SHOW" to VISIBLE
                        tvNothing.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(context, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR100: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<List<NewsFeed>> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
