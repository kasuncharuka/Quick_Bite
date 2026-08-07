package lk.kcd.quickbite.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.res.ColorStateList;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.model.Order;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private static final String TAG = "OrderAdapter";

    private final List<Order> orders;
    private OnReorderListener reorderListener;

    public interface OnReorderListener {
        void onReorder(Order order);
    }

    public OrderAdapter(List<Order> orders) {
        this.orders = orders;
    }

    public void setOnReorderListener(OnReorderListener listener) {
        this.reorderListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);


        String id = order.getOrderId();
        String shortId = (id != null && id.length() > 8)
                ? id.substring(id.length() - 8) : (id != null ? id : "N/A");
        holder.orderId.setText("Order #" + shortId);


        if (order.getOrderDate() != null) {
            long millis = order.getOrderDate().toDate().getTime();
            if (millis > 0) {
                holder.orderDate.setText(new SimpleDateFormat(
                        "dd MMM yyyy  ·  hh:mm a", Locale.getDefault())
                        .format(order.getOrderDate().toDate()));
            } else {
                holder.orderDate.setText("Date not recorded");
            }
        } else {
            holder.orderDate.setText("Date not recorded");
        }


        String status = (order.getStatus() != null && !order.getStatus().isEmpty())
                ? order.getStatus() : "PENDING";
        holder.orderStatus.setText(status);

        int badgeColor, textColor;
        switch (status.toUpperCase()) {
            case "PAID": case "DELIVERED":
                badgeColor = 0xFFC8E6C9; textColor = 0xFF1B5E20; break;
            case "CANCELLED":
                badgeColor = 0xFFFFCDD2; textColor = 0xFFB71C1C; break;
            default:
                badgeColor = 0xFFFFF9C4; textColor = 0xFFF57F17; break;
        }
        holder.orderStatus.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
        holder.orderStatus.setTextColor(textColor);


        holder.orderTotal.setText(order.getTotalAmount() > 0
                ? String.format(Locale.US, "LKR %,.2f", order.getTotalAmount())
                : "LKR —");


        holder.productsContainer.removeAllViews();

        holder.productsContainer.setTag(order.getOrderId());

        List<Order.OrderItem> items = order.getOrderItems();

        if (items == null || items.isEmpty()) {
            addProductRow(holder, "No items recorded", 0, 0.0, null);
        } else {
            List<String> productIds = new ArrayList<>();
            for (Order.OrderItem item : items) {
                if (item.getProductId() != null) productIds.add(item.getProductId());
            }

            if (productIds.isEmpty()) {
                addProductRow(holder, "No product info", 0, 0.0, null);
            } else {
                // Skeleton rows while loading
                for (Order.OrderItem item : items) {
                    addProductRow(holder, "Loading…", item.getQuantity(), item.getUnitPrice(), null);
                }

                final String expectedTag = order.getOrderId();

                FirebaseFirestore.getInstance()
                        .collection("products")
                        .whereIn("productId", productIds)
                        .get()
                        .addOnSuccessListener(qds -> {
                            // Guard: skip if this ViewHolder was recycled for a different order
                            if (!expectedTag.equals(holder.productsContainer.getTag())) return;

                            Map<String, String> titleMap    = new HashMap<>();
                            Map<String, String> imageUrlMap = new HashMap<>();

                            qds.getDocuments().forEach(ds -> {
                                String pid = ds.getString("productId");
                                if (pid == null) return;

                                // Title — try multiple field names
                                String title = ds.getString("title");
                                if (title == null) title = ds.getString("name");
                                if (title != null) titleMap.put(pid, title);

                                // Image — "images" is a List<String>, grab the first element
                                String img = null;
                                List<String> imgList = (List<String>) ds.get("images");
                                if (imgList != null && !imgList.isEmpty()) {
                                    img = imgList.get(0);
                                }
                                // Fallback: single-value field names
                                if (img == null) img = ds.getString("imageUrl");
                                if (img == null) img = ds.getString("imageURL");
                                if (img == null) img = ds.getString("image");

                                if (img != null) {
                                    imageUrlMap.put(pid, img);
                                }
                            });

                            holder.productsContainer.removeAllViews();
                            for (Order.OrderItem item : items) {
                                String pid   = item.getProductId();
                                String title = titleMap.containsKey(pid)
                                        ? titleMap.get(pid) : "Unknown product";
                                String img   = imageUrlMap.get(pid);
                                addProductRow(holder, title, item.getQuantity(), item.getUnitPrice(), img);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (!expectedTag.equals(holder.productsContainer.getTag())) return;
                            holder.productsContainer.removeAllViews();
                            for (Order.OrderItem item : items) {
                                addProductRow(holder,
                                        "Product: " + item.getProductId(),
                                        item.getQuantity(), item.getUnitPrice(), null);
                            }
                        });
            }
        }


        holder.btnReorder.setOnClickListener(v -> {
            if (reorderListener != null) reorderListener.onReorder(order);
        });
    }


    private void loadImage(ImageView imgView, String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imgView.setImageResource(R.drawable.shopping_cart_24);
            return;
        }

        if (imageUrl.startsWith("gs://")) {

            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
            ref.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Glide.with(imgView.getContext())
                                .load(uri.toString())
                                .placeholder(R.drawable.shopping_cart_24)
                                .error(R.drawable.shopping_cart_24)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .centerCrop()
                                .into(imgView);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL for: " + imageUrl, e);
                        imgView.setImageResource(R.drawable.shopping_cart_24);
                    });
        } else {

            Glide.with(imgView.getContext())
                    .load(imageUrl)

                    .error(R.drawable.shopping_cart_24)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imgView);
        }
    }


    private void addProductRow(ViewHolder holder,
                               String name, int qty, double unitPrice, String imageUrl) {
        View row = LayoutInflater.from(holder.itemView.getContext())
                .inflate(R.layout.item_order_product_row, holder.productsContainer, false);

        ImageView imgView = row.findViewById(R.id.order_product_image);
        TextView  tvName  = row.findViewById(R.id.order_product_name);
        TextView  tvQty   = row.findViewById(R.id.order_product_qty);
        TextView  tvPrice = row.findViewById(R.id.order_product_price);

        tvName.setText(name);
        tvQty.setText(qty > 0 ? "×" + qty : "");
        tvPrice.setText(unitPrice > 0
                ? String.format(Locale.US, "LKR %,.0f", unitPrice) : "");

        loadImage(imgView, imageUrl);

        holder.productsContainer.addView(row);
    }

    @Override
    public int getItemCount() { return orders.size(); }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, orderDate, orderStatus, orderTotal;
        android.widget.LinearLayout productsContainer;
        MaterialButton btnReorder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId           = itemView.findViewById(R.id.order_item_id);
            orderDate         = itemView.findViewById(R.id.order_item_date);
            orderStatus       = itemView.findViewById(R.id.order_item_status);
            orderTotal        = itemView.findViewById(R.id.order_item_total);
            productsContainer = itemView.findViewById(R.id.order_item_products_container);
            btnReorder        = itemView.findViewById(R.id.order_item_btn_reorder);
        }
    }
}