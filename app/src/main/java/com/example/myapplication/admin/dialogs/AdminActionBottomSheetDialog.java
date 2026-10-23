package com.example.myapplication.admin.dialogs;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;

import com.example.myapplication.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AdminActionBottomSheetDialog extends BottomSheetDialogFragment {

    public static final String ACTION_DELETE = "ACTION_DELETE";
    public static final String ACTION_DISMISS = "ACTION_DISMISS";

    private static final String ARG_ACTION_TYPE = "arg_action_type";
    private static final String ARG_TARGET_TYPE = "arg_target_type";
    private static final String ARG_TITLE = "arg_title";

    public interface OnActionConfirmListener {
        void onConfirm(String reason);
    }

    private String actionType = ACTION_DELETE;
    private String targetType = "QUESTION";
    private String targetTitle = "";
    private OnActionConfirmListener confirmListener;

    public static AdminActionBottomSheetDialog newInstance(String actionType, String targetType, String targetTitle) {
        AdminActionBottomSheetDialog dialog = new AdminActionBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ACTION_TYPE, actionType);
        args.putString(ARG_TARGET_TYPE, targetType);
        args.putString(ARG_TITLE, targetTitle);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnActionConfirmListener(OnActionConfirmListener listener) {
        this.confirmListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            actionType = getArguments().getString(ARG_ACTION_TYPE, ACTION_DELETE);
            targetType = getArguments().getString(ARG_TARGET_TYPE, "QUESTION");
            targetTitle = getArguments().getString(ARG_TITLE, "");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_admin_action_bottom_sheet, container, false);

        FrameLayout layoutIconCircle = view.findViewById(R.id.layoutIconCircle);
        ImageView imgActionIcon = view.findViewById(R.id.imgActionIcon);
        TextView tvActionTitle = view.findViewById(R.id.tvActionTitle);
        TextView tvActionMessage = view.findViewById(R.id.tvActionMessage);
        TextView tvReasonLabel = view.findViewById(R.id.tvReasonLabel);
        EditText etActionReason = view.findViewById(R.id.etActionReason);
        TextView tvCharCounter = view.findViewById(R.id.tvCharCounter);
        TextView tvReasonError = view.findViewById(R.id.tvReasonError);
        ProgressBar pbActionLoading = view.findViewById(R.id.pbActionLoading);
        View layoutButtons = view.findViewById(R.id.layoutButtons);
        TextView btnCancelAction = view.findViewById(R.id.btnCancelAction);
        TextView btnConfirmAction = view.findViewById(R.id.btnConfirmAction);

        boolean isDelete = ACTION_DELETE.equals(actionType);
        String typeName = "QUESTION".equalsIgnoreCase(targetType) ? "câu hỏi" : "câu trả lời";

        if (isDelete) {
            layoutIconCircle.setBackgroundResource(R.drawable.bg_status_pending);
            imgActionIcon.setImageResource(R.drawable.ic_delete);
            ImageViewCompat.setImageTintList(imgActionIcon, ColorStateList.valueOf(Color.parseColor("#DC2626")));

            tvActionTitle.setText("Xóa nội dung vi phạm");
            tvActionMessage.setText("Bạn chắc chắn muốn xóa " + typeName + " này? Hành động này sẽ chuyển trạng thái nội dung sang đã xóa và cập nhật các báo cáo liên quan.");
            tvReasonLabel.setText("Lý do xóa *");
            btnConfirmAction.setText("Xóa nội dung");
            btnConfirmAction.setBackgroundResource(R.drawable.bg_btn_danger);
        } else {
            layoutIconCircle.setBackgroundResource(R.drawable.bg_status_dismissed);
            imgActionIcon.setImageResource(R.drawable.ic_block);
            ImageViewCompat.setImageTintList(imgActionIcon, ColorStateList.valueOf(Color.parseColor("#475569")));

            tvActionTitle.setText("Bỏ qua báo cáo");
            tvActionMessage.setText("Bạn chắc chắn muốn bỏ qua các báo cáo này? Nội dung " + typeName + " sẽ được giữ nguyên và các báo cáo sẽ được đánh dấu là đã bác bỏ.");
            tvReasonLabel.setText("Lý do bỏ qua *");
            btnConfirmAction.setText("Bỏ qua báo cáo");
            btnConfirmAction.setBackgroundResource(R.drawable.bg_btn_primary);
        }

        // Live Character counter (0/500)
        etActionReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s != null ? s.length() : 0;
                tvCharCounter.setText(length + "/500");
                if (length >= 5) {
                    tvReasonError.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancelAction.setOnClickListener(v -> dismiss());

        btnConfirmAction.setOnClickListener(v -> {
            String reason = etActionReason.getText() != null ? etActionReason.getText().toString().trim() : "";
            if (reason.length() < 5) {
                tvReasonError.setVisibility(View.VISIBLE);
                etActionReason.requestFocus();
                return;
            }

            // Hide buttons and show progress to avoid multi-clicks
            layoutButtons.setVisibility(View.GONE);
            pbActionLoading.setVisibility(View.VISIBLE);
            etActionReason.setEnabled(false);

            if (confirmListener != null) {
                confirmListener.onConfirm(reason);
            }
        });

        return view;
    }
}
