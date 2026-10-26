package com.example.myapplication.qna.questionDetail;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageView;
import com.example.myapplication.R;

public class AnswerViewHolder extends RecyclerView.ViewHolder {

    public TextView tvAnswerAuthor;
    public TextView tvAnswerDate;
    public TextView tvAnswerContent;
    public TextView tvAnswerUpVote;
    public TextView tvAnswerDownVote;
    public TextView tvAnswerVoteCount;
    public ImageView ivAuthorAvatar;
    public TextView btnReportAnswer;
    public AnswerViewHolder(@NonNull View itemView) {
        super(itemView);
        btnReportAnswer = itemView.findViewById(R.id.btnReportAnswer);
        ivAuthorAvatar = itemView.findViewById(R.id.ivAnswerAuthorAvatar);
        tvAnswerAuthor = itemView.findViewById(R.id.tvAnswerAuthor);
        tvAnswerDate = itemView.findViewById(R.id.tvAnswerDate);
        tvAnswerContent = itemView.findViewById(R.id.tvAnswerContent);
        tvAnswerUpVote = itemView.findViewById(R.id.tvAnswerUpVote);
        tvAnswerDownVote = itemView.findViewById(R.id.tvAnswerDownVote);
        tvAnswerVoteCount = itemView.findViewById(R.id.tvAnswerVoteCount);
    }
}
