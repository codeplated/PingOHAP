package codeplated.pingohap;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * Created by honey on 5/13/2016.
 */
public class PingConnection {

    private boolean running = false;
    private HandlerThread handlerThread = null;
    private IncomingThread incomingThread = null;
    private Socket socket = null;
    private Handler outgoingMessageHandler = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private URL url = null;
    private PingObserver observer = null;
    private static final String TAG = "PingConnection";
    private static PingConnection pingConnection=null;
    private boolean dataCame;

    protected PingConnection(){

    }
    public static PingConnection getInstance(){
        if(pingConnection==null){
            pingConnection=new PingConnection();
            return pingConnection;
        }else{
            return pingConnection;
        }
    }
    public void initialize(URL url, PingObserver obs){

        this.url=url;
        observer=obs;
        Log.d(TAG,"Sigelton Initialized");
    }
    public void start(PingObserver obs){
        this.observer=obs;
        Log.d(TAG,"Starting Networking");
        startNetworking();

    }
    public void stop(){

        Log.d(TAG,"Stopping Networking");
        observer=null;
        stopNetworking();



    }
    public void doPing() {


        if (outgoingMessageHandler!=null ) {

            long pingIdentifier = SystemClock.uptimeMillis();
            Log.d(TAG, "Send ping to server: " + pingIdentifier);
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x02).integer32(pingIdentifier);
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
        }else{
            Log.d(TAG, "Outgoing Message Handler is Null");
        }
    }
    private void startNetworking(){
        running=true;
        handlerThread=new HandlerThread("PingConnectionHandlerThread");
        handlerThread.start();
        incomingThread=new IncomingThread();
        incomingThread.start();

    }
    private void stopNetworking(){
        try {
            running=false;
            handlerThread.stop();
            handlerThread=null;
            ///////////////////
            incomingThread.interrupt();
            incomingThread.join();
            incomingThread=null;
            ///////////////////
            outgoingMessageHandler=null;
            Log.d(TAG, "Stopping Networking ");
        } catch (InterruptedException e) {
            Log.d(TAG, "Stopping Networking, Interupt exeption ");
            e.printStackTrace();
        } catch (RuntimeException e){
            Log.d(TAG, "Stopping Networking, Runtime exeption ");
            e.printStackTrace();
        }
    }
    public interface PingObserver {
        public void handlePingResponse(IncomingMessage response);
    }

    private class IncomingThread extends Thread{

        @Override
        public void run() {
            super.run();
            socket=new Socket();
            try {


                socket.setSoTimeout(5000);

                InetSocketAddress address = new InetSocketAddress(url.getHost(),url.getPort());
                Log.d(TAG, " Socket address=" + address);
                socket.connect(address, 5000);

                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
                outgoingMessageHandler= new Handler(handlerThread.getLooper());
                Handler incomingHandler=new Handler(Looper.getMainLooper());


                Log.d(TAG, "Sending login message");

                OutgoingMessage outgoingMessage = new OutgoingMessage();
                outgoingMessage.integer8(0x00)
                        .integer8(0x01)
                        .text("antti")
                        .text("password");
                outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
                while(running){
                    if(socket!=null){

                        try {
                            Log.d(TAG, "Just about to read from input stream");
                            IncomingMessage incomingMessage= new IncomingMessage();
                            incomingMessage.readFrom(inputStream);
                            if(incomingMessage.toString().length()>0){
                                dataCame = true;
                                Log.d(TAG, "Incoming Message String" + incomingMessage.toString()+ " Length: " + incomingMessage.toString().length());
                            }

                            if(dataCame==true){
                                IncomingMessageAction incomingMessageAction= new IncomingMessageAction(incomingMessage);
                                incomingHandler.post(incomingMessageAction);
                                dataCame=false;

                            }
                        } catch (SocketTimeoutException e) {
                            //running=false;
                            Log.d(TAG, "Main while loop is restarted");
                            //e.printStackTrace();
                        }

                    }
                }
                if(socket!=null){

                    Log.d(TAG, "nullyfying all streams and main socket connection ");
                    socket.close();
                    socket=null;
                    inputStream=null;
                    outputStream=null;
                    outgoingMessageHandler=null;
                }


            }catch (SocketException e){

                Log.d(TAG,"Error while Setting Socket Timeout or connecting");

                e.printStackTrace();
            }catch (IOException e) {
                running=false;
                Log.d(TAG,"IO exception caught in main RUN()");

                e.printStackTrace();
            }

        }
    }
    private class IncomingMessageAction implements Runnable {
        private IncomingMessage incomingMessage;
        public IncomingMessageAction(IncomingMessage incomingMessage) {
            this.incomingMessage = incomingMessage;
        }

        @Override
        public void run() {
            // Handle ping reply message.
            Log.d(TAG, "Incoming msg: " + incomingMessage);
            if (null != observer) {
                observer.handlePingResponse(incomingMessage);
            }
        }
    }
    private class OutgoingMessageAction implements Runnable{
        private OutgoingMessage outgoingMessage;

        public OutgoingMessageAction(OutgoingMessage outgoingMessage) {
            this.outgoingMessage = outgoingMessage;
        }

        @Override
        public void run() {
            Log.d(TAG, "Outgoing msg: " + outgoingMessage);

            if(socket !=null && outputStream !=null){
                try {
                    Log.d(TAG, "Writing to out put stream of socket ");
                    ///////////////////////////////////////////
                    /*Keep an eye here*/
                    //////////////////////////////////////////
                    outgoingMessage.writeTo(outputStream);
                    socket.getOutputStream().write(outputStream.toString().getBytes());
                } catch (IOException e) {
                    Log.d(TAG, "Error writing in outputstream in OutgoingMessageAction");
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

}
