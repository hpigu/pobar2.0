package com.pobar.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class XssUtil {

    private XssUtil() {}

    public static String sanitize(String input) {
        if (input == null) return null;
        return Jsoup.clean(input.trim(), Safelist.none());
    }
}
