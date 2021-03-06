package com.example.sye;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MyService extends Service {
    public static final String hostname = "10.0.2.2";
    public final int port = 7777;
    public Socket clientSocket;
    public ConnectThread thread;
    public ReceiveThread receivethread;
    public PrintWriter sendWriter;
    private ArrayList<EventModule> transactionList = new ArrayList<EventModule>();
    public String[] fund_transaction = new String[2];
    public Transaction transaction = new Transaction();
    public String[] transactionAry;
    public EventModule module;
    public Boolean status=false;
    public String JsonList[];
    public JSONObject jsonObject;
    public MyService() {

    }

    private IBinder mBinder = new MyBinder();
    public class MyBinder extends Binder {
        public MyService getService(){
            return MyService.this;
        }
    }

    public ArrayList<EventModule> getTransactionList() {
        return transactionList;
    }

    public void setTransactionList(ArrayList<EventModule> transactionList) {
        this.transactionList = transactionList;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(FSOCKET.ACCESS_CODE.equals(intent.getAction())){
            startForgroundService();
            thread = new ConnectThread();
            thread.start();
        }
        else if(FSOCKET.QUIT_CODE.contains(intent.getAction())){
            SendThread sendThread = new SendThread(intent.getAction());
            sendThread.start();
        }
        else if(intent.getAction().contains(FSOCKET.NEWUSER_CODE)){
            SendThread sendThread = new SendThread(intent.getAction());
            sendThread.start();
        }
        else if(intent.getAction().contains(FSOCKET.FUNDING_CODE)){
            SendThread sendThread = new SendThread(intent.getAction());
            sendThread.start();
            //????????? ??? ????????? ?????????, ReceiveThread?????? ????????? ???????????? ??????????????????
        }
        else if(intent.getAction().contains(FSOCKET.LOGIN_CODE)){
            SendThread sendThread = new SendThread(intent.getAction());
            sendThread.start();
        }
        else if(intent.getAction().contains(FSOCKET.CHARGE_CODE)){
            SendThread sendThread = new SendThread(intent.getAction());
            sendThread.start();
        }
        else if(intent.getAction().contains(FSOCKET.COMPENSATE_CODE)){//intent.getAction()??? compensatecha&10000?????????????????????      money??? 0?????? ???????????? ??????????????????
            SendThread sendThread = new SendThread(intent.getAction());
            sendThread.start();
        }


        //return????????? ???????????? ??????????????????
        //???????????? ????????? intent??? ?????? ???????????? ?????????.
        return START_STICKY;//???????????? ????????? ???????????? or ???????????? ??????????????? ?????????????????? ???????????? ???????????????
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("onDestroy","onDestory: ");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void startForgroundService(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"default");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("???????????? ??????");
        builder.setContentText("????????? ???????????? ????????????");

        Intent notificationIntent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
        builder.setContentIntent(pendingIntent);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(new NotificationChannel("default","?????? ??????",NotificationManager.IMPORTANCE_DEFAULT));
        }
        startForeground(1,builder.build());
    }

    public class SendThread extends Thread {
        public String msg = "";

        public SendThread(String msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            sendWriter.println(msg);
            sendWriter.flush();
        }
    }

    public class ConnectThread extends Thread{
        public void run(){
            try {
                clientSocket = new Socket(hostname,port);
                Log.d("ConnectThread","1");
                sendWriter = new PrintWriter(clientSocket.getOutputStream());
                receivethread = new ReceiveThread();
                receivethread.setSocket(clientSocket);
                receivethread.start();
                while(true){
                    if(status){
                        sendWriter.close();
                        clientSocket.close();
                        stopSelf();
                        break;
                    }
                }
            }
            catch (UnknownHostException e){
                Log.e("TAG"," ?????? Error : ???????????? IP ????????? ????????? ??? ??????.(????????? ?????? ??? ?????? ??????????????? ??????)");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch(SecurityException e){
                Log.e("TAG"," ?????? Error : ??????(Security) ????????? ?????? ?????? ?????????(Security Manager)??? ?????? ??????. (?????????(proxy) ?????? ??????, ???????????? ?????? ?????? ??????)");
            }
            catch (IllegalArgumentException e){
                Log.e("TAG"," ?????? Error : ???????????? ????????? ??????????????? ???????????? ?????? ??????. (0~65535 ?????? ?????? ?????? ?????? ??????, null ?????????(proxy) ??????)");
            }
        }
    }

    public class ReceiveThread extends Thread{
        private Socket socket;
        EventModule module = new EventModule();
        boolean first_status = true;
        boolean thread_status = true;
        boolean msg_Type = true;

        public void setSocket(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String receiveString ="";

                Log.d("ReceiveThread","1");
                while(socket != null) {
                    receiveString = bufferedreader.readLine();
                    if(first_status){
                        receiveString = receiveString.substring(1,receiveString.length() -2);
                        Log.d("ss",receiveString);
                        transactionAry = receiveString.split("\\},");
                        for (int i = 0; i < transactionAry.length; i++) {
                            transactionAry[i] += "}";
                            Log.d("?????????2 : ", transactionAry[i] + "/" + transactionAry.length);
                        }

                        for (int i =0;i<transactionAry.length;i++){
                            jsonObject = new JSONObject(transactionAry[i]);
                            module = new EventModule();
                            module.setUserID(jsonObject.getString("userID"));
                            module.setMoney(jsonObject.getString("Money"));
                            transaction.getTransaction().add(module);
                        }
                        for(int i =0;i<transaction.getTransaction().size();i++){
                            Log.d("good",transaction.getTransaction().get(i).getUserID() + "/" + transaction.getTransaction().get(i).getMoney());
                        }
                        first_status = false;
                    }
                    else{
                        if(receiveString.equals(FSOCKET.TIMEOUT_CODE)){
                            SendHashThread hashThread = new SendHashThread(transaction);
                            hashThread.start();
                        }
                        else if(receiveString.equals("quit_ok")){
                            thread_status = false;
                            break;
                        }
                        else if(receiveString.contains(FSOCKET.ACCURATETRANSACTION_CODE)) {
                            EventModule receivemodule;
                            Log.d("?????? ????????? ??????????????? ???????????? ", receiveString);
                            String receiveTransaction = receiveString.substring(20,receiveString.length()-1);
                            JSONObject receivejsonObject;
                            JsonList = receiveTransaction.split("\\},");
                            transaction.getTransaction().clear();
                            for (int i = 0; i < JsonList.length - 1; i++) {
                                receivemodule = new EventModule();
                                JsonList[i] += "}";
                                Log.d("???????????? : ", JsonList[i]);
                                receivejsonObject = new JSONObject(JsonList[i]);
                                receivemodule.setUserID(receivejsonObject.getString("userID"));
                                receivemodule.setMoney(receivejsonObject.getString("Money"));
                                transaction.getTransaction().add(receivemodule);
                            }

                            for(int i =0;i<transaction.getTransaction().size();i++){
                                Log.d("accurate",transaction.getTransaction().get(i).getUserID() + "/" + transaction.getTransaction().get(i).getMoney());
                            }
                        }
                        else{
                            jsonObject = new JSONObject(receiveString);
                            Log.d("why",receiveString);
                            EventModule module = new EventModule();
                            module.setUserID(jsonObject.getString("userID"));
                            module.setMoney(jsonObject.getString("Money"));

                            for(EventModule t : transaction.getTransaction()){
                                if(t.getUserID().equals(module.getUserID())){
                                    if(module.getMoney().contains("-")){
                                        t.setMoney(String.valueOf(Integer.parseInt(t.getMoney()) - Integer.parseInt(module.getMoney().substring(1))));
                                    }
                                    else{
                                        t.setMoney(String.valueOf(Integer.parseInt(t.getMoney()) + Integer.parseInt(module.getMoney())));
                                    }
                                    msg_Type = false;
                                }
                            }
                            if(msg_Type){
                                transaction.getTransaction().add(module);
                                Log.d("MyService 256",module.getUserID() + "/" + module.getMoney());
                            }
                            for(int i =0;i<transaction.getTransaction().size();i++){
                                Log.d("good",transaction.getTransaction().get(i).getUserID() + "/" + transaction.getTransaction().get(i).getMoney());
                            }
                            msg_Type = true;
                        }
                    }
                }
                Log.d("first","blood");
                bufferedreader.close();
                status = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    public class SendHashThread extends Thread{

        public Transaction transaction;
        public String StringOfTransaction;
        public String HashOfTransaction;
        public SendHashThread(Transaction transaction){
            this.transaction = transaction;
        }

        @Override
        public void run() {
            //????????????
//            for(EventModule component : transaction.getTransaction()){
//                if(component.getUserID().equals("hongjineer")){
//                    component.setMoney(String.valueOf(Integer.parseInt(component.getMoney()) + 3000000));
//                }
//            }
            //hongjineer????????? ??????????????? 300????????? ???????????? ????????????

            for (EventModule component : transaction.getTransaction()){
                StringOfTransaction += (component.getUserID() + component.getMoney());
            }
            HashOfTransaction = SignatureUtil.applySHA256(StringOfTransaction);
            HashOfTransaction = "hash" + HashOfTransaction;
            Log.d("?????? ???????????? ",HashOfTransaction.substring(4));
            SendThread send = new SendThread(HashOfTransaction);
            send.start();
        }
    }
}
