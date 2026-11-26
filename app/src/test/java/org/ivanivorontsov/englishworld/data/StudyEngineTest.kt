package org.ivanivorontsov.englishworld.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyEngineTest {
    @Test fun curriculumHasEightLessonsAtEveryLevel() {
        CefrLevel.entries.forEach { level -> assertEquals(8, Curriculum.lessons.count { it.level == level }) }
    }
    @Test fun successfulRecallIncreasesTheInterval() {
        val now = 1_000_000L
        val first = StudyEngine.schedule(StudyEngine.initialCards.first(), ReviewGrade.GOOD, now)
        assertEquals(1, first.intervalDays)
        val second = StudyEngine.schedule(first, ReviewGrade.GOOD, now)
        assertEquals(6, second.intervalDays)
        val third = StudyEngine.schedule(second, ReviewGrade.GOOD, now)
        assertTrue(third.intervalDays > second.intervalDays)
    }
    @Test fun failedRecallSchedulesQuickRetryAndResetsRepetitions() {
        val now = 1_000_000L
        val card = StudyEngine.initialCards.first().copy(intervalDays = 12, repetitions = 4)
        val next = StudyEngine.schedule(card, ReviewGrade.AGAIN, now)
        assertEquals(0, next.repetitions)
        assertEquals(0, next.intervalDays)
        assertEquals(now + 10 * 60_000L, next.dueAt)
        assertEquals(1, next.lapses)
    }
    @Test fun b2ReadinessRequiresAllFiveSkills() {
        val scores = mapOf("range" to 4, "accuracy" to 4, "coherence" to 5, "pronunciation" to 4, "interaction" to 4)
        assertTrue(StudyEngine.isB2SkillReady(scores))
        assertFalse(StudyEngine.isB2SkillReady(scores + ("pronunciation" to 3)))
    }
}
