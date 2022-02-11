package com.example.tempshapes

import android.graphics.Color
import android.widget.ImageView
import androidx.databinding.BindingAdapter

// Adapter for floating toolbar
@BindingAdapter("activeTool", "currTool")
fun setActive(imageView: ImageView, activeTool: TOOL?, currTool: TOOL) {
    activeTool?.let {
        if (it == currTool) {
            imageView.setBackgroundResource(R.drawable.selected_icon)
            imageView.setColorFilter(Color.BLACK)
        } else {
            imageView.setBackgroundResource(R.drawable.deselected_icon)
            imageView.colorFilter = null
        }
    }
}

// Adapter for color palette
@BindingAdapter("showPalette", "selectedColor")
fun showPalette(imageView: ImageView, showPalette: Boolean?, color: Int) {
    imageView.setColorFilter(color)
    showPalette?.let {
        if (showPalette) {
            imageView.setBackgroundResource(R.drawable.selected_icon)
        } else {
            imageView.setBackgroundResource(R.drawable.deselected_icon)
        }
    }
}