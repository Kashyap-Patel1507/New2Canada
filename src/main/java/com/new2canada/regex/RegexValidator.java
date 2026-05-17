package com.new2canada.regex;

import java.util.regex.Pattern;

/**
 * Validates user-supplied strings against well-known Canadian formats.
 *
 * <p>Each pattern is compiled once as a {@code static final Pattern} so the
 * regex engine doesn't recompile on every call.
 *
 * <p>Currently supported:
 * <ul>
 *   <li>Canadian postal code — letter-digit-letter, optional space, digit-letter-digit
 *       (e.g. "N9B 3P4" or "M5J0E6").</li>
 *   <li>Email address — RFC-5322-ish; tight enough for student-form input.</li>
 *   <li>Canadian phone number — accepts +1, parentheses, spaces, dashes.</li>
 *   <li>Social Insurance Number — 9 digits, optional dashes (we only check
 *       shape, not the Luhn checksum — that's covered by
 *       {@link com.new2canada.regex.PatternFinder}).</li>
 * </ul>
 *
 * Demonstrates: <b>Regular Expressions</b> (validation).
 */
public final class RegexValidator {

    private RegexValidator() {}

    // Each pattern: letter [A-Z], digit [0-9], optional space, letter, digit, letter.
    private static final Pattern POSTAL_CODE =
            Pattern.compile("^[A-CEGHJ-NPRSTVXY]\\d[A-CEGHJ-NPRSTV-Z]\\s?\\d[A-CEGHJ-NPRSTV-Z]\\d$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_CA =
            Pattern.compile("^\\+?1?[\\s\\-.]?\\(?\\d{3}\\)?[\\s\\-.]?\\d{3}[\\s\\-.]?\\d{4}$");

    private static final Pattern SIN =
            Pattern.compile("^\\d{3}[- ]?\\d{3}[- ]?\\d{3}$");

    public static boolean isPostalCode(String s) { return s != null && POSTAL_CODE.matcher(s.trim()).matches(); }
    public static boolean isEmail(String s)      { return s != null && EMAIL.matcher(s.trim()).matches(); }
    public static boolean isPhone(String s)      { return s != null && PHONE_CA.matcher(s.trim()).matches(); }
    public static boolean isSin(String s)        { return s != null && SIN.matcher(s.trim()).matches(); }

    /** Dispatcher used by /api/validate. */
    public static boolean validate(String type, String value) {
        if (type == null) return false;
        return switch (type.toLowerCase()) {
            case "postal", "postal_code", "postalcode" -> isPostalCode(value);
            case "email"                               -> isEmail(value);
            case "phone"                               -> isPhone(value);
            case "sin"                                 -> isSin(value);
            default                                    -> false;
        };
    }
}
