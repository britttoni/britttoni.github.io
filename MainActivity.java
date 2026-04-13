package com.example.cs360_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements InventoryAdapter.OnItemClickListener {
    private static final int SMS_PERMISSION_CODE = 100;
    RecyclerView recyclerView;
    DBHelper dbHelper;
    InventoryAdapter adapter;
    ArrayList<InventoryItem> inventoryList;
    EditText editTextItemName, editTextItemQuantity;
    Button buttonAddItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewInventory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        editTextItemName = findViewById(R.id.editTextItemName);
        editTextItemQuantity = findViewById(R.id.editTextItemQuantity);
        buttonAddItem = findViewById(R.id.buttonAddItem);
        dbHelper = new DBHelper(this);
        inventoryList = new ArrayList<>();
        loadInventoryData();
        adapter = new InventoryAdapter(inventoryList, this);
        recyclerView.setAdapter(adapter);

        buttonAddItem.setOnClickListener(v -> {
            String name = editTextItemName.getText().toString().trim();
            String quantityStr = editTextItemQuantity.getText().toString().trim();
            if (name.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(this, "Enter item name and quantity", Toast.LENGTH_SHORT).show();
            } else {
                int quantity = Integer.parseInt(quantityStr);
                if (dbHelper.addItem(name, quantity)) {
                    Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
                    reloadInventoryData();
                    requestSmsPermission();
                } else {
                    Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

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
    }

    private void reloadInventoryData() {
        loadInventoryData();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDeleteClick(int id) {
        if (dbHelper.deleteItem(id)) {
            Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
            reloadInventoryData();
        } else {
            Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        } else {
            sendSmsNotification();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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