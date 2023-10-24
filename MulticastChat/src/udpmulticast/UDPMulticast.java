
package udpmulticast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.json.*;

public class UDPMulticast {

    public static void main(String[] args) throws Exception{
        Scanner input = new Scanner(System.in);
        JSONObject obj = new JSONObject();
        
        //cria buffer de comunicação para chat
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        //criar chaves
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);

        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey privUsuario = pair.getPrivate();
        PublicKey pubUsuario = pair.getPublic();
        
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
            
            //dados
            int tamanhoDados = entrada.readInt();
            byte[] dadosCifrados = new byte[tamanhoDados];
            entrada.readFully(dadosCifrados, 0, dadosCifrados.length);
            
            //chave
            int tamanhoChave = entrada.readInt();
            byte[] chaveAsi = new byte[tamanhoChave];
            entrada.readFully(chaveAsi, 0, chaveAsi.length);
            
            //fechando fluxos
            entrada.close();
            saida.close();
            clienteSocket.close();
            
            //decifra chave simétrica
            Cipher decifrarChave = Cipher.getInstance("RSA");
            decifrarChave.init(Cipher.DECRYPT_MODE, privUsuario);
            byte[] bytesChaveDecifrada = decifrarChave.doFinal(chaveAsi);
            SecretKey chaveSimetricaDeci = new SecretKeySpec(bytesChaveDecifrada, "AES");
            
            Cipher decifrarDados = Cipher.getInstance("AES");
            decifrarDados.init(Cipher.DECRYPT_MODE, chaveSimetricaDeci);
            byte[] jsonDecifrada = decifrarDados.doFinal(dadosCifrados);
            JSONObject jsonRecebido = new JSONObject(new String(jsonDecifrada));
            
            if(jsonRecebido.getString("Status").equals("Conectado")){   
                //cria socket multicast
                InetAddress multicastGroup = InetAddress.getByName(jsonRecebido.getString("IP"));
                MulticastSocket multiSock = new MulticastSocket(jsonRecebido.getInt("Porta"));
                multiSock.joinGroup(multicastGroup);
                SecretKey chaveChat = stringParaSecretKey(jsonRecebido.getString("Chave"));

                //thread loop de recebimento
                Connection receive = new Connection(multiSock, chaveChat);
                receive.start();

                //avisa que o programa esta rodando
                System.out.println("Starting UDPMulticast...\n\tServer address: "+multicastGroup);
                System.out.print("Digite uma mensagem: ");

                //cria buffers de comunicação
                byte[] txData = new byte[65507];
                
                System.out.print("Digite seu nome: ");
                String nome = input.nextLine();
                
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
                    obj.put("name", nome);
                    obj.put("time", LocalDateTime.now());
                    obj.put("message", txMsg);
                    }
                    txData = obj.toString().getBytes();
                    
                    //cifrar dados json
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, chaveChat);
                    byte[] encryptedData = cipher.doFinal(txData);

                    //criar payload
                    DatagramPacket packet = new DatagramPacket(encryptedData, encryptedData.length, multicastGroup, jsonRecebido.getInt("Porta"));

                    //envia a msg
                    multiSock.send(packet);
                }
            }
        } catch (IOException | NumberFormatException e){
            System.out.println(e.toString());
        }
                
    }
    
    private static SecretKey stringParaSecretKey (String chave) {
            byte[] chaveSecretaBytes = Base64.getDecoder().decode(chave);
            SecretKey chaveAES = new SecretKeySpec(chaveSecretaBytes, 0, chaveSecretaBytes.length, "AES");

            return chaveAES;
    }
    
   
}