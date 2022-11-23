package com.sahil_admin.codedoubt.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sahil_admin.codedoubt.R
import com.sahil_admin.codedoubt.databinding.DialogCreateDoubtBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateDoubtDialog: DialogFragment() {
    lateinit var binding: DialogCreateDoubtBinding

    private val dashboardViewModel: DashboardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCreateDoubtBinding.inflate(layoutInflater)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ask_a_doubt)
            .setView(binding.root)
            .setPositiveButton(R.string.create) {_, _ ->
                dashboardViewModel.createChannel(binding.etChannelName.text.toString())
            }.setNegativeButton(R.string.cancel) {dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
    }
}