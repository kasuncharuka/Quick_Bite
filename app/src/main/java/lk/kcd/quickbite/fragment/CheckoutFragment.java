package lk.kcd.quickbite.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.databinding.FragmentCheckoutBinding;
import lk.kcd.quickbite.helper.NotificationHelper;
import lk.kcd.quickbite.lisner.FirestoreCallback;
import lk.kcd.quickbite.model.CartItem;
import lk.kcd.quickbite.model.Order;
import lk.kcd.quickbite.model.Product;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class CheckoutFragment extends Fragment {

    private static final String TAG = "CheckoutFragment";

    private static final String MERCHANT_ID     = "1223975";
    private static final String MERCHANT_SECRET = "MjA2ODg4NjYzNTc2NzI5NjUwMzExMDgxNzM3MDE0NTM0NTA2NDA=";

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private double total = 0;
    private boolean paymentActive = false;

    private String deliveryAddress = "";
    private double deliveryLat = 0;
    private double deliveryLng = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db           = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);

        binding.shippingLayoutBtn.setOnClickListener(v -> {
            boolean gone = binding.shippingLayoutBody.getVisibility() == View.GONE;
            binding.shippingLayoutBody.setVisibility(gone ? View.VISIBLE : View.GONE);
            binding.shippingLayoutBtn.setRotation(gone ? 180f : 0f);
        });

        binding.billingLayoutBtn.setOnClickListener(v -> {
            boolean gone = binding.bilingLayoutBody.getVisibility() == View.GONE;
            binding.bilingLayoutBody.setVisibility(gone ? View.VISIBLE : View.GONE);
            binding.billingLayoutBtn.setRotation(gone ? 180f : 0f);
        });

        binding.shippingDetailsBilling.setOnCheckedChangeListener((btn, isChecked) -> {
            binding.billingLayout.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            if (!isChecked) binding.bilingLayoutBody.setVisibility(View.VISIBLE);
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });

        final double shippingCost = 400;
        getCartItems(cartItems -> {

            if (!isAdded() || binding == null) return;

            List<String> productIds = new ArrayList<>();
            for (CartItem c : cartItems) productIds.add(c.getProductId());

            getProductsByIds(productIds, data -> {
                if (!isAdded() || binding == null) return;

                double subTotal = 0;
                for (CartItem c : cartItems) {
                    Product p = data.get(c.getProductId());
                    if (p != null) subTotal += p.getPrice() * c.getQuantity();
                }
                total = subTotal + shippingCost;
                binding.checkoutSub.setText(
                        String.format(Locale.US, "LKR %,.2f", subTotal));
                binding.checkoutShippingTotal.setText(
                        String.format(Locale.US, "LKR %,.2f", shippingCost));
                binding.checkOutTotal.setText(
                        String.format(Locale.US, "LKR %,.2f", total));
                paymentActive = true;
            });
        });

        requireActivity().getSupportFragmentManager()
                .setFragmentResultListener("deliveryLocation", getViewLifecycleOwner(),
                        (key, result) -> {
                            if (!isAdded() || binding == null) return;
                            deliveryAddress = result.getString("deliveryAddress", "");
                            deliveryLat     = result.getDouble("deliveryLat", 0);
                            deliveryLng     = result.getDouble("deliveryLng", 0);

                            if (!deliveryAddress.isEmpty()) {
                                binding.shippingDetailsAddress1.setText(deliveryAddress);
                                binding.checkoutSelectedAddress.setText(deliveryAddress);
                                binding.checkoutBtnDirections.setEnabled(true);
                                binding.checkoutBtnDirections.setAlpha(1.0f);
                                Toast.makeText(requireContext(),
                                        "📍 Delivery location set!", Toast.LENGTH_SHORT).show();
                            }
                        });

        binding.checkoutBtnPickLocation.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new DeliveryMapFragment())
                        .addToBackStack(null)
                        .commit());

        binding.checkoutBtnDirections.setOnClickListener(v -> {
            if (deliveryLat == 0 && deliveryLng == 0) {
                Toast.makeText(requireContext(),
                        "Please pick a delivery location first", Toast.LENGTH_SHORT).show();
                return;
            }
            double restaurantLat = 6.9271;
            double restaurantLng = 79.8612;
            String uri = String.format(Locale.US,
                    "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                    restaurantLat, restaurantLng, deliveryLat, deliveryLng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        });

        binding.checkoutBtnProceed.setOnClickListener(v -> {
            String name     = binding.shippingDetailsName.getText().toString().trim();
            String email    = binding.shippingDetailsEmail.getText().toString().trim();
            String phone    = binding.shippingDetailsContact.getText().toString().trim();
            String address1 = binding.shippingDetailsAddress1.getText().toString().trim();
            String city     = binding.shippingDetailsPostCity.getText().toString().trim();

            if (name.isEmpty()) {
                binding.shippingDetailsName.setError("Name is required");
                binding.shippingDetailsName.requestFocus(); return;
            }
            if (email.isEmpty()) {
                binding.shippingDetailsEmail.setError("Email is required");
                binding.shippingDetailsEmail.requestFocus(); return;
            }
            if (phone.isEmpty()) {
                binding.shippingDetailsContact.setError("Contact is required");
                binding.shippingDetailsContact.requestFocus(); return;
            }
            if (address1.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Please enter a delivery address", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!paymentActive) {
                Toast.makeText(requireContext(),
                        "Please wait, loading cart...", Toast.LENGTH_SHORT).show();
                return;
            }
            launchPayHere(name, email, phone, address1, city.isEmpty() ? "Colombo" : city);
        });
    }



    private void launchPayHere(String name, String email,
                               String phone, String address, String city) {
        String orderId = "QB-" + System.currentTimeMillis();

        String firstName = name, lastName = name;
        int spaceIdx = name.indexOf(' ');
        if (spaceIdx > 0) {
            firstName = name.substring(0, spaceIdx);
            lastName  = name.substring(spaceIdx + 1);
        }

        String fmtPhone  = phone.startsWith("+") ? phone : "+94" + phone.replaceFirst("^0", "");
        String amountStr = String.format(Locale.US, "%.2f", total);
        String hash      = generatePayHereHash(MERCHANT_ID, orderId, amountStr, "LKR", MERCHANT_SECRET);

        if (hash == null) {
            Toast.makeText(requireContext(),
                    "Payment setup error. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        InitRequest req = new InitRequest();
        req.setSandBox(true);
        req.setMerchantId(MERCHANT_ID);
        req.setMerchantSecret(MERCHANT_SECRET);
        req.setCurrency("LKR");
        req.setAmount(total);
        req.setOrderId(orderId);
        req.setItemsDescription("QuickBite Food Order");
        req.setNotifyUrl("https://quickbite.requestcatcher.com/notify");

        req.getCustomer().setFirstName(firstName);
        req.getCustomer().setLastName(lastName);
        req.getCustomer().setEmail(email);
        req.getCustomer().setPhone(fmtPhone);
        req.getCustomer().getAddress().setAddress(address);
        req.getCustomer().getAddress().setCity(city);
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        Intent intent = new Intent(getActivity(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        payhereLauncher.launch(intent);
    }

    private String generatePayHereHash(String merchantId, String orderId,
                                       String amount, String currency, String secret) {
        try {
            String secretMd5 = md5(secret).toUpperCase(Locale.US);
            String raw = merchantId + orderId + amount + currency + secretMd5;
            return md5(raw).toUpperCase(Locale.US);
        } catch (Exception e) {
            Log.e(TAG, "Hash error: " + e.getMessage());
            return null;
        }
    }

    private String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        BigInteger no = new BigInteger(1, digest);
        StringBuilder hex = new StringBuilder(no.toString(16));
        while (hex.length() < 32) hex.insert(0, "0");
        return hex.toString();
    }



    private final ActivityResultLauncher<Intent> payhereLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            PHResponse<StatusResponse> response =
                                    (PHResponse<StatusResponse>) result.getData()
                                            .getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                            if (response != null && response.isSuccess()) {
                                Log.i(TAG, "PayHere SUCCESS");
                                saveOrder(response.getData());
                            } else {
                                // ── GUARD ──
                                if (!isAdded()) return;
                                String msg = (response != null && response.getData() != null)
                                        ? response.getData().getMessage() : "Unknown error";
                                Log.e(TAG, "PayHere FAILED: " + msg);
                                Toast.makeText(requireContext(),
                                        "Payment failed: " + msg, Toast.LENGTH_LONG).show();
                            }
                        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                            if (!isAdded()) return;
                            Log.w(TAG, "PayHere CANCELLED by user");
                            Toast.makeText(requireContext(),
                                    "Payment cancelled", Toast.LENGTH_SHORT).show();
                        }
                    });



    private void saveOrder(StatusResponse statusResponse) {
        // ── GUARD: fragment may be detached after PayHere returns ──
        if (!isAdded() || binding == null) return;

        getCartItems(cartItems -> {
            if (!isAdded() || binding == null) return;

            String uid = firebaseAuth.getCurrentUser().getUid();

            Order.Address shippingAddr = Order.Address.builder()
                    .name(binding.shippingDetailsName.getText().toString())
                    .email(binding.shippingDetailsEmail.getText().toString())
                    .contact(binding.shippingDetailsContact.getText().toString())
                    .address1(binding.shippingDetailsAddress1.getText().toString())
                    .address2(binding.shippingDetailsAddress2.getText().toString())
                    .city(binding.shippingDetailsPostCity.getText().toString())
                    .postcode(binding.shippingDetailsPostCode.getText().toString())
                    .build();

            Order order = new Order();
            order.setOrderId("QB-" + System.currentTimeMillis());
            order.setUserId(uid);
            order.setTotalAmount(total);
            order.setStatus("PAID");
            order.setOrderDate(Timestamp.now());
            order.setShippingAddress(shippingAddr);

            if (!binding.shippingDetailsBilling.isChecked()) {
                order.setBillingAddress(Order.Address.builder()
                        .name(binding.billingDetailsName.getText().toString())
                        .email(binding.billingDetailsEmail.getText().toString())
                        .contact(binding.billingDetailsContactNo.getText().toString())
                        .address1(binding.billingDetailsAddress1.getText().toString())
                        .address2(binding.billingDetailsAddress2.getText().toString())
                        .city(binding.billingDetailsCity.getText().toString())
                        .postcode(binding.billingDetailsPostCode.getText().toString())
                        .build());
            }

            List<String> productIds = new ArrayList<>();
            for (CartItem c : cartItems) productIds.add(c.getProductId());

            getProductsByIds(productIds, data -> {
                if (!isAdded() || binding == null) return;

                List<Order.OrderItem> orderItems = new ArrayList<>();
                for (CartItem cartItem : cartItems) {
                    Product product = data.get(cartItem.getProductId());
                    if (product == null) continue;

                    List<Order.OrderItem.Attribute> attrs = new ArrayList<>();
                    if (cartItem.getAttributes() != null) {
                        for (CartItem.Attribute at : cartItem.getAttributes()) {
                            attrs.add(Order.OrderItem.Attribute.builder()
                                    .name(at.getName()).value(at.getValue()).build());
                        }
                    }
                    orderItems.add(Order.OrderItem.builder()
                            .productId(cartItem.getProductId())
                            .unitPrice(product.getPrice())
                            .quantity(cartItem.getQuantity())
                            .attributes(attrs).build());
                }
                order.setOrderItems(orderItems);

                db.collection("orders").document().set(order)
                        .addOnSuccessListener(v -> {
                            // ── GUARD: still attached? ──
                            if (!isAdded() || binding == null) return;

                            String shortId = order.getOrderId().length() > 8
                                    ? order.getOrderId().substring(order.getOrderId().length() - 8)
                                    : order.getOrderId();

                            NotificationHelper.showPaymentSuccess(
                                    requireContext(), shortId, order.getTotalAmount());
                            new android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed(() -> {
                                        if (!isAdded()) return;
                                        NotificationHelper.showOrderInQueue(
                                                requireContext(), shortId);
                                    }, 1500);

                            Toast.makeText(requireContext(),
                                    "Order placed! 🎉", Toast.LENGTH_SHORT).show();

                            // Clear cart
                            db.collection("users").document(uid)
                                    .collection("cart").get()
                                    .addOnSuccessListener(qds -> qds.getDocuments()
                                            .forEach(ds -> ds.getReference().delete()));


                            if (!isAdded()) return;
                            requireActivity().getSupportFragmentManager()
                                    .popBackStack(null,
                                            androidx.fragment.app.FragmentManager
                                                    .POP_BACK_STACK_INCLUSIVE);
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, new HomeFragment())
                                    .commit();
                        })
                        .addOnFailureListener(e -> {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(),
                                    "Save failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
            });
        });
    }



    private void getCartItems(FirestoreCallback<List<CartItem>> callback) {
        String uid = firebaseAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(qds -> callback.onCallback(
                        qds.isEmpty() ? new ArrayList<>() : qds.toObjects(CartItem.class)));
    }

    private void getProductsByIds(List<String> productIds,
                                  FirestoreCallback<Map<String, Product>> callback) {
        Map<String, Product> products = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            callback.onCallback(products);
            return;
        }
        db.collection("products").whereIn("productId", productIds).get()
                .addOnSuccessListener(qds -> {
                    qds.getDocuments().forEach(ds -> {
                        Product p = ds.toObject(Product.class);
                        if (p != null) products.put(p.getProductId(), p);
                    });
                    callback.onCallback(products);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}