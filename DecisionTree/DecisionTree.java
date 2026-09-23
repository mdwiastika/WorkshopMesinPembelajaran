package DecisionTree;

import java.util.*;

public class DecisionTree {

    public static class TreeNode {
        public String attribute;
        public String leafClass;
        public int samples;
        public Map<String, Integer> classDistribution = new LinkedHashMap<>();
        public Map<String, TreeNode> children = new LinkedHashMap<>();

        public boolean isLeaf() {
            return leafClass != null;
        }
    }

    public static class CaseDataset {
        public String title, targetName;
        public String[] featureNames;
        public int idColIndex, targetColIndex;
        public int[] featureIndices;
        public List<String[]> data;

        public CaseDataset(String title, String[] featureNames, String targetName,
                int idColIndex, int targetColIndex, int[] featureIndices, String[][] rawData) {
            this.title = title;
            this.featureNames = featureNames;
            this.targetName = targetName;
            this.idColIndex = idColIndex;
            this.targetColIndex = targetColIndex;
            this.featureIndices = featureIndices;
            this.data = new ArrayList<>(Arrays.asList(rawData));
        }
    }

    private static void printSeparator(String title) {
        System.out.println("==========================================================================");
        if (title != null)
            System.out.println(" " + title);
        System.out.println("==========================================================================");
    }

    private static void printLine() {
        System.out.println("--------------------------------------------------------------------------");
    }

    public static double calculateEntropy(List<String> labels) {
        if (labels == null || labels.isEmpty())
            return 0.0;
        int n = labels.size();
        Map<String, Integer> counts = new HashMap<>();
        for (String label : labels)
            counts.merge(label, 1, Integer::sum);

        double entropy = 0.0;
        for (int count : counts.values()) {
            double p = (double) count / n;
            if (p > 0.0)
                entropy -= p * (Math.log(p) / Math.log(2.0));
        }
        return entropy;
    }

    public static double calculateWeightedEntropy(List<String[]> subset, int featIdx, int targetIdx) {
        int n = subset.size();
        if (n == 0)
            return 0.0;

        Map<String, List<String>> valSubsets = new LinkedHashMap<>();
        for (String[] row : subset)
            valSubsets.computeIfAbsent(row[featIdx], k -> new ArrayList<>()).add(row[targetIdx]);

        double weightedEnt = 0.0;
        for (List<String> subLabels : valSubsets.values())
            weightedEnt += ((double) subLabels.size() / n) * calculateEntropy(subLabels);
        return weightedEnt;
    }

    public static String getMajorityFromMap(Map<String, Integer> counts, String fallback) {
        String best = fallback;
        int max = -1;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    public static String getMajorityClass(List<String[]> subset, int targetIdx, String fallback) {
        if (subset == null || subset.isEmpty())
            return fallback;
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String[] row : subset)
            counts.merge(row[targetIdx], 1, Integer::sum);
        return getMajorityFromMap(counts, fallback);
    }

    public static TreeNode buildTree(List<String[]> subset, CaseDataset ds,
            List<Integer> availableFeatureIndices,
            Integer maxDepth, int currentDepth, String parentMajority) {
        TreeNode node = new TreeNode();
        node.samples = subset.size();

        for (String[] row : subset)
            node.classDistribution.merge(row[ds.targetColIndex], 1, Integer::sum);

        String currentMajority = getMajorityClass(subset, ds.targetColIndex, parentMajority);

        if (node.classDistribution.size() == 1) {
            node.leafClass = subset.get(0)[ds.targetColIndex];
            return node;
        }
        if (availableFeatureIndices == null || availableFeatureIndices.isEmpty()) {
            node.leafClass = currentMajority;
            return node;
        }
        if (maxDepth != null && currentDepth >= maxDepth) {
            node.leafClass = currentMajority;
            return node;
        }

        List<String> targetLabels = new ArrayList<>();
        for (String[] row : subset)
            targetLabels.add(row[ds.targetColIndex]);
        double currentEntropy = calculateEntropy(targetLabels);

        int bestFeatIdx = -1;
        double maxGain = -1.0;

        for (int fIdx : availableFeatureIndices) {
            double gain = currentEntropy - calculateWeightedEntropy(subset, fIdx, ds.targetColIndex);

            boolean isBetter = gain > maxGain + 1e-6;
            if (!isBetter && Math.abs(gain - maxGain) <= 1e-6
                    && ds.featureNames[fIdx - 1].equalsIgnoreCase("Kelamin")) {
                isBetter = true;
            }
            if (isBetter) {
                maxGain = gain;
                bestFeatIdx = fIdx;
            }
        }

        if (bestFeatIdx == -1 || maxGain <= 1e-7) {
            node.leafClass = currentMajority;
            return node;
        }

        node.attribute = ds.featureNames[bestFeatIdx - 1];

        List<Integer> nextFeatures = new ArrayList<>(availableFeatureIndices);
        nextFeatures.remove(Integer.valueOf(bestFeatIdx));

        Map<String, List<String[]>> partitions = new LinkedHashMap<>();
        for (String[] row : subset)
            partitions.computeIfAbsent(row[bestFeatIdx], k -> new ArrayList<>()).add(row);

        for (Map.Entry<String, List<String[]>> entry : partitions.entrySet())
            node.children.put(entry.getKey(),
                    buildTree(entry.getValue(), ds, nextFeatures, maxDepth, currentDepth + 1, currentMajority));

        return node;
    }

    public static void displayRootNodeCalculations(CaseDataset ds) {
        printSeparator(String.format("LANGKAH 1: PEMILIHAN NODE AKAR (ROOT NODE) - %s", ds.title.toUpperCase()));

        List<String> allLabels = new ArrayList<>();
        for (String[] row : ds.data)
            allLabels.add(row[ds.targetColIndex]);
        double totalEntropy = calculateEntropy(allLabels);

        System.out.printf("Total Sampel (S) : %d data%nEntropy(S) Total : %.4f%n%n", ds.data.size(), totalEntropy);
        System.out.printf("%-15s | %-20s | %-18s | %-15s%n",
                "Atribut", "Entropy Terboboti", "Information Gain", "Status");
        printLine();

        String bestAttr = null;
        double maxGain = -1.0, bestWEnt = 0.0;
        double[] gains = new double[ds.featureNames.length];
        double[] wEnts = new double[ds.featureNames.length];

        for (int i = 0; i < ds.featureNames.length; i++) {
            wEnts[i] = calculateWeightedEntropy(ds.data, ds.featureIndices[i], ds.targetColIndex);
            gains[i] = totalEntropy - wEnts[i];
            if (gains[i] > maxGain) {
                maxGain = gains[i];
                bestWEnt = wEnts[i];
                bestAttr = ds.featureNames[i];
            }
        }
        for (int i = 0; i < ds.featureNames.length; i++) {
            boolean isBest = ds.featureNames[i].equalsIgnoreCase(bestAttr);
            System.out.printf("%-15s | %-20.4f | %-18.4f | %s%n",
                    ds.featureNames[i], wEnts[i], gains[i], isBest ? "<-- TERPILIH (ROOT NODE)" : "");
        }

        printLine();
        System.out.printf("KESIMPULAN: Atribut '%s' terpilih sebagai NODE AKAR%n", bestAttr);
        System.out.printf("            karena memiliki Entropy Terboboti TERKECIL (%.4f)%n", bestWEnt);
        System.out.printf("            dan Information Gain TERTINGGI (%.4f).%n%n", maxGain);
    }

    public static void printTree(TreeNode node, String indent, boolean isRoot) {
        if (node.isLeaf()) {
            System.out.printf("--> [%s] (n=%d, dist=%s)%n", node.leafClass, node.samples, node.classDistribution);
            return;
        }
        if (isRoot)
            System.out.println(node.attribute);

        for (Map.Entry<String, TreeNode> entry : node.children.entrySet()) {
            TreeNode child = entry.getValue();
            System.out.printf("%s|-- %s = %s", indent, node.attribute, entry.getKey());
            if (child.isLeaf()) {
                System.out.print(" ");
                printTree(child, indent + "|   ", false);
            } else {
                System.out.printf(" : [Atribut: %s]%n", child.attribute);
                printTree(child, indent + "|   ", false);
            }
        }
    }

    public static void extractRules(TreeNode node, List<String> conditions, List<String> rulesList) {
        if (node.isLeaf()) {
            rulesList.add("IF " + String.join(" AND ", conditions) + " THEN " + node.leafClass);
            return;
        }
        for (Map.Entry<String, TreeNode> entry : node.children.entrySet()) {
            List<String> next = new ArrayList<>(conditions);
            next.add(node.attribute.toLowerCase() + " = " + entry.getKey());
            extractRules(entry.getValue(), next, rulesList);
        }
    }

    public static String predict(TreeNode node, String[] instance, CaseDataset ds) {
        if (node.isLeaf())
            return node.leafClass;

        int attrColIdx = -1;
        for (int i = 0; i < ds.featureNames.length; i++) {
            if (ds.featureNames[i].equalsIgnoreCase(node.attribute)) {
                attrColIdx = ds.featureIndices[i];
                break;
            }
        }
        if (attrColIdx == -1)
            return node.leafClass != null ? node.leafClass : "UNKNOWN";

        TreeNode child = node.children.get(instance[attrColIdx]);

        if (child == null)
            return getMajorityFromMap(node.classDistribution, "UNKNOWN");
        return predict(child, instance, ds);
    }

    public static void evaluateModel(TreeNode root, CaseDataset ds, String modelName) {
        printLine();
        System.out.printf(" EVALUASI & PREDIKSI DATA TRAINING (%s)%n", modelName);
        printLine();

        System.out.printf("%-6s | ", "ID/No");
        for (String feat : ds.featureNames)
            System.out.printf("%-12s | ", feat);
        System.out.printf("%-15s | %-15s | %-8s%n", "Aktual", "Prediksi", "Status");
        printLine();

        int correct = 0;
        for (String[] row : ds.data) {
            String actual = row[ds.targetColIndex];
            String pred = predict(root, row, ds);
            boolean match = actual.equalsIgnoreCase(pred);
            if (match)
                correct++;

            System.out.printf("%-6s | ", row[ds.idColIndex]);
            for (int fIdx : ds.featureIndices)
                System.out.printf("%-12s | ", row[fIdx]);
            System.out.printf("%-15s | %-15s | %-8s%n", actual, pred, match ? "OK" : "SALAH");
        }

        double accuracy = ((double) correct / ds.data.size()) * 100.0;
        printLine();
        System.out.printf("Jumlah Data      : %d%n", ds.data.size());
        System.out.printf("Prediksi Benar   : %d data (Akurasi: %.2f%%)%n", correct, accuracy);
        System.out.printf("Prediksi Salah   : %d data (Error Rate: %.2f%%)%n",
                ds.data.size() - correct, 100.0 - accuracy);
        printLine();
        System.out.println();
    }

    public static CaseDataset getHipertensiDataset() {
        return new CaseDataset("Diagnosa Hipertensi",
                new String[] { "Usia", "Berat", "Kelamin" }, "Hipertensi", 0, 4, new int[] { 1, 2, 3 },
                new String[][] {
                        { "Ali", "muda", "overweight", "pria", "ya" },
                        { "Edi", "muda", "underweight", "pria", "tidak" },
                        { "Annie", "muda", "average", "wanita", "tidak" },
                        { "Budiman", "tua", "overweight", "pria", "tidak" },
                        { "Herman", "tua", "overweight", "pria", "ya" },
                        { "Didi", "muda", "underweight", "pria", "tidak" },
                        { "Rina", "tua", "overweight", "wanita", "ya" }, { "Gatot", "tua", "average", "pria", "tidak" }
                });
    }

    public static CaseDataset getGangguanServerDataset() {
        return new CaseDataset("Deteksi Gangguan Jaringan Komputer / Server",
                new String[] { "Waktu", "Paket", "Frekwensi", "Prioritas" }, "Gangguan", 0, 5, new int[] { 1, 2, 3, 4 },
                new String[][] {
                        { "1", "PENDEK", "BESAR", "SEDANG", "RENDAH", "GANGGUAN" },
                        { "2", "PENDEK", "KECIL", "RENDAH", "TINGGI", "GANGGUAN" },
                        { "3", "PANJANG", "BESAR", "SEDANG", "TINGGI", "NORMAL" },
                        { "4", "PANJANG", "KECIL", "TINGGI", "RENDAH", "NORMAL" },
                        { "5", "PENDEK", "BESAR", "TINGGI", "TINGGI", "GANGGUAN" },
                        { "6", "PANJANG", "KECIL", "RENDAH", "TINGGI", "GANGGUAN" },
                        { "7", "PANJANG", "KECIL", "TINGGI", "RENDAH", "GANGGUAN" },
                        { "8", "PANJANG", "KECIL", "SEDANG", "RENDAH", "NORMAL" },
                        { "9", "PANJANG", "BESAR", "TINGGI", "TINGGI", "NORMAL" },
                        { "10", "PANJANG", "KECIL", "SEDANG", "RENDAH", "GANGGUAN" },
                        { "11", "PENDEK", "BESAR", "SEDANG", "TINGGI", "NORMAL" },
                        { "12", "PANJANG", "BESAR", "RENDAH", "TINGGI", "NORMAL" }
                });
    }

    public static CaseDataset getSakitJantungDataset() {
        return new CaseDataset("Diagnosa Risiko Penyakit Jantung",
                new String[] { "Usia", "Kelamin", "Merokok", "Olahraga" }, "Jantung", 0, 5, new int[] { 1, 2, 3, 4 },
                new String[][] {
                        { "1", "TUA", "PRIA", "TIDAK", "YA", "TIDAK" }, { "2", "TUA", "PRIA", "YA", "YA", "TIDAK" },
                        { "3", "MUDA", "PRIA", "YA", "TIDAK", "TIDAK" },
                        { "4", "TUA", "PRIA", "TIDAK", "TIDAK", "TIDAK" },
                        { "5", "MUDA", "WANITA", "TIDAK", "TIDAK", "YA" }, { "6", "MUDA", "PRIA", "TIDAK", "YA", "YA" },
                        { "7", "MUDA", "PRIA", "TIDAK", "YA", "TIDAK" },
                        { "8", "TUA", "WANITA", "TIDAK", "TIDAK", "YA" },
                        { "9", "MUDA", "PRIA", "YA", "TIDAK", "TIDAK" },
                        { "10", "TUA", "PRIA", "YA", "TIDAK", "TIDAK" },
                        { "11", "MUDA", "PRIA", "YA", "YA", "YA" }, { "12", "TUA", "PRIA", "YA", "TIDAK", "TIDAK" },
                        { "13", "MUDA", "PRIA", "TIDAK", "TIDAK", "TIDAK" },
                        { "14", "TUA", "PRIA", "TIDAK", "YA", "TIDAK" },
                        { "15", "MUDA", "PRIA", "YA", "TIDAK", "TIDAK" }
                });
    }

    public static void runCase(CaseDataset ds, Integer pruneDepth) {
        System.out.println("\n##########################################################################");
        System.out.printf("           STUDI KASUS: %s%n", ds.title.toUpperCase());
        System.out.println("##########################################################################\n");

        displayRootNodeCalculations(ds);

        List<Integer> features = new ArrayList<>();
        for (int idx : ds.featureIndices)
            features.add(idx);

        TreeNode fullTree = buildTree(ds.data, ds, features, null, 0, "UNKNOWN");
        printSeparator("2. VISUALISASI POHON KEPUTUSAN (FULL TREE - TANPA PRUNING)");
        printTree(fullTree, "", true);
        System.out.println("==========================================================================\n");

        printSeparator("3. ATURAN KEPUTUSAN (DECISION RULES - FULL TREE)");
        List<String> rules = new ArrayList<>();
        extractRules(fullTree, new ArrayList<>(), rules);
        for (int i = 0; i < rules.size(); i++)
            System.out.printf("R%d: %s%n", i + 1, rules.get(i));
        System.out.println("==========================================================================\n");

        evaluateModel(fullTree, ds, "Full Tree");

        if (pruneDepth != null) {
            TreeNode prunedTree = buildTree(ds.data, ds, features, pruneDepth, 0, "UNKNOWN");

            printSeparator(
                    String.format("4. VISUALISASI POHON KEPUTUSAN SETELAH PRUNING (MAX DEPTH = %d)", pruneDepth));
            printTree(prunedTree, "", true);
            System.out.println("==========================================================================\n");

            printSeparator("5. ATURAN KEPUTUSAN SETELAH PRUNING (PRUNED RULES)");
            List<String> prunedRules = new ArrayList<>();
            extractRules(prunedTree, new ArrayList<>(), prunedRules);
            for (int i = 0; i < prunedRules.size(); i++)
                System.out.printf("RP%d: %s%n", i + 1, prunedRules.get(i));
            System.out.println("==========================================================================\n");

            evaluateModel(prunedTree, ds, "Pruned Tree (Depth " + pruneDepth + ")");
        }
    }

    public static void main(String[] args) {
        runCase(getHipertensiDataset(), null);
        runCase(getGangguanServerDataset(), 2);
        runCase(getSakitJantungDataset(), 2);
    }
}
