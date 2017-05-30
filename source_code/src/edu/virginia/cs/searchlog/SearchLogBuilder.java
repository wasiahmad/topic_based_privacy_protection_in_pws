/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.searchlog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import edu.virginia.cs.utility.FileOperations;
import java.io.FileWriter;

/**
 *
 * @author Wasi
 */
public class SearchLogBuilder {

    private final FileOperations fiop;
    private final HashMap<String, ArrayList<String>> userProfile;
    private final HashMap<String, String> crawledData;
    private final HashSet<String> aliveURLs;

    public SearchLogBuilder() {
        fiop = new FileOperations();
        userProfile = new HashMap<>();
        crawledData = new HashMap<>();
        aliveURLs = new HashSet<>();
    }

    /**
     *
     * @param unsortedMap
     * @param order
     * @param skip
     * @param top
     * @throws Throwable
     */
    private void sortByComparator(HashMap<String, Integer> unsortedMap, boolean order, int skip, int top) throws Throwable {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortedMap.entrySet());
        Collections.sort(list, (Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) -> {
            if (order) {
                return o1.getValue().compareTo(o2.getValue());
            } else {
                return o2.getValue().compareTo(o1.getValue());

            }
        });
        int i = 0;
        System.out.println("Started writing to file");
        for (Map.Entry<String, Integer> entry : list) {
            if (i < (skip + 50)) {
                i++;
            } else if (i < (50 + skip + top)) {
                i++;
                ArrayList<String> record = userProfile.get(entry.getKey());
                System.out.println(record.size());
                String fileName = "data/search_log(top " + top + ")/" + entry.getKey() + ".txt";
                FileWriter fw = new FileWriter(fileName);
                for (String str : record) {
                    String[] words = str.split("::");
                    String s = words[0] + "\t" + words[2] + "\n" + words[1];
                    fw.write(s + "\n");
                    fw.flush();
                }
                fw.close();
            }
        }
    }

    /**
     *
     * @param content
     */
    private void generateUserProfile(ArrayList<String> lines) {
        boolean flag = true;
        for (String line : lines) {
            if (flag) {
                flag = false;
                continue;
            }
            String words[] = line.split("\t");
            if (words.length >= 2) {
                String user_id = words[0].trim();
                String user_query = words[1].trim();
                if (!user_id.isEmpty() && !user_query.isEmpty()) {
                    ArrayList<String> temp;
                    if (userProfile.containsKey(user_id)) {
                        temp = userProfile.get(user_id);
                    } else {
                        temp = new ArrayList<>();
                    }
                    String clicked_url;
                    if (words.length >= 5) {
                        clicked_url = words[4].trim();
                        clicked_url += "/";
                        if (aliveURLs.contains(clicked_url)) {
                            String timestamp = words[2].trim();
                            temp.add(user_query + "::" + clicked_url + "::" + timestamp);
                            userProfile.put(user_id, temp);
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param folder
     * @param choice
     * @throws Throwable
     */
    private void LoadDirectory(String folder, int choice) throws Throwable {
        ArrayList<String> lines = fiop.LoadFile("data/urls/alive_urls.txt", -1);
        int i = 0;
        while (i < lines.size()) {
            aliveURLs.add(lines.get(i));
            i++;
        }

        int numberOfDocumentsLoaded = 0;
        File dir = new File(folder);
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                numberOfDocumentsLoaded++;
                if (choice == 1) {
                    generateUserProfile(fiop.LoadFile(f.getAbsolutePath(), -1));
                } else if (choice == 2) {
                    loadFile(fiop.LoadFile(f.getAbsolutePath(), -1));
                }
            } else if (f.isDirectory()) {
                LoadDirectory(f.getAbsolutePath(), choice);
            }
        }

        System.out.println("Loading " + numberOfDocumentsLoaded + " documents from " + folder);
        if (choice == 1) {
            System.out.println("Total Number of Unique Profiles - " + userProfile.size());
        }
    }

    /**
     *
     * @param skip
     * @param top
     * @throws Throwable
     */
    private void generateTopUserProfile(int skip, int top) throws Throwable {
        HashMap<String, Integer> tempMap = new HashMap<>();
        for (Map.Entry<String, ArrayList<String>> entry : userProfile.entrySet()) {
            tempMap.put(entry.getKey(), entry.getValue().size());
        }
        sortByComparator(tempMap, false, skip, top);
    }

    /**
     *
     * @param param
     */
    private void loadFile(ArrayList<String> lines) {
        System.out.println("Loading file...");
        int i = 0;
        while (i < lines.size()) {
            crawledData.put(lines.get(i), lines.get(i + 1));
            i += 2;
        }
    }

    public static void main(String[] args) throws Throwable {
        SearchLogBuilder pBuilder = new SearchLogBuilder();
        pBuilder.LoadDirectory("./data/AOL query log", 1);
        pBuilder.generateTopUserProfile(0, 1000);
    }

}
