package laba.maven;

public class SumDigitsCalculator {
    public static int calculate(String inputText) {
        int outSum = 0;
        for (int i = 0; i < inputText.length(); i++) {
            char ch = inputText.charAt(i);
            if (Character.isDigit(ch)) {
                outSum += Character.getNumericValue(ch);
            }
        }
        return outSum;
    }
}

