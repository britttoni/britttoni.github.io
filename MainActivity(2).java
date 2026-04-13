package com.example.cs360_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements InventoryAdapter.OnItemClickListener {

    private static final int SMS_PERMISSION_CODE = 100;

    private RecyclerView recyclerView;
    private DBHelper dbHelper;
    private InventoryAdapter adapter;

    // Full inventory list from database
    private ArrayList<InventoryItem> inventoryList;

    // What is currently shown on screen
    private ArrayList<InventoryItem> displayedList;

    // Faster lookup by item ID
    private HashMap<Integer, InventoryItem> inventoryMap;

    private EditText editTextItemName;
    private EditText editTextItemQuantity;
    private EditText editTextSearch;

    private Button buttonAddItem;
    private Button buttonSort;

    // Toggle so the sort button can switch back and forth
    private boolean sortByQuantity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewInventory);
        editTextItemName = findViewById(R.id.editTextItemName);
        editTextItemQuantity = findViewById(R.id.editTextItemQuantity);
        editTextSearch = findViewById(R.id.editTextSearch);
        buttonAddItem = findViewById(R.id.buttonAddItem);
        buttonSort = findViewById(R.id.buttonSort);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DBHelper(this);
        inventoryList = new ArrayList<>();
        displayedList = new ArrayList<>();
        inventoryMap = new HashMap<>();

        loadInventoryData();

        adapter = new InventoryAdapter(displayedList, this);
        recyclerView.setAdapter(adapter);

        // Add item button
        buttonAddItem.setOnClickListener(v -> addInventoryItem());

        // Search bar listener
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterInventory(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Sort button listener
        buttonSort.setOnClickListener(v -> toggleSort());
    }

    //Adds a new item after validating input.

    private void addInventoryItem() {
        String name = editTextItemName.getText().toString().trim();
        String quantityStr = editTextItemQuantity.getText().toString().trim();

        if (name.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, "Enter item name and quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Quantity must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (quantity < 0) {
            Toast.makeText(this, "Quantity cannot be negative", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.addItem(name, quantity)) {
            Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();

            editTextItemName.setText("");
            editTextItemQuantity.setText("");

            reloadInventoryData();
            requestSmsPermission();
        } else {
            Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
        }
    }

    //Loads all items from database into the main list, then refreshes the displayed list.

    private void loadInventoryData() {
        inventoryList.clear();

        Cursor cursor = dbHelper.getAllItems();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ITEM_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ITEM_NAME));
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ITEM_QUANTITY));

                inventoryList.add(new InventoryItem(id, name, quantity));
            } while (cursor.moveToNext());

            cursor.close();
        }

        // Build map for faster lookup
        inventoryMap = InventoryUtils.buildItemMap(inventoryList);

        // Default sort by name when data loads
        InventoryUtils.sortByName(inventoryList);

        displayedList.clear();
        displayedList.addAll(inventoryList);
    }

    //Reload database data and refresh list.

    private void reloadInventoryData() {
        loadInventoryData();

        String currentSearch = editTextSearch.getText().toString().trim();
        if (!currentSearch.isEmpty()) {
            filterInventory(currentSearch);
        } else {
            adapter.updateData(displayedList);
        }
    }

    //Filters displayed list based on search input.

    private void filterInventory(String query) {
        ArrayList<InventoryItem> filteredList = InventoryUtils.filterByName(inventoryList, query);

        // Keep current sort mode after filtering
        if (sortByQuantity) {
            InventoryUtils.sortByQuantityDescending(filteredList);
        } else {
            InventoryUtils.sortByName(filteredList);
        }

        displayedList.clear();
        displayedList.addAll(filteredList);
        adapter.updateData(displayedList);
    }

    //Toggles sorting between name and quantity.

    private void toggleSort() {
        sortByQuantity = !sortByQuantity;

        if (sortByQuantity) {
            InventoryUtils.sortByQuantityDescending(displayedList);
            buttonSort.setText("Sort by Name");
        } else {
            InventoryUtils.sortByName(displayedList);
            buttonSort.setText("Sort by Quantity");
        }

        adapter.updateData(displayedList);
    }

    @Override
    public void onDeleteClick(int id) {
        InventoryItem item = inventoryMap.get(id);

        if (dbHelper.deleteItem(id)) {
            Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();

            if (item != null) {
                inventoryMap.remove(id);
            }

            reloadInventoryData();
        } else {
            Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_CODE
            );
        } else {
            sendSmsNotification();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendSmsNotification();
        } else {
            Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSmsNotification() {
        String phoneNumber = "5551234567";
        String message = "Alert: New item added to inventory!";
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(this, "SMS sent!", Toast.LENGTH_SHORT).show();
    }
}