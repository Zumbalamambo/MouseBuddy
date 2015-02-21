

package cmu.zherengz.mbserver;

import java.awt.*;
import java.net.*;
import java.io.*;

/* Starts a server to listen for mouse movement deltas.
 */
public class MBServer
{
    /* Would like to integrate this with build system somehow lol */
    static final int MBSERVER_PORT = 55555;
    static final int LISTEN_PORT = 55556;
    static final float SCALE_FACTOR = 5.0f;
    static DatagramSocket listenSocket;
    static ServerSocket servSocket;
    static Robot mouseRobot;
    
    private static void awaitConnection() throws IOException
    {
        listenSocket = new DatagramSocket(LISTEN_PORT);
        listenSocket.setBroadcast(true);
        
        while (true)
        {
            System.out.println("MBServer: waiting on a phone to connect");
            byte[] receiveBuffer = new byte[1028];
            DatagramPacket requestPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            listenSocket.receive(requestPacket);
            
            System.out.println("MBServer: packet received.");
            String requestData = new String(requestPacket.getData());
            System.out.println("MBServer: packet data is: " + requestData);
            
            if (requestData.equals("MOUSEBUDDY_CONNECTION_REQUEST"))
            {
                servSocket = new ServerSocket(MBSERVER_PORT);
                System.out.println("MBServer: activating server socket");
                byte[] responseData = "MOUSEBUDDY_CONNECTION_RESPONSE".getBytes();
                DatagramPacket responsePacket =
                    new DatagramPacket(responseData, responseData.length,
                                       requestPacket.getAddress(), requestPacket.getPort());
                listenSocket.send(responsePacket);
                System.out.println("MBServer: Reponse packet sent");
                listenSocket.close();
                break;
            }
        }
    }
    
    private static void serveConnection() throws IOException
    {
        Socket phoneSocket = servSocket.accept();
        phoneSocket.setSoTimeout(10000);
        DataInputStream mouseIn = new DataInputStream(phoneSocket.getInputStream());
        
        /* Stream format: 1 boolean to determine whether phone is still connected 
         * followed by 2 floats for delta-x and delta-y*/
        while (true)
        {
            try {
                if (!mouseIn.readBoolean())
                {
                    mouseIn.close();
                    phoneSocket.close();
                    servSocket.close();
                    break;
                }
                float deltaX = mouseIn.readFloat();
                float deltaY = mouseIn.readFloat();
                Point pos = MouseInfo.getPointerInfo().getLocation();
                mouseRobot.mouseMove(pos.x + (int)(deltaX/SCALE_FACTOR), pos.y + (int)(deltaY/SCALE_FACTOR));
            }
            catch (SocketTimeoutException e)
            {
                System.out.println("Connection timed out");
                servSocket.close();
                mouseIn.close();
                phoneSocket.close();
                break;
            }
        }
    }
    
    public static void main(String[] args)
    {
        try {
            mouseRobot = new Robot();
        }
        catch (AWTException e)
        {
            System.out.println("failed to make robot");
            System.out.println(e.getMessage());
            return;
        }
        
        while (true)
        {
            /* allow us to recieve any broadcasts from phones */
            try {
                awaitConnection();
                serveConnection();
            }
            catch (IOException e)
            {
                System.err.println(e.getMessage());
                break;
            }
            
        }
    }
}