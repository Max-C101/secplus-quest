package com.secplusquest.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Home-screen widget: streak, today's XP, review cards due, hearts, exam countdown. */
public class StatsWidget extends AppWidgetProvider {

    static final String PREFS = "secplus_widget";
    static final String KEY = "state";
    private static final long DAY = 86_400_000L, REGEN = 2 * 3_600_000L;
    private static final int MAX_HEARTS = 5;

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(ctx));
    }

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, StatsWidget.class));
        if (ids.length == 0) return;
        RemoteViews rv = build(ctx);
        for (int id : ids) mgr.updateAppWidget(id, rv);
    }

    private static String dayKey(Calendar c) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private static RemoteViews build(Context ctx) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_stats);

        int streak = 0, todayXP = 0, goal = 30, due = 0, hearts = MAX_HEARTS, done = 0, total = 21;
        long days = Long.MIN_VALUE;
        boolean hasData = false;

        String raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (raw != null) {
            try {
                JSONObject o = new JSONObject(raw);
                hasData = true;
                long now = System.currentTimeMillis();
                Calendar cal = Calendar.getInstance();
                String today = dayKey(cal);
                cal.add(Calendar.DAY_OF_MONTH, -1);
                String yday = dayKey(cal);

                String last = o.optString("lastGoalDay", "");
                if (last.equals(today) || last.equals(yday)) streak = o.optInt("streak", 0);
                if (today.equals(o.optString("todayKey", ""))) todayXP = o.optInt("todayXP", 0);
                goal = Math.max(1, o.optInt("goal", 30));

                JSONArray dues = o.optJSONArray("dues");
                if (dues != null) for (int i = 0; i < dues.length(); i++) if (dues.optLong(i, Long.MAX_VALUE) <= now) due++;

                hearts = o.optInt("hearts", MAX_HEARTS);
                if (hearts < MAX_HEARTS) {
                    long gained = (now - o.optLong("heartsAt", now)) / REGEN;
                    hearts = (int) Math.min(MAX_HEARTS, hearts + Math.max(0, gained));
                }
                done = o.optInt("lessonsDone", 0);
                total = o.optInt("lessons", 21);

                String exam = o.optString("examDate", "");
                if (exam.length() == 10) {
                    Calendar e = Calendar.getInstance();
                    e.set(Integer.parseInt(exam.substring(0, 4)), Integer.parseInt(exam.substring(5, 7)) - 1,
                            Integer.parseInt(exam.substring(8, 10)), 0, 0, 0);
                    Calendar t = Calendar.getInstance();
                    t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 0); t.set(Calendar.SECOND, 0);
                    days = Math.round((e.getTimeInMillis() - t.getTimeInMillis()) / (double) DAY);
                }
            } catch (Exception ignored) { }
        }

        rv.setTextViewText(R.id.w_streak, "🔥 " + streak);
        rv.setTextViewText(R.id.w_hearts, "❤️ " + hearts);
        rv.setTextViewText(R.id.w_xp, Math.min(todayXP, goal) + " / " + goal + " XP today");
        rv.setProgressBar(R.id.w_progress, goal, Math.min(todayXP, goal), false);
        rv.setTextViewText(R.id.w_due, due == 1 ? "1 card due" : due + " cards due");
        rv.setTextViewText(R.id.w_exam,
                days == Long.MIN_VALUE ? done + "/" + total + " lessons"
                        : days >= 0 ? days + " days to exam" : "Exam date passed");

        String msg;
        if (!hasData) msg = "Tap to start your first lesson";
        else if (todayXP >= goal) msg = "Daily goal done! 🏆";
        else if (streak > 0) msg = "Keep your " + streak + "-day streak alive";
        else if (due > 0) msg = "Your review cards are ready";
        else msg = "Start a lesson to begin a streak";
        rv.setTextViewText(R.id.w_msg, msg);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent open = new Intent(ctx, MainActivity.class);
        rv.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(ctx, 1, open, flags));

        Intent review = new Intent(ctx, MainActivity.class).putExtra(MainActivity.EXTRA_REVIEW, true);
        rv.setOnClickPendingIntent(R.id.w_practice, PendingIntent.getActivity(ctx, 2, review, flags));
        return rv;
    }
}
