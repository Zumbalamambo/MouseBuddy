

package cmu.zherengz.mbserver;

import java.awt.*;
import java.net.*;
import java.io.*;

import java.awt.event.InputEvent;

/* Starts a server to listen for mouse movement deltas.
 */
public class MBServer
{
    /* Would like to integrate this with build system somehow lol */
    static final int MBSERVER_PORT = 8888;
    static final int LISTEN_PORT = 8887;
    static final float SCALE_FACTOR = 15.0f;
    
    static float accumX;
    static float accumY;
    
    static DatagramSocket listenSocket;
    static ServerSocket servSocket;
    static Robot mouseRobot;
    
    private static void awaitConnection() throws IOException
    {
        listenSocket = new DatagramSocket(LISTEN_PORT);
        listenSocket.setBroadcast(true);
        System.out.println("MBServer: Listen socket opened at " + listenSocket.getLocalAddress().getCanonicalHostName());
        
        while (true)
        {
            System.out.println("MBServer: waiting on a phone to connect");
            byte[] receiveBuffer = new byte[512];
            DatagramPacket requestPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            listenSocket.receive(requestPacket);
            
            System.out.println("MBServer: packet received.");
            String requestData = new String(requestPacket.getData()).trim();
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
        servSocket.setSoTimeout(30000);
        try {
            mouseRobot = new Robot();
        }
        catch (Exception e)
        {
            System.err.println("Failed to make robot");
            servSocket.close();
            return;
        }
        
        Socket phoneSocket = null;
        
        for (int i = 0; i < 10; i++)
        {
            try {
                phoneSocket = servSocket.accept();
            }
            catch (SocketTimeoutException e)
            {
				System.out.println("MBServer: Socket Timeout Exception");
				e.printStackTrace();
                continue;
            }
            break;
        }
        
        if (phoneSocket == null)
        {
            System.out.println("MBServer: Connection attempt timed out");
            return;
        }
        
        System.out.println("MBServer: Connected to mouse socket");
        phoneSocket.setSoTimeout(5000);
        DataInputStream mouseIn = new DataInputStream(phoneSocket.getInputStream());
        DataOutputStream mouseOut = new DataOutputStream(phoneSocket.getOutputStream());
        
        System.out.println("MBServer: Starting transfer");
        mouseOut.writeBoolean(true);
        
        /* Stream format: 1 boolean to determine whether phone is still connected 
         * followed by 2 floats for delta-x and delta-y*/
        while (true)
        {
            try {
                if (!mouseIn.readBoolean())
                {
                    System.out.println("MBServer: Ending connection");
                    mouseIn.close();
                    mouseOut.close();
                    phoneSocket.close();
                    servSocket.close();
                    break;
                }
                float deltaX = mouseIn.readFloat();
                float deltaY = mouseIn.readFloat();
                accumX += deltaX;
                accumY += deltaY;
                
                
                int deltaPixelX = 0;
                int deltaPixelY = 0;
                
                if (Math.abs(accumX) > 0.5)
                {
                    deltaPixelX = (int) (accumX+0.5);
                    accumX -= deltaPixelX;
                    System.out.println("MBServer: X: " + deltaX);
                }
                if (Math.abs(accumY) > 0.5)
                {
                    deltaPixelY = (int) (accumY+0.5);
                    accumY -= deltaPixelY;
                    System.out.println("MBServer: Y: " + deltaY);
                }
                Point pos = MouseInfo.getPointerInfo().getLocation();
                mouseRobot.mouseMove(pos.x + deltaPixelX, pos.y + deltaPixelY);
                
                if (mouseIn.readBoolean())
                {
                    mouseRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                }
                else
                {
                    mouseRobot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
                }
                if (mouseIn.readBoolean())
                {
                    mouseRobot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
                }
                else
                {
                    mouseRobot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
                }
                
            }
            catch (SocketTimeoutException e)
            {
                mouseOut.writeBoolean(false);
                System.out.println("Connection timed out");
                servSocket.close();
                mouseIn.close();
                mouseOut.close();
                phoneSocket.close();
                break;
            }
            mouseOut.writeBoolean(true);
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