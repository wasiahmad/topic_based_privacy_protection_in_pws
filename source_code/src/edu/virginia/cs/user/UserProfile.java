/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.user;

import edu.virginia.cs.utility.SpecialAnalyzer;
import edu.virginia.cs.utility.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

/**
 *
 * @author wasi
 */
public class UserProfile {

    private final HashMap<String, Integer> uProfile;
    private final HashMap<String, Double> IDFRecord;
    private HashMap<String, Float> referenceModel;
    private int totalTokens;
    private final QueryParser parser;
    private double totalQueryLength;
    private double totalQuery;

    public UserProfile() {
        uProfile = new HashMap<>();
        IDFRecord = new HashMap<>();
        loadIDFRecord("./data/AOL-Dictionary.txt");

        totalTokens = 0;
        totalQuery = 0;
        totalQueryLength = 0;

        SpecialAnalyzer analyzer = new SpecialAnalyzer();
        parser = new QueryParser(Version.LUCENE_46, "", analyzer);
        BooleanQuery.setMaxClauseCount(2048);
    }

    private void loadIDFRecord(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
            double totalCount = Double.valueOf(br.readLine());
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\t");
                double docFreq = 1 + Math.log10(totalCount / Double.valueOf(split[1]));
                IDFRecord.put(split[0], docFreq);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(UserProfile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UserProfile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @return
     */
    public int getTotalTokenCount() {
        return totalTokens;
    }

    /**
     *
     * @param rModel
     */
    public void setReferenceModel(HashMap<String, Float> rModel) {
        referenceModel = rModel;
//        for (String str : rModel.keySet()) {
//            uProfile.put(str, 0);
//        }
    }

    public HashMap<String, Float> getReferenceModel() {
        return referenceModel;
    }

    /**
     * Return the user profile.
     *
     * @return a mapping of words to their term frequency.
     */
    public HashMap<String, Integer> getUserProfile() {
        return uProfile;
    }

    /**
     * Update user profile by the user submitted query.
     *
     * @param queryText
     * @throws java.io.IOException
     */
    public void updateUserProfile(String queryText)
            throws IOException {
        try {
            Query textQuery = parser.parse(QueryParser.escape(queryText));
            String[] qParts = textQuery.toString().split(" ");
            totalQuery++;
            totalQueryLength = totalQueryLength + qParts.length;
            for (String qPart : qParts) {
                if (qPart.isEmpty()) {
                    continue;
                }
                totalTokens++;//for every token
                Integer n = uProfile.get(qPart);
                n = (n == null) ? 1 : ++n;
                uProfile.put(qPart, n);
            }
        } catch (ParseException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Update user profile by the content of the user clicked document.
     *
     * @param content
     * @throws java.io.IOException
     */
    public void updateUserProfileUsingClickedDocument(String content)
            throws IOException {
        List<String> tokens = StringTokenizer.TokenizeString(content);
        /* To update user profile, select the top 10 tokens using tf-idf weight */
        HashMap<String, Integer> retVal = selectTopKtokens(tokens, 10);
        for (Map.Entry<String, Integer> entry : retVal.entrySet()) {
            totalTokens += entry.getValue();
            Integer n = uProfile.get(entry.getKey());
            n = (n == null) ? entry.getValue() : (n + entry.getValue());
            uProfile.put(entry.getKey(), n);
        }
    }

    /**
     * Returns average length of all queries submitted by the user. Default
     * value is 3.
     *
     * @return average query length
     */
    public double getAvgQueryLength() {
        if (totalQuery < 1) {
            return 3;
        }
        return totalQueryLength / totalQuery;
    }

    /**
     * Computes KL-Divergence between two different user profiles.
     *
     * @param otherProfile
     * @param otherProfileTokenCount
     * @return KL-Divergence value
     */
    public float calculateKLDivergence(HashMap<String, Integer> otherProfile, int otherProfileTokenCount) {
        float klDiv = 0;
        float lambda = 0.1f;
        HashSet<String> keySet = new HashSet<>();
        keySet.addAll(otherProfile.keySet());
        keySet.addAll(uProfile.keySet());
        for (String name : keySet) {
            Integer value = otherProfile.get(name);
            if (value == null) {
                value = 0;
            }
            Float tokenProb = (value * 1.0f) / otherProfileTokenCount;
            Float refProb = referenceModel.get(name);
            if (refProb == null) {
                refProb = 0.0f;
            }
            Float smoothedTokenProb = (1 - lambda) * tokenProb + lambda * refProb;
            Float p1 = smoothedTokenProb;

            Integer value2 = uProfile.get(name);
            if (value2 == null) {
                value2 = 0;
            }
            Float tokenProb2 = (value2 * 1.0f) / totalTokens;
            Float refProb2 = refProb;

            Float smoothedTokenProb2 = (1 - lambda) * tokenProb2 + lambda * refProb2;
            Float p2 = smoothedTokenProb2;
            if (p1 == 0) {
                continue;
            }
            if (p2 == 0) {
                continue;
            }
            klDiv = (float) (klDiv + p1 * Math.log(p1 / p2));
        }
        return klDiv;
    }

    /**
     * Method that returns the top k tokens from a list of tokens. Tokens are
     * ranked based on their tf-idf value.
     *
     * @param tokenList list of tokens
     * @param k return only the top k elements
     * @return top k tokens with their term frequency
     */
    private HashMap<String, Integer> selectTopKtokens(List<String> tokenList, int k) {
        HashMap<String, Integer> retValue = new HashMap<>();
        HashMap<String, Integer> tempMap = new HashMap<>();
        /* Stores tf-idf weight of all tokens */
        HashMap<String, Float> unsortedMap = new HashMap<>();
        for (String token : tokenList) {
            Integer n = tempMap.get(token);
            n = (n == null) ? 1 : ++n;
            tempMap.put(token, n);
        }
        for (Map.Entry<String, Integer> entry : tempMap.entrySet()) {
            Double idf = IDFRecord.get(entry.getKey());
            if (idf == null) {
                idf = 0.0;
            }
            double tfIdfWeight = entry.getValue() * idf;
            unsortedMap.put(entry.getKey(), (float) tfIdfWeight);
        }
        HashMap<String, Float> temp = sortByComparator(unsortedMap, false, k);
        for (Map.Entry<String, Float> entry : temp.entrySet()) {
            retValue.put(entry.getKey(), tempMap.get(entry.getKey()));
        }
        return retValue;
    }

    /**
     * Method that generate the id of all users for evaluation.
     *
     * @param unsortMap unsorted Map
     * @param order if true, then sort in ascending order, otherwise in
     * descending order
     * @param k return only the top k elements
     * @return sorted Map of k elements
     */
    private HashMap<String, Float> sortByComparator(Map<String, Float> unsortMap, final boolean order, int k) {
        List<Map.Entry<String, Float>> list = new LinkedList<>(unsortMap.entrySet());
        // Sorting the list based on values
        Collections.sort(list, (Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) -> {
            if (order) {
                return o1.getValue().compareTo(o2.getValue());
            } else {
                return o2.getValue().compareTo(o1.getValue());

            }
        });
        // Maintaining insertion order with the help of LinkedList
        HashMap<String, Float> sortedMap = new LinkedHashMap<>();
        int i = 0;
        for (Map.Entry<String, Float> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
            i++;
            if (i == k) {
                break;
            }
        }
        return sortedMap;
    }
}
