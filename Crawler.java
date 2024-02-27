import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.beans.binding.BooleanExpression;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Crawler {
    static long startTime, stopTime, elapsedTime; 

    static void printProgress(long startTime, long total, long current) 
    {
        long eta = current == 0 ? 0 :
                (total - current) * (System.currentTimeMillis() - startTime) / current;

        String etaHms = current == 0 ? "N/A" :
                String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                        TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

        StringBuilder string = new StringBuilder(140);
        int percent = (int) (current * 100 / total);
        string
                .append('\r')
                .append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
                .append(String.format(" %d%% [", percent))
                .append(String.join("", Collections.nCopies(percent, "=")))
                .append('>')
                .append(String.join("", Collections.nCopies(100 - percent, " ")))
                .append(']')
                .append(String.join("", Collections.nCopies(current == 0 ? (int) (Math.log10(total)) : (int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
                .append(String.format(" %d/%d, Estimated time left: %s", current, total, etaHms));

        System.out.print(string);
    }

    
    public static void main(String[] args) throws IOException {
        System.out.print("Loading website... ");
        WebsiteInfo websiteInfo = new WebsiteInfo("./ietf.org/", "http://ietf.org/");
        System.out.println("OK\n");

        HashMap<String, HashMap<String, Integer>> directIndex = null;
        TreeMap<String, HashMap<String, Integer>> indirectIndex = null;
        HashMap<String, TreeMap<String, Double>> associatedVectors = null;

        // for searches
        String query;
        Set<String> booleanSearchResults;
        SortedSet<HashMap.Entry<String, Double>> vectorSearchResults;
        Scanner queryScanner = new Scanner(System.in);

        // the menu displayed to the user
        do {
            System.out.println("1. Create direct index + indice \"tf\"");
            System.out.println("2. Create indirect index + indice \"idf\"");
            System.out.println("3. Load indirect index in memory (for Boolean search)");
            System.out.println("4. Boolean search");
            System.out.println("5. Creation of vectors associated with HTML documents");
            System.out.println("6. Load associated vectors into memory (for vector search)");
            System.out.println("7. Vectorial search");
            System.out.println("8. Exit");

            System.out.print("Your option ");
            Scanner reader = new Scanner(System.in);
            int option = reader.nextInt();
            System.out.println();

            switch (option)
            {
                case 1:
                    System.out.print("The direct index is being created, please wait... ");
                    startTime = System.currentTimeMillis();
                    try {
                        directIndex = DirectIndex.directIndex(websiteInfo);
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nERROR: Unable to write required files to disk, possibly due to restricted permissions.");
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " seconds)");
                    break;
                case 2:
                    System.out.print("The indirect index is being created, please wait... ");
                    startTime = System.currentTimeMillis();
                    try {
                        indirectIndex = IndirectIndex.indirectIndex(websiteInfo);
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nERROR: The direct index was not created, or the required files cannot be written to disk, possibly due to restricted permissions.");
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " seconds)");
                    break;
                case 3:
                    System.out.print("Loading the indirect index in memory, please wait...");
                    startTime = System.currentTimeMillis();
                    try {
                        indirectIndex = IndirectIndex.loadIndirectIndex(websiteInfo.getWebsiteFolder() + "indirectindex.json", true);
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nERROR: The indirect index was not created, or its associated file cannot be read from disk, possibly due to restricted permissions.");
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " seconds)");
                    break;
                case 4:
                    if (indirectIndex == null)
                    {
                        System.out.println("\nERROR: The indirect index is not created / loaded in memory. Boolean search cannot be performed!");
                        break;
                    }
                    System.out.println("Enter the search query:");
                    query = queryScanner.nextLine();

                    System.out.print("\nLoading... ");
                    startTime = System.currentTimeMillis();
                    booleanSearchResults = BooleanSearch.Search(indirectIndex, query);
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    if (booleanSearchResults != null)
                    {
                        System.out.println("OK (" + booleanSearchResults.size() + " results found in " + (double)elapsedTime / 1000 + " seconds)");
                        System.out.println("\nResults:");
                        for (String doc : booleanSearchResults) {
                            System.out.println("\t" + doc);
                        }
                    }
                    else
                    {
                        System.out.println("no results! (" + (double)elapsedTime / 1000 + " seconds)");
                    }
                    break;
                case 5:
                    if (indirectIndex == null)
                    {
                        System.out.println("\nERROR: The indirect index is not created / loaded in memory. The vectors associated with the documents cannot be created!");
                        break;
                    }
                    System.out.print("The vectors associated with HTML documents are created, waiting...");
                    startTime = System.currentTimeMillis();
                    associatedVectors = VectorSearch.getAssociatedDocumentVectors(websiteInfo);
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("\nOK (" + (double)elapsedTime / 1000 + " seconds)");
                    break;
                case 6:
                    System.out.print("Loading the vectors associated with the documents in memory, wait...");
                    startTime = System.currentTimeMillis();
                    try {
                        associatedVectors = VectorSearch.loadAssociatedVectors(websiteInfo);
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nERROR: The vectors associated with the documents were not created, or the corresponding file cannot be read from disk, possibly due to restricted permissions.");
                        e.printStackTrace();
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " seconds)");
                    break;
                case 7:
                    if (associatedVectors == null)
                    {
                        System.out.println("\nERROR: The vectors associated with the documents were not loaded into memory. Vector search cannot be performed!");
                        break;
                    }
                    System.out.println("Enter the search query:");
                    query = queryScanner.nextLine();

                    System.out.print("\nLoading... ");
                    startTime = System.currentTimeMillis();
                    vectorSearchResults = VectorSearch.Search(query, websiteInfo, associatedVectors);
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    if (vectorSearchResults != null && !vectorSearchResults.isEmpty())
                    {
                        System.out.println("OK (" + vectorSearchResults.size() + " results found in " + (double)elapsedTime / 1000 + " seconds)");
                        System.out.println("\nResults:");
                        for (Map.Entry<String, Double> resultDoc : vectorSearchResults)
                        {
                            System.out.println("\t" + resultDoc.getKey() + " (relevance " + (double)Math.round(resultDoc.getValue() * 100.0 * 100.0) / 100.0 + "%)");
                        }
                    }
                    else
                    {
                        System.out.println("no results! (" + (double)elapsedTime / 1000 + " seconds)");
                    }
                    break;
                case 8:
                    System.exit(0);
                default:
                    System.out.println("\nERROR: The option does not exist!");
            }

            System.out.print("\nPress a key to continue...");
            Scanner cont = new Scanner(System.in);
            cont.nextLine();

            // clear console output
            System.out.print("\033[H\033[2J\n");
            System.out.flush();
            Runtime.getRuntime().exec("clear");
        } while (true);
    }
}
