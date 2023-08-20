package com.jannik_kuehn.loritime.common.utils;

import com.jannik_kuehn.loritime.common.config.localization.Localization;

import java.util.Objects;

public final class TimeUtil {

    public static String formatTime(long seconds, Localization localization) {
        Objects.requireNonNull(localization);
        long sec = seconds;

        long min = sec / 60;
        sec %= 60;

        long h = min / 60;
        min %= 60;

        long d = h / 24;
        h %= 24;

        long m = d / 30;
        d %= 30;

        long y = m / 12;
        m %= 12;

        long w = d / 7;
        d %= 7;

        String secStr =            sec + " " + (sec == 1 ? localization.getRawMessage("unit.second.singular") : localization.getRawMessage("unit.second.plural"))       ;
        String minStr = min != 0 ? min + " " + (min == 1 ? localization.getRawMessage("unit.minute.singular") : localization.getRawMessage("unit.minute.plural")) : null;
        String hStr   = h   != 0 ? h   + " " + (h   == 1 ? localization.getRawMessage("unit.hour.singular")   : localization.getRawMessage("unit.hour.plural"))   : null;
        String dStr   = d   != 0 ? d   + " " + (d   == 1 ? localization.getRawMessage("unit.day.singular")    : localization.getRawMessage("unit.day.plural"))    : null;
        String wStr   = w   != 0 ? w   + " " + (w   == 1 ? localization.getRawMessage("unit.week.singular")   : localization.getRawMessage("unit.week.plural"))   : null;
        String mStr   = m   != 0 ? m   + " " + (m   == 1 ? localization.getRawMessage("unit.month.singular")  : localization.getRawMessage("unit.month.plural"))  : null;
        String yStr   = y   != 0 ? y   + " " + (y   == 1 ? localization.getRawMessage("unit.year.singular")   : localization.getRawMessage("unit.year.plural"))   : null;

        String r = (yStr   == null ? "" : yStr   + " ")
                 + (mStr   == null ? "" : mStr   + " ")
                 + (wStr   == null ? "" : wStr   + " ")
                 + (dStr   == null ? "" : dStr   + " ")
                 + (hStr   == null ? "" : hStr   + " ")
                 + (minStr == null ? "" : minStr + " ")
                 + (sec == 0 && seconds != 0 ? "" : secStr + " ");
        return r.substring(0, r.length() - 1);
    }

    private TimeUtil() {}
}
