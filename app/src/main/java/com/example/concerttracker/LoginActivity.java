package com.example.concerttracker;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText username,password;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); Screen.layout(this,R.layout.activity_login);
        username=findViewById(R.id.etUsername); password=findViewById(R.id.etPassword);
        findViewById(R.id.btnLogin).setOnClickListener(v->authenticate(false));
        findViewById(R.id.btnRegister).setOnClickListener(v->authenticate(true));
    }
    private void authenticate(boolean create) {
        String name=username.getText().toString().trim(), pass=password.getText().toString();
        username.setError(null); password.setError(null);
        if(name.isEmpty() || name.length()>64) { username.setError("Enter a username (1–64 characters)"); username.requestFocus(); Screen.message(this,"Check your username"); return; }
        if(pass.trim().isEmpty() || pass.length()>128 || (create && pass.length()<6)) { password.setError(create?"Use 6–128 characters":"Enter your password"); password.requestFocus(); Screen.message(this,"Check your password"); return; }
        try {
            DatabaseHelper db=DatabaseHelper.get(this);
            long id=create?db.register(name,pass):db.login(name,pass);
            if(id<0) { Screen.message(this,create?"Username already exists. Log in instead.":"Incorrect username or password. Create an account if you are new."); return; }
            Screen.message(this,create?"Account created!":"Logged in!");
            startActivity(Screen.intent(this,HomeActivity.class,id,-1)); finish();
        } catch (RuntimeException e) { android.util.Log.e("ConcertLogify","Login failed",e); Screen.message(this,"Could not access accounts. Please try again."); }
    }
}
