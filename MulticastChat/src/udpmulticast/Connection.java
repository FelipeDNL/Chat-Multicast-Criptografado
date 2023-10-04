
package udpmulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class Connection extends Thread{
    public MulticastSocket aClientSock;
    public byte[] rxData = new byte[65507];

    public Connection(MulticastSocket aClientSock) {
        this.aClientSock = aClientSock;
    }
            
    public void run(){
        try {
            while(true){
            //cria o pacote de recebimento da reposta
            DatagramPacket rxPkt = new DatagramPacket(rxData, rxData.length);
            
            //recebe a msg
            aClientSock.receive(rxPkt);
            
            //imprime a msg
            String rxMsg = new String(rxPkt.getData());
            JSONObject obj = new JSONObject(rxMsg);
            System.out.println(obj.getString("name")+": "+obj.getString("message"));
            }
        } catch (IOException | JSONException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
