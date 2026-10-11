package com.generated.app;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathEvaluator {

    /**
     * Evaluates a mathematical string expression like "5*5", "10 + 20 / 2", "(3+2)*4"
     * and adds the secret offset if secret mode is enabled.
     */
    public static double evaluateExpression(String expression, boolean secretEnabled, double secretOffset) throws Exception {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty expression");
        }
        double baseResult = new Parser(expression.trim()).parse();
        if (secretEnabled) {
            return baseResult + secretOffset;
        }
        return baseResult;
    }

    /**
     * Formats a double neatly: integer without .0, decimals up to 4 places.
     */
    public static String formatResult(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        DecimalFormat df = new DecimalFormat("#.####");
        return df.format(value);
    }

    /**
     * Scans through text looking for math patterns like:
     * "5*5 =" or "5*5=" or "10+20 = ?" or standalone formulas ending with '='
     * and evaluates them with the secret offset.
     */
    public static String solveExpressionsInText(String text, boolean secretEnabled, double offset) {
        if (text == null) return "";

        // Match patterns like "5*5=" or "5*5 =" or "(10+20)*3="
        Pattern pattern = Pattern.compile("(?m)([0-9\\.\\s\\+\\-\\*/\\(\\)\\^]+)\\s*=\\s*(\\?|[0-9\\.\\-]+)?");
        Matcher matcher = pattern.matcher(text);

        StringBuffer sb = new StringBuffer();
        boolean found = false;
        while (matcher.find()) {
            String exprStr = matcher.group(1).trim();
            // Validate that it looks like a valid mathematical expression
            if (exprStr.matches(".*[\\+\\-\\*/\\^].*") && exprStr.matches("^[0-9\\.\\s\\+\\-\\*/\\(\\)\\^]+$")) {
                try {
                    double result = evaluateExpression(exprStr, secretEnabled, offset);
                    String formatted = formatResult(result);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(exprStr + " = " + formatted));
                    found = true;
                    continue;
                } catch (Exception ignored) {
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
        }
        matcher.appendTail(sb);

        // If no "= " patterns were matched, also check line-by-line standalone arithmetic lines
        if (!found) {
            String[] lines = text.split("\n");
            StringBuilder resultText = new StringBuilder();
            boolean anyCalculated = false;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String trimmed = line.trim();
                if (trimmed.matches("^[0-9\\.\\s\\+\\-\\*/\\(\\)\\^]+$") && trimmed.matches(".*[\\+\\-\\*/\\^].*")) {
                    try {
                        double val = evaluateExpression(trimmed, secretEnabled, offset);
                        resultText.append(trimmed).append(" = ").append(formatResult(val));
                        anyCalculated = true;
                    } catch (Exception e) {
                        resultText.append(line);
                    }
                } else {
                    resultText.append(line);
                }
                if (i < lines.length - 1) {
                    resultText.append("\n");
                }
            }
            if (anyCalculated) {
                return resultText.toString();
            }
        }

        return sb.toString();
    }

    // Recursive descent parser for standard arithmetic
    private static class Parser {
        private final String str;
        private int pos = -1;
        private int ch;

        Parser(String str) {
            this.str = str;
        }

        void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < str.length()) {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }
            return x;
        }

        double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*') || eat('×')) x *= parseFactor();
                else if (eat('/') || eat('÷')) {
                    double divisor = parseFactor();
                    if (divisor == 0) throw new ArithmeticException("Division by zero");
                    x /= divisor;
                } else return x;
            }
        }

        double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                eat(')');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(str.substring(startPos, this.pos));
            } else {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }

            if (eat('^')) x = Math.pow(x, parseFactor());

            return x;
        }
    }
}
