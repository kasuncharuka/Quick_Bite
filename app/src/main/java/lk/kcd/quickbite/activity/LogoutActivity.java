package lk.kcd.quickbite.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.kcd.quickbite.databinding.ActivityLogoutBinding;
import lk.kcd.quickbite.model.User;

public class LogoutActivity extends AppCompatActivity {

    private ActivityLogoutBinding binding;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLogoutBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        firebaseAuth  = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();




        binding.signinBtnSignin.setOnClickListener(v -> {
            Intent intent = new Intent(LogoutActivity.this,LoginActivity.class);

            startActivity(intent);
            finish();

        });

binding.signinBtnSignup.setOnClickListener(view ->{

    String name =  binding.signupInputName.getText().toString();

    String email =  binding.signupInputEmail.getText().toString();
    String password =  binding.signupInputPassword.getText().toString();
    String retypepassword =  binding.signupInputRetypePassword.getText().toString();

    if (name.isEmpty()){
        binding.signupInputName.setError("Name is requred");
        binding.signupInputName.requestFocus();
        return;
    }

    if (email.isEmpty()){
        binding.signupInputEmail.setError("email is requred");
        binding.signupInputEmail.requestFocus();
        return;
    }
    if (password.isEmpty()){
        binding.signupInputPassword.setError("Password is required");
        binding.signupInputPassword.requestFocus();
        return;
    }
    if (password.length() < 6){
        binding.signupInputPassword.setError("Password must be at least 6 characters");
        binding.signupInputPassword.requestFocus();
        return;
    }
//    if (retypepassword.equals(password)){
//        binding.signupInputRetypePassword.setError("Retype password ");
//        binding.signupInputRetypePassword.requestFocus();
//        return;
//    }
    firebaseAuth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
               String uid =        task.getResult().getUser().getUid();

               User user = User.builder()
                                .uid(uid)
                                .name(name)
                                .email(email).build();

               firebaseFirestore.collection("users")
                       .document(uid)
                       .set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                           @Override
                           public void onSuccess(Void unused) {
                               Toast.makeText(getApplicationContext(),"Saved success",Toast.LENGTH_SHORT).show();
                               Intent intent= new Intent(LogoutActivity.this,MainActivity.class);
                               startActivity(intent);

                           }
                       }).addOnFailureListener(new OnFailureListener() {
                           @Override
                           public void onFailure(@NonNull Exception e) {

                           }
                       });


//                        updateUI(firebaseAuth.getCurrentUser());
                    }else {

                    }
                }
            });




});


       

    }


}