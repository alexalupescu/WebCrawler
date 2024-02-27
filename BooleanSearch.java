import java.util.*;

public class BooleanSearch {
    // functie care returneaza lista de documente in care apare un cuvant dat
    private static Set<String> searchForWord(TreeMap<String, HashMap<String, Integer>> indirectIndex, String word)
    {
        if (!indirectIndex.containsKey(word))
        {
            return null;
        }
        return indirectIndex.get(word).keySet();
    }

    // functie care aplica operatorul dat peste 2 operanzi
    private static HashSet<String> applyOperator(Set<String> operand1, Set<String> operand2, String operator)
    {
        HashSet<String> result = new HashSet<>();
        Set<String> firstSet;
        Set<String> secondSet;
        boolean firstSetIsSmaller;

        switch (operator.toLowerCase())
        {
            case "and":
                // pentru eficienta, se parcurge multimea cu cardinalul mai mic
                firstSetIsSmaller = (operand1.size() < operand2.size());
                firstSet = (firstSetIsSmaller) ? operand1 : operand2;
                secondSet = (firstSetIsSmaller) ? operand2 : operand1;
                for (String doc : firstSet) // iteram in prima multime
                {
                    if (secondSet.contains(doc)) // daca documentul curent exista si in a doua multime
                    {
                        result.add(doc); // adaugam la rezultat
                    }
                }
                return result;
            case "or":
                // pentru eficienta, se cloneaza multimea cu cardinalul mai mare
                firstSetIsSmaller = (operand1.size() < operand2.size());
                firstSet = (firstSetIsSmaller) ? operand2 : operand1;
                secondSet = (firstSetIsSmaller) ? operand1 : operand2;
                result.addAll(firstSet);
                for (String doc : secondSet) // iteram in a doua multime, cu cardinalul mai mic
                {
                    if (result.contains(doc)) // daca documentul curent nu exista in prima multime
                    {
                        result.add(doc); // adaugam la rezultat
                    }
                }
                return result;
            case "not":
                for (String doc : operand1) // iteram in prima multime
                {
                    if (!operand2.contains(doc)) // daca documentul curent nu exista in a doua multime
                    {
                        result.add(doc); // adaugam la rezultat
                    }
                }
                return result;
            default:
                return null;
        }
    }

    // functie care realizeaza cautarea booleana in functie de interogarea data de utilizator
    public static Set<String> Search(TreeMap<String, HashMap<String, Integer>> indirectIndex, String query)
    {
        // impartim interogarea in cuvinte, dupa spatii
        String[] splitQuery = query.split("\\s+");

        // cream doua stive, una de operatori si una de operanzi
        Stack<String> operators = new Stack<>();
        Stack<String> operands = new Stack<>();

        // stocam operatorii si operanzii in stive
        // parcurgem de la coada la cap pentru ca vom parsa expresia de la dreapta la stanga. varful stivei = primul operand s.a.m.d.
        int i = splitQuery.length - 1;
        while (i >= 0)
        {
            // ordinea fireasca este: operand OPERATOR operand OPERATOR ...
            String word = splitQuery[i];

            // mai intai, verificam daca este exceptie
            if (ExceptionList.exceptions.contains(word))
            {
                // il adaugam asa cum este
                operands.push(word); --i;

                if (i >= 0)
                {
                    operators.push(splitQuery[i--]);
                }
            }
            // apoi daca este stopword
            else if (StopWordList.stopwords.contains(word))
            {
                // ignoram si cuvantul si operatorul asociat lui
                i -= 2;
            }
            else // cuvant de dictionar
            {
                // se foloseste algoritmul Porter pentru stemming
                PorterStemmer stemmer = new PorterStemmer();
                stemmer.add(word.toCharArray(), word.length());
                stemmer.stem();
                word = stemmer.toString();

                operands.push(word); --i;

                if (i >= 0)
                {
                    operators.push(splitQuery[i--]);
                }
            }
        }

        // scoatem primul operand si il consideram ca "primul rezultat de cautare"
        Set<String> resultSet = searchForWord(indirectIndex, operands.pop());

        try {
            while (!operands.empty() && !operators.empty()) // pana golim ambele stive
            {
                // scoatem cate un operator si cate un operand
                String operand = operands.pop();
                String operator = operators.pop();

                // cream multimea de documente in care apare operandul curent
                Set<String> currentSet = searchForWord(indirectIndex, operand);

                // aplicam operatia si stocam rezultatul
                resultSet = applyOperator(resultSet, currentSet, operator);
            }
        } catch (NullPointerException e)
        {
            return null;
        }

        return resultSet;
    }
}
