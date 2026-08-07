package lk.kcd.quickbite.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.databinding.FragmentMessageBinding;

public class MessageFragment extends Fragment {

    private static final String SUPPORT_PHONE   = "+94713278433";
    private static final String SUPPORT_EMAIL   = "support@quickbite.lk";

    private static final String WHATSAPP_MSG    =
            "Hello QuickBite Support, I need help with my order.";

    private FragmentMessageBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        binding.messageBtnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });


        binding.msgBtnWhatsapp.setOnClickListener(v -> openWhatsApp());


        binding.msgBtnCall.setOnClickListener(v -> makePhoneCall());


        binding.msgBtnEmail.setOnClickListener(v -> sendEmail());
    }


    private void openWhatsApp() {
        try {
            String url = "https://wa.me/" + SUPPORT_PHONE.replace("+", "")
                    + "?text=" + Uri.encode(WHATSAPP_MSG);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
        }
    }


    private void makePhoneCall() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:" + SUPPORT_PHONE));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Could not open dialler", Toast.LENGTH_SHORT).show();
        }
    }


    private void sendEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse("mailto:" + SUPPORT_EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, "QuickBite Support Request");
            intent.putExtra(Intent.EXTRA_TEXT,
                    "Hello QuickBite Support,\n\nI need help with:\n\n");
            startActivity(Intent.createChooser(intent, "Send email"));
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}