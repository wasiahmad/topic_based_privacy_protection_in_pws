package edu.virginia.cs.eval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import edu.virginia.cs.index.ResultDoc;
import edu.virginia.cs.index.Searcher;
import edu.virginia.cs.utility.SpecialAnalyzer;
import edu.virginia.cs.user.UserProfile;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import edu.virginia.cs.model.GenerateCoverQuery;
import edu.virginia.cs.model.QueryTopicInference;
import edu.virginia.cs.similarities.OkapiBM25;
import edu.virginia.cs.utility.FileOperations;
import edu.virginia.cs.utility.StringTokenizer;
import java.io.FileWriter;

public class Evaluate {

    /* folder path where a specified number of user's search log is present */
    private final String _judgeFile = "./data/user_search_logs/";
    /* folder path where the AOL index is located */
    private final String _indexPath = "lucene-AOL-index";
    /* Searcher class object to search for results of a user query */
    private Searcher _searcher = null;
    /* Storing a specific user's queries and corresponding all clicked documents */
    private HashMap<String, HashSet<String>> mappingQueryToURL;
    /* Storing a specific user's all queries */
    private ArrayList<String> listOfUserQuery;
    /* Storing a specific user's all queries */
    private ArrayList<String> listOfCoverQuery;
    /* Storing a specific user's all query probabilities. One query will have n 
     different  probability values for n different topics assigned by the topic model */
    private ArrayList<String> allQueryTopocProb;
    /* Object to generate cover queries for a specific user query */
    private GenerateCoverQuery gCoverQuery;
    /* User profile which is constructed and maintained in the client side */
    private UserProfile uProfile;
    /* Total MAP for 'n' users that we are evaluating, ex. in our case, n = 250 */
    private double totalMAP = 0.0;
    /* Total number of queries evaluated for 'n' users, ex. in our case, n = 250 */
    private double totalQueries = 0.0;
    /* Total KL-Divergence for 'n' users that we are evaluating, ex. in our case, n = 250 */
    private double totalKL = 0;
    /* Total mutual information for 'n' users that we are evaluating, ex. in our case, n = 250 */
    private double totalMI = 0;
    /* First parameter of our approach, Entropy range */
    private final double entropyRange;
    /* Second parameter of our approach, number of cover query for each user query */
    private final int numOfCoverQ;
    /* Query parser used to parse user query */
    private final QueryParser parser;
    /* To turn client side re-ranking on or off */
    private final boolean isCSRankingActive;

    public Evaluate(double entropy, int totalCoverQ, boolean param) {
        gCoverQuery = new GenerateCoverQuery();
        // setting entropy range for our model
        entropyRange = entropy;
        // setting number of cover query that will be generated for each user query
        numOfCoverQ = totalCoverQ;
        // setting flag for turning on or off client side re-ranking
        isCSRankingActive = param;

        parser = new QueryParser(Version.LUCENE_46, "", new SpecialAnalyzer());
        _searcher = new Searcher(_indexPath);
        _searcher.setSimilarity(new OkapiBM25());
        // setting the flag to enable personalization
        _searcher.activatePersonalization(true);
    }

    /**
     * Method that generates a mapping between each user query and corresponding
     * clicked documents.
     *
     * @param userId
     * @throws java.lang.Throwable
     */
    private void createUserJudgements(String userId) {
        String judgeFile = _judgeFile + userId + ".txt";
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(judgeFile));
            String query;
            while ((query = br.readLine()) != null) {
                String relDoc = br.readLine();
                HashSet<String> tempSet;
                if (mappingQueryToURL.containsKey(query)) {
                    tempSet = mappingQueryToURL.get(query);
                    tempSet.add(relDoc);
                } else {
                    tempSet = new HashSet<>();
                    tempSet.add(relDoc);
                    mappingQueryToURL.put(query, tempSet);
                    listOfUserQuery.add(query);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Evaluate.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Method that initializes required things for the cover query generation
     * procedure.
     *
     * @throws java.File.IOException
     */
    private void intializeCoverQueryGeneration(HashMap<String, Float> referenceModel, String threadId) throws IOException {
        uProfile = new UserProfile();
        uProfile.setReferenceModel(referenceModel);

        QueryTopicInference qti = new QueryTopicInference();
        qti.initializeGeneration(listOfUserQuery, threadId);

        gCoverQuery = new GenerateCoverQuery();
        gCoverQuery.setEntropy(entropyRange);
        // load all the words and their probability distribution for each topic
        gCoverQuery.loadTopicWords("topic_repo");

        allQueryTopocProb = new ArrayList<>();
        // load topic probabilities for user queries
        allQueryTopocProb = new FileOperations().LoadFile("lda-output-files/result_" + threadId + "-gamma.dat", -1);
    }

    /**
     * Main method that executes the entire pipeline.
     *
     * @param allUserId
     * @param referenceModel
     * @param threadId
     * @param semEval
     * @return
     * @throws java.lang.Throwable
     */
    public String startEval(ArrayList<String> allUserId, HashMap<String, Float> referenceModel, String threadId, SemanticEvaluation semEval) throws Throwable {
        int countUsers = 0;
        FileWriter writer = new FileWriter("model-output-files/" + threadId + ".txt");
        for (String userId : allUserId) {
            countUsers++;

            // initializing user profile of the server side and setting the reference model
            _searcher.initializeUserProfile();
            _searcher.getUserProfile().setReferenceModel(referenceModel);

            // required for calculating mutual information between user queris and cover queries
            listOfUserQuery = new ArrayList<>();
            listOfCoverQuery = new ArrayList<>();

            mappingQueryToURL = new HashMap<>();
            // generate the clicked urls for evaluation
            createUserJudgements(userId);
            // initialization for cover query generation
            intializeCoverQueryGeneration(referenceModel, threadId);

            double meanAvgPrec = 0.0;
            // Number of queries that have non-zero result set
            double numQueries = 0;
            // index of the user query
            int queryIndex = 0;

            for (String query : mappingQueryToURL.keySet()) {
                ArrayList<String> relDocs = new ArrayList<>();
                HashSet<String> clickedDocs = mappingQueryToURL.get(query);

                Query textQuery = parser.parse(QueryParser.escape(query));
                String[] qParts = textQuery.toString().split(" ");

                /**
                 * Checking whether a user query and clicked documents are
                 * actually relevant or not! For example, "american idol season
                 * one" is no longer related to www.tv.com.
                 *
                 */
                for (String relDoc : clickedDocs) {
                    String docContent = _searcher.search(relDoc, "clicked_url");
                    HashSet<String> tokSet = new HashSet<>(StringTokenizer.TokenizeString(docContent));
                    int tokMatched = 0;

                    for (String part : qParts) {
                        if (tokSet.contains(part)) {
                            tokMatched++;
                        }
                    }
                    if (tokMatched > 0) {
                        relDocs.add(relDoc);
                    }
                }

                /**
                 * If a user query has at least one corresponding clicked
                 * document, then we evaluate it, otherwise not.
                 *
                 */
                if (relDocs.size() > 0) {
                    String judgement = "";
                    for (String doc : relDocs) {
                        judgement += doc + " ";
                    }
                    // computing average precision for a query
                    double avgPrec = AvgPrec(query, judgement, queryIndex);
                    meanAvgPrec += avgPrec;
                    ++numQueries;
                }
                queryIndex++;
            }

            // totalMAP = sum of all MAP computed for queries of 'n' users
            totalMAP += meanAvgPrec;
            // totalQueries = total number of queries for 'n' users
            totalQueries += numQueries;
            /**
             * computing KL-Divergence from true user profile to noisy user
             * profile. give the number of tokens to calculate token probability
             * on the fly.
             */
            double klDivergence = (double) _searcher.getUserProfile().calculateKLDivergence(uProfile.getUserProfile(), uProfile.getTotalTokenCount());
            totalKL += klDivergence;
            // compute MAP for the current user
            double MAP = meanAvgPrec / numQueries;
            // compute mutual information for the current user
            double mutualInfo = semEval.calculateNMI(listOfUserQuery, listOfCoverQuery);
            // totalMI = sum of all MI computed for 'n' users
            totalMI += mutualInfo;

            writer.write(countUsers + "\t" + Integer.parseInt(userId) + "\t" + MAP + "\t" + klDivergence + "\t" + mutualInfo + "\n");
            writer.flush();
            System.out.printf("%-8d\t%-8d\t%-8f\t%.8f\t%.8f\n", countUsers, Integer.parseInt(userId), MAP, klDivergence, mutualInfo);
        }

        double avgKL = 0;
        double avgMI = 0;
        double finalMAP = totalMAP / totalQueries;
        if (countUsers > 0) {
            avgKL = totalKL / countUsers;
            avgMI = totalMI / countUsers;
        }

        writer.write("\n************Result after full pipeline execution for n users**************" + "\n");
        writer.write("\nTotal number of users : " + countUsers + "\n");
        writer.write("Total number of quries tested : " + totalQueries + "\n");
        writer.write("Map : " + finalMAP + "\n");
        writer.write("Average KL : " + avgKL + "\n");
        writer.write("Average MI : " + avgMI + "\n");
        writer.flush();
        writer.close();

        String retValue = countUsers + "\t" + totalQueries + "\t" + totalMAP + "\t" + totalKL + "\t" + totalMI;
        return retValue;
    }

    /**
     * Method that computes average precision of a user submitted query.
     *
     * @param query user's original query
     * @param clickedDocs clicked documents for the true user query
     * @param index index of the user query
     * @return average precision
     */
    private double AvgPrec(String query, String clickedDocs, int index) throws Throwable {
        double avgp = 0.0;
        if (numOfCoverQ == 0) {
            // if no cover query, just submit the original query
            avgp = submitOriginalQuery(query, clickedDocs);
        } else {
            // generating the cover queries
            ArrayList<String> coverQueries = gCoverQuery.generateCoverQueries(allQueryTopocProb.get(index), numOfCoverQ, uProfile.getAvgQueryLength());
            int randNum = (int) (Math.random() * coverQueries.size());
            for (int k = 0; k < coverQueries.size(); k++) {
                listOfCoverQuery.add(coverQueries.get(k));
                // submitting cover query to the search engine
                ArrayList<ResultDoc> searchResults = _searcher.search(coverQueries.get(k)).getDocs();
                // generating fake clicks for the cover queries, one click per cover query
                if (!searchResults.isEmpty()) {
                    int rand = (int) (Math.random() * searchResults.size());
                    ResultDoc rdoc = searchResults.get(rand);
                    // update user profile kept in the server side
                    _searcher.updateUProfileUsingClickedDocument(rdoc.content());
                }
                // submitting the original user query to the search engine
                if (k == randNum) {
                    avgp = submitOriginalQuery(query, clickedDocs);
                }
            }
        }
        return avgp;
    }

    /**
     * Submit the original query to search engine and computes average precision
     * of the search results.
     *
     * @param query
     * @param clickedDocs
     * @return
     * @throws IOException
     */
    private double submitOriginalQuery(String query, String clickedDocs) throws IOException {
        double avgp = 0.0;
        ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
        if (results.isEmpty()) {
            return avgp;
        }
        // re-rank the results based on the user profile kept in client side
        if (isCSRankingActive) {
            results = reRankResults(results);
        }
        HashSet<String> relDocs = new HashSet<>(Arrays.asList(clickedDocs.split(" ")));
        int i = 1;
        double numRel = 0;
        for (ResultDoc rdoc : results) {
            if (relDocs.contains(rdoc.geturl())) {
                numRel++;
                avgp = avgp + (numRel / i);
                // update user profile kept in the client side
                uProfile.updateUserProfileUsingClickedDocument(rdoc.content());
                // update user profile kept in the server side
                _searcher.updateUProfileUsingClickedDocument(rdoc.content());
            }
            ++i;
        }
        avgp = avgp / relDocs.size();
        // updating user profile kept in client side using original user query
        uProfile.updateUserProfile(query);
        return avgp;
    }

    /**
     * Method that re-ranks the result in the client side.
     *
     * @param relDocs all the relevant documents returned by the search engine
     * @return re-ranked resulting documents
     */
    private ArrayList<ResultDoc> reRankResults(ArrayList<ResultDoc> relDocs) throws IOException {
        HashMap<String, Float> docScoreMap = new HashMap<>();
        HashMap<String, Integer> uniqueDocTerms;

        for (int i = 0; i < relDocs.size(); i++) {
            List<String> tokens = StringTokenizer.TokenizeString(relDocs.get(i).content());
            uniqueDocTerms = new HashMap<>();

            // computing term frequency of all the unique terms found in the document
            for (String tok : tokens) {
                if (uniqueDocTerms.containsKey(tok)) {
                    uniqueDocTerms.put(tok, uniqueDocTerms.get(tok) + 1);
                } else {
                    uniqueDocTerms.put(tok, 1);
                }
            }

            float docScore = 0;
            // smoothing parameter for linear interpolation
            float lambda = 0.1f;
            for (String term : uniqueDocTerms.keySet()) {
                // term frequency in the user profile
                Integer value = uProfile.getUserProfile().get(term);
                if (value == null) {
                    value = 0;
                }

                // maximum likelihood calculation
                Float tokenProb = ((value * 1.0f) / uProfile.getTotalTokenCount()) * uniqueDocTerms.get(term);
                // probability from reference model for smoothing purpose
                Float refProb = uProfile.getReferenceModel().get(term);
                if (refProb == null) {
                    refProb = 0.0f;
                }

                // smoothing token probability using linear interpolation
                Float smoothedTokenProb = (1 - lambda) * tokenProb + lambda * refProb;
                docScore += smoothedTokenProb;
            }
            docScoreMap.put(String.valueOf(i), docScore);
        }

        /**
         * Client side re-ranking using true user profile.
         */
        Map<String, Float> resultedMap = sortByComparator(docScoreMap, false);

        /**
         * Re-rank the documents by giving weight to the search engine rank and
         * the client side rank.
         */
        int i = 0;
        for (Map.Entry<String, Float> entry : resultedMap.entrySet()) {
            float score = 0;
            // Giving 50% weight to both search engine and client side rank.
            score = 0.5f * (1.0f / (i + 1)) + 0.5f * (1.0f / (Integer.parseInt(entry.getKey() + 1)));
            docScoreMap.put(entry.getKey(), score);
            i++;
        }

        // sort the documents in descending order according to the new score assigned
        Map<String, Float> result = sortByComparator(docScoreMap, false);
        ArrayList<ResultDoc> retValue = new ArrayList<>();
        for (Map.Entry<String, Float> entry : result.entrySet()) {
            retValue.add(relDocs.get(Integer.parseInt(entry.getKey())));
        }

        // return re-ranked documents
        return retValue;
    }

    /**
     * Method that generate the id of all users for evaluation.
     *
     * @param unsortMap unsorted Map
     * @param order if true, then sort in ascending order, otherwise in
     * descending order
     * @return sorted Map
     */
    private Map<String, Float> sortByComparator(Map<String, Float> unsortMap, final boolean order) {
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
        Map<String, Float> sortedMap = new LinkedHashMap<>();
        list.stream().forEach((entry) -> {
            sortedMap.put(entry.getKey(), entry.getValue());
        });
        return sortedMap;
    }

}
