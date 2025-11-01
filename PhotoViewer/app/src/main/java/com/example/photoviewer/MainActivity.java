package com.example.photoviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    String site_url = "http://10.0.2.2:8000";
    CloadImage taskDownload;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    RecyclerView recyclerView;
    ImageAdapter adapter;
    List<PostItem> postList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        onClickDownload(null);

        SearchView searchView = findViewById(R.id.searchView);
        Spinner spinnerSort = findViewById(R.id.spinnerSort);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) adapter.filter(newText);
                return true;
            }
        });

        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (adapter != null) {
                    if (position == 0) adapter.sortByDate();
                    else adapter.sortByTitle();
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    // 동기화 버튼
    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING)
            taskDownload.cancel(true);
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_SHORT).show();
    }

    // 업로드 버튼
    public void onClickUpload(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri != null) showUploadDialog();
        }
    }

    private void showUploadDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload, null);
        EditText edtTitle = dialogView.findViewById(R.id.edtTitle);
        EditText edtText = dialogView.findViewById(R.id.edtText);

        new AlertDialog.Builder(this)
                .setTitle("새로운 포스트 업로드")
                .setView(dialogView)
                .setPositiveButton("업로드", (dialog, which) ->
                        uploadImageToServer(imageUri, edtTitle.getText().toString(), edtText.getText().toString()))
                .setNegativeButton("취소", null)
                .show();
    }

    private void uploadImageToServer(Uri uri, String title, String text) {
        new Thread(() -> {
            try {
                String serverUrl = site_url + "/api_root/Post/";
                String token = "376f1c71f59b6557f69fae40bed960aedd6402f8";

                String boundary = "*****" + System.currentTimeMillis() + "*****";
                HttpURLConnection conn = (HttpURLConnection) new URL(serverUrl).openConnection();
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n" + title + "\r\n");
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n" + text + "\r\n");
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                InputStream inputStream = getContentResolver().openInputStream(uri);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1)
                    dos.write(buffer, 0, bytesRead);
                inputStream.close();

                dos.writeBytes("\r\n--" + boundary + "--\r\n");
                dos.flush(); dos.close();

                int responseCode = conn.getResponseCode();
                Log.d("UPLOAD", "Response Code: " + responseCode);

                runOnUiThread(() -> {
                    if (responseCode == 201 || responseCode == 200) {
                        Toast.makeText(MainActivity.this, "업로드 성공!", Toast.LENGTH_SHORT).show();
                        onClickDownload(null);
                    } else {
                        Toast.makeText(MainActivity.this, "업로드 실패 (" + responseCode + ")", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 다운로드 (이미지 + 메타데이터)
    private class CloadImage extends AsyncTask<String, Integer, List<PostItem>> {
        @Override
        protected List<PostItem> doInBackground(String... urls) {
            List<PostItem> list = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Token 376f1c71f59b6557f69fae40bed960aedd6402f8");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                JSONArray aryJson = new JSONArray(result.toString());
                for (int i = 0; i < aryJson.length(); i++) {
                    JSONObject p = aryJson.getJSONObject(i);
                    PostItem post = new PostItem();
                    post.id = p.getInt("id");
                    post.title = p.getString("title");
                    post.text = p.getString("text");
                    post.imageUrl = p.getString("image");
                    post.date = p.getString("published_date");
                    list.add(post);
                }
            } catch (Exception e) { e.printStackTrace(); }
            return list;
        }

        @Override
        protected void onPostExecute(List<PostItem> posts) {
            postList = posts;
            adapter = new ImageAdapter(MainActivity.this, postList);
            recyclerView.setAdapter(adapter);
            textView.setText("총 게시물: " + posts.size());
        }
    }
}