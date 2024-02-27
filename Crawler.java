import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.beans.binding.BooleanExpression;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Crawler {
    static long startTime, stopTime, elapsedTime; // pentru masurarea timpilor de executie

    static void printProgress(long startTime, long total, long current) // pentru afisarea progresului
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
                .append(String.format(" %d/%d, Timp estimativ ramas: %s", current, total, etaHms));

        System.out.print(string);
    }

    public static void main(String[] args) throws IOException {
        System.out.print("Incarcare website... ");
        WebsiteInfo websiteInfo = new WebsiteInfo("./ietf.org/", "http://ietf.org/");
        System.out.println("OK\n");

        HashMap<String, HashMap<String, Integer>> directIndex = null;
        TreeMap<String, HashMap<String, Integer>> indirectIndex = null;
        HashMap<String, TreeMap<String, Double>> associatedVectors = null;

        // pentru cautari
        String query;
        Set<String> booleanSearchResults;
        SortedSet<HashMap.Entry<String, Double>> vectorSearchResults;
        Scanner queryScanner = new Scanner(System.in);

        // meniul afisat utilizatorului
        do {
            System.out.println("1. Creare index direct + indice \"tf\"");
            System.out.println("2. Creare index indirect + indice \"idf\"");
            System.out.println("3. Incarcare index indirect in memorie (pentru cautarea booleana)");
            System.out.println("4. Cautare booleana");
            System.out.println("5. Creare vectori asociati documentelor HTML");
            System.out.println("6. Incarcare vectori asociati in memorie (pentru cautarea vectoriala)");
            System.out.println("7. Cautare vectoriala");
            System.out.println("8. Iesire");

            System.out.print("Optiunea dvs: ");
            Scanner reader = new Scanner(System.in);
            int option = reader.nextInt();
            System.out.println();

            switch (option)
            {
                case 1:
                    System.out.print("Se creeaza index-ul direct, asteptati... ");
                    startTime = System.currentTimeMillis();
                    try {
                        directIndex = DirectIndex.directIndex(websiteInfo);
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nEROARE: Nu se pot scrie fisierele necesare pe disc, posibil din cauza permisiunilor restrictionate.");
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " secunde)");
                    break;
                case 2:
                    System.out.print("Se creeaza index-ul indirect, asteptati... ");
                    startTime = System.currentTimeMillis();
                    try {
                        indirectIndex = IndirectIndex.indirectIndex(websiteInfo);
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nEROARE: Index-ul direct nu a fost creat, sau nu se pot scrie fisierele necesare pe disc, posibil din cauza permisiunilor restrictionate.");
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " secunde)");
                    break;
                case 3:
                    System.out.print("Se incarca index-ul indirect in memorie, asteptati... ");
                    startTime = System.currentTimeMillis();
                    try {
                        indirectIndex = IndirectIndex.loadIndirectIndex(websiteInfo.getWebsiteFolder() + "indirectindex.json", true);
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nEROARE: Index-ul indirect nu a fost creat, sau fisierul asociat acestuia nu poate fi citit de pe disc, posibil din cauza permisiunilor restrictionate.");
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " secunde)");
                    break;
                case 4:
                    if (indirectIndex == null)
                    {
                        System.out.println("\nEROARE: Index-ul indirect nu este creat / incarcat in memorie. Nu se poate efectua cautarea booleana!");
                        break;
                    }
                    System.out.println("Introduceti interogarea pentru cautare:");
                    query = queryScanner.nextLine();

                    System.out.print("\nSe cauta... ");
                    startTime = System.currentTimeMillis();
                    booleanSearchResults = BooleanSearch.Search(indirectIndex, query);
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    if (booleanSearchResults != null)
                    {
                        System.out.println("OK (" + booleanSearchResults.size() + " rezultate gasite in " + (double)elapsedTime / 1000 + " secunde)");
                        System.out.println("\nRezultatele cautarii:");
                        for (String doc : booleanSearchResults) {
                            System.out.println("\t" + doc);
                        }
                    }
                    else
                    {
                        System.out.println("niciun rezultat gasit! (" + (double)elapsedTime / 1000 + " secunde)");
                    }
                    break;
                case 5:
                    if (indirectIndex == null)
                    {
                        System.out.println("\nEROARE: Index-ul indirect nu este creat / incarcat in memorie. Nu se pot crea vectorii asociati documentelor!");
                        break;
                    }
                    System.out.print("Se creeaza vectorii asociati documentelor HTML, asteptati... ");
                    startTime = System.currentTimeMillis();
                    associatedVectors = VectorSearch.getAssociatedDocumentVectors(websiteInfo);
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("\nOK (" + (double)elapsedTime / 1000 + " secunde)");
                    break;
                case 6:
                    System.out.print("Se incarca vectorii asociati documentelor in memorie, asteptati... ");
                    startTime = System.currentTimeMillis();
                    try {
                        associatedVectors = VectorSearch.loadAssociatedVectors(websiteInfo);
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nEROARE: Vectorii asociati documentelor nu au fost creati, sau fisierul corespunzator nu poate fi citit de pe disc, posibil din cauza permisiunilor restrictionate.");
                        e.printStackTrace();
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " secunde)");
                    break;
                case 7:
                    if (associatedVectors == null)
                    {
                        System.out.println("\nEROARE: Vectorii asociati documentelor nu au fost incarcati in memorie. Nu se poate efectua cautarea vectoriala!");
                        break;
                    }
                    System.out.println("Introduceti interogarea pentru cautare:");
                    query = queryScanner.nextLine();

                    System.out.print("\nSe cauta... ");
                    startTime = System.currentTimeMillis();
                    vectorSearchResults = VectorSearch.Search(query, websiteInfo, associatedVectors);
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    if (vectorSearchResults != null && !vectorSearchResults.isEmpty())
                    {
                        System.out.println("OK (" + vectorSearchResults.size() + " rezultate gasite in " + (double)elapsedTime / 1000 + " secunde)");
                        System.out.println("\nRezultatele cautarii:");
                        for (Map.Entry<String, Double> resultDoc : vectorSearchResults)
                        {
                            System.out.println("\t" + resultDoc.getKey() + " (relevanta " + (double)Math.round(resultDoc.getValue() * 100.0 * 100.0) / 100.0 + "%)");
                        }
                    }
                    else
                    {
                        System.out.println("niciun rezultat gasit! (" + (double)elapsedTime / 1000 + " secunde)");
                    }
                    break;
                case 8:
                    System.exit(0);
                default:
                    System.out.println("\nEROARE: Optiunea nu exista!");
            }

            System.out.print("\nApasati o tasta pentru continuare...");
            Scanner cont = new Scanner(System.in);
            cont.nextLine();

            // stergere output din consola
            System.out.print("\033[H\033[2J\n");
            System.out.flush();
            Runtime.getRuntime().exec("clear");
        } while (true);
    }
}
