
package udpmulticast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
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
import org.json.*;

public class UDPMulticast {

    public static void main(String[] args) throws Exception{
        
        JSONObject obj = new JSONObject();
        String multicastIP, porta;
        
        //String chave = "fZMyRo5fUEAz5mUCrfnpLKoIlz5TSTF7";
        //https://www.javacodegeeks.com/2018/03/aes-encryption-and-decryption-in-javacbc-mode.html
        
        //cria buffer de comunicação para chat
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        //criar chaves
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(1024, random);

        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey privUsuario = pair.getPrivate();
        PublicKey pubUsuario = pair.getPublic();
        
        System.out.println("Chave usuario: "+pubUsuario);
        
        try {
            //cria um socket para se conectar ao servidor
            Socket clienteSocket = new Socket("localhost", 12345);
            
            //cria um fluxo de entrada para receber dados do servidor
            BufferedReader entrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));

            //cria um fluxo de saída para enviar dados para o servidor
            PrintWriter saida = new PrintWriter(clienteSocket.getOutputStream(), true);
            
            //enviando chave publica do usuario em formato txt
            String pubUsuarioTxt = Base64.getEncoder().encodeToString(pubUsuario.getEncoded());
            saida.println(pubUsuarioTxt);
            
            //recebendo chave publica do servidor
            PublicKey keyServer = pair.getPublic();
            keyServer = stringParaPublicKey(entrada.readLine());

            //lê a resposta do servidor
            String respostaDoServidor = entrada.readLine();
            System.out.println("Resposta do servidor: " + respostaDoServidor);
            
            if(respostaDoServidor.equals("Conectado")){
                multicastIP = entrada.readLine();
                System.out.println("IP recebido: "+multicastIP);
                
                porta = entrada.readLine();
                System.out.println("Porta recebida: "+porta);
                
                //cria socket multicast
                InetAddress multicastGroup = InetAddress.getByName(multicastIP);
                MulticastSocket multiSock = new MulticastSocket(Integer.parseInt(porta));
                multiSock.joinGroup(multicastGroup);

                //thread loop de recebimento
                Connection receive = new Connection(multiSock);
                receive.start();

                //avisa que o programa esta rodando
                System.out.println("Starting UDPMulticast...\n\tServer address: "+multicastGroup);

                //cria buffers de comunicação
                byte[] txData = new byte[65507];

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
                    DatagramPacket txPkt = new DatagramPacket(txData, txData.length, multicastGroup, 50000);

                    //envia a msg
                    multiSock.send(txPkt);
                }
            } else {
                System.err.println("Erro ao conectar");
                //Fecha o socket do cliente
                //clienteSocket.close();
            }  
            
        } catch (IOException | NumberFormatException e){
            System.out.println(e.toString());
        }
                
    }
    
    private static PublicKey stringParaPublicKey (String key) throws NoSuchAlgorithmException, InvalidKeySpecException{
        byte[] chavePublicaBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec chaveSpec = new X509EncodedKeySpec(chavePublicaBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        PublicKey chavePublicaRecebida = keyFactory.generatePublic(chaveSpec);   
        
        return chavePublicaRecebida;
    }
   
}