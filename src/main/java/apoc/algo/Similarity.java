package apoc.algo;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList; 
import java.util.Arrays; 


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
            "given two collection vectors, calculate a MinHash-based similarity")
    public double minHashSimilarity(@Name("vector1") List<Object> vector1, @Name("vector2") List<Object> vector2, @Name(value = "hashes", defaultValue = "400") Long hashes) throws NoSuchAlgorithmException {
        // Note: 400 hashes gives 0.05 error rate on the estimator of the Jaccard similarity. 1/sqrt(k) 
        List<Long> vector1Hashes = minHashSignature(vector1, hashes);
        List<Long> vector2Hashes = minHashSignature(vector2, hashes);
        int matches = 0;
        for (int h = 0; h < hashes; h++) {
            if (vector1Hashes.get(h).equals(vector2Hashes.get(h))) matches++;
        }
        return 1d * matches / hashes;
    }

    @UserFunction
    @Description("apoc.algo.minHashSignature([vector])" +
            "given a collection vector, calculate a MinHash signature")
    public List<Long> minHashSignature(@Name("vector") List<Object> vector, @Name(value = "hashes", defaultValue = "400") Long hashes) throws NoSuchAlgorithmException {
        ArrayList<Long> result = new ArrayList<Long>();
        for (int h = 0; h < hashes; h++) {
            result.add(minHashOnce(vector, String.valueOf(h)));  // use h index as salt to re-randomize the hash function
        }
        return result;
    }

    @UserFunction
    @Description("apoc.algo.minHashOnce([vector], [salt])" +
            "given a collection vector and salt, calculate a MinHash value")
    public Long minHashOnce(@Name("vector") List<Object> vector, @Name("salt") Object salt) throws NoSuchAlgorithmException {
        String saltStr = String.valueOf(salt);
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        Long currMin = Long.MAX_VALUE;
        for (Object value : vector) {
            String input = String.valueOf(value) + saltStr;
            messageDigest.update(input.getBytes());
            byte[] md5sum = messageDigest.digest();  // implicitly resets messageDigest for next iteration
            Long hashedValue = new BigInteger(1, Arrays.copyOfRange(md5sum, 0, 8)).longValue();
            if (hashedValue < currMin) currMin = hashedValue;
        }
        return currMin;
    }
}
