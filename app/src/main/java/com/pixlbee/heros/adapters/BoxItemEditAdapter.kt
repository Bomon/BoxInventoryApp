package com.pixlbee.heros.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.pixlbee.heros.R
import com.pixlbee.heros.utility.Utils
import com.pixlbee.heros.models.BoxItemModel
import dev.sasikanth.colorsheet.ColorSheet
import it.sephiroth.android.library.numberpicker.NumberPicker
import it.sephiroth.android.library.numberpicker.NumberPicker.OnNumberPickerChangeListener

class BoxItemEditAdapter(private val mDataList: ArrayList<BoxItemModel>) : RecyclerView.Adapter<BoxItemEditAdapter.BoxItemViewHolder>() {

    lateinit var context: Context
    private var mItemModel: ArrayList<BoxItemModel> = ArrayList()


    fun getCurrentStatus(): ArrayList<BoxItemModel>{
        return mItemModel
    }


    init {
        setFilter(mDataList)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxItemViewHolder {
        context = parent.context
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
            button.setIconTintResource(R.color.black)
        } else {
            button.setTextColor(Color.WHITE)
            button.setIconTintResource(R.color.white)
        }
    }


    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        var item_edit_amount: NumberPicker
        var item_edit_invnum: ChipGroup
        //var item_edit_status: EditText
        var item_name: TextView
        var item_color_button: MaterialButton
        var item_delete_button: MaterialButton
        var item_invnum_button: MaterialButton


        init {
            item_edit_amount = itemView.findViewById<NumberPicker>(R.id.box_item_edit_amount)
            item_edit_invnum = itemView.findViewById<ChipGroup>(R.id.box_item_invnums)
            //item_edit_status = itemView.findViewById<EditText>(R.id.box_item_edit_status)
            item_name = itemView.findViewById<EditText>(R.id.box_item_name)
            item_delete_button = itemView.findViewById<MaterialButton>(R.id.box_item_delete_btn)
            item_color_button = itemView.findViewById<MaterialButton>(R.id.box_item_color_btn)
            item_invnum_button = itemView.findViewById<MaterialButton>(R.id.box_item_invnum_btn)
            item_delete_button.setOnClickListener {

                mItemModel.removeAt(adapterPosition)
                notifyItemRemoved(adapterPosition)
                notifyItemRangeChanged(adapterPosition, mItemModel.size)
            }

            for( v in item_edit_amount.children) {
                if(v is EditText) {
                    v.setOnEditorActionListener { _, _, _ ->
                        v.clearFocus()
                        val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                        true
                    }
                }
            }
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            val colors = context.resources.getIntArray(R.array.picker_colors)
            item_color_button.setOnClickListener {
                ColorSheet().colorPicker(
                    colors = colors,
                    listener = { color ->
                        setColorButtonColor(color, item_color_button)
                        mItemModel[adapterPosition].item_color = color
                    })
                    .show(fragmentManager)
            }

            item_invnum_button.setOnClickListener {
                val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context)
                builder.setTitle(context.resources.getString(R.string.dialog_add_invnr_title))

                val viewInflated: View = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_add_invnum, itemView as ViewGroup?, false)
                val input = viewInflated.findViewById<View>(R.id.dialog_input_invnum) as EditText
                val container = viewInflated.findViewById<View>(R.id.dialog_input_invnum_container) as TextInputLayout

                builder.setView(viewInflated)
                builder.setPositiveButton(context.resources.getString(R.string.dialog_add), null)
                builder.setNegativeButton(context.resources.getString(R.string.dialog_cancel),
                    DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

                val mAlertDialog: androidx.appcompat.app.AlertDialog = builder.create();
                mAlertDialog.setOnShowListener(DialogInterface.OnShowListener {
                    val b: Button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    b.setOnClickListener {
                        val inputText = input.text.toString()
                        if (inputText != "" && !(";" in inputText)) {
                            val chip = Chip(context)
                            chip.text = input.text.toString()
                            chip.isCloseIconVisible = true
                            chip.setOnCloseIconClickListener {
                                item_edit_invnum.removeView(chip as View)
                                mItemModel[adapterPosition].item_invnum = Utils.chipListToString(item_edit_invnum)
                            }
                            item_edit_invnum.addView(chip)
                            mItemModel[adapterPosition].item_invnum = Utils.chipListToString(item_edit_invnum)
                            mAlertDialog.dismiss()
                        } else {
                            if (inputText == ""){
                                container.isErrorEnabled = true
                                container.error = context.resources.getString(R.string.error_dialog_invnr_empty)
                            } else {
                                container.isErrorEnabled = true
                                container.error = context.resources.getString(R.string.error_dialog_invnr_invalid)
                            }
                        }
                    }
                })
                mAlertDialog.show()

            }

            item_edit_amount.numberPickerChangeListener = object : OnNumberPickerChangeListener{
                override fun onProgressChanged(numberPicker: it.sephiroth.android.library.numberpicker.NumberPicker, progress: kotlin.Int, fromUser: kotlin.Boolean): kotlin.Unit {
                    mItemModel[adapterPosition].item_amount = item_edit_amount.progress.toString()
                }

                override fun onStartTrackingTouch(numberPicker: it.sephiroth.android.library.numberpicker.NumberPicker): kotlin.Unit {
                }

                override fun onStopTrackingTouch(numberPicker: it.sephiroth.android.library.numberpicker.NumberPicker): kotlin.Unit{
                }
            }

        }

        fun bind(model: BoxItemModel): Unit {
            item_name.text = model.item_name
            //item_edit_amount.setText(model.item_amount)
            item_edit_amount.progress = model.item_amount.toInt()
            setColorButtonColor(model.item_color, item_color_button)

            item_edit_invnum.removeAllViews()
            for (invnum in model.item_invnum.split(";")){
                if (invnum != ""){
                    val chip = Chip(context)
                    chip.text = invnum
                    chip.isCloseIconVisible = true
                    item_edit_invnum.addView(chip)
                    chip.setOnCloseIconClickListener {
                        item_edit_invnum.removeView(chip as View)
                        mItemModel[adapterPosition].item_invnum = Utils.chipListToString(item_edit_invnum)
                    }
                }
            }

            if (model.item_invnum == ""){

            }
        }
    }


    fun addToItemList(newlist: ArrayList<BoxItemModel>) {
        this.mItemModel = newlist
        notifyItemInserted(mItemModel.size-1)
        notifyItemRangeChanged(0, mItemModel.size)
    }


    fun setFilter(itemList: List<BoxItemModel>) {
        mItemModel.clear()
        mItemModel.addAll(itemList)
        this.notifyDataSetChanged()

    }


}