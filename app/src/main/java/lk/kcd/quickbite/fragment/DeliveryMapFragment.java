package lk.kcd.quickbite.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.databinding.FragmentDeliveryMapBinding;

public class DeliveryMapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentDeliveryMapBinding binding;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;


    private final Handler geocodeHandler = new Handler(Looper.getMainLooper());
    private Runnable geocodeRunnable;


    private LatLng selectedLatLng;
    private String selectedAddress = "";


    private static final LatLng RESTAURANT_LATLNG = new LatLng(6.9271, 79.8612);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDeliveryMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());


        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.delivery_map);
        if (mapFragment != null) mapFragment.getMapAsync(this);


        binding.mapBtnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());


        binding.mapBtnConfirm.setOnClickListener(v -> {
            if (selectedLatLng == null) {
                binding.mapAddressText.setText("Please move the map to select a location");
                return;
            }


            Bundle result = new Bundle();
            result.putString("deliveryAddress", selectedAddress);
            result.putDouble("deliveryLat", selectedLatLng.latitude);
            result.putDouble("deliveryLng", selectedLatLng.longitude);

            requireActivity().getSupportFragmentManager()
                    .setFragmentResult("deliveryLocation", result);

            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);


        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 15f));
                } else {

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(RESTAURANT_LATLNG, 13f));
                }
            });
        } else {

            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(RESTAURANT_LATLNG, 13f));
        }

        // Geocode as camera moves — debounced 600ms
        googleMap.setOnCameraIdleListener(() -> {
            LatLng center = googleMap.getCameraPosition().target;
            selectedLatLng = center;

            if (geocodeRunnable != null) geocodeHandler.removeCallbacks(geocodeRunnable);
            geocodeRunnable = () -> reverseGeocode(center);
            geocodeHandler.postDelayed(geocodeRunnable, 600);
        });

        googleMap.setOnCameraMoveListener(() ->
                binding.mapAddressText.setText("Locating…"));
    }

    private void reverseGeocode(LatLng latLng) {
        if (!isAdded()) return;
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                    sb.append(addr.getAddressLine(i));
                    if (i < addr.getMaxAddressLineIndex()) sb.append(", ");
                }
                selectedAddress = sb.toString();
                binding.mapAddressText.setText(selectedAddress);


                float[] results = new float[1];
                android.location.Location.distanceBetween(
                        RESTAURANT_LATLNG.latitude, RESTAURANT_LATLNG.longitude,
                        latLng.latitude, latLng.longitude, results);
                float km = results[0] / 1000f;
                binding.mapDistanceText.setText(
                        String.format(Locale.US, "%.1f km from restaurant", km));
            } else {
                selectedAddress = String.format(Locale.US,
                        "%.5f, %.5f", latLng.latitude, latLng.longitude);
                binding.mapAddressText.setText(selectedAddress);
                binding.mapDistanceText.setText("");
            }
        } catch (IOException e) {
            selectedAddress = String.format(Locale.US,
                    "%.5f, %.5f", latLng.latitude, latLng.longitude);
            binding.mapAddressText.setText(selectedAddress);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1001 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 15f));
                    }
                });
            }
        }
    }
}
