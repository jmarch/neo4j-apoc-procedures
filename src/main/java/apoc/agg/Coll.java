package apoc.agg;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserAggregationFunction;
import org.neo4j.procedure.UserAggregationUpdate;
import org.neo4j.procedure.UserAggregationResult;

import java.util.ArrayList;
import java.util.List;


public class Coll {

    @UserAggregationFunction
    @Description("apoc.agg.intersection(list) - aggregates lists producing the intersection")
    public IntersectionAggregator intersection() {
        return new IntersectionAggregator();
    }

    public static class IntersectionAggregator {
        private final apoc.coll.Coll coll = new apoc.coll.Coll();
        private List<Object> resultSet = null;

        @UserAggregationUpdate
        public void update(@Name("vector") List<Object> vector) {
            if (resultSet == null) {
                resultSet = vector;
            } else {
                resultSet = coll.intersection(resultSet, vector);
            }
        }

        @UserAggregationResult
        public List<Object> result() {
            return resultSet;
        }
    }

    @UserAggregationFunction
    @Description("apoc.agg.union(list) - aggregates lists producing the union")
    public UnionAggregator union() {
        return new UnionAggregator();
    }

    public static class UnionAggregator {
        private final apoc.coll.Coll coll = new apoc.coll.Coll();
        private List<Object> resultSet = new ArrayList<Object>();

        @UserAggregationUpdate
        public void update(@Name("vector") List<Object> vector) {
            resultSet = coll.union(resultSet, vector);
        }

        @UserAggregationResult
        public List<Object> result() {
            return resultSet;
        }
    }
}

