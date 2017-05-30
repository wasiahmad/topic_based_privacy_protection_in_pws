package edu.virginia.cs.index;

import edu.virginia.cs.utility.SpecialAnalyzer;
import edu.virginia.cs.user.UserProfile;
import edu.virginia.cs.utility.StringTokenizer;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Searcher {

    private IndexSearcher indexSearcher;
    private SpecialAnalyzer analyzer;
    private static SimpleHTMLFormatter formatter;
    private static final int numFragments = 4;
    private static final String defaultField = "content";
    /* User profile which is constructed and maintained in the server side */
    private UserProfile userProfile;
    /* Flag to turn on or off personalization */
    private boolean activatePersonalization = false;

    /**
     * Sets up the Lucene index Searcher with the specified index.
     *
     * @param indexPath The path to the desired Lucene index.
     */
    public Searcher(String indexPath) {
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
            indexSearcher = new IndexSearcher(reader);
            analyzer = new SpecialAnalyzer();
            formatter = new SimpleHTMLFormatter("****", "****");
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Activate or deactivate personalization.
     *
     * @param flag
     */
    public void activatePersonalization(boolean flag) {
        activatePersonalization = flag;
    }

    /**
     * Initialize the user profile maintained by the server side.
     *
     */
    public void initializeUserProfile() {
        userProfile = new UserProfile();
    }

    /**
     * Update user profile based on the clicked document content.
     *
     * @param content
     * @throws java.io.IOException
     */
    public void updateUProfileUsingClickedDocument(String content) throws IOException {
        userProfile.updateUserProfileUsingClickedDocument(content);
    }

    /**
     * Return user profile maintained by the server side.
     *
     * @return user profile
     */
    public UserProfile getUserProfile() {
        return userProfile;
    }

    /**
     * Sets ranking function for index searching.
     *
     * @param sim
     */
    public void setSimilarity(Similarity sim) {
        indexSearcher.setSimilarity(sim);
    }

    /**
     * The main search function.
     *
     * @param searchQuery Set this object's attributes as needed.
     * @return
     */
    public SearchResult search(SearchQuery searchQuery) {
        BooleanQuery combinedQuery = new BooleanQuery();
        for (String field : searchQuery.fields()) {
            QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);
            try {
                Query textQuery = parser.parse(QueryParser.escape(searchQuery.queryText()));
                combinedQuery.add(textQuery, BooleanClause.Occur.MUST);
            } catch (ParseException exception) {
                exception.printStackTrace();
            }
        }
        return runSearch(combinedQuery, searchQuery);
    }

    /**
     * The simplest search function. Searches the abstract field and returns a
     * the default number of results.
     *
     * @param queryText The text to search
     * @return the SearchResult
     */
    public SearchResult search(String queryText) {
        return search(new SearchQuery(queryText, defaultField));
    }

    /**
     * Searches for a document content in the index.
     *
     * @param queryText the document title, a URL
     * @param field
     * @return clicked document content
     */
    public String search(String queryText, String field) {
        return runSearch(new SearchQuery(queryText, field), "content");
    }

    /**
     * Performs the actual Lucene search.
     *
     * @param luceneQuery
     * @param numResults
     * @return the SearchResult
     */
    private SearchResult runSearch(Query luceneQuery, SearchQuery searchQuery) {
        try {
            TopDocs docs = indexSearcher.search(luceneQuery, searchQuery.fromDoc() + searchQuery.numResults());
            ScoreDoc[] hits;
            String field = searchQuery.fields().get(0);
            if (activatePersonalization) {
                ScoreDoc[] relDocs = docs.scoreDocs;

                /* Store return document with their personalized score */
                HashMap<String, Float> mapDocToScore = new HashMap<>();
                /* Unique terms found in the returned document */
                HashSet<String> uniqueDocTerms;
                /* Fetching server side user profile for personalization */
                HashMap<String, Integer> uProf = userProfile.getUserProfile();

                for (int i = 0; i < relDocs.length; i++) {
                    Document doc = indexSearcher.doc(relDocs[i].doc);
                    /**
                     * Extract the unique tokens from a relevant document
                     * returned by the lucene index searcher.
                     */
                    uniqueDocTerms = new HashSet<>(StringTokenizer.TokenizeString(doc.getField(field).stringValue()));

                    /* Score after personalizing result */
                    float score = 0;
                    /* Smoothing paramter for linear interpolation */
                    float lambda = 0.1f;

                    /**
                     * Computing score for a returned document based on user
                     * profile maintained by the server side.
                     */
                    for (String str : uniqueDocTerms) {
                        Integer value = uProf.get(str);
                        if (value == null) {
                            value = 0;
                        }
                        int tokenCount = userProfile.getTotalTokenCount();
                        if (tokenCount < 1) {
                            tokenCount = 1;
                        }
                        Float tokenProb = (value * 1.0f) / userProfile.getTotalTokenCount();
                        Float refProb = userProfile.getReferenceModel().get(str);
                        if (refProb == null) {
                            refProb = 0.0f;
                        }
                        /* Smoothing using linear interpolation */
                        Float smoothedTokenProb = (1 - lambda) * tokenProb + lambda * refProb;
                        score = score + smoothedTokenProb;
                    }

                    /**
                     * New score is the sum of the score returned by the lucene
                     * index and the personalized score generated.
                     */
                    relDocs[i].score = relDocs[i].score + score;
                    mapDocToScore.put(String.valueOf(i), relDocs[i].score + score);
                }

                /**
                 * Re-rank the document after doing personalization.
                 */
                Map<String, Float> resultedMap = sortByComparator(mapDocToScore, false);
                int i = 0;
                hits = new ScoreDoc[relDocs.length];
                for (Map.Entry<String, Float> entry : resultedMap.entrySet()) {
                    hits[i] = relDocs[Integer.parseInt(entry.getKey())];
                    i++;
                }
                /* Updating the server side user profile with the query text */
                userProfile.updateUserProfile(searchQuery.queryText());
            } else {
                hits = docs.scoreDocs;
            }

            SearchResult searchResult = new SearchResult(searchQuery, docs.totalHits);
            for (ScoreDoc hit : hits) {
                Document doc = indexSearcher.doc(hit.doc);
                ResultDoc rdoc = new ResultDoc(hit.doc);
                String highlighted;
                try {
                    Highlighter highlighter = new Highlighter(formatter, new QueryScorer(luceneQuery));
                    rdoc.title("" + (hit.doc + 1));
                    String contents = doc.getField(field).stringValue();
                    String contentsJudge = doc.getField("clicked_url").stringValue();
                    rdoc.content(contents);
                    rdoc.url(contentsJudge);
                    String[] snippets = highlighter.getBestFragments(analyzer, field, contents, numFragments);
                    highlighted = createOneSnippet(snippets);
                } catch (InvalidTokenOffsetsException exception) {
                    exception.printStackTrace();
                    highlighted = "(no snippets yet)";
                }
                searchResult.addResult(rdoc);
                searchResult.setSnippet(rdoc, highlighted);
            }
            searchResult.trimResults(searchQuery.fromDoc());
            return searchResult;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return new SearchResult(searchQuery);
    }

    /**
     * Searches for document content in the lucene index.
     *
     * @param searchQuery a clicked URL
     * @param indexableField
     * @return clicked document content
     */
    private String runSearch(SearchQuery searchQuery, String indexableField) {
        BooleanQuery combinedQuery = new BooleanQuery();
        for (String field : searchQuery.fields()) {
            QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);
            try {
                Query textQuery = parser.parse(QueryParser.escape(searchQuery.queryText()));
                combinedQuery.add(textQuery, BooleanClause.Occur.MUST);
            } catch (ParseException exception) {
                exception.printStackTrace();
            }
        }

        Query luceneQuery = combinedQuery;
        String returnedResult = null;
        try {
            TopDocs docs = indexSearcher.search(luceneQuery, 1);
            ScoreDoc[] hits = docs.scoreDocs;
            Document doc = indexSearcher.doc(hits[0].doc);
            returnedResult = doc.getField(indexableField).stringValue();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return returnedResult;
    }

    /**
     * Create one string of all the extracted snippets from the highlighter
     *
     * @param snippets
     * @return
     */
    private String createOneSnippet(String[] snippets) {
        String result = " ... ";
        for (String s : snippets) {
            result += s + " ... ";
        }
        return result;
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
        List<Entry<String, Float>> list = new LinkedList<>(unsortMap.entrySet());
        // Sorting the list based on values
        Collections.sort(list, (Entry<String, Float> o1, Entry<String, Float> o2) -> {
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
