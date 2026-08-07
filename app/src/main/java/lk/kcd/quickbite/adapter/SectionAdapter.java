package lk.kcd.quickbite.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.model.Product;

public class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.ViewHolder> {

    private final List<Product> products;
    private final OnListingItemClickListener listener;

    public SectionAdapter(List<Product> products, OnListingItemClickListener listener) {

        this.products = products != null ? products : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_recycler, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        if (product == null) return;


        holder.productTitle.setText(product.getTitle() != null
                ? product.getTitle() : "—");


        holder.productPrice.setText(
                String.format(Locale.US, "LKR %,.0f", product.getPrice()));


        if (product.getImages() != null && !product.getImages().isEmpty()
                && product.getImages().get(0) != null) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImages().get(0))
                    .centerCrop()
                    .placeholder(R.drawable.home_24)
                    .error(R.drawable.home_24)
                    .into(holder.productImage);
        } else {

            holder.productImage.setImageResource(R.drawable.home_24);
        }


        holder.itemView.setOnClickListener(v -> {
            Animation animation = AnimationUtils.loadAnimation(
                    v.getContext(), R.anim.click_animation);
            v.startAnimation(animation);
            if (listener != null) listener.onListingItemClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productTitle;
        TextView productPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.item_product_r_image);
            productTitle = itemView.findViewById(R.id.item_product_r_name);
            productPrice = itemView.findViewById(R.id.item_product_r_price);
        }
    }

    public interface OnListingItemClickListener {
        void onListingItemClick(Product product);
    }
}