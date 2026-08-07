package lk.kcd.quickbite.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.activity.LoginActivity;
import lk.kcd.quickbite.databinding.FragmentProfileBinding;
import lk.kcd.quickbite.model.User;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri pendingImageUri = null;
    private ImageView sheetAvatarPreview = null;

    // Track notification state locally since the switch may not exist in XML
    private boolean notificationsEnabled = true;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            pendingImageUri = result.getData().getData();
                            if (sheetAvatarPreview != null && pendingImageUri != null) {
                                Glide.with(this)
                                        .load(pendingImageUri)
                                        .circleCrop()
                                        .into(sheetAvatarPreview);
                            }
                        }
                    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        db           = FirebaseFirestore.getInstance();
        storage      = FirebaseStorage.getInstance();

        // ── Back navigation ───────────────────────────────────────────────
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() { navigateBack(); }
                });

        binding.profileBtnBack.setOnClickListener(v -> navigateBack());
        binding.profileBtnEdit.setOnClickListener(v -> showEditBottomSheet());
        binding.profileAvatarEdit.setOnClickListener(v -> showEditBottomSheet());

        loadUserData();
        loadOrderCount();
        setupMenuClicks();

        binding.profileBtnSignout.setOnClickListener(v -> {
            firebaseAuth.signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ── Edit bottom sheet ─────────────────────────────────────────────────
    private void showEditBottomSheet() {
        pendingImageUri = null;

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_edit_profile, null);
        sheet.setContentView(sheetView);

        sheetAvatarPreview = sheetView.findViewById(R.id.sheet_avatar_preview);
        com.google.android.material.button.MaterialButton btnChangePic =
                sheetView.findViewById(R.id.sheet_btn_change_pic);
        EditText    inputName = sheetView.findViewById(R.id.sheet_input_name);
        Button      btnSave   = sheetView.findViewById(R.id.sheet_btn_save);
        Button      btnCancel = sheetView.findViewById(R.id.sheet_btn_cancel);
        ProgressBar progress  = sheetView.findViewById(R.id.sheet_progress);

        inputName.setText(binding.profileName.getText());

        String currentPicUrl = (String) binding.profileAvatar.getTag();
        if (currentPicUrl != null && !currentPicUrl.isEmpty()) {
            Glide.with(this).load(currentPicUrl).circleCrop().into(sheetAvatarPreview);
        }

        View.OnClickListener openPicker = v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        };
        sheetAvatarPreview.setOnClickListener(openPicker);
        btnChangePic.setOnClickListener(openPicker);
        btnCancel.setOnClickListener(v -> sheet.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = inputName.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                inputName.setError("Name cannot be empty");
                inputName.requestFocus();
                return;
            }
            btnSave.setEnabled(false);
            progress.setVisibility(View.VISIBLE);

            if (pendingImageUri != null) {
                uploadImageThenSave(newName, pendingImageUri, sheet, progress, btnSave);
            } else {
                saveNameToFirestore(newName, null, sheet, progress, btnSave);
            }
        });

        sheet.setOnDismissListener(d -> sheetAvatarPreview = null);
        sheet.show();
    }

    // ── Firebase helpers ──────────────────────────────────────────────────
    private void uploadImageThenSave(String name, Uri imageUri,
                                     BottomSheetDialog sheet,
                                     ProgressBar progress, Button btnSave) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        StorageReference ref = storage.getReference()
                .child("profile_pics/" + user.getUid() + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(snap ->
                        ref.getDownloadUrl().addOnSuccessListener(uri ->
                                saveNameToFirestore(name, uri.toString(), sheet, progress, btnSave)))
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progress.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Image upload failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNameToFirestore(String name, @Nullable String picUrl,
                                     BottomSheetDialog sheet,
                                     ProgressBar progress, Button btnSave) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (picUrl != null) updates.put("profilePicUrl", picUrl);

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (!isAdded() || binding == null) return;
                    binding.profileName.setText(name);
                    if (picUrl != null) {
                        binding.profileAvatar.setTag(picUrl);
                        Glide.with(this).load(picUrl).circleCrop().into(binding.profileAvatar);
                    }
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    sheet.dismiss();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progress.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(ds -> {
                    // ── FIX: guard against binding being null after fragment destroyed ──
                    if (!isAdded() || binding == null || !ds.exists()) return;
                    User user = ds.toObject(User.class);
                    if (user == null) return;

                    binding.profileName.setText(user.getName());
                    binding.profileEmail.setText(user.getEmail());

                    String pic = user.getProfilePicUrl();
                    binding.profileAvatar.setTag(pic != null ? pic : "");
                    if (pic != null && !pic.isEmpty()) {
                        Glide.with(this).load(pic).circleCrop().into(binding.profileAvatar);
                    }
                });
    }

    private void loadOrderCount() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("orders")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(qds -> {
                    // ── FIX: guard against null binding when callback fires after navigation ──
                    if (!isAdded() || binding == null) return;
                    binding.profileStatOrders.setText(String.valueOf(qds.size()));
                });
    }

    private void setupMenuClicks() {
        binding.profileMenuOrders.setOnClickListener(v -> loadFragment(new OrdersFragment()));
        binding.profileMenuSettings.setOnClickListener(v -> loadFragment(new SettingsFragment()));
        binding.profileMenuFavourites.setOnClickListener(v ->
                Toast.makeText(getContext(), "Coming soon", Toast.LENGTH_SHORT).show());
        binding.profileMenuHelp.setOnClickListener(v -> loadFragment(new MessageFragment()));
        binding.profileMenuAddresses.setOnClickListener(v ->
                Toast.makeText(getContext(), "Coming soon", Toast.LENGTH_SHORT).show());
        binding.profileMenuPayment.setOnClickListener(v ->
                Toast.makeText(getContext(), "Coming soon", Toast.LENGTH_SHORT).show());
        binding.profileMenuPrivacy.setOnClickListener(v ->
                Toast.makeText(getContext(), "Coming soon", Toast.LENGTH_SHORT).show());

        // ── FIX: profile_switch_notifications does not exist in fragment_profile.xml.
        // Tapping the row toggles local state and shows a toast instead of crashing. ──
        binding.profileMenuNotifications.setOnClickListener(v -> {
            notificationsEnabled = !notificationsEnabled;
            Toast.makeText(getContext(),
                    "Notifications " + (notificationsEnabled ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void navigateBack() {
        if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            requireActivity().getSupportFragmentManager().popBackStack();
        } else {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private void loadFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}