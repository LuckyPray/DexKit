package org.luckypray.dexkit.util;

/**
 * This class provides Unicode conversion utility methods that allow to convert a string into Unicode sequence and vice-versa. (See methods
 * descriptions for details)
 *
 * @author Michael Gantman
 */
public class StringUnicodeEncoderDecoder {
    private final static String UNICODE_PREFIX = "\\u";
    private final static String UPPER_CASE_UNICODE_PREFIX = "\\U";
    private final static String UPPER_CASE_UNICODE_PREFIX_REGEX = "\\\\U";
    private final static String DELIMITER = "\\\\u";

    /**
     * This method converts a {@link String} of characters in any language into a String That contains a sequence of Unicode codes corresponding to
     * characters in the original String For Example String "Hello" will be converted into a String "\u005c\u00750048\u005c\u00750065\u005c\u0075006c\u005c\u0075006c\u005c\u0075006f" Null or empty
     * String conversion will return an empty String
     *
     * @param txt {@link String} that contains a sequence of characters to convert
     * @return {@link String} That contains a sequence of unicode codes corresponding to the characters in the original String. Each code will be in
     *         hexadecimal format preceded by prefix "\u005c\u0075" with no spaces between them. The String also will have no leading or trailing
     *         white spaces
     */
    public static String encodeStringToUnicodeSequence(String txt) {
        StringBuilder result = new StringBuilder();
        if (txt != null && !txt.isEmpty()) {
            for (int i = 0; i < txt.length(); i++) {
                result.append(convertCodePointToUnicodeString(Character.codePointAt(txt, i)));
                if (Character.isHighSurrogate(txt.charAt(i))) {
                    i++;
                }
            }
        }
        return result.toString();
    }

    /**
     * This method converts {@link String} that contains a sequence of Unicode codes onto a String of corresponding characters. For example a String
     * "\u005c\u00750048\u005c\u00750065\u005c\u0075006c\u005c\u0075006c\u005c\u0075006f" will be converted into String "Hello" by this method. This method performs reverse conversion of the one
     * performed by method {@link #encodeStringToUnicodeSequence(String)} I.e. Any textual String converted into sequence of Unicode codes by method
     * {@link #encodeStringToUnicodeSequence(String)} may be retrieved back by invoking this method on that Unicode sequence String.
     *
     * @param unicodeSequence {@link String} That contains sequence of Unicode codes. Each code must be in hexadecimal format and must be preceded by
     *                        "'backslash' + 'u'" prefix. (note that prefix '\U' is now valid as opposed to earlier versions). This method allows
     *                        leading and trailing whitespaces for the whole String as well as spaces between codes. Those white spaces will be ignored.
     * @return {@link String} That contains sequence of characters that correspond to the respective Unicode codes in the original String
     * @throws IllegalArgumentException if input String is in invalid format. For example if any code is not in hexadecimal format or the code is not a valid Unicode code
     *                                  (not valid code point).
     */
    public static String decodeUnicodeSequenceToString(String unicodeSequence) throws IllegalArgumentException {
        StringBuilder result = new StringBuilder();
        try {
            unicodeSequence = replaceUpperCase_U_WithLoverCase(unicodeSequence);
            unicodeSequence = unicodeSequence.trim().substring(UNICODE_PREFIX.length());
            for (String codePointStr : unicodeSequence.split(DELIMITER)) {
                result.append(Character.toChars(Integer.parseInt(codePointStr.trim(), 16)));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while converting unicode sequence String to String", e);
        }
        return result.toString();
    }

    private static String replaceUpperCase_U_WithLoverCase(String unicodeSequence) {
        String result = unicodeSequence;
        if(unicodeSequence != null && unicodeSequence.contains(UPPER_CASE_UNICODE_PREFIX)) {
            result = unicodeSequence.replaceAll(UPPER_CASE_UNICODE_PREFIX_REGEX, DELIMITER);
        }
        return result;
    }

    /**
     * This method converts an integer that holds a unicode code value into a String
     *
     * @param codePoint a unicode code value
     * @return {@link String} that starts with prefix "'backslash' + 'u'" that follows with hexadecimal value of an integer. If the hexadecimal value
     *         of an integer is less then four digits the value is padded with preceding zeros. For example if the integer has value 32 (decimal) it
     *         will be converted into String "\u0020"
     */
    private static String convertCodePointToUnicodeString(int codePoint) {
        StringBuilder result = new StringBuilder(UNICODE_PREFIX);
        String codePointHexStr = Integer.toHexString(codePoint);
        codePointHexStr = codePointHexStr.startsWith("0") ? codePointHexStr.substring(1) : codePointHexStr;
        if (codePointHexStr.length() <= 4) {
            result.append(getPrecedingZerosStr(codePointHexStr.length()));
        }
        result.append(codePointHexStr);
        return result.toString();
    }

    /**
     * This method receives a length of a String and if it is less then 4 it generates a padding String of zeros that can be appended to the String to
     * make it of length 4 I.e. if parameter passed is 1 the returned String will be "000". If the parameter passed is 4 or greater empty String is
     * returned.
     *
     * @param codePointStrLength Length of a String to be padded by preceding zeros to the length of 4
     * @return padding String
     */
    private static String getPrecedingZerosStr(int codePointStrLength) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 4 - codePointStrLength; i++) {
            result.append("0");
        }
        return result.toString();
    }
}