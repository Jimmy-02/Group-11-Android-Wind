package com.example.myapplication.admin.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.myapplication.R;

public class AdminSuccessDialog extends DialogFragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";

    public interface OnDoneClickListener {
        void onDone();
    }

    private String title = "Thao tác thành công";
    private String message = "Dữ liệu đã được cập nhật thành công.";
    private OnDoneClickListener doneListener;

    public static AdminSuccessDialog newInstance(String title, String message) {
        AdminSuccessDialog dialog = new AdminSuccessDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnDoneClickListener(OnDoneClickListener listener) {
        this.doneListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE, "Thao tác thành công");
            message = getArguments().getString(ARG_MESSAGE, "Dữ liệu đã được cập nhật thành công.");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_admin_success, container, false);

        TextView tvSuccessTitle = view.findViewById(R.id.tvSuccessTitle);
        TextView tvSuccessMessage = view.findViewById(R.id.tvSuccessMessage);
        TextView btnSuccessDone = view.findViewById(R.id.btnSuccessDone);

        tvSuccessTitle.setText(title);
        tvSuccessMessage.setText(message);

        btnSuccessDone.setOnClickListener(v -> {
            dismiss();
            if (doneListener != null) {
                doneListener.onDone();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
