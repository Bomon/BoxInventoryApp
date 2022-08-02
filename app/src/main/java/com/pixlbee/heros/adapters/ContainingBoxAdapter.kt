package com.pixlbee.heros.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.fragments.ItemFragmentDirections
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.VehicleModel
import com.pixlbee.heros.utility.Utils


class ContainingBoxAdapter(
    mDataList: ArrayList<BoxModel>,
    item_id: String,
) : RecyclerView.Adapter<ContainingBoxAdapter.ContainingBoxViewHolder>() {

    lateinit var mContext: Context
    lateinit var mItemId: String
    private var mBoxList: ArrayList<BoxModel> = ArrayList()


    private lateinit var mListener: OnContainingBoxClickListener


    init {
        setFilter(mDataList)
        this.mItemId = item_id
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContainingBoxViewHolder {
        mContext = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return ContainingBoxViewHolder(layoutInflater.inflate(R.layout.card_box_with_inv, parent, false))
    }


    override fun getItemCount(): Int {
        return mBoxList.size
    }


    override fun onBindViewHolder(holder: ContainingBoxViewHolder, position: Int) {
        holder.bind(mBoxList[position])
    }


    inner class ContainingBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        private var boxId: TextView = itemView.findViewById(R.id.box_inv_id)
        private var boxName: TextView = itemView.findViewById(R.id.box_inv_name)
        private var boxVehicle: TextView = itemView.findViewById(R.id.box_inv_vehicle)
        private var boxImage: ImageView = itemView.findViewById(R.id.box_inv_img)
        private var boxInvLabel: TextView = itemView.findViewById(R.id.box_inv_label)
        private var boxInvnums: ChipGroup = itemView.findViewById(R.id.box_inv_invnums)
        private var boxAmount: TextView = itemView.findViewById(R.id.box_inv_amount)
        private var boxStatus: ChipGroup = itemView.findViewById(R.id.box_inv_status)
        private var boxColor: View = itemView.findViewById(R.id.box_inv_color)
        private var boxContainer: MaterialCardView = itemView.findViewById(R.id.box_inv_card_small)
        private var boxIncompleteIcon: ImageView = itemView.findViewById(R.id.box_inv_incomplete_icon)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(model: BoxModel) {
            boxId.text = model.id
            boxName.text = model.name


            val vehiclesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(mContext)).child("vehicles")
            vehiclesRef.get().addOnCompleteListener { task ->
                var foundVehicle = false
                var mVehicle: VehicleModel? = null
                if (task.isSuccessful) {
                    val vehicles: DataSnapshot? = task.result
                    if (vehicles != null) {
                        for (vehicle: DataSnapshot in vehicles.children) {
                            val id = vehicle.child("id").value.toString()
                            val vehicleKey = vehicle.key.toString()
                            if (id == mBoxList[position].vehicle) {
                                boxVehicle.text = vehicle.child("name").value.toString()
                                foundVehicle = true
                                mVehicle = Utils.readVehicleModelFromDataSnapshot(vehicle)
                                break
                            }
                        }
                    }
                }
                if (!foundVehicle){
                    boxVehicle.text = mContext.resources.getString(R.string.error_no_vehicle_assigned)
                } else {
                    boxVehicle.setOnClickListener {
                        val navController: NavController = Navigation.findNavController(it)
                        navController.navigate(
                            ItemFragmentDirections.actionItemFragmentToVehicleDetailFragment(
                                mVehicle!!
                            )
                        )
                    }
                }
            }

            boxColor.background.setTint(model.color)

            val img = Utils.stringToBitMap(model.image)
            if (img != null){
                boxImage.setImageBitmap(img)
            }

            boxStatus.removeAllViews()
            if(model.status != ""){
                for (tag in model.status.split(";")){
                    if (tag != ""){
                        val chip = Chip(mContext)
                        chip.text = tag
                        chip.minHeight = 1
                        chip.setTextAppearance(R.style.BoxStatusChip)
                        boxStatus.addView(chip)
                    }
                }
            }

            if (boxInvLabel != null && boxInvnums != null) {
                var countAmount = 0
                var countAmountTaken = 0
                val invnums = ArrayList<String>()
                for (ci in model.content){
                    if (ci.id == mItemId){
                        countAmountTaken += ci.amount_taken.toInt()
                        countAmount += ci.amount.toInt()
                        if (ci.invnum != ""){
                            invnums.add(ci.invnum)
                        }
                    }
                }

                if (countAmountTaken == 0) {
                    boxAmount.text = countAmount.toString()
                } else {
                    boxAmount.text = (countAmount - countAmountTaken).toString() + " / $countAmount"
                }

                if (invnums.size > 0){
                    boxInvLabel.visibility = View.VISIBLE
                    boxInvnums.visibility = View.VISIBLE
                    boxInvnums.removeAllViews()
                    for (invnum in invnums){
                        if (invnum != ""){
                            val chip = Chip(mContext)
                            chip.setTextAppearance(android.R.style.TextAppearance_Material_Small)
                            chip.isClickable = false
                            chip.text = invnum
                            boxInvnums.addView(chip)
                        }
                    }
                } else {
                    boxInvLabel.visibility = View.GONE
                    boxInvnums.visibility = View.GONE
                }
            }
            boxContainer.transitionName = model.id




            // show incomplete icon if box has items taken out
            var isIncomplete = false
            for (ci in mBoxList[position].content){
                if (ci.amount_taken.toInt() != 0){
                    isIncomplete = true
                    break
                }
            }
            if (isIncomplete){
                boxIncompleteIcon.visibility = View.VISIBLE
            } else {
                boxIncompleteIcon.visibility = View.GONE
            }

        }

        override fun onClick(view: View?) {
            if(mListener != null){
                // second argument is the element from which the transition will start
                mListener.onContainingBoxClicked(mBoxList[adapterPosition], boxContainer)
            }
            true
        }
    }


    fun setFilter(boxList: List<BoxModel>) {
        mBoxList.clear()
        mBoxList.addAll(boxList)
        this.notifyDataSetChanged()
    }


    interface OnContainingBoxClickListener{
        fun onContainingBoxClicked(box: BoxModel, view: View)
    }


    fun setOnBoxClickListener(mListener: OnContainingBoxClickListener) {
        this.mListener = mListener
    }


}