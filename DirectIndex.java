import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

public class DirectIndex {
    // takes the text from the document and puts it in a file
    private static File getTextFromHTML(WebsiteInfo websiteInfo, Document doc, File html) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append(websiteInfo.getTitle(doc)); // title
        sb.append(System.lineSeparator());
        sb.append(websiteInfo.getKeywords(doc)); // keywords
        sb.append(System.lineSeparator());
        sb.append(websiteInfo.getDescription(doc));
        sb.append(System.lineSeparator());
        sb.append(doc.body().text());
        String text = sb.toString();

        // generate numbers for the corresponding text file, with the extension txt
        StringBuilder textFileNameBuilder = new StringBuilder(html.getAbsolutePath());

        // add the txt extension
        textFileNameBuilder.append(".txt");
        /*
        // HTML files containing "?" in the name they will receive the extension ".txt" along with the whole name
        if (textFileNameBuilder.indexOf("?") != -1)
        {
            textFileNameBuilder.append(".txt");
        }
        else // if not, we replace the extension after "." with "txt"
        {
            textFileNameBuilder.replace(textFileNameBuilder.lastIndexOf(".") + 1, textFileNameBuilder.length(), "txt");
        }
        */
        String textFileName = textFileNameBuilder.toString();

        // write the result in the text file
        FileWriter fw = new FileWriter(new File(textFileName), false);
        fw.write(text);
        fw.close();

