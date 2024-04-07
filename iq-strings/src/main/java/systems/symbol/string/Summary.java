package systems.symbol.string;

import java.util.*;
import java.util.***REMOVED***.Pattern;

/**
 * Text-Summarizer (very experimental)
 *
 *  Based on code found @  https://github.com/karimo94/Text-Summarizer/blob/master/Summarizer.java
 */
public class Summary {

private static List<String> sortByFreqThenDropFreq(Map<String,Integer> wordFrequencies)
{
//sort the dictionary, sort by frequency and drop counts ['code', language']
//return a List<string>
List<String> sortedCollection = new ArrayList<String>(wordFrequencies.keySet());
Collections.sort(sortedCollection);
Collections.reverse(sortedCollection);	//largest to smallest
return sortedCollection;
}

private static Map<String, Integer> getWordCounts(String text)
{
Map<String,Integer> allWords = new HashMap<String, Integer>();

/* start with raw frequencies
 * scan entire text and record all words and word counts
 * so if a word appears multiple times, increment the word count for that particular word
 * if a word appears only once, add the new word to the Map
 */
text.trim();
String[] words = text.split("\\s+");//split with white space delimiters

for(int i = 0; i < words.length; i++)
{

if(allWords.containsKey(words[i]))//do a check to see if a word already exists in the collection
{
allWords.put(words[i], allWords.get(words[i]) + 2);
}
else
{
allWords.put(words[i], 1);
}
}
return allWords;
}

private static String search(String[] sentences, String word)
{
//search for a particular sentence containing a particular word
//this function will return the first matching sentence that has a value word
String first_matching_sentence = null;
for(int i = 0; i < sentences.length; i++)
{
if(sentences[i].contains(word))
{
first_matching_sentence = sentences[i];
}
}
return first_matching_sentence;
}


private static String[] getSentences(String text) {
//we need to fix alphabet letters like A. B. etc...use a ***REMOVED***
text = text.replaceAll("([A-Z])\\.", "$1");

//split using ., !, ?, and omit decimal numbers
String pattern = "(?<!\\d)\\.(?!\\d)|(?<=\\d)\\.(?!\\d)|(?<!\\d)\\.(?=\\d)";
Pattern pt = Pattern.compile(pattern);

String[] sentences = pt.split(text);
return sentences;
}

public static String summary(String text, int maxSummarySize) {
//start with raw freqs
Map<String, Integer> wordCounts = getWordCounts(text);

//sort
List<String> sorted = sortByFreqThenDropFreq(wordCounts);

//split the sentences
String[] sentences = getSentences(text);

//select up to maxSummarySize sentences, so create a List<String>
List<String> setSummarySentences = new ArrayList<String>();

//foreach string in the sorted list
for(String word : sorted)
{
String matching_sentence = search(sentences, word);
//add to summary list
if (!setSummarySentences.contains(matching_sentence) && setSummarySentences.size() < maxSummarySize) {
setSummarySentences.add(matching_sentence);
System.out.println("matching_sentence: "+matching_sentence);
}
}

//construct the summary size out of select sentences
StringBuilder summary = new StringBuilder();
for(String sentence : setSummarySentences)//foreach string sentence in sentences list
{
if(setSummarySentences.contains(sentence))
{
//produce each sentence with a bullet point and good amounts of spacing
summary.append(sentence.trim()+".");
}
}
return summary.toString();
}
}
