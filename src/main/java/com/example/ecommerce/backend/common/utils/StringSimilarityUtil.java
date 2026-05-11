package com.example.ecommerce.backend.common.utils;

/**
 * Utility class for string similarity calculations using Levenshtein distance.
 *
 * <p>Levenshtein distance is the minimum number of single-character edits
 * (insertions, deletions, substitutions) required to change one string into another.</p>
 *
 * <p>Lower distance values indicate higher similarity between strings.</p>
 */
public class StringSimilarityUtil {

    private StringSimilarityUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     *
     * <p>This implementation uses dynamic programming to efficiently compute
     * the minimum number of single-character edits needed to transform one
     * string into another.</p>
     *
     * @param str1 first string (non-null)
     * @param str2 second string (non-null)
     * @return the Levenshtein distance between the two strings (always >= 0)
     */
    public static int levenshteinDistance(String str1, String str2) {
        if (str1 == null || str2 == null) {
            throw new IllegalArgumentException("Strings cannot be null");
        }

        // Convert to lowercase for case-insensitive comparison
        String s1 = str1.toLowerCase();
        String s2 = str2.toLowerCase();

        int len1 = s1.length();
        int len2 = s2.length();

        // Create a 2D DP table to store distances
        int[][] dp = new int[len1 + 1][len2 + 1];

        // Initialize first row and column
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        // Fill the DP table
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                // If characters match, no edit is needed
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    // Take minimum of three operations: insert, delete, replace
                    dp[i][j] = 1 + Math.min(
                            dp[i - 1][j],      // deletion
                            Math.min(
                                    dp[i][j - 1],      // insertion
                                    dp[i - 1][j - 1]   // substitution
                            )
                    );
                }
            }
        }

        return dp[len1][len2];
    }
}

