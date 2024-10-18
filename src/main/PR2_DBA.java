package main;

import java.io.IOException;
import mapa.Mapa;

/**
 *
 * @author juanmi
 */
public class PR2_DBA {

    public static void main(String[] args) throws IOException {
        
        String paquete_mapas= "ejemplos_mapas/";
        
        Mapa mapa = new Mapa(paquete_mapas+"mapWithComplexObstacle1.txt");

        mapa.imprimirMapa();
        
        System.out.println(mapa.getCasilla(3, 4));
    }
}
