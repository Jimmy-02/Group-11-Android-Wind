package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.content.Intent;
import com.example.myapplication.discussions.creation.CreateDiscussionActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DiscussionFragment extends Fragment {

    public DiscussionFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discussion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fabCreateDiscussion = view.findViewById(R.id.fab_create_discussion);

        fabCreateDiscussion.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateDiscussionActivity.class);
            startActivity(intent);
        });
    }
}