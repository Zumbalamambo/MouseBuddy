package cmu.ruixin.mousebuddy;

import java.net.*;
import java.io.*;

import android.os.AsyncTask;

/**
 * Created by JJ on 2/21/2015.
 */
public class ConnectServerAsyncTask extends AsyncTask<Void, Void, InetAddress>
{
    static final int CONNECT_PORT = 55556;

    DatagramSocket connectSocket;
    MouseActivity activity;
    byte[] connectData;

    public ConnectServerAsyncTask(MouseActivity ma) throws SocketException
    {
        connectSocket = new DatagramSocket();
        connectSocket.setBroadcast(true);
        connectSocket.setSoTimeout(2000);

        connectData = "MOUSEBUDDY_CONNECTION_REQUEST".getBytes();
        activity = ma;
    }

    @Override
    protected InetAddress doInBackground(Void... a)
    {
        final int ATTEMPTS = 20;
        try {
            for (int i = 0; i < ATTEMPTS; i++) {
                DatagramPacket connectPacket = new DatagramPacket(connectData, connectData.length,
                        InetAddress.getByName("255.255.255.255"), CONNECT_PORT);
                connectSocket.send(connectPacket);

                DatagramPacket responsePacket;

                try {
                    byte[] responseBuf = new byte[512];
                    responsePacket = new DatagramPacket(responseBuf, responseBuf.length);
                    connectSocket.receive(responsePacket);
                } catch (SocketTimeoutException e) {
                    continue;
                }

                String responseData = new String(responsePacket.getData()).trim();
                if (responseData.equals("MOUSEBUDDY_CONNECTION_RESPONSE"))
                {
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
            // failed to connect
        }
        else
        {
            new Thread(new DataSender(result, activity)).start();
        }

    }
}
