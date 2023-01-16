package jadeproject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.lang.reflect.Type;
import java.util.*;

public class TravellingAgent extends Agent {
    private Graph<String, DefaultWeightedEdge> worldMap;// graph representing the world map
    private DijkstraShortestPath<String, DefaultWeightedEdge> dijkstra;// shortest path algorithm
    private Map<String, Double> shortestDistances;// map containing the shortest distance from current location to every other vertex
    private String currentLocation;// current location of the agent


    protected void setup() {
        // register agent with DF service to advertise its capabilities
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("travelling-agent");
        sd.setName("traveller");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Initialize worldMap and shortest path algorithm
        worldMap = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        dijkstra = new DijkstraShortestPath<>(worldMap);

        // add vertices and edges to worldMap
        worldMap.addVertex("0");
        worldMap.addVertex("1");
        worldMap.addVertex("2");
        worldMap.addVertex("3");
        worldMap.addVertex("4");
        worldMap.addVertex("5");
        worldMap.setEdgeWeight(worldMap.addEdge("0", "2"), 1);
        worldMap.setEdgeWeight(worldMap.addEdge("0", "3"), 3);
        worldMap.setEdgeWeight(worldMap.addEdge("1", "3"), 5);
        worldMap.setEdgeWeight(worldMap.addEdge("2", "3"), 2);
        worldMap.setEdgeWeight(worldMap.addEdge("2", "4"), 1);
        worldMap.setEdgeWeight(worldMap.addEdge("2", "5"), 4);
        worldMap.setEdgeWeight(worldMap.addEdge("3", "4"), 7);
        worldMap.setEdgeWeight(worldMap.addEdge("4", "5"), 2);


        // Determine current location of the agent
        List<String> vertices = new ArrayList<>(worldMap.vertexSet());
        Random rand = new Random();
        Set<String> vertexSet = worldMap.vertexSet();
        currentLocation = vertexSet.stream().skip(rand.nextInt(vertexSet.size())).findFirst().get();
        System.out.println(getAID().getLocalName() + ": My location is: " + currentLocation);


        // Find shortest distance from current location to every other vertex
        shortestDistances = new HashMap<>();
        for (String vertex : worldMap.vertexSet()) {
            GraphPath<String, DefaultWeightedEdge> path = dijkstra.getPath(currentLocation, vertex);
            if (path != null) {
                shortestDistances.put(vertex, path.getWeight());
            }
        }

        // Send ACL message containing shortest distance information to other agents
        Gson gson = new Gson();
        String json = gson.toJson(shortestDistances);
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.setContent(json);

        DFAgentDescription template = new DFAgentDescription();
        sd = new ServiceDescription();
        sd.setType("organizer-agent");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            for (DFAgentDescription agent : result) {
                message.addReceiver(agent.getName());
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        send(message);
    }
}
