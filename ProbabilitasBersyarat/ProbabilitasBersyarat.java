import java.util.*;

public class ProbabilitasBersyarat {

    static final String[] FEATURE_NAMES = { "Outlook", "Temperature", "Humidity", "Wind" };
    static final String TARGET_NAME = "PlayTennis";

    static List<String[]> dataset = new ArrayList<>();

    public static void initDataset() {
        dataset.clear();
        String[][] initialData = {
                { "D1", "Sunny", "Hot", "High", "Weak", "No" },
                { "D2", "Sunny", "Hot", "High", "Strong", "No" },
                { "D3", "Overcast", "Hot", "High", "Weak", "Yes" },
                { "D4", "Rain", "Mild", "High", "Weak", "Yes" },
                { "D5", "Rain", "Cool", "Normal", "Weak", "Yes" },
                { "D6", "Rain", "Cool", "Normal", "Strong", "No" },
                { "D7", "Overcast", "Cool", "Normal", "Strong", "Yes" },
                { "D8", "Sunny", "Mild", "High", "Weak", "No" },
                { "D9", "Sunny", "Cool", "Normal", "Weak", "Yes" },
                { "D10", "Rain", "Mild", "Normal", "Weak", "Yes" },
                { "D11", "Sunny", "Mild", "Normal", "Strong", "Yes" },
                { "D12", "Overcast", "Mild", "High", "Strong", "Yes" },
                { "D13", "Overcast", "Hot", "Normal", "Weak", "Yes" },
                { "D14", "Rain", "Mild", "High", "Strong", "No" }
        };

        for (String[] row : initialData) {
            dataset.add(row);
        }
    }

    public static int countTarget(String targetVal) {
        int count = 0;
        for (String[] row : dataset) {
            if (row[5].equalsIgnoreCase(targetVal)) {
                count++;
            }
        }
        return count;
    }

    public static int countFeatureGivenTarget(int featureIdx, String featureVal, String targetVal) {
        int count = 0;
        for (String[] row : dataset) {
            if (row[featureIdx + 1].equalsIgnoreCase(featureVal) && row[5].equalsIgnoreCase(targetVal)) {
                count++;
            }
        }
        return count;
    }

    public static double conditionalProbability(int featureIdx, String featureVal, String targetVal) {
        int targetCount = countTarget(targetVal);
        if (targetCount == 0)
            return 0.0;
        int featureCount = countFeatureGivenTarget(featureIdx, featureVal, targetVal);
        return (double) featureCount / targetCount;
    }

    public static void displayProbabilityTables() {
        int total = dataset.size();
        int yesCount = countTarget("Yes");
        int noCount = countTarget("No");

        System.out.println("==========================================================================");
        System.out.println("                     1. PROBABILITAS PRIOR (MARGINAL)                     ");
        System.out.println("==========================================================================");
        System.out.printf("Total Data (n)          : %d%n", total);
        System.out.printf("P(PlayTennis = Yes)     : %d/%d = %.4f (%.2f%%)%n",
                yesCount, total, (double) yesCount / total, ((double) yesCount / total) * 100);
        System.out.printf("P(PlayTennis = No)      : %d/%d = %.4f (%.2f%%)%n",
                noCount, total, (double) noCount / total, ((double) noCount / total) * 100);
        System.out.println("==========================================================================\n");

        System.out.println("==========================================================================");
        System.out.println("                      2. TABEL PROBABILITAS BERSYARAT                     ");
        System.out.println("==========================================================================");

        for (int i = 0; i < FEATURE_NAMES.length; i++) {
            String featureName = FEATURE_NAMES[i];

            Set<String> uniqueVals = new LinkedHashSet<>();
            for (String[] row : dataset) {
                uniqueVals.add(row[i + 1]);
            }

            System.out.printf("Atribut: %-12s%n", featureName);
            System.out.printf("%-12s | %-12s | %-12s | %-16s | %-16s%n",
                    "Nilai Fitur", "Count (Yes)", "Count (No)", "P(Fitur|Yes)", "P(Fitur|No)");
            System.out.println("--------------------------------------------------------------------------");

            for (String val : uniqueVals) {
                int cYes = countFeatureGivenTarget(i, val, "Yes");
                int cNo = countFeatureGivenTarget(i, val, "No");
                double pYes = conditionalProbability(i, val, "Yes");
                double pNo = conditionalProbability(i, val, "No");

                System.out.printf("%-12s | %-12d | %-12d | %2d/%-2d = %-8.4f | %2d/%-2d = %-8.4f%n",
                        val, cYes, cNo, cYes, yesCount, pYes, cNo, noCount, pNo);
            }
            System.out.println("--------------------------------------------------------------------------\n");
        }
    }

