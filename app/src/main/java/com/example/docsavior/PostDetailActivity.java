package com.example.docsavior;

import static com.example.docsavior.ApplicationInfo.username;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PostDetailActivity extends AppCompatActivity {

    private ImageButton btnClose;
    private ImageView profileImg;
    private TextView tvUsername;
    private TextView tvPostDesciption;
    private TextView tvPostContent;
    private ImageButton btnLike;
    private ImageButton btnDislike;
    private ImageButton btnDownload;
    private ListView lvComment;
    private EditText edComment;
    private ImageButton btnPost;
    private TextView tvNothing;

    private Boolean isLiked;
    private Boolean isDisliked;
    private PostDetailAdapter postDetailAdapter;
    private ArrayList<PostDetail> postDetailArrayList;

    private ImageView imgPost;
    private TextView tvDocumentName;

    private Newsfeed newsFeed = null;

    private TextView tvDateTime;
    private View loadingPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        findViewByIds();

        setOnClickListeners();

        initVariables();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(ApplicationInfo.activityLifecycleManager);
        }
    }

    private void findViewByIds() {
        btnClose = findViewById(R.id.btnClose);
        profileImg = findViewById(R.id.profileImg);
        tvUsername = findViewById(R.id.tvUsername);
        tvPostDesciption = findViewById(R.id.tvPostDesciption);
        tvPostContent = findViewById(R.id.tvPostContent);
        btnLike = findViewById(R.id.btnLike);
        btnDislike = findViewById(R.id.btnDislike);
        btnDownload = findViewById(R.id.btnDownload);
        lvComment = findViewById(R.id.lvComment);
        edComment = findViewById(R.id.edComment);
        btnPost = findViewById(R.id.btnPost);
        tvNothing = findViewById(R.id.tvNothing);
        isLiked = false;
        isDisliked = false;
        imgPost = findViewById(R.id.imgPost);
        tvDocumentName = findViewById(R.id.tvDocumentName);
        tvDateTime = findViewById(R.id.tvDateTime);
        loadingPanel = findViewById(R.id.loadingPanel);
    }

    private void checkInteract()
    {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(ApplicationInfo.apiPath).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<Detail> call = apiService.getInteract(username, newsFeed.getId());
        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(Call<Detail> call, Response<Detail> response) {
                try {
                    if (response.isSuccessful())
                    {
                        String res = response.body().getDetail();
                        if(res.equals("like"))
                        {
                            btnLike.setImageResource(R.drawable.like_icon_red);
                            isLiked = true;
                            // add animation change from like to unlike
                        } else if (res.equals("dislike"))
                        {
                            btnDislike.setImageResource(R.drawable.dislike_icon_red);
                            isDisliked = true;
                            // add animation change from dislike to undislike
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(), response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                }
                catch (Throwable t)
                {
                    Log.e("ERROR100: ", t.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setOnClickListeners() {
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
            }
        });

        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(PostDetailActivity.this, ProfileActivity.class);
                myIntent.putExtra(ApplicationInfo.KEY_TO_PROFILE_ACTIVITY, newsFeed.getUsername());
                startActivity(myIntent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        lvComment.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (!postDetailArrayList.get(position).getUsername().equals(username)) {
                    Toast.makeText(PostDetailActivity.this, "You cannot delete other's comment!", Toast.LENGTH_SHORT).show();
                    return false;
                }

                if (postDetailArrayList.size() == 1) {
                    tvNothing.setVisibility(View.VISIBLE);
                }

                // call API to delete comment
                deleteComment(postDetailArrayList.get(position).getIdComment());

                // call API to delete post's comment notification
                deleteNotification(newsFeed.getUsername(), 2, newsFeed.getId(), username);

                // call API to decrease comment number
                postDecreaseCommentNumber(newsFeed.getId());

                // delete item from ListView
                postDetailArrayList.remove(position);
                postDetailAdapter.notifyDataSetChanged();

                return false;
            }
        });

        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edComment.getText().toString().isEmpty()) {
                    Toast.makeText(PostDetailActivity.this, "Please enter your comment!", Toast.LENGTH_LONG).show();
                    return;
                }

                btnPost.setClickable(false);
                btnPost.setBackgroundTintList(getResources().getColorStateList(R.color.grey));

                if (postDetailArrayList.isEmpty()) {
                    tvNothing.setVisibility(View.GONE);
                }

                // call API to upload comment to database
                postComment(edComment.getText().toString());

                // post notification if user doesn't comment on their own post
                if (!newsFeed.getUsername().equals(username)) {
                    // call api to post the comment notification
                    postNotification(newsFeed.getUsername(), 2, newsFeed.getId(), username);
                }

                // call API to increase comment number
                postIncreaseCommentNumber(newsFeed.getId());
            }
        });

        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Retrofit retrofit = new Retrofit.Builder().baseUrl(ApplicationInfo.apiPath).addConverterFactory(GsonConverterFactory.create()).build();
                ApiService apiService = retrofit.create(ApiService.class);
                if(isLiked) // if already liked
                {
                    // call api to delete the like notification
                    deleteNotification(newsFeed.getUsername(), 0, newsFeed.getId(), username);

                    isLiked = false;
                    btnLike.setImageResource(R.drawable.like_icon);
                    Call<Detail> callNewsfeed = apiService.postUnlike(newsFeed.getId());
                    callNewsfeed.enqueue(new Callback<Detail>() {
                        @Override
                        public void onResponse(Call<Detail> call, Response<Detail> response) {
                            try {
                                if (response.isSuccessful())
                                {
                                    //Toast.makeText(context, "Unlike successfully!", Toast.LENGTH_LONG).show();
                                }
                                else {
                                    Toast.makeText(getApplicationContext(), response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                                }
                            }
                            catch (Throwable t)
                            {
                                Log.e("ERROR100: ", t.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(Call<Detail> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else
                {
                    if(isDisliked)
                    {
                        Toast.makeText(getApplicationContext(), "Cannot like and dislike at a same time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // post notification if user doesn't like their own post
                    if (!newsFeed.getUsername().equals(username)) {
                        // call api to post the like notification
                        postNotification(newsFeed.getUsername(), 0, newsFeed.getId(), username);
                    }

                    btnLike.setImageResource(R.drawable.like_icon_red);
                    isLiked = true;
                    Call<Detail> callNewsfeed = apiService.postLike(newsFeed.getId());
                    callNewsfeed.enqueue(new Callback<Detail>() {
                        @Override
                        public void onResponse(Call<Detail> call, Response<Detail> response) {
                            try {
                                if (response.isSuccessful())
                                {
                                    //Toast.makeText(context, "Like successfully!", Toast.LENGTH_LONG).show();
                                }
                                else {
                                    Toast.makeText(getApplicationContext(), response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                                }
                            }
                            catch (Throwable t)
                            {
                                Log.e("ERROR100: ", t.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(Call<Detail> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                Call<Detail> callUserInteract = apiService.postInteract(username,newsFeed.getId(), true);
                callUserInteract.enqueue(new Callback<Detail>() {
                    @Override
                    public void onResponse(Call<Detail> call, Response<Detail> response) {
                        try {
                            if (response.isSuccessful())
                            {
                                //Toast.makeText(context, "user_interact_like successfully!", Toast.LENGTH_LONG).show();
                            }
                            else {
                                Toast.makeText(getApplicationContext(), response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                            }
                        }
                        catch (Throwable t)
                        {
                            Log.e("ERROR100: ", t.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<Detail> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnDislike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Retrofit retrofit = new Retrofit.Builder().baseUrl(ApplicationInfo.apiPath).addConverterFactory(GsonConverterFactory.create()).build();

                ApiService apiService = retrofit.create(ApiService.class);

                if(isDisliked)
                {
                    // call api to delete the dislike notification
                    deleteNotification(newsFeed.getUsername(), 1, newsFeed.getId(), username);

                    isDisliked = false;
                    btnDislike.setImageResource(R.drawable.dislike_icon);
                    Call<Detail> callNewsfeed = apiService.postUndislike(newsFeed.getId());
                    callNewsfeed.enqueue(new Callback<Detail>() {
                        @Override
                        public void onResponse(Call<Detail> call, Response<Detail> response) {
                            try {
                                if (response.isSuccessful())
                                {
                                    //Toast.makeText(context, "Undislike successfully!", Toast.LENGTH_LONG).show();
                                }
                                else {
                                    Toast.makeText(getApplicationContext(), response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                                }
                            }
                            catch (Throwable t)
                            {
                                Log.e("ERROR100: ", t.getMessage());
                            }
                        }
                        @Override
                        public void onFailure(Call<Detail> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else
                {
                    if (isLiked)
                    {
                        Toast.makeText(getApplicationContext(), "Cannot like and dislike at a same time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // post notification if user doesn't dislike their own post
                    if (newsFeed.getUsername().equals(username)) {
                        // call api to post the dislike notification
                        postNotification(newsFeed.getUsername(), 1, newsFeed.getId(), username);
                    }

                    isDisliked=true;
                    btnDislike.setImageResource(R.drawable.dislike_icon_red);
                    Call<Detail> callNewsfeed = apiService.postDislike(newsFeed.getId());
                    callNewsfeed.enqueue(new Callback<Detail>() {
                        @Override
                        public void onResponse(Call<Detail> call, Response<Detail> response) {
                            try {
                                if (response.isSuccessful())
                                {
                                    //Toast.makeText(context, "Disike successfully!", Toast.LENGTH_LONG).show();
                                }
                                else {
                                    Toast.makeText(getApplicationContext(), response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                                }
                            }
                            catch (Throwable t)
                            {
                                Log.e("ERROR100: ", t.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(Call<Detail> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }
                Call<Detail> callUserInteract = apiService.postInteract(username ,newsFeed.getId(), false);
                callUserInteract.enqueue(new Callback<Detail>() {
                    @Override
                    public void onResponse(Call<Detail> call, Response<Detail> response) {
                        try {
                            if (response.isSuccessful())
                            {
                                //Toast.makeText(context, "user_interact_dislike successfully!", Toast.LENGTH_LONG).show();
                            }
                            else {
                                Toast.makeText(getApplicationContext(), response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                            }
                        }
                        catch (Throwable t)
                        {
                            Log.e("ERROR100: ", t.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<Detail> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FileDownloader(PostDetailActivity.this, newsFeed).execute();
            }
        });
    }

    private void initVariables() {
        postDetailArrayList = new ArrayList<>();
        postDetailAdapter = new PostDetailAdapter(this, R.layout.item_post_detail, postDetailArrayList);
        lvComment.setAdapter(postDetailAdapter);

        // retrieve the post's id from NewsfeedAdapter
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            int postId = extras.getInt(ApplicationInfo.KEY_TO_POST_DETAIL_ACTIVITY);

            // call API to get the newsfeed info again, because cannot put from previous activity
            getNewsfeedById(postId);
        }
    }

    private void loadPostInfo() {
        // set those TextViews
        tvUsername.setText(newsFeed.getUsername());
        tvPostDesciption.setText(newsFeed.getPostDescription());
        tvPostContent.setText(newsFeed.getPostContent());
        tvDocumentName.setText(newsFeed.getFileName() + "." + newsFeed.getFileExtension());

        // load the image if the post is image
        if (newsFeed.getFileExtension().equals("jpg") || newsFeed.getFileExtension().equals("png") || newsFeed.getFileExtension().equals("jpeg")) {
            setImage(imgPost, newsFeed.getFileData(), true);
        } else {
            imgPost.setVisibility(View.GONE);
        }

        // load the post's admin avatar
        getAndSetPostAdminAvatar(profileImg, newsFeed.getUsername());
    }

    private void getNewsfeedById(int id) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Newsfeed> call = apiService.getNewsfeedById(id);

        call.enqueue(new Callback<Newsfeed>() {
            @Override
            public void onResponse(Call<Newsfeed> call, Response<Newsfeed> response) {
                try {
                    if (response.isSuccessful()) {
                        newsFeed = response.body();

                        // load post info after newsFeed is completely initialized
                        loadPostInfo();

                        // load post comment after newsFeed is completely initialized
                        loadPostComments();

                        // load the interact information
                        checkInteract();

                        // set the post's datetime
                        setPostDateTime(tvDateTime, newsFeed.getTime());
                    } else {
                        Toast.makeText(PostDetailActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR100: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Newsfeed> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPostComments() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<List<Comment>> call = apiService.getPostComment(newsFeed.getId());

        call.enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                try {
                    if (response.isSuccessful()) {
                        List<Comment> comments = response.body();

                        if (comments.size() == 0) {
                            tvNothing.setVisibility(View.VISIBLE);
                            loadingPanel.setVisibility(View.GONE);
                        } else {
                            tvNothing.setVisibility(View.GONE);
                            loadingPanel.setVisibility(View.GONE);
                            for (Comment i : comments) {
                                // call API to get avatarData
                                PostDetail postDetail = new PostDetail(i.getIdComment(), i.getUsername(), i.getCommentContent(), i.getTime());
                                postDetailArrayList.add(postDetail);
                                postDetailAdapter.notifyDataSetChanged();
                            }
                        }
                    } else {
                        Toast.makeText(PostDetailActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR567: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAndSetPostAdminAvatar(ImageView imageView, String username) {
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
                        // setting the post's admin avatar after complete loading
                        setImage(imageView, response.body().getDetail(), false);
                    } else {
                        Toast.makeText(PostDetailActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR106: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postComment(String commentContent) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Detail> call = apiService.postComment(ApplicationInfo.username, newsFeed.getId(), commentContent);

        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(Call<Detail> call, Response<Detail> response) {
                try {
                    if (response.isSuccessful()) {
                        // get the idComment
                        int idComment = Integer.parseInt(response.body().getDetail());

                        // add comment to ListView
                        PostDetail newComment = new PostDetail(idComment, ApplicationInfo.username, commentContent, System.currentTimeMillis() / 1000);
                        postDetailArrayList.add(0, newComment);
                        postDetailAdapter.notifyDataSetChanged();

                        // clear the comment in EditText
                        edComment.setText("");

                        // notify user that the comment is successfully posted
                        Toast.makeText(PostDetailActivity.this, "Comment successfully!", Toast.LENGTH_LONG).show();

                        btnPost.setClickable(true);
                        btnPost.setBackgroundTintList(getResources().getColorStateList(R.color.blue));
                    } else {
                        Toast.makeText(PostDetailActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR567: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postIncreaseCommentNumber(int idPost) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Detail> call = apiService.postComment(idPost);

        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(Call<Detail> call, Response<Detail> response) {
                try {
                    if (response.isSuccessful()) {
                        // do nothing
                    } else {
                        Toast.makeText(PostDetailActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR567: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postDecreaseCommentNumber(int idPost) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Detail> call = apiService.postUncomment(idPost);

        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(Call<Detail> call, Response<Detail> response) {
                try {
                    if (response.isSuccessful()) {
                        // do nothing
                    } else {
                        Toast.makeText(PostDetailActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR567: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setImage(ImageView imageView, String avatarData, Boolean isPostImage) {
        if (avatarData != null && !avatarData.isEmpty()) {
            StringToImageViewAsync stringToImageViewAsync = new StringToImageViewAsync(this, avatarData, imageView, isPostImage);
            stringToImageViewAsync.execute();
        }
    }

    private void setPostDateTime(TextView textView, long time) {
        Date date = new Date(time * 1000);

        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String dateFormatted = formatter.format(date);

        textView.setText(dateFormatted);
    }

    private void postNotification(String username, Integer type,  Integer idPost, String interacter) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Detail> call = apiService.postNotification(username, type, idPost, interacter);

        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(Call<Detail> call, Response<Detail> response) {
                try {
                    if (response.isSuccessful()) {
                        // do nothing
                    } else {
                        Toast.makeText(PostDetailActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR106: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteNotification(String username, Integer type,  Integer idPost, String interacter) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Detail> call = apiService.deleteNotification(username, type, idPost, interacter);

        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(Call<Detail> call, Response<Detail> response) {
                try {
                    if (response.isSuccessful()) {
                        // do nothing
                    } else {
                        Toast.makeText(PostDetailActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR106: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteComment(int idComment) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Detail> call = apiService.deleteComment(idComment);

        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(Call<Detail> call, Response<Detail> response) {
                try {
                    if (response.isSuccessful()) {
                        Toast.makeText(PostDetailActivity.this, "Comment deleted successfully!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(PostDetailActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR106: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}