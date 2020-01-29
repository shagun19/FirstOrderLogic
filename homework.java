import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class homework {
    private static final String INPUT_TXT = "//input.txt";
    private static final String OUTPUT_TXT = "//output.txt";
    private static final char NEGATION = '~';
    private static final char AND = '&';
    private static final char OR = '|';
    private static final String SPACE = " ";
    private static final String IMPLY = "=>";
    private static final String COMMA = ",";
    private static final char LEFT_PARENTHESES = '(';
    private static final char RIGHT_PARENTHESES = ')';
    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";
    private static final int UPPER_LIMIT = 2000;

    private static String negateLiteral(String literal){
        String[] literals = literal.split(String.valueOf(AND));
        String negateLiteral = "";
        if(literals.length>1){
            for(int i=0; i<literals.length; i++){
                literals[i]=literals[i].trim();
                if(i!=literals.length-1){
                    if(literals[i].charAt(0)==NEGATION) negateLiteral+=literals[i].trim()+SPACE+OR+SPACE;
                    else negateLiteral+=NEGATION+literals[i].trim()+SPACE+OR+SPACE;
                }
                else{
                    if(literals[i].charAt(0)==NEGATION) negateLiteral+=literals[i].trim()+SPACE;
                    else negateLiteral+=NEGATION+literals[i].trim()+SPACE;
                }
            }
        }
        else{
            if(literal.charAt(0)==NEGATION) negateLiteral=literal.trim().substring(1)+SPACE;
            else negateLiteral=NEGATION+literal.trim()+SPACE;
        }
        return negateLiteral;
    }

    private static String removeSpaces(String literal){
        literal=literal.replace(LEFT_PARENTHESES+SPACE,String.valueOf(LEFT_PARENTHESES));
        literal=literal.replace(SPACE+COMMA,COMMA);
        literal=literal.replace(COMMA+SPACE,COMMA);
        literal=literal.replace(SPACE+RIGHT_PARENTHESES,String.valueOf(RIGHT_PARENTHESES));
        return literal;
    }

    private static HashMap<String,List<String>> cloneKnowledgeBase(HashMap<String,List<String>> original){
        HashMap<String ,List<String>> copy = new HashMap<>();
        for(String key : original.keySet()){
            String copyKey = key;
            List<String> copyVal = new ArrayList<>(original.get(key));
            copy.put(copyKey,copyVal);
        }
        return copy;
    }

    private static List<String> convertToCNF(List<String> kbSentences){
        List<String> convertedCNF = new ArrayList<>();
        for(String kbSentence : kbSentences){
            String[] sentenceSplit = kbSentence.split(IMPLY);
            if(sentenceSplit.length>1){
                String predicate = sentenceSplit[0];
                String conclusion = sentenceSplit[1];
                String negatedPredicate = negateLiteral(predicate);
                convertedCNF.add(negatedPredicate+OR+conclusion);
            }
            else convertedCNF.add(kbSentence);
        }
        return convertedCNF;
    }

    private static String getPredicateName(String predicate){
        return predicate.split("\\(")[0];
    }

    private static List<String> getIndividualLiterals(String literals){
        literals=removeSpaces(literals);
        String[] individualLiteral = literals.split("\\s+");
        List<String> individualLiterals = new ArrayList<>();
        for(int i=0;i<individualLiteral.length;i++){
            if(!individualLiteral[i].equals(String.valueOf(OR)) && !individualLiteral[i].equals(String.valueOf(AND))){
                individualLiterals.add(individualLiteral[i]);
            }
        }
        return individualLiterals;
    }

    private static HashMap<String,List<String>> tableIndexing(List<String> convertedCNF){
        HashMap<String,List<String>> tableIndex = new HashMap<>();
        for(String literals : convertedCNF){
            List<String> individualLiterals = getIndividualLiterals(literals);
            for(String individualLiteral : individualLiterals){
                String predicateKey = getPredicateName(individualLiteral);
                if(!tableIndex.containsKey(predicateKey)){
                    List<String> allPredicateValues = new ArrayList<>();
                    allPredicateValues.add(literals);
                    tableIndex.put(predicateKey,allPredicateValues);
                }
                else{
                    List<String> allPredicateValues = tableIndex.get(predicateKey);
                    if(!allPredicateValues.contains(literals)) allPredicateValues.add(literals);
                    tableIndex.put(predicateKey,allPredicateValues);
                }
            }
        }
        return tableIndex;
    }

    private static boolean checkIfVariable(String arg, boolean initial){
        boolean result = false;
        if(initial && Character.isLowerCase(arg.charAt(0))){
            for(int i=1;i<arg.length();i++){
                if(!Character.isDigit(arg.charAt(i))) return false;
                else result=true;
            }
        }
        else if(!initial && Character.isLowerCase(arg.charAt(0)) && arg.length()==1) return true;
        return result;
    }

    private static List<String> standardizeSentences(List<String> convertedCNF){
        List<String> standardizedCNF = new ArrayList<>();
        int count = 0;
        for(String sentences: convertedCNF){
            List<String> predicates = getIndividualLiterals(sentences);
            String standardizedSentence = "";
            for(String predicate : predicates){
                String[] args = predicate.trim().substring(predicate.indexOf(LEFT_PARENTHESES)+1,
                        predicate.indexOf(RIGHT_PARENTHESES)).split(COMMA);
                for(int i=0;i<args.length;i++){
                    if(checkIfVariable(args[i], false)) args[i]=args[i]+count;
                }
                String predicateStandardized = predicate.substring(0,predicate.indexOf(LEFT_PARENTHESES)+1);
                for(int i=0;i<args.length;i++){
                    if(i==args.length-1) predicateStandardized+=args[i]+RIGHT_PARENTHESES;
                    else predicateStandardized+=args[i]+COMMA;
                }
                if(predicates.indexOf(predicate)==predicates.size()-1) standardizedSentence+=predicateStandardized;
                else standardizedSentence+=predicateStandardized+SPACE+OR+SPACE;
            }
            standardizedCNF.add(standardizedSentence);
            count+=1;
        }
        return standardizedCNF;
   }

   private static boolean resolveKnowledgeBase(String query, List<String> convertedCNF,
                                               HashMap<String,List<String>> knowledgeBase){
        HashMap<String,List<String>> knowledgeBaseCopy = cloneKnowledgeBase(knowledgeBase);
        List<String> convertedCNFCopy = new ArrayList<>(convertedCNF);
        convertedCNFCopy.add(0,negateLiteral(query));

       int kbSizeDifference=-1;
       while(kbSizeDifference!=0) {
           int size = convertedCNFCopy.size();
           for (int i=0;i<convertedCNFCopy.size();i++) {
               if (convertedCNFCopy.size() > UPPER_LIMIT){
                   return false;
               }
               if(unify(convertedCNFCopy.get(i),convertedCNFCopy,knowledgeBaseCopy))
                   return true;
           }
           kbSizeDifference = convertedCNFCopy.size()-size;
       }
       return false;
   }

   private static String[] getArgumentsForPredicate(String predicate){
       return predicate.substring(predicate.indexOf(LEFT_PARENTHESES) + 1,
               predicate.indexOf(RIGHT_PARENTHESES)).split(COMMA);
   }

   private static HashMap<String,String> getSubstitutionMap(String[] queryArgs, String[] matchingArgs){
        HashMap<String,String> substitutionMap = new HashMap<>();
        for(int x=0;x<queryArgs.length;x++){
           if(queryArgs[x].equals(matchingArgs[x])){
               substitutionMap.put(queryArgs[x],queryArgs[x]);
           }
           else{
               if(checkIfVariable(queryArgs[x], true) && !checkIfVariable(matchingArgs[x],true)){
                   if(!substitutionMap.containsKey(queryArgs[x])){
                       substitutionMap.put(queryArgs[x], matchingArgs[x]);
                   }
                   else if(substitutionMap.containsKey(queryArgs[x]) &&
                           substitutionMap.get(queryArgs[x]).equals(matchingArgs[x])){
                       continue;
                   }
                   else{
                       substitutionMap.clear();
                       break;
                   }
               }
               else if(!checkIfVariable(queryArgs[x], true) && checkIfVariable(matchingArgs[x], true)){
                   if(!substitutionMap.containsKey(matchingArgs[x])){
                       substitutionMap.put(matchingArgs[x], queryArgs[x]);
                   }
                   else if(substitutionMap.containsKey(matchingArgs[x]) &&
                           substitutionMap.get(matchingArgs[x]).equals(queryArgs[x])){
                       continue;
                   }
                   else{
                       substitutionMap.clear();
                       break;
                   }
               }
               else if(!checkIfVariable(queryArgs[x], true) && !checkIfVariable(matchingArgs[x],
                       true)){
                   substitutionMap.clear();
                   break;
               }
               else{
                   if(substitutionMap.containsKey(queryArgs[x])){
                       substitutionMap.put(matchingArgs[x], substitutionMap.get(queryArgs[x]));
                   }
                   else if(!substitutionMap.containsKey(matchingArgs[x]))
                       substitutionMap.put(matchingArgs[x],queryArgs[x]);
                   else{
                       substitutionMap.clear();
                       break;
                   }
               }
           }
       }
        return substitutionMap;
   }

    private static boolean unify(String query, List<String> convertedCNF, HashMap<String,List<String>>
    knowledgeBase) {
        List<String> splittedQueries = getIndividualLiterals(query);
        if(splittedQueries.size()==1){
            String queryToCheck = negateLiteral(splittedQueries.get(0));
            if (convertedCNF.contains(queryToCheck)){
                return true;
            }
        }
        for(int i=0;i<splittedQueries.size();i++) {
            String firstSplittedQuery = negateLiteral(splittedQueries.get(i));
            String[] args = getArgumentsForPredicate(firstSplittedQuery); // Get
            String firstSplittedQueryPredicateName = getPredicateName(firstSplittedQuery);
            List<String> predicateSentences = knowledgeBase.get(firstSplittedQueryPredicateName);
            boolean queryFound = false;
            if(predicateSentences==null) continue;
            if (predicateSentences.size() > 0) {
                for (String predicateSentence : predicateSentences) {
                    if (query.equals(predicateSentence)) {
                        queryFound=true;
                    }
                }
            }
            if(queryFound) continue;
            for(int j=0;j<predicateSentences.size();j++){
                String otherQueryPredicateSentence = predicateSentences.get(j);
                HashMap<String,String> substitutionMap = new HashMap<>();
                List<String> predicatesFromQuerySentences = getIndividualLiterals(predicateSentences.get(j));
                String otherQuery = null;
                for(int k=0;k<predicatesFromQuerySentences.size();k++){
                    otherQuery=predicatesFromQuerySentences.get(k);
                    if(firstSplittedQueryPredicateName.equals(getPredicateName(predicatesFromQuerySentences.get(k)))){
                        String[] argsFromSentencePredicate =
                                getArgumentsForPredicate(predicatesFromQuerySentences.get(k));
                        if(args.length==argsFromSentencePredicate.length){
                            substitutionMap = getSubstitutionMap(args,argsFromSentencePredicate);
                            if(substitutionMap.size()>0) break;
                        }
                    }
                }
                if(substitutionMap.size()>0){
                    String unificationResult1 = unifySentences(negateLiteral(firstSplittedQuery),
                            getIndividualLiterals(query), substitutionMap);
                    String unificationResult2 = unifySentences(otherQuery,
                            getIndividualLiterals(otherQueryPredicateSentence), substitutionMap);
                    String unificationResult = compareUnifiedSentences(unificationResult1,unificationResult2);
                    if(unificationResult.isEmpty()){
                        return true;
                    }
                    else {
                        List<String> predicateFromUnifiedResult = getIndividualLiterals(unificationResult);
                        if(predicateFromUnifiedResult.size()==1 && convertedCNF.contains(negateLiteral(unificationResult)))
                            return true;
                        if(convertedCNF.contains(unificationResult)) continue;
                        if(!convertedCNF.contains(unificationResult)) convertedCNF.add(unificationResult);
                        addUnifiedSentencesToKB(unificationResult,knowledgeBase);
                    }
                }
            }
            break;
        }
        return false;
    }

    private static String unifySentences(String query1, List<String> individualLiterals,
                                         HashMap<String,String> substitutionMap) {
        List<String> completeUnified = new ArrayList<>();
        for (String predicate : individualLiterals) {
            String unified = "";
            if (!(query1.trim()).equals(predicate.trim())) {
                unified+=getPredicateName(predicate) + LEFT_PARENTHESES;
                String[] args = getArgumentsForPredicate(predicate);
                for (int i = 0; i < args.length; i++) {
                    if(i!=args.length-1){
                        if (substitutionMap.containsKey(args[i])) unified += substitutionMap.get(args[i]) + COMMA;
                        else unified+=args[i]+COMMA;
                    }
                    else{
                        if (substitutionMap.containsKey(args[i])) unified += substitutionMap.get(args[i]) + RIGHT_PARENTHESES;
                        else unified+=args[i]+RIGHT_PARENTHESES;
                    }
                }
            }
            if(!unified.isEmpty()) completeUnified.add(unified);
        }
        String res = "";
        Collections.sort(completeUnified);
        for(int i=0; i<completeUnified.size();i++){
            if(i!=completeUnified.size()-1) res+=completeUnified.get(i)+SPACE+OR+SPACE;
            else res+=completeUnified.get(i);
        }
        return res;
    }

    private static String compareUnifiedSentences(String unificationResult1, String unificationResult2){
        List<String> unificationResult = new ArrayList<>();
        if(unificationResult1.isEmpty() && !unificationResult2.isEmpty()) return unificationResult2;
        if(!unificationResult1.isEmpty() && unificationResult2.isEmpty()) return unificationResult1;
        if(unificationResult1.isEmpty() && unificationResult2.isEmpty()) return "";
        unificationResult.add(unificationResult1);
        unificationResult.add(unificationResult2);
        Collections.sort(unificationResult);
        return unificationResult.get(0)+SPACE+OR+SPACE+unificationResult.get(1);
    }

    private static void addUnifiedSentencesToKB(String inferredSentence, HashMap<String,
            List<String>> knowledgeBase){
        List<String> predicates = getIndividualLiterals(inferredSentence);
        for(String predicate : predicates){
            String predicateKey = getPredicateName(predicate);
            if(knowledgeBase.containsKey(predicateKey)){
                List<String> alreadyPresentSentences = knowledgeBase.get(predicateKey);
                if(!alreadyPresentSentences.contains(inferredSentence)) alreadyPresentSentences.add(inferredSentence);
                knowledgeBase.put(predicateKey,alreadyPresentSentences);
            }
            else{
                List<String> newSentences = new ArrayList<>();
                newSentences.add(inferredSentence);
                knowledgeBase.put(predicateKey,newSentences);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String inputFileName = System.getProperty("user.dir")+INPUT_TXT;
        String outputFileName = System.getProperty("user.dir")+OUTPUT_TXT;
        Path outputPath = Paths.get(outputFileName);
        List<String> inputList = Files.readAllLines(Paths.get(inputFileName));
        List<String> queries = new ArrayList<>();
        int numberOfQueries = Integer.parseInt(inputList.get(0));
        for(int i=1;i<numberOfQueries+1;i++){
            queries.add(inputList.get(i));
        }
        List<String> convertedCNF = convertToCNF(inputList.subList(numberOfQueries+2,inputList.size()));
        List<String> standardisedCNF = standardizeSentences(convertedCNF);
        HashMap<String,List<String>> knowledgeBase = tableIndexing(standardisedCNF);
        String queryResult = "";
        for(String query: queries){
            boolean result = resolveKnowledgeBase(query,standardisedCNF,knowledgeBase);
            if(queries.indexOf(query)!=queries.size()-1){
                if(result) queryResult+=TRUE+"\n";
                else queryResult+=FALSE+"\n";
            }
            else{
                if(result) queryResult+=TRUE;
                else queryResult+=FALSE;
            }
        }
        Files.write(outputPath,queryResult.getBytes());
    }
}

