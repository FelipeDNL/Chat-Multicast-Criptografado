
package udpmulticast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.json.*;

public class UDPMulticast {

    public static void main(String[] args) throws Exception{
        
        JSONObject obj = new JSONObject();
        
        //cria buffer de comunicação
        BufferedReader inBufferedReader = new BufferedReader(new InputStreamReader(System.in));
        
        try {
            // Cria um socket para se conectar ao servidor
            Socket clienteSocket = new Socket("localhost", 12345);
            
            // Cria um fluxo de entrada para receber dados do servidor
            BufferedReader entrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));

            // Cria um fluxo de saída para enviar dados para o servidor
            PrintWriter saida = new PrintWriter(clienteSocket.getOutputStream(), true);
            
            // Envia uma mensagem para o servidor
            String mensagemParaServidor = "Olá, servidor!";
            saida.println(mensagemParaServidor);

            // Lê a resposta do servidor
            String respostaDoServidor = entrada.readLine();
            System.out.println("Resposta do servidor: " + respostaDoServidor);
            
            if(respostaDoServidor.equals("Conectado")){
            
                //cria socket multicast
                InetAddress multicastGroup = InetAddress.getByName("230.100.10.1");
                MulticastSocket multiSock = new MulticastSocket(50000);
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
                    String txMsg = inBufferedReader.readLine();

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
                System.out.println("Erro ao conectar");
                // Fecha o socket do cliente
                //clienteSocket.close();
            }  
            
        } catch (IOException | NumberFormatException e){
            System.out.println(e.toString());
        }
                
    }
   
}
