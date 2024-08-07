package com.example.docsavior;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

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
    private ArrayList<Newsfeed> newsfeedArrayList;
    private NewsfeedAdapter newsFeedAdapter;
    private TextView tvNothing;
    private View loadingPanel;
    private Boolean isLoading = false; // check if app is calling api
    private int numberOfPost; // total posts in database
    private int page = 0;
    private final int PAGE_SIZE = 3; // page size (load PAGE_SIZE post after scroll to the bottom of the ListView)

    private int loadType = 0; // 0 is in NewsfeedFragment, 1 is in LookUpResultActivity for post type, 2 is in ProfileActivity
    private String passedData = ""; // with loadType == 0 -> passedData can be anything, loadType == 1 -> passedData is lookUpInfo, loadType == 2 -> passedData is username

    public NewsfeedLoader(Context context, RecyclerView lvPost, ArrayList<Newsfeed> newsfeedArrayList, NewsfeedAdapter newsFeedAdapter, TextView tvNothing, View loadingPanel, int loadType, String passedData) {
        super();
        this.context = context;
        this.lvPost = lvPost;
        this.newsfeedArrayList = newsfeedArrayList;
        this.newsFeedAdapter = newsFeedAdapter;
        this.tvNothing = tvNothing;
        this.loadingPanel = loadingPanel;
        this.loadType = loadType;
        this.passedData = passedData;
    }

    @Override
    public void run() {
        getNumberOfPosts(); // get total posts

        loadPostForTheFirstTime();

        setOnClickListeners();
    }

    private void loadPostForTheFirstTime() {
        if (loadType == 0) {
            getSequenceOfNewsfeedPost(0, PAGE_SIZE);
        } else if (loadType == 1) {
            getSequenceOfFoundPost(0, PAGE_SIZE);
        } else { // if (loadType == 2)
            getSequenceOfMyPost(0, PAGE_SIZE);
        }
    }

    private void getNumberOfPosts()
    {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Integer> call = null;
        if (loadType == 0) {
            call = apiService.getNumberOfPosts();
        } else if (loadType == 1) {
            call = apiService.getNumberOfFoundPosts(passedData);
        } else { // if (loadType == 2)
            call = apiService.getNumberOfMyPosts(passedData);
        }

        call.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                try{
                    if(response.isSuccessful())
                    {
                        numberOfPost = response.body().intValue();
                        if(numberOfPost==0)
                        {
                            tvNothing.setVisibility(View.VISIBLE);
                            loadingPanel.setVisibility(View.GONE);
                        }
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

    private void setOnClickListeners() {
        lvPost.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(!recyclerView.canScrollVertically(1 )&& (newsfeedArrayList.size())<numberOfPost) // scroll to the end and still has posts to load
                {
                    loadingPanel.setVisibility(View.VISIBLE);
                }
                if(!recyclerView.canScrollVertically(1 )&& (newsfeedArrayList.size())==numberOfPost && newState == RecyclerView.SCROLL_STATE_IDLE) // scroll to the end and there is no post
                {
                    Toast.makeText(context, "There is no more post to load", Toast.LENGTH_SHORT).show();
                }
                if (/*!recyclerView.canScrollVertically(0) &&*/ !isLoading && page <= (numberOfPost / PAGE_SIZE) + 1 && newState == RecyclerView.SCROLL_STATE_DRAGGING) // scroll and load posts
                {
                    isLoading = true; // app is calling api
                    if (loadType == 0) {
                        getSequenceOfNewsfeedPost(page, PAGE_SIZE);
                    } else if (loadType == 1) {
                        getSequenceOfFoundPost(page, PAGE_SIZE);
                    } else { // if (loadType == 2)
                        getSequenceOfMyPost(page, PAGE_SIZE);
                    }
                }
            }
        });
    }

    private void getSequenceOfNewsfeedPost(int number, int pageSize) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<List<Newsfeed>> call = apiService.getSequenceOfPost(number, pageSize);

        call.enqueue(new Callback<List<Newsfeed>>() {
            @Override
            public void onResponse(Call<List<Newsfeed>> call, Response<List<Newsfeed>> response) {
                try {
                    if (response.isSuccessful()) {
                        List<Newsfeed> responseList = response.body();
                        page++; // if response successfully, page increase one more
                        // add the elements in responseList to newsfeedArrayList
                        for (Newsfeed i : responseList) {
                            newsfeedArrayList.add(i);

                            // update the ListView every one post
                            newsFeedAdapter.notifyItemInserted(newsfeedArrayList.size() - 1);
                        }

                        // set the visibility of "NOTHING TO SHOW" to GONE
                        lvPost.setVisibility(View.VISIBLE);
                        loadingPanel.setVisibility(View.GONE);
                        tvNothing.setVisibility(View.GONE);
                    } else if (response.code() == 600) {
                        // do nothing
                    } else {
                        Toast.makeText(context, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                    isLoading = false; // after calling api, set isLoading to false
                } catch (Exception ex) {
                    Log.e("ERROR100: ", ex.getMessage());
                    isLoading = false; // after calling api, set isLoading to false
                }
            }

            @Override
            public void onFailure(Call<List<Newsfeed>> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                isLoading = false; // after calling api, set isLoading to false
            }
        });
    }

    /* POST LOOK UP FROM HERE */
    private void getSequenceOfFoundPost(int number, int pageSize) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<List<Newsfeed>> call = apiService.postPostLookUp(passedData, number, pageSize);

        call.enqueue(new Callback<List<Newsfeed>>() {
            @Override
            public void onResponse(Call<List<Newsfeed>> call, Response<List<Newsfeed>> response) {
                try {
                    if (response.isSuccessful()) {
                        List<Newsfeed> foundNewsfeeds = response.body();
                        page++;
                        if (foundNewsfeeds.size() == 0) {
                            // tvNothing.setVisibility(View.VISIBLE);
                            loadingPanel.setVisibility(View.GONE);
                        } else {
                            // tvNothing.setVisibility(View.GONE);
                            loadingPanel.setVisibility(View.GONE);
                            assignFoundNewsfeedsToListView(foundNewsfeeds);
                        }
                    } else {
                        JSONObject jsonObject = new JSONObject(response.errorBody().string());
                        Toast.makeText(context, jsonObject.get("detail").toString(), Toast.LENGTH_SHORT).show();
                    }
                    isLoading = false;
                } catch (Exception ex) {
                    Log.e("ERROR601: ", ex.getMessage());
                    isLoading = false;
                }
            }

            @Override
            public void onFailure(Call<List<Newsfeed>> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                isLoading = false;
            }
        });
    }

    private void assignFoundNewsfeedsToListView(List<Newsfeed> foundNewsfeeds) {
        for (int i = 0; i < foundNewsfeeds.size(); i++) {
            newsfeedArrayList.add(foundNewsfeeds.get(i));
            newsFeedAdapter.notifyItemInserted(newsfeedArrayList.size() - 1);
        }
    }

    /* GET USER'S POST FROM HERE */
    private void getSequenceOfMyPost(int number, int pageSize) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<List<Newsfeed>> call = apiService.getMyPost(passedData, number, pageSize);

        call.enqueue(new Callback<List<Newsfeed>>() {
            @Override
            public void onResponse(Call<List<Newsfeed>> call, Response<List<Newsfeed>> response) {
                try {
                    if (response.isSuccessful()) {
                        List<Newsfeed> responseList = response.body();
                        page++;
                        // add the elements in responseList to newsfeedArrayList
                        for (Newsfeed i : responseList) {
                            newsfeedArrayList.add(i);
                            // update the ListView every one post
                            newsFeedAdapter.notifyItemInserted(newsfeedArrayList.size() - 1);
                        }
                        loadingPanel.setVisibility(View.GONE);

                        // set the visibility of "NOTHING TO SHOW" to GONE
                        // tvNothing.setVisibility(View.GONE);
                    } else if (response.code() == 600) {
                        loadingPanel.setVisibility(View.GONE);
                        // set the visibility of "NOTHING TO SHOW" to VISIBLE
                        // tvNothing.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(context, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                    isLoading = false;
                } catch (Exception ex) {
                    Log.e("ERROR100: ", ex.getMessage());
                    isLoading = false;
                }
            }

            @Override
            public void onFailure(Call<List<Newsfeed>> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                isLoading = false;
            }
        });
    }
}
