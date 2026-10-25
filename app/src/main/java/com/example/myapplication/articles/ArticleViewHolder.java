package com.example.myapplication.articles;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

public class ArticleViewHolder extends RecyclerView.ViewHolder {

    ImageView coverView;
    TextView titleView;
    TextView authorView;
    TextView metadataView;
    TextView categoryView;
    TextView reportButton;

    public ArticleViewHolder(@NonNull View itemView) {
        super(itemView);

        coverView = itemView.findViewById(R.id.iv_article_cover);
        titleView = itemView.findViewById(R.id.tv_article_title);
        authorView = itemView.findViewById(R.id.tv_article_author);
        metadataView = itemView.findViewById(R.id.tv_article_metadata);
        categoryView = itemView.findViewById(R.id.tv_article_category);
        reportButton = itemView.findViewById(R.id.btn_report_article);
    }
}