import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WordCount {
    public static void main(String args[]) {
        String directoryPath = ".";

        File directory = new File(directoryPath);

        File[] files = directory.listFiles();

        if (files == null) {
            System.out.println("No files found in the current directory.");
            return;
        }

        File[] filteredFiles = filterFiles(files);

        System.out.println("\nFirst iteration:");
        System.out.println("===================================================");
        long firstIterationStart = System.currentTimeMillis();
        for (File file : filteredFiles) {
            String mostFrequentWord = getMostFrequentWord(file, 6);
            System.out.printf("File Name: %s\t\t\tWord: %s\n", file.getName(), mostFrequentWord);
        }
        long firstIterationEnd = System.currentTimeMillis();
        long firstIterationDuration = firstIterationEnd - firstIterationStart;
        System.out.printf("First iteration duration: %d seconds\n", firstIterationDuration / 1000.0);

        System.out.println("\nSecond iteration:");
        System.out.println("====================================================");
        long secondIterationStart = System.currentTimeMillis();
        for (File file : filteredFiles) {
            String mostFrequentWord = getMostFrequentWord(file, 7);
            System.out.printf("File Name: %s\t\t\tWord: %s\n", file.getName(), mostFrequentWord);
        }
        long secondIterationEnd = System.currentTimeMillis();
        long secondIterationDuration = secondIterationEnd - secondIterationStart;
        System.out.printf("Second iteration duration: %d seconds\n", secondIterationDuration / 1000.0);

        System.out.println("\nThird iteration:");
        System.out.println("===================================================");
        long thirdIterationStart = System.currentTimeMillis();
        for (File file : filteredFiles) {
            String mostFrequentWord = getMostFrequentWord(file, 8);
            System.out.printf("File Name: %s\t\t\tWord: %s\n", file.getName(), mostFrequentWord);
        }
        long thirdIterationEnd = System.currentTimeMillis();
        long thirdIterationDuration = thirdIterationEnd - thirdIterationStart;
        System.out.printf("Third iteration duration: %d seconds\n", thirdIterationDuration / 1000.0);

        long totalRuntime = thirdIterationDuration + secondIterationDuration + firstIterationDuration;

        System.out.println("========================================================");
        System.out.printf("Total Runtime: %d\n", totalRuntime / 1000);
        System.out.println("========================================================");
    }

    public static File[] filterFiles(File[] files) {
        int count = 0;

        for (File file : files) {
            if (file.isFile() && (file.getName().endsWith(".csv") || file.getName().endsWith(".txt"))) {
                count++;
            }
        }

        File[] result = new File[count];
        int index = 0;

        for (File file : files) {
            if (file.isFile() && (file.getName().endsWith(".csv") || file.getName().endsWith(".txt"))) {
                result[index] = file;
                index++;
            }
        }
        return result;
    }

    public static String getMostFrequentWord(File file, int letters) {
        Map<String, Integer> wordCountMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\W+");

                for (String word : words) {
                    if (word.length() == letters) {
                        word = word.toLowerCase();
                        wordCountMap.put(word, wordCountMap.getOrDefault(word, 0) + 1);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        String mostFrequentWord = null;
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : wordCountMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequentWord = entry.getKey();
            }
        }

        return mostFrequentWord;
    }
}