package com.udacity.project4.locationreminders.savereminder


import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin



@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

        private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var  fakeDataSource: FakeDataSource



    @Before
    fun setUpViewModel(){
  // Before test set up my fake data
        stopKoin()
        // stop koin
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(),fakeDataSource)

    }



    @Test
    fun check_loading()= mainCoroutineRule.runBlockingTest {
        //Pause
        mainCoroutineRule.pauseDispatcher()
        // enter my fake data
        val data = ReminderDataItem(
            "Test",
            "Description",
            "location",
            50.5,
            42.5)

        saveReminderViewModel.saveReminder(data)
        //test show loading in pause
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        //Resume
        mainCoroutineRule.resumeDispatcher()
        //test show loading after resume
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    // test save not correct data like null title and desc to show error message
    @Test
    fun shouldReturnError()= mainCoroutineRule.runBlockingTest {
        //save data to check it
        val data = ReminderDataItem(
            null,
            null,
            "location",
            50.5,
            42.5
        )
        val validEnteredData = saveReminderViewModel.validateEnteredData(data)
        //Test
        assertThat(validEnteredData, `is`(false))
       //Test snack bar:
        MatcherAssert.assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }


}