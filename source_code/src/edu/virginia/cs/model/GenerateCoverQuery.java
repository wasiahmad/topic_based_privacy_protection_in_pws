/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import edu.virginia.cs.utility.FileOperations;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Wasi
 */
public class GenerateCoverQuery {

    private final FileOperations fiop;
    private final ArrayList<String> topicList;
    private final HashMap<String, HashMap<String, Double>> mapTopicToWords;

    private double entropyRange;

    public GenerateCoverQuery() {
        fiop = new FileOperations();
        mapTopicToWords = new HashMap<>();
        topicList = new ArrayList<>();
    }

    public void setEntropy(double val) {
        entropyRange = val;
    }

    /**
     * Method that generates a Poisson random number to vary cover query length.
     *
     * @param lambda average length of queries submitted by a user
     * @return length of a cover query
     */
    private int getPoisson(double lambda) {
        int n = 1;
        double prob = 1.0;
        Random r = new Random();

        while (true) {
            prob *= r.nextDouble();
            if (prob < Math.exp(-lambda)) {
                break;
            }
            n += 1;
        }
        return n - 1;
    }

    /**
     * Method that checks whether cover query entropy in between a range or not.
     *
     * @param prob
     * @param entropy
     * @return boolean
     */
    private boolean checkEntropy(double prob, double entropy) {
        double qEntropy = -1 * prob * Math.log(prob);
        double upper = entropy + entropyRange;
        double lower = entropy - entropyRange;
        if (qEntropy <= upper && qEntropy >= lower) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method that computes entropy of a generated cover query.
     *
     * @param probability
     * @return entropy
     */
    private double calculateQueryEntropy(double probability) {
        double entropy = -1 * probability * Math.log(probability);
        return entropy;
    }

    /**
     * Method to generate cover query using unigram language model.
     *
     * @param probOfUnigrams
     * @param documentLength
     * @param origEntropy
     * @return
     */
    private String generateQuerytFromLangModel(HashMap<String, Double> probOfUnigrams, double documentLength, double origEntropy) {
        Random rand = new Random();
        int docLength = 0;
        int fakeQueryLength;
        while (true) {
            fakeQueryLength = getPoisson(documentLength);
            if (fakeQueryLength > 0) {
                break;
            }
        }
        String document = null;
        double probability = 1.0;
        int numberOfAttempt = 0;
        while (true) {
            if (docLength == fakeQueryLength) {
                if (!checkEntropy(probability, origEntropy)) {
                    if (numberOfAttempt == 10) {
                        break;
                    }
                    docLength = 0;
                    document = null;
                    probability = 1.0;
                    numberOfAttempt++;
                } else {
                    break;
                }
            }
            double randValue = rand.nextDouble();
            double temp = 0.0;
            for (Map.Entry<String, Double> entry : probOfUnigrams.entrySet()) {
                temp = temp + entry.getValue();
                if (temp >= randValue) {
                    docLength++;
                    probability = probability * entry.getValue();
                    if (document == null) {
                        document = entry.getKey();
                    } else {
                        document = document + " " + entry.getKey();
                    }
                    break;
                }
            }
        }
        return document;
    }

    /**
     * Method that stores all topical words in HashMap.
     *
     * @param folder
     */
    private void storeWordInMap(String topic, ArrayList<String> lines) {
        HashMap<String, Double> tempMap = new HashMap<>();
        for (String line : lines) {
            String words[] = line.split(" ");
            if (words.length == 2) {
                tempMap.put(words[0], Double.valueOf(words[1]));
            } else if (words.length == 3) {
                String temp = words[0] + " " + words[1];
                tempMap.put(temp, Double.valueOf(words[2]));
            } else {
                System.out.println("Exception found when loading topic words!!!");
            }
        }
        topicList.add(topic);
        mapTopicToWords.put(topic, tempMap);
    }

    /**
     * Method that loads all the topical words.
     *
     * @param folder
     */
    public void loadTopicWords(String folder) {
        File dir = new File(folder);
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                String filename = f.getName();
                int pos = filename.lastIndexOf(".");
                String fname = pos > 0 ? filename.substring(0, pos) : filename;
                storeWordInMap(fname, fiop.LoadFile(f.getAbsolutePath(), -1));
            } else if (f.isDirectory()) {
                loadTopicWords(f.getAbsolutePath());
            }
        }
    }

    /**
     * Method converts string to Double[].
     *
     * @param line
     */
    private Double[] topicPercentage(String line) {
        String words[] = line.split(" ");
        Double[] retValue = new Double[words.length];
        Double totalVal = 0.0;
        for (int i = 0; i < words.length; i++) {
            Double probab = Double.valueOf(words[i]);
            totalVal = totalVal + probab;
            retValue[i] = probab;
        }
        for (int i = 0; i < retValue.length; i++) {
            retValue[i] = retValue[i] / totalVal;
        }
        return retValue;
    }

    /**
     * Method that computes average precision of a user submitted query.
     *
     * @param queryTopicProb query topic probability for each topic
     * @param n number of cover query
     * @param avgQueryLength required for to vary cover query length
     * @return a list of cover queries
     */
    public ArrayList<String> generateCoverQueries(String queryTopicProb, int n, double avgQueryLength) {
        ArrayList<String> retValue = new ArrayList<>();
        Random rand = new Random();
        Double[] percentage = topicPercentage(queryTopicProb);
        Double[] negativeExponent = new Double[percentage.length];
        for (int i = 0; i < percentage.length; i++) {
            negativeExponent[i] = Math.exp(-percentage[i]);
        }

        for (int i = 0; i < n; i++) {
            double randValue = rand.nextDouble();
            double temp = 0.0;
            for (int j = 0; j < negativeExponent.length; j++) {
                temp = temp + negativeExponent[j];
                if (temp >= randValue) {
                    double entropy = calculateQueryEntropy(percentage[j]);
                    retValue.add(generateQuerytFromLangModel(mapTopicToWords.get(topicList.get(j)), avgQueryLength, entropy));
                    break;
                }
            }
        }
        return retValue;
    }

}
