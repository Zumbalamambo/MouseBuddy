package cmu.ruixin.mousebuddy;

import java.net.*;
import java.io.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by JJ on 2/21/2015.
 */
public class ConnectServerAsyncTask extends AsyncTask<Void, Void, InetAddress>
{
    static final int CONNECT_PORT = 8887;

    DatagramSocket connectSocket;
    MouseActivity activity;
    byte[] connectData;
    String connectIP;

    public ConnectServerAsyncTask(MouseActivity ma, String connectIP) throws SocketException
    {
        connectSocket = new DatagramSocket();
        connectSocket.setBroadcast(true);
        connectSocket.setSoTimeout(5000);

        connectData = "MOUSEBUDDY_CONNECTION_REQUEST".getBytes();
        activity = ma;
        this.connectIP = "128.237.174.129";
    }

    @Override
    protected InetAddress doInBackground(Void... a)
    {
        final int ATTEMPTS = 20;
        try {
            for (int i = 0; i < ATTEMPTS; i++) {
                /*
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    Log.d("MBServerConnection", "found interface");
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }

                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcastAddr = interfaceAddress.getBroadcast();
                        if (broadcastAddr == null) {
                            Log.d("MBServerConnection", "no broadcast address found");
                            continue;
                        }

                        Log.d("MBServerConnection", "broadcast address found, sending");
                        Log.d("MBServerConnection", broadcastAddr.getCanonicalHostName());
                        DatagramPacket connectPacket = new DatagramPacket(connectData, connectData.length,
                                broadcastAddr, CONNECT_PORT);
                        connectSocket.send(connectPacket);

                    }

                }
                */

                DatagramPacket connectPacket = new DatagramPacket(connectData, connectData.length,
                        InetAddress.getByName(connectIP), CONNECT_PORT);
                connectSocket.send(connectPacket);

                DatagramPacket responsePacket;

                try {
                    byte[] responseBuf = new byte[512];
                    responsePacket = new DatagramPacket(responseBuf, responseBuf.length);
                    connectSocket.receive(responsePacket);
                } catch (SocketTimeoutException e) {
                    Log.d("MBServerConnection", "failed, " + (ATTEMPTS - i) + " attempts remaining");
                    continue;
                }

                String responseData = new String(responsePacket.getData()).trim();
                Log.d("MBServerConnection", "receive response " + responseData);
                if (responseData.equals("MOUSEBUDDY_CONNECTION_RESPONSE")) {
                    return responsePacket.getAddress();
                }
            }
        }
        catch (Exception e)
        {
            connectSocket.close();
            return null;
        }
        return null;
    }

    @Override
    protected void onPostExecute(InetAddress result)
    {
        connectSocket.close();
        if (result == null)
        {
            Log.d("MBServerConection", "Failed to find server");
        }
        else
        {
            new Thread(new DataSender(result, activity)).start();
        }

    }
}
