package lk.kcd.quickbite.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }


        startAnimations();
    }

    private void startAnimations() {


        binding.splashContent.animate()
                .alpha(1f)
                .setDuration(200)
                .start();


        Animation logoBounce = AnimationUtils.loadAnimation(this, R.anim.splash_logo_bounce);
        binding.splashLogo.startAnimation(logoBounce);


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.splash_pulse);
            binding.splashGlowRing.startAnimation(pulse);
        }, 700);


        Animation fadeUp = AnimationUtils.loadAnimation(this, R.anim.splash_fade_up);
        binding.splashAppName.startAnimation(fadeUp);


        new Handler(Looper.getMainLooper()).postDelayed(() ->
                binding.splashBottom.animate()
                        .alpha(1f)
                        .setDuration(500)
                        .start(), 800);


        new Handler(Looper.getMainLooper()).postDelayed(() ->
                binding.splashProgressBar.setVisibility(View.VISIBLE), 1200);


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            binding.splashProgressBar.setVisibility(View.INVISIBLE);
            binding.splashRoot.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(() -> {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                        overridePendingTransition(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out);
                    })
                    .start();
        }, 3500);
    }
}