
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
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
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
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONException;
import org.json.JSONObject;

public class ServidorTCP {
    public static void main(String[] args){
        //porta em que o servidor irá ouvir as conexões
        JSONObject obj = new JSONObject();

        try {
            //criar chaves assimetricas
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            
            KeyPair par = keyGen.generateKeyPair();
            PrivateKey privServer = par.getPrivate();
            PublicKey pubServer = par.getPublic();
            
            //chave simetrica
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey chaveSimetrica = keyGenerator.generateKey();
            
            //chama método
            SecretKey chaveChat = gerarChave("sus", 50);

            //cria um servidor socket que escuta na porta especificada
            ServerSocket servidorSocket = new ServerSocket(12345);
            System.out.println("Servidor TCP esperando conexões na porta...." +servidorSocket.getLocalPort());

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
                
                //chave simétrica para chat
                String chaveChatString = Base64.getEncoder().encodeToString(chaveChat.getEncoded());
                
                //envia IP/porta e chave simétrica
                {
                    obj.put("Status", "Conectado");
                    obj.put("IP", "230.100.10.1");
                    obj.put("Porta", 50000);
                    obj.put("Chave", chaveChatString);
                }
                byte[] dadosJson = obj.toString().getBytes();
                
                //cifrando com chave simetrica
                Cipher cifraSimetrica = Cipher.getInstance("AES");
                cifraSimetrica.init(Cipher.ENCRYPT_MODE, chaveSimetrica);
                byte[] dadosJsonCifrados = cifraSimetrica.doFinal(dadosJson);
                
                saida.writeInt(dadosJsonCifrados.length);
                saida.write(dadosJsonCifrados);
                
                //cifrando chave simétrica, assimetricamente
                Cipher cifraAssimetrica = Cipher.getInstance("RSA");
                cifraAssimetrica.init(Cipher.ENCRYPT_MODE, keyUsuario);
                byte[] chaveSimetricaCifrada = cifraAssimetrica.doFinal(chaveSimetrica.getEncoded());
                
                saida.writeInt(chaveSimetricaCifrada.length);
                saida.write(chaveSimetricaCifrada);

                //fecha fluxos do cliente
                entrada.close();
                saida.close();
            }
        } catch (IOException e) {
        } catch (NoSuchAlgorithmException | JSONException | 
                NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException  ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static PublicKey stringParaPublicKey(String chave) {
        PublicKey chavePublicaRecebida = null;
        
        try {
            
            byte[] chavePublicaBytes = Base64.getDecoder().decode(chave);
            
            //usado para representar uma chave pública em um formato codificado em X.509 (certificado)
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(chavePublicaBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            
            chavePublicaRecebida = keyFactory.generatePublic(keySpec);
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return chavePublicaRecebida;
    }
    
    private static SecretKey gerarChave (String password, int iteracoes){
        SecretKey chaveAES = null; 
        
        try {
            
            byte[] iv = new byte[16];
            SecureRandom secureRandom = new SecureRandom(); //RNG
            secureRandom.nextBytes(iv);
            
            //password based encryption
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), iv, iteracoes, 256);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            
            SecretKey chaveSimetrica = keyFactory.generateSecret(keySpec);
            chaveAES = new SecretKeySpec(chaveSimetrica.getEncoded(), "AES");
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(ServidorTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return chaveAES;
    }
    
}
