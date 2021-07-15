package com.yhjoo.dochef.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yhjoo.dochef.App;
import com.yhjoo.dochef.R;
import com.yhjoo.dochef.databinding.APostwriteBinding;
import com.yhjoo.dochef.interfaces.RetrofitServices;
import com.yhjoo.dochef.model.UserBrief;
import com.yhjoo.dochef.utils.BasicCallback;
import com.yhjoo.dochef.utils.GlideApp;
import com.yhjoo.dochef.utils.PermissionUtil;
import com.yhjoo.dochef.utils.RetrofitBuilder;
import com.yhjoo.dochef.utils.Utils;

import java.io.File;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

public class PostWriteActivity extends BaseActivity {
    private final int CODE_PERMISSION = 22;
    private final int EXTRA_RQ_PICKFROMGALLERY = 200;

    enum MODE {WRITE, REVISE}

    APostwriteBinding binding;
    RetrofitServices.PostService postService;
    FirebaseStorage storage;
    StorageReference storageReference;

    String userID;
    int postID;
    Uri mImageUri;
    String image_url;
    MODE current_mode = MODE.WRITE;


    /*
        TODO
        REVISE 시 최초 통신해서 데이터 가져오는걸로
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = APostwriteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Gson gson = new Gson();
        UserBrief userInfo = gson.fromJson(mSharedPreferences.getString(getString(R.string.SP_USERINFO), null), UserBrief.class);
        userID = userInfo.getUserID();

        postService = RetrofitBuilder.create(this, RetrofitServices.PostService.class);

        current_mode = (PostWriteActivity.MODE) getIntent().getSerializableExtra("MODE");

        if (current_mode == MODE.REVISE) {
            binding.postwriteToolbar.setTitle("수정");
            postID = getIntent().getIntExtra("postID", -1);
            binding.postwriteContents.setText(getIntent().getStringExtra("contents"));

            if (getIntent().getStringExtra("postImg") != null){
                StorageReference sr = FirebaseStorage
                        .getInstance().getReference().child("post/" + getIntent().getStringExtra("postImg"));
                GlideApp.with(this)
                        .load(sr)
                        .into(binding.postwritePostimg);
            }

            ArrayList<String> tags = (ArrayList<String>) getIntent().getSerializableExtra("tags");
            binding.postwriteTags.setTags((String[]) tags.toArray());
        }
        setSupportActionBar(binding.postwriteToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.postwritePostimgAdd.setOnClickListener(this::addImage);
        binding.postwriteOk.setOnClickListener(this::createOrUpdatePost);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXTRA_RQ_PICKFROMGALLERY)
            if (data != null) {
                Utils.log(data);
                mImageUri = data.getData();
                binding.postwritePostimg.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(mImageUri)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE).skipMemoryCache(true))
                        .into(binding.postwritePostimg);
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_PERMISSION) {
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    App.getAppInstance().showToast("권한 거부");
                    return;
                }
            }

            mImageUri = Uri.fromFile(new File(getExternalCacheDir(), "filterimage"));

            Intent intent = new Intent(Intent.ACTION_PICK)
                    .setType(MediaStore.Images.Media.CONTENT_TYPE)
                    .putExtra("crop", "true")
                    .putExtra("aspectX", 3)
                    .putExtra("aspectY", 2)
                    .putExtra("scale", true)
                    .putExtra("output", mImageUri);
            startActivityForResult(intent, EXTRA_RQ_PICKFROMGALLERY);
        }
    }

    void addImage(View v) {
        final String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        if (PermissionUtil.checkPermission(this, permissions)) {
            mImageUri = Uri.fromFile(new File(getExternalCacheDir(), "filterimage"));

            Intent intent = new Intent(Intent.ACTION_PICK)
                    .setType(MediaStore.Images.Media.CONTENT_TYPE)
                    .putExtra("crop", "true")
                    .putExtra("aspectX", 3)
                    .putExtra("aspectY", 2)
                    .putExtra("scale", true)
                    .putExtra("output", mImageUri);
            startActivityForResult(intent, EXTRA_RQ_PICKFROMGALLERY);
        } else
            ActivityCompat.requestPermissions(this, permissions, CODE_PERMISSION);
    }

    void createOrUpdatePost(View v) {
        ArrayList<String> tags = new ArrayList<>();
        for (String a: binding.postwriteTags.getTags()) {
            tags.add(a);
        }

        image_url = "";
        if(mImageUri != null){
            image_url= String.format(getString(R.string.format_upload_post),
                    userID, Long.toString(System.currentTimeMillis()));
        }
        if (current_mode == MODE.WRITE) {
            StorageReference ref = storageReference.child(image_url);
            ref.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            postService.createPost(
                                    userID,
                                    image_url,
                                    binding.postwriteContents.getText().toString(),
                                    System.currentTimeMillis(),
                                    tags)
                                    .enqueue(new BasicCallback<JsonObject>(PostWriteActivity.this) {
                                        @Override
                                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                            super.onResponse(call, response);

                                            if (response.code() == 500) {
                                                App.getAppInstance().showToast("post 등록 실패");
                                            } else {
                                                App.getAppInstance().showToast("글이 등록되었습니다.");
                                                finish();
                                            }
                                        }
                                    });
                        }
                    });
        } else {
            postService.updatePost(
                    postID,
                    image_url,
                    binding.postwriteContents.getText().toString(),
                    System.currentTimeMillis(),
                    tags)
                    .enqueue(new BasicCallback<JsonObject>(this) {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            super.onResponse(call, response);

                            if (response.code() == 500) {
                                App.getAppInstance().showToast("comment 가져오기 실패");
                            } else {
                                App.getAppInstance().showToast("업데이트 되었습니다.");
                                finish();
                            }
                        }
                    });

        }
    }
}