        return new File(textFileName);
    }

    // takes the text from the file, character by character and returns the list of words
    private static HashMap<String, Integer> processDocument(String fileName) throws IOException
    {
        HashMap<String, Integer> wordList = new HashMap<String, Integer>();

        TreeMap<String, Double> tfList = new TreeMap<>(); // for storing the tf
        int numberOfWordsInDocument = 0; // for calculating the tf

        // we read from the file character by character
        FileReader inputStream = null;
        inputStream = new FileReader(fileName);

        // String delimiters = " \t\",.?!;:()[]{}@&#^%'`~<>/\\-–_|„”“=+*";
        StringBuilder sb = new StringBuilder();

        int c; // the current character
        while ((c = inputStream.read()) != -1)
        {
            // if (delimiters.indexOf(textChars[i]) != -1) // we are on a separator
            if (!Character.isLetterOrDigit((char)c)) // we are on a separator
            {
                String newWord = sb.toString(); // create the new word
                if (newWord.equals("")) // ignore the empty word
                {
                    continue;
                }

               // first, we check if it is an exception
                if (ExceptionList.exceptions.contains(newWord))
                {
                    // we add it as it is
                    if (wordList.containsKey(newWord)) // if it already exists in the HashMap
                    {
                        wordList.put(newWord, wordList.get(newWord) + 1); // we increase the number of occurrences
                    } else // if not, we add it
                    {
                        wordList.put(newWord, 1);
                    }
                    ++numberOfWordsInDocument;
                }
                // then if it is a stopword
                else if (StopWordList.stopwords.contains(newWord))
                {
                    // we ignore it
                    sb.setLength(0);
                    continue;
                }
                else // dictionary word
                {
                    // the Porter algorithm is used for stemming
                    PorterStemmer stemmer = new PorterStemmer();
                    stemmer.add(newWord.toCharArray(), newWord.length());
                    stemmer.stem();
                    newWord = stemmer.toString();

                    if (wordList.containsKey(newWord)) // if it already exists in the HashMap
                    {
                        wordList.put(newWord, wordList.get(newWord) + 1); // we increase the number of occurrences
                    } else // if not, we add it
                    {
                        wordList.put(newWord, 1);
                    }
                    ++numberOfWordsInDocument;
                }

                // System.out.println(newWord + " -> " + hashText.get(newWord));
                sb.setLength(0); // we clean the StringBuilder
            }
            else // we are in the middle of a word
            {
                sb.append((char)c); // add the current letter to the word being created
            }
        }

        // remove the empty word
        wordList.remove("");

        // write in the file the list of words and the number of occurrences (the direct index)
        StringBuilder sbDirectIndexFileName = new StringBuilder(fileName);

        // the file will have the extension ".directindex.json" added to the original HTML file name
        sbDirectIndexFileName.replace(sbDirectIndexFileName.lastIndexOf(".") + 1, sbDirectIndexFileName.length(), "directindex.json");
        Writer directIndexWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(sbDirectIndexFileName.toString()), "utf-8"));

        Gson directIndexGsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        String directIndexJsonFile = directIndexGsonBuilder.toJson(wordList);

        directIndexWriter.write(directIndexJsonFile);
        directIndexWriter.close();

        inputStream.close();
        // System.out.println("The words in the text on the website have been processed!");

        // for tf calculation
        // create the HashMap that contains the tf for each word
        for (String word : wordList.keySet())
        {
            tfList.put(word, (double)wordList.get(word) / numberOfWordsInDocument);
        }

        // write the tf for the current document in a file
        StringBuilder sbTfFileName = new StringBuilder(fileName);
        sbTfFileName.replace(sbTfFileName.lastIndexOf(".") + 1, sbTfFileName.length(), "tf.json");
        Writer tfWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(sbTfFileName.toString()), "utf-8"));

        Gson tfGsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        String tfJsonFile = tfGsonBuilder.toJson(tfList);

        tfWriter.write(tfJsonFile);
        tfWriter.close();

        return wordList;
    }

    // calculate the index directly
    public static HashMap<String, HashMap<String, Integer>> directIndex(WebsiteInfo websiteInfo) throws IOException
    {
        HashMap<String, HashMap<String, Integer>> directIndex = new HashMap<>();
        Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();

        String websiteFolder = websiteInfo.getWebsiteFolder();
        String baseUri = websiteInfo.getBaseUri();

        // to get the mapping file, called "directindex.map", located in the root directory
        Writer mapFileWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(websiteFolder + "directindex.map"), "utf-8"));
        HashMap<String, String> mapFile = new HashMap<>();

        // for directory traversal, we use a queue
        LinkedList<String> folderQueue = new LinkedList<>();

        // start with the root folder
        folderQueue.add(websiteFolder);

        while (!folderQueue.isEmpty()) // as long as there are no more child folders to go through
        {
            // preluam un folder din coada
            String currentFolder = folderQueue.pop();
            File folder = new File(currentFolder);
            File[] listOfFiles = folder.listFiles();

            // we go through the list of files / folders
            try {
                for (File file : listOfFiles)
                {
                    // if we have arrived at a file, we check that it is an HTML file
                    if (file.isFile() && Files.probeContentType(file.toPath()).equals("text/html"))
                    {
                        // parse the HTML file using JSOUP
                        Document doc = Jsoup.parse(file, null, baseUri);
                        String fileName = file.getAbsolutePath();
                        // System.out.println("[PARSARE] I parse the HTML file \"" + fileName + "\".");

                        // stocam textul in fisier separat
                        File textFile = getTextFromHTML(websiteInfo, doc, file);
                        String textFileName = textFile.getAbsolutePath();
                        // System.out.println("[TEXT] I took the text from the HTML file \"" + fileName + "\".");

                        // process the words, resulting in a HashMap of type (word -> number_of_occurrences)
                        // store the index directly in a file with the extension ".directindex"
                        HashMap<String, Integer> currentDocWords = processDocument(textFileName);

                        // add the document and the list of words to the final HashMap
                        directIndex.put(fileName, currentDocWords);

                        // add the current document, together with the directly associated index in the mapping file
                        mapFile.put(fileName, fileName + ".directindex.json");

                        // System.out.println("[INDEX_DIRECT] I created the index directly from the TEXT file \"" + textFileName + "\".");
                    }
                    else if (file.isDirectory()) // if it's a folder, we put it in the queue
                    {
                        folderQueue.add(file.getAbsolutePath());
                    }
                }
            } catch (NullPointerException e) {
                // System.out.println("There are no files in the folder \"" + currentFolder + "\"!");
            }
        }

        // write the JSON mapping file
        mapFileWriter.write(gsonBuilder.toJson(mapFile));
        mapFileWriter.close();
        // System.out.println(System.lineSeparator());
        // System.out.println("[MAPARE] I have created the mapping file \"" + websiteFolder + "directindex.map\".");

        return directIndex;
    }
}
