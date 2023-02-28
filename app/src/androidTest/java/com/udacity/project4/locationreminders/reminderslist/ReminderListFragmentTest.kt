package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito
import org.hamcrest.BaseDescription



@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: AutoCloseKoinTest() {

    private lateinit var app:Application
    private lateinit var reminderDataSource:ReminderDataSource


    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun init() {
        //STOP CURRENT KOIN
        stopKoin()
        app = ApplicationProvider.getApplicationContext()

        // initialize all as singletons using Koin
        val currentModule = module {
            viewModel{
                RemindersListViewModel(
                    app,
                    get() as ReminderDataSource
                )
            }
            single { SaveReminderViewModel(app, get() as ReminderDataSource) }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(app) }
        }

        //START NEW KOIN
        startKoin {
            modules(listOf(currentModule))
        }

       reminderDataSource = get()

        //DELETE ALL
        runBlocking {
            reminderDataSource.deleteAllReminders()
        }
    }


    @Test
    fun testNavigationFromFabButton(){
        // Create my fragment scenario
        val fragmentScenario = launchFragmentInContainer <ReminderListFragment>(Bundle(),R.style.AppTheme)

        val navController = Mockito.mock(NavController::class.java)

        fragmentScenario.onFragment {
            Navigation.setViewNavController(it.view!!,navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // test navigation
        Mockito.verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )

    }


    @Test
    fun addReminders_testNavigateAndRecyclerView():Unit = runBlocking {
        // enter data
        val data = ReminderDTO("title1","description","location",20.0,30.0)
        val data2 = ReminderDTO("title2","description2","location2",25.0,35.0)

        // save data
        reminderDataSource.saveReminder(data)
        reminderDataSource.saveReminder(data2)

        launchFragmentInContainer <ReminderListFragment>(Bundle(),R.style.AppTheme)

        // check if the data entered correctly and user can see  it in recycler view
        onView(withId(R.id.reminderssRecyclerView)).perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
            hasDescendant(withText(data2.title))
        ))
    }

    @Test
    fun addReminders_andDeleteThem_testNoDataInUi():Unit = runBlocking {
        // enter data
        val data = ReminderDTO("title1","description","location",20.0,30.0)

        // save the data
        reminderDataSource.saveReminder(data)

        // delete all data
        reminderDataSource.deleteAllReminders()

        launchFragmentInContainer <ReminderListFragment>(Bundle(),R.style.AppTheme)

        // test that all reminders has been deleted and noDataTextView is displaying on the screen
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

    }

}