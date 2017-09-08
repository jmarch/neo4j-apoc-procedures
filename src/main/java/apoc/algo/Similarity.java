package apoc.algo;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList; 


public class Similarity {
    @Context
    public GraphDatabaseService db;

    @UserFunction
    @Description("apoc.algo.cosineSimilarity([vector1], [vector2]) " +
            "given two collection vectors, calculate cosine similarity")
    public double cosineSimilarity(@Name("vector1") List<Number> vector1, @Name("vector2") List<Number> vector2) {
        if (vector1.size() != vector2.size() || vector1.size() == 0) {
            throw new RuntimeException("Vectors must be non-empty and of the same size");
        }

        double dotProduct = 0d;
        double xLength = 0d;
        double yLength = 0d;
        for (int i = 0; i < vector1.size(); i++) {
            double weight1 = vector1.get(i).doubleValue();
            double weight2 = vector2.get(i).doubleValue();

            dotProduct += weight1 * weight2;
            xLength += weight1 * weight1;
            yLength += weight2 * weight2;
        }

        xLength = Math.sqrt(xLength);
        yLength = Math.sqrt(yLength);

        return dotProduct / (xLength * yLength);
    }

    @UserFunction
    @Description("apoc.algo.euclideanDistance([vector1], [vector2]) " +
            "given two collection vectors, calculate the euclidean distance (square root of the sum of the squared differences)")
    public double euclideanDistance(@Name("vector1") List<Number> vector1, @Name("vector2") List<Number> vector2) {
        if (vector1.size() != vector2.size() || vector1.size() == 0) {
            throw new RuntimeException("Vectors must be non-empty and of the same size");
        }

        double distance = 0.0;
        for (int i = 0; i < vector1.size(); i++) {
            double sqOfDiff = vector1.get(i).doubleValue() - vector2.get(i).doubleValue();
            sqOfDiff *= sqOfDiff;
            distance += sqOfDiff;
        }
        distance = Math.sqrt(distance);

        return distance;
    }

    @UserFunction
    @Description("apoc.algo.euclideanSimilarity([vector1], [vector2]) " +
            "given two collection vectors, calculate similarity based on euclidean distance")
    public double euclideanSimilarity(@Name("vector1") List<Number> vector1, @Name("vector2") List<Number> vector2) {
        return 1.0d / (1 + euclideanDistance(vector1, vector2));
    }

    @UserFunction
    @Description("apoc.algo.minHashSimilarity([vector1], [vector2]) " +
            "given two collection vectors, calculate MinHash similarity")
    public double minHashSimilarity(@Name("vector1") List<Object> vector1, @Name("vector2") List<Object> vector2) {
        final int HASHES = 400;  // 400 hashes gives 0.05 error rate on the estimator of the Jaccard similarity. 1/sqrt(k) 

        List<String> vector1Hashes = minHashSignature(vector1, HASHES);
        List<String> vector2Hashes = minHashSignature(vector2, HASHES);

        int matches = 0;
        int hashes = vector1Hashes.size();

        for (int h = 0; h < hashes; h++) {
            if (vector1Hashes.get(h).equals(vector2Hashes.get(h))) matches++;
        }
        return 1d * matches / hashes;
    }

    @UserFunction
    @Description("apoc.algo.minHashSignature([vector])" +
            "given a collection vector, calculate a MinHash signature")
    public List<String> minHashSignature(@Name("vector") List<Object> vector, @Name(value = "hashes", defaultValue = "400") double hashes) {
        ArrayList<String> result = new ArrayList<String>();
        for (int h = 0; h < hashes; h++) {
            String salt = String.valueOf(h);
            result.add(minHashOnce(vector, salt));
        }
        return result;
    }

    @UserFunction
    @Description("apoc.algo.minHashOnce([vector], [salt])" +
            "given a collection vector and salt, calculate MinHash value")
    public String minHashOnce(@Name("vector") List<Object> vector, @Name("salt") Object salt) {
        String currMin = "ffffffffffffffffffffffffffffffff";
        String saltStr;
        try {
            saltStr = salt.toString();
        } catch (Exception e) {
            saltStr = String.valueOf(salt);
        }
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            for (int i = 0; i < vector.size(); i++) {
                String input;
                try {
                    input = vector.get(i).toString() + saltStr;
                } catch (Exception e) {
                    input = String.valueOf(vector.get(i)) + saltStr;
                }
                messageDigest.update(input.getBytes());
                byte[] digest = messageDigest.digest();  // implicitly resets messageDigest for next iteration
                StringBuffer sb = new StringBuffer();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b & 0xff));
                }
                String hashedValue = sb.toString();
                if (hashedValue.compareTo(currMin) < 0) currMin = hashedValue;
            }
        } catch (NoSuchAlgorithmException e) {}
        return currMin;
    }
}
