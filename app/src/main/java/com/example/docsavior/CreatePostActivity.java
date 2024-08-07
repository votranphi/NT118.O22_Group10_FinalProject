package com.example.docsavior;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import org.json.JSONArray;

import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CreatePostActivity extends AppCompatActivity {

    private ImageButton btnClose;
    private ImageButton btnPost;
    private TextView tvUsername;
    private TextView tvSelectedFile;
    private Spinner edPostDesciption;
    private EditText edPostContent;
    private Button btnPictureVideo;
    private Button btnFile;
    private ImageView btnUser;
    private boolean isFileChosen;
    // Request code for selecting a PDF document.
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    private String fileData = "";
    private String fileName = "";
    private String fileExtension = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        findViewByIds();

        setOnClickListeners();

        initVariables();

        getAndSetAvatar(btnUser, ApplicationInfo.username);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(ApplicationInfo.activityLifecycleManager);
        }
    }

    private void findViewByIds() {
        btnClose = findViewById(R.id.btnClose);
        btnPost = findViewById(R.id.btnPost);
        tvUsername = findViewById(R.id.tvUsername);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        edPostDesciption = findViewById(R.id.edPostDesciption);
        edPostContent = findViewById(R.id.edPostContent);
        btnPictureVideo = findViewById(R.id.btnPictureVideo);
        btnFile = findViewById(R.id.btnFile);
        btnUser = findViewById(R.id.btnUser);
    }

    private void setOnClickListeners() {
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
            }
        });

        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edPostDesciption.getSelectedItem().toString().equals("Select your topic")) {
                    Toast.makeText(CreatePostActivity.this, "Please select a topic", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (edPostContent.getText().toString().isEmpty()) {
                    Toast.makeText(CreatePostActivity.this, "Please provide post's description!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isFileChosen) {
                    Toast.makeText(CreatePostActivity.this, "Please choose a file/video/image from your device!", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(() -> {
                    // call API to post the post to database
                    postNewsfeed(ApplicationInfo.username, edPostDesciption.getSelectedItem().toString(), edPostContent.getText().toString(), fileData, fileName, fileExtension);
                }).start();

                Toast.makeText(CreatePostActivity.this, "Uploading...", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnPictureVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // the same as btnFile but need to let user choose only video, image file type
                openFileChooser(false);
            }
        });

        btnFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(true);
            }
        });
    }

    private void initVariables() {
        tvUsername.setText(ApplicationInfo.username);
        isFileChosen = false;

        String[] items = new String[]{"Select your topic", "Maths", "Literature", "Physic", "English", "Chemistry", "Geography", "History", "Calculus", "Programming", "Network", "AI", "System", "Picture" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        edPostDesciption.setAdapter(adapter);
    }

    private void postNewsfeed(String username, String postDescription, String postContent, String fileData, String fileName, String fileExtension) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApplicationInfo.apiPath)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Newsfeed newsFe123ed = new Newsfeed(username, postDescription, postContent, fileData, fileName, fileExtension);

        Call<Detail> call = apiService.postNewsfeed(newsFe123ed);

        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(Call<Detail> call, Response<Detail> response) {
                try {
                    if (response.isSuccessful()) {
                        Toast.makeText(CreatePostActivity.this, "Your post is uploaded, please refresh newsfeed to see it!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(CreatePostActivity.this, "Upload post unsuccessfully!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR1: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(CreatePostActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFileChooser(boolean isFile) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        if (isFile) {
            intent.setType("*/*");
        } else {
            intent.setType("image/*");
        }

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (isFile) {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), REQUEST_CODE_OPEN_DOCUMENT);
        } else {
            startActivityForResult(Intent.createChooser(intent, "Select a Image"), REQUEST_CODE_OPEN_DOCUMENT);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
                        if (documentFile != null && documentFile.isFile()) {
                            // Read content as InputStream
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            byte[] bytes = new byte[inputStream.available()];
                            inputStream.read(bytes);
                            inputStream.close();

                            // create jsonArray to store byte array in it
                            JSONArray jsonArray = new JSONArray();
                            for (byte i : bytes) {
                                jsonArray.put(i);
                            }
                            // convert jsonArray to string for posting it
                            fileData = jsonArray.toString();

                            // get the file's full name
                            String fileFullName = documentFile.getName();
                            // split the fileFullName to get file's name and file's extension
                            String[] splitString = fileFullName.split("\\.");
                            fileName = splitString[0];
                            fileExtension = splitString[1];

                            // set the chosen state to true
                            isFileChosen = true;
                            tvSelectedFile.setText(documentFile.getName());
                        }
                    } catch (Exception e) {
                        Log.e("ERROR123: ", e.getMessage());
                    }
                }
            }
        }
    }

    private void getAndSetAvatar(ImageView imageView, String username) {
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
                        // after getting the user's info, set the avatar
                        setImage(imageView, response.body().getDetail());
                    } else {
                        Toast.makeText(CreatePostActivity.this, response.code() + response.errorBody().string(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e("ERROR106: ", ex.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Detail> call, Throwable t) {
                Toast.makeText(CreatePostActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setImage(ImageView imageView, String avatarData) {
        if (avatarData != null && !avatarData.isEmpty()) {
            StringToImageViewAsync stringToImageViewAsync = new StringToImageViewAsync(this, avatarData, imageView, false);
            stringToImageViewAsync.execute();
        }
    }
}