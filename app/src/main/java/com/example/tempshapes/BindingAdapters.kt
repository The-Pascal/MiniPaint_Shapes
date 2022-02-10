package com.example.tempshapes

import android.graphics.Color
import android.widget.ImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("activeTool","currTool")
fun setActive(imageView: ImageView, activeTool: TOOL?, currTool: TOOL) {
    activeTool?.let {
        if(it==currTool) {
            imageView.setBackgroundResource(R.drawable.selected_icon)
            imageView.setColorFilter(Color.BLACK)
        }
        else {
            imageView.setBackgroundResource(R.drawable.deselected_icon)
            imageView.setColorFilter(null)
        }
    }
}