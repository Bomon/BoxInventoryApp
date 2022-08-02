package com.pixlbee.heros.fragments

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budiyev.android.codescanner.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.BoxAdapter
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.utility.Utils


class QRScannerFragment : Fragment() {

    //private var _binding: FragmentQRscannerBinding? = null
    private lateinit var codeScanner: CodeScanner
    var mBoxList: ArrayList<BoxModel> = ArrayList()
    lateinit var mAdapter: BoxAdapter

    private var qrContent: String = ""
    private var mSnackbar: Snackbar? = null
    private var toast: Toast? = null
    private lateinit var mFirebaseListener: ValueEventListener


    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // permission is granted
            } else {
                // handle permission denial
                val activity = requireActivity()
                activity. runOnUiThread {
                    Toast.makeText(activity, resources.getString(R.string.permission_camera_request),
                        Toast.LENGTH_LONG).show()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(Manifest.permission.CAMERA)
        }

        val view: View = inflater.inflate(R.layout.fragment_qrscanner, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_qr) as RecyclerView
        mAdapter = BoxAdapter(mBoxList, false)
        mAdapter.setOnBoxClickListener(object: BoxAdapter.OnBoxClickListener{
            override fun onBoxClicked(box: BoxModel, view: View) {
                val navController: NavController = Navigation.findNavController(view)
                val bundle = Bundle()
                bundle.putSerializable("boxModel", box)
                navController.navigate(R.id.action_navigation_qrscanner_to_boxFragment, bundle)
            }

            override fun onBoxTagClicked(tag: String) {
                // do nothing
            }
        })

        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = mAdapter

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val scannerView = view.findViewById<CodeScannerView>(R.id.scanner_view)
        val activity = requireActivity()
        codeScanner = CodeScanner(activity, scannerView)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.CONTINUOUS // or CONTINUOUS or PREVIEW or SINGLE
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            if (qrContent != it.text) {
                mFirebaseListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        val urlSplit = it.text.split("&")
                        var scannedCode = ""
                        var scannedOrg = ""
                        if (urlSplit.size == 1){
                            // Assume this is a legacy QRcode (without URL)
                            scannedCode = urlSplit[0]
                        } else if (urlSplit.size == 2){
                            // assume it is a code without org
                            scannedCode = urlSplit[1].replace("box=", "")
                        } else {
                            // assume it is a code with org
                            scannedOrg = urlSplit[1].replace("org=", "")
                            scannedCode = urlSplit[2].replace("box=", "")
                        }
                        // do not continue to search box if orgs dont match
                        if (scannedOrg != "") {
                            val currentOrg = Utils.getCurrentlySelectedOrg(context!!)
                            if (scannedOrg.lowercase() != currentOrg.lowercase()) {
                                val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
                                // serach if the user has access to this org
                                for (org in dataSnapshot.child("read_permissions").children) {
                                    val orgId: String = org.key.toString()
                                    val orgName =
                                        dataSnapshot.child(orgId).child("name").value.toString()
                                    val orgReadPermissions = org.value.toString()
                                    // If he belongs to it, show a snackbar with option to switch org
                                    if (orgId == scannedOrg && orgReadPermissions.contains(userId)) {
                                        qrContent = it.text
                                        mSnackbar?.dismiss()
                                        mBoxList.clear()
                                        toast?.cancel()
                                        mAdapter.setFilter(mBoxList)
                                        mSnackbar = Snackbar.make(
                                            view,
                                            resources.getString(R.string.qrcode_belongs_other_org) + orgName + resources.getString(R.string.qrcode_change_org),
                                            Snackbar.LENGTH_INDEFINITE
                                        ).setAction(resources.getString(R.string.qrcode_change_org_btn)) {
                                            val sharedPreferences: SharedPreferences =
                                                context!!.getSharedPreferences(
                                                    "AppPreferences",
                                                    AppCompatActivity.MODE_PRIVATE
                                                )
                                            val editor = sharedPreferences.edit()
                                            editor.putString("selected_organization", scannedOrg)
                                            editor.commit()
                                            // restart activity to load new org content
                                            startActivity(activity.intent)
                                            activity.finish()
                                        }
                                        mSnackbar?.anchorView = view.findViewById(R.id.RV_qr)
                                        mSnackbar?.show()
                                        return
                                    }
                                }
                                // otherwise show a snackbar that he is not allowd to switch
                                qrContent = ""
                                mBoxList.clear()
                                mSnackbar?.dismiss()
                                mAdapter.setFilter(mBoxList)
                                showToast(resources.getString(R.string.error_qrcode_org_no_permission), Toast.LENGTH_SHORT)
                                return
                            }
                        }

                        val boxes = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                        for (box: DataSnapshot in boxes.children){
                            val qrcode = box.child("qrcode").value.toString()
                            if (qrcode.lowercase() == scannedCode.lowercase()){
                                mSnackbar?.dismiss()
                                mBoxList.clear()
                                val boxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                                mBoxList.add(boxModel)
                                qrContent = it.text
                                toast?.cancel()
                                mAdapter.setFilter(mBoxList)
                                return
                            }
                        }
                        // if we end up here: code is invalid
                        qrContent = ""
                        mSnackbar?.dismiss()
                        showToast(resources.getString(R.string.error_qrcode_invalid), Toast.LENGTH_SHORT)
                        mBoxList.clear()
                        mAdapter.setFilter(mBoxList)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                }

                FirebaseDatabase.getInstance().reference.addValueEventListener(mFirebaseListener)
            }

        }

        //codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
        //    activity. runOnUiThread {
        //        Toast.makeText(activity, resources.getString(R.string.error_start_camera) + {it.message},
        //            Toast.LENGTH_LONG).show()
        //    }
        //}

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }


    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }


    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        mSnackbar?.dismiss()
        if (::mFirebaseListener.isInitialized) {
            FirebaseDatabase.getInstance().reference.removeEventListener(mFirebaseListener)
        }
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }


    fun showToast(text: CharSequence?, duration: Int) {
        if (toast == null)  {
            toast = Toast.makeText(requireContext(), text, duration)
        } else {
            toast!!.setText(text)
        }
        toast!!.show()
    }


}