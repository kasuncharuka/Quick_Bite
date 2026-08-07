package lk.kcd.quickbite.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.databinding.ActivityMainBinding;
import lk.kcd.quickbite.databinding.SideNavHeaderBinding;
import lk.kcd.quickbite.fragment.CartFragment;
import lk.kcd.quickbite.fragment.CategoryFragment;
import lk.kcd.quickbite.fragment.HomeFragment;
import lk.kcd.quickbite.fragment.MessageFragment;
import lk.kcd.quickbite.fragment.OrdersFragment;
import lk.kcd.quickbite.fragment.ProfileFragment;
import lk.kcd.quickbite.fragment.SettingsFragment;
import lk.kcd.quickbite.helper.NotificationHelper;
import lk.kcd.quickbite.helper.ShakeSensorManager;
import lk.kcd.quickbite.model.User;
import lk.kcd.quickbite.service.MenuSyncService;          // ← NEW (Multitasking)

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnItemSelectedListener {

    private static final String TAG  = "MainActivity";
    private ActivityMainBinding binding;
    private SideNavHeaderBinding sideNavHeaderBinding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    // Sensor
    private ShakeSensorManager shakeSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NotificationHelper.createChannels(this);

        // ── MULTITASKING:
        startMenuSyncService();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
            }
        }

        View headerView = binding.sideNavigationView.getHeaderView(0);
        sideNavHeaderBinding = SideNavHeaderBinding.bind(headerView);

        drawerLayout         = binding.drawerLayout;
        toolbar              = binding.toolbar;
        navigationView       = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigationView;
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
        }


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }
                Fragment current = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                if (!(current instanceof HomeFragment)) {
                    loadFragment(new HomeFragment());
                    navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
                    bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
                    return;
                }
                finish();
            }
        });

        firebaseAuth      = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseFirestore.collection("users")
                    .document(currentUser.getUid()).get()
                    .addOnSuccessListener(ds -> {
                        if (ds.exists()) {
                            User user = ds.toObject(User.class);
                            if (user != null) {
                                sideNavHeaderBinding.headerUserName.setText(user.getName());
                                sideNavHeaderBinding.headerUserEmail.setText(user.getEmail());
                                if (user.getProfilePicUrl() != null) {
                                    Glide.with(MainActivity.this)
                                            .load(user.getProfilePicUrl())
                                            .circleCrop()
                                            .into(sideNavHeaderBinding.headerProfilePic);
                                }
                                showWelcomeNotification(user.getName());
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "User load failed: " + e.getMessage()));

            navigationView.getMenu().findItem(R.id.side_nav_profile).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_orders).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_cart).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_message).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_logout).setVisible(true);
        }

        // Shake sensor
        shakeSensorManager = new ShakeSensorManager(this);
        shakeSensorManager.setOnShakeListener(count -> {
            Snackbar.make(binding.getRoot(),
                            "🍔 Shake detected! Showing you our menu…", Snackbar.LENGTH_SHORT)
                    .setAction("Browse", v -> {
                        loadFragment(new CategoryFragment());
                        navigationView.getMenu()
                                .findItem(R.id.side_nav_home).setChecked(false);
                        bottomNavigationView.getMenu()
                                .findItem(R.id.bottom_nav_category).setChecked(true);
                    })
                    .show();
        });


        MenuSyncService.setSyncListener(count ->
                Log.d(TAG, "MenuSyncService → synced " + count + " products in background"));
    }


    private void startMenuSyncService() {
        Intent svcIntent = new Intent(this, MenuSyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svcIntent);
        } else {
            startService(svcIntent);
        }
        Log.d(TAG, "MenuSyncService started");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopService(new Intent(this, MenuSyncService.class));
        MenuSyncService.removeSyncListener();
    }


    private void showWelcomeNotification(String userName) {
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                NotificationHelper.showWelcome(this, userName), 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        shakeSensorManager.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shakeSensorManager.stop();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        Menu navMenu    = navigationView.getMenu();
        Menu bottomMenu = bottomNavigationView.getMenu();
        for (int i = 0; i < navMenu.size(); i++) navMenu.getItem(i).setChecked(false);
        for (int i = 0; i < bottomMenu.size(); i++) bottomMenu.getItem(i).setChecked(false);

        if (itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home) {
            loadFragment(new HomeFragment());
            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);

        } else if (itemId == R.id.side_nav_profile || itemId == R.id.bottom_nav_profile) {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            }
            loadFragment(new ProfileFragment());
            navigationView.getMenu().findItem(R.id.side_nav_profile).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_profile).setChecked(true);

        } else if (itemId == R.id.side_nav_orders) {
            loadFragment(new OrdersFragment());
            navigationView.getMenu().findItem(R.id.side_nav_orders).setChecked(true);

        } else if (itemId == R.id.side_nav_cart || itemId == R.id.bottom_nav_cart) {
            loadFragment(new CartFragment());
            navigationView.getMenu().findItem(R.id.side_nav_cart).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart).setChecked(true);

        } else if (itemId == R.id.side_nav_message) {
            loadFragment(new MessageFragment());

        } else if (itemId == R.id.side_nav_settings) {
            loadFragment(new SettingsFragment());

        } else if (itemId == R.id.bottom_nav_category) {
            loadFragment(new CategoryFragment());
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_category).setChecked(true);

        } else if (itemId == R.id.side_nav_login) {
            startActivity(new Intent(this, LoginActivity.class));

        } else if (itemId == R.id.side_nav_logout) {
            firebaseAuth.signOut();
            loadFragment(new HomeFragment());
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.side_nav_menu);
            navigationView.removeHeaderView(sideNavHeaderBinding.getRoot());
            navigationView.inflateHeaderView(R.layout.side_nav_header);
        }

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    public void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.replace(R.id.fragment_container, fragment);
        tx.commit();
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Uri uri = result.getData().getData();
                    Log.i(TAG, "Image URI: " + uri.getPath());
                }
            });
}