package com.qdot.fitgo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.qdot.fitgo.databinding.ActivityMainBinding
import id.passage.android.Passage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), LoginInterface {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var passage: Passage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        passage = Passage(this).also {pass ->
            binding.loginBtn.setOnClickListener {
                AuthBottomSheet(pass,auth,this).show(supportFragmentManager,"AUTH")
            }
        }
        loadData()

    }

    override fun loginStatus(loggedIn: Boolean,err : String) {
        if (loggedIn){
            loadData()
        }else{
            CoroutineScope(Dispatchers.Main).launch{
                Toast.makeText(this@MainActivity,
                    err,Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun logoutListener(loggedOut: Boolean) {
        if (loggedOut){
            loadData()
        }
    }

    private fun checkHealthConnectIsAvailable(){
        val availabilityStatus = HealthConnectClient.getSdkStatus(this)
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            Toast.makeText(this,"Can not get data",Toast.LENGTH_SHORT).show()
            return
        }
        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        CoroutineScope(Dispatchers.IO).launch {
            checkPermissionsAndRun(healthConnectClient)
        }
    }
    private val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    private val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(PERMISSIONS)) {
                loadData()
            }
        }

    @SuppressLint("SetTextI18n")
    private suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.containsAll(PERMISSIONS)) {
            val timeEnd = LocalDateTime.now()
            val timeStart = timeEnd.withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

            val requestSteps = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(timeStart, timeEnd)
            )
            val responseSteps = healthConnectClient.readRecords(requestSteps)
            if (responseSteps.records.isNotEmpty()){
                val stepsNo = responseSteps.records.sumOf { it.count }.toString()
                withContext(Dispatchers.Main){
                    binding.stepsTv.text = stepsNo
                }
            }

            val requestDistance = ReadRecordsRequest(
                recordType = DistanceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(timeStart, timeEnd)
            )
            val responseDistance = healthConnectClient.readRecords(requestDistance)
            if (responseDistance.records.isNotEmpty()){
                val distNo = (responseDistance.records.sumOf { it.distance.inKilometers }*100.0).roundToInt()/100.0
                withContext(Dispatchers.Main){
                    binding.distanceTv.text = distNo.toString()
                }
            }

            val requestCal = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(timeStart, timeEnd)
            )
            val responseCal = healthConnectClient.readRecords(requestCal)
            if (responseCal.records.isNotEmpty()){
                val distNo = (responseCal.records[0].energy.inKilocalories*100.0).roundToInt()/100.0
                withContext(Dispatchers.Main){
                    binding.energyTv.text = distNo.toString()
                }
            }else{
                withContext(Dispatchers.Main){
                    binding.energyTv.text = "NA"
                }
            }

            val requestWeight = ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(timeStart, timeEnd)
            )
            val responseWeight = healthConnectClient.readRecords(requestWeight)
            if (responseWeight.records.isNotEmpty()){
                val distNo = (responseWeight.records.sumOf { it.weight.inKilograms }*100.0).roundToInt()/100.0
                withContext(Dispatchers.Main){
                    binding.weightTv.text = distNo.toString()
                }
            }else{
                withContext(Dispatchers.Main){
                    binding.weightTv.text = "NA"
                }
            }

            val requestHeart = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(timeStart, timeEnd)
            )
            val responseHeart = healthConnectClient.readRecords(requestHeart)
            if (responseHeart.records.isNotEmpty()){
                val distNo = responseHeart.records[0].samples[0].beatsPerMinute
                withContext(Dispatchers.Main){
                    binding.heartRateTv.text = distNo.toString()
                }
            }else{
                withContext(Dispatchers.Main){
                    binding.heartRateTv.text = "NA"
                }
            }

            val requestCycle = ReadRecordsRequest(
                recordType = SpeedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(timeStart, timeEnd)
            )
            val responseCycle = healthConnectClient.readRecords(requestCycle)
            if (responseCycle.records.isNotEmpty()){
                val distNo = (responseCycle.records[0].samples[0].speed.inMetersPerSecond*100.0).roundToInt()/100.0
                withContext(Dispatchers.Main){
                    binding.cycleDisTv.text = distNo.toString()
                }
            }else{
                withContext(Dispatchers.Main){
                    binding.cycleDisTv.text = "00.0"
                }
            }

        } else {
            requestPermissions.launch(PERMISSIONS)
        }
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = passage.getCurrentUser()
            if (currentUser != null) {
                withContext(Dispatchers.Main) {
                    checkHealthConnectIsAvailable()
                    binding.userNameTv.text = currentUser.email
                    binding.loginLayout.visibility = View.GONE
                    binding.mainDataLayout.visibility = View.VISIBLE
                    binding.menuBtn.setOnClickListener {
                        SettingsBottomSheet(passage,auth,this@MainActivity)
                            .show(supportFragmentManager,"SET")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.loginLayout.visibility = View.VISIBLE
                    binding.mainDataLayout.visibility = View.GONE
                }
            }
        }
    }

    companion object{
        val PERMISSIONS =
            setOf(
                HealthPermission.getReadPermission(HeartRateRecord::class),
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getReadPermission(DistanceRecord::class),
                HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                HealthPermission.getReadPermission(WeightRecord::class),
                HealthPermission.getReadPermission(SpeedRecord::class)
            )
    }

}