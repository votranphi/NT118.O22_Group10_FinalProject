package com.example.docsavior;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LookUpResultActivity extends AppCompatActivity {

    private ImageButton btnClose;
    private EditText tvLookUpContent;
    private TextView tvNothing;
    private RecyclerView lvResult;

    // use two variables below if user looks for post
    private NewsfeedAdapter newsFeedAdapter;
    private ArrayList<Newsfeed> newsfeedArrayList;

    // use two variables below if user looks for friend or chat conversation
    private FriendAdapter friendAdapter;
    private ArrayList<Friend> friendArrayList;

    private int itemType = 0; // 0 is post (item_newsfeed), 1 is chat (item_chat), 2 is user (item_friend)

    private String lookUpInfo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_look_up_result);

        findViewByIds();

        initVariables();

        setOnClickListeners();

        if (itemType == 0) {
            loadPostLookUpResult();
        } else if (itemType == 1) {
            loadChatLookUpResult();
        } else {
            loadUserLookUpResult();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(ApplicationInfo.activityLifecycleManager);
        }
    }

    private void findViewByIds() {
        btnClose = findViewById(R.id.btnClose);
        tvLookUpContent = findViewById(R.id.tvLookUpContent);
        tvNothing = findViewById(R.id.tvNothing);
        lvResult = findViewById(R.id.lvResult);
    }

    private void initVariables() {
        // retrieve the arrayList from LookUpPostUserActivity
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            ArrayList<String> temp = extras.getStringArrayList(LookUpPostUserActivity.KEY_TO_LOOK_UP_RESULT_ACTIVITY);
            lookUpInfo = temp.get(0);
            itemType = Integer.parseInt(temp.get(1));
        }

        if (itemType == 0) {
            newsfeedArrayList = new ArrayList<>();
            newsFeedAdapter = new NewsfeedAdapter(this, newsfeedArrayList);
            lvResult.setAdapter(newsFeedAdapter);
        } else if (itemType == 1) {
            friendArrayList = new ArrayList<>();
            friendAdapter = new FriendAdapter(this, friendArrayList, 1);
            lvResult.setAdapter(friendAdapter);
        } else {
            friendArrayList = new ArrayList<>();
            friendAdapter = new FriendAdapter(this, friendArrayList, 0);
            lvResult.setAdapter(friendAdapter);
        }
        lvResult.setLayoutManager(new LinearLayoutManager(LookUpResultActivity.this));

        tvLookUpContent.setText(lookUpInfo);
    }

    private void setOnClickListeners() {
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
            }
        });
    }

    /* POST LOOK UP FROM HERE */
    private void loadPostLookUpResult() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<List<Newsfeed>> call = apiService.postPostLookUp(lookUpInfo);

        call.enqueue(new Callback<List<Newsfeed>>() {
            @Override
            public void onResponse(Call<List<Newsfeed>> call, Response<List<Newsfeed>> response) {
                try {
                    if (response.isSuccessful()) {
                        List<Newsfeed> foundNewsfeeds = response.body();

                        if (foundNewsfeeds.size() == 0) {
                            tvNothing.setVisibility(View.VISIBLE);
                        } else {
                            tvNothing.setVisibility(View.GONE);

                            assignFoundNewsfeedsToListView(foundNewsfeeds);
                        }
                    } else {
                        JSONObject jsonObject = new JSONObject(response.errorBody().string());
                        Toast.makeText(LookUpResultActivity.this, jsonObject.get("detail").toString(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR601: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<List<Newsfeed>> call, Throwable t) {
                Toast.makeText(LookUpResultActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void assignFoundNewsfeedsToListView(List<Newsfeed> foundNewsfeeds) {
        for (int i = 0; i < foundNewsfeeds.size(); i++) {
            newsfeedArrayList.add(foundNewsfeeds.get(i));
            newsFeedAdapter.notifyDataSetChanged();
        }
    }

    /* CHAT LOOK UP FROM HERE */
    private void loadChatLookUpResult() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<FoundFriends> call = apiService.postLookUpFriend(ApplicationInfo.username, lookUpInfo);

        call.enqueue(new Callback<FoundFriends>() {
            @Override
            public void onResponse(Call<FoundFriends> call, Response<FoundFriends> response) {
                try {
                    if (response.isSuccessful()) {
                        FoundFriends foundFriends = response.body();

                        if (foundFriends.getFoundFriends().length == 0) {
                            tvNothing.setVisibility(View.VISIBLE);
                        } else {
                            tvNothing.setVisibility(View.GONE);

                            assignFoundFriendsToListView(foundFriends);
                        }
                    } else {
                        JSONObject jsonObject = new JSONObject(response.errorBody().string());
                        Toast.makeText(LookUpResultActivity.this, jsonObject.get("detail").toString(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR601: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<FoundFriends> call, Throwable t) {
                Toast.makeText(LookUpResultActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void assignFoundFriendsToListView(FoundFriends foundFriends) {
        try {
            for (int i = 0; i < foundFriends.getFoundFriends().length; i++) {
                getAvatarDataThenAddToArrayList(foundFriends.getFoundFriends()[i]);
            }
        } catch (Exception ex) {
            Log.e("ERROR2: ", ex.getMessage());
        }
    }

    private void getAvatarDataThenAddToArrayList(String username) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Detail> call = apiService.getAvatarData(username);

        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(Call<Detail> call, Response<Detail> response) {
                try {
                    if (response.isSuccessful()) {
                        Friend friend = new Friend(response.body().getDetail(), username);
                        friendArrayList.add(friend);
                        friendAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(LookUpResultActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR106: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(LookUpResultActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* USER LOOK UP FROM HERE */
    private void loadUserLookUpResult() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<List<User>> call = apiService.postUserLookUp(lookUpInfo);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                try {
                    if (response.isSuccessful()) {
                        List<User> foundUsers = response.body();

                        if (foundUsers.size() == 0) {
                            tvNothing.setVisibility(View.VISIBLE);
                        } else {
                            tvNothing.setVisibility(View.GONE);

                            assignFoundUsersToListView(foundUsers);
                        }
                    } else {
                        JSONObject jsonObject = new JSONObject(response.errorBody().string());
                        Toast.makeText(LookUpResultActivity.this, jsonObject.get("detail").toString(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR106: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(LookUpResultActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void assignFoundUsersToListView(List<User> foundUsers) {
        for (int i = 0; i < foundUsers.size(); i++) {
            Friend friend = new Friend(foundUsers.get(i).getAvatarData(), foundUsers.get(i).getUsername());
            friendArrayList.add(friend);
            friendAdapter.notifyDataSetChanged();
        }
    }
}