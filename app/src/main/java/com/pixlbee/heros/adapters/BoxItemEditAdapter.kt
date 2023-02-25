package com.pixlbee.heros.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.fragments.BoxEditFragment
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.models.ItemMoveModel
import com.pixlbee.heros.utility.Utils
import dev.sasikanth.colorsheet.ColorSheet
import it.sephiroth.android.library.numberpicker.NumberPicker
import it.sephiroth.android.library.numberpicker.NumberPicker.OnNumberPickerChangeListener
import java.lang.Integer.min
import kotlin.math.max

class BoxItemEditAdapter(
    mDataList: ArrayList<BoxItemModel>,
    boxId: String,
    compartmentName: String,
    parentFragment: BoxEditFragment,
) : RecyclerView.Adapter<BoxItemEditAdapter.BoxItemViewHolder>() {

    lateinit var mContext: Context
    private var mItemModel: ArrayList<BoxItemModel> = ArrayList()
    lateinit var mBoxId: String
    lateinit var mCompartmentName: String
    lateinit var mParentFragment: BoxEditFragment

    private lateinit var mListenerRemove: OnItemRemoveListener
    private lateinit var mListenerMove: OnItemMoveListener


    init {
        setFilter(mDataList)
        setHasStableIds(true)
        mBoxId = boxId
        mCompartmentName = compartmentName
        mParentFragment = parentFragment
    }

    override fun getItemId(position: Int): Long {
        return mItemModel[position].numeric_id.toLong()
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

        private var itemContainer: MaterialCardView
        private var itemName: TextView
        private var itemAmount: TextView
        private var itemColorDisplay: View
        private var itemInvnums: ChipGroup

        private var itemDeleteButton: MaterialButton
        private var itemEditButton: MaterialButton
        private var itemMoveButton: MaterialButton
        private var itemPlusButton: MaterialButton
        private var itemMinusButton: MaterialButton

        init {
            itemContainer = itemView.findViewById(R.id.card_box)
            itemName = itemView.findViewById(R.id.box_item_name)
            itemAmount = itemView.findViewById(R.id.box_item_amount)
            itemColorDisplay = itemView.findViewById(R.id.box_item_color)
            itemInvnums = itemView.findViewById(R.id.box_item_invnums)
            itemDeleteButton = itemView.findViewById(R.id.box_item_delete_btn)
            itemEditButton = itemView.findViewById(R.id.box_item_edit_btn)
            itemMoveButton = itemView.findViewById(R.id.box_item_move_btn)
            itemPlusButton = itemView.findViewById(R.id.box_item_edit_plus)
            itemMinusButton = itemView.findViewById(R.id.box_item_edit_minus)

            itemDeleteButton.setOnClickListener {
                val removedItem = mItemModel.removeAt(absoluteAdapterPosition)
                notifyItemRemoved(absoluteAdapterPosition)
                notifyItemRangeChanged(absoluteAdapterPosition, mItemModel.size)
                mListenerRemove.onItemRemove(removedItem.item_compartment, removedItem.numeric_id, it)
            }

            fun fixAmountTextfield(){
                val taken = mItemModel[absoluteAdapterPosition].item_amount_taken.toInt()
                val amount = mItemModel[absoluteAdapterPosition].item_amount.toInt()
                if (taken == 0) {
                    itemAmount.text = amount.toString()
                } else {
                    itemAmount.text = (amount - taken).toString() + " / " + amount
                }
            }

            itemPlusButton.setOnClickListener {
                val newAmount = mItemModel[absoluteAdapterPosition].item_amount.toInt() + 1
                mItemModel[absoluteAdapterPosition].item_amount = newAmount.toString()
                fixAmountTextfield()
            }

            itemMinusButton.setOnClickListener {
                val newAmount = max(0, mItemModel[absoluteAdapterPosition].item_amount.toInt() - 1)
                mItemModel[absoluteAdapterPosition].item_amount = newAmount.toString()
                fixAmountTextfield()
            }

            itemMoveButton.setOnClickListener {
                // Init Elements
                val builder = MaterialAlertDialogBuilder(mContext)
                builder.setTitle(mContext.resources.getString(R.string.dialog_move_item_title))

                val viewInflated: View = LayoutInflater.from(mContext)
                    .inflate(R.layout.dialog_move_item, itemView as ViewGroup?, false)

                val targetBoxSelect = viewInflated.findViewById<View>(R.id.dialog_move_item_dropdown_box) as AutoCompleteTextView
                val targetCompartmentSelect = viewInflated.findViewById<View>(R.id.dialog_move_item_dropdown_compartment) as AutoCompleteTextView

                val targetNewCompartmentInput = viewInflated.findViewById<View>(R.id.dialog_move_item_new_compartment) as TextInputEditText
                val targetNewCompartmentInputContainer = viewInflated.findViewById<View>(R.id.dialog_move_item_new_compartment_container) as TextInputLayout

                val allBoxes: HashMap<String, String> = HashMap()
                val allBoxCompartments: ArrayList<String> = ArrayList()
                var defaultBox: String = ""
                var defaultCompartment: String = mCompartmentName

                val defaultCompartmentStrings = setOf("", "null")
                if (defaultCompartment in defaultCompartmentStrings)
                    defaultCompartment = mContext.resources.getString(R.string.compartment_default_name)


                val movedItem: ItemMoveModel = ItemMoveModel(
                    mItemModel[absoluteAdapterPosition],
                    mBoxId,
                    mCompartmentName,
                    mBoxId,
                    mCompartmentName,
                    ""
                )

                val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(mContext)).child("boxes")
                boxesRef.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val boxes: DataSnapshot? = task.result
                        if (boxes != null) {
                            for (box: DataSnapshot in boxes.children) {
                                val boxKey = box.key.toString()
                                val boxName = box.child("name").value.toString()
                                val boxId = box.child("id").value.toString()
                                allBoxes["$boxId - $boxName"] = boxKey
                                if (boxId == mBoxId) {
                                    defaultBox = "$boxId - $boxName"
                                    /*for (item: DataSnapshot in box.child("content").children){
                                        var compartment = item.child("compartment").value.toString()
                                        compartment = if (compartment in defaultCompartmentStrings) mContext.resources.getString(R.string.compartment_default_name) else compartment
                                        if (compartment !in allBoxCompartments) {
                                            allBoxCompartments.add(compartment)
                                        }
                                    }*/
                                }
                            }
                        }

                        for (newCompartment in mParentFragment.getTempCompartments()){
                            val tempComp = if (newCompartment in defaultCompartmentStrings) mContext.resources.getString(R.string.compartment_default_name) else newCompartment
                            allBoxCompartments.add(tempComp)
                        }

                        // Add entry for new compartment
                        allBoxCompartments.add(mContext.resources.getString(R.string.dialog_new_compartment_select))

                        // Init the dropdown default compartment.
                        targetCompartmentSelect.setText(allBoxCompartments[0], false)
                        when (allBoxCompartments[0]) {
                            mContext.resources.getString(R.string.compartment_default_name) -> {
                                movedItem.target_compartment = ""
                            }
                            mContext.resources.getString(R.string.dialog_new_compartment_select) -> {
                                targetNewCompartmentInputContainer.visibility = View.VISIBLE
                                movedItem.target_compartment = "CHECK_NEW_COMP_FIELD"
                            }
                            else -> {
                                movedItem.target_compartment = allBoxCompartments[0]
                            }
                        }
                        // If "new compartment" is the only button, set it to visible
                        if (allBoxCompartments.size == 1) {
                            targetNewCompartmentInputContainer.visibility = View.VISIBLE
                        } else {
                            targetNewCompartmentInputContainer.visibility = View.GONE
                        }

                        // Init adapters
                        val arrayAdapterCompartment = ArrayAdapter(mContext, R.layout.dropdown_item, allBoxCompartments)
                        targetCompartmentSelect.setAdapter(arrayAdapterCompartment)

                        val arrayAdapterBox = ArrayAdapter(mContext, R.layout.dropdown_item, allBoxes.keys.toList())
                        targetBoxSelect.setText(defaultBox, false)
                        targetBoxSelect.setAdapter(arrayAdapterBox)

                        // Find compartments if other box is clicked
                        targetBoxSelect.onItemClickListener =
                            AdapterView.OnItemClickListener { parent, view, position, id ->
                                val clickedBox = parent.adapter.getItem(position)
                                val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(mContext)).child("boxes")
                                boxesRef.get().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val boxes: DataSnapshot? = task.result
                                        if (boxes != null) {
                                            for (box: DataSnapshot in boxes.children) {
                                                val boxName = box.child("name").value.toString()
                                                val boxId = box.child("id").value.toString()
                                                val boxTitle = "$boxId - $boxName"
                                                val boxKey = box.key.toString()
                                                movedItem.target_box_id = boxId
                                                movedItem.target_box_key = boxKey
                                                if (clickedBox == boxTitle) {
                                                    allBoxCompartments.clear()
                                                    // If we are in our box, add from temp compartments
                                                    if (boxId == mBoxId) {
                                                        for (newCompartment in mParentFragment.getTempCompartments()){
                                                            val tempComp = if (newCompartment in defaultCompartmentStrings) mContext.resources.getString(R.string.compartment_default_name) else newCompartment
                                                            allBoxCompartments.add(tempComp)
                                                        }
                                                    } else {
                                                        for (item: DataSnapshot in box.child("content").children){
                                                            var compartment = item.child("compartment").value.toString()
                                                            compartment = if (compartment in defaultCompartmentStrings) mContext.resources.getString(R.string.compartment_default_name) else compartment
                                                            if (compartment !in allBoxCompartments) {
                                                                allBoxCompartments.add(compartment)
                                                            }
                                                        }
                                                    }
                                                    // Add entry for new compartment
                                                    allBoxCompartments.add(mContext.resources.getString(R.string.dialog_new_compartment_select))

                                                    targetCompartmentSelect.setText(allBoxCompartments[0], false)
                                                    if (allBoxCompartments.size == 1) {
                                                        targetNewCompartmentInputContainer.visibility = View.VISIBLE
                                                    } else {
                                                        targetNewCompartmentInputContainer.visibility = View.GONE
                                                    }
                                                    arrayAdapterCompartment.notifyDataSetChanged()

                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        // Handle compartment click
                        targetCompartmentSelect.onItemClickListener =
                            AdapterView.OnItemClickListener { parent, view, position, id ->
                                targetNewCompartmentInputContainer.visibility = View.GONE
                                val clickedCompartment = parent.adapter.getItem(position)
                                when (clickedCompartment) {
                                    mContext.resources.getString(R.string.compartment_default_name) -> {
                                        movedItem.target_compartment = ""
                                    }
                                    mContext.resources.getString(R.string.dialog_new_compartment_select) -> {
                                        targetNewCompartmentInputContainer.visibility = View.VISIBLE
                                        movedItem.target_compartment = "CHECK_NEW_COMP_FIELD"
                                    }
                                    else -> {
                                        movedItem.target_compartment = clickedCompartment.toString()
                                    }
                                }
                            }
                    }
                }

                // Init dialog buttons
                builder.setView(viewInflated)
                builder.setPositiveButton(mContext.resources.getString(R.string.dialog_move), null)
                builder.setNegativeButton(mContext.resources.getString(R.string.dialog_cancel)) { dialog, _ -> dialog.cancel() }

                val mAlertDialog: AlertDialog = builder.create()
                mAlertDialog.setOnShowListener {
                    val b: Button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    b.setOnClickListener {
                        if (movedItem.target_compartment != "CHECK_NEW_COMP_FIELD") {
                            if (movedItem.src_box_id != movedItem.target_box_id || movedItem.src_compartment != movedItem.target_compartment) {
                                mListenerMove.onItemMove(movedItem, it)
                                mAlertDialog.dismiss()
                            }
                        } else {
                            if (targetNewCompartmentInput.text.toString() in allBoxCompartments || targetNewCompartmentInput.text.toString() == ""){
                                targetNewCompartmentInputContainer.isErrorEnabled = true
                                targetNewCompartmentInputContainer.error = mContext.resources.getString(R.string.error_dialog_compartment_already_exists)
                            } else {
                                movedItem.target_compartment = targetNewCompartmentInput.text.toString()
                                mListenerMove.onItemMove(movedItem, it)
                                mAlertDialog.dismiss()
                            }
                        }
                    }
                }
                mAlertDialog.show()

            }

            // edit button
            itemEditButton.setOnClickListener {
                val builder = MaterialAlertDialogBuilder(mContext)
                builder.setTitle(mContext.resources.getString(R.string.dialog_edit_item_title))

                val viewInflated: View = LayoutInflater.from(mContext)
                    .inflate(R.layout.dialog_edit_item, itemView as ViewGroup?, false)

                val itemEditAmount: NumberPicker = viewInflated.findViewById(R.id.box_item_edit_amount)
                val itemEditAmountTaken: NumberPicker = viewInflated.findViewById(R.id.box_item_edit_amount_taken)
                val itemEditInvnum: ChipGroup = viewInflated.findViewById(R.id.box_item_invnums)
                val itemColorButton: MaterialButton = viewInflated.findViewById(R.id.box_item_color_btn)
                val itemInvnumButton: MaterialButton = viewInflated.findViewById(R.id.box_item_invnum_btn)


                itemEditAmount.progress = mItemModel[absoluteAdapterPosition].item_amount.toInt()
                itemEditAmountTaken.progress = mItemModel[absoluteAdapterPosition].item_amount_taken.toInt()
                setColorButtonColor(mItemModel[absoluteAdapterPosition].item_color, itemColorButton)

                itemEditInvnum.removeAllViews()
                for (invnum in mItemModel[absoluteAdapterPosition].item_invnum.split(";")){
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

                // For loops are needed for automatically closing number keyboard on enter press
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
                for( v in itemEditAmountTaken.children) {
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
                        mItemModel[absoluteAdapterPosition].item_amount = itemEditAmount.progress.toString()
                        itemEditAmountTaken.progress = min(itemEditAmountTaken.progress, itemEditAmount.progress)
                    }

                    override fun onStartTrackingTouch(numberPicker: NumberPicker) {
                    }

                    override fun onStopTrackingTouch(numberPicker: NumberPicker) {
                    }
                }

                itemEditAmountTaken.numberPickerChangeListener = object : OnNumberPickerChangeListener{
                    override fun onProgressChanged(numberPicker: NumberPicker, progress: Int, fromUser: Boolean) {
                        mItemModel[absoluteAdapterPosition].item_amount_taken = min(itemEditAmountTaken.progress, itemEditAmount.progress).toString()
                        itemEditAmountTaken.progress = min(itemEditAmountTaken.progress, itemEditAmount.progress)

                    }

                    override fun onStartTrackingTouch(numberPicker: NumberPicker) {
                    }

                    override fun onStopTrackingTouch(numberPicker: NumberPicker) {
                    }
                }

                builder.setView(viewInflated)
                builder.setPositiveButton(mContext.resources.getString(R.string.dialog_close)) { dialog, _ -> dialog.cancel() }

                val mAlertDialog: AlertDialog = builder.create()
                mAlertDialog.setOnShowListener {
                    val b: Button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    b.setOnClickListener {
                        bind(mItemModel[absoluteAdapterPosition])
                        mAlertDialog.dismiss()
                    }
                }
                mAlertDialog.show()
            }
        }

        fun bind(model: BoxItemModel) {
            itemName.text = model.item_name
            if (model.item_amount_taken.toInt() == 0) {
                itemAmount.text = model.item_amount
            } else {
                itemAmount.text = (model.item_amount.toInt() - model.item_amount_taken.toInt()).toString() + " / " + model.item_amount
            }
            itemColorDisplay.background.setTint(model.item_color)

            itemInvnums.removeAllViews()
            for (invnum in model.item_invnum.split(";")){
                if (invnum != ""){
                    val chip = Chip(mContext)
                    chip.text = invnum
                    chip.isCloseIconVisible = false
                    itemInvnums.addView(chip)
                }
            }
        }
    }


    fun moveItem(from: Int, to: Int): ArrayList<BoxItemModel> {
        val fromLocation = mItemModel[from]
        mItemModel.removeAt(from)
        if (to < from) {
            mItemModel.add(to, fromLocation)
        } else {
            mItemModel.add(to, fromLocation)
        }
        return mItemModel
    }


    interface OnItemRemoveListener{
        fun onItemRemove(compartmentName: String, numericItemId: String, view: View)
    }

    fun setOnItemRemoveListener(mListener: OnItemRemoveListener) {
        this.mListenerRemove = mListener
    }

    interface OnItemMoveListener{
        fun onItemMove(movedItem: ItemMoveModel, view: View)
    }

    fun setOnItemMoveListener(mListener: OnItemMoveListener) {
        this.mListenerMove = mListener
    }


    fun setFilter(itemList: List<BoxItemModel>) {
        mItemModel.clear()
        mItemModel.addAll(itemList)
        this.notifyDataSetChanged()
    }


}