package com.example.tempshapes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.tempshapes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.myCanvasData = binding.myCanvasView
        binding.lifecycleOwner = this
    }
}