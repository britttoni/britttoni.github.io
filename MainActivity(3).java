package com.example.cs360_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
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
    private static final String ITEM_ADDED_MESSAGE = "Alert: New item added to inventory!";

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

    // Toggle so the sort button can switch between name and quantity
    private boolean sortByQuantity = false;

    // Tracks update mode for full CRUD
    private boolean isUpdating = false;
    private int updatingItemId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        loadInventoryData();
        setupClickListeners();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewInventory);
        editTextItemName = findViewById(R.id.editTextItemName);
        editTextItemQuantity = findViewById(R.id.editTextItemQuantity);
        editTextSearch = findViewById(R.id.editTextSearch);
        buttonAddItem = findViewById(R.id.buttonAddItem);
        buttonSort = findViewById(R.id.buttonSort);

        dbHelper = new DBHelper(this);
        inventoryList = new ArrayList<>();
        displayedList = new ArrayList<>();
        inventoryMap = new HashMap<>();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter(displayedList, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        buttonAddItem.setOnClickListener(v -> {
            if (isUpdating) {
                handleUpdateItem();
            } else {
                handleAddItem();
            }
        });

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

        buttonSort.setOnClickListener(v -> toggleSort());
    }

    private void handleAddItem() {
        String itemName = ValidationUtils.normalizeItemName(
                editTextItemName.getText().toString()
        );

        String quantityText = editTextItemQuantity.getText().toString().trim();
        Integer quantity = ValidationUtils.parsePositiveQuantity(quantityText);

        if (ValidationUtils.isBlank(itemName)) {
            showToast("Enter an item name.");
            return;
        }

        if (quantity == null) {
            showToast("Enter a valid quantity greater than 0.");
            return;
        }

        if (dbHelper.addItem(itemName, quantity)) {
            showToast("Item added.");
            clearItemInputs();
            reloadInventoryData();
            requestSmsPermissionIfNeeded();
        } else {
            showToast("Failed to add item.");
        }
    }

    private void handleUpdateItem() {
        String itemName = ValidationUtils.normalizeItemName(
                editTextItemName.getText().toString()
        );

        String quantityText = editTextItemQuantity.getText().toString().trim();
        Integer quantity = ValidationUtils.parsePositiveQuantity(quantityText);

        if (ValidationUtils.isBlank(itemName)) {
            showToast("Enter an item name.");
            return;
        }

        if (quantity == null) {
            showToast("Enter a valid quantity greater than 0.");
            return;
        }

        if (dbHelper.updateItem(updatingItemId, itemName, quantity)) {
            showToast("Item updated.");
            resetUpdateMode();
            reloadInventoryData();
        } else {
            showToast("Update failed.");
        }
    }

    private void clearItemInputs() {
        editTextItemName.setText("");
        editTextItemQuantity.setText("");
    }

    private void resetUpdateMode() {
        isUpdating = false;
        updatingItemId = -1;
        buttonAddItem.setText("Add Item");
        clearItemInputs();
    }

    private void loadInventoryData() {
        inventoryList.clear();

        Cursor cursor = dbHelper.getAllItems();

        if (cursor == null) {
            return;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ITEM_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ITEM_NAME));
                    int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ITEM_QUANTITY));

                    inventoryList.add(new InventoryItem(id, name, quantity));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        // Build map for faster lookup
        inventoryMap = InventoryUtils.buildItemMap(inventoryList);

        // Default sort by name when data loads
        InventoryUtils.sortByName(inventoryList);

        displayedList.clear();
        displayedList.addAll(inventoryList);
    }

    private void reloadInventoryData() {
        loadInventoryData();

        String currentSearch = editTextSearch.getText().toString().trim();
        if (!currentSearch.isEmpty()) {
            filterInventory(currentSearch);
        } else {
            adapter.updateData(displayedList);
        }
    }

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
            showToast("Item deleted.");

            if (item != null) {
                inventoryMap.remove(id);
            }

            if (isUpdating && updatingItemId == id) {
                resetUpdateMode();
            }

            reloadInventoryData();
        } else {
            showToast("Failed to delete item.");
        }
    }

    @Override
    public void onUpdateClick(InventoryItem item) {
        isUpdating = true;
        updatingItemId = item.getId();

        editTextItemName.setText(item.getName());
        editTextItemQuantity.setText(String.valueOf(item.getQuantity()));
        buttonAddItem.setText("Update Item");

        showToast("Edit item then press Update.");
    }

    private void requestSmsPermissionIfNeeded() {
        if (!AppPreferences.hasNotificationPhoneNumber(this)) {
            showToast("No SMS phone number saved, so notification was skipped.");
            return;
        }

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

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSmsNotification();
            } else {
                showToast("SMS permission denied.");
            }
        }
    }

    private void sendSmsNotification() {
        String phoneNumber = AppPreferences.getNotificationPhoneNumber(this);
        boolean smsSent = SmsUtils.sendSms(this, phoneNumber, ITEM_ADDED_MESSAGE);

        if (smsSent) {
            showToast("SMS sent.");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}