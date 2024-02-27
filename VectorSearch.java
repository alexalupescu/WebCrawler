import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class VectorSearch {
    private static HashMap<String, TreeMap<String, Double>> associatedVectorsCollection = null;
    private static boolean associatedVectorsLoaded = false;

    public static HashMap<String, TreeMap<String, Double>> getAssociatedDocumentVectors(WebsiteInfo websiteInfo) throws IOException
    {
        HashMap<String, TreeMap<String, Double>> documentVectors = new HashMap<>();
        Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();

        String websiteFolder = websiteInfo.getWebsiteFolder();

        // we use the indirect index mapping file to retrieve the entire list of documents
        File indirectIndexMapFile = new File(websiteFolder + "indirectindex.map");
        Type indirectIndexType = new TypeToken<HashMap<String, String>>(){}.getType();
        HashMap<String, String> indirectIndexCollection = gsonBuilder.fromJson(new String(Files.readAllBytes(indirectIndexMapFile.toPath())), indirectIndexType);

        // iterate through all the documents in the collection
        int numberOfDocuments = indirectIndexCollection.keySet().size();
        int documentIndex = 0; // for displaying progress
        System.out.println();
        for (String document : indirectIndexCollection.keySet())
        {
            ++documentIndex;
            Crawler.printProgress(Crawler.startTime, numberOfDocuments, documentIndex);

            // we retrieve all the existing words in the current document from the local indirect index of that document
            File indirectIndexFile = new File(indirectIndexCollection.get(document));
            TreeMap<String, HashMap<String, Integer>> indirectIndex = IndirectIndex.loadIndirectIndex(indirectIndexFile.getAbsolutePath(), false);

            // create the vector associated with the current document
            TreeMap<String, Double> currentDocumentVector = new TreeMap<>();

            for (String word : indirectIndex.keySet())  // for each individual word in the document
            {
                // take over the tf and the idf
                double tf = getTf(word, document);
                double idf = getIdf(word, websiteInfo);

                // in the vector of the current document, we add the entry <word, tf x idf>
                currentDocumentVector.put(word, tf * idf);
            }

            // at the end, we add the document to the vector collection
            documentVectors.put(document, currentDocumentVector);
        }

        // store the associated vectors in a JSON file
        Gson documentVectorsGsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        String documentVectorsFile = documentVectorsGsonBuilder.toJson(documentVectors);
        Writer documentVectorsWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(websiteFolder + "documentVectors.json"), "utf-8"));
        documentVectorsWriter.write(documentVectorsFile);
        documentVectorsWriter.close();

        return documentVectors;
    }

    // the function that loads the vectors associated with HTML documents into memory
    public static HashMap<String, TreeMap<String, Double>> loadAssociatedVectors(WebsiteInfo websiteInfo) throws IOException
    {
        if (associatedVectorsLoaded)
        {
            return associatedVectorsCollection;
        }

        HashMap<String, TreeMap<String, Double>> associatedVectors = new HashMap<>();
        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(websiteInfo.getWebsiteFolder() + "documentVectors.json"), "UTF-8"));

        // parse the JSON manually, object by object
        reader.beginObject();
        while(reader.hasNext())
        {
           // read each document here
            String document = reader.nextName();

            TreeMap<String, Double> currentDocumentVector = new TreeMap<>();

            // here start the details about the document (content word -> tf x idf)
            reader.beginObject();
            while (reader.hasNext())
            {
                currentDocumentVector.put(reader.nextName(), reader.nextDouble());
            }
            reader.endObject();

            associatedVectors.put(document, currentDocumentVector);
        }
        reader.endObject();

        associatedVectorsLoaded = true;
        associatedVectorsCollection = associatedVectors;
        return associatedVectors;
    }

    // retrieves the value of the tf for a given word within a document
    private static double getTf(String word, String document) throws IOException
    {
        File tfFile = new File(document + ".tf.json");
        Type tfFileType = new TypeToken<TreeMap<String, Double>>(){}.getType();
        Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        TreeMap<String, Double> tfFileCollection = gsonBuilder.fromJson(new String(Files.readAllBytes(tfFile.toPath())), tfFileType);
        return tfFileCollection.get(word);
    }

    // get the value of the idf for a given word
    private static double getIdf(String word, WebsiteInfo websiteInfo) throws IOException
    {
        File idfFile = new File(websiteInfo.getWebsiteFolder() + "idf.json");
        Type idfFileType = new TypeToken<TreeMap<String, Double>>(){}.getType();
        Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        TreeMap<String, Double> idfFileCollection = gsonBuilder.fromJson(new String(Files.readAllBytes(idfFile.toPath())), idfFileType);
        if (idfFileCollection.containsKey(word))
        {
            return idfFileCollection.get(word);
        }
        return 0;
    }

    // calculate the tf for the user query
    private static double getTfQuery(String word, ArrayList<String> query)
    {
        int numberOfApparitions = 0;
        for (String w : query)
        {
            if (w.equals(word))
            {
                ++numberOfApparitions;
            }
        }
        return (double)numberOfApparitions / query.size();
    }

    private static double cosineSimilarity(TreeMap<String, Double> doc, TreeMap<String, Double> queryDoc)
    {
        double dotProduct = 0; 
        double sumSquaresD1 = 0; 
        double sumSquaresD2 = 0;
        double tfIdf1; // tf x idf
        double tfIdf2;

        boolean atLeastOneWordInCommon = false;
        for(String word : queryDoc.keySet())
        {
            if (doc.containsKey(word)) // we make the scalar products only for the elements that exist in both documents
            {
                atLeastOneWordInCommon = true;
                tfIdf1 = doc.get(word);
                tfIdf2 = queryDoc.get(word);
                dotProduct += Math.abs(tfIdf1 * tfIdf2);
                sumSquaresD1 += tfIdf1 * tfIdf1;
                sumSquaresD2 += tfIdf2 * tfIdf2;
            }
        }

        if (!atLeastOneWordInCommon || dotProduct == 0)
        {
            return 0;
        }
        return Math.abs(dotProduct) / (Math.sqrt(sumSquaresD1) * Math.sqrt(sumSquaresD2));
    }

    // for sorting the results
    static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = e2.getValue().compareTo(e1.getValue());
                        return res != 0 ? res : 1;
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    public static SortedSet<HashMap.Entry<String, Double>> Search(String query, WebsiteInfo websiteInfo, HashMap<String, TreeMap<String, Double>> documentVectors) throws IOException
    {
        // divide the query into words, by spaces
        String[] splitQuery = query.split("\\s+");
        ArrayList<String> queryWords = new ArrayList<>();

        int i = 0;
        while (i <= splitQuery.length - 1)
        {
            // the natural order is: operand OPERATOR operand OPERATOR ...
            String word = splitQuery[i];

            // first, we check if it is an exception
            if (ExceptionList.exceptions.contains(word))
            {
                // we add it as it is
                queryWords.add(word); ++i;
            }
            // then if it is a stopword
            else if (StopWordList.stopwords.contains(word))
            {
                // ignore the word everything
                ++i;
            }
            else // dictionary word
            {
                // the Porter algorithm is used for stemming
                PorterStemmer stemmer = new PorterStemmer();
                stemmer.add(word.toCharArray(), word.length());
                stemmer.stem();
                word = stemmer.toString();

                queryWords.add(word); ++i;
            }
        }

        // we transform the query into a vector
        TreeMap<String, Double> queryVector = new TreeMap<>();
        for (String word : queryWords)
        {
            queryVector.put(word, getTfQuery(word, queryWords) * getIdf(word, websiteInfo));
        }

        // calculate cosine similarities for all existing documents
        HashMap<String, Double> similarities = new HashMap<>();
        for (String document: documentVectors.keySet())
        {
            // calculate the cosine similarity between the current document and the user's query
            double similarity = cosineSimilarity(documentVectors.get(document), queryVector);
            if (similarity != 0)
            {
                // we take into account only the documents in which there is at least one word of the user
                similarities.put(document, similarity);
            }
        }

        // we sort the documents in descending order by of cosine similarity
        return entriesSortedByValues(similarities);
    }
}
