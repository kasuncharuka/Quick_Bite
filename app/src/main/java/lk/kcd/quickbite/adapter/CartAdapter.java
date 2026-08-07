package lk.kcd.quickbite.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Locale;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.model.CartItem;
import lk.kcd.quickbite.model.Product;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<CartItem> cartItems;
    private OnQuantityChangeListener changeListener;
    private OnRemoveListener removeListener;

    public CartAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.changeListener = listener;
    }

    public void setOnRemoveListener(OnRemoveListener listener) {
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cart_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products").whereEqualTo("productId", cartItem.getProductId()).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot qds) {
                if (!qds.isEmpty()) {

                    int currentPosition = holder.getAbsoluteAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION) {
                        return;
                    }


                    Product product = qds.getDocuments().get(0).toObject(Product.class);

                    holder.productTitle.setText(product.getTitle());
                    holder.productPrice.setText(String.format(Locale.US, "LKR %,.2f", product.getPrice()));
                    holder.productQuantity.setText(String.valueOf(cartItem.getQuantity()));

                    Glide.with(holder.itemView.getContext())
                            .load(product.getImages().get(0))
                            .centerCrop()
                            .into(holder.productImage);


                    holder.btnPlus.setOnClickListener(v -> {
                        if (cartItem.getQuantity() < product.getStockCount()) {
                            cartItem.setQuantity(cartItem.getQuantity() + 1);
                            notifyItemChanged(currentPosition);
                            if (changeListener != null) {
                                changeListener.onChanged(cartItem);
                            }
                        }
                    });

                    holder.btnMinus.setOnClickListener(v -> {
                        if (cartItem.getQuantity() > 1) {
                            cartItem.setQuantity(cartItem.getQuantity() - 1);
                            notifyItemChanged(currentPosition);
                            if (changeListener != null) {
                                changeListener.onChanged(cartItem);
                            }
                        }

                    });


                    holder.btnRemove.setOnClickListener(v -> {
                        int pos = holder.getAbsoluteAdapterPosition();
                        Log.i("Position", String.valueOf(pos));
                        if (pos != RecyclerView.NO_POSITION && removeListener != null) {
                            removeListener.onRemoved(currentPosition);
                        }
                    });


                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productTitle;
        TextView productPrice;
        TextView productQuantity;
        AppCompatButton btnPlus;
        AppCompatButton btnMinus;
        ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.item_cart_image);
            productTitle = itemView.findViewById(R.id.item_cart_title);
            productPrice = itemView.findViewById(R.id.item_cart_price);
            productQuantity = itemView.findViewById(R.id.item_cart_quantity);
            btnPlus = itemView.findViewById(R.id.item_cart_btn_plus);
            btnMinus = itemView.findViewById(R.id.item_cart_btn_minus);
            btnRemove = itemView.findViewById(R.id.item_cart_remove);
        }
    }

    public interface OnQuantityChangeListener {
        void onChanged(CartItem cartItem);
    }

    public interface OnRemoveListener {
        void onRemoved(int position);
    }
}
