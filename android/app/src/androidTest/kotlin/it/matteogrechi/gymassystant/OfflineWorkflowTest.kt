package it.matteogrechi.gymassystant

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteogrechi.gymassystant.app.GymApplication
import it.matteogrechi.gymassystant.app.MainActivity
import it.matteogrechi.gymassystant.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// GYM-001/004/005/009/010/012/014/017/022/026/027: genuine offline logs, lifecycle and durable state.
@RunWith(AndroidJUnit4::class)
class OfflineWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<GymApplication>()

    @Before fun freshLocalFixture(): Unit = runBlocking {
        app.repository.load()
        app.repository.update {
            val fresh = initialDataset(app.clock.read())
            val plan = fresh.plans.first().let { plan -> plan.copy(dateOverrides = mapOf(fresh.createdDate to plan.sessions.first().id)) }
            fresh.copy(plans = listOf(plan) + fresh.plans.drop(1), schedules = listOf(EffectiveSchedule(fresh.createdDate, plan)))
        }
        Unit
    }

    @Test fun sessionLogsPersistAndExerciseHasNoLifecycleActions() {
        compose.onNodeWithText("View session").performClick()
        compose.onNodeWithContentDescription("Start session").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithContentDescription("Set 1 reps").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Leg press").assertIsDisplayed()
        compose.onNodeWithText("Finish").assertDoesNotExist()
        compose.onNodeWithText("Set 1 reps", substring = false).assertDoesNotExist()
        compose.onNodeWithContentDescription("Set 1 reps").performTextInput("10")
        compose.onNodeWithContentDescription("Set 1 weight in kg").performTextInput("20.5")
        compose.waitUntil(15_000) {
            val set = app.repository.state.value?.occurrences?.singleOrNull()?.exercises?.first()?.sets?.first()
            set?.reps == 10 && set.kg?.toPlainString() == "20.5"
        }
        compose.onNodeWithContentDescription("Mark set 1 done").assertIsEnabled().performClick()
        compose.waitUntil(10_000) {
            app.repository.state.value?.occurrences?.singleOrNull()?.exercises?.first()?.sets?.first()?.done == true
        }
        var saved = runBlocking { app.repository.stored() }.let { decodeDataset(it.json) }
        val entry = saved.occurrences.single().exercises.first()
        assertEquals(10, entry.sets.first().reps)
        assertEquals("20.5", entry.sets.first().kg?.toPlainString())
        assertNotNull(saved.occurrences.single().firstVisit)
        assertEquals(1, Metrics.performance(saved, ComparisonContext(entry.definition.id, entry.setup), entry.sets.first().kg,
            saved.createdDate, saved.createdDate).single().completedWorkingSets)
        compose.onNodeWithText("Warm-up").performClick()
        compose.waitUntil(10_000) { app.repository.state.value!!.occurrences.single().exercises.first().sets.size == 4 }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Leg press").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mark set 1 not done").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Pause").performClick()
        compose.waitUntil(15_000) { app.repository.state.value!!.occurrences.single().lifecycle == Lifecycle.PAUSED }
        compose.onNodeWithText("Finish").performClick()
        compose.waitUntil(15_000) { runCatching { compose.onNodeWithText("Finish this session?").assertIsDisplayed() }.isSuccess }
        compose.onNodeWithText("Finish this session?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithContentDescription("Resume session").assertIsDisplayed()
        compose.onNodeWithText("Finish").performClick()
        compose.onAllNodesWithText("Finish").onLast().performClick()
        compose.waitUntil(10_000) { app.repository.state.value!!.occurrences.single().lifecycle == Lifecycle.FINISHED }
        saved = runBlocking { app.repository.stored() }.let { decodeDataset(it.json) }
        assertEquals(1, workingDoneForTest(saved.occurrences.single()))
        assertFalse(saved.occurrences.single().exercises.first().executionTimer.running)
    }

    @Test fun planBrowsingDoesNotActivateAndSettingsKeepsThreeDestinations() {
        val active = app.repository.state.value!!.activePlanId
        compose.onNodeWithText("Plan", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Two-day routine").performClick()
        assertEquals(active, app.repository.state.value!!.activePlanId)
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("App language").assertIsDisplayed()
        compose.onNodeWithText("Download status · JSON").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Two-day routine").assertExists()
        assertEquals(active, app.repository.state.value!!.activePlanId)
    }

    @Test fun acceptedEditSurvivesCallerCancellation(): Unit = runBlocking {
        val edit = launch(Dispatchers.IO) {
            val caller = requireNotNull(currentCoroutineContext()[Job])
            app.repository.update { current ->
                caller.cancel()
                current.copy(settings = current.settings.copy(theme = Theme.DARK))
            }
        }
        edit.join()
        val stored = decodeDataset(app.repository.stored().json)
        assertEquals(Theme.DARK, stored.settings.theme)
        assertEquals(stored, app.repository.state.value)
    }

    private fun workingDoneForTest(occurrence: Occurrence) = occurrence.exercises.sumOf { entry ->
        entry.sets.count { it.kind == SetKind.WORKING && it.done }
    }
}
