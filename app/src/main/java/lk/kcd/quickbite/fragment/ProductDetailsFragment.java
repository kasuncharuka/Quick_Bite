package lk.kcd.quickbite.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.activity.LoginActivity;
import lk.kcd.quickbite.adapter.ProductSliderAdapter;
import lk.kcd.quickbite.adapter.SectionAdapter;
import lk.kcd.quickbite.databinding.FragmentProductDetailsBinding;
import lk.kcd.quickbite.model.CartItem;
import lk.kcd.quickbite.model.Product;

public class ProductDetailsFragment extends Fragment {

    private static final String TAG = "ProductDetailsFragment";

    private String productId;
    private int quantity = 1;
    private int availableQuantity = 0;

    private final Map<String, ChipGroup> attributeGroupMap = new HashMap<>();
    private FragmentProductDetailsBinding binding;

    public ProductDetailsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide bottom nav
        View nav = getActivity().findViewById(R.id.bottom_navigation_view);
        if (nav != null) nav.setVisibility(View.GONE);

        // Back press
        getActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });

        loadProduct();


        binding.productDetailsBtnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.productDetailsQty.setText(String.valueOf(quantity));
            }
        });

        binding.productDetailsBtnPlus.setOnClickListener(v -> {
            if (quantity < availableQuantity) {
                quantity++;
                binding.productDetailsQty.setText(String.valueOf(quantity));
            } else {
                Toast.makeText(getContext(), "Max stock reached", Toast.LENGTH_SHORT).show();
            }
        });


        binding.productDetailsBtnAddCart.setOnClickListener(v -> addToCart(false));


        binding.productDetailsBtnBuyNow.setOnClickListener(v -> {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL,
                    android.net.Uri.parse("tel:0785927334"));
            startActivity(dialIntent);
        });
    }

    private void loadProduct() {
        FirebaseFirestore.getInstance()
                .collection("products")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(qds -> {
                    if (qds.isEmpty()) return;

                    Product product = qds.getDocuments().get(0).toObject(Product.class);
                    if (product == null) return;


                    ProductSliderAdapter sliderAdapter =
                            new ProductSliderAdapter(product.getImages());
                    binding.productImageSlider.setAdapter(sliderAdapter);
                    binding.dotsIndicator.attachTo(binding.productImageSlider);


                    binding.productDetailsTitle.setText(product.getTitle());
                    binding.productDetailsRating.setRating(product.getRating());
                    binding.productDetailsPrice.setText(
                            String.format("LKR %,.0f", product.getPrice()));
                    binding.productDetailsAvbQty.setText(
                            String.valueOf(product.getStockCount()));
                    availableQuantity = product.getStockCount();


                    if (product.getAttributes() != null && !product.getAttributes().isEmpty()) {
                        for (Product.Attribute attr : product.getAttributes()) {
                            renderAttribute(attr, binding.productDetailsAttributeContainer);
                        }

                        binding.productDetailsAttributeCard.setVisibility(View.VISIBLE);
                    }

                    loadRelatedProducts();
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Product load failed: " + e.getMessage()));
    }

    private void addToCart(boolean goToCheckout) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            return;
        }

        List<CartItem.Attribute> attributes = getFinalSelection();
        CartItem cartItem = new CartItem(productId, quantity, attributes);

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("cart").document()
                .set(cartItem)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(),
                            goToCheckout ? "Proceeding to checkout…" : "Added to cart!",
                            Toast.LENGTH_SHORT).show();

                    if (goToCheckout) {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new CartFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void renderAttribute(Product.Attribute attribute, ViewGroup container) {
        // Skip color attributes entirely — not relevant for food products
        if ("color".equalsIgnoreCase(attribute.getType())) return;
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = 16;
        row.setLayoutParams(rowParams);


        TextView label = new TextView(getContext());
        label.setText(attribute.getName());
        label.setTextSize(13f);
        label.setTextColor(
                resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        label.setPadding(0, 0, 0, 8);
        row.addView(label);


        ChipGroup group = new ChipGroup(getContext());
        group.setSelectionRequired(true);
        group.setSingleSelection(true);

        if (attribute.getValues() != null) {
            for (String value : attribute.getValues()) {
                Chip chip = new Chip(getContext());
                chip.setCheckable(true);
                chip.setTag(value);

                if ("color".equalsIgnoreCase(attribute.getType())) {
                    try {
                        chip.setChipBackgroundColor(
                                ColorStateList.valueOf(Color.parseColor(value)));
                        chip.setChipStrokeWidth(2f);
                        chip.setChipStrokeColor(ColorStateList.valueOf(
                                resolveAttrColor(com.google.android.material.R.attr.colorOutline)));
                    } catch (IllegalArgumentException e) {
                        chip.setText(value);
                    }
                } else {
                    chip.setText(value);
                }

                group.addView(chip);
            }
        }

        row.addView(group);
        container.addView(row);
        attributeGroupMap.put(attribute.getName(), group);
    }


    private List<CartItem.Attribute> getFinalSelection() {
        List<CartItem.Attribute> attributes = new ArrayList<>();

        for (Map.Entry<String, ChipGroup> entry : attributeGroupMap.entrySet()) {
            String name = entry.getKey();
            ChipGroup chipGroup = entry.getValue();

            int checkedId = chipGroup.getCheckedChipId();
            if (checkedId == View.NO_ID) continue;

            View checkedView = chipGroup.findViewById(checkedId);
            if (checkedView == null) continue;

            Object tag = checkedView.getTag();
            if (tag == null) continue;

            attributes.add(new CartItem.Attribute(name, tag.toString()));
        }

        return attributes;
    }

    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void loadRelatedProducts() {
        FirebaseFirestore.getInstance()
                .collection("products")
                .whereNotEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(qds -> {
                    if (qds.isEmpty()) return;
                    List<Product> products = qds.toObjects(Product.class);

                    LinearLayoutManager lm = new LinearLayoutManager(
                            getContext(), LinearLayoutManager.HORIZONTAL, false);
                    binding.productDetailsTopSellSection.itemSectionContainer.setLayoutManager(lm);

                    SectionAdapter adapter = new SectionAdapter(products, product -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("productId", product.getProductId());

                        ProductDetailsFragment next = new ProductDetailsFragment();
                        next.setArguments(bundle);

                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, next)
                                .addToBackStack(null)
                                .commit();
                    });

                    binding.productDetailsTopSellSection.itemSectionContainer.setAdapter(adapter);
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        View nav = getActivity().findViewById(R.id.bottom_navigation_view);
        if (nav != null) nav.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        View nav = getActivity().findViewById(R.id.bottom_navigation_view);
        if (nav != null) nav.setVisibility(View.GONE);
    }
}