package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var remindersLocalRepository:RemindersLocalRepository
    private lateinit var remindersDatabase: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun initDbAndRepo(){
        // initialize the Database
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
        // initialize the Repo
        remindersLocalRepository = RemindersLocalRepository(remindersDatabase.reminderDao(),Dispatchers.Main)
    }

    @Test
    fun test_saveReminder_getById() = runBlocking {

        // enter fake reminder
        val data = ReminderDTO("title","description","location",50.0,70.0)

        // save element using repo
        remindersLocalRepository.saveReminder(data)

        //get reminder using id using repo
        val getReminder = remindersLocalRepository.getReminder(data.id) as? Result.Success

        //Test Success
        assertThat(getReminder is Result.Success, `is`(true))

        //test elements of saved reminder is it the same I did or not
        assertThat(getReminder?.data?.id, `is`(data.id))
        assertThat(getReminder?.data?.title, `is`(data.title))
        assertThat(getReminder?.data?.description, `is`(data.description))
        assertThat(getReminder?.data?.location, `is`(data.location))
        assertThat(getReminder?.data?.latitude, `is`(data.latitude))
        assertThat(getReminder?.data?.longitude, `is`(data.longitude))

    }

   // test save reminder and delete it and get it by id and show error that no reminders found
   @Test
   fun test_saveReminder_deleteIt_andGetById_resultErrorAndPass()= runBlocking {

    // enter fake reminder
       val data = ReminderDTO("title","description","location",50.0,70.0)
    // save reminder
       remindersLocalRepository.saveReminder(data)

    // delete all reminders
       remindersLocalRepository.deleteAllReminders()

    // get reminder by id
       val getReminder = remindersLocalRepository.getReminder(data.id)

    // test that is error
       assertThat(getReminder is Result.Error, `is`(true))
       getReminder as Result.Error

    // test error message in reminderLocalRepository
       assertThat(getReminder.message, `is`("Reminder not found!"))
   }


    // test save reminders and delete them and return empty list
    @Test
    fun testDeleteReminders() = runBlocking {
        // enter fake reminder
        val data = ReminderDTO("title","description","location",50.0,70.0)
        val data1 = ReminderDTO("title1","description1","location1",55.0,75.0)
        val data2 = ReminderDTO("title2","description2","location2",60.0,80.0)
        // save reminders
        remindersLocalRepository.saveReminder(data)
        remindersLocalRepository.saveReminder(data1)
        remindersLocalRepository.saveReminder(data2)

        // delete them
        remindersLocalRepository.deleteAllReminders()

        val getReminders = remindersLocalRepository.getReminders()

        assertThat(getReminders is Result.Success, `is`(true))
        getReminders as Result.Success
        // test that return empty list
        assertThat(getReminders.data, `is`(listOf()))
    }

    @After
    // clear the DB after test
    fun clearDatabase() = remindersDatabase.close()

}