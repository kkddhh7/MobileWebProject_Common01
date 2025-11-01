package com.example.photoviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final Context context;
    private final String siteUrl = "http://10.0.2.2:8000";
    private final String token = "376f1c71f59b6557f69fae40bed960aedd6402f8";
    private List<PostItem> postList;
    private List<PostItem> filteredList;  // 검색용

    public ImageAdapter(Context context, List<PostItem> postList) {
        this.context = context;
        this.postList = new ArrayList<>(postList);
        this.filteredList = new ArrayList<>(postList);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView txtTitle, txtDate;
        Button btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDate = itemView.findViewById(R.id.txtDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PostItem post = filteredList.get(position);

        holder.txtTitle.setText(post.title);
        holder.txtDate.setText(post.date);

        // 이미지 로드
        new Thread(() -> {
            try {
                URL url = new URL(post.imageUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                InputStream inputStream = conn.getInputStream();
                Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                holder.imageView.post(() -> holder.imageView.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // 이미지 클릭 시 전체보기
        holder.imageView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            ImageView bigImage = new ImageView(context);
            bigImage.setImageDrawable(holder.imageView.getDrawable());
            builder.setView(bigImage);
            builder.setPositiveButton("닫기", null);
            builder.show();
        });

        // 삭제 버튼
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("삭제 확인")
                    .setMessage("이 게시물을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> deletePost(post.id, holder.getAdapterPosition()))
                    .setNegativeButton("취소", null)
                    .show();
        });
    }

    private void deletePost(int id, int position) {
        new Thread(() -> {
            try {
                URL url = new URL(siteUrl + "/api_root/Post/" + id + "/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.connect();

                int responseCode = conn.getResponseCode();
                ((Activity) context).runOnUiThread(() -> {
                    if (responseCode == 204) {
                        Toast.makeText(context, "삭제 완료", Toast.LENGTH_SHORT).show();
                        filteredList.remove(position);
                        notifyItemRemoved(position);
                    } else {
                        Toast.makeText(context, "삭제 실패 (" + responseCode + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "에러 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // 🔹 제목 검색 기능
    public void filter(String query) {
        query = query.toLowerCase();
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(postList);
        } else {
            for (PostItem p : postList) {
                if (p.title.toLowerCase().contains(query)) {
                    filteredList.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    // 🔹 제목 정렬
    public void sortByTitle() {
        Collections.sort(filteredList, Comparator.comparing(p -> p.title.toLowerCase()));
        notifyDataSetChanged();
    }

    // 🔹 날짜 정렬
    public void sortByDate() {
        Collections.sort(filteredList, (a, b) -> b.date.compareTo(a.date)); // 최신순
        notifyDataSetChanged();
    }
}