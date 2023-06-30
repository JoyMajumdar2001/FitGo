package com.qdot.fitgo

interface LoginInterface {
    fun loginStatus(loggedIn : Boolean,err : String)
    fun logoutListener(loggedOut : Boolean)
}