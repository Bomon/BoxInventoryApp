package com.pixlbee.heros.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.preferences.ImagePreference
import com.pixlbee.heros.utility.Utils


class SettingsFragment : PreferenceFragmentCompat() {

    lateinit var clickedPreference: Preference
    private lateinit var startSelectImageResult: ActivityResultLauncher<Intent>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "AppPreferences"
        setPreferencesFromResource(R.xml.pref_settings, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startSelectImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == AppCompatActivity.RESULT_OK) {
                    val uri: Uri = data?.data!!
                    val bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
                    (clickedPreference as ImagePreference).setImageURI(uri)

                    val image = Utils.getEncoded64ImageStringFromBitmap(bitmap)
                    val orgRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!))
                    orgRef.get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val org: DataSnapshot? = task.result
                            if (org != null) {
                                FirebaseDatabase.getInstance().reference.child(
                                    Utils.getCurrentlySelectedOrg(
                                        context!!
                                    )
                                ).child(clickedPreference.key)
                                    .setValue(image)

                                val sharedPreferences: SharedPreferences = context!!.getSharedPreferences("AppPreferences",
                                    AppCompatActivity.MODE_PRIVATE
                                )
                                val editor = sharedPreferences.edit()
                                editor.putString(clickedPreference.key, image)
                                editor.commit()

                            }
                        }
                    }

                    (clickedPreference as ImagePreference).hideSpinner()
                } else {
                    (clickedPreference as ImagePreference).hideSpinner()
                }
            }

    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {

        if (preference.key == "pdf_logo_left" || preference.key == "pdf_logo_right") {
            clickedPreference = preference

            ImagePicker.with(this)
                .crop()                    //Crop image(Optional), Check Customization for more option
                .compress(2048)            //Final image size will be less than 1 MB(Optional)
                .createIntent { intent ->
                    (preference as ImagePreference).showSpinner()
                    startSelectImageResult.launch(intent)
                }
        }

        return super.onPreferenceTreeClick(preference)
    }

}