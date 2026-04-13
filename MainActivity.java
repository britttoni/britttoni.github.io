package com.example.cs360_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

// connects the inventory data to the RecyclerView
// displays each inventory item on the screen
public class MainActivity extends AppCompatActivity implements InventoryAdapter.OnItemClickListener {

    private static final int SMS_PERMISSION_CODE = 100;
    private static final String ITEM_ADDED_MESSAGE = "Alert: New item added to inventory!";

    private RecyclerView recyclerView;
    private DBHelper dbHelper;
    private InventoryAdapter adapter;
    private ArrayList<InventoryItem> inventoryList;
    private EditText editTextItemName;
    private EditText editTextItemQuantity;
    private Button buttonAddItem;

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
        buttonAddItem = findViewById(R.id.buttonAddItem);

        dbHelper = new DBHelper(this);
        inventoryList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter(inventoryList, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        buttonAddItem.setOnClickListener(v -> handleAddItem());
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

    private void clearItemInputs() {
        editTextItemName.setText("");
        editTextItemQuantity.setText("");
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
    }

    private void reloadInventoryData() {
        loadInventoryData();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDeleteClick(int id) {
        if (dbHelper.deleteItem(id)) {
            showToast("Item deleted.");
            reloadInventoryData();
        } else {
            showToast("Failed to delete item.");
        }
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
