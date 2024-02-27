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

    public String getTitle(Document doc) // preia titlul documentului
    {
        String title = doc.title();
        // System.out.println("Titlul site-ului: " + title);
        return title;
    }

    public String getKeywords(Document doc) // preia cuvintele cheie
    {
        Element keywords = doc.selectFirst("meta[name=keywords]");
        String keywordsString = "";
        if (keywords == null) {
            // System.out.println("Nu exista tag-ul <meta name=\"keywords\">!");
        } else {
            keywordsString = keywords.attr("content");
            // System.out.println("Cuvintele cheie au fost preluate!");
        }
        return keywordsString;
    }

    public String getDescription(Document doc) // preia descrierea site-ului
    {
        Element description = doc.selectFirst("meta[name=description]");
        String descriptionString = "";
        if (description == null) {
            // System.out.println("Nu exista tag-ul <meta name=\"description\">!");
        } else {
            descriptionString = description.attr("content");
            // System.out.println("Descrierea site-ului a fost preluata!");
        }
        return descriptionString;
    }

    public String getRobots(Document doc) // preia lista de robots
    {
        Element robots = doc.selectFirst("meta[name=robots]");
        String robotsString = "";
        if (robots == null) {
            System.out.println("Nu exista tag-ul <meta name=\"robots\">!");
        } else {
            robotsString = robots.attr("content");
            // System.out.println("Lista de robots a site-ului a fost preluata!");
        }
        return robotsString;
    }

    public Set<String> getLinks(Document doc) throws IOException // preia link-urile de pe site (ancorele)
    {
        Elements links = doc.select("a[href]");
        Set<String> URLs = new HashSet<String>();
        for (Element link : links) {
            String absoluteLink = link.attr("abs:href"); // facem link-urile relative sa fie absolute
            if (absoluteLink.contains(baseUri)) // ignoram legaturile interne
            {
                continue;
            }

            // cautam eventuale ancore in link-uri
            int anchorPosition = absoluteLink.indexOf('#');
            if (anchorPosition != -1) // daca exista o ancora (un #)
            {
                // stergem partea cu ancora din link
                StringBuilder tempLink = new StringBuilder(absoluteLink);
                tempLink.replace(anchorPosition, tempLink.length() - 1, "");
                absoluteLink = tempLink.toString();
            }

            // nu vrem sa adaugam duplicate, asa incat folosim o colectie de tip Set
            URLs.add(absoluteLink);
        }
        // System.out.println("Link-urile de pe site au fost preluate!");
        return URLs;
    }
}
