/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.searchlog;

import edu.virginia.cs.index.Searcher;
import edu.virginia.cs.utility.SpecialAnalyzer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import edu.virginia.cs.utility.StringTokenizer;
import java.util.ArrayList;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 *
 * @author Wasi
 */
public class UpdateSearchLog {

    private final String _indexPath = "lucene-AOL-index";
    private HashMap<String, HashSet<String>> queryToJudgement;
    private ArrayList<String> listQueries;
    private Searcher _searcher = null;
    private final SpecialAnalyzer analyzer;
    private final QueryParser parser;

    public UpdateSearchLog() {
        _searcher = new Searcher(_indexPath);
        analyzer = new SpecialAnalyzer();
        parser = new QueryParser(Version.LUCENE_46, "", analyzer);
    }

    /**
     *
     * @param filename
     * @param id
     */
    private void LoadFile(String filename, String id) {
        queryToJudgement = new HashMap<>();
        listQueries = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] query = line.split("\t");
                Query textQuery = parser.parse(QueryParser.escape(query[0]));
                String[] qParts = textQuery.toString().split(" ");

                String url = reader.readLine();
                String docContent = _searcher.search(url, "clicked_url");
                HashSet<String> tokSet = new HashSet<>(StringTokenizer.TokenizeString(docContent));

                int tokMatched = 0;
                for (String part : qParts) {
                    if (tokSet.contains(part)) {
                        tokMatched++;
                    }
                }
                if (tokMatched == 0) {
                    continue;
                }

                HashSet<String> temp;
                if (queryToJudgement.containsKey(line)) {
                    temp = queryToJudgement.get(line);
                    temp.add(url);
                } else {
                    listQueries.add(line);
                    temp = new HashSet<>();
                    temp.add(url);
                    queryToJudgement.put(line, temp);
                }
            }
            reader.close();
        } catch (IOException | ParseException e) {
            System.err.format("[Error]Failed to open file %s!", filename);
        }
        WriteToFile("./data/updated_search_log(top 1000)/" + id + ".txt");
    }

    /**
     *
     * @param filename
     */
    private void WriteToFile(String filename) {
        try {
            FileWriter fw = new FileWriter(filename);
            for (String query : listQueries) {
                fw.write(query + "\n");
                for (String str : queryToJudgement.get(query)) {
                    fw.write(str + "\n");
                }
                fw.write("\n");
            }
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(UpdateSearchLog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @param folder
     * @throws Throwable
     */
    private void LoadDirectory(String folder) throws Throwable {
        int numberOfDocumentsLoaded = 0;
        File dir = new File(folder);
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                numberOfDocumentsLoaded++;
                String filename = f.getName();
                LoadFile(f.getAbsolutePath(), filename.substring(0, filename.lastIndexOf(".")));
            } else if (f.isDirectory()) {
                LoadDirectory(f.getAbsolutePath());
            }
        }
        System.out.println("Loading " + numberOfDocumentsLoaded + " documents from " + folder);
    }

    public static void main(String[] args) throws Throwable {
        UpdateSearchLog upBuilder = new UpdateSearchLog();
        upBuilder.LoadDirectory("./data/search_log(top 1000)/");
    }

}
