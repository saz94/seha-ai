package ai.seha.app

import ai.seha.app.data.ActivityLevel
import ai.seha.app.data.Goal
import ai.seha.app.data.Profile
import ai.seha.app.data.Sex
import ai.seha.app.domain.Calc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalcTest {
    private fun profile(goal: Goal, sex: Sex = Sex.MALE, pace: Double = 0.5) = Profile(
        sex = sex, birthYear = LocalDate.now().year - 30, heightCm = 180.0, startWeightKg = 80.0, targetWeightKg = 75.0,
        goal = goal, activity = ActivityLevel.SEDENTARY, paceKgPerWeek = pace,
    )

    @Test
    fun restingEnergyFollowsMifflinStJeor() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 1780
        assertEquals(1780, Calc.targets(profile(Goal.MAINTAIN), 80.0).bmr)
        // the same person as a woman: -161 instead of +5
        assertEquals(1614, Calc.targets(profile(Goal.MAINTAIN, Sex.FEMALE), 80.0).bmr)
    }

    @Test
    fun maintainingEatsWhatIsBurned() {
        val t = Calc.targets(profile(Goal.MAINTAIN), 80.0)
        assertEquals(2136, t.maintenance) // 1780 * 1.2
        assertEquals(2140, t.calories)
    }

    @Test
    fun losingNeverCutsMoreThanAQuarterOrBelowTheFloor() {
        val steady = Calc.targets(profile(Goal.LOSE, pace = 0.5), 80.0)
        assertEquals(1600, steady.calories) // 2136 - min(550, 534) = 1602
        val small = Profile(sex = Sex.FEMALE, birthYear = LocalDate.now().year - 50, heightCm = 150.0, startWeightKg = 50.0, targetWeightKg = 45.0, goal = Goal.LOSE, activity = ActivityLevel.SEDENTARY, paceKgPerWeek = 0.75)
        val t = Calc.targets(small, 50.0)
        assertEquals(1200, t.calories)
        assertTrue(t.floored)
    }

    @Test
    fun macrosAddUpToTheCalories() {
        val t = Calc.targets(profile(Goal.LOSE), 80.0)
        val fromMacros = t.protein * 4 + t.carbs * 4 + t.fat * 9
        assertTrue("macros give $fromMacros of ${t.calories}", kotlin.math.abs(fromMacros - t.calories) <= 12)
    }

    @Test
    fun aKidneyConditionHoldsProteinDown() {
        val t = Calc.targets(profile(Goal.LOSE).copy(conditions = listOf("kidney")), 80.0)
        assertEquals(64, t.protein)
        assertTrue(t.proteinLimited)
    }

    @Test
    fun weeksToGoalUsesThePace() {
        assertEquals(10, Calc.weeksToGoal(profile(Goal.LOSE, pace = 0.5), 80.0))
        assertEquals(null, Calc.weeksToGoal(profile(Goal.MAINTAIN), 80.0))
    }
}
