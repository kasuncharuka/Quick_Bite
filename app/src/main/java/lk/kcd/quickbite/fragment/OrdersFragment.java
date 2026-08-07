package lk.kcd.quickbite.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.adapter.OrderAdapter;
import lk.kcd.quickbite.databinding.FragmentOrdersBinding;
import lk.kcd.quickbite.model.CartItem;
import lk.kcd.quickbite.model.Order;

public class OrdersFragment extends Fragment {

    private static final String TAG = "OrdersFragment";
    private FragmentOrdersBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.ordersBtnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        binding.ordersBtnBrowse.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit());

        loadOrders();
    }

    private void loadOrders() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { showEmpty(); return; }

        binding.ordersProgress.setVisibility(View.VISIBLE);
        binding.ordersRecycler.setVisibility(View.GONE);
        binding.ordersEmptyState.setVisibility(View.GONE);

        // Try with orderBy first; if index missing, fall back without sort
        FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("userId", user.getUid())
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qds -> {
                    binding.ordersProgress.setVisibility(View.GONE);
                    if (qds.isEmpty()) { showEmpty(); return; }
                    showOrders(qds.toObjects(Order.class));
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Ordered query failed, trying without sort: " + e.getMessage());
                    // Fallback — no orderBy (works without composite index)
                    FirebaseFirestore.getInstance()
                            .collection("orders")
                            .whereEqualTo("userId", user.getUid())
                            .get()
                            .addOnSuccessListener(qds2 -> {
                                binding.ordersProgress.setVisibility(View.GONE);
                                if (qds2.isEmpty()) { showEmpty(); return; }
                                showOrders(qds2.toObjects(Order.class));
                            })
                            .addOnFailureListener(e2 -> {
                                binding.ordersProgress.setVisibility(View.GONE);
                                Log.e(TAG, "Orders load failed: " + e2.getMessage());
                                Toast.makeText(getContext(),
                                        "Could not load orders", Toast.LENGTH_SHORT).show();
                                showEmpty();
                            });
                });
    }

    private void showOrders(List<Order> orders) {
        binding.ordersRecycler.setVisibility(View.VISIBLE);
        binding.ordersEmptyState.setVisibility(View.GONE);
        binding.ordersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        OrderAdapter adapter = new OrderAdapter(orders);
        adapter.setOnReorderListener(order -> {
            if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                Toast.makeText(getContext(), "No items to reorder", Toast.LENGTH_SHORT).show();
                return;
            }
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            for (Order.OrderItem item : order.getOrderItems()) {
                CartItem cartItem = new CartItem(item.getProductId(), item.getQuantity(), null);
                FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .collection("cart").document().set(cartItem);
            }
            Toast.makeText(getContext(), "Items added to cart!", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CartFragment())
                    .addToBackStack(null).commit();
        });

        binding.ordersRecycler.setAdapter(adapter);
    }

    private void showEmpty() {
        binding.ordersEmptyState.setVisibility(View.VISIBLE);
        binding.ordersRecycler.setVisibility(View.GONE);
    }
}