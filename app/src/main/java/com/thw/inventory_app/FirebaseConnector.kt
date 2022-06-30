package com.thw.inventory_app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FirebaseConnector {

    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var mRef: DatabaseReference

    companion object {
        var persistenceWasEnabled = false
    }

    fun initConnection() {
        val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

        firebaseAuth.signInWithEmailAndPassword("storage-app@thw.obb", "thw1952")
            .addOnCompleteListener { signIn ->
                if (signIn.isSuccessful) {
                    Log.e("Error", "Login Successful")
                } else {
                    Log.e("Error", "Login Failed")
                }
            }


        mDatabase = FirebaseDatabase.getInstance()
        if (!FirebaseConnector.persistenceWasEnabled) {
            mDatabase.setPersistenceEnabled(true)
            FirebaseConnector.persistenceWasEnabled = true
        }
        mRef = mDatabase.reference
    }

    fun getDatabase(): FirebaseDatabase {
        return mDatabase
    }

    fun getReference(): DatabaseReference {
        return mRef
    }

    fun getAllLocations(): List<String> {
        val allLocations: ArrayList<String> = ArrayList()

        val ref: DatabaseReference = mRef.child("boxes")
        val valueEventListener: ValueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (box: DataSnapshot in dataSnapshot.children){
                    val location = box.child("location").value.toString()
                    allLocations.add(location)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        ref.addListenerForSingleValueEvent(valueEventListener)
        return allLocations.distinct()
    }

}