package io.github.nikanique.springrestframework.utilities;


import io.github.nikanique.springrestframework.common.FieldType;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class ValueFormatter {
    private static final Pattern DECIML_POINT_PATTERN = Pattern.compile("\\{\\.\\d{1,2}f}");

    public static Object formatValue(Object value, FieldType outputType, String format) {
        // Cast the value to the desired type if outputType is not null

        if (format == null) {
            return value;
        }

        if (outputType != null) {
            value = castToType(value, outputType);
        }


        // Format DateTime if outputType is DATE_TIME and format is not null
        if (outputType.isDateTime()) {
            return formatDate((Date) value, format);
        }

        // Format Float if outputType is FLOAT and format is not null
        if ((outputType == FieldType.FLOAT || outputType == FieldType.DOUBLE)) {
            return formatFloatAndDouble(value, format);
        }


        // Format numeric types (INT and FLOAT) with ',' separator if format is not null
        if ((outputType == FieldType.INTEGER || outputType == FieldType.LONG)) {
            return formatIntegerAndLong(value, format);
        }

        return value;
    }

    private static Object castToType(Object value, FieldType outputType) {
        // Handle casting based on the outputType
        switch (outputType) {
            case INTEGER:
                return Integer.parseInt(value.toString());
            case STRING:
                return value.toString();
            case FLOAT:
                return Float.parseFloat(value.toString());
            case LONG:
                return Long.parseLong(value.toString());
            case DOUBLE:
                return Double.parseDouble(value.toString());
            case DATE_TIME:
                // Assuming value is already of type Date
                return value;
            default:
                return value;
        }
    }

    private static String formatDate(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    private static Object formatFloatAndDouble(Object value, String format) {
        if (DECIML_POINT_PATTERN.matcher(format).matches()) {
            int decimalPoints = Integer.parseInt(format.substring(2, format.indexOf("f}"))); // Extract the number of decimal points from the format string
            return roundToDecimal(Double.parseDouble(value.toString()), decimalPoints);
        }
        DecimalFormat df = new DecimalFormat(format);
        return df.format(value);
    }

    private static double roundToDecimal(double value, int decimalPoints) {
        double powerOfTen = Math.pow(10, decimalPoints);
        return Math.round(value * powerOfTen) / powerOfTen;
    }

    private static String formatIntegerAndLong(Object value, String format) {
        // Format numeric value with ',' separator and apply decimal formatting if specified
        DecimalFormat df = new DecimalFormat(format);
        return df.format(value);
    }


}
