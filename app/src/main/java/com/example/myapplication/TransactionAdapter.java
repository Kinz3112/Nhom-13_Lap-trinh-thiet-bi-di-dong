package com.example.myapplication;

import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactionList;
    public interface OnTransactionLongClickListener {
        void onEdit(Transaction transaction);
        void onDelete(Transaction transaction);
    }

    private OnTransactionLongClickListener listener;

    public TransactionAdapter(List<Transaction> transactionList, OnTransactionLongClickListener listener) {
        this.transactionList = transactionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.tvTitle.setText(transaction.title);
        holder.tvCategory.setText(transaction.category);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateStr = sdf.format(new java.util.Date(transaction.date));
        holder.tvDate.setText(dateStr);

        if (transaction.imagePath != null) {
            holder.ivTransactionImg.setImageURI(Uri.parse(transaction.imagePath));
        } else {
            holder.ivTransactionImg.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // LUÔN LUÔN VISIBLE DÙ CÓ ẢNH HAY KHÔNG
        holder.ivTransactionImg.setVisibility(View.VISIBLE);

        if (transaction.type.equals("EXPENSE")) {
            holder.tvAmount.setText("-" + String.format("%,.0f", transaction.amount) + "đ");
            holder.tvAmount.setTextColor(Color.RED);
        } else {
            holder.tvAmount.setText("+" + String.format("%,.0f", transaction.amount) + "đ");
            holder.tvAmount.setTextColor(Color.GREEN);
        }

        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, transaction);
            return true;
        });
    }

    private void showPopupMenu(View view, Transaction transaction) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenuInflater().inflate(R.menu.transaction_context_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_edit) {
                if (listener != null) listener.onEdit(transaction);
                return true;
            } else if (id == R.id.menu_delete) {
                if (listener != null) listener.onDelete(transaction);
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvAmount, tvDate;
        ImageView ivTransactionImg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTransactionImg = itemView.findViewById(R.id.ivTransactionImg);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}