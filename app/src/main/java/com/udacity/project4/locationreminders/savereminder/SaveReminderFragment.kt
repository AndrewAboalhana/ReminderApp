package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private  var binding: FragmentSaveReminderBinding?= null
    
    private var geofencingClient : GeofencingClient?= null
    private val geofencePendingIntent : PendingIntent by lazy {
        val intent = Intent(requireActivity(),GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val runningQOrLater = Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private val LOCATION_PERMISSION_INDEX =0
    private val BACKGROUND_LOCATION_PERMISSION_INDEX =1
    private val REQUEST_FOREGROUND_AND_BACKGROUND_LOCATION_PERMISSION_RESULT_CODE =5
    private val REQUEST_FOREGROUND_ONLY_PERMISSION_REQUEST_CODE =6

    private var userReminderData : ReminderDataItem?=null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding?.viewModel = _viewModel

        return binding !!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.lifecycleOwner = this

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        binding?.selectLocation?.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding?.saveReminder?.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value//
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value//
            val longitude = _viewModel.longitude.value

            userReminderData = ReminderDataItem(
                title =title,
                description = description,
                location = location,
                latitude = latitude,
                longitude = longitude
            )

            if (_viewModel.validateEnteredData(userReminderData!!)){

                if(foregroundAndBackgroundLocationPermissionApproved()){
                    checkDeviceLocationSettingsAndStartGeofence()
                }else{
                    requestForegroundAndBackgroundLocationPermission()
                }
            }

        }
    }


    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved():Boolean{
        val foreGroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        context?.let { ActivityCompat.checkSelfPermission(
                            it,android.Manifest.permission.ACCESS_FINE_LOCATION )}
                )
        val backgroundPermissionApproved =
            if (runningQOrLater){
                PackageManager.PERMISSION_GRANTED == context?.let {
                    ActivityCompat.checkSelfPermission(it,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION )}
            }else{
                true
            }
        return foreGroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermission(){
        if(foregroundAndBackgroundLocationPermissionApproved())
            return

        var permissionArray = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val requestCode = when {
            runningQOrLater -> {
                 permissionArray += android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                     REQUEST_FOREGROUND_AND_BACKGROUND_LOCATION_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSION_REQUEST_CODE
        }

        requestPermissions(permissionArray,requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if(grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_LOCATION_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            binding?.let {
                Snackbar.make(
                    it.root,
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.settings){
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package",com.udacity.project4.BuildConfig.APPLICATION_ID,null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
            }
        }else{
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    private val resolutionForResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->



        if (activityResult.resultCode== Activity.RESULT_OK){
            addGeoFence()
        }else{
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }


    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val settings = LocationServices.getSettingsClient(requireActivity())
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val locationSettingsResponse = settings.checkLocationSettings(builder.build())

        locationSettingsResponse.addOnFailureListener {exception ->
            if (exception is ResolvableApiException && resolve) {
                try {

                    val intentSenderRequest = IntentSenderRequest
                        .Builder(exception.resolution).build()

                    resolutionForResult.launch(intentSenderRequest)

                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("TAG", "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                binding?.let {
                    Snackbar.make(
                        it.root,
                        R.string.location_required_error, Snackbar.LENGTH_LONG
                    ).setAction(android.R.string.ok) {
                        checkDeviceLocationSettingsAndStartGeofence()
                    }.show()
                }
            }

        }

        locationSettingsResponse.addOnCompleteListener {
            Log.d("isSuccessful", it.isSuccessful.toString())
            if (it.isSuccessful) {


                addGeoFence()
            }
        }
    }





    @SuppressLint("MissingPermission")
    private fun addGeoFence() {
        if (userReminderData!=null){

            val geo = Geofence.Builder()
                .setRequestId(userReminderData!!.id)
                .setCircularRegion(
                    userReminderData!!.latitude!!,
                    userReminderData!!.longitude!!,
                       Constants.GEOFENCE_RADIUS_IN_METERS
                )

                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()



            geofencingClient?.addGeofences(getGeofencingRequest(geo),
                geofencePendingIntent)?.run {
                addOnSuccessListener {

                    _viewModel.validateAndSaveReminder(userReminderData!!)
                }
                addOnFailureListener {
                    Toast.makeText(
                        context,
                        "Error !!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getGeofencingRequest(geo: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geo)
        }.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
