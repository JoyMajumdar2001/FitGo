package com.qdot.fitgo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.qdot.fitgo.databinding.SettingsBottomSheetLayoutBinding
import id.passage.android.Passage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsBottomSheet(private val passage: Passage,private val auth : FirebaseAuth,private val loginInterface : LoginInterface) : BottomSheetDialogFragment() {
    private var _binding: SettingsBottomSheetLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsBottomSheetLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = passage.getCurrentUser()!!
            withContext(Dispatchers.Main) {
                binding.emailTv.text = currentUser.email
                if (currentUser.emailVerified == true) {
                    binding.emailVerifyImg.setImageDrawable(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.baseline_verified_24
                        )
                    )
                }
                binding.joinedTv.text =
                    "Joined us on ${currentUser.createdAt.toString().substring(0, 11)}"
                binding.loginCountTv.text =
                    "You have logged-in ${currentUser.loginCount} times with this account"
                binding.logoutBtn.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        passage.signOutCurrentUser()
                        loginInterface.logoutListener(true)
                        auth.signOut()
                        dismiss()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}