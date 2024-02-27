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

        // folosim fisierul de mapare al index-ului indirect pentru a prelua toata lista de documente
        File indirectIndexMapFile = new File(websiteFolder + "indirectindex.map");
        Type indirectIndexType = new TypeToken<HashMap<String, String>>(){}.getType();
        HashMap<String, String> indirectIndexCollection = gsonBuilder.fromJson(new String(Files.readAllBytes(indirectIndexMapFile.toPath())), indirectIndexType);

        // iteram prin toate documentele din colectie
        int numberOfDocuments = indirectIndexCollection.keySet().size();
        int documentIndex = 0; // pentru afisarea progresului
        System.out.println();
        for (String document : indirectIndexCollection.keySet())
        {
            ++documentIndex;
            Crawler.printProgress(Crawler.startTime, numberOfDocuments, documentIndex);

            // preluam toate cuvintele existente in documentul curent din index-ul indirect local acelui document
            File indirectIndexFile = new File(indirectIndexCollection.get(document));
            TreeMap<String, HashMap<String, Integer>> indirectIndex = IndirectIndex.loadIndirectIndex(indirectIndexFile.getAbsolutePath(), false);

            // cream vectorul asociat documentului curent
            TreeMap<String, Double> currentDocumentVector = new TreeMap<>();

            for (String word : indirectIndex.keySet())  // pentru fiecare cuvant in parte din document
            {
                // preluam tf-ul si idf-ul
                double tf = getTf(word, document);
                double idf = getIdf(word, websiteInfo);

                // in vectorul documentului curent, adaugam intrarea <cuvant, tf x idf>
                currentDocumentVector.put(word, tf * idf);
            }

            // la final, adaugam documentul in colectia de vectori
            documentVectors.put(document, currentDocumentVector);
        }

        // stocam vectorii asociati intr-un fisier JSON
        Gson documentVectorsGsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        String documentVectorsFile = documentVectorsGsonBuilder.toJson(documentVectors);
        Writer documentVectorsWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(websiteFolder + "documentVectors.json"), "utf-8"));
        documentVectorsWriter.write(documentVectorsFile);
        documentVectorsWriter.close();

        return documentVectors;
    }

    // functia care incarca vectorii asociati documentelor HTML in memorie
    public static HashMap<String, TreeMap<String, Double>> loadAssociatedVectors(WebsiteInfo websiteInfo) throws IOException
    {
        if (associatedVectorsLoaded)
        {
            return associatedVectorsCollection;
        }

        HashMap<String, TreeMap<String, Double>> associatedVectors = new HashMap<>();
        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(websiteInfo.getWebsiteFolder() + "documentVectors.json"), "UTF-8"));

        // parsam JSON-ul manual, obiect cu obiect
        reader.beginObject();
        while(reader.hasNext())
        {
            // aici se citeste fiecare document
            String document = reader.nextName();

            TreeMap<String, Double> currentDocumentVector = new TreeMap<>();

            // aici incep detaliile despre document (cuvant continut -> tf x idf)
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

    // preia valoarea tf-ului pentru un cuvant dat din cadrul unui document
    private static double getTf(String word, String document) throws IOException
    {
        File tfFile = new File(document + ".tf.json");
        Type tfFileType = new TypeToken<TreeMap<String, Double>>(){}.getType();
        Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        TreeMap<String, Double> tfFileCollection = gsonBuilder.fromJson(new String(Files.readAllBytes(tfFile.toPath())), tfFileType);
        return tfFileCollection.get(word);
    }

    // preia valoarea idf-ului pentru un anumit cuvant dat
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

    // calculeaza tf-ul pentru interogarea utilizatorului
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
        double dotProduct = 0; // produsul scalar de la numarator
        double sumSquaresD1 = 0; // sumele de patrate pentru norme
        double sumSquaresD2 = 0;
        double tfIdf1; // tf x idf
        double tfIdf2;

        boolean atLeastOneWordInCommon = false;
        for(String word : queryDoc.keySet())
        {
            if (doc.containsKey(word)) // facem produsele scalare doar pentru elementele ce exista in ambele documente
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

    // pentru sortarea rezultatelor
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
        // impartim interogarea in cuvinte, dupa spatii
        String[] splitQuery = query.split("\\s+");
        ArrayList<String> queryWords = new ArrayList<>();

        int i = 0;
        while (i <= splitQuery.length - 1)
        {
            // ordinea fireasca este: operand OPERATOR operand OPERATOR ...
            String word = splitQuery[i];

            // mai intai, verificam daca este exceptie
            if (ExceptionList.exceptions.contains(word))
            {
                // il adaugam asa cum este
                queryWords.add(word); ++i;
            }
            // apoi daca este stopword
            else if (StopWordList.stopwords.contains(word))
            {
                // ignoram cuvantul de tot
                ++i;
            }
            else // cuvant de dictionar
            {
                // se foloseste algoritmul Porter pentru stemming
                PorterStemmer stemmer = new PorterStemmer();
                stemmer.add(word.toCharArray(), word.length());
                stemmer.stem();
                word = stemmer.toString();

                queryWords.add(word); ++i;
            }
        }

        // transformam interogarea in vector
        TreeMap<String, Double> queryVector = new TreeMap<>();
        for (String word : queryWords)
        {
            queryVector.put(word, getTfQuery(word, queryWords) * getIdf(word, websiteInfo));
        }

        // calculam similaritatile cosinus pentru toate documentele existente
        HashMap<String, Double> similarities = new HashMap<>();
        for (String document: documentVectors.keySet())
        {
            // calculam similaritatea cosinus intre documentul curent si interogarea utilizatorului
            double similarity = cosineSimilarity(documentVectors.get(document), queryVector);
            if (similarity != 0)
            {
                // luam in calcul doar documentele in care exista cel putin un cuvant al utilizatorului
                similarities.put(document, similarity);
            }
        }

        // sortam documentele descrescator d.p.d.v. a similaritatii cosinus
        return entriesSortedByValues(similarities);
    }
}
