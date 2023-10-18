
package udpmulticast;

import java.io.DataInputStream;
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
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            
            keyGenerator.init(256);
            SecretKey symmetricKey = keyGenerator.generateKey();
            SecretKey chaveChat = this.chaveChat;
                
            Cipher decryptionCipher = Cipher.getInstance("AES");
            decryptionCipher.init(Cipher.DECRYPT_MODE, chaveChat);    
            
            //cria o pacote de recebimento da reposta
            DatagramPacket rxPkt = new DatagramPacket(rxData, rxData.length);
            
            //recebe a msg
            aClientSock.receive(rxPkt);
            
            DatagramPacket receivedPacket = rxPkt;
            byte[] receivedData = receivedPacket.getData();
            byte[] decryptedData = decryptionCipher.doFinal(receivedData);
            
            DatagramPacket decryptedPacket = new DatagramPacket(decryptedData, decryptedData.length, receivedPacket.getAddress(), receivedPacket.getPort());

            
            //imprime a msg
            String rxMsg = new String(decryptedPacket.getData());
            JSONObject obj = new JSONObject(rxMsg);
            System.out.println(obj.getString("name")+": "+obj.getString("message"));
            }
        } catch (IOException | JSONException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
