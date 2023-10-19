
package udpmulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.json.JSONException;
import org.json.JSONObject;

public class Connection extends Thread{
    public MulticastSocket aClientSock;
    public SecretKey chaveChat;
    public byte[] rxData = new byte[65507];

    public Connection(MulticastSocket aClientSock, SecretKey chaveChat) {
        this.aClientSock = aClientSock;
        this.chaveChat = chaveChat;
    }
            
    public void run(){
        try {
            while(true){
                //cria o pacote de recebimento da reposta
                DatagramPacket rxPkt = new DatagramPacket(rxData, rxData.length);

                //recebe a msg
                aClientSock.receive(rxPkt);
                
                //decifrar dados
                Cipher decifrarJSON = Cipher.getInstance("AES");
                decifrarJSON.init(Cipher.DECRYPT_MODE, chaveChat);
                byte[] decryptedData = decifrarJSON.doFinal(rxPkt.getData(), 0, rxPkt.getLength());
                
                //imprime a msg
                String rxMsg = new String(decryptedData);
                JSONObject obj = new JSONObject(rxMsg);
                System.out.println(obj.getString("name")+": "+obj.getString("message"));
            }
        } catch (IOException | JSONException | NoSuchAlgorithmException | 
                NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
