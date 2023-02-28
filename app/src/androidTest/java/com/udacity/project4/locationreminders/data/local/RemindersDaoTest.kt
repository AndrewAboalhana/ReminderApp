package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var remindersDatabase:RemindersDatabase


    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun initDatabase(){
    //initialize the Database
    //Database will be killed or removed after the test
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @Test
    fun save_fake_reminder_test_getById() = runBlockingTest{

        // save fake data
        val data = ReminderDTO("title","description","location",50.0,70.0)
        remindersDatabase.reminderDao().saveReminder(data)

        val getReminder = remindersDatabase.reminderDao().getReminderById(data.id)

        // test that the data saved in the DB is the same to the fake data tha i entered
        assertThat(getReminder?.id, `is`(data.id))
        assertThat(getReminder?.title, `is`(data.title))
        assertThat(getReminder?.description, `is`(data.description))
        assertThat(getReminder?.location, `is`(data.location))
        assertThat(getReminder?.latitude, `is`(data.latitude))
        assertThat(getReminder?.longitude, `is`(data.longitude))

        assertThat(getReminder as ReminderDTO,CoreMatchers.notNullValue())

    }

    @Test
    fun testGetRemindersFromDatabase()= runBlockingTest {
        // save fake reminders
        val data = ReminderDTO("title","description","location",50.0,70.0)
        val data1 = ReminderDTO("title1","description1","location1",55.0,75.0)
        val data2 = ReminderDTO("title2","description2","location2",60.0,80.0)

        //save data from Database
        remindersDatabase.reminderDao().saveReminder(data)
        remindersDatabase.reminderDao().saveReminder(data1)
        remindersDatabase.reminderDao().saveReminder(data2)

        val getReminders = remindersDatabase.reminderDao().getReminders()

        //test that getReminders is not null
        assertThat(getReminders, `is`(CoreMatchers.notNullValue()))
    }

    @Test
    fun insertReminders_checkDeleteThem() = runBlockingTest {

        //Enter fake reminders
        val data = ReminderDTO("title","description","location",50.0,70.0)
        val data1 = ReminderDTO("title1","description1","location1",55.0,75.0)
        val data2 = ReminderDTO("title2","description2","location2",60.0,80.0)
        //Save reminders
        remindersDatabase.reminderDao().saveReminder(data)
        remindersDatabase.reminderDao().saveReminder(data1)
        remindersDatabase.reminderDao().saveReminder(data2)

        // delete the reminders
        remindersDatabase.reminderDao().deleteAllReminders()


        val getReminders = remindersDatabase.reminderDao().getReminders()

        //Test that is emptyList
        assertThat(getReminders, `is`(emptyList()))

    }

    @After
    //Close the Database after the test end
    fun closeDatabase()= remindersDatabase.close()
}