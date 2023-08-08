package com.sahil_admin.codedoubt.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.sahil_admin.codedoubt.Utility
import com.sahil_admin.codedoubt.databinding.DoubtListViewBinding
import com.sahil_admin.codedoubt.objects.Doubt
import com.sahil_admin.codedoubt.ui.ChatActivity
import okhttp3.internal.notify

class DoubtsAdapter (
    var doubts: List<Doubt>
): RecyclerView.Adapter<DoubtsAdapter.DoubtsViewHolder>() {

    inner class DoubtsViewHolder(val binding: DoubtListViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoubtsViewHolder {
        val binding =
            DoubtListViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoubtsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoubtsViewHolder, position: Int) {
            holder.binding.apply {
            tvDoubt.text = doubts[position].question

//            if(doubts[position].solved) {
//                tvUnsolved.visibility = View.GONE
//                tvSolved.visibility = View.VISIBLE
//
//            } else {
//                tvUnsolved.visibility = View.VISIBLE
//                tvSolved.visibility = View.GONE
//            }

            tvAuthorName.text = doubts[position].author

            root.setOnClickListener {
                startActivity(root.context, Intent(root.context, ChatActivity::class.java).apply {
                    putExtra(Utility.DOUBT_CODE, doubts[position])
                }, null)
            }
        }
    }

    override fun getItemCount(): Int {
        return doubts.size
    }

    fun changeDoubts(dts: List<Doubt>) {
        doubts = dts
        notifyDataSetChanged()
    }
}