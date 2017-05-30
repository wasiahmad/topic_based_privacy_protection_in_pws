package edu.virginia.cs.eval;

import java.io.IOException;
import java.util.ArrayList;
import edu.virginia.cs.utility.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SemanticEvaluation {

    /* data structure for mutual information measurement */
    private final HashMap<String, HashSet<Integer>> dictionaryWords;
    private int totalDocumentCount;

    public SemanticEvaluation(String filename) {
        dictionaryWords = new HashMap<>();
        doinitiliazation(filename);
    }

    /**
     * Load probabilities of BBC dictionary tokens.
     */
    private void doinitiliazation(String filename) {
        BufferedReader br;
        try {
            String line;
            br = new BufferedReader(new FileReader(filename));
            /* reading number of documents in BBC dataset */
            totalDocumentCount = Integer.parseInt(br.readLine());
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\t");
                String key = tokens[0];
                HashSet<Integer> tempSet = new HashSet<>();
                for (int i = 1; i < tokens.length; i++) {
                    int docNo = Integer.parseInt(tokens[i]);
                    tempSet.add(docNo);
                }
                dictionaryWords.put(key, tempSet);
            }
            br.close();
        } catch (IOException | NumberFormatException ex) {
            Logger.getLogger(SemanticEvaluation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Computing probability for a query.
     *
     * @param query
     * @return
     */
    private double getProbability(String query) {
        double probQuery = 0;
        try {
            if (query.isEmpty()) {
                return 0;
            }
            List<String> tokens = StringTokenizer.TokenizeString(query);
            if (tokens.size() > 0) {
                int docFreqCount = getDocFrequency(tokens);
                probQuery = (docFreqCount * 1.0) / totalDocumentCount;
            }
        } catch (Exception ex) {
            Logger.getLogger(SemanticEvaluation.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(query);
        }
        return probQuery;
    }

    /**
     *
     * @param param1
     * @param param2
     * @return
     */
    private int getDocFrequency(List<String> tokens) {
        for (String token : tokens) {
            if (!dictionaryWords.containsKey(token)) {
                return 0;
            }
        }
        HashSet<Integer> set1 = dictionaryWords.get(tokens.get(0));
        HashSet<Integer> intersection = new HashSet<>(set1);
        for (int i = 1; i < tokens.size(); i++) {
            HashSet<Integer> set2 = dictionaryWords.get(tokens.get(i));
            intersection.retainAll(set2);
        }
        return intersection.size();
    }

    /**
     * Computing normalized mutual information between set of true queries and
     * cover queries.
     *
     * @param origQuery
     * @param coverQuery
     * @return
     */
    public double calculateNMI(ArrayList<String> origQuery, ArrayList<String> coverQuery) {
        HashMap<String, Double> Px = new HashMap<>();
        HashMap<String, Double> Py = new HashMap<>();
        HashMap<String, Double> Pxy = new HashMap<>();
        /* computing P(x) */
        for (String qr : origQuery) {
            double prob = getProbability(qr);
            Px.put(qr, prob);
        }
        /* computing P(y) */
        for (String qr : coverQuery) {
            double prob = getProbability(qr);
            Py.put(qr, prob);
        }
        /* computing P(x, y) */
        for (String origQuery1 : origQuery) {
            for (String coverQuery1 : coverQuery) {
                String combineQuery = origQuery1 + " " + coverQuery1;
                double prob = getProbability(combineQuery);
                Pxy.put(combineQuery, prob);
            }
        }
        /* computing mutual information */
        double muInfo = 0;
        for (String origQuery1 : origQuery) {
            for (String coverQuery1 : coverQuery) {
                String combineQuery = origQuery1 + " " + coverQuery1;
                double pxy = Pxy.get(combineQuery);
                double px = Px.get(origQuery1);
                double py = Py.get(coverQuery1);
                if (pxy > 0 && px > 0 && py > 0) {
                    double partER = pxy * ((Math.log10(pxy / (px * py))) / Math.log10(2));
                    muInfo += partER;
                }
            }
        }
        return muInfo;
    }

}
