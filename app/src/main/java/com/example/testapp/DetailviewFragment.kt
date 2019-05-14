package com.example.testapp

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.testapp.model.AlarmDTO
import com.example.testapp.model.ContentDTO
import com.example.testapp.model.FollowDTO
import com.facebook.share.model.AppInviteContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailviewFragment : Fragment(){
    var firestore : FirebaseFirestore? = null
    var user : FirebaseAuth? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        firestore = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance()

        var view =  LayoutInflater.from(inflater.context).inflate(R.layout.fragment_detail,container,false)
        view.detailviewfragment_recyclerview.adapter = DetailRecyclerviewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }
    inner class DetailRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        val contentDTOs : ArrayList<ContentDTO>
        val contentUidList : ArrayList<String>

        init {
            contentDTOs = ArrayList()
            contentUidList = ArrayList()

            var uid = FirebaseAuth.getInstance().currentUser?.uid

            firestore?.collection("users")?.document(uid!!)?.get()?.addOnCompleteListener {
                task->
                if(task.isSuccessful)
                {
                    var userDTO = task.result.toObject(FollowDTO::class.java)
                    if(userDTO != null) {
                        getContents(userDTO.followings)
                    }
                }
            }
        }

        fun getContents(followers:MutableMap<String,Boolean>){
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    if(followers.keys.contains(item.uid)) {
                        contentDTOs.add(item)
                    }
                    contentUidList.add(snapshot.id)
                }

                notifyDataSetChanged()
            }

        }
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_detail,p0,false)

            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            val viewHolder = (holder as CustomViewHolder).itemView
            //유저 아이디
            viewHolder.detailviewitem_profile_textview.text = contentDTOs!![position].userId

            //이미지
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewHolder.detailviewitem_imageview_content)

            //설명 텍스트
            viewHolder.detailviewitem_explain_textview.text = contentDTOs!![position].explain

            //좋아요 카운터 설정
            viewHolder.detailviewitem_favoritecounter_textview.text = "좋아요 " + contentDTOs!![position].favoriteCount + "개"
            var uid = FirebaseAuth.getInstance().currentUser!!.uid
            viewHolder.detailviewitem_favorite_imageview.setOnClickListener{
                favoriteEvent(position)
            }

            //좋아요를 클릭
            if(contentDTOs!![position].favorites.containsKey(uid)){
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{ //클릭하지 않았을경우
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            viewHolder.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid)
                bundle.putString("userId",contentDTOs[position].userId)
                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction().replace(R.id.main_content,fragment).commit()
            }

            viewHolder.detailviewitem_comment_imageview.setOnClickListener{v ->
                var intent = Intent(v.context,CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[position])
                intent.putExtra("destinationUid",contentDTOs[position].uid)
                startActivity(intent)

            }
        }

        private fun favoriteEvent(position: Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction{
                transaction ->
                var uid = FirebaseAuth.getInstance().currentUser!!.uid
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                //좋아요 관련
                if(contentDTO!!.favorites.containsKey(uid)){
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount - 1
                    contentDTO?.favorites.remove(uid)

                }else {
                    //좋아요를 누르지 않은 상태 -> 누르는 상태
                    contentDTO?.favorites[uid] = true
                    contentDTO?.favoriteCount = contentDTO.favoriteCount + 1
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc,contentDTO)
            }
        }
        fun favoriteAlarm(destinationUid:String){
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = user?.currentUser?.email
            alarmDTO.uid = user?.currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        }
    }
}