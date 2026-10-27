package com.example.myapplication;

import android.content.Context;
import android.widget.ImageView;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public final class ImageLoader {

    private static volatile Picasso picasso;

    private ImageLoader() {
    }

    public static void load(String imageUrl, ImageView imageView) {
        load(imageUrl, imageView, R.drawable.ic_account);
    }

    public static void load(String imageUrl, ImageView imageView, int placeholderResId) {
        if (imageView == null) return;

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            getPicasso(imageView.getContext()).cancelRequest(imageView);
            imageView.setImageResource(placeholderResId);
            return;
        }

        getPicasso(imageView.getContext())
                .load(imageUrl.trim())
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .fit()
                .centerCrop()
                .into(imageView);
    }

    private static Picasso getPicasso(Context context) {

        Picasso instance = picasso;

        if (instance == null) {
            synchronized (ImageLoader.class) {

                instance = picasso;

                if (instance == null) {
                    Context appContext =
                            context.getApplicationContext();

                    instance = new Picasso.Builder(appContext)
                            .downloader(
                                    new OkHttp3Downloader(appContext)
                            )
                            .build();

                    picasso = instance;
                }
            }
        }

        return instance;
    }
}
