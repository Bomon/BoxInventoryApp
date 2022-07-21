package com.pixlbee.heros.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.pixlbee.heros.R
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.utility.Utils
import dev.sasikanth.colorsheet.ColorSheet
import it.sephiroth.android.library.numberpicker.NumberPicker
import it.sephiroth.android.library.numberpicker.NumberPicker.OnNumberPickerChangeListener

class BoxItemEditAdapter(mDataList: ArrayList<BoxItemModel>) : RecyclerView.Adapter<BoxItemEditAdapter.BoxItemViewHolder>() {

    lateinit var mContext: Context
    private var mItemModel: ArrayList<BoxItemModel> = ArrayList()


    fun getCurrentStatus(): ArrayList<BoxItemModel>{
        return mItemModel
    }


    init {
        setFilter(mDataList)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxItemViewHolder {
        mContext = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return BoxItemViewHolder(layoutInflater.inflate(R.layout.card_item_in_box_edit, parent, false))

    }


    override fun getItemCount(): Int {
        return mItemModel.size
    }


    override fun onBindViewHolder(holder: BoxItemViewHolder, position: Int) {
        holder.bind(mItemModel[position])
    }


    fun setColorButtonColor(color: Int, button: MaterialButton){

        button.background.setTint(color)
        val red = (color shr 16 and 0xFF).toFloat()
        val green = (color shr 8 and 0xFF).toFloat()
        val blue = (color and 0xFF).toFloat()
        val alpha = (color shr 24 and 0xFF).toFloat()
        if ((red * 0.299 + green * 0.587 + blue * 0.114) > 186) {
            button.setTextColor(Color.BLACK)
            button.setIconTintResource(R.color.md_black_1000)
        } else {
            button.setTextColor(Color.WHITE)
            button.setIconTintResource(R.color.md_white_1000)
        }
    }


    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        private var itemEditAmount: NumberPicker
        private var itemEditInvnum: ChipGroup
        //var item_edit_status: EditText
        private var itemName: TextView
        private var itemColorButton: MaterialButton
        private var itemDeleteButton: MaterialButton
        private var itemInvnumButton: MaterialButton


        init {
            itemEditAmount = itemView.findViewById(R.id.box_item_edit_amount)
            itemEditInvnum = itemView.findViewById(R.id.box_item_invnums)
            //item_edit_status = itemView.findViewById<EditText>(R.id.box_item_edit_status)
            itemName = itemView.findViewById<EditText>(R.id.box_item_name)
            itemDeleteButton = itemView.findViewById(R.id.box_item_delete_btn)
            itemColorButton = itemView.findViewById(R.id.box_item_color_btn)
            itemInvnumButton = itemView.findViewById(R.id.box_item_invnum_btn)
            itemDeleteButton.setOnClickListener {
                mItemModel.removeAt(adapterPosition)
                notifyItemRemoved(adapterPosition)
                notifyItemRangeChanged(adapterPosition, mItemModel.size)
            }

            for( v in itemEditAmount.children) {
                if(v is EditText) {
                    v.setOnEditorActionListener { _, _, _ ->
                        v.clearFocus()
                        val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                        true
                    }
                }
            }
            val fragmentManager = (mContext as FragmentActivity).supportFragmentManager
            val colors = mContext.resources.getIntArray(R.array.picker_colors)
            itemColorButton.setOnClickListener {
                ColorSheet().colorPicker(
                    colors = colors,
                    listener = { color ->
                        setColorButtonColor(color, itemColorButton)
                        mItemModel[adapterPosition].item_color = color
                    })
                    .show(fragmentManager)
            }

            itemInvnumButton.setOnClickListener {
                val builder = MaterialAlertDialogBuilder(mContext)
                builder.setTitle(mContext.resources.getString(R.string.dialog_add_invnr_title))

                val viewInflated: View = LayoutInflater.from(mContext)
                    .inflate(R.layout.dialog_add_invnum, itemView as ViewGroup?, false)
                val input = viewInflated.findViewById<View>(R.id.dialog_input_invnum) as EditText
                val container = viewInflated.findViewById<View>(R.id.dialog_input_invnum_container) as TextInputLayout

                builder.setView(viewInflated)
                builder.setPositiveButton(mContext.resources.getString(R.string.dialog_add), null)
                builder.setNegativeButton(mContext.resources.getString(R.string.dialog_cancel)) { dialog, _ -> dialog.cancel() }

                val mAlertDialog: AlertDialog = builder.create()
                mAlertDialog.setOnShowListener {
                    val b: Button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    b.setOnClickListener {
                        val inputText = input.text.toString()
                        if (inputText != "" && ";" !in inputText) {
                            val chip = Chip(mContext)
                            chip.text = input.text.toString()
                            chip.isCloseIconVisible = true
                            chip.setOnCloseIconClickListener {
                                itemEditInvnum.removeView(chip as View)
                                mItemModel[adapterPosition].item_invnum =
                                    Utils.chipListToString(itemEditInvnum)
                            }
                            itemEditInvnum.addView(chip)
                            mItemModel[adapterPosition].item_invnum =
                                Utils.chipListToString(itemEditInvnum)
                            mAlertDialog.dismiss()
                        } else {
                            if (inputText == "") {
                                container.isErrorEnabled = true
                                container.error =
                                    mContext.resources.getString(R.string.error_dialog_invnr_empty)
                            } else {
                                container.isErrorEnabled = true
                                container.error =
                                    mContext.resources.getString(R.string.error_dialog_invnr_invalid)
                            }
                        }
                    }
                }
                mAlertDialog.show()

            }

            itemEditAmount.numberPickerChangeListener = object : OnNumberPickerChangeListener{
                override fun onProgressChanged(numberPicker: NumberPicker, progress: Int, fromUser: Boolean) {
                    mItemModel[adapterPosition].item_amount = itemEditAmount.progress.toString()
                }

                override fun onStartTrackingTouch(numberPicker: NumberPicker) {
                }

                override fun onStopTrackingTouch(numberPicker: NumberPicker) {
                }
            }

        }

        fun bind(model: BoxItemModel) {
            itemName.text = model.item_name
            //item_edit_amount.setText(model.item_amount)
            itemEditAmount.progress = model.item_amount.toInt()
            setColorButtonColor(model.item_color, itemColorButton)

            itemEditInvnum.removeAllViews()
            for (invnum in model.item_invnum.split(";")){
                if (invnum != ""){
                    val chip = Chip(mContext)
                    chip.text = invnum
                    chip.isCloseIconVisible = true
                    itemEditInvnum.addView(chip)
                    chip.setOnCloseIconClickListener {
                        itemEditInvnum.removeView(chip as View)
                        mItemModel[adapterPosition].item_invnum = Utils.chipListToString(itemEditInvnum)
                    }
                }
            }
        }
    }


    fun moveItem(from: Int, to: Int) {
        val fromLocation = mItemModel[from]
        mItemModel.removeAt(from)
        if (to < from) {
            mItemModel.add(to , fromLocation)
        } else {
            mItemModel.add(to, fromLocation)
        }
    }


    fun addToItemList(newList: ArrayList<BoxItemModel>) {
        this.mItemModel = newList
        notifyItemInserted(mItemModel.size-1)
        notifyItemRangeChanged(0, mItemModel.size)
    }


    fun setFilter(itemList: List<BoxItemModel>) {
        mItemModel.clear()
        mItemModel.addAll(itemList)
        this.notifyDataSetChanged()
    }


}