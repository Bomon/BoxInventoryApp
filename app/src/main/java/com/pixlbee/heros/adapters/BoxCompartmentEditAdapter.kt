package com.pixlbee.heros.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.fragments.BoxEditFragment
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.models.CompartmentModel
import com.pixlbee.heros.models.ItemMoveModel
import com.pixlbee.heros.utility.Animations
import com.pixlbee.heros.utility.Utils


class BoxCompartmentEditAdapter(boxId: String, parentFragment: BoxEditFragment) : RecyclerView.Adapter<BoxCompartmentEditAdapter.BoxItemViewHolder>() {

    private lateinit var mContext: Context
    private var mCompartmentList: ArrayList<CompartmentModel> = ArrayList()
    private var mBoxId: String

    private lateinit var mListener: OnCompartmentItemAddListener
    private var mParentFragment: BoxEditFragment


    init {
        mBoxId = boxId
        mParentFragment = parentFragment
        setHasStableIds(true)
    }

    fun getCurrentStatus(): ArrayList<CompartmentModel> {
        return mCompartmentList
    }

    override fun getItemId(position: Int): Long {
        return mCompartmentList[position].name.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxItemViewHolder {
        mContext = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return BoxItemViewHolder(layoutInflater.inflate(R.layout.card_compartment_in_box_edit, parent, false))
    }


    override fun getItemCount(): Int {
        return mCompartmentList.size
    }


    override fun onBindViewHolder(holder: BoxItemViewHolder, position: Int) {
        val compartment = mCompartmentList[position]


        // Set display name of default compartment
        if (compartment.name == "") {
            holder.compartmentName.text = mContext.resources.getString(R.string.compartment_default_name)
        } else {
            holder.compartmentName.text = compartment.name
        }

        holder.compartmentContainer.transitionName = compartment.name

        val adapter = BoxItemEditAdapter(ArrayList(), mBoxId, compartment.name, mParentFragment)

        adapter.setOnItemRemoveListener(object: BoxItemEditAdapter.OnItemRemoveListener {
            override fun onItemRemove(compartmentName: String, numericItemId: String, view: View) {
                for (c in mCompartmentList) {
                    if (c.name == compartmentName) {
                        c.content.removeIf {e -> e.numeric_id == numericItemId}
                    }
                }
            }
        })

        fun moveItem(movedItem: ItemMoveModel) {
            val newItem = movedItem.item
            newItem.item_compartment = movedItem.target_compartment
            // move inside box
            if (movedItem.target_box_id == movedItem.src_box_id) {
                for (c in mCompartmentList) {
                    if (c.name == movedItem.target_compartment) {
                        c.content.add(newItem)
                        //adapter.setFilter(compartment.content)
                    }
                    if (c.name == movedItem.src_compartment) {
                        c.content.removeIf { e -> e.numeric_id == newItem.numeric_id }
                    }
                }
                if (movedItem.target_compartment !in mCompartmentList.map { cm -> cm.name }) {
                    var nCmpList = ArrayList<BoxItemModel>()
                    nCmpList.add(movedItem.item)
                    mCompartmentList.add(CompartmentModel(movedItem.target_compartment, nCmpList, true))
                }
            } else {  // move item out of box
                for (c in mCompartmentList) {
                    if (c.name == movedItem.src_compartment) {
                        c.content.removeIf { e -> e.numeric_id == newItem.numeric_id }
                    }
                }
                mParentFragment.moveItem(movedItem)
            }
            notifyDataSetChanged()
        }

        adapter.setOnItemMoveListener(object: BoxItemEditAdapter.OnItemMoveListener {
            override fun onItemMove(movedItem: ItemMoveModel, view: View) {
                moveItem(movedItem)
            }
        })

        holder.mBoxItemEditAdapter = adapter
        holder.recyclerview.layoutManager = LinearLayoutManager(mContext)
        holder.recyclerview.adapter = holder.mBoxItemEditAdapter

        if (compartment.is_expanded) {
            holder.rvContainer.visibility = View.VISIBLE
        } else {
            holder.rvContainer.visibility = View.GONE
        }

        val mItemList = compartment.content
        holder.mBoxItemEditAdapter.setFilter(mItemList)

        // On Compartment Clicked collapase / ellapse
        holder.itemView.setOnClickListener {
            if (compartment.is_expanded) {
                holder.collapseContent(it)
            } else {
                holder.expandContent(it)
            }
            compartment.is_expanded = !compartment.is_expanded
        }

        // On Item Added
        holder.compartmentAddItemButton.setOnClickListener { view ->
            if (view != null) {
                mListener.onCompartmentItemAdd(compartment.name, view)
            }
        }

        // On Delete Compartment
        holder.compartmentDeleteButton.visibility = View.VISIBLE
        holder.compartmentDeleteButton.setOnClickListener { view ->
            if (view != null) {
                // Init Elements
                val builder = MaterialAlertDialogBuilder(mContext)
                builder.setTitle(mContext.resources.getString(R.string.dialog_delete_compartment_title))

                val viewInflated: View = LayoutInflater.from(mContext)
                    .inflate(
                        R.layout.dialog_delete_compartment,
                        holder.itemView as ViewGroup?,
                        false
                    )

                val moveContainer =
                    viewInflated.findViewById<View>(R.id.dialog_del_move_menu) as LinearLayout

                val radioBtnDelete =
                    viewInflated.findViewById<View>(R.id.dialog_del_comp_radio_delete) as RadioButton
                val radioBtnMove =
                    viewInflated.findViewById<View>(R.id.dialog_del_comp_radio_move) as RadioButton

                val targetBoxSelect =
                    viewInflated.findViewById<View>(R.id.dialog_del_comp_dropdown_box) as AutoCompleteTextView
                val targetCompartmentSelect =
                    viewInflated.findViewById<View>(R.id.dialog_del_comp_dropdown_compartment) as AutoCompleteTextView

                val targetNewCompartmentInput = viewInflated.findViewById<View>(R.id.dialog_move_item_new_compartment) as TextInputEditText
                val targetNewCompartmentInputContainer = viewInflated.findViewById<View>(R.id.dialog_move_item_new_compartment_container) as TextInputLayout

                moveContainer.visibility = View.GONE

                radioBtnDelete.setOnClickListener {
                    moveContainer.visibility = View.GONE
                }
                radioBtnMove.setOnClickListener {
                    moveContainer.visibility = View.VISIBLE
                }

                // Init the default dropdown selection
                val allBoxes: HashMap<String, String> = HashMap()
                val allBoxCompartments: ArrayList<String> = ArrayList()
                var defaultBox: String = ""
                val defaultCompartment: String = holder.compartmentName.text.toString()

                var targetBoxId = mBoxId
                var targetBoxKey = ""
                var targetCompartment = holder.compartmentName.text

                val defaultCompartmentStrings = setOf("", "null")

                val boxesRef = FirebaseDatabase.getInstance().reference.child(
                    Utils.getCurrentlySelectedOrg(mContext)
                ).child("boxes")
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
                                    /*for (item: DataSnapshot in box.child("content").children) {
                                        var compartment = item.child("compartment").value.toString()
                                        compartment = if (compartment in defaultCompartmentStrings) mContext.resources.getString(R.string.compartment_default_name) else compartment
                                        if (compartment !in allBoxCompartments) {
                                            allBoxCompartments.add(compartment)
                                        }
                                    }*/
                                }
                            }
                        }

                        for (newCompartment in mParentFragment.getTempCompartments()) {
                            val tempComp = if (newCompartment in defaultCompartmentStrings) mContext.resources.getString(R.string.compartment_default_name) else newCompartment
                            allBoxCompartments.add(tempComp)
                        }

                        // Add entry for new compartment
                        allBoxCompartments.add(mContext.resources.getString(R.string.dialog_new_compartment_select))

                        // Init the dropdown default compartment.
                        targetCompartmentSelect.setText(allBoxCompartments[0], false)
                        when (allBoxCompartments[0]) {
                            mContext.resources.getString(R.string.compartment_default_name) -> {
                                targetCompartment = ""
                            }
                            mContext.resources.getString(R.string.dialog_new_compartment_select) -> {
                                targetNewCompartmentInputContainer.visibility = View.VISIBLE
                                targetCompartment = "CHECK_NEW_COMP_FIELD"
                            }
                            else -> {
                                targetCompartment = allBoxCompartments[0].toString()
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

                        val arrayAdapterBox = ArrayAdapter(
                            mContext,
                            R.layout.dropdown_item,
                            allBoxes.keys.toList()
                        )
                        targetBoxSelect.setText(defaultBox, false)
                        targetBoxSelect.setAdapter(arrayAdapterBox)

                        // Find compartments if other box is clicked
                        targetBoxSelect.onItemClickListener =
                            AdapterView.OnItemClickListener { parent, view, position, id ->
                                val clickedBox = parent.adapter.getItem(position)
                                val boxesRef = FirebaseDatabase.getInstance().reference.child(
                                    Utils.getCurrentlySelectedOrg(mContext)
                                ).child("boxes")
                                boxesRef.get().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val boxes: DataSnapshot? = task.result
                                        if (boxes != null) {
                                            for (box: DataSnapshot in boxes.children) {
                                                val boxName = box.child("name").value.toString()
                                                val boxId = box.child("id").value.toString()
                                                val boxTitle = "$boxId - $boxName"
                                                val boxKey = box.key.toString()
                                                targetBoxId = boxId
                                                targetBoxKey = boxKey
                                                if (clickedBox == boxTitle) {
                                                    allBoxCompartments.clear()
                                                    // If we are in our box, add from temp compartments
                                                    if (boxId == mBoxId) {
                                                        for (newCompartment in mParentFragment.getTempCompartments()){
                                                            val tempComp = if (newCompartment in defaultCompartmentStrings) mContext.resources.getString(R.string.compartment_default_name) else newCompartment
                                                            allBoxCompartments.add(tempComp)
                                                        }
                                                    }  else {
                                                        for (item: DataSnapshot in box.child("content").children) {
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
                                var clickedCompartment = parent.adapter.getItem(position)
                                when (clickedCompartment) {
                                    mContext.resources.getString(R.string.compartment_default_name) -> {
                                        targetCompartment = ""
                                    }
                                    mContext.resources.getString(R.string.dialog_new_compartment_select) -> {
                                        targetNewCompartmentInputContainer.visibility = View.VISIBLE
                                        targetCompartment = "CHECK_NEW_COMP_FIELD"
                                    }
                                    else -> {
                                        targetCompartment = clickedCompartment.toString()
                                    }
                                }
                            }
                    }
                }

                // Init dialog buttons
                builder.setView(viewInflated)
                builder.setPositiveButton(
                    mContext.resources.getString(R.string.dialog_ok),
                    null
                )
                builder.setNegativeButton(mContext.resources.getString(R.string.dialog_cancel)) { dialog, _ -> dialog.cancel() }

                val mAlertDialog: AlertDialog = builder.create()
                mAlertDialog.setOnShowListener {
                    val b: Button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    b.setOnClickListener {
                        // On Move
                        if (radioBtnMove.isChecked) {
                            var doMove = false
                            val itemsToMove = ArrayList<ItemMoveModel>()
                            if (targetCompartment != "CHECK_NEW_COMP_FIELD") {
                                if (mBoxId != targetBoxId || compartment.name != targetCompartment) {
                                    doMove = true
                                }
                            } else {
                                if (targetNewCompartmentInput.text.toString() in allBoxCompartments || targetNewCompartmentInput.text.toString() == ""){
                                    targetNewCompartmentInputContainer.isErrorEnabled = true
                                    targetNewCompartmentInputContainer.error = mContext.resources.getString(R.string.error_dialog_compartment_already_exists)
                                } else {
                                    targetCompartment = targetNewCompartmentInput.text.toString()
                                    doMove = true
                                }
                            }
                            if (doMove) {
                                for (item in compartment.content) {
                                    val movedItem = ItemMoveModel(
                                        item,
                                        mBoxId,
                                        compartment.name,
                                        targetBoxId,
                                        targetCompartment.toString(),
                                        targetBoxKey
                                    )
                                    itemsToMove.add(movedItem)
                                }
                                for (item in itemsToMove) {
                                    moveItem(item)
                                }
                                mCompartmentList.removeAt(position)
                                notifyDataSetChanged()
                                mAlertDialog.dismiss()
                            }
                        // On Delete
                        } else if (radioBtnDelete.isChecked) {
                            mParentFragment.removeTempCompartment(mCompartmentList[position].name)
                            mCompartmentList.removeAt(position)
                            notifyDataSetChanged()
                            mAlertDialog.dismiss()
                        }
                    }
                }
                mAlertDialog.show()
            }

        }
        holder.isExpanded = compartment.is_expanded


        val itemTouchHelper by lazy {
            val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, 0) {

                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    val adapter = recyclerView.adapter as BoxItemEditAdapter
                    val from = viewHolder.adapterPosition
                    val to = target.adapterPosition

                    adapter.notifyItemMoved(from, to)

                    return true
                }

                override fun onMoved(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    fromPos: Int,
                    target: RecyclerView.ViewHolder,
                    toPos: Int,
                    x: Int,
                    y: Int
                ) {
                    super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                    val adapter = recyclerView.adapter as BoxItemEditAdapter
                    val updatedItemList = adapter.moveItem(fromPos, toPos)
                    mCompartmentList[holder.absoluteAdapterPosition].content = updatedItemList
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)

                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        viewHolder?.itemView?.alpha = 0.5f
                    }
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)

                    viewHolder.itemView.alpha = 1.0f
                }
            }

            ItemTouchHelper(simpleItemTouchCallback)
        }
        itemTouchHelper.attachToRecyclerView(holder.recyclerview)

    }


    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var compartmentName: TextView = itemView.findViewById(R.id.compartment_name)
        var compartmentContainer: MaterialCardView = itemView.findViewById(R.id.box_compartment_card_small)
        val recyclerview = itemView.findViewById<View>(R.id.compartment_content) as RecyclerView
        private val compartmentArrow: ImageView = itemView.findViewById(R.id.compartment_arrow)
        val compartmentAddItemButton: MaterialButton = itemView.findViewById(R.id.compartment_add_item_button)
        val compartmentDeleteButton: MaterialButton = itemView.findViewById(R.id.compartment_delete_button)
        val rvContainer: RelativeLayout = itemView.findViewById(R.id.compartment_content_container)
        var isExpanded: Boolean = false

        lateinit var mBoxItemEditAdapter: BoxItemEditAdapter

        fun expandContent(p0: View?) {
            compartmentArrow.animate().setDuration(100).rotation(180F)
            Animations.expand(rvContainer)
            //recyclerview.visibility = View.VISIBLE
            //recyclerview.alpha = 0.0f
            //recyclerview.animate()
                //.translationY(recyclerview.height.toFloat())
            //    .alpha(1.0f)
           //     .setDuration(500)
        }

        fun collapseContent(p0: View?) {
            compartmentArrow.animate().setDuration(100).rotation(0F)
            Animations.collapse(rvContainer)
            //recyclerview.visibility = View.GONE
            //recyclerview.alpha = 1.0f
            //recyclerview.animate()
                //.translationY(recyclerview.height.toFloat())
            //    .alpha(0.0f)
            //    .setDuration(500)
        }
    }

    interface OnCompartmentItemAddListener{
        fun onCompartmentItemAdd(compartmentName: String, view: View)
    }

    fun setOnCompartmentItemAddListener(mListener: OnCompartmentItemAddListener) {
        this.mListener = mListener
    }


    fun setFilter(compartmentItems: ArrayList<CompartmentModel>) {
        mCompartmentList.clear()
        mCompartmentList.addAll(compartmentItems)
        this.notifyDataSetChanged()
    }

}