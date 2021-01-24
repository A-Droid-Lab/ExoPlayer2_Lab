package com.groodysoft.exoplayerserviceexample.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.groodysoft.exoplayerserviceexample.R
import com.groodysoft.exoplayerserviceexample.databinding.ItemImageBinding

class PlayerImageSlideAdapter : RecyclerView.Adapter<PlayerImageSlideAdapter.SliderViewHolder>() {

    private var ImageData : ArrayList<String> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerImageSlideAdapter.SliderViewHolder {
        val binding: ItemImageBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_image, parent, false)
        return SliderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerImageSlideAdapter.SliderViewHolder, position: Int) {
        holder.bind(ImageData, position)
    }

    override fun getItemCount(): Int = ImageData.size

    fun setPlayerViewPagerData(imageData:ArrayList<String>){
        ImageData.addAll(imageData)
        notifyDataSetChanged()
    }

    fun refresh(){
        notifyDataSetChanged()
    }

    inner class SliderViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageData: List<String>, position: Int) {
            binding.apply {
                this.imageData = imageData
                this.pos = position
            }
        }

    }
}