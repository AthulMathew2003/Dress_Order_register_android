
package com.example.schooluniformorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper db;
    private EditText etSearch;
    private ListView listOrders;
    private ArrayAdapter<String> adapter;

    private TextView tvTotalOrders;
    private TextView tvPendingOrders;
    private TextView tvReadyOrders;
    private TextView tvDeliveredOrders;

    private final ArrayList<Integer> orderIds = new ArrayList<>();
    private final ArrayList<String> orderDetails = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);

        listOrders = findViewById(R.id.listOrders);
        etSearch = findViewById(R.id.etSearch);

        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);
        tvReadyOrders = findViewById(R.id.tvReadyOrders);
        tvDeliveredOrders = findViewById(R.id.tvDeliveredOrders);

        Button btnAddOrder = findViewById(R.id.btnAddOrder);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                orderDetails
        );

        listOrders.setAdapter(adapter);

        btnAddOrder.setOnClickListener(
                v -> showOrderDialog(null)
        );

        // Search while typing.
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                searchOrders(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Tap an order to edit it or update its status.
        listOrders.setOnItemClickListener(
                (parent, view, position, id) -> {
                    showOrderActionsDialog(orderIds.get(position));
                }
        );

        // Long press an order to delete it.
        listOrders.setOnItemLongClickListener(
                (parent, view, position, id) -> {
                    confirmDelete(orderIds.get(position));
                    return true;
                }
        );

        refreshAll();
    }

    // Refresh dashboard and order list.
    private void refreshAll() {
        updateDashboard();

        String keyword = etSearch.getText().toString().trim();

        if (keyword.isEmpty()) {
            loadOrders();
        } else {
            searchOrders(keyword);
        }
    }

    // Update the four dashboard cards from SQLite.
    private void updateDashboard() {
        int[] counts = db.getDashboardCounts();

        tvTotalOrders.setText(String.valueOf(counts[0]));
        tvPendingOrders.setText(String.valueOf(counts[1]));
        tvReadyOrders.setText(String.valueOf(counts[2]));
        tvDeliveredOrders.setText(String.valueOf(counts[3]));
    }

    private void showOrderActionsDialog(int orderId) {
        String[] actions = {
                "Edit Order",
                "Update Order Status"
        };

        new AlertDialog.Builder(this)
                .setTitle("Order #" + orderId)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showOrderDialog(orderId);
                    } else {
                        showStatusDialog(orderId);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStatusDialog(int orderId) {
        String[] statuses = {
                "Pending",
                "Processing",
                "Ready for Collection",
                "Delivered"
        };

        new AlertDialog.Builder(this)
                .setTitle("Update Order Status")
                .setItems(statuses, (dialog, which) -> {
                    int result = db.updateOrderStatus(
                            orderId, statuses[which]);

                    if (result > 0) {
                        Toast.makeText(
                                this,
                                "Status updated",
                                Toast.LENGTH_SHORT
                        ).show();

                        refreshAll();
                    } else {
                        Toast.makeText(
                                this,
                                "Could not update status",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadOrders() {
        displayOrders(db.getAllOrders());
    }

    private void searchOrders(String keyword) {
        displayOrders(db.searchOrders(keyword.trim()));
    }

    private void displayOrders(Cursor cursor) {
        orderIds.clear();
        orderDetails.clear();

        try {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id"));

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("student_name"));

                String className = cursor.getString(
                        cursor.getColumnIndexOrThrow("class_name"));

                String type = cursor.getString(
                        cursor.getColumnIndexOrThrow("uniform_type"));

                String size = cursor.getString(
                        cursor.getColumnIndexOrThrow("size"));

                int quantity = cursor.getInt(
                        cursor.getColumnIndexOrThrow("quantity"));

                double price = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("price"));

                String status = cursor.getString(
                        cursor.getColumnIndexOrThrow("status"));

                double total = quantity * price;

                orderIds.add(id);

                orderDetails.add(
                        "Order #" + id +
                                "\nStudent: " + name +
                                "\nClass: " + className +
                                "\nUniform: " + type +
                                " | Size: " + size +
                                "\nQuantity: " + quantity +
                                " | Unit Price: ₹" +
                                String.format(Locale.getDefault(), "%.2f", price) +
                                "\nTotal: ₹" +
                                String.format(Locale.getDefault(), "%.2f", total) +
                                "\nStatus: " + status
                );
            }
        } finally {
            cursor.close();
        }

        adapter.notifyDataSetChanged();
    }

    private void showOrderDialog(Integer orderId) {
        boolean isEdit = orderId != null;

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(40, 12, 40, 4);

        EditText name = createField("Student name");
        EditText className = createField("Class and section (e.g. 8A)");
        EditText type = createField("Uniform type (e.g. Shirt)");
        EditText size = createField("Size (e.g. 32)");
        EditText quantity = createField("Quantity");
        EditText price = createField("Unit price (₹)");

        quantity.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER);

        price.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        form.addView(name);
        form.addView(className);
        form.addView(type);
        form.addView(size);
        form.addView(quantity);
        form.addView(price);

        // Load existing values for editing.
        if (isEdit) {
            Cursor cursor = db.getAllOrders();

            try {
                while (cursor.moveToNext()) {
                    int currentId = cursor.getInt(
                            cursor.getColumnIndexOrThrow("id"));

                    if (currentId == orderId) {
                        name.setText(cursor.getString(
                                cursor.getColumnIndexOrThrow("student_name")));

                        className.setText(cursor.getString(
                                cursor.getColumnIndexOrThrow("class_name")));

                        type.setText(cursor.getString(
                                cursor.getColumnIndexOrThrow("uniform_type")));

                        size.setText(cursor.getString(
                                cursor.getColumnIndexOrThrow("size")));

                        quantity.setText(String.valueOf(
                                cursor.getInt(
                                        cursor.getColumnIndexOrThrow("quantity"))));

                        price.setText(String.valueOf(
                                cursor.getDouble(
                                        cursor.getColumnIndexOrThrow("price"))));

                        break;
                    }
                }
            } finally {
                cursor.close();
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Edit Uniform Order" : "Add Uniform Order")
                .setView(form)
                .setPositiveButton(isEdit ? "Update" : "Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {

                            String student = name.getText()
                                    .toString().trim();
                            String cls = className.getText()
                                    .toString().trim();
                            String uniform = type.getText()
                                    .toString().trim();
                            String uniformSize = size.getText()
                                    .toString().trim();
                            String qtyText = quantity.getText()
                                    .toString().trim();
                            String priceText = price.getText()
                                    .toString().trim();

                            if (student.isEmpty() || cls.isEmpty()
                                    || uniform.isEmpty()
                                    || uniformSize.isEmpty()
                                    || qtyText.isEmpty()
                                    || priceText.isEmpty()) {

                                Toast.makeText(
                                        this,
                                        "Please fill in all fields",
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            int qty;
                            double unitPrice;

                            try {
                                qty = Integer.parseInt(qtyText);
                                unitPrice = Double.parseDouble(priceText);

                                if (qty <= 0
                                        || Double.isNaN(unitPrice)
                                        || Double.isInfinite(unitPrice)
                                        || unitPrice < 0) {
                                    throw new NumberFormatException();
                                }
                            } catch (NumberFormatException e) {
                                Toast.makeText(
                                        this,
                                        "Enter a valid quantity and price",
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            if (isEdit) {
                                int result = db.updateOrder(
                                        orderId,
                                        student,
                                        cls,
                                        uniform,
                                        uniformSize,
                                        qty,
                                        unitPrice
                                );

                                if (result <= 0) {
                                    Toast.makeText(
                                            this,
                                            "Could not update order",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    return;
                                }

                                Toast.makeText(
                                        this,
                                        "Order updated",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {
                                long result = db.insertOrder(
                                        student,
                                        cls,
                                        uniform,
                                        uniformSize,
                                        qty,
                                        unitPrice
                                );

                                if (result == -1) {
                                    Toast.makeText(
                                            this,
                                            "Could not save order",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    return;
                                }

                                Toast.makeText(
                                        this,
                                        "Order added",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            refreshAll();
                            dialog.dismiss();
                        })
        );

        dialog.show();
    }

    private EditText createField(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setTextSize(16);
        return editText;
    }

    private void confirmDelete(int orderId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Order")
                .setMessage(
                        "Are you sure you want to delete order #" +
                                orderId + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int result = db.deleteOrder(orderId);

                    if (result > 0) {
                        Toast.makeText(
                                this,
                                "Order deleted",
                                Toast.LENGTH_SHORT
                        ).show();

                        refreshAll();
                    } else {
                        Toast.makeText(
                                this,
                                "Could not delete order",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }
}
