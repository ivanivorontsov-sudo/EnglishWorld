package org.ivanivorontsov.englishworld.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("englishworld_progress", Context.MODE_PRIVATE)

    fun completedLessons(): Set<String> = prefs.getStringSet(KEY_DONE, emptySet()).orEmpty().toSet()

    fun markLessonCompleted(id: String) {
        prefs.edit().putStringSet(KEY_DONE, completedLessons() + id).apply()
    }

    fun allCards(): List<ReviewCard> {
        val raw = prefs.getString(KEY_CARDS, null) ?: return StudyEngine.initialCards
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(ReviewCard(
                        id = o.getString("id"), front = o.getString("front"), back = o.getString("back"),
                        dueAt = o.optLong("dueAt"), intervalDays = o.optInt("interval"),
                        ease = o.optDouble("ease", 2.5), repetitions = o.optInt("reps"), lapses = o.optInt("lapses")
                    ))
                }
            }
        }.getOrDefault(StudyEngine.initialCards)
    }

    fun saveCard(card: ReviewCard) {
        val cards = allCards().associateBy { it.id }.toMutableMap()
        cards[card.id] = card
        val array = JSONArray()
        cards.values.forEach { c ->
            array.put(JSONObject()
                .put("id", c.id).put("front", c.front).put("back", c.back).put("dueAt", c.dueAt)
                .put("interval", c.intervalDays).put("ease", c.ease).put("reps", c.repetitions).put("lapses", c.lapses))
        }
        prefs.edit().putString(KEY_CARDS, array.toString()).apply()
    }

    private companion object {
        const val KEY_DONE = "completed_lessons"
        const val KEY_CARDS = "review_cards"
    }
}
