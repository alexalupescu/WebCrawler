# WebCrawler Project

This application was written in the Java language, using the IntelliJ IDEA development environment. Also, two external libraries were used: JSoup (for processing HTML files) and Gson (for processing JSON files). The user interface is represented by command line, menu and interaction being in text mode, simple.

**Module I: Create direct index + _tf_ index**
This module creates the direct index of all HTML documents found in the directories and subdirectories of the source website. A queue is used for directory traversal, because we want a sequential and not a recursive path. The target files are those with the extension ".html", but the application checks the content of the files, not relying on extension alone. There are files with no extension that contain HTML, which are still taken into consideration. From each HTML file, the text is retrieved using the library's internal parser and JSoup, being stored in a separate file. The resulting text is processed character by character. In this way, all the words from the obtained text are extracted, and these are passed through 3 filters: 
  1. are tested against a list of exceptions = words of interest, but not found in the dictionary, so they must be treated as such, in their current form;
  2. check if they are not stopwords = words with no interest/relevance resulting from the search; they are simply ignored;
  3. dictionary words are put through a stemming process, using Porter's algorithm; they are brought to a basic form, eliminating the endings that determine different forms of the same notion. 
A mapping file is also created that indicates, for each individual document, the location of the direct index file, which contains pairs of the form _<word, number of occurrences>_
The_ tf _ index is also created at this stage, due to the ease with which it can be accessed each individual document, locally.

**Module II: Create indirect index + index _idf_**
At this stage, the indirect index is created, which is largely based on the logic of functioning of the previous stage. Each direct index file created previously is retrieved to get the list of words from that document. Each individual word is considered in the creation of the final data structure, which contains pairs of the form _<word, <document, number of t
occurences>>_. Thus, all documents are "unified" in a global indirect index, maintained in a JSON file. 
The _idf_ index is created at this stage because we have global access to documents, and the formula of this index uses the total number of documents and the number of documents in which a word appears. It is most effective in accessing this information itself when the indirect index is created.
As in the previous step, a mapping file is obtained that will contain the index location of the indirect file for each individual file, although a global one is also created, with all the words in the extracted sources.

**Module III: Load indirect index into memory**
When a Boolean search is desired, we must have the indirect index loaded into memory, in a data structure that allows us to search based on keys (the words entered by user, related to operators). In this application, we used _TreeMap<String,
HashMap<String, Integer>>_. Basically, the JSON file is parsed with the indirect index and so on load, input by input, into the given data structure,_ the key : value pairs_.

**Module IV: Boolean search**
It is the simplest search model, which shows us whether a document contains or not a specific key given by the user in the query. It's not possible to provide information related to relevant, under a certain order. The application uses, for the boolean search, the indirect index loaded in memory, and respectively the query given by the user, in the form: _OPERATOR KEY OPERATOR KEY KEY ..._, where OPERATOR = an element from the set {AND, OR, NOT}. For query parsing, 2 stacks are used: one for operands and one for operators. The parsing order is from right to left. All words in the query are passed through
the same 3 filtering processes mentioned previously. The search principle is simple: for a given key found, the set of its returned documents containing that key, using the indirect index. Then, _t_ is applied to that set operator to the left of the current search key. For the operator, the set with the smallest cardinality is traversed and added to result the current document, only if it exists in the other set. At the OR operator, we apply the reverse principle: the number with the largest cardinality is traversed. These "optimizations" results from the desire for a speed that is as good as possible compared to the usual case. The NOT operator is the simplest, being non-commutative: the first set is iterated over checks if the current document doesn't exist in the second, in that case it will be added to the result.

**Module V: Creation of vectors associated with HTML documents**
According to the vector search algorithm, HTML documents must be represented under a form of vectors, which contain elements of the type _<word, tf index x idf index>_. First, the indirect index mapping file is used for retrieval the document list (easier in terms of speed than going through the directory list again). For each separate document, all the words that exist in that document are retrieved, using the local direct index created earlier (it's in a JSON file on disk). For each word in that document, _tf_ and _idf_ indices are calculated, and an added input _<word, tf x idf>_ into the final data structure.
Since this process is time-consuming for large data sets, the result is stored in a JSON file and loaded on demand to perform vectorial searches. The process of loading has an infinitesimal time compared to the actual processing.

**Module VI: Load associated vectors into memory (for vector search)**
As with the Boolean search, it's necessary that the data structure with which we are searching to be in memory, so before using vectorial search for the first time, it is loaded into the memory, in a structure of type _HashMap<String, TreeMap<String, Double>>_ vectors associates HTML documents, thus preparing the similarity calculation that follows in the algorithm.

**Module VII: Vectorial search**
The search principle, in the proposed application, is similar to Boolean search. It parses the user query and splits it into words, using the same 3 filters for the resulting words. The list of keywords is then transformed into a vector of the same form as in the previous step: _<word, tf x idf>_, noting that the index _tf_ is local to the query! In other words, the query is treated as a document in itself, so that the _tf _is calculated using the query as parameter for the document. The cosine similarity is calculated according to the formula:

<img width="264" alt="Capture" src="https://github.com/alexalupescu/WebCrawler/assets/134335603/4dfab2f7-2a45-4193-a581-ec42f9da0708">



The scalar product between the vectors from the numerator is obtained only using the elements that exists in the user query. If the vector d1 has 1000 elements, and the user query contains 3 keywords, then 3 pairs of _tf x idf _values are multiplied and that's it, the rest are being considered 0, as well as for the sums of squares in the calculation of the norms from the numerator. The structure of obtained documents is sorted in descending order, using the relevance criterium as an element of comparison, ignoring documents with relevance 0. It is  presented the final results to the user, ordered list of documents, taken from a structure _SortedSet<HashMap.Entry<String, Double>>_.
​
​
