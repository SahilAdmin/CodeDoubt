package com.sahil_admin.codedoubt.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sahil_admin.codedoubt.databinding.LeaderboardListViewBinding
import com.sahil_admin.codedoubt.objects.AuthUser

class LeaderBoardAdapter (
    var leaders: MutableList<AuthUser>,
    var upvoted: MutableList<String?>,
    val listener: LeaderBoardRVListener
        ) : RecyclerView.Adapter<LeaderBoardAdapter.ViewHolder>(){

    inner class ViewHolder (val binding: LeaderboardListViewBinding):
        RecyclerView.ViewHolder(binding.root)

    interface LeaderBoardRVListener {
        fun onFavoriteClicked (email: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            LeaderboardListViewBinding.inflate(LayoutInflater.from(parent.context), null, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            val user = leaders[position]
            tvName.text = user.name
            tvVotes.text = user.upvotes!!.toString()

            if(upvoted.contains(user.email)) {
                ivHeartOutline.visibility = View.GONE
                ivHeartRed.visibility = View.VISIBLE
            } else {
                ivHeartOutline.visibility = View.VISIBLE
                ivHeartRed.visibility = View.GONE
            }

            ivHeartOutline.setOnClickListener {
                listener.onFavoriteClicked(user.email!!)
            }

            ivHeartRed.setOnClickListener {
                listener.onFavoriteClicked(user.email!!)
            }
        }
    }

    override fun getItemCount(): Int {
        return leaders.size
    }

    fun changeList (newLeader: MutableList<AuthUser>) {
        leaders = newLeader
        notifyDataSetChanged()
    }
}