    public static void classifyCase(String outlook, String temperature, String humidity, String wind, boolean verbose) {
        String[] query = { outlook, temperature, humidity, wind };
        int total = dataset.size();
        int yesCount = countTarget("Yes");
        int noCount = countTarget("No");

        double pPriorYes = (double) yesCount / total;
        double pPriorNo = (double) noCount / total;

        double[] condYes = new double[4];
        int[] countYes = new int[4];
        for (int i = 0; i < 4; i++) {
            condYes[i] = conditionalProbability(i, query[i], "Yes");
            countYes[i] = countFeatureGivenTarget(i, query[i], "Yes");
        }

        double[] condNo = new double[4];
        int[] countNo = new int[4];
        for (int i = 0; i < 4; i++) {
            condNo[i] = conditionalProbability(i, query[i], "No");
            countNo[i] = countFeatureGivenTarget(i, query[i], "No");
        }

        double likelihoodYes = condYes[0] * condYes[1] * condYes[2] * condYes[3];
        double scoreYes = pPriorYes * likelihoodYes;

        double likelihoodNo = condNo[0] * condNo[1] * condNo[2] * condNo[3];
        double scoreNo = pPriorNo * likelihoodNo;

        double totalScore = scoreYes + scoreNo;
        double probYesGivenX = totalScore > 0 ? (scoreYes / totalScore) : 0;
        double probNoGivenX = totalScore > 0 ? (scoreNo / totalScore) : 0;

        String prediction = scoreYes > scoreNo ? "Yes (Play Tennis)" : "No (Not Play Tennis)";

        if (verbose) {
            System.out.println("==========================================================================");
            System.out.println("                           3. PENGUJIAN KASUS                             ");
            System.out.println("==========================================================================");
            System.out.println("Query Kasus Baru:");
            System.out.printf("  - Outlook     : %s%n", outlook);
            System.out.printf("  - Temperature : %s%n", temperature);
            System.out.printf("  - Humidity    : %s%n", humidity);
            System.out.printf("  - Wind        : %s%n", wind);
            System.out.println("--------------------------------------------------------------------------");

            System.out.println("A. Perhitungan untuk Kelas 'Yes':");
            System.out.printf("   P(Yes)          = %d/%d = %.4f%n", yesCount, total, pPriorYes);
            System.out.printf("   P(%s|Yes)      = %d/%d = %.4f%n", outlook, countYes[0], yesCount, condYes[0]);
            System.out.printf("   P(%s|Yes)     = %d/%d = %.4f%n", temperature, countYes[1], yesCount, condYes[1]);
            System.out.printf("   P(%s|Yes)     = %d/%d = %.4f%n", humidity, countYes[2], yesCount, condYes[2]);
            System.out.printf("   P(%s|Yes)   = %d/%d = %.4f%n", wind, countYes[3], yesCount, condYes[3]);
            System.out.println("\n   Rumus Posterior (Yes):");
            System.out.printf("   P(Yes|X) ~ P(Yes) x P(%s|Yes) x P(%s|Yes) x P(%s|Yes) x P(%s|Yes)%n",
                    outlook, temperature, humidity, wind);
            System.out.printf("            = (%d/%d) x (%d/%d) x (%d/%d) x (%d/%d) x (%d/%d)%n",
                    yesCount, total, countYes[0], yesCount, countYes[1], yesCount, countYes[2], yesCount, countYes[3],
                    yesCount);
            System.out.printf("            = %.4f x %.4f x %.4f x %.4f x %.4f%n",
                    pPriorYes, condYes[0], condYes[1], condYes[2], condYes[3]);
            System.out.printf("            = %.6f%n", scoreYes);
            System.out.printf("   Bentuk Pecahan Sesuai Slide: (%d x %d x %d x %d) x 1 / (%d x %d^3)%n",
                    countYes[0], countYes[1], countYes[2], countYes[3], total, yesCount);
            System.out.printf("                              = %d / %d = %.6f%n",
                    (countYes[0] * countYes[1] * countYes[2] * countYes[3]),
                    (long) (total * Math.pow(yesCount, 3)),
                    scoreYes);
            System.out.println();

            System.out.println("B. Perhitungan untuk Kelas 'No':");
            System.out.printf("   P(No)           = %d/%d = %.4f%n", noCount, total, pPriorNo);
            System.out.printf("   P(%s|No)       = %d/%d = %.4f%n", outlook, countNo[0], noCount, condNo[0]);
            System.out.printf("   P(%s|No)      = %d/%d = %.4f%n", temperature, countNo[1], noCount, condNo[1]);
            System.out.printf("   P(%s|No)      = %d/%d = %.4f%n", humidity, countNo[2], noCount, condNo[2]);
            System.out.printf("   P(%s|No)    = %d/%d = %.4f%n", wind, countNo[3], noCount, condNo[3]);
            System.out.println("\n   Rumus Posterior (No):");
            System.out.printf("   P(No|X) ~ P(No) x P(%s|No) x P(%s|No) x P(%s|No) x P(%s|No)%n",
                    outlook, temperature, humidity, wind);
            System.out.printf("           = (%d/%d) x (%d/%d) x (%d/%d) x (%d/%d) x (%d/%d)%n",
                    noCount, total, countNo[0], noCount, countNo[1], noCount, countNo[2], noCount, countNo[3], noCount);
            System.out.printf("           = %.4f x %.4f x %.4f x %.4f x %.4f%n",
                    pPriorNo, condNo[0], condNo[1], condNo[2], condNo[3]);
            System.out.printf("           = %.6f%n", scoreNo);
            System.out.printf("   Bentuk Pecahan Sesuai Slide: (%d x %d x %d x %d) x 1 / (%d x %d^3)%n",
                    countNo[0], countNo[1], countNo[2], countNo[3], total, noCount);
            System.out.printf("                              = %d / %d = %.6f%n",
                    (countNo[0] * countNo[1] * countNo[2] * countNo[3]),
                    (long) (total * Math.pow(noCount, 3)),
                    scoreNo);
            System.out.println();

            System.out.println("C. Perbandingan & Normalisasi Probabilitas:");
            System.out.printf("   Nilai Skor (Yes)    = %.6f%n", scoreYes);
            System.out.printf("   Nilai Skor (No)     = %.6f%n", scoreNo);
            System.out.printf("   P(PlayTennis=Yes|X) = %.6f / (%.6f + %.6f) = %.4f (%.2f%%)%n",
                    scoreYes, scoreYes, scoreNo, probYesGivenX, probYesGivenX * 100);
            System.out.printf("   P(PlayTennis=No|X)  = %.6f / (%.6f + %.6f) = %.4f (%.2f%%)%n",
                    scoreNo, scoreYes, scoreNo, probNoGivenX, probNoGivenX * 100);
            System.out.println("--------------------------------------------------------------------------");
            if (scoreYes >= scoreNo) {
                System.out.printf("   KESIMPULAN: Karena Skor(Yes) [%.6f] >= Skor(No) [%.6f],%n", scoreYes, scoreNo);
                System.out.printf("   Maka keputusan klasifikasi adalah: %s%n", prediction);
            } else {
                System.out.printf("   KESIMPULAN: Karena Skor(No) [%.6f] > Skor(Yes) [%.6f],%n", scoreNo, scoreYes);
                System.out.printf("   Maka keputusan klasifikasi adalah: %s%n", prediction);
            }
            System.out.println("==========================================================================\n");
        } else {
            System.out.printf("  Query: [%s, %s, %s, %s]%n", outlook, temperature, humidity, wind);
            System.out.printf("  -> Skor Yes: %.6f (%.2f%%) | Skor No: %.6f (%.2f%%) => Keputusan: %s%n",
                    scoreYes, probYesGivenX * 100, scoreNo, probNoGivenX * 100, prediction);
        }
    }

