package org.ivanivorontsov.englishworld.data

import kotlin.math.roundToInt

enum class CefrLevel(val label: String) {
    A1("A1 · Beginner"), A2("A2 · Elementary"), B1("B1 · Intermediate"), B2("B2 · Upper-intermediate")
}
enum class Strand(val title: String, val description: String) {
    INPUT("Восприятие", "Слушать и читать понятный английский по смыслу."),
    OUTPUT("Активная речь", "Отвечать, задавать вопросы и формулировать мысли."),
    LANGUAGE("Работа над языком", "Разбирать лексику, грамматику и произношение."),
    FLUENCY("Беглость", "Говорить свободнее, повторяя реальные задания.")
}
data class Lesson(
    val id: String, val level: CefrLevel, val title: String, val focus: String,
    val goal: String, val speakingTask: String, val phrases: List<String> = emptyList()
)

object Curriculum {
    val strands = Strand.entries.toList()
    val lessons = listOf(
        Lesson("a1-01", CefrLevel.A1, "Meet and introduce yourself", "Verb to be · introductions", "Представиться и задать простые вопросы.", "Introduce yourself to a new classmate. Tell them where you live, what you do and what you enjoy.", listOf("Nice to meet you", "I am interested in…", "Where are you from?")),
        Lesson("a1-02", CefrLevel.A1, "My everyday life", "Present Simple · routine", "Рассказать о распорядке дня.", "Describe a normal weekday from morning to evening. Ask your partner about their routine.", listOf("I usually get up at…", "I hardly ever…", "What time do you…?")),
        Lesson("a1-03", CefrLevel.A1, "People and places", "Have got · adjectives", "Описать человека, семью и знакомое место.", "Describe a person you admire and a place you like. Use at least five adjectives.", listOf("He looks friendly", "There is…", "It is famous for…")),
        Lesson("a1-04", CefrLevel.A1, "Food and ordering", "Countable nouns · polite requests", "Заказать еду и вежливо попросить помощь.", "Role-play: order a meal in a café. Ask about ingredients and the price.", listOf("Could I have…?", "I'd like…", "Is it possible to…?")),
        Lesson("a1-05", CefrLevel.A1, "Free time", "Can / can't · like + -ing", "Говорить об умениях и интересах.", "Talk about three things you can do, two things you want to learn and your hobbies.", listOf("I'm keen on…", "I'm good at…", "I'd love to try…")),
        Lesson("a1-06", CefrLevel.A1, "Around town", "Prepositions · directions", "Спрашивать дорогу и понимать указания.", "Ask for directions to a station, a pharmacy and a museum. Check that you understood.", listOf("How do I get to…?", "Turn left at…", "It's opposite…")),
        Lesson("a1-07", CefrLevel.A1, "Yesterday and last weekend", "Past Simple · regular verbs", "Сообщать о событиях в прошлом.", "Describe what you did last weekend. Say where you went and who you met.", listOf("Last weekend I…", "I went to…", "It was really…")),
        Lesson("a1-08", CefrLevel.A1, "Plans for tomorrow", "Going to · time expressions", "Рассказывать о ближайших планах.", "Explain your plans for tomorrow and the weekend. Ask a friend to join you.", listOf("I'm going to…", "Are you free on…?", "Why don't we…?")),

        Lesson("a2-01", CefrLevel.A2, "Tell a proper story", "Past Simple vs Past Continuous", "Строить рассказ с фоном и последовательностью событий.", "Tell a story about a day when something unexpected happened. Use when, while and suddenly.", listOf("I was… when…", "All of a sudden…", "In the end…")),
        Lesson("a2-02", CefrLevel.A2, "Experiences so far", "Present Perfect · ever / never / yet", "Говорить об опыте и результатах.", "Compare experiences: places you have visited, skills you have learned and things you haven't tried yet.", listOf("Have you ever…?", "I've never…", "I haven't… yet")),
        Lesson("a2-03", CefrLevel.A2, "Travel problems", "Requests · past forms · modals", "Решать типовые проблемы в поездке.", "Role-play a delayed flight, a wrong hotel booking and lost luggage. Stay polite but clear.", listOf("I'm afraid there's been a mistake", "Would you mind…?", "Could you possibly…?")),
        Lesson("a2-04", CefrLevel.A2, "Health and advice", "Should / must / have to", "Объяснять бытовую проблему и давать совет.", "Describe an everyday problem and ask for advice. Then give advice to your partner.", listOf("You should…", "You don't have to…", "If I were you…")),
        Lesson("a2-05", CefrLevel.A2, "Comparing choices", "Comparatives · superlatives", "Сравнивать варианты и объяснять выбор.", "Choose between two phones, courses or holiday destinations. Compare the advantages.", listOf("Compared with…", "The best option is…", "It's much more…")),
        Lesson("a2-06", CefrLevel.A2, "Making arrangements", "Future forms · suggestions", "Договариваться о встрече и менять планы.", "Arrange a weekend activity with a friend. Suggest options, disagree politely and agree on a plan.", listOf("How about…?", "That sounds good, but…", "Shall we…?")),
        Lesson("a2-07", CefrLevel.A2, "Shopping and services", "Too / enough · quantifiers", "Решать проблему с покупкой или услугой.", "Explain that an item is the wrong size or faulty. Ask for an exchange or refund.", listOf("It's too small", "It's not good enough", "I'd like to return this")),
        Lesson("a2-08", CefrLevel.A2, "How things work", "Sequencing · first / then / finally", "Объяснять знакомый процесс по шагам.", "Explain how to use an app, cook a simple meal or solve a familiar technical problem.", listOf("First of all…", "The next step is…", "Once you've done that…")),

        Lesson("b1-01", CefrLevel.B1, "A story worth telling", "Narrative tenses · sequencing", "Рассказывать связные истории и выделять детали.", "Tell a two-minute story about a decision, surprise or mistake that taught you something.", listOf("What happened was…", "Looking back…", "The main thing I learned was…")),
        Lesson("b1-02", CefrLevel.B1, "Give your point of view", "Opinion phrases · linking words", "Выражать мнение и обосновывать его примерами.", "Discuss whether online learning is as effective as classroom learning. Give reasons and an example.", listOf("From my point of view…", "One reason for this is…", "Having said that…")),
        Lesson("b1-03", CefrLevel.B1, "What if…?", "First and Second Conditionals", "Обсуждать реальные и гипотетические ситуации.", "Imagine you could redesign your town or change one rule at work. Explain what you would do and why.", listOf("If I had the chance…", "Unless…", "That would mean…")),
        Lesson("b1-04", CefrLevel.B1, "Work and study", "Relative clauses · useful vocabulary", "Подробно описывать учёбу, работу и навыки.", "Describe a project you are proud of, the people involved and the skills you used.", listOf("The reason why…", "What I found challenging was…", "I was responsible for…")),
        Lesson("b1-05", CefrLevel.B1, "News and technology", "Passive voice · reporting verbs", "Обсуждать новости и объяснять последствия.", "Choose a technology trend. Explain how it works, who benefits and what concerns you.", listOf("It is believed that…", "According to…", "This could lead to…")),
        Lesson("b1-06", CefrLevel.B1, "Agreeing and disagreeing", "Polite disagreement · discourse markers", "Поддерживать дискуссию и уточнять чужую позицию.", "Discuss whether smartphones should be restricted at school. Challenge one argument politely.", listOf("I see your point, but…", "Could you clarify what you mean?", "I partly agree…")),
        Lesson("b1-07", CefrLevel.B1, "Describe a film or book", "Narrative vocabulary · speculation", "Пересказывать сюжет и объяснять впечатление.", "Recommend a film, series or book. Describe the story without spoilers and explain who might enjoy it.", listOf("What makes it interesting is…", "The story revolves around…", "I'd recommend it to…")),
        Lesson("b1-08", CefrLevel.B1, "Solve a real-life problem", "Problem solving · negotiation", "Обсуждать проблему и приходить к решению.", "You have a limited budget and several urgent tasks. Explain your priorities and negotiate a plan.", listOf("The main issue is…", "A possible solution would be…", "We need to take into account…")),

        Lesson("b2-01", CefrLevel.B2, "Build a convincing argument", "Thesis · evidence · counterargument", "Аргументировать и учитывать противоположную позицию.", "Should artificial intelligence be used in education? Give a structured argument, an example and a counterargument.", listOf("It could be argued that…", "A major consideration is…", "Nevertheless…")),
        Lesson("b2-02", CefrLevel.B2, "Nuance and diplomacy", "Hedging · stance · degree", "Выражать осторожное мнение и степень уверенности.", "Discuss a controversial but familiar issue without sounding too absolute. Qualify at least three claims.", listOf("To some extent…", "It seems reasonable to assume…", "I wouldn't go so far as to say…")),
        Lesson("b2-03", CefrLevel.B2, "Advanced hypothetical thinking", "Mixed conditionals · inversion", "Рассуждать о последствиях сложных сценариев.", "Explain how a project or your life might be different if one important decision had gone another way.", listOf("Had I known…", "If it hadn't been for…", "That would have resulted in…")),
        Lesson("b2-04", CefrLevel.B2, "Sound natural and precise", "Collocations · phrasal verbs · register", "Использовать устойчивые сочетания и подходящий стиль.", "Explain the same idea to a close friend, a manager and a public audience. Adapt your register.", listOf("It turns out that…", "To put it another way…", "The issue boils down to…")),
        Lesson("b2-05", CefrLevel.B2, "Discuss evidence and uncertainty", "Reporting · passive · cautious claims", "Описывать данные, источники и ограничения выводов.", "Present a claim about a scientific or social topic. Separate evidence, interpretation and uncertainty.", listOf("The findings suggest that…", "This does not necessarily mean…", "Further evidence would be needed…")),
        Lesson("b2-06", CefrLevel.B2, "Lead a discussion", "Turn-taking · clarification", "Участвовать в дискуссии и развивать идеи собеседника.", "Lead a short discussion about remote work. Invite another view, clarify a disagreement and summarise the outcome.", listOf("Could I just add something here?", "Picking up on your point…", "Where we seem to agree is…")),
        Lesson("b2-07", CefrLevel.B2, "Compare options in depth", "Concession · comparison · evaluation", "Сравнивать варианты по критериям и обосновывать выбор.", "Choose the best way to spend a limited public budget. Compare three options, weigh trade-offs and make a recommendation.", listOf("While it is true that…", "The key trade-off is…", "All things considered…")),
        Lesson("b2-08", CefrLevel.B2, "Speak for two minutes", "Extended discourse · coherence", "Выступать развёрнуто, логично и без заученного текста.", "Give a two-minute talk about a change that could improve society. Include an opening, three points and a conclusion.")
    )

    val starterCards = listOf(
        "It depends on the context." to "Это зависит от контекста.",
        "What I mean is…" to "Я имею в виду…",
        "I'm not entirely sure, but…" to "Я не совсем уверен, но…",
        "Could you rephrase that?" to "Не могли бы вы переформулировать?",
        "On the other hand…" to "С другой стороны…",
        "The main advantage is…" to "Главное преимущество заключается в…",
        "I see what you mean." to "Понимаю, что вы имеете в виду.",
        "As far as I know…" to "Насколько мне известно…",
        "It is worth pointing out that…" to "Стоит отметить, что…",
        "To some extent, I agree." to "В некоторой степени я согласен."
    )
}

enum class ReviewGrade(val quality: Int) { AGAIN(1), HARD(3), GOOD(4), EASY(5) }
data class ReviewCard(
    val id: String, val front: String, val back: String, val dueAt: Long,
    val intervalDays: Int = 0, val ease: Double = 2.5, val repetitions: Int = 0, val lapses: Int = 0
)

object StudyEngine {
    const val DAY_MS = 86_400_000L
    val initialCards = Curriculum.starterCards.mapIndexed { i, pair ->
        ReviewCard("starter-$i", pair.first, pair.second, 0L)
    }

    fun schedule(card: ReviewCard, grade: ReviewGrade, now: Long): ReviewCard {
        val q = grade.quality
        val ease = (card.ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceAtLeast(1.3)
        if (q < 3) return card.copy(
            dueAt = now + 10 * 60_000L, intervalDays = 0, ease = ease, repetitions = 0, lapses = card.lapses + 1
        )
        val base = when (card.repetitions) {
            0 -> 1
            1 -> 6
            else -> (card.intervalDays * ease).roundToInt().coerceAtLeast(card.intervalDays + 1)
        }
        val interval = if (q == 3) (base * 0.8).roundToInt().coerceAtLeast(1) else base
        return card.copy(
            dueAt = now + interval * DAY_MS, intervalDays = interval, ease = ease, repetitions = card.repetitions + 1
        )
    }

    fun due(cards: List<ReviewCard>, now: Long): List<ReviewCard> = cards.filter { it.dueAt <= now }.sortedBy { it.dueAt }

    fun isB2SkillReady(scores: Map<String, Int>): Boolean =
        listOf("range", "accuracy", "coherence", "pronunciation", "interaction").all { (scores[it] ?: 0) >= 4 }
}
