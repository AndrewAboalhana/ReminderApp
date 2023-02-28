package com.udacity.project4

import android.Manifest
import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
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


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest : AutoCloseKoinTest() {
// Extended Koin Test - embed auto close @after method to close Koin after every test

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application


    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource(){
        // idlingRegistry.EspressoIdlingResource.countingIdlingResource
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        // idlingRegistry.dataBindingIdlingResource
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }


    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>):Activity{
        lateinit var activity: Activity
        activityScenario.onActivity {
            activity=it
        }
        return activity
    }



    @Test
    fun testSaveEmptyTitleReminderToShowErrorMessage(){

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        // test show bar message say enter title
        val errorMessage= appContext.getString(R.string.err_enter_title)
        onView(withText(errorMessage)).check(matches(isDisplayed()))

        activityScenario.close()
    }


    @Test
    fun testSaveEmptyLocationReminderToShowErrorMessage(){
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("title"))

        onView(withId(R.id.reminderTitle)).perform(pressImeActionButton())
        onView(withId(R.id.saveReminder)).perform(click())

        // test show snackBar say error select location
        val errorMessage= appContext.getString(R.string.err_select_location)
        onView(withText(errorMessage)).check(matches(isDisplayed()))

        activityScenario.close()

    }

    // full test to check add reminder with correct data
    @Test
    fun testSaveFineReminderData(){
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)


        onView(withId(R.id.addReminderFAB)).perform(click())

        // test add title to reminder
        onView(withId(R.id.reminderTitle)).perform(typeText("title"))
        onView(withId(R.id.reminderTitle)).perform(pressImeActionButton())

        // test add desc to reminder
        onView(withId(R.id.reminderDescription)).perform(typeText("description"))
        onView(withId(R.id.reminderDescription)).perform(pressImeActionButton())

        // test select location
        onView(withId(R.id.selectLocation)).perform(click())

        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.saveBtn)).perform(click())

        onView(withId(R.id.saveReminder)).perform(click())

        // test show toast message that said saved reminder
        onView(withText(R.string.reminder_saved)).inRoot(
            withDecorView(not(`is`(getActivity(activityScenario).window.decorView))))
            .check(matches(isDisplayed()))

//        onView(withText(R.string.reminder_saved)).inRoot(
//            withDecorView(CoreMatchers.not(CoreMatchers.`is`(getActivity(activityScenario).window.decorView)))
//        ).check(matches(isDisplayed()))

        activityScenario.close()
    }




    @After
    fun unRegisterIdlingResource(){
        // unRegister EspressoIdlingResource.countingIdlingResource
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        // unRegister dataBindingIdlingResource
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

}