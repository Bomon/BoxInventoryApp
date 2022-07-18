package com.pixlbee.heros.fragments

import android.Manifest
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
    var boxList: ArrayList<BoxModel> = ArrayList()
    lateinit var adapter: BoxAdapter
    lateinit var rv: RecyclerView

    private var qr_content: String = ""
    private var toast: Toast? = null
    private lateinit var firebase_listener: ValueEventListener


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
        adapter = BoxAdapter(boxList, false)
        adapter.setOnBoxClickListener(object: BoxAdapter.OnBoxClickListener{
            override fun onBoxClicked(box: BoxModel, view: View) {
                val navController: NavController = Navigation.findNavController(view)
                val bundle = Bundle()
                bundle.putSerializable("boxModel", box)
                navController.navigate(R.id.action_navigation_qrscanner_to_boxFragment, bundle)
            }
        })

        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = adapter

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
            if (qr_content != it.text) {
                firebase_listener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        val boxes = dataSnapshot.child("boxes")
                        for (box: DataSnapshot in boxes.children){
                            val qrcode = box.child("qrcode").value.toString()
                            val urlSplit = it.text.split("&")
                            var scannedCode = ""
                            scannedCode = if (urlSplit.size == 1){
                                // Assume this is a legacy QRcode (without URL)
                                urlSplit[0]
                            } else {
                                urlSplit[1].replace("box=", "")
                            }
                            if (qrcode.lowercase() == scannedCode.lowercase()){
                                boxList.clear()
                                val boxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                                boxList.add(boxModel)
                                qr_content = it.text
                                toast?.cancel()
                                adapter.setFilter(boxList)
                                return
                            }
                        }
                        // if we end up here: code is invalid
                        showToast(resources.getString(R.string.error_qrcode_invalid), Toast.LENGTH_SHORT)
                        boxList.clear()
                        adapter.setFilter(boxList)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                }

                FirebaseDatabase.getInstance().reference.addValueEventListener(firebase_listener)
            }

        }

        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            activity. runOnUiThread {
                Toast.makeText(activity, resources.getString(R.string.error_start_camera) + {it.message},
                    Toast.LENGTH_LONG).show()
            }
        }

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
        if (::firebase_listener.isInitialized) {
            FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
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