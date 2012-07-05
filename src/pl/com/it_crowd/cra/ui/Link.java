package pl.com.it_crowd.cra.ui;

public class Link {
// ------------------------------ FIELDS ------------------------------

    private String text;

    private String url;

// --------------------------- CONSTRUCTORS ---------------------------

    public Link(String url, String text)
    {
        this.text = text;
        this.url = url;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getText()
    {
        return text;
    }

    public String getUrl()
    {
        return url;
    }
}