    public static void demonstrateLearner(String outlook, String temperature, String humidity, String wind) {
        System.out.println("==========================================================================");
        System.out.println("                   4. DEMONSTRASI KONSEP 'LEARNER'                        ");
        System.out.println("==========================================================================");
        System.out.println("  \"If a new data is: (Outlook=sunny) ^ (Temperature=mild) ^");
        System.out.println("   (Humidity=high) ^ (Wind=strong) -> PlayTennis\"");
        System.out.println("  \"The result (using Naïve Bayes Classifier) -> no\"");
        System.out.println("  \"When the new data is added to the table, the results will change to 'yes'\"");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Simulasi penambahan data baru bernilai 'Yes' ke dataset secara bertahap:\n");

        System.out.println("--- Kondisi Awal (14 Data Asli) ---");
        classifyCase(outlook, temperature, humidity, wind, false);
        System.out.println();
        int maxIterations = 10;
        int iteration = 1;
        boolean changedToYes = false;

        while (iteration <= maxIterations) {
            String newDayId = "D" + (dataset.size() + 1);
            String[] newRow = { newDayId, outlook, temperature, humidity, wind, "Yes" };
            dataset.add(newRow);

            System.out.printf("--- Setelah Menambahkan Data ke-%d: [%s, %s, %s, %s, %s, %s] (Total Data = %d) ---%n",
                    iteration, newRow[0], newRow[1], newRow[2], newRow[3], newRow[4], newRow[5], dataset.size());
            classifyCase(outlook, temperature, humidity, wind, false);
            System.out.println();

            int yesCount = countTarget("Yes");
            int noCount = countTarget("No");
            double pPriorYes = (double) yesCount / dataset.size();
            double pPriorNo = (double) noCount / dataset.size();

            double sYes = pPriorYes
                    * conditionalProbability(0, outlook, "Yes")
                    * conditionalProbability(1, temperature, "Yes")
                    * conditionalProbability(2, humidity, "Yes")
                    * conditionalProbability(3, wind, "Yes");

            double sNo = pPriorNo
                    * conditionalProbability(0, outlook, "No")
                    * conditionalProbability(1, temperature, "No")
                    * conditionalProbability(2, humidity, "No")
                    * conditionalProbability(3, wind, "No");

            if (sYes > sNo) {
                changedToYes = true;
                System.out.printf("    Pada iterasi ke-%d, conditional probability ter-update,%n", iteration);
                System.out.println("    sehingga keputusan klasifikasi resmi berubah menjadi 'Yes (Play Tennis)'!");
                break;
            }
            iteration++;
        }

        if (!changedToYes) {
            System.out.printf(">>> CATATAN: Hingga %d iterasi, hasil belum berubah menjadi 'Yes'.%n", maxIterations);
            System.out.println("    Hal ini terjadi apabila nilai peluang awal kelas 'No' sangat dominan,");
            System.out.println("    sehingga dibutuhkan lebih banyak sampel data 'Yes' untuk menggeser posterior.");
        }
        System.out.println("==========================================================================\n");
    }

