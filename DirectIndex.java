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
    // preia textul din document si il pune intr-un fisier
    private static File getTextFromHTML(WebsiteInfo websiteInfo, Document doc, File html) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append(websiteInfo.getTitle(doc)); // titlul
        sb.append(System.lineSeparator());
        sb.append(websiteInfo.getKeywords(doc)); // cuvintele cheie
        sb.append(System.lineSeparator());
        sb.append(websiteInfo.getDescription(doc));
        sb.append(System.lineSeparator());
        sb.append(doc.body().text());
        String text = sb.toString();

        // generam numere fisierului text corespunzator, cu extensia txt
        StringBuilder textFileNameBuilder = new StringBuilder(html.getAbsolutePath());

        // adaugam extensia txt
        textFileNameBuilder.append(".txt");
        /*
        // fisierele HTML ce contin "?" in nume vor primi extensia ".txt" alaturi de intregul nume
        if (textFileNameBuilder.indexOf("?") != -1)
        {
            textFileNameBuilder.append(".txt");
        }
        else // daca nu, inlocuim extensia de dupa "." cu "txt"
        {
            textFileNameBuilder.replace(textFileNameBuilder.lastIndexOf(".") + 1, textFileNameBuilder.length(), "txt");
        }
        */
        String textFileName = textFileNameBuilder.toString();

        // scriem rezultatul in fisierul text
        FileWriter fw = new FileWriter(new File(textFileName), false);
        fw.write(text);
        fw.close();

        return new File(textFileName);
    }

    // preia textul din fisier, caracter cu caracter si returneaza lista de cuvinte
    private static HashMap<String, Integer> processDocument(String fileName) throws IOException
    {
        HashMap<String, Integer> wordList = new HashMap<String, Integer>();

        TreeMap<String, Double> tfList = new TreeMap<>(); // pentru stocarea tf-ului
        int numberOfWordsInDocument = 0; // pentru calculul tf-ului

        // citim din fisier caracter cu caracter
        FileReader inputStream = null;
        inputStream = new FileReader(fileName);

        // String delimiters = " \t\",.?!;:()[]{}@&#^%'`~<>/\\-–_|„”“=+*";
        StringBuilder sb = new StringBuilder();

        int c; // caracterul curent
        while ((c = inputStream.read()) != -1)
        {
            // if (delimiters.indexOf(textChars[i]) != -1) // suntem pe un separator
            if (!Character.isLetterOrDigit((char)c)) // suntem pe un separator
            {
                String newWord = sb.toString(); // cream cuvantul nou
                if (newWord.equals("")) // ignoram cuvantul vid
                {
                    continue;
                }

                // mai intai, verificam daca este exceptie
                if (ExceptionList.exceptions.contains(newWord))
                {
                    // il adaugam asa cum este
                    if (wordList.containsKey(newWord)) // daca exista deja in HashMap
                    {
                        wordList.put(newWord, wordList.get(newWord) + 1); // incrementam numarul de aparitii
                    } else // daca nu, il adaugam
                    {
                        wordList.put(newWord, 1);
                    }
                    ++numberOfWordsInDocument;
                }
                // apoi daca este stopword
                else if (StopWordList.stopwords.contains(newWord))
                {
                    // il ignoram
                    sb.setLength(0);
                    continue;
                }
                else // cuvant de dictionar
                {
                    // se foloseste algoritmul Porter pentru stemming
                    PorterStemmer stemmer = new PorterStemmer();
                    stemmer.add(newWord.toCharArray(), newWord.length());
                    stemmer.stem();
                    newWord = stemmer.toString();

                    if (wordList.containsKey(newWord)) // daca exista deja in HashMap
                    {
                        wordList.put(newWord, wordList.get(newWord) + 1); // incrementam numarul de aparitii
                    } else // daca nu, il adaugam
                    {
                        wordList.put(newWord, 1);
                    }
                    ++numberOfWordsInDocument;
                }

                // System.out.println(newWord + " -> " + hashText.get(newWord));
                sb.setLength(0); // curatam StringBuilder-ul
            }
            else // suntem in mijlocul unui cuvant
            {
                sb.append((char)c); // adaugam litera curenta la cuvantul ce se creeaza
            }
        }

        // eliminam cuvantul vid
        wordList.remove("");

        // scriem in fisier lista de cuvinte si numarul de aparitii (index-ul direct)
        StringBuilder sbDirectIndexFileName = new StringBuilder(fileName);

        // fisierul va avea extensia ".directindex.json" adaugata numelui original al fisierului HTML
        sbDirectIndexFileName.replace(sbDirectIndexFileName.lastIndexOf(".") + 1, sbDirectIndexFileName.length(), "directindex.json");
        Writer directIndexWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(sbDirectIndexFileName.toString()), "utf-8"));

        Gson directIndexGsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        String directIndexJsonFile = directIndexGsonBuilder.toJson(wordList);

        directIndexWriter.write(directIndexJsonFile);
        directIndexWriter.close();

        inputStream.close();
        // System.out.println("Cuvintele din textul de pe site au fost prelucrate!");

        // pentru calculul tf
        // cream HashMap-ul ce contine tf-ul pentru fiecare cuvant
        for (String word : wordList.keySet())
        {
            tfList.put(word, (double)wordList.get(word) / numberOfWordsInDocument);
        }

        // scriem intr-un fisier tf-ul pentru documentul curent
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

    // calculeaza indexul direct
    public static HashMap<String, HashMap<String, Integer>> directIndex(WebsiteInfo websiteInfo) throws IOException
    {
        HashMap<String, HashMap<String, Integer>> directIndex = new HashMap<>();
        Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();

        String websiteFolder = websiteInfo.getWebsiteFolder();
        String baseUri = websiteInfo.getBaseUri();

        // pentru obtinerea fisierului de mapare, numit "directindex.map", aflat in directorul radacina
        Writer mapFileWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(websiteFolder + "directindex.map"), "utf-8"));
        HashMap<String, String> mapFile = new HashMap<>();

        // pentru parcurgerea directoarelor, folosim o coada
        LinkedList<String> folderQueue = new LinkedList<>();

        // pornim cu folder-ul radacina
        folderQueue.add(websiteFolder);

        while (!folderQueue.isEmpty()) // cat timp nu mai sunt foldere copil de parcurs
        {
            // preluam un folder din coada
            String currentFolder = folderQueue.pop();
            File folder = new File(currentFolder);
            File[] listOfFiles = folder.listFiles();

            // ii parcurgem lista de fisiere / foldere
            try {
                for (File file : listOfFiles)
                {
                    // daca am ajuns pe un fisier, verificam sa fie fisier de tip HTML
                    if (file.isFile() && Files.probeContentType(file.toPath()).equals("text/html"))
                    {
                        // parsam fisierul HTML folosind JSOUP
                        Document doc = Jsoup.parse(file, null, baseUri);
                        String fileName = file.getAbsolutePath();
                        // System.out.println("[PARSARE] Am parsat fisierul HTML \"" + fileName + "\".");

                        // stocam textul in fisier separat
                        File textFile = getTextFromHTML(websiteInfo, doc, file);
                        String textFileName = textFile.getAbsolutePath();
                        // System.out.println("[TEXT] Am preluat textul din fisierul HTML \"" + fileName + "\".");

                        // procesam cuvintele, rezultand un HashMap de tip (cuvant -> numar_aparitii)
                        // stocam index-ul direct intr-un fisier cu extensia ".directindex"
                        HashMap<String, Integer> currentDocWords = processDocument(textFileName);

                        // adaugam documentul si lista de cuvinte in HashMap-ul final
                        directIndex.put(fileName, currentDocWords);

                        // adaugam documentul curent, impreuna cu index-ul direct asociat in fisierul de mapare
                        mapFile.put(fileName, fileName + ".directindex.json");

                        // System.out.println("[INDEX_DIRECT] Am creat index-ul direct din fisierul TEXT \"" + textFileName + "\".");
                    }
                    else if (file.isDirectory()) // daca este folder, il punem in coada
                    {
                        folderQueue.add(file.getAbsolutePath());
                    }
                }
            } catch (NullPointerException e) {
                // System.out.println("Nu exista fisiere in folderul \"" + currentFolder + "\"!");
            }
        }

        // scriem fisierul de mapare JSON
        mapFileWriter.write(gsonBuilder.toJson(mapFile));
        mapFileWriter.close();
        // System.out.println(System.lineSeparator());
        // System.out.println("[MAPARE] Am creat fisierul de mapare \"" + websiteFolder + "directindex.map\".");

        return directIndex;
    }
}
