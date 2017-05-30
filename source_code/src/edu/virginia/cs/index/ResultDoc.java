package edu.virginia.cs.index;

public class ResultDoc {

    private final int _id;
    private String _title = "[no title]";
    private String _content = "[no content]";
    private String _docUrl = "[no url]";

    public ResultDoc(int id) {
        _id = id;
    }

    public int id() {
        return _id;
    }

    public String title() {
        return _title;
    }

    public ResultDoc title(String nTitle) {
        _title = nTitle;
        return this;
    }

    public String content() {
        return _content;
    }

    public String geturl() {
        return _docUrl;
    }

    public ResultDoc content(String nContent) {
        _content = nContent;
        return this;
    }

    public ResultDoc url(String nContent) {
        _docUrl = nContent;
        return this;
    }
}
