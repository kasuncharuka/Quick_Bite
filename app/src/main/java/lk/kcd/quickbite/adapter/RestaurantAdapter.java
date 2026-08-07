package lk.kcd.quickbite.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.model.Category;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

    private final List<Category> items;
    private final OnRestaurantClickListener listener;
    private final FirebaseStorage storage;

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Category category);
    }

    public RestaurantAdapter(List<Category> items, OnRestaurantClickListener listener) {
        this.items = items;
        this.listener = listener;
        this.storage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category item = items.get(position);
        holder.name.setText(item.getName());


        holder.subtitle.setText("4.5★  ·  20–30 min");

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            storage.getReference(imageUrl)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri ->
                            Glide.with(holder.itemView.getContext())
                                    .load(uri)
                                    .centerCrop()
                                    .into(holder.image));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRestaurantClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        TextView subtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image    = itemView.findViewById(R.id.item_restaurant_image);
            name     = itemView.findViewById(R.id.item_restaurant_name);
            subtitle = itemView.findViewById(R.id.item_restaurant_subtitle);
        }
    }
}
