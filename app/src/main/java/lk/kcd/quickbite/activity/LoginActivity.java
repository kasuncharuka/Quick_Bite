package lk.kcd.quickbite.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.kcd.quickbite.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();


        binding.signinBtnSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, LogoutActivity.class));
            finish();
        });

        // Sign In
        binding.signinBtnSignin.setOnClickListener(v -> {
            String email    = binding.signinInputEmail.getText().toString().trim();
            String password = binding.signinInputPassword.getText().toString();

            if (email.isEmpty()) {
                binding.signinInputEmail.setError("Email is required");
                binding.signinInputEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                binding.signinInputPassword.setError("Password is required");
                binding.signinInputPassword.requestFocus();
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            updateUI(firebaseAuth.getCurrentUser());
                        } else {
                            String msg = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Sign in failed. Check your credentials.";
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Forgot Password
        binding.signinForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void showForgotPasswordDialog() {
        EditText emailInput = new EditText(this);
        emailInput.setHint("your@email.com");
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setPadding(56, 40, 56, 16);


        String typed = binding.signinInputEmail.getText().toString().trim();
        if (!typed.isEmpty()) emailInput.setText(typed);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email and we'll send you a reset link.")
                .setView(emailInput)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    firebaseAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused ->
                                    new AlertDialog.Builder(this)
                                            .setTitle("Check your inbox ✅")
                                            .setMessage("Reset link sent to:\n" + email
                                                    + "\n\nAlso check your spam folder.")
                                            .setPositiveButton("OK", null)
                                            .show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUI(FirebaseUser user) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}