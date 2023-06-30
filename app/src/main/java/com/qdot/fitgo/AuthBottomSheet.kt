package com.qdot.fitgo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.qdot.fitgo.databinding.AuthSheetLayoutBinding
import fuel.Fuel
import fuel.get
import id.passage.android.Passage
import id.passage.android.exceptions.RegisterWithPasskeyException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AuthBottomSheet(private val passage: Passage, private val auth : FirebaseAuth,
                      private val loginInterface : LoginInterface) : BottomSheetDialogFragment() {
    private var _binding: AuthSheetLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AuthSheetLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginNow.setOnClickListener {
            if (binding.mobText.text.isNullOrEmpty()){
                Toast.makeText(requireContext(),
                "Enter email",
                Toast.LENGTH_SHORT).show()
            }
            else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        passage.loginWithPasskey(binding.mobText.text.toString())
                        val user = passage.getCurrentUser() ?: return@launch
                        loginWithFirebase(user.id!!,user.email!!)
                    } catch (e: RegisterWithPasskeyException) {
                        loginInterface.loginStatus(false,e.message.toString())
                        dismiss()
                    }
                }
            }
        }

        binding.signupNow.setOnClickListener {
            if (binding.mobText.text.isNullOrEmpty()){
                Toast.makeText(requireContext(),
                    "Enter email",
                    Toast.LENGTH_SHORT).show()
            }
            else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        passage.registerWithPasskey(binding.mobText.text.toString())
                        val user = passage.getCurrentUser() ?: return@launch
                        loginWithFirebase(user.id!!,user.email!!)
                    } catch (e: RegisterWithPasskeyException) {
                        loginInterface.loginStatus(false,e.message.toString())
                        dismiss()
                    }
                }
            }
        }
    }

    private suspend fun loginWithFirebase(id: String,email:String) {
        try {
            val responseDat = Fuel.get("https://iamtoken-1-g1882055.deta.app/user/create/$id").body
            val jsonObject = JSONObject(responseDat)
            if (jsonObject.getBoolean("ok")){
                auth.signInWithCustomToken(jsonObject.getString("data"))
                    .addOnCompleteListener {
                        if (it.isSuccessful){
                            if (auth.currentUser?.email.isNullOrEmpty()) {
                                auth.currentUser?.updateEmail(email)
                            }
                            loginInterface.loginStatus(true,"")
                            dismiss()
                        }else{
                            loginInterface.loginStatus(false,"FB auth is not completed")
                            dismiss()
                        }
                    }
            }else{
                loginInterface.loginStatus(false,"Can not create token")
                dismiss()
            }
        }catch (e:Exception){
            loginInterface.loginStatus(false,e.message.toString())
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}