    public static void runInteractiveMenu(Scanner sc) {
        initDataset();

        System.out.println("==========================================================================");
        System.out.println("                       UJI COBA QUERY KUSTOM                              ");
        System.out.println("==========================================================================");
        System.out.println("Pilihan nilai atribut yang tersedia:");
        System.out.println("  1. Outlook     : Sunny, Overcast, Rain");
        System.out.println("  2. Temperature : Hot, Mild, Cool");
        System.out.println("  3. Humidity    : High, Normal");
        System.out.println("  4. Wind        : Weak, Strong");
        System.out.println("--------------------------------------------------------------------------");

        try {
            System.out.print("Masukkan Outlook     (Sunny/Overcast/Rain) : ");
            String outlook = sc.nextLine().trim();
            System.out.print("Masukkan Temperature (Hot/Mild/Cool)       : ");
            String temp = sc.nextLine().trim();
            System.out.print("Masukkan Humidity    (High/Normal)         : ");
            String humidity = sc.nextLine().trim();
            System.out.print("Masukkan Wind        (Weak/Strong)         : ");
            String wind = sc.nextLine().trim();

            System.out.println();
            classifyCase(outlook, temp, humidity, wind, true);
        } catch (Exception e) {
            System.out.println("Input dibatalkan atau selesai.");
        }
    }

    public static void main(String[] args) {
        initDataset();

        displayProbabilityTables();

        classifyCase("Sunny", "Mild", "High", "Strong", true);

        demonstrateLearner("Sunny", "Mild", "High", "Strong");

        if (args.length >= 4) {
            System.out.println("--- Menguji Query dari Argumen Command Line ---");
            initDataset();
            classifyCase(args[0], args[1], args[2], args[3], true);
        } else if (System.console() != null) {
            Scanner sc = new Scanner(System.in);
            System.out.print("Apakah ingin mencoba query cuaca kustom? (y/n): ");
            String ans = sc.nextLine().trim();
            if (ans.equalsIgnoreCase("y")) {
                runInteractiveMenu(sc);
            }
            sc.close();
        }
    }
}
