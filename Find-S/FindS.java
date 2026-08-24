import java.util.*;

public class FindS {

    public static void trainFindS(String[][] dataset, int featureStartIndex) {
        int targetIndex = dataset[0].length - 1;
        Set<String> uniqueTargets = new LinkedHashSet<>();
        for (String[] row : dataset)
            uniqueTargets.add(row[targetIndex]);

        for (String targetLabel : uniqueTargets) {
            String[] hypothesis = null;

            for (String[] row : dataset) {
                if (row[targetIndex].equalsIgnoreCase(targetLabel)) {
                    if (hypothesis == null) {
                        hypothesis = Arrays.copyOfRange(row, featureStartIndex, targetIndex);
                    } else {
                        for (int j = 0; j < hypothesis.length; j++) {
                            if (!hypothesis[j].equals("*")
                                    && !hypothesis[j].equalsIgnoreCase(row[featureStartIndex + j])) {
                                hypothesis[j] = "*";
                            }
                        }
                    }
                }
            }
            System.out.println("H(" + String.join(", ", hypothesis) + ") = " + targetLabel);
        }
    }

    public static void main(String[] args) {
        String[][] dtTraining = {
                { "D1", "Cerah", "Normal", "Pelan", "Ya" },
                { "D2", "Cerah", "Normal", "Pelan", "Ya" },
                { "D3", "Hujan", "Tinggi", "Pelan", "Tidak" },
                { "D4", "Cerah", "Normal", "Kencang", "Ya" },
                { "D5", "Hujan", "Tinggi", "Kencang", "Tidak" },
                { "D6", "Cerah", "Normal", "Pelan", "Ya" }
        };

        trainFindS(dtTraining, 1);
    }
}