package com.udacity.project4.locationreminders.reminderslist


import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import org.hamcrest.MatcherAssert.assertThat
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersListViewModel:RemindersListViewModel
    private lateinit var fakeDataSource: FakeDataSource


    @Before
    fun setUpViewModel(){
// Before test set up my fake data
        // stop the koin before test
        stopKoin()
        fakeDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(),fakeDataSource)

    }


    // this method will check if the exception message is loaded
    // in to the snack bar and will the exception will be handled
    // correctly and I can show it to the user
    @Test
    fun shouldReturnError() = mainCoroutineRule.runBlockingTest {

        fakeDataSource.setReturnError(true)

        // load method
        remindersListViewModel.loadReminders()

        //test
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception"))
    }



    // in this method I am testing load reminders so at the real load reminders I have Boolean showLoading in the start it will be true and after the coroutine star and get my reminders it will be false

    // this test have to wait to the real coroutines that's why I'm  pausing and resuming the dispatcher

    // so I will test if I entered the view model scope and get reminders data correctly or not but I am keeping on mind that I have to wait for the correct value so I'm using pause/resume dispatcher and getOrQAwaitValue
    @Test
    fun check_loading() =mainCoroutineRule.runBlockingTest {
    //pause
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
    //test
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue (), `is`(true))
    //resumed
        mainCoroutineRule.resumeDispatcher()
    //test
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }



}