package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
//Repo
class FakeDataSource : ReminderDataSource {


    private val reminderDTO = mutableListOf<ReminderDTO>()
    private var returnError=false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        if (returnError){
            //this will return task exception so Here I will try to assume
            // that this get reminder throw exception and I am trying
            // to handle this exception
            return Result.Error("Test exception")
        }
        return try {
            reminderDTO.let {
                return Result.Success(it)

            }
        }catch (e:Exception){
            Result.Error(e.localizedMessage)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderDTO.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (returnError) {
            //this will return task exception so Here I will try to assume
            // that this get reminder throw exception and I am trying
            // to handle this exception
            return Result.Error("Test exception")

        }

      return try {
          val data = reminderDTO.find { it.id== id }
       // return result.success(data)
       // here there is data for this is success
          if (data!= null){
              Result.Success(data)
          }else{
              // or don't have
             Result.Error("Reminder not found!")
          }

      }catch (e:Exception) {
          return Result.Error(e.localizedMessage)
      }

    }

    override suspend fun deleteAllReminders() {
        // clear Fake data
        reminderDTO.clear()
    }

    fun setReturnError(value:Boolean){
       returnError=value
    }


}