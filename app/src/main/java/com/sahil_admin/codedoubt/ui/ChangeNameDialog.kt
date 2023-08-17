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
import com.sahil_admin.codedoubt.databinding.DialogChangeNameBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeNameDialog: DialogFragment() {
    lateinit var binding: DialogChangeNameBinding

    private val dashboardViewModel: DashboardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogChangeNameBinding.inflate(layoutInflater)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.enter_username)
            .setView(binding.root)
            .setPositiveButton(R.string.update) {_, _ ->
                val text = binding.etNewName.text.toString()
                if(!text.isEmpty()) dashboardViewModel.updateUsername(text)
            }.setNegativeButton(R.string.cancel) {dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
    }
}