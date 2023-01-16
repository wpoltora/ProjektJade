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
import java.lang.reflect.Type;
import java.util.*;



public class OrganizerAgent extends Agent {
    private int expectedNumberOfAgents;
    private Map<String, Map<String, Double>> allAgentDistances;   // all distances from all agents
    private String optimalMeetingPlace;

    protected void setup() {
        allAgentDistances = new HashMap<>();

        // register agent with DF service to advertise its capabilities
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("organizer-agent");
        sd.setName("organizer");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // search for other agents of type travelling-agent
        DFAgentDescription template = new DFAgentDescription();
        sd = new ServiceDescription();
        sd.setType("travelling-agent");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            expectedNumberOfAgents = result.length;
        } catch (FIPAException e) {
            e.printStackTrace();
        }


        // Receive ACL messages containing shortest distance information from other agents
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, Double>>(){}.getType();
                    Map<String, Double> agentDistances = gson.fromJson(msg.getContent(), type);
                    String agentName = msg.getSender().getLocalName();
                    allAgentDistances.put(agentName, agentDistances);

                    // if enough agents have sent their distance information find the optimal meeting place
                    if (allAgentDistances.size() == expectedNumberOfAgents) {
                        optimalMeetingPlace = findOptimalMeetingPlace(allAgentDistances);
                        System.out.println(getAID().getLocalName() +": meeting place: " + optimalMeetingPlace);
                    }
                }
                else{
                    block();
                }
            }
        });
    }


    // find optimal meeting place by summing up all distance to each vertex and finding the minimum
    private String findOptimalMeetingPlace(Map<String, Map<String, Double>> allAgentDistances) {
        Map<String, Double> totalDistances = new HashMap<>();
        for (Map<String, Double> agentDistances : allAgentDistances.values()) {
            for (Map.Entry<String, Double> entry : agentDistances.entrySet()) {
                String vertex = entry.getKey();
                double distance = entry.getValue();
                totalDistances.merge(vertex, distance, Double::sum);
            }
        }
        System.out.println("sum of distances to each vertex: "+totalDistances);
        return totalDistances.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}

