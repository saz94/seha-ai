package ai.seha.app.data

import java.time.LocalDate

/** Sample data, shown only when the screens are photographed during the build. */
object Demo {
    fun data(): AppData {
        val today = LocalDate.now()
        fun day(back: Int) = today.minusDays(back.toLong()).toString()
        val weights = listOf(84.6, 84.4, 84.5, 84.1, 83.9, 83.9, 83.6, 83.4, 83.5, 83.1, 82.9, 82.8, 82.6, 82.4)
            .mapIndexed { i, kg -> WeightEntry(day(13 - i), kg) }
        val checkins = (1..6).map { back ->
            CheckIn(
                date = day(back), mood = 3 + back % 3, energy = 2 + back % 3, sleepHours = 6.0 + back % 3 * 0.75,
                workedOut = back % 2 == 0, workoutType = if (back % 2 == 0) tr("Weights", "أوزان") else "", workoutMinutes = if (back % 2 == 0) 50 else 0,
                ateOnPlan = back % 3,
            )
        } + CheckIn(
            date = day(0), mood = 4, energy = 4, sleepHours = 7.5, workedOut = true, workoutType = tr("Weights, upper body", "أوزان، الجزء العلوي"),
            workoutMinutes = 55, ateOnPlan = 2,
            coachNote = tr(
                "Strong day: training done, sleep above 7 hours and food on plan. Tomorrow, keep protein near 150 g and walk 8,000 steps to stay on pace.",
                "يوم قوي: أنجزت التمرين، ونمت أكثر من 7 ساعات، والتزمت بالأكل. غداً حافظ على البروتين قرب 150 غ وامشِ 8000 خطوة.",
            ),
        )
        val meals = listOf(
            Meal(
                "m1", "breakfast", tr("Shakshuka with labneh and whole-wheat pita", "شكشوكة مع لبنة وخبز أسمر"), 480, 32, 42, 19, 20,
                listOf(tr("3 eggs", "3 بيضات"), tr("200 g chopped tomatoes", "200 غ طماطم مفرومة"), tr("60 g labneh", "60 غ لبنة"), tr("1 small whole-wheat pita", "رغيف أسمر صغير")),
                listOf(
                    tr("Soften the onion and pepper in the oil for 5 minutes.", "قلّب البصل والفلفل في الزيت 5 دقائق."),
                    tr("Add tomatoes and spices, simmer 8 minutes.", "أضف الطماطم والبهارات واتركها 8 دقائق."),
                    tr("Make three wells, crack in the eggs, cover and cook 5 minutes.", "اصنع ثلاث حفر واكسر البيض، غطِّ واطبخ 5 دقائق."),
                ),
                eaten = true,
            ),
            Meal(
                "m2", "lunch", tr("Grilled chicken machboos bowl", "طبق مجبوس دجاج مشوي"), 690, 58, 74, 16, 40,
                listOf(tr("200 g chicken breast", "200 غ صدر دجاج"), tr("80 g basmati rice (dry)", "80 غ أرز بسمتي (جاف)"), tr("1 dried lime", "حبة لومي"), tr("1 tsp bezar spice mix", "ملعقة صغيرة بزار")),
                listOf(
                    tr("Rub the chicken with bezar, salt and a little oil; grill 6 minutes a side.", "تبّل الدجاج بالبزار والملح وقليل من الزيت واشوِه 6 دقائق لكل جانب."),
                    tr("Cook the rice with the dried lime and onion until fluffy.", "اطبخ الأرز مع اللومي والبصل حتى ينضج."),
                    tr("Slice the chicken over the rice and serve with cucumber yoghurt.", "قطّع الدجاج فوق الأرز وقدّمه مع روب بالخيار."),
                ),
                source = "https://www.example.com/machboos", sourceName = "example.com",
            ),
            Meal("m3", "snack", tr("Greek yoghurt, dates and walnuts", "زبادي يوناني مع تمر وجوز"), 260, 18, 28, 9, 3),
            Meal("m4", "dinner", tr("Baked hammour with roasted vegetables", "هامور بالفرن مع خضار مشوية"), 560, 46, 38, 22, 35),
        )
        return AppData(
            profile = Profile(
                onboarded = true, name = tr("Saud", "سعود"), sex = Sex.MALE, birthYear = today.year - 31, heightCm = 176.0,
                startWeightKg = 84.6, targetWeightKg = 78.0, goal = Goal.LOSE, activity = ActivityLevel.LIGHT, workoutsPerWeek = 4,
                allergies = tr("None", "لا يوجد"), dislikes = tr("tomato skins", "قشر الطماطم"),
            ),
            checkins = checkins.sortedBy { it.date },
            weights = weights,
            measurements = listOf(
                Measurement(day(13), waist = 94.0, chest = 103.0, arm = 35.5, bodyFat = 24.0),
                Measurement(day(1), waist = 91.5, chest = 103.5, arm = 36.0, bodyFat = 22.5),
            ),
            plans = listOf(DayPlan(day(0), meals), DayPlan(today.plusDays(1).toString(), meals.map { it.copy(eaten = false) })),
            chat = listOf(
                ChatMsg(true, tr("I only have 30 minutes today. What should I train?", "عندي 30 دقيقة فقط اليوم. ماذا أتمرّن؟")),
                ChatMsg(
                    false,
                    tr(
                        "Do a full-body circuit, 3 rounds: goblet squats x12, push-ups x12, dumbbell rows x12 each side, plank 40 s. Rest 60 s between rounds. You trained upper body yesterday, so keep the rows lighter.",
                        "نفّذ دائرة للجسم كله، 3 جولات: سكوات بالدمبل ×12، ضغط ×12، سحب بالدمبل ×12 لكل جانب، بلانك 40 ثانية. استرح 60 ثانية بين الجولات.",
                    ),
                ),
            ),
            water = mapOf(day(0) to 1750, day(1) to 2500, day(2) to 2250, day(3) to 3000, day(4) to 1500, day(5) to 2750, day(6) to 2500),
            waterLog = listOf(500, 250, 250, 500, 250).mapIndexed { i, ml -> WaterEntry(day(0), System.currentTimeMillis() - (5 - i) * 5_400_000L, ml) },
            health = (0..6).map { HealthDay(day(it), steps = 5200L + it * 830, sleepMinutes = 400L + it * 12, activeKcal = 380 + it * 20, exerciseMinutes = 30L + it * 5, restingHr = 61) },
            tip = tr(
                "You are down 2.2 kg in two weeks, right on pace. Today is a rest day: hit your protein and get 8,000 steps.",
                "نزلت 2.2 كغ في أسبوعين، وهذا ضمن الخطة. اليوم راحة: أكمل البروتين وامشِ 8000 خطوة.",
            ),
            tipDate = day(0),
            settings = Settings(healthConnect = true),
        )
    }
}
