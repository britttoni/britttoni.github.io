package com.example.cs360_project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "InventoryApp.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    public static final String TABLE_INVENTORY = "inventory";
    public static final String COLUMN_ITEM_ID = "id";
    public static final String COLUMN_ITEM_NAME = "name";
    public static final String COLUMN_ITEM_QUANTITY = "quantity";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, "
                + COLUMN_PASSWORD + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_INVENTORY + " ("
                + COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_ITEM_NAME + " TEXT NOT NULL, "
                + COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        onCreate(db);
    }

    // Checks whether username and password is valid.

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPassword = hashPassword(password);

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, hashedPassword},
                null,
                null,
                null
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean checkUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + "=?",
                new String[]{username},
                null,
                null,
                null
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    //registers a new user

    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username.trim());
        values.put(COLUMN_PASSWORD, hashPassword(password));

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean addItem(String name, int quantity) {
        if (name == null || name.trim().isEmpty() || quantity < 0) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, name.trim());
        values.put(COLUMN_ITEM_QUANTITY, quantity);

        long result = db.insert(TABLE_INVENTORY, null, values);
        return result != -1;
    }


    // crud operations for inventory items
    public boolean updateItem(int id, String name, int quantity) {
        if (id <= 0 || name == null || name.trim().isEmpty() || quantity < 0) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, name.trim());
        values.put(COLUMN_ITEM_QUANTITY, quantity);

        int rowsAffected = db.update(
                TABLE_INVENTORY,
                values,
                COLUMN_ITEM_ID + "=?",
                new String[]{String.valueOf(id)}
        );

        return rowsAffected > 0;
    }

    //returns all items sorted alphabetically by name.

    public Cursor getAllItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_INVENTORY,
                null,
                null,
                null,
                null,
                null,
                COLUMN_ITEM_NAME + " COLLATE NOCASE ASC"
        );
    }


    public Cursor searchItemsByName(String searchText) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_INVENTORY,
                null,
                COLUMN_ITEM_NAME + " LIKE ?",
                new String[]{"%" + searchText + "%"},
                null,
                null,
                COLUMN_ITEM_NAME + " COLLATE NOCASE ASC"
        );
    }

    public boolean deleteItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(
                TABLE_INVENTORY,
                COLUMN_ITEM_ID + "=?",
                new String[]{String.valueOf(id)}
        );
        return rowsDeleted > 0;
    }

    // SHA-256 hashing
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();

            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }
}
