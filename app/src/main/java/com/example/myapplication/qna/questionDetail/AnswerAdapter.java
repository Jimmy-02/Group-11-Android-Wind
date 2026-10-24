
package com.example.myapplication.qna.questionDetail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnswerAdapter
        extends RecyclerView.Adapter<AnswerAdapter.AnswerViewHolder> {

    private final List<Answer> answerList = new ArrayList<>();
    private final OnAnswerVoteListener voteListener;

    public interface OnAnswerVoteListener {
        void onVoteClick(Answer answer, int value);
    }

    public AnswerAdapter(
            List<Answer> answers,
            OnAnswerVoteListener voteListener
    ) {
        if (answers != null) {
            answerList.addAll(answers);
        }

        this.voteListener = voteListener;
    }

    @NonNull
    @Override
    public AnswerViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_answer, parent, false);

        return new AnswerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AnswerViewHolder holder,
            int position
    ) {
        Answer answer = answerList.get(position);

        // Tên người trả lời
        String authorName = answer.getAuthorName();

        holder.tvAnswerAuthor.setText(
                authorName == null || authorName.trim().isEmpty()
                        ? "Người dùng"
                        : authorName
        );

        // Nội dung câu trả lời
        holder.tvAnswerContent.setText(
                answer.getContent() == null
                        ? ""
                        : answer.getContent()
        );

        // Thời gian trả lời
        holder.tvAnswerDate.setText(
                formatDate(answer.getCreatedAt())
        );

        // Số vote, không hiển thị mũi tên
        holder.tvAnswerVoteCount.setText(
                answer.getVoteCount() + " vote"
        );

        // UP
        holder.tvAnswerUpVote.setOnClickListener(v -> {
            if (voteListener != null) {
                voteListener.onVoteClick(answer, 1);
            }
        });

        // DOWN
        holder.tvAnswerDownVote.setOnClickListener(v -> {
            if (voteListener != null) {
                voteListener.onVoteClick(answer, -1);
            }
        });
    }

    private String formatDate(
            com.google.firebase.Timestamp timestamp
    ) {
        if (timestamp == null) {
            return "";
        }

        Date date = timestamp.toDate();

        return new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
        ).format(date);
    }

    public void setAnswers(List<Answer> answers) {
        answerList.clear();

        if (answers != null) {
            answerList.addAll(answers);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return answerList.size();
    }

    static class AnswerViewHolder extends RecyclerView.ViewHolder {

        TextView tvAnswerAuthor;
        TextView tvAnswerDate;
        TextView tvAnswerContent;
        TextView tvAnswerUpVote;
        TextView tvAnswerDownVote;
        TextView tvAnswerVoteCount;

        AnswerViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAnswerAuthor =
                    itemView.findViewById(R.id.tvAnswerAuthor);

            tvAnswerDate =
                    itemView.findViewById(R.id.tvAnswerDate);

            tvAnswerContent =
                    itemView.findViewById(R.id.tvAnswerContent);

            tvAnswerUpVote =
                    itemView.findViewById(R.id.tvAnswerUpVote);

            tvAnswerDownVote =
                    itemView.findViewById(R.id.tvAnswerDownVote);

            tvAnswerVoteCount =
                    itemView.findViewById(R.id.tvAnswerVoteCount);
        }
    }
}

