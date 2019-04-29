package com.example.testapp

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.testapp.model.AlarmDTO
import com.example.testapp.model.ContentDTO
import com.example.testapp.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_first_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    var fragmentView : View? = null
    var PICK_PROFILE_FROM_ALBUM = 10
    var firestore: FirebaseFirestore? = null
    var currentUserUid: String? = null
    var uid : String? = null

    var auth : FirebaseAuth? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fragmentView = inflater.inflate(R.layout.fragment_user, container, false)

        uid = currentUserUid
        if(arguments != null) {
            uid = arguments!!.getString("destinationUid")
            if (uid != null && uid == currentUserUid) {
                //나의 유저 페이지
                fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
                fragmentView?.account_btn_follow_signout?.setOnClickListener{
                    activity?.finish()
                    startActivity(Intent(activity,MainActivity::class.java))
                    auth?.signOut()
                }

            } else {
                //제3자의 유저 페이지
                fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)

                var mainActivity = (activity as FirstMainActivity)
                mainActivity.toolbar_title_image.visibility = View.GONE
                mainActivity.toolbar_btn_back.visibility = View.VISIBLE
                mainActivity.toolbar_username.visibility = View.VISIBLE
                mainActivity.toolbar_username.text = arguments!!.getString("userId")

                mainActivity.toolbar_btn_back.setOnClickListener {
                    mainActivity.bottom_navigation.selectedItemId = R.id.action_home
                }

                fragmentView?.account_btn_follow_signout?.setOnClickListener{
                    requestFollow()
                }
            }
        }


        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)
        getProfileImages()

        getFollower()
        getFollowing()

        return fragmentView
    }

    fun requestFollow(){
        var tsDocFollowing = firestore!!.collection("users").document(currentUserUid!!)

        firestore?.runTransaction{
            transaction ->
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            if(followDTO == null){
                //아무도 팔로잉 하지 않았을 경우
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true

                transaction.set(tsDocFollowing,followDTO)
                return@runTransaction
            }

            //내 아이디가 제3자를 이미 팔로잉 하고 있을경우
            if(followDTO.followings.containsKey(uid)){
                followDTO?.followerCount = followDTO?.followerCount - 1
                followDTO?.followings.remove(uid)
            }else{
                //내가 제3자를 팔로잉 하지 않았을 경우
                followDTO?.followerCount = followDTO?.followerCount + 1
                followDTO.followings[uid!!] = true
            }

            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }

        var tsDocFollower = firestore!!.collection("users").document(uid!!)
        firestore?.runTransaction{ transaction ->
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)

            if(followDTO == null) {
                //아무도 팔로워 하지 않았을 경우
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower,followDTO)
                return@runTransaction
            }
            //제3자의 유저를 내가 팔로잉 하고 있을 경우
            if(followDTO.followers.containsKey(currentUserUid!!)){
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)

                transaction.set(tsDocFollower,followDTO)
            }else{
                //제3자를 내가 팔로워 하지 않았을 경우 - > 팔로워 하겠다.
                followDTO.followerCount = followDTO.followerCount + 1
                followDTO.followers[currentUserUid!!] = true
                followerAlarm(uid)
            }
            transaction.set(tsDocFollower,followDTO)
            return@runTransaction
        }
    }

    fun getProfileImages(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->

            if(documentSnapshot == null) return@addSnapshotListener

            if(documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]
                Glide.with(activity).load(url).apply(RequestOptions().circleCrop()).into(fragmentView!!.account_iv_profile)
            }
        }
    }

    fun followerAlarm(destinationUid: String?){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)


    }

    fun getFollower(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount.toString()
        }
    }

    fun getFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            fragmentView?.account_tv_following_count?.text = followDTO?.followingCount.toString()
        }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO>
        init {
            contentDTOs = ArrayList()
            firestore?.collection("images")?.whereEqualTo("uid",currentUserUid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java))
                }
                account_tv_post_count.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3
            var imageview = ImageView(parent.context)
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageview)
        }

        private inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

    }

}
