
package com.example.myapplication.qna;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuestionAdapter
        extends RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {

    private final List<Question> questionList = new ArrayList<>();
    private final String currentUserId;
    private final OnQuestionActionListener listener;

    public interface OnQuestionActionListener {
        void onQuestionClick(Question question);

        void onReportClick(Question question);

        void onCloseClick(Question question);

        void onVoteClick(Question question, int value);
    }

    public QuestionAdapter(
            List<Question> questions,
            String currentUserId,
            OnQuestionActionListener listener
    ) {
        if (questions != null) {
            questionList.addAll(questions);
        }

        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question, parent, false);

        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull QuestionViewHolder holder,
            int position
    ) {
        Question question = questionList.get(position);

        holder.tvTitle.setText(
                question.getTitle() == null
                        ? ""
                        : question.getTitle()
        );

        holder.tvContent.setText(
                question.getContent() == null
                        ? ""
                        : question.getContent()
        );

        holder.tvAuthor.setText(
                question.getAuthorName() == null
                        ? "Người dùng"
                        : question.getAuthorName()
        );

        holder.tvDate.setText(formatTimeAgo(question.getCreatedAt()));

        holder.tvVoteCount.setText(
                 question.getVoteCount() + " vote"
        );

        holder.tvAnswerCount.setText(
                question.getAnswerCount() + " câu trả lời"
        );

        // Hiển thị trạng thái

        String rawStatus = question.getStatus();

        final String status =
                rawStatus == null || rawStatus.trim().isEmpty()
                        ? "OPEN"
                        : rawStatus.trim().toUpperCase(Locale.ROOT);


        holder.tvStatus.setText(status);

        if ("CLOSED".equals(status)) {
            holder.tvStatus.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#E5E7EB"))
            );
            holder.tvStatus.setTextColor(
                    Color.parseColor("#475569")
            );
        } else {
            holder.tvStatus.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#DDF7E8"))
            );
            holder.tvStatus.setTextColor(
                    Color.parseColor("#16834A")
            );
        }

        // Hiển thị category bằng Chip
        holder.chipGroupCategories.removeAllViews();

        List<String> categories = question.getCategories();

        if (categories != null) {
            for (String category : categories) {
                if (category == null || category.trim().isEmpty()) {
                    continue;
                }

                Chip chip = new Chip(holder.chipGroupCategories.getContext());

                chip.setText(category);
                chip.setCheckable(false);
                chip.setClickable(false);
                chip.setFocusable(false);
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                chip.setTextColor(Color.parseColor("#285EA8"));

                chip.setChipBackgroundColor(
                        ColorStateList.valueOf(
                                Color.parseColor("#E8F1FF")
                        )
                );

                holder.chipGroupCategories.addView(chip);
            }
        }

        // Xác định câu hỏi của người dùng hiện tại
        boolean isOwner = currentUserId != null
                && currentUserId.equals(question.getUserId());

        // Chủ câu hỏi mới nhìn thấy nút đóng
        holder.btnCloseQuestion.setVisibility(
                isOwner && "OPEN".equals(status)
                        ? View.VISIBLE
                        : View.GONE
        );

        // Không hiện nút báo cáo cho câu hỏi của chính mình
        holder.btnReportQuestion.setVisibility(
                isOwner ? View.GONE : View.VISIBLE
        );

        // Nhấn thẻ câu hỏi để mở chi tiết
        holder.cardQuestion.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuestionClick(question);
            }
        });

        // Báo cáo
        holder.btnReportQuestion.setOnClickListener(v -> {
            if (!isOwner && listener != null) {
                listener.onReportClick(question);
            }
        });

        // Đóng câu hỏi
        holder.btnCloseQuestion.setOnClickListener(v -> {
            if (isOwner
                    && "OPEN".equals(status)
                    && listener != null) {
                listener.onCloseClick(question);
            }
        });

        // UP vote
        holder.tvQuestionUpVote.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVoteClick(question, 1);
            }
        });

        // DOWN vote
        holder.tvQuestionDownVote.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVoteClick(question, -1);
            }
        });
    }

    private String formatTimeAgo(
            com.google.firebase.Timestamp timestamp
    ) {
        if (timestamp == null) {
            return "";
        }

        Date createdDate = timestamp.toDate();

        long difference = System.currentTimeMillis()
                - createdDate.getTime();

        if (difference < 0) {
            difference = 0;
        }

        long minutes = difference / (60 * 1000);

        if (minutes < 1) {
            return "Vừa xong";
        }

        if (minutes < 60) {
            return minutes + " phút trước";
        }

        long hours = minutes / 60;

        if (hours < 24) {
            return hours + " giờ trước";
        }

        long days = hours / 24;

        if (days < 7) {
            return days + " ngày trước";
        }

        return new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        ).format(createdDate);
    }

    public void setQuestions(List<Question> questions) {
        questionList.clear();

        if (questions != null) {
            questionList.addAll(questions);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    static class QuestionViewHolder
            extends RecyclerView.ViewHolder {

        View cardQuestion;
        TextView tvTitle;
        TextView tvStatus;
        TextView btnCloseQuestion;
        TextView tvContent;
        TextView tvAuthor;
        TextView tvDate;
        TextView tvQuestionUpVote;
        TextView tvQuestionDownVote;
        TextView tvVoteCount;
        TextView tvAnswerCount;
        TextView btnReportQuestion;
        ChipGroup chipGroupCategories;

        QuestionViewHolder(@NonNull View itemView) {
            super(itemView);

            cardQuestion = itemView.findViewById(R.id.cardQuestion);
            tvTitle = itemView.findViewById(R.id.tvQuestionTitle);
            tvStatus = itemView.findViewById(R.id.tvQuestionStatus);
            btnCloseQuestion = itemView.findViewById(R.id.btnCloseQuestion);
            tvContent = itemView.findViewById(R.id.tvQuestionBody);
            tvAuthor = itemView.findViewById(R.id.tvQuestionAuthor);
            tvDate = itemView.findViewById(R.id.tvQuestionDate);
            tvQuestionUpVote = itemView.findViewById(R.id.tvQuestionUpVote);
            tvQuestionDownVote = itemView.findViewById(R.id.tvQuestionDownVote);
            tvVoteCount = itemView.findViewById(R.id.tvQuestionVoteCount);
            tvAnswerCount = itemView.findViewById(R.id.tvQuestionAnswerCount);
            btnReportQuestion = itemView.findViewById(R.id.btnReportQuestion);

            chipGroupCategories = itemView.findViewById(
                    R.id.chipGroupQuestionCategories
            );
        }
    }
}
