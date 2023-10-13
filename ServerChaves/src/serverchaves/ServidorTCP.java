/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package serverchaves;

/**
 *
 * @author Felipe
 */
import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.json.JSONException;
import org.json.JSONObject;

public class ServidorTCP {
    public static void main(String[] args){
        //porta em que o servidor irá ouvir as conexões
        int porta = 12345;
        JSONObject obj = new JSONObject();

        try {
            //criar chaves assimetricas
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privServer = pair.getPrivate();
            PublicKey pubServer = pair.getPublic();
            
            //chave simetrica
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey symmetricKey = keyGenerator.generateKey();
            
            System.out.println("Chave servidor: "+pubServer);

            //cria um servidor socket que escuta na porta especificada
            ServerSocket servidorSocket = new ServerSocket(porta);
            System.out.println("Servidor TCP esperando conexões na porta " + porta);

            while (true) {
                //aguarda por uma conexão de cliente
                Socket clienteSocket = servidorSocket.accept();
                System.out.println("Cliente conectado: " + clienteSocket.getInetAddress().getHostAddress());

                //cria um fluxo de entrada para receber dados do cliente
                DataInputStream entrada = new DataInputStream(clienteSocket.getInputStream());

                //cria um fluxo de saída para enviar dados para o cliente
                DataOutputStream saida = new DataOutputStream(clienteSocket.getOutputStream());
                
                //enviando chave publica em formato txt
                String pubkey = Base64.getEncoder().encodeToString(pubServer.getEncoded());
                saida.writeUTF(pubkey);
                
                //recebendo chave publica do usuario
                String chaveRecebida = entrada.readUTF();
                PublicKey keyUsuario = stringParaPublicKey(chaveRecebida);
                
                //envia IP/porta e chave simétrica
                {
                    obj.put("Status", "Conectado");
                    obj.put("IP", "230.100.10.1");
                    obj.put("Porta", 50000);
                    obj.put("Chave", 1);
                }
                
                byte[] jsonData = obj.toString().getBytes();
                
                Cipher symmetricCipher = Cipher.getInstance("AES");
                symmetricCipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
                byte[] encryptedJsonData = symmetricCipher.doFinal(jsonData);
                
                saida.writeInt(encryptedJsonData.length);
                saida.write(encryptedJsonData);
                
                Cipher asymmetricCipher = Cipher.getInstance("RSA");
                asymmetricCipher.init(Cipher.ENCRYPT_MODE, keyUsuario);
                byte[] encryptedSymmetricKey = asymmetricCipher.doFinal(symmetricKey.getEncoded());
                
                saida.writeInt(encryptedSymmetricKey.length);
                saida.write(encryptedSymmetricKey);
                
                
                
                saida.writeUTF(obj.toString());

                //fecha o socket do cliente
                //clienteSocket.close();
            }
        } catch (IOException e) {
        } catch (NoSuchAlgorithmException | JSONException | InvalidKeySpecException | 
                NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException  ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static PublicKey stringParaPublicKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException{
        byte[] publicBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey chavePublicaRecebida = keyFactory.generatePublic(keySpec);   
        
        return chavePublicaRecebida;
    }
    
}
