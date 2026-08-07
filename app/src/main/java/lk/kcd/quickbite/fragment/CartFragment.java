package lk.kcd.quickbite.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.adapter.CartAdapter;
import lk.kcd.quickbite.databinding.FragmentCartBinding;
import lk.kcd.quickbite.model.CartItem;
import lk.kcd.quickbite.model.Product;


public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private List<CartItem> cartItems;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCartBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {

            String uid = firebaseAuth.getCurrentUser().getUid();

            db.collection("users").document(uid).collection("cart").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot qds) {
                    if (!qds.isEmpty()) {
                        updateTotal();
                        cartItems = new ArrayList<>();

                        for (DocumentSnapshot ds : qds.getDocuments()) {
                            CartItem cartItem = ds.toObject(CartItem.class);
                            if (cartItem != null) {
                                String documentId = ds.getId();
                                cartItem.setDocumentId(documentId);

                                cartItems.add(cartItem);
                            }
                        }




                        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                        binding.cartCartItem.setLayoutManager(layoutManager);

                        CartAdapter adapter = new CartAdapter(cartItems);

                        adapter.setOnQuantityChangeListener(cartItem -> {
                            String documentId = cartItem.getDocumentId();
                            db.collection("users").document(uid)
                                    .collection("cart")
                                    .document(documentId)
                                    .update("quantity", cartItem.getQuantity())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Item quantity has been updated!", Toast.LENGTH_SHORT).show();
                                    });


                            updateTotal();
                        });

                        adapter.setOnRemoveListener(position -> {

                            String documentId = cartItems.get(position).getDocumentId();
                            db.collection("users").document(uid).collection("cart").document(documentId).delete().addOnSuccessListener(aVoid -> {
                                cartItems.remove(position);
                                adapter.notifyItemRemoved(position);
                                adapter.notifyItemRangeChanged(position, cartItems.size());
                                updateTotal();
                                Toast.makeText(getContext(), "Item has been removed!", Toast.LENGTH_SHORT).show();
                            });


                        });

                        binding.cartCartItem.setAdapter(adapter);
                        updateTotal();
                    }
                }
            });

        }

        binding.cartBtnProceed.setOnClickListener(v -> {
            CheckoutFragment checkoutFragment = new CheckoutFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, checkoutFragment)
                    .addToBackStack(null)
                    .commit();
        });

    }

    private void updateTotal() {
        if (cartItems == null || cartItems.isEmpty()) {
            binding.cartTextTotal.setText(String.format(Locale.US, "LKR %,.2f", 0.00));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<String> productIds = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            if (cartItem.getProductId() != null) { // ✅ null check here
                productIds.add(cartItem.getProductId());
            }
        }


        db.collection("products").whereIn("productId", productIds).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot qds) {

                Map<String, Product> productMap = new HashMap<>();

                qds.getDocuments().forEach(ds -> {
                    Product product = ds.toObject(Product.class);
                    if (product != null) {
                        productMap.put(product.getProductId(), product);
                    }
                });


//                        final double[] total = {0};
//
//                        cartItems.forEach(cartItem -> {
//                            Product product = productMap.get(cartItem.getProductId());
//
//                            if (product != null){
//                                total[0] += product.getPrice() * cartItem.getQuantity();
//                            }
//                        });


                double total = 0;
                for (CartItem cartItem : cartItems) {
                    Product product = productMap.get(cartItem.getProductId());
                    if (product != null) {
                        total += product.getPrice() * cartItem.getQuantity();
                    }
                }

                binding.cartTextTotal.setText(String.format(Locale.US, "LKR %,.2f", total));

            }
        });


    }
}