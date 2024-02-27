import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class WebsiteInfo {
    private String websiteFolder;
    private String baseUri;

    WebsiteInfo(String websiteFolder, String baseUri)
    {
        this.websiteFolder = websiteFolder;
        this.baseUri = baseUri;
    }

    public String getWebsiteFolder()
    {
        return websiteFolder;
    }

    public String getBaseUri()
    {
        return baseUri;
    }

    public String getTitle(Document doc) // get the title of the document
    {
        String title = doc.title();
        // System.out.println("Website title: " + title);
        return title;
    }

    public String getKeywords(Document doc) // get the keywords
    {
        Element keywords = doc.selectFirst("meta[name=keywords]");
        String keywordsString = "";
        if (keywords == null) {
            // System.out.println("The tag doesn't exist: <meta name=\"keywords\">!");
        } else {
            keywordsString = keywords.attr("content");
            // System.out.println("Keywords have been retrieved!");
        }
        return keywordsString;
    }

    public String getDescription(Document doc) // get the website description
    {
        Element description = doc.selectFirst("meta[name=description]");
        String descriptionString = "";
        if (description == null) {
            // System.out.println("The tag doesn't exist: <meta name=\"description\">!");
        } else {
            descriptionString = description.attr("content");
            // System.out.println("The website description have been retrieved!");
        }
        return descriptionString;
    }

    public String getRobots(Document doc) // get list of robots
    {
        Element robots = doc.selectFirst("meta[name=robots]");
        String robotsString = "";
        if (robots == null) {
            System.out.println("The tag doesn't exist: <meta name=\"robots\">!");
        } else {
            robotsString = robots.attr("content");
            // System.out.println("The list of robots have been retrieved");
        }
        return robotsString;
    }

    public Set<String> getLinks(Document doc) throws IOException // get the links from the website (anchors)
    {
        Elements links = doc.select("a[href]");
        Set<String> URLs = new HashSet<String>();
        for (Element link : links) {
            String absoluteLink = link.attr("abs:href"); // we make the relative links absolute
            if (absoluteLink.contains(baseUri)) // ignore internal links
            {
                continue;
            }

            // we are looking for possible anchors in the links
            int anchorPosition = absoluteLink.indexOf('#');
            if (anchorPosition != -1) // if there is an anchor (#)
            {
                // delete the part with the anchor from the link
                StringBuilder tempLink = new StringBuilder(absoluteLink);
                tempLink.replace(anchorPosition, tempLink.length() - 1, "");
                absoluteLink = tempLink.toString();
            }

            // we don't want to add duplicates, so we use a collection of type Set
            URLs.add(absoluteLink);
        }
        // System.out.println("The links on the site have been taken!");
        return URLs;
    }
}
