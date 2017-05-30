/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import edu.virginia.cs.utility.FileOperations;
import edu.virginia.cs.utility.SpecialAnalyzer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

/**
 *
 * @author Wasi
 */
public class QueryTopicInference {

    private final HashMap<String, Integer> dictionary;
    private final FileOperations fiop;
    private final SpecialAnalyzer analyzer;
    private final QueryParser parser;

    public QueryTopicInference() throws IOException {
        fiop = new FileOperations();
        dictionary = new HashMap<>();
        analyzer = new SpecialAnalyzer();
        parser = new QueryParser(Version.LUCENE_46, "", analyzer);
    }

    /**
     * Load BBC dictionary.
     * 
     * @param filename 
     */
    private void LoadDictionary(String filename) {
        ArrayList<String> lines = fiop.LoadFile(filename, -1);
        int wordCount = 0;
        for (String line : lines) {
            dictionary.put(line, wordCount);
            wordCount++;
        }
    }

    /**
     * Method that run topic model to infer topic of the user submitted query.
     *
     * @throws java.io.IOException
     */
    private void runTopicInference(String threadId) throws IOException {
        String queryFile = "./lda-output-files/query" + "_" + threadId + ".dat";
        String resultFile = "./lda-output-files/result" + "_" + threadId;
        String command = "lda-win64 inf settings.txt topic_model/final " + queryFile + " " + resultFile;
        Process proc = Runtime.getRuntime().exec(command);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        String s;
        while ((s = stdInput.readLine()) != null) {
        }

        // read any errors from the attempted command
        s = stdError.readLine();
        if (s != null) {
            System.out.println("\nHere is the standard error of the command (if any):\n");
        }
        while (s != null) {
            System.out.println(s);
            s = stdError.readLine();
        }
    }

    /**
     * Method that pre-process all the queries of a user.
     *
     * @param allQueries list of all query of a user
     * @throws java.io.IOException
     */
    private void ProcessQueryLog(ArrayList<String> allQueries, String threadId) throws IOException {
        File file = new File("./lda-output-files/query" + "_" + threadId + ".dat");
        BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
        for (String query : allQueries) {
            String processedResult = ProcessDocument(query);
            bw.write(processedResult + "\n");
        }
        bw.close();
    }

    /**
     * Method that pre-process and run the topic inference for all the queries
     * of a user.
     *
     * @param allQueries list of all query of a user
     * @param threadId
     * @throws java.io.IOException
     */
    public void initializeGeneration(ArrayList<String> allQueries, String threadId) throws IOException {
        LoadDictionary("dictionary.txt");
        ProcessQueryLog(allQueries, threadId);
        runTopicInference(threadId);
    }

    /**
     * The main method that creates and starts threads.
     *
     * @param document
     * @return one line representation of the user query
     * @throws java.io.IOException
     */
    public String ProcessDocument(String document) throws IOException {
        String line = null;
        try {
            HashMap<String, Integer> tempRecord = new HashMap<>();
            Query textQuery = parser.parse(QueryParser.escape(document));
            String[] tokens = textQuery.toString().split(" ");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    if (dictionary.containsKey(token)) {
                        if (tempRecord.containsKey(token)) {
                            tempRecord.put(token, tempRecord.get(token) + 1);
                        } else {
                            tempRecord.put(token, 1);
                        }
                    }
                }
            }
            line = String.valueOf(tempRecord.size());
            for (Map.Entry<String, Integer> entry : tempRecord.entrySet()) {
                line = line + " " + dictionary.get(entry.getKey()) + ":" + entry.getValue();
            }

        } catch (ParseException ex) {
            Logger.getLogger(QueryTopicInference.class.getName()).log(Level.SEVERE, null, ex);
        }
        return line;
    }
}
