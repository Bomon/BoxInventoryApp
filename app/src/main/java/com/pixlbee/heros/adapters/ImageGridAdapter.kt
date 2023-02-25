package com.pixlbee.heros.adapters

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.pixlbee.heros.R
import com.pixlbee.heros.models.GridElementType
import com.pixlbee.heros.models.ImageGridElementModel
import java.util.*


class ImageGridAdapter(gridImages: ArrayList<ImageGridElementModel>) : RecyclerView.Adapter<ImageGridAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var mImageList: ArrayList<ImageGridElementModel> = ArrayList()
    private var toastMessage: Toast? = null

    private lateinit var mListener: OnImageGridClickListener
    private lateinit var holder: ItemViewHolder


    init {
        mImageList = gridImages
        if (!mImageList.any { im -> im.grid_element_type != GridElementType.IMAGE })
            mImageList.add(ImageGridElementModel("", GridElementType.ADD_BTN))
        setHasStableIds(true)
    }


    fun getImages(): ArrayList<ImageGridElementModel> {
        return mImageList
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.grid_image, parent, false)
        return ItemViewHolder(view)
    }


    override fun getItemCount(): Int {
        return mImageList.size
    }


    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        this.holder = holder

        if (mImageList[position].grid_element_type == GridElementType.IMAGE) {
            // Load image
            if (mImageList[position].image != ""){
                val imageByteArray = Base64.decode(mImageList[position].image, Base64.DEFAULT)
                Glide.with(context)
                    .load(imageByteArray)
                    .into(holder.imageView)
            }
            // Prepare delete btn listener
            holder.deleteBtn.visibility = View.VISIBLE
            holder.deleteBtn.setOnClickListener {
                deleteImage(position)
            }
            holder.imageView.setOnClickListener(null)
        } else {
            holder.deleteBtn.visibility = View.GONE
            holder.imageView.setOnClickListener {
                if (mImageList.size >= 11) {
                    toastMessage?.cancel()
                    toastMessage = Toast.makeText(context, context.resources.getString(R.string.error_too_many_images), Toast.LENGTH_LONG)
                    toastMessage?.show()
                } else {
                    mListener.onImageAdd(it)
                }
            }

            Glide.with(context)
                .load(R.drawable.ic_outline_add_photo_alternate_24_padding)
                .into(holder.imageView)
        }
    }


    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var deleteBtn: MaterialButton = itemView.findViewById(R.id.delete_btn)
        var imageView: ImageView = itemView.findViewById(R.id.image)

    }


    fun setFilter(imageList: List<ImageGridElementModel>) {
        mImageList.clear()
        mImageList.addAll(imageList)
        if (mImageList.size < 10)
            mImageList.add(ImageGridElementModel("", GridElementType.ADD_BTN))
        this.notifyDataSetChanged()
    }


    interface OnImageGridClickListener{
        fun onImageAdd(view: View)
    }


    fun setOnImageGridClickListener(mListener: OnImageGridClickListener) {
        this.mListener = mListener
    }

    fun onRowMoved(fromPosition: Int, toPosition: Int) {
        Collections.swap(mImageList, fromPosition, toPosition)
        notifyItemChanged(fromPosition)
        notifyItemMoved(fromPosition, toPosition)
        notifyItemRangeChanged(fromPosition,toPosition)
    }

    fun addImage(newImage: ImageGridElementModel) {
        mImageList.add(mImageList.size-1, newImage)
        this.notifyItemInserted(mImageList.size - 2)
    }

    private fun deleteImage(position: Int) {
        mImageList.removeAt(position)
        this.notifyItemRemoved(position)
    }


}