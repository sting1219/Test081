package com.example.testapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.internal.BottomNavigationItemView
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.MenuItem
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_first_main.*
import java.util.jar.Manifest

class FirstMainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    var PICK_PROFILE_FROM_ALBUM = 10

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setToolbarDefault()

        when (item.itemId) {
            R.id.action_home -> {
                var detailviewFragment = DetailviewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content,detailviewFragment).commit()
                return true
            }

            R.id.action_search -> {
                var gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content,gridFragment).commit()
                return true
            }

            R.id.action_add_photo -> {
                if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }

                return true
            }

            R.id.action_favorite_alarm -> {
                var alertFragment = AlertFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content,alertFragment).commit()
                return true
            }

            R.id.action_account -> {
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                var userFragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content,userFragment).commit()
                return true
            }
        }

        return false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_main)
        bottom_navigation.setOnNavigationItemSelectedListener(this)
        bottom_navigation.selectedItemId = R.id.action_home

        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),1)
    }

    fun setToolbarDefault(){
        toolbar_btn_back.visibility = View.GONE
        toolbar_username.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK){
            var imageUri = data?.data
            var uid = FirebaseAuth.getInstance().currentUser!!.uid
            FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid).putFile(imageUri!!).addOnCompleteListener{
                task ->
                var url = task.result.downloadUrl.toString()
                var map = HashMap<String, Any>()
                map["image"] = url
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }
        }
    }
}
