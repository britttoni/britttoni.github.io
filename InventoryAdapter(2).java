package com.example.cs360_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter for displaying inventory items.
 * This adapter also supports deleting items and refreshing data.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private List<InventoryItem> inventoryList;
    private final OnItemClickListener listener;

    /**
     * Interface for handling delete button clicks.
     */
    public interface OnItemClickListener {
        void onDeleteClick(int id);
    }

    /**
     * Constructor for adapter.
     *
     * @param inventoryList list of inventory items
     * @param listener click listener for delete action
     */
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
    }

    @Override
    public int getItemCount() {
        return inventoryList == null ? 0 : inventoryList.size();
    }

    @Override
    public long getItemId(int position) {
        return inventoryList.get(position).getId();
    }

    // Updates the adapter data and refreshes the RecyclerView.

    public void updateData(List<InventoryItem> newList) {
        this.inventoryList = newList;
        notifyDataSetChanged();
    }


    public static class InventoryViewHolder extends RecyclerView.ViewHolder {

        TextView textViewItemName;
        TextView textViewItemQuantity;
        Button buttonDelete;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewItemName = itemView.findViewById(R.id.textViewItemName);
            textViewItemQuantity = itemView.findViewById(R.id.textViewItemQuantity);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}