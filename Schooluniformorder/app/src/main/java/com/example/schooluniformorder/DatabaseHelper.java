
package com.example.schooluniformorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "UniformOrders.db";
    private static final int DB_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE orders (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "student_name TEXT NOT NULL, " +
                        "class_name TEXT NOT NULL, " +
                        "uniform_type TEXT NOT NULL, " +
                        "size TEXT NOT NULL, " +
                        "quantity INTEGER NOT NULL, " +
                        "price REAL NOT NULL, " +
                        "status TEXT NOT NULL DEFAULT 'Pending')"
        );
    }

    private boolean columnExists(
            SQLiteDatabase db, String table, String column) {

        Cursor cursor = db.rawQuery(
                "PRAGMA table_info(" + table + ")", null);

        try {
            int nameIndex = cursor.getColumnIndexOrThrow("name");

            while (cursor.moveToNext()) {
                if (column.equalsIgnoreCase(
                        cursor.getString(nameIndex))) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 2 &&
                !columnExists(db, "orders", "status")) {

            db.execSQL(
                    "ALTER TABLE orders ADD COLUMN " +
                            "status TEXT NOT NULL DEFAULT 'Pending'"
            );
        }
    }

    // CREATE
    public long insertOrder(
            String name, String className, String type,
            String size, int quantity, double price) {

        ContentValues values = new ContentValues();
        values.put("student_name", name);
        values.put("class_name", className);
        values.put("uniform_type", type);
        values.put("size", size);
        values.put("quantity", quantity);
        values.put("price", price);
        values.put("status", "Pending");

        return getWritableDatabase().insert(
                "orders", null, values);
    }

    // READ
    public Cursor getAllOrders() {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM orders ORDER BY id DESC",
                null);
    }

    // UPDATE order details
    public int updateOrder(
            int id, String name, String className,
            String type, String size,
            int quantity, double price) {

        ContentValues values = new ContentValues();
        values.put("student_name", name);
        values.put("class_name", className);
        values.put("uniform_type", type);
        values.put("size", size);
        values.put("quantity", quantity);
        values.put("price", price);

        return getWritableDatabase().update(
                "orders", values, "id=?",
                new String[]{String.valueOf(id)});
    }

    // DELETE
    public int deleteOrder(int id) {
        return getWritableDatabase().delete(
                "orders", "id=?",
                new String[]{String.valueOf(id)});
    }

    // SEARCH
    public Cursor searchOrders(String keyword) {
        String search = "%" + keyword + "%";

        return getReadableDatabase().rawQuery(
                "SELECT * FROM orders " +
                        "WHERE student_name LIKE ? " +
                        "OR class_name LIKE ? " +
                        "OR uniform_type LIKE ? " +
                        "ORDER BY id DESC",
                new String[]{search, search, search});
    }

    // UPDATE status
    public int updateOrderStatus(int id, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);

        return getWritableDatabase().update(
                "orders", values, "id=?",
                new String[]{String.valueOf(id)});
    }

    // DASHBOARD: total, pending, ready, delivered
    public int[] getDashboardCounts() {
        int[] counts = new int[4];

        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT " +
                        "COUNT(*), " +
                        "COALESCE(SUM(CASE WHEN status = 'Pending' " +
                        "THEN 1 ELSE 0 END), 0), " +
                        "COALESCE(SUM(CASE WHEN status = 'Ready for Collection' " +
                        "THEN 1 ELSE 0 END), 0), " +
                        "COALESCE(SUM(CASE WHEN status = 'Delivered' " +
                        "THEN 1 ELSE 0 END), 0) " +
                        "FROM orders", null);

        try {
            if (cursor.moveToFirst()) {
                counts[0] = cursor.getInt(0);
                counts[1] = cursor.getInt(1);
                counts[2] = cursor.getInt(2);
                counts[3] = cursor.getInt(3);
            }
        } finally {
            cursor.close();
        }

        return counts;
    }
}
