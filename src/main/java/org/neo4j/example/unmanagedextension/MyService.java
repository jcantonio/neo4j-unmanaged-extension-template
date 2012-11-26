package org.neo4j.example.unmanagedextension;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphalgo.*;
import org.neo4j.kernel.Traversal;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.server.rest.repr.PathRepresentation;
import org.neo4j.server.rest.repr.WeightedPathRepresentation;
import javax.ws.rs.Produces;

@Path("/service")
public class MyService {

    @GET
    @Path("/helloworld")
    public String helloWorld() {
        return "Hello World!";
    }

    @GET
    @Path("/warmup")
    public String warmUp(@Context GraphDatabaseService db) {
		Node start;
		for ( Node n : GlobalGraphOperations.at( db ).getAllNodes() ) {
		   n.getPropertyKeys();
		   for ( Relationship relationship : n.getRelationships() ) {
              start = relationship.getStartNode();
           }
        }
        for ( Relationship r : GlobalGraphOperations.at( db ).getAllRelationships() ) {
          start = r.getStartNode();
          r.getPropertyKeys();
        }
    return "Warmed up and ready to go!";
    }

    @GET
    @Path("/astar/{from}/{to}")
    @Produces("application/json")
    public Response routeAStar(@PathParam("from") Long from, @PathParam("to") Long to, @Context GraphDatabaseService db) throws IOException {
       Node nodeA = db.getNodeById(from);
       Node nodeB = db.getNodeById(to);

  	   EstimateEvaluator<Double> estimateEvaluator = new EstimateEvaluator<Double>()
	        {
	            public Double getCost( final Node node, final Node goal )
	            {
	                double dx = (Double) node.getProperty( "x" ) - (Double) goal.getProperty( "x" );
	                double dy = (Double) node.getProperty( "y" ) - (Double) goal.getProperty( "y" );
	                double result = Math.sqrt( Math.pow( dx, 2 ) + Math.pow( dy, 2 ) );
	                return result;
	            }
	        };
	        PathFinder<WeightedPath> astar = GraphAlgoFactory.aStar(
	                Traversal.expanderForAllTypes(),
	                CommonEvaluators.doubleCostEvaluator( "time" ), estimateEvaluator );
	        WeightedPath path = astar.findSinglePath( nodeA, nodeB );

		    return Response.ok().entity( path.toString() ).build();
    }
	
	
    @GET
    @Path("/astar2/{from}/{to}")
    @Produces("application/json")
    public Response routeAStar2(@PathParam("from") Long from, @PathParam("to") Long to, @Context GraphDatabaseService db) throws IOException {
       Node nodeA = db.getNodeById(from);
       Node nodeB = db.getNodeById(to);

  	   EstimateEvaluator<Double> estimateEvaluator = new EstimateEvaluator<Double>()
	        {
	            public Double getCost( final Node node, final Node goal )
	            {
	                double dx = (Double) node.getProperty( "x" ) - (Double) goal.getProperty( "x" );
	                double dy = (Double) node.getProperty( "y" ) - (Double) goal.getProperty( "y" );
	                double result = Math.sqrt( Math.pow( dx, 2 ) + Math.pow( dy, 2 ) );
	                return result;
	            }
	        };
	        PathFinder<WeightedPath> astar = GraphAlgoFactory.aStar(
	                Traversal.expanderForAllTypes(),
	                CommonEvaluators.doubleCostEvaluator( "time" ), estimateEvaluator );
	        WeightedPath path = astar.findSinglePath( nodeA, nodeB );
	
			Map<String, Object> astarMap = new HashMap<String, Object>();
			astarMap.put("time", path.weight());
			
			List<Object> nodes = new ArrayList<Object>();
			for ( Node node : path.nodes() )
			    {
			      nodes.add(node.getId());
	            }
			astarMap.put("nodes", nodes);
			
			List<Object> relationships = new ArrayList<Object>();
			for ( Relationship relationship : path.relationships() )
			    {
            		 Map<String, Object> relMap = new HashMap<String, Object>();
                     relMap.put("id", relationship.getId());
                     relMap.put("rel_type", relationship.getType().name());
                     relMap.put("start_node", relationship.getStartNode().getId());
                     relMap.put("end_node", relationship.getEndNode().getId());
                     relMap.put("time", relationship.getProperty("time"));
			         relationships.add(relMap);
	            }

			astarMap.put("relationships", relationships);
            
            //PathRepresentationCreator representationCreator = WEIGHTED_PATH_REPRESENTATION_CREATOR;

			ObjectMapper objectMapper = new ObjectMapper();
	        //return Response.ok().entity(objectMapper.writeValueAsString(path.toString())).build();
	        //return Response.ok().entity(WeightedPathRepresentation(path)).build();
            //return Response.ok().entity(new WeightedPathRepresentation( path )).build();
            //return Response.ok().entity(new PathRepresentation( path ).toString()).build();
            return Response.ok().entity(objectMapper.writeValueAsString(astarMap)).build();
            //return Response.ok().entity(objectMapper.writeValueAsString(new WeightedPathRepresentation( path ))).build();
}

    @GET
    @Path("/friends/{name}")
    public Response getFriends(@PathParam("name") String name, @Context GraphDatabaseService db) throws IOException {
        ExecutionEngine executionEngine = new ExecutionEngine(db);
        ExecutionResult result = executionEngine.execute("START person=node:people(name={n}) MATCH person-[:KNOWS]-other RETURN other.name",
                Collections.<String, Object>singletonMap("n", name));
        List<String> friends = new ArrayList<String>();
        for (Map<String, Object> item : result) {
            friends.add((String) item.get("other.name"));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return Response.ok().entity(objectMapper.writeValueAsString(friends)).build();
    }
}
