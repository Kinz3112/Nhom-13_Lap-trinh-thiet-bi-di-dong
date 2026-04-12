package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {
    private List<Reminder> reminderList;
    private OnReminderClickListener listener;

    public interface OnReminderClickListener {
        void onToggle(Reminder reminder, boolean isChecked);
        void onDelete(Reminder reminder);
    }

    public ReminderAdapter(List<Reminder> reminderList, OnReminderClickListener listener) {
        this.reminderList = reminderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);

        holder.tvTitle.setText(reminder.title);
        String timeStr = String.format("%02d:%02d - %s", reminder.hour, reminder.minute, reminder.frequency);
        holder.tvTime.setText(timeStr);


        holder.switchActive.setOnCheckedChangeListener(null);
        holder.switchActive.setChecked(reminder.isActive);


        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onToggle(reminder, isChecked);
        });

        // Bắt sự kiện xóa
        holder.ivDelete.setOnClickListener(v -> listener.onDelete(reminder));
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        Switch switchActive;
        ImageView ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReminderTitle);
            tvTime = itemView.findViewById(R.id.tvReminderTime);
            switchActive = itemView.findViewById(R.id.switchActive);
            ivDelete = itemView.findViewById(R.id.ivDeleteReminder);
        }
    }
}