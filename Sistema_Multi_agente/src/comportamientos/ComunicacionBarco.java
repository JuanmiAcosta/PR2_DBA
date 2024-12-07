package comportamientos;

import agentes.AgenteBarco;
import estados.EstadosBarco;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.Behaviour;
import utiles.GestorComunicacion;
import utiles.GestorDF;

public class ComunicacionBarco extends Behaviour {

    private final String CONV_BARCO_SKAL_ID = "barco-skal-conv";
    private final String CONV_BARCO_JARL_ID = "barco-jarl-conv";
    private String CONV_BARCO_VIDENTE_ID;

    private EstadosBarco paso;
    private AgenteBarco agente;
    private Boolean finish = false;
    private AID jarl;
    private AID skal;
    private AID vidente;

    public ComunicacionBarco(AgenteBarco agent) {
        super(agent);
        this.agente = agent;
        this.paso = EstadosBarco.INICIO_MISION;
        this.prepararParaComunicaciones();
    }

    private void prepararParaComunicaciones() {

        boolean todosLosAgentesRegistrados = false;
        AID[] agentes = null;

        while (!todosLosAgentesRegistrados) {

            // Buscar los agentes del DF
            agentes = GestorDF.buscarAgentes(this.agente, "aldeano");

            if (agentes.length == 3) { // Número esperado de servicios
                todosLosAgentesRegistrados = true;
            } else {
                try {
                    Thread.sleep(100); // Esperar 1 segundo antes de volver a buscar
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        this.jarl = GestorDF.buscarAgenteEnLista(agentes, "jarl");
        this.skal = GestorDF.buscarAgenteEnLista(agentes, "skal");
        this.vidente = GestorDF.buscarAgenteEnLista(agentes, "vidente");
    }

    @Override
    public void action() {

        ACLMessage msgSkal;
        ACLMessage msgJarl;
        ACLMessage msgVidente;

        if (agente.getPosObj() == null) {

            switch (this.paso) {
                case INICIO_MISION:

                    msgSkal = new ACLMessage(ACLMessage.REQUEST);
                    msgSkal.addReceiver(this.skal);
                    msgSkal.setReplyWith("presentation-request");
                    msgSkal.setContent("Bro, estoy listo para encontrar a la tripulación perdida. En plan.");
                    msgSkal.setConversationId(CONV_BARCO_SKAL_ID);
                    myAgent.send(msgSkal);

                    //System.out.println("[Master] Enviado REQUEST a todos los esclavos.");
                    this.agente.getGraficos().agregarTraza(msgSkal.toString());
                    this.paso = EstadosBarco.ESPERANDO_TRADUCCION_INICIO;

                    break;

                case ESPERANDO_TRADUCCION_INICIO:

                    msgSkal = agente.blockingReceive();

                    if (msgSkal != null && msgSkal.getPerformative() == ACLMessage.INFORM) {

                        if (msgSkal.getSender().equals(skal) && GestorComunicacion.checkMensajeJarl(msgSkal.getContent())) {

                            // Traduccion del mensaje
                            String mensajeAMandar = msgSkal.getContent();

                            // Enviar PROPOSAL a Skal
                            msgJarl = new ACLMessage(ACLMessage.PROPOSE);
                            msgJarl.addReceiver(this.jarl);
                            msgJarl.setReplyWith("validation-request");
                            msgJarl.setContent(mensajeAMandar);
                            msgJarl.setConversationId(CONV_BARCO_JARL_ID);
                            myAgent.send(msgJarl);
                            agente.getGraficos().agregarTraza(msgJarl.toString());

                        } else {
                            System.out.println("No entiendo lo que me quieres decir");
                        }
                    } else {
                        System.out.println("Error esperando INFORM en: " + agente.getLocalName());
                    }

                case ESPERANDO_TOTEM_JARL:

                    msgJarl = agente.blockingReceive();

                    if (msgJarl != null && msgJarl.getPerformative() == ACLMessage.CONFIRM) {

                        if (msgJarl.getSender().equals(jarl) && GestorComunicacion.checkMensajeBarco(msgJarl.getContent())) {

                            // Jarl me da el token
                            CONV_BARCO_VIDENTE_ID = msgJarl.getContent();
                            
                            // Le mando mensaje al vidente
                            
                        }else{
                            
                            System.out.println("No esperaba ese mensaje en este momento");
                            
                        }

                        // Enviar REQUEST a vidente
                    } else if (msgJarl != null && msgJarl.getPerformative() == ACLMessage.DISCONFIRM) {
                        
                        if (msgJarl.getSender().equals(jarl) && GestorComunicacion.checkMensajeBarco(msgJarl.getContent())) {

                               // Se termina el programa
                               System.exit(0);
                            
                        }else{
                            
                            System.out.println("No esperaba ese mensaje en este momento");
                            
                        }

                    } else {
                        System.out.println("Error esperando CONFIRM / DISCONFIRM en: " + agente.getLocalName());
                    }
                    
                    break;

                default:
                    System.out.println("[Barco] Error: Estado desconocido.");
                    myAgent.doDelete();
                    break;
            }
        }

    }

    @Override
    public boolean done() {
        return this.finish;
    }
}
