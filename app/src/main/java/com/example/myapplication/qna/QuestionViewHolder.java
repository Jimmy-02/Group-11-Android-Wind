
package com.example.myapplication.qna;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageView;
import com.example.myapplication.R;
import com.google.android.material.chip.ChipGroup;
import android.widget.ImageView;
public class QuestionViewHolder extends RecyclerView.ViewHolder {

    public View cardQuestion;
    public TextView tvTitle;
    public TextView tvStatus;
    public TextView btnCloseQuestion;
    public TextView tvContent;
    public TextView tvAuthor;
    public TextView tvDate;
    public TextView tvQuestionUpVote;
    public TextView tvQuestionDownVote;
    public TextView tvVoteCount;
    public TextView tvAnswerCount;
    public TextView btnReportQuestion;
    public ChipGroup chipGroupCategories;
    public ImageView ivQuestionImage;
    public ImageView ivAuthorAvatar;


    public QuestionViewHolder(@NonNull View itemView) {
        super(itemView);
        ivAuthorAvatar = itemView.findViewById(R.id.ivQuestionAuthorAvatar);
        cardQuestion = itemView.findViewById(R.id.cardQuestion);
        tvTitle = itemView.findViewById(R.id.tvQuestionTitle);
        tvStatus = itemView.findViewById(R.id.tvQuestionStatus);
        btnCloseQuestion = itemView.findViewById(R.id.btnCloseQuestion);
        tvContent = itemView.findViewById(R.id.tvQuestionBody);
        ivQuestionImage = itemView.findViewById(R.id.ivQuestionImage);
        tvAuthor = itemView.findViewById(R.id.tvQuestionAuthor);
        tvDate = itemView.findViewById(R.id.tvQuestionDate);
        tvQuestionUpVote = itemView.findViewById(R.id.tvQuestionUpVote);
        tvQuestionDownVote = itemView.findViewById(R.id.tvQuestionDownVote);
        tvVoteCount = itemView.findViewById(R.id.tvQuestionVoteCount);
        tvAnswerCount = itemView.findViewById(R.id.tvQuestionAnswerCount);
        btnReportQuestion = itemView.findViewById(R.id.btnReportQuestion);
        chipGroupCategories = itemView.findViewById(R.id.chipGroupQuestionCategories);
    }
}
