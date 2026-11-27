package com.example.concerttracker;

import android.content.Intent;
import android.content.Context;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class Screen {
    static final String USER="user_id", CONCERT="concert_id";
    static void layout(AppCompatActivity a,int layout) {
        EdgeToEdge.enable(a); a.setContentView(layout);
        ViewCompat.setOnApplyWindowInsetsListener(a.findViewById(R.id.main),(v,insets)-> {
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
    }
    static long user(AppCompatActivity a) { return a.getIntent().getLongExtra(USER,-1); }
    static long concert(AppCompatActivity a) { return a.getIntent().getLongExtra(CONCERT,-1); }
    static Intent intent(Context c,Class<?> cls,long user,long concert) { return new Intent(c,cls).putExtra(USER,user).putExtra(CONCERT,concert); }
    static void message(Context c,String text) { Toast.makeText(c,text,Toast.LENGTH_SHORT).show(); }
    static boolean requireUser(AppCompatActivity a,DatabaseHelper db,long user) {
        if(db.userExists(user)) return true;
        message(a,"Please log in first"); logout(a); return false;
    }
    static void logout(AppCompatActivity a) { a.startActivity(new Intent(a,LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); a.finish(); }
    static int dp(Context c,int n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
}
