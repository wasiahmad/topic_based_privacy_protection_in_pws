/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.preprocessing;

import edu.virginia.cs.utility.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Wasi
 */
public class TermIndex {

    private static SAXParserFactory factory;
    private static SAXParser saxParser;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        try {
            factory = SAXParserFactory.newInstance();
            saxParser = factory.newSAXParser();
            readFile("./data/xml/AolCrawledData.xml");
        } catch (ParserConfigurationException | SAXException ex) {
            Logger.getLogger(AOLDictionary.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void readFile(String filename) {
        try {
            File inputFile = new File(filename);
            AOLHandler aolHandler = new AOLHandler("./data/Term-Index", "./data/AOL-Dictionary");
            saxParser.parse(inputFile, aolHandler);
        } catch (SAXException | IOException ex) {
            Logger.getLogger(AOLDictionary.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

class AOLHandler extends DefaultHandler {

    private FileWriter fwriter;
    private StringBuilder buffer;
    private boolean isContent;
    private int pageCount;
    private final HashMap<String, List<Integer>> Dictionary;
    private final StringTokenizer tokenizer;

    public AOLHandler(String filename, String dictFile) {
        buffer = new StringBuilder();
        Dictionary = new HashMap<>();
        tokenizer = new StringTokenizer();
        try {
            fwriter = new FileWriter(filename);
            BufferedReader br = new BufferedReader(new FileReader(new File(dictFile)));
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\t");
                List<Integer> temp = new ArrayList<>();
                Dictionary.put(split[0], temp);
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(DataHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void startElement(String uri,
            String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equalsIgnoreCase("crawledData")) {
            System.out.println("Parsing Started!!!");
            pageCount = 0;
        } else if (qName.equalsIgnoreCase("page")) {
        } else if (qName.equalsIgnoreCase("content")) {
            isContent = true;
            buffer.setLength(0);
        }
    }

    @Override
    public void endElement(String uri,
            String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("crawledData")) {
            System.out.println("Parsing Completed!!!");
            System.out.println("Total " + pageCount + " pages loaded.");
            try {
                int counter = 0;
                for (String token : Dictionary.keySet()) {
                    fwriter.write(token);
                    for (int i : Dictionary.get(token)) {
                        fwriter.write("\t" + i);
                        fwriter.flush();
                    }
                    fwriter.write("\n");
                    fwriter.flush();
                    counter++;
                }
                fwriter.close();
                System.out.println("Total tokens = " + counter);
            } catch (IOException ex) {
                Logger.getLogger(DataHandler.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        } else if (qName.equalsIgnoreCase("page")) {
            if (buffer.toString().length() > 0) {
                pageCount++;
                StoreInDictionary(tokenizer.TokenizeString(buffer.toString()), pageCount);
            }
            if (pageCount % 10000 == 0) {
                System.out.println(pageCount + " pages completed...!");
            }
        } else if (qName.equalsIgnoreCase("content")) {
            isContent = false;
        }
    }

    @Override
    public void characters(char ch[],
            int start, int length) throws SAXException {
        if (isContent) {
            buffer.append(ch, start, length);
        }
    }

    private void StoreInDictionary(List<String> param, int docId) {
        HashSet<String> tokenSet = new HashSet<>(param);
        for (String token : tokenSet) {
            List<Integer> list = Dictionary.get(token);
            if (list != null) {
                list.add(docId);
            }
        }
    }
}
