import java.util.*;

public class BooleanSearch {
    private static Set<String> searchForWord(TreeMap<String, HashMap<String, Integer>> indirectIndex, String word)
    {
        if (!indirectIndex.containsKey(word))
        {
            return null;
        }
        return indirectIndex.get(word).keySet();
    }
    
    private static HashSet<String> applyOperator(Set<String> operand1, Set<String> operand2, String operator)
    {
        HashSet<String> result = new HashSet<>();
        Set<String> firstSet;
        Set<String> secondSet;
        boolean firstSetIsSmaller;

        switch (operator.toLowerCase())
        {
            case "and":

                firstSetIsSmaller = (operand1.size() < operand2.size());
                firstSet = (firstSetIsSmaller) ? operand1 : operand2;
                secondSet = (firstSetIsSmaller) ? operand2 : operand1;
                for (String doc : firstSet) // iteram in prima multime
                {
                    if (secondSet.contains(doc)) 
                    {
                        result.add(doc);
                    }
                }
                return result;
            case "or":

                firstSetIsSmaller = (operand1.size() < operand2.size());
                firstSet = (firstSetIsSmaller) ? operand2 : operand1;
                secondSet = (firstSetIsSmaller) ? operand1 : operand2;
                result.addAll(firstSet);
                for (String doc : secondSet) 
                {
                    if (result.contains(doc))
                    {
                        result.add(doc);
                    }
                }
                return result;
            case "not":
                for (String doc : operand1) 
                {
                    if (!operand2.contains(doc))
                    {
                        result.add(doc); 
                    }
                }
                return result;
            default:
                return null;
        }
    }


    public static Set<String> Search(TreeMap<String, HashMap<String, Integer>> indirectIndex, String query)
    {

        String[] splitQuery = query.split("\\s+");

        Stack<String> operators = new Stack<>();
        Stack<String> operands = new Stack<>();

        int i = splitQuery.length - 1;
        while (i >= 0)
        {

            String word = splitQuery[i];

            if (ExceptionList.exceptions.contains(word))
            {
                operands.push(word); --i;

                if (i >= 0)
                {
                    operators.push(splitQuery[i--]);
                }
            }
            else if (StopWordList.stopwords.contains(word))
            {
                i -= 2;
            }
            else 
            {
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
        Set<String> resultSet = searchForWord(indirectIndex, operands.pop());

        try {
            while (!operands.empty() && !operators.empty()) 
            {
                String operand = operands.pop();
                String operator = operators.pop();

                Set<String> currentSet = searchForWord(indirectIndex, operand);
                resultSet = applyOperator(resultSet, currentSet, operator);
            }
        } catch (NullPointerException e)
        {
            return null;
        }

        return resultSet;
    }
}
