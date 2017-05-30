/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import edu.virginia.cs.utility.FileOperations;
import edu.virginia.cs.utility.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.search.BooleanQuery;

/**
 *
 * @author Wasi
 */
public class BuildTopicModel {

    private final HashMap<String, Integer> dictionary;
    private final HashMap<String, Integer> dictionaryWithTF;
    private int numberOfWordsInDictionary = 0;
    private final FileOperations fiop;

    public BuildTopicModel() throws IOException {
        fiop = new FileOperations();
        dictionary = new HashMap<>();
        dictionaryWithTF = new HashMap<>();
        BooleanQuery.setMaxClauseCount(2048);
    }

    /**
     * Method that analyzes a document.
     *
     * @param document unsorted Map
     * @throws java.io.IOException
     */
    private void analyzeDocument(String document) throws IOException {
        HashMap<String, Integer> tempRecord = new HashMap<>();
        String previousToken = "";
        List<String> tokens = StringTokenizer.TokenizeString(document);
        for (String token : tokens) {
            if (!token.isEmpty()) {
                if (!dictionary.containsKey(token)) {
                    dictionary.put(token, numberOfWordsInDictionary);
                    numberOfWordsInDictionary++;
                }
                if (tempRecord.containsKey(token)) {
                    tempRecord.put(token, tempRecord.get(token) + 1);
                } else {
                    tempRecord.put(token, 1);
                }
                // generating bigrams
                if (!previousToken.isEmpty()) {
                    String bigram = previousToken + " " + token;
                    if (!dictionary.containsKey(bigram)) {
                        dictionary.put(bigram, numberOfWordsInDictionary);
                        numberOfWordsInDictionary++;
                    }
                    if (tempRecord.containsKey(bigram)) {
                        tempRecord.put(bigram, tempRecord.get(bigram) + 1);
                    } else {
                        tempRecord.put(bigram, 1);
                    }
                }

                previousToken = token;
            }
        }
        String line = String.valueOf(tempRecord.size());
        for (Map.Entry<String, Integer> entry : tempRecord.entrySet()) {
            line = line + " " + dictionary.get(entry.getKey()) + ":" + entry.getValue();
            if (dictionaryWithTF.containsKey(entry.getKey())) {
                int tempFreq = entry.getValue() + dictionaryWithTF.get(entry.getKey());
                dictionaryWithTF.put(entry.getKey(), tempFreq);
            } else {
                dictionaryWithTF.put(entry.getKey(), entry.getValue());
            }
        }
        FileWriter fwriter = new FileWriter("documentRecord.dat", true);
        fwriter.write(line + "\n");
        fwriter.close();
    }

    /**
     * Loads a file.
     * 
     * @param filename
     * @return
     */
    private String LoadFile(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            StringBuilder builder = new StringBuilder(1024);
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    builder.append(line);
                    builder.append("\n");
                }
            }
            reader.close();
            return builder.toString();
        } catch (IOException e) {
            System.err.format("[Error]Failed to open file %s!", filename);
            return null;
        }
    }

    /**
     * Method that loads all the documents from a directory.
     *
     * @param folder path of the directory
     * @throws java.Throwable
     */
    private void LoadDirectory(String folder) throws Throwable {
        int numberOfDocumentsLoaded = 0;
        File dir = new File(folder);
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                numberOfDocumentsLoaded++;
                analyzeDocument(LoadFile(f.getAbsolutePath()));
            } else if (f.isDirectory()) {
                LoadDirectory(f.getAbsolutePath());
            }
        }
        System.out.println("Loading " + numberOfDocumentsLoaded + " documents from " + folder);
        writeDictionaryToFile("dictionary.txt", 1);
        writeDictionaryToFile("dictionaryWithFrequency.txt", 2);
    }

    /**
     * Method that generate the id of all users for evaluation.
     *
     * @param unsortMap unsorted Map
     * @param order if true, then sort in ascending order, otherwise in
     * descending order
     */
    private void sortByComparator(HashMap<String, Integer> unsortedMap, boolean order, String filename, int choice) throws Throwable {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortedMap.entrySet());
        Collections.sort(list, (Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) -> {
            if (order) {
                return o1.getValue().compareTo(o2.getValue());
            } else {
                return o2.getValue().compareTo(o1.getValue());

            }
        });
        fiop.appnedToFile(filename, list, choice);
    }

    /**
     * Method that stores the dictionary into file.
     *
     * @param filename
     * @param choice if choice = 1, then store only words, otherwise store words
     * with term frequency
     */
    private void writeDictionaryToFile(String filename, int choice) throws Throwable {
        if (choice == 1) {
            sortByComparator(dictionary, true, filename, choice);
        } else {
            sortByComparator(dictionaryWithTF, true, filename, choice);
        }
    }

    /**
     * Main method.
     *
     * @param args command line arguments
     * @throws java.lang.Throwable
     */
    public static void main(String[] args) throws Throwable {
        // TODO code application logic here
        BuildTopicModel tmodel = new BuildTopicModel();
        tmodel.LoadDirectory("./data/bbc/");
    }
}
