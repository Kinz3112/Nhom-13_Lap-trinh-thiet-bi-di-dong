package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<Category> categoryList;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onDeleteClick(Category category);
        void onJarClick(Category category);
    }

    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvName.setText(category.name);

        holder.tvBalance.setText(String.format("%,.0f đ", category.balance));

        holder.ivDelete.setOnClickListener(v -> listener.onDeleteClick(category));

        holder.itemView.setOnClickListener(v -> listener.onJarClick(category));
    }

    @Override
    public int getItemCount() { return categoryList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvBalance;
        ImageView ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvBalance = itemView.findViewById(R.id.tvCategoryBalance);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}