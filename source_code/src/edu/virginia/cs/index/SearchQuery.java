package edu.virginia.cs.index;

import java.util.ArrayList;
import java.util.Objects;

public class SearchQuery {

    private ArrayList<String> fields;
    private String queryText;
    private int numResults;
    private int from;
    private final static int defaultNumResults = 100;
    private final static String defaultField = "content";

    public SearchQuery queryText(String queryText) {
        this.queryText = queryText;
        return this;
    }

    public SearchQuery fields(ArrayList<String> fields) {
        this.fields = new ArrayList<>(fields);
        return this;
    }

    public ArrayList<String> fields() {
        return fields;
    }

    public String queryText() {
        return queryText;
    }

    public SearchQuery fields(String field) {
        fields = new ArrayList<>();
        fields.add(field);
        return this;
    }

    public int numResults() {
        return numResults;
    }

    public SearchQuery numResults(int numResults) {
        this.numResults = numResults;
        return this;
    }

    public int fromDoc() {
        return from;
    }

    public SearchQuery fromDoc(int fromDoc) {
        this.from = fromDoc;
        return this;
    }

    public SearchQuery(String queryText, ArrayList<String> fields) {
        this.queryText = queryText;
        this.numResults = defaultNumResults;
        this.fields = fields;
        from = 0;
    }

    public SearchQuery() {
        this.queryText = null;
        this.numResults = defaultNumResults;
        this.fields = new ArrayList<>();
        fields.add(defaultField);
        from = 0;
    }

    public SearchQuery(String queryText, String field) {
        this.queryText = queryText;
        this.numResults = defaultNumResults;
        fields = new ArrayList<>();
        fields.add(field);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SearchQuery)) {
            return false;
        }

        SearchQuery otherQuery = (SearchQuery) other;
        return otherQuery.queryText.equals(queryText)
                && otherQuery.fields == fields
                && otherQuery.numResults == numResults
                && otherQuery.from == from;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.fields);
        hash = 89 * hash + Objects.hashCode(this.queryText);
        hash = 89 * hash + this.numResults;
        hash = 89 * hash + this.from;
        return hash;
    }
}
