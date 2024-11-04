import java.io.File;

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
        System.out.println("----------------------------------------------------");
        for (File file : filteredFiles) {
            System.out.printf("Iterating through file name: %s\n", file.getName());
        }

        System.out.println("\nSecond iteration:");
        System.out.println("----------------------------------------------------");
        for (File file : filteredFiles) {
            System.out.printf("Iterating through file name: %s\n", file.getName());
        }

        System.out.println("\nThird iteration:");
        System.out.println("----------------------------------------------------");
        for (File file : filteredFiles) {
            System.out.printf("Iterating through file name: %s\n", file.getName());
        }
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
}