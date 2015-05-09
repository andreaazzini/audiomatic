package se.kth.id2012_project;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

public class TCPClient implements Runnable {
    private static final String SERVER_IP = "130.229.161.101";
    private static final int PORT = 4444;
    private static final int BYTE_LENGTH = 4096;

    private Socket socket;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;


    @Override
    public void run() {
        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            socket = new Socket(serverAddr, PORT);

            inputStream = new BufferedInputStream(socket.getInputStream());
            outputStream = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String message) {
        byte[] outgoingMessage = message.getBytes();
        try {
            outputStream.write(outgoingMessage);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receive() {
        try {
            return new GetResponseTask().execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void closeConnection() {
        try {
            outputStream.close();
            inputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class GetResponseTask extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... params) {
            byte[] fromServer = new byte[100];
            try {
                int n = inputStream.read(fromServer);
                String response = new String(fromServer);
                return response.substring(0, n);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
