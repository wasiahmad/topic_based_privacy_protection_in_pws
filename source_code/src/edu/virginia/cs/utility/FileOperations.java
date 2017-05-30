/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Wasi
 */
public class FileOperations {

    public void appnedToFile(String filename, List<Map.Entry<String, Integer>> param, int choice) throws IOException {
        FileWriter fw = new FileWriter(filename);
        if (choice == 1) {
            for (Map.Entry<String, Integer> entry : param) {
                fw.write(entry.getKey() + "\n");
            }
        } else {
            for (Map.Entry<String, Integer> entry : param) {
                fw.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        }
        fw.close();
    }

    public void storeInFile(String filename, List<Map.Entry<String, Double>> param) throws IOException {
        File file = new File(filename);
        FileWriter fw = new FileWriter(file.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        for (Map.Entry<String, Double> entry : param) {
            bw.write(entry.getKey() + " " + entry.getValue() + "\n");
        }
        bw.close();
    }

    public void storeInFile(String filename, HashMap<String, ArrayList<String>> param) throws IOException {
        FileWriter fw = new FileWriter(filename);
        for (Map.Entry<String, ArrayList<String>> entry : param.entrySet()) {
            String finalStr = "";
            for (String str : entry.getValue()) {
                finalStr += str + " ";
            }
            fw.write(entry.getKey() + " " + finalStr + "\n");
        }
        fw.close();
    }

    public void storeHashMapInFile(String filename, HashMap<String, Float> param) throws IOException {
        FileWriter fw = new FileWriter(filename);
        for (Map.Entry<String, Float> entry : param.entrySet()) {
            fw.write(entry.getKey() + " " + entry.getValue() + "\n");
        }
        fw.close();
    }

    public ArrayList<String> LoadFile(String filename, int numberOfLines) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            ArrayList<String> lines = new ArrayList<>();
            String line;
            int counter = 0;
            while ((line = reader.readLine()) != null) {
                if (counter == numberOfLines) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                lines.add(line);
                counter++;
            }
            reader.close();
            return lines;
        } catch (IOException e) {
            System.err.format("[Error]Failed to open file %s!", filename);
            return null;
        }
    }
}
