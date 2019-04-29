package com.example.testapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.testapp.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {

    var contentUid : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        contentUid = intent.getStringExtra("contentUid")
        comment_recyclerview.adapter = CommentRecyclerViewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        comment_btn_send.setOnClickListener{
            var comment = ContentDTO.Comment()
        comment.userId = FirebaseAuth.getInstance().currentUser!!.email
        comment.comment = comment_edit_message.text.toString()
        comment.uid = FirebaseAuth.getInstance().currentUser!!.uid
        comment.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
        comment_edit_message.setText("")
    }
}

    inner class CommentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        val comments : ArrayList<ContentDTO.Comment>

        init{
            comments = ArrayList()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").orderBy("timestamp").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                comments.clear()
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents!!){
                    comments.add(snapshot.toObject(ContentDTO.Comment::class.java))
                }
                notifyDataSetChanged()
            }

        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent,false)
            return CustomViewHolder(view)

        }

        private inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            view.commentviewItem_textview_comment.text = comments[position].comment
            view.commentviewItem_textview_profile.text = comments[position].userId

        }

    }
}
