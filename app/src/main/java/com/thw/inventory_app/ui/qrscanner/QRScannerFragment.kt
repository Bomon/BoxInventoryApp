package com.thw.inventory_app.ui.qrscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budiyev.android.codescanner.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.thw.inventory_app.*
import com.thw.inventory_app.R


class QRScannerFragment : Fragment() {

    //private var _binding: FragmentQrscannerBinding? = null
    private lateinit var codeScanner: CodeScanner
    var cardList: ArrayList<BoxModel> = ArrayList<BoxModel>()
    lateinit var adapter: BoxAdapter
    private var qr_content: String = ""
    private var toast: Toast? = null
    lateinit var firebase_listener: ValueEventListener

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // permission is granted
            } else {
                // handle permission denial
                val activity = requireActivity()
                activity. runOnUiThread {
                    Toast.makeText(activity, "Kamera Zugriff wird benötigt, um QR Code Scanner zu nutzen",
                        Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //(activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(Manifest.permission.CAMERA)
        }

        cardList = ArrayList()
        val view: View = inflater.inflate(R.layout.fragment_qrscanner, container, false)
        val myrv = view.findViewById<View>(R.id.RV_qr) as RecyclerView
        adapter = BoxAdapter(cardList, true, R.layout.card_box_qr)
        myrv.layoutManager = LinearLayoutManager(activity)
        myrv.adapter = adapter

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
                            if (qrcode.lowercase() == it.text.lowercase()){
                                cardList.clear()
                                val boxModel = Utils.readBoxModelFromDataSnapshot(box)
                                cardList.add(boxModel)
                                qr_content = it.text
                                toast?.cancel()
                                adapter.setFilter(cardList)
                                return
                            }
                        }
                        // if we end up here: code is invalid
                        showToast("QR-Code ungültig", Toast.LENGTH_SHORT)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                }

                FirebaseDatabase.getInstance().reference.addValueEventListener(firebase_listener)



                //qr_content = it.text
                //cardList.clear()
                //cardList.add(BoxModel("ID1", "Box 1", it.text, "R.drawable.ic_home_black_24dp"))

                //Handler(Looper.getMainLooper()).post(Runnable { card_adapter.notifyDataSetChanged() })
            }

        }
        //codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            //activity. runOnUiThread {
            //    Toast.makeText(activity, "Kamera konnte nicht gestartet werden: ${it.message}",
            //        Toast.LENGTH_LONG).show()
            //}
        //}
        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }


    }

    override fun onResume() {
        Log.w("me","resume")
        super.onResume()
        codeScanner.startPreview()
    }

    fun pause() {
        codeScanner.releaseResources()
    }

    override fun onPause() {
        Log.w("me","pause")
        codeScanner.releaseResources()
        super.onPause()
    }

    override fun onStop() {
        Log.w("me","stop")
        super.onStop()
        //(activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }

    override fun onDestroyView() {
        Log.w("qr","destroy")
        super.onDestroyView()
        if (::firebase_listener.isInitialized) {
            FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
        }
        //_binding = null
    }


    fun showToast(text: CharSequence?, duration: Int) {
        if (toast == null)  {
            toast = Toast.makeText(requireContext(), text, duration)
        } else {
            toast!!.setText(text)
        }
        toast!!.show()
    }

    fun showToast(resId: Int, duration: Int) {
        showToast(resources.getText(resId), duration)
    }


}