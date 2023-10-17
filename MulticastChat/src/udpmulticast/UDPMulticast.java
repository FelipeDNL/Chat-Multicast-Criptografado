
package udpmulticast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.json.*;

public class UDPMulticast {

    public static void main(String[] args) throws Exception{
        
        JSONObject obj = new JSONObject();
        JSONObject objServidor = new JSONObject();
        
        //cria buffer de comunicação para chat
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        //criar chaves
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGen.initialize(1024, random);

        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey privUsuario = pair.getPrivate();
        PublicKey pubUsuario = pair.getPublic();
        
        System.out.println("Chave usuario: "+pubUsuario);
        
        try {
            //cria um socket para se conectar ao servidor
            Socket clienteSocket = new Socket("localhost", 12345);
            
            //cria um fluxo de entrada para receber dados do servidor
            DataInputStream entrada = new DataInputStream(clienteSocket.getInputStream());

            //cria um fluxo de saída para enviar dados para o servidor
            DataOutputStream saida = new DataOutputStream(clienteSocket.getOutputStream());
            
            //enviando chave publica do usuario em formato txt
            String pubUsu = Base64.getEncoder().encodeToString(pubUsuario.getEncoded());
            saida.writeUTF(pubUsu);
            
            //recebendo chave publica do servidor
            String chaveRecebida = entrada.readUTF();
            PublicKey keyServer = stringParaPublicKey(chaveRecebida);
            
            int length = entrada.readInt();
            byte[] dataCifrada = new byte[length];
            entrada.readFully(dataCifrada, 0, dataCifrada.length);
            
            int length1 = entrada.readInt();
            byte[] chaveAsi = new byte[length1];
            entrada.readFully(chaveAsi, 0, chaveAsi.length);
            
            
            Cipher asymmetricCipher = Cipher.getInstance("RSA");
            asymmetricCipher.init(Cipher.DECRYPT_MODE, privUsuario);
            byte[] chaveDecifrada = asymmetricCipher.doFinal(chaveAsi);
            
            byte[] keyBytes = chaveDecifrada;
            SecretKey chaveSimetrica = new SecretKeySpec(keyBytes, "AES");
            
            Cipher dadosDecifrados = Cipher.getInstance("AES");
            dadosDecifrados.init(Cipher.DECRYPT_MODE, chaveSimetrica);
            byte[] jsonDecifrada = dadosDecifrados.doFinal(dataCifrada);
            
            JSONObject resposta = new JSONObject(new String(jsonDecifrada));
            //
            
            if(resposta.getString("Status").equals("Conectado")){   
                //cria socket multicast
                InetAddress multicastGroup = InetAddress.getByName(resposta.getString("IP"));
                MulticastSocket multiSock = new MulticastSocket(resposta.getInt("Porta"));
                multiSock.joinGroup(multicastGroup);

                //thread loop de recebimento
                Connection receive = new Connection(multiSock);
                receive.start();

                //avisa que o programa esta rodando
                System.out.println("Starting UDPMulticast...\n\tServer address: "+multicastGroup);
                System.out.print("Digite uma mensagem: ");

                //cria buffers de comunicação
                byte[] txData = new byte[65507];
                
                String chave = resposta.getString("Chave");
                SecretKey chaveChat = stringParaPublicKey(chave);

                //cria looping de comunicaão
                while(true){
                    //pedir ao usuário para digitar uma msg
                    String txMsg = in.readLine();

                    if(txMsg.equals("<close>")){
                        multiSock.leaveGroup(multicastGroup);
                        multiSock.close();
                    }

                    //cria JSON
                    {
                    obj.put("date", LocalDate.now());
                    obj.put("name", "Lopes");
                    obj.put("time", LocalDateTime.now());
                    obj.put("message", txMsg);
                    }
                    txData = obj.toString().getBytes();

                    //cria o pacote de envio
                    DatagramPacket txPkt = new DatagramPacket(txData, txData.length, multicastGroup, resposta.getInt("Porta"));
                    
                    //envia a msg
                    multiSock.send(txPkt);
                }
            }
        } catch (IOException | NumberFormatException e){
            System.out.println(e.toString());
        }
                
    }
    
    private static PublicKey stringParaKey (String key) throws NoSuchAlgorithmException, InvalidKeySpecException{
        byte[] chavePublicaBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec chaveSpec = new X509EncodedKeySpec(chavePublicaBytes);
        
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey chavePublicaRecebida = keyFactory.generatePublic(chaveSpec);   
        
        return chavePublicaRecebida;
    }
    
    private static SecretKey stringParaSecretKey (String key) throws NoSuchAlgorithmException, InvalidKeySpecException{
        byte[] chavePublicaBytes = Base64.getDecoder().decode(key);
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey secretKey = keyFactory.generateSecret(keySpec);
        return new SecretKeySpec(secretKey.getEncoded(), "AES");
        
        return chavePublicaRecebida;
    }
    
   
}