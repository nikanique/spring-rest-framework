package io.github.nikanique.springrestframework.utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper {

    public static int countOfOccurrences(String mainString, String substring) {
        if (mainString == null || mainString.isEmpty() || substring == null || substring.isEmpty()) {
            return 0;
        }
        Pattern pattern = Pattern.compile(Pattern.quote(substring));
        Matcher matcher = pattern.matcher(mainString);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

}
