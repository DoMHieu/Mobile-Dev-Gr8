package com.example.music

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Hide Action bar
        supportActionBar?.hide()
        actionBar?.hide()
        //Fullscreen layout
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        val container = findViewById<View>(R.id.container)
        val bottomNavigationView =
            findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val cutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            v.setPadding(0, cutoutInsets.top, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView) { v, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }
        supportFragmentManager.beginTransaction().replace(R.id.container, SearchFragment()).commit()
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, SearchFragment())
                        .commit()
                    true
                }
                R.id.control -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, PlayerFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
