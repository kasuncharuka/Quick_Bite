package lk.kcd.quickbite.fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.List;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.adapter.CategoryAdapter;
import lk.kcd.quickbite.adapter.RestaurantAdapter;
import lk.kcd.quickbite.adapter.SectionAdapter;
import lk.kcd.quickbite.databinding.FragmentHomeBinding;
import lk.kcd.quickbite.helper.NetworkHelper;
import lk.kcd.quickbite.model.Category;
import lk.kcd.quickbite.model.Product;
import lk.kcd.quickbite.receiver.NetworkChangeReceiver;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setGreeting();
        loadCategories();
        loadFeaturedDishes();
        loadRestaurants();


        fetchLivePromoBanner();


        registerNetworkListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister listener to avoid memory leaks
        NetworkChangeReceiver.removeListener();
        binding = null;
    }


    private void fetchLivePromoBanner() {
        // Only attempt if we are online
        if (!NetworkChangeReceiver.isConnected(requireContext())) {
            showOfflineBanner();
            return;
        }

        NetworkHelper.get(NetworkHelper.PROMO_URL, new NetworkHelper.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                // Parse and apply to the promo card
                NetworkHelper.PromoData promo = NetworkHelper.parsePromo(responseBody);
                if (promo != null && binding != null) {
                    applyPromoBanner(promo);
                }
            }

            @Override
            public void onFailure(String errorMessage) {

                if (binding != null) {
                    Snackbar.make(binding.getRoot(),
                            "Could not load latest deals", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void applyPromoBanner(NetworkHelper.PromoData promo) {

        try {
            // Find by tag / position — adapt to your actual view IDs if you added @+id
            binding.homePromoBanner.setVisibility(View.VISIBLE);

            // binding.homePromoTitle.setText(promo.title);
            // binding.homePromoSubtitle.setText(promo.subtitle);
            // binding.homePromoCode.setText("Code: " + promo.code);
            // binding.homePromoBtn.setText(promo.btnLabel);
        } catch (Exception ignored) { /* IDs not bound — XML unchanged */ }
    }


    private void showOfflineBanner() {
        if (binding != null) {
            Snackbar.make(binding.getRoot(),
                            "⚠️ You're offline — showing cached data",
                            Snackbar.LENGTH_LONG)
                    .setAction("Dismiss", v -> { })
                    .show();
        }
    }


    private void registerNetworkListener() {
        NetworkChangeReceiver.setListener((isConnected, networkType) -> {
            if (binding == null) return;

            if (isConnected) {
                Snackbar.make(binding.getRoot(),
                                "✅ Back online (" + networkType + ") — refreshing…",
                                Snackbar.LENGTH_SHORT)
                        .show();

                fetchLivePromoBanner();
            } else {
                Snackbar.make(binding.getRoot(),
                                "⚠️ No internet connection",
                                Snackbar.LENGTH_LONG)
                        .setAction("Dismiss", v -> { })
                        .show();
            }
        });
    }


    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeGreeting;
        if (hour < 12)      timeGreeting = "Good morning";
        else if (hour < 17) timeGreeting = "Good afternoon";
        else                timeGreeting = "Good evening";

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = (user != null && user.getDisplayName() != null
                && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() : "there";

        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(ds -> {
                        if (binding == null) return;
                        if (ds.exists() && ds.getString("name") != null) {
                            binding.homeGreeting.setText(
                                    timeGreeting + ", " + ds.getString("name"));
                        } else {
                            binding.homeGreeting.setText(timeGreeting + ", " + name);
                        }
                    });
        } else {
            binding.homeGreeting.setText(timeGreeting + "!");
        }
    }

    private void loadCategories() {
        LinearLayoutManager lm = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.homeCategoriesRecycler.setLayoutManager(lm);

        FirebaseFirestore.getInstance().collection("categories").get()
                .addOnSuccessListener(qds -> {
                    if (binding == null || qds.isEmpty()) return;
                    List<Category> categories = qds.toObjects(Category.class);
                    CategoryAdapter adapter = new CategoryAdapter(categories, category -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("categoryId", category.getCategoryId());
                        ListingFragment fragment = new ListingFragment();
                        fragment.setArguments(bundle);
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                    });
                    binding.homeCategoriesRecycler.setAdapter(adapter);
                });
    }

    private void loadFeaturedDishes() {
        FirebaseFirestore.getInstance().collection("products")
                .limit(6).get()
                .addOnSuccessListener(qds -> {
                    if (binding == null || qds.isEmpty()) return;
                    List<Product> products = qds.toObjects(Product.class);
                    LinearLayoutManager lm = new LinearLayoutManager(
                            getContext(), LinearLayoutManager.HORIZONTAL, false);
                    binding.homeTopSellSection.itemSectionContainer.setLayoutManager(lm);
                    SectionAdapter adapter = new SectionAdapter(products, product -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("productId", product.getProductId());
                        ProductDetailsFragment detailsFragment = new ProductDetailsFragment();
                        detailsFragment.setArguments(bundle);
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, detailsFragment)
                                .addToBackStack(null)
                                .commit();
                    });
                    binding.homeTopSellSection.itemSectionContainer.setAdapter(adapter);
                });
    }

    private void loadRestaurants() {
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        binding.homeRestaurantsRecycler.setLayoutManager(lm);
        binding.homeRestaurantsRecycler.setNestedScrollingEnabled(false);

        FirebaseFirestore.getInstance().collection("categories").get()
                .addOnSuccessListener(qds -> {
                    if (binding == null || qds.isEmpty()) return;
                    List<Category> categories = qds.toObjects(Category.class);
                    RestaurantAdapter adapter = new RestaurantAdapter(categories, category -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("categoryId", category.getCategoryId());
                        ListingFragment fragment = new ListingFragment();
                        fragment.setArguments(bundle);
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                    });
                    binding.homeRestaurantsRecycler.setAdapter(adapter);
                });
    }
}