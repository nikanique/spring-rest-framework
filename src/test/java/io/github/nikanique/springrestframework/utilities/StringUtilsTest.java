package io.github.nikanique.springrestframework.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilsTest {

    @Test
    void testCountOfOccurrences() {
        String mainString = "hello world hello";
        String substring = "hello";
        int expectedCount = 2;
        int actualCount = StringUtils.countOfOccurrences(mainString, substring);
        assertEquals(expectedCount, actualCount);
    }

    @Test
    void testCapitalize() {
        String input = "hello";
        String expectedOutput = "Hello";
        String actualOutput = StringUtils.capitalize(input);
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    void testGenerateHashCode() {
        String input = "hello";
        String expectedHashCode = "5e918d2";
        String actualHashCode = StringUtils.generateHashCode(input);
        assertEquals(expectedHashCode, actualHashCode);
    }
}