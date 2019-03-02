import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;


public class decisiontree {

    private static String INPUT_FILE = "input.csv";

    enum Attributes
    {
        ALTERNATE, BAR, FRI_SAT, HUNGRY, PATRONS, PRICE, RAINING, RESERVATION, TYPE, WAIT_ESTIMATE, WILL_WAIT;
    }

    //Reads Data from CSV file.
    private Map<Attributes, List<String>> readData() throws Exception {
        Map<Attributes, List<String>> dataSet = new HashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(INPUT_FILE)));
        String inputLine =  null;
        while( (inputLine = br.readLine()) != null) {
            String Data[] =  inputLine.split(",");
            int i = 0;
            for(Attributes f : Attributes.values()) {
                if(!dataSet.containsKey(f)) {
                    dataSet.put(f, new ArrayList<>());
                }
                dataSet.get(f).add(Data[i].trim());
                i++;
            }
        }
        return dataSet;

    }

    //Iterates over CSV file.
    private List<Integer> getRow(List<String> strList, String value) {
        List<Integer> rowList = new ArrayList<>();
        int i = 0;
        for(String str : strList) {
            if(str.equals(value)) {
                rowList.add(i);
            }
            i++;
        }
        return rowList;
    }

    //Plurality value incase examples or attributes don't exist
    private String PluralityValue(List<String> domainValues) {
        Random rand = new Random();
        Map<String,Integer> countMap = new HashMap<>();
        for(String str : domainValues) {
            if(!countMap.containsKey(str)) {
                countMap.put(str, 0);
            }
            countMap.put(str, countMap.get(str) +1);
        }
        int max = -1;
        String retStr = null;
        for(Map.Entry<String, Integer> entry : countMap.entrySet()) {
            if(max == entry.getValue()) {
                retStr =  rand.nextBoolean() ? entry.getKey() : retStr;
            }
            if(max < entry.getValue()) {
                max  = entry.getValue();
                retStr = entry.getKey();
            }
        }
        return  retStr;
    }

    //DepthFIrst expansion of tree nodes.
    private Node DFS(Map<Attributes, List<String>> dataSet) {
        if(dataSet.size() == 1) {
            Node node = new Node(PluralityValue(dataSet.get(Attributes.WILL_WAIT)),true);
            return node;
        }

        if(isClean(dataSet.get(Attributes.WILL_WAIT))) {
            Node node = new Node(dataSet.get(Attributes.WILL_WAIT).get(0),true);
            return node;
        }


        Double maxInfoGain = -Double.MAX_VALUE;
        double targetEntropy = Entropy(dataSet.get(Attributes.WILL_WAIT));
        Attributes maxGainAttributes = null;
        for(Attributes f : dataSet.keySet()) {
            if(f.equals(Attributes.WILL_WAIT)) {
                continue;
            }
            Double infoGain_i = InfoGain(targetEntropy, dataSet.get(f), dataSet.get(Attributes.WILL_WAIT));
            if(maxInfoGain == null || maxInfoGain < infoGain_i) {
                maxGainAttributes = f;
                maxInfoGain = infoGain_i;
            }
        }
        Node rootNode = new Node(maxGainAttributes.toString(), false) ;
        Set<String> featureDomain = new HashSet<>(dataSet.get(maxGainAttributes));
        for(String domainValue : featureDomain) {

            Map<Attributes, List<String>> filteredDataSet = copy(dataSet);
            List<Integer> rowIndex = getRow(dataSet.get(maxGainAttributes), domainValue);
            filter(filteredDataSet, rowIndex);
            filteredDataSet.remove(maxGainAttributes);

            Node node =  DFS( filteredDataSet);
            rootNode.getChildNodes().put(domainValue, node);
        }

        return rootNode;
    }

    //FIlters rows eliminating the pureset.
    private void filter(Map<Attributes, List<String>> dataSet, List<Integer> rowList) {
        for(Attributes f : dataSet.keySet()) {
            List<String> filteredValues = new ArrayList<>();
            for(Integer row : rowList) {
                filteredValues.add(dataSet.get(f).get(row));
            }
            dataSet.put(f,filteredValues);
        }
    }

    private boolean isClean(List<String> resultList) {
        String str = resultList.get(0);
        for (String compareStr : resultList) {
            if (!compareStr.equals(str))
                return false;
        }
        return true;
    }

    public  void run() throws Exception {
        Map<Attributes, List<String>> dataSet = readData();
        Node root = DFS(dataSet);
        print(null, root, 0);
        System.out.println("");
    }

    //Log value for Entropy.
    private double Log2(double value) {
        return Math.log(value) / Math.log(2);
    }

    //Calculates infogain=target_entropy-min(feature_entropy)
    private double InfoGain(double rootEntropy, List<String> featureValues, List<String> targetFeatureValues) {
        Set<String> domainValues = new HashSet<>(featureValues);
        Double totalEntropy = 0.0;
        int size = featureValues.size();
        for(String domain : domainValues) {
            List<String> targetSubset = new ArrayList<>();
            int domainCount = 0;
            int i = 0;
            for(String str : featureValues) {
                if(str.equals(domain)) {
                    targetSubset.add(targetFeatureValues.get(i));
                    domainCount++;
                }
                i++;
            }
            double domainEntropy = domainCount/(double)size * Entropy(targetSubset);
            totalEntropy += domainEntropy;
        }
        return  rootEntropy - totalEntropy;
    }

    // sum of -P * log2(P)
    private double Entropy(List<String> featureValues) {
        Map<String, Integer> valueCountMap = new HashMap<>();

        for(String str : featureValues ) {
            if(!valueCountMap.containsKey(str)) {
                valueCountMap.put(str,0);
            }
            valueCountMap.put(str, valueCountMap.get(str) + 1);
        }
        int size = featureValues.size();
        double entropy = 0.0;
        for(Map.Entry<String, Integer> valueCountEntry : valueCountMap.entrySet()) {
            double prob = valueCountEntry.getValue() /(double)size;
            double valueEntropy = -prob*Log2(prob);
            entropy += valueEntropy;
        }
        return entropy;
    }

    //Copy data
    private Map<Attributes,List<String>> copy(Map<Attributes, List<String>> featureListMap) {
        Map<Attributes,List<String>> featureMap = new HashMap<>();
        for(Map.Entry<Attributes, List<String>> entry : featureListMap.entrySet()) {
            featureMap.put(entry.getKey(), copy(entry.getValue()));
        }
        return featureMap;
    }

    //Copy data
    private List<String> copy(List<String> strList) {
        List<String> arr = new ArrayList<>();
        for(String str: strList) {
            arr.add(str);
        }
        return arr;
    }

    //Prints output tree structure.
    private void print(String domainValue, Node node, int spacing) {
        System.out.print("\n");
        for(int i = 0; i<spacing-1; i++) {
            System.out.print("!   ");
        }
        if(spacing > 0)
            System.out.print("!===");
        if(domainValue !=null) {
            System.out.print(  domainValue + " <<<<<");
        }
        System.out.print(node.getData());
        for(Map.Entry<String, Node> entry : node.getChildNodes().entrySet()) {
            print(entry.getKey(), entry.getValue(), spacing + 1);
        }
    }

    public static void main(String arg[]) throws Exception {
        decisiontree s = new decisiontree();
        s.run();

    }

    //Describes the members and functions of every node in tree

    class Node {
        private String data;
        //    private String data;
        private boolean isPureDataSet;
        private Map<String, Node> childNodes;

        public Node(String data, boolean isPureDataSet) {
            this.data = data;
            this.isPureDataSet = isPureDataSet;
            childNodes = new HashMap<>();
        }

        public String getData() {
            return data;
        }

        public Map<String, Node> getChildNodes() {
            return childNodes;
        }

        @Override
        public String toString() {
            return data +  " "  + isPureDataSet;
        }
    }

}
