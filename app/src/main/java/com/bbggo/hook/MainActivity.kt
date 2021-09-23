package com.bbggo.hook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.btn)
        btn.text = "Hello Main"
        btn.setOnClickListener {
            /**
             * TargetActivity未在AndroidManifest.xml文件中定义
             */
            startActivity((Intent(this, TargetActivity::class.java)))
        }
    }
}