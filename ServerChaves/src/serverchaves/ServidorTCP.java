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
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.json.JSONException;
import org.json.JSONObject;

public class ServidorTCP {
    public static void main(String[] args){
        //porta em que o servidor irá ouvir as conexões
        int porta = 12345;
        JSONObject obj = new JSONObject();

        try {
            //criar chaves
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(1024, random);
            
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privServer = pair.getPrivate();
            PublicKey pubServer = pair.getPublic();
            
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
                PublicKey keyUsuario = stringParaPublicKey(entrada.readUTF());
                System.out.println("Chave Server: "+pubServer);
                System.out.println("Chave usuario: "+keyUsuario);
                
                String conectado = "Conectado";
                saida.write(cifrarString(conectado, keyUsuario));

                
                //envia IP/porta e chave simétrica
                {
                    obj.put("IP", "230.100.10.1");
                    obj.put("Porta", 50000);
                    obj.put("Chave", 1);
                }
                saida.writeUTF(obj.toString());

                //fecha o socket do cliente
                //clienteSocket.close();
            }
        } catch (IOException e) {
        } catch (NoSuchAlgorithmException | JSONException ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static PublicKey stringParaPublicKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException{
        byte[] publicBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        PublicKey chavePublicaRecebida = keyFactory.generatePublic(keySpec);   
        
        return chavePublicaRecebida;
    }
    
    private static byte[] cifrarString(String texto, PublicKey key) throws 
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
        
        //Creating a Cipher object
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        //Initializing a Cipher object
        cipher.init(Cipher.ENCRYPT_MODE, key);

        //responde ao cliente
        byte[] respostaAoCliente = texto.getBytes();
        cipher.update(respostaAoCliente);

        //Encrypting the data
        byte[] cipherText = cipher.doFinal();
        
        return cipherText;
        
    }
    
}
