package com.groodysoft.exoplayerserviceexample.adapter

import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

@RequiresApi(Build.VERSION_CODES.M)
@BindingAdapter("loadImageWithTopText")
fun setImageUrlWithTopText(view : ImageView, url : String?){
    if(!url.isNullOrEmpty()){
        Glide.with(view.context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .centerCrop()
            .into(view)
    }
}