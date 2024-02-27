import java.util.*;

public class BooleanSearch {

    // function that returns the list of documents in which a given word appears
    private static Set<String> searchForWord(TreeMap<String, HashMap<String, Integer>> indirectIndex, String word)
    {
        if (!indirectIndex.containsKey(word))
        {
            return null;
        }
        return indirectIndex.get(word).keySet();
    }

    // function that applies the given operator over 2 operands
    private static HashSet<String> applyOperator(Set<String> operand1, Set<String> operand2, String operator)
    {
        HashSet<String> result = new HashSet<>();
        Set<String> firstSet;
        Set<String> secondSet;
        boolean firstSetIsSmaller;

        switch (operator.toLowerCase())
        {
            case "and":
                // for efficiency, the set with the smaller cardinality is traversed
                firstSetIsSmaller = (operand1.size() < operand2.size());
                firstSet = (firstSetIsSmaller) ? operand1 : operand2;
                secondSet = (firstSetIsSmaller) ? operand2 : operand1;
                for (String doc : firstSet) // iteram in prima multime
                {
                    if (secondSet.contains(doc))  // if the current document also exists in the second set
                    {
                        result.add(doc); // add to the result
                    }
                }
                return result;
            case "or":
                // for efficiency, the set with the larger cardinality is cloned
                firstSetIsSmaller = (operand1.size() < operand2.size());
                firstSet = (firstSetIsSmaller) ? operand2 : operand1;
                secondSet = (firstSetIsSmaller) ? operand1 : operand2;
                result.addAll(firstSet);
                for (String doc : secondSet) // we iterate in the second set, with the smaller cardinal
                {
                    if (result.contains(doc)) // if the current document does not exist in the first set
                    {
                        result.add(doc); // add to the result
                    }
                }
                return result;
            case "not":
                for (String doc : operand1)  // we iterate through the first set
                {
                    if (!operand2.contains(doc)) // if the current document does not exist in the second set
                    {
                        result.add(doc); // add to the result
                    }
                }
                return result;
            default:
                return null;
        }
    }

    // function that performs the boolean search according to the query given by the user
    public static Set<String> Search(TreeMap<String, HashMap<String, Integer>> indirectIndex, String query)
    {
        // divide the query into words, by spaces
        String[] splitQuery = query.split("\\s+");

        // create two stacks, one for operators and one for operands
        Stack<String> operators = new Stack<>();
        Stack<String> operands = new Stack<>();

        // we store operators and operands in stacks
        // we go from tail to head because we will parse the expression from right to left. top of stack = first operand etc.
        int i = splitQuery.length - 1;
        while (i >= 0)
        {
            // the natural order is: operand OPERATOR operand OPERATOR ...
            String word = splitQuery[i];

            // first, we check if it is an exception
            if (ExceptionList.exceptions.contains(word))
            {
                // we add it as it is
                operands.push(word); --i;

                if (i >= 0)
                {
                    operators.push(splitQuery[i--]);
                }
            }
            // then if it is a stopword
            else if (StopWordList.stopwords.contains(word))
            {
                // ignore both the word and its associated operator
                i -= 2;
            }
            else 
            { // dictionary word
                // the Porter algorithm is used for stemming
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
        // remove the first operand and consider it as "the first search result"
        Set<String> resultSet = searchForWord(indirectIndex, operands.pop());

        try {
            while (!operands.empty() && !operators.empty())  // until we empty both stacks
            {
                // output one operator and one operand
                String operand = operands.pop();
                String operator = operators.pop();
                
                // create the set of documents in which the current operand appears
                Set<String> currentSet = searchForWord(indirectIndex, operand);

                // apply the operation and store the result
                resultSet = applyOperator(resultSet, currentSet, operator);
            }
        } catch (NullPointerException e)
        {
            return null;
        }

        return resultSet;
    }
}
