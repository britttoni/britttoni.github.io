package com.example.cs360_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

//recyclerView adapter for displaying inventory items.

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private List<InventoryItem> inventoryList;
    private final OnItemClickListener listener;


    public interface OnItemClickListener {
        void onDeleteClick(int id);
        void onUpdateClick(InventoryItem item);
    }


    public InventoryAdapter(List<InventoryItem> inventoryList, OnItemClickListener listener) {
        this.inventoryList = inventoryList;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem item = inventoryList.get(position);

        holder.textViewItemName.setText(item.getName());
        holder.textViewItemQuantity.setText("Quantity: " + item.getQuantity());

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item.getId());
            }
        });

        holder.buttonUpdate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpdateClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return inventoryList == null ? 0 : inventoryList.size();
    }

    @Override
    public long getItemId(int position) {
        return inventoryList.get(position).getId();
    }

    public void updateData(List<InventoryItem> newList) {
        this.inventoryList = newList;
        notifyDataSetChanged();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {

        TextView textViewItemName;
        TextView textViewItemQuantity;
        Button buttonDelete;
        Button buttonUpdate;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewItemName = itemView.findViewById(R.id.textViewItemName);
            textViewItemQuantity = itemView.findViewById(R.id.textViewItemQuantity);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonUpdate = itemView.findViewById(R.id.buttonUpdate);
        }
    }
}