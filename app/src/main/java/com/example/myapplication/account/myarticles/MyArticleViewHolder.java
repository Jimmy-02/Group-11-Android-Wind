
package com.example.myapplication.account.myarticles;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

public class MyArticleViewHolder extends RecyclerView.ViewHolder {

    public TextView tvArticleTitle;
    public TextView tvArticleStatus;
    public TextView tvArticleDescription;
    public TextView tvArticleDate;
    public ImageView ivArticleImage;

    public MyArticleViewHolder(@NonNull View itemView) {
        super(itemView);

        ivArticleImage = itemView.findViewById(R.id.ivArticleImage);
        tvArticleTitle = itemView.findViewById(R.id.tvArticleTitle);
        tvArticleStatus = itemView.findViewById(R.id.tvArticleStatus);
        tvArticleDescription =
                itemView.findViewById(R.id.tvArticleDescription);
        tvArticleDate = itemView.findViewById(R.id.tvArticleDate);
    }
}
