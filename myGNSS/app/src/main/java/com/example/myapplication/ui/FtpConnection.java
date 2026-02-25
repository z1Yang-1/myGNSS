package com.example.myapplication.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

public class FtpConnection {
    static ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    static int currentUtcHour = utcDateTime.getHour()-3;
    static int currentUtcHour2 = utcDateTime.getHour();
    static String strCurrentUtcHour;
    static String strCurrentUtcHour2;
    private static int dayOfYear = utcDateTime.getDayOfYear()-1;
    private static int dayOfYear_brdc = utcDateTime.getDayOfYear();
    private static final int Year = utcDateTime.getYear();;
    private static final String server = "igs.gnsswhu.cn";
    private static final int port = 21;
    private static final String TAG = "FtpHelper";
    // 远程FTP服务器上的文件夹路径
    // 本地目录路径，用于保存下载的文件
    private static final int TIMEOUT = 5000;
    private static final String ENCODING = "UTF-8";

    private static int getgpstweek(){
        // 设置GPS时间的起始日期
        Calendar gpsStart = Calendar.getInstance();
        gpsStart.set(1980, Calendar.JANUARY, 6);
        // 获取UTC时间的当前实例
        Calendar now = Calendar.getInstance(TimeZone.getDefault()); // 这里使用了TimeZone.getDefault()来获取UTC时间
        // 计算从GPS时间开始到现在的总天数
        long daysBetween = now.getTimeInMillis() - gpsStart.getTimeInMillis();
        // 将天数转换为周数
        return (int) (daysBetween / (7 * 24 * 60 * 60 * 1000L));
    }
    public static FTPClient getFTPClient() throws IOException {
        FTPClient client = new FTPClient();
        // 设置连接超时
        client.setConnectTimeout(TIMEOUT);
        // 设置FTP控制连接的字符编码
        client.setControlEncoding(ENCODING);

        // 连接到FTP服务器
        client.connect(server, port);
        Log.d("FTP", "Connected to " + server + " on " + port);

        // 使用匿名登录
        if (!client.login("anonymous", "")) {
            Log.d("FTP", "Could not login to " + server);
            return null; // 或者抛出异常
        }
        Log.d("FTP", "Logged in to " + server);

        // 设置为主动模式
        client.enterLocalActiveMode();
        client.setFileType(FTP.BINARY_FILE_TYPE); // 设置文件类型为二进制，防止文件损坏
        Log.d("FTP", "FTP connection status codes" + client.getReplyCode());
        return client;
    }
    // 下载FTP服务器上的最新的.gz文件到本地目录
    //option == 1:下载广播星历
    //option == 2:下载精密星历
    //option == other:两者都下载
    public static void downloadFile(Context context, FTPClient ftpClient, String[] remoteDirPath,
                                    String[] infile, int option) throws IOException {
        FTPFile brdclatestFile = null;
        FTPFile preclatestFile = null;
        FTPFile clklatestFile = null;
        File externalStorageDir = context.getExternalFilesDir(null);
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) % 100; // 获取当前年份并取模得到最后两位数字
        File myDir = new File(externalStorageDir, "myFolder");

        if(currentUtcHour<0){
            currentUtcHour+=24;
            dayOfYear-=1;
            if(currentUtcHour<10){
                strCurrentUtcHour = "0"+(currentUtcHour);
            }else {
                strCurrentUtcHour = String.valueOf((currentUtcHour));
            }
        }else{
            if(currentUtcHour<10){
                strCurrentUtcHour = "0"+(currentUtcHour);
            }else {
                strCurrentUtcHour = String.valueOf((currentUtcHour));
            }
        }
        if(currentUtcHour2-2<10){
            strCurrentUtcHour2 = "0"+(currentUtcHour2-2);
        }else {
            strCurrentUtcHour2 = String.valueOf((currentUtcHour2-2));
        }
        if (!myDir.exists()) {
            myDir.mkdir(); // 创建文件夹
        }
        if(option == 1)
        {
            // 声明状态变量
            boolean isDirectoryEmpty = false;
            boolean isPatternMismatch = false;
            String dayOfYearFormatted = String.format("%03d", dayOfYear_brdc);
            Handler handler = new Handler(Looper.getMainLooper());

            try {
                // 获取广播星历文件列表
                FTPFile[] files = ftpClient.listFiles(remoteDirPath[3]);

                if (files == null || files.length == 0) {
                    isDirectoryEmpty = true;
                } else {
                    // 寻找最新的广播星历的.gz文件
                    for (FTPFile file : files) {
                        if (file.isFile()) {
                            // 检查是否符合命名规则
                            boolean matchesN = file.getName().contains("n.gz");
                            boolean matchesHour = file.getName().contains("hour" + dayOfYearFormatted);

                            if (matchesN && matchesHour) {
                                if (brdclatestFile == null || file.getTimestamp().after(brdclatestFile.getTimestamp())) {
                                    brdclatestFile = file;
                                }
                            } else {
                                isPatternMismatch = true; // 记录存在文件但不符合规则的情况
                            }
                        }
                    }
                }

                // 根据不同情况处理结果
                if (brdclatestFile != null) {
                    // --- 情况 1: 找到文件，尝试下载 ---
                    String remoteFilePath = remoteDirPath[3] + "/" + brdclatestFile.getName();
                    infile[0] = myDir.getPath() + "/" + brdclatestFile.getName();

                    try (OutputStream outputStream = Files.newOutputStream(Paths.get(infile[0]))) {
                        boolean success = ftpClient.retrieveFile(remoteFilePath, outputStream);
                        if (success) {
                            String fileName = brdclatestFile.getName();
                            handler.post(() -> Toast.makeText(context, "Download successful: " + fileName, Toast.LENGTH_SHORT).show());
                            Log.d(TAG, fileName + " has been downloaded successfully.");
                        } else {
                            handler.post(() -> Toast.makeText(context, "FTP server rejected download (Retrieve failed)", Toast.LENGTH_SHORT).show());
                            Log.d(TAG, brdclatestFile.getName() + " download failed.");
                        }
                    }
                } else if (isDirectoryEmpty) {
                    // --- 情况 2: 目录本身就是空的 ---
                    handler.post(() -> Toast.makeText(context, "Error: Remote directory is empty", Toast.LENGTH_SHORT).show());
                    Log.d(TAG, "No files found in the directory: " + remoteDirPath[3]);
                } else if (isPatternMismatch) {
                    // --- 情况 3: 有文件但都不符合命名规则 ---
                    handler.post(() -> Toast.makeText(context, "Error: No files match 'hour" + dayOfYearFormatted + "n.gz'", Toast.LENGTH_SHORT).show());
                    Log.d(TAG, "Files exist but naming pattern mismatch for DOY: " + dayOfYearFormatted);
                } else {
                    // --- 情况 4: 其他未知原因 ---
                    handler.post(() -> Toast.makeText(context, "Broadcast ephemeris not found (Unknown error)", Toast.LENGTH_SHORT).show());
                    Log.d(TAG, "General failure in finding ephemeris.");
                }

            } catch (IOException e) {
                // --- 情况 5: 网络或 IO 异常 ---
                handler.post(() -> Toast.makeText(context, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                Log.e(TAG, "FTP operation failed", e);
            }
        }else if(option == 2)
        {
            //只获取精密星历文件
            //获取精密星历文件列表
            FTPFile[] files2 = ftpClient.listFiles(remoteDirPath[1]+"/"+getgpstweek());
            // 寻找最新的精密星历的.gz文件
            // 判断文件夹是否为空
            if (files2.length == 0) {
                System.out.println("Directory is empty.");

            }else{
                for (FTPFile file : files2) {
                    if (file.isFile() && file.getName().contains(".SP3") && file.getName()
                            .contains("_"+Year+dayOfYear+strCurrentUtcHour)) {
                        if (preclatestFile == null || file.getTimestamp().after(preclatestFile.getTimestamp())) {
                            preclatestFile = file;
                        }
                    }
                }
            }
            // 如果找到最新的精密星历的.gz文件，则下载
            if (preclatestFile != null) {
                String remoteFilePath2 = remoteDirPath[1] + "/"+getgpstweek()+"/" + preclatestFile.getName();
                infile[1] = myDir.getPath()+"/"+preclatestFile.getName();
                Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
                try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + preclatestFile.getName()))) {
                    boolean success = ftpClient.retrieveFile(remoteFilePath2, outputStream);
                    if (success) {
                        handler.post(() -> Toast.makeText(context, "精密星历下载成功"+Year+dayOfYear+strCurrentUtcHour, Toast.LENGTH_SHORT).show());
                        Log.d(TAG, preclatestFile.getName() + " has been downloaded successfully.");
                    }else {
                        handler.post(() ->Toast.makeText(context, "下载失败"+Year+dayOfYear+strCurrentUtcHour, Toast.LENGTH_SHORT).show());
                        Log.d(TAG, preclatestFile.getName() + " has been downloaded Failed.");
                    }
                }
            } else {
                for (FTPFile file : files2) {
                    if (file.isFile() && file.getName().contains(".SP3") && file.getName()
                            .contains("_"+Year+dayOfYear+strCurrentUtcHour2)) {
                        if (preclatestFile == null || file.getTimestamp().after(preclatestFile.getTimestamp())) {
                            preclatestFile = file;
                        }
                    }
                }
                if (preclatestFile != null) {
                    String remoteFilePath2 = remoteDirPath[1] + "/"+getgpstweek()+"/" + preclatestFile.getName();
                    infile[1] = myDir.getPath()+"/"+preclatestFile.getName();
                    Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
                    try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + preclatestFile.getName()))) {
                        boolean success = ftpClient.retrieveFile(remoteFilePath2, outputStream);
                        if (success) {
                            handler.post(() -> Toast.makeText(context, "精密星历下载成功", Toast.LENGTH_SHORT).show());
                            Log.d(TAG, preclatestFile.getName() + " has been downloaded successfully.");
                        }else {
                            handler.post(() ->Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show());
                            Log.d(TAG, preclatestFile.getName() + " has been downloaded Failed.");
                        }
                    }
                }else{
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "未找到精密星历文件", Toast.LENGTH_SHORT).show());
                    Log.d(TAG, "No files found in the directory.");
                }
            }
        } else if(option == 3)
        {
            //只获取CLK文件
            //获取精密星历文件列表
            FTPFile[] files3 = ftpClient.listFiles(remoteDirPath[1]+"/"+getgpstweek());
            // 寻找最新的精密星历的.gz文件
            for (FTPFile file : files3) {
                if (file.isFile() && file.getName().contains(".CLK") && file.getName().contains("_"+Year+dayOfYear+strCurrentUtcHour)) {
                    if (clklatestFile == null || file.getTimestamp().after(clklatestFile.getTimestamp())) {
                        clklatestFile = file;
                    }
                }
            }
            // 如果找到最新的精密星历的.gz文件，则下载
            if (clklatestFile != null) {
                String remoteFilePath2 = remoteDirPath[1] + "/"+getgpstweek()+"/" + clklatestFile.getName();
                infile[2] = myDir.getPath()+"/"+clklatestFile.getName();
                Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
                try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + clklatestFile.getName()))) {
                    boolean success = ftpClient.retrieveFile(remoteFilePath2, outputStream);
                    if (success) {
                        handler.post(() -> Toast.makeText(context, "钟差文件下载成功", Toast.LENGTH_SHORT).show());
                        Log.d(TAG, clklatestFile.getName() + " has been downloaded successfully.");
                    }else {
                        handler.post(() ->Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show());
                        Log.d(TAG, clklatestFile.getName() + " has been downloaded Failed.");
                    }
                }
            } else {
                for (FTPFile file : files3) {
                    if (file.isFile() && file.getName().contains(".CLK") && file.getName().contains("_"+Year+dayOfYear+strCurrentUtcHour2)) {
                        if (clklatestFile == null || file.getTimestamp().after(clklatestFile.getTimestamp())) {
                            clklatestFile = file;
                        }
                    }
                }
                // 如果找到最新的精密星历的.gz文件，则下载
                if (clklatestFile != null) {
                    String remoteFilePath2 = remoteDirPath[1] + "/"+getgpstweek()+"/" + clklatestFile.getName();
                    infile[2] = myDir.getPath()+"/"+clklatestFile.getName();
                    Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
                    try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + clklatestFile.getName()))) {
                        boolean success = ftpClient.retrieveFile(remoteFilePath2, outputStream);
                        if (success) {
                            handler.post(() -> Toast.makeText(context, "钟差文件下载成功", Toast.LENGTH_SHORT).show());
                            Log.d(TAG, clklatestFile.getName() + " has been downloaded successfully.");
                        }else {
                            handler.post(() ->Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show());
                            Log.d(TAG, clklatestFile.getName() + " has been downloaded Failed.");
                        }
                    }
                }else{
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "未找到精密星历文件", Toast.LENGTH_SHORT).show());
                    Log.d(TAG, "No files found in the directory.");
                }
            }
        } else if(option == 4)
        {
            //三种星历文件都获取
            //获取广播星历文件列表
            FTPFile[] files = ftpClient.listFiles(remoteDirPath[0]);
            //获取精密星历文件列表
            FTPFile[] files2 = ftpClient.listFiles(remoteDirPath[1]+"/"+getgpstweek());
            //获取精密星历文件列表
            FTPFile[] files3 = ftpClient.listFiles(remoteDirPath[1]+"/"+getgpstweek());
            // 寻找最新的广播星历的.gz文件
            for (FTPFile file : files) {
                if (file.isFile() && file.getName().contains("MN.rnx.gz")&&file.getName().contains("ABPO")) {
                    if (brdclatestFile == null || file.getTimestamp().after(brdclatestFile.getTimestamp())) {
                        brdclatestFile = file;
                    }
                }
            }
            // 寻找最新的广播星历的.gz文件，并且内存最大
//            for (FTPFile file : files) {
//                if (file.isFile() && file.getName().contains("MN.rnx.gz")) {
//                    if (brdclatestFile == null ||
//                            file.getTimestamp().after(brdclatestFile.getTimestamp()) ||
//                            (file.getTimestamp().equals(brdclatestFile.getTimestamp()) && file.getSize() > brdclatestFile.getSize())) {
//                        brdclatestFile = file;
//                    }
//                }
//            }
            // 寻找最新的精密星历的.gz文件
            for (FTPFile file : files2) {
                if (file.isFile() && file.getName().contains(".SP3") && file.getName().contains("_"+Year+dayOfYear+strCurrentUtcHour)) {
                    if (preclatestFile == null || file.getTimestamp().after(preclatestFile.getTimestamp())) {
                        preclatestFile = file;
                    }
                }
            }
            // 寻找最新的精密星历的.gz文件
            for (FTPFile file : files3) {
                if (file.isFile() && file.getName().contains(".CLK") && file.getName().contains("_"+Year+dayOfYear+strCurrentUtcHour)) {
                    if (clklatestFile == null || file.getTimestamp().after(clklatestFile.getTimestamp())) {
                        clklatestFile = file;
                    }
                }
            }
            // 如果找到最新的广播\精密星历的.gz文件，则下载
            if (brdclatestFile != null) {
                String remoteFilePath = remoteDirPath[0] + "/" + brdclatestFile.getName();
                infile[0] = myDir.getPath()+"/"+brdclatestFile.getName();
                Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
                try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + brdclatestFile.getName()))) {
                    boolean success = ftpClient.retrieveFile(remoteFilePath, outputStream);
                    if (success) {
                        FTPFile finalBrdclatestFile = brdclatestFile;
                        handler.post(() -> Toast.makeText(context, "广播星历下载成功:"+ finalBrdclatestFile.getName(), Toast.LENGTH_SHORT).show());
                        Log.d(TAG, brdclatestFile.getName() + " has been downloaded successfully.");
                    }else {
                        handler.post(() ->Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show());
                        Log.d(TAG, brdclatestFile.getName() + " has been downloaded Failed.");
                    }
                }
            } else {
                // 寻找最新的广播星历的.gz文件
                for (FTPFile file : files) {
                    if (file.isFile() && file.getName().contains(dayOfYear+"0."+year+"n.gz")) {
                        if (brdclatestFile == null || file.getTimestamp().after(brdclatestFile.getTimestamp())) {
                            brdclatestFile = file;
                        }
                    }
                }
                // 如果找到最新的广播\精密星历的.gz文件，则下载
                if (brdclatestFile != null) {
                    String remoteFilePath = remoteDirPath[0] + "/" + brdclatestFile.getName();
                    infile[0] = myDir.getPath()+"/"+brdclatestFile.getName();
                    Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
                    try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + brdclatestFile.getName()))) {
                        boolean success = ftpClient.retrieveFile(remoteFilePath, outputStream);
                        if (success) {
                            handler.post(() -> Toast.makeText(context, "广播星历下载成功", Toast.LENGTH_SHORT).show());
                            Log.d(TAG, brdclatestFile.getName() + " has been downloaded successfully.");
                        }else {
                            handler.post(() ->Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show());
                            Log.d(TAG, brdclatestFile.getName() + " has been downloaded Failed.");
                        }
                    }
                }else{
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "未找到广播星历文件", Toast.LENGTH_SHORT).show());
                    Log.d(TAG, "No files found in the directory.");
                }
            }
            // 如果找到最新的精密星历的.gz文件，则下载
            if (preclatestFile != null) {
                String remoteFilePath2 = remoteDirPath[1] + "/"+getgpstweek()+"/" + preclatestFile.getName();
                infile[1] = myDir.getPath()+"/"+preclatestFile.getName();
                Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
                try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + preclatestFile.getName()))) {
                    boolean success = ftpClient.retrieveFile(remoteFilePath2, outputStream);
                    if (success) {
                        handler.post(() -> Toast.makeText(context, "精密星历下载成功"+Year+dayOfYear+strCurrentUtcHour, Toast.LENGTH_SHORT).show());
                        Log.d(TAG, preclatestFile.getName() + " has been downloaded successfully.");
                    }else {
                        handler.post(() ->Toast.makeText(context, "下载失败"+Year+dayOfYear+strCurrentUtcHour, Toast.LENGTH_SHORT).show());
                        Log.d(TAG, preclatestFile.getName() + " has been downloaded Failed.");
                    }
                }
            } else {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "未找到精密星历文件"+getgpstweek()+Year+dayOfYear+strCurrentUtcHour, Toast.LENGTH_SHORT).show());
                Log.d(TAG, "No files found in the directory.");
            }
            // 如果找到最新的精密星历的.gz文件，则下载
            if (clklatestFile != null) {
                String remoteFilePath2 = remoteDirPath[1] + "/"+getgpstweek()+"/" + clklatestFile.getName();
                infile[2] = myDir.getPath()+"/"+clklatestFile.getName();
                Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
                try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + clklatestFile.getName()))) {
                    boolean success = ftpClient.retrieveFile(remoteFilePath2, outputStream);
                    if (success) {
                        handler.post(() -> Toast.makeText(context, "钟差文件下载成功", Toast.LENGTH_SHORT).show());
                        Log.d(TAG, clklatestFile.getName() + " has been downloaded successfully.");
                    }else {
                        handler.post(() ->Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show());
                        Log.d(TAG, clklatestFile.getName() + " has been downloaded Failed.");
                    }
                }
            } else {
                // 寻找最新的精密星历的.gz文件
//                for (FTPFile file : files3) {
//                    if (file.isFile() && file.getName().contains(".CLK") && file.getName().contains("_"+Year+dayOfYear+strCurrentUtcHour2)) {
//                        if (clklatestFile == null || file.getTimestamp().after(clklatestFile.getTimestamp())) {
//                            clklatestFile = file;
//                        }
//                    }
//                }
//                // 如果找到最新的精密星历的.gz文件，则下载
//                if (clklatestFile != null) {
//                    String remoteFilePath2 = remoteDirPath[1] + "/"+getgpstweek()+"/" + clklatestFile.getName();
//                    infile[2] = myDir.getPath()+"/"+clklatestFile.getName();
//                    Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
//                    try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + clklatestFile.getName()))) {
//                        boolean success = ftpClient.retrieveFile(remoteFilePath2, outputStream);
//                        if (success) {
//                            handler.post(() -> Toast.makeText(context, "钟差文件下载成功", Toast.LENGTH_SHORT).show());
//                            Log.d(TAG, clklatestFile.getName() + " has been downloaded successfully.");
//                        }else {
//                            handler.post(() ->Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show());
//                            Log.d(TAG, clklatestFile.getName() + " has been downloaded Failed.");
//                        }
//                    }
//                }else{
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "未找到钟差文件", Toast.LENGTH_SHORT).show());
                Log.d(TAG, "No files found in the directory.");
//                }
            }
        }else if(option == 5)
        {
            //获取混合星历文件
            //获取广播星历文件列表
            FTPFile[] files = ftpClient.listFiles(remoteDirPath[0]);
            // 寻找最新的广播星历的.gz文件
            for (FTPFile file : files) {
                if (file.isFile() && file.getName().contains("MN.rnx.gz")&&file.getName().contains("BRMG")) {
                    if (brdclatestFile == null || file.getTimestamp().after(brdclatestFile.getTimestamp())) {
                        brdclatestFile = file;
                    }
                }
            }
            // 如果找到最新的广播\精密星历的.gz文件，则下载
            if (brdclatestFile != null) {
                String remoteFilePath = remoteDirPath[0] + "/" + brdclatestFile.getName();
                infile[0] = myDir.getPath()+"/"+brdclatestFile.getName();
                Handler handler = new Handler(Looper.getMainLooper()); // 在这里声明handler变量
                try (OutputStream outputStream = Files.newOutputStream(Paths.get(myDir.getPath() + "/" + brdclatestFile.getName()))) {
                    boolean success = ftpClient.retrieveFile(remoteFilePath, outputStream);
                    if (success) {
                        FTPFile finalBrdclatestFile2 = brdclatestFile;
                        handler.post(() ->
                                Toast.makeText(context, "Mixed ephemeris download successful"+ finalBrdclatestFile2.getName(), Toast.LENGTH_SHORT)
                                        .show());
                        Log.d(TAG, brdclatestFile.getName()
                                + " has been downloaded successfully.");
                    }else {
                        handler.post(() ->
                                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show());
                        Log.d(TAG, brdclatestFile.getName() + " has been downloaded Failed.");
                    }
                }
            } else {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "No files found in the directory", Toast.LENGTH_SHORT).show());
                Log.d(TAG, "No files found in the directory.");
            }
        }
    }

    public static void decompressGzipFile(String[] infile, String[] outputFilePath)
            throws IOException {
        if(infile[0]!=null && new File(infile[0]).exists()){
            try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(Paths.get(infile[0])));
                 FileOutputStream fos = new FileOutputStream(outputFilePath[0])) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(infile[1]!=null && new File(infile[1]).exists()) {
            try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(Paths.get(infile[1])));
                 FileOutputStream fos = new FileOutputStream(outputFilePath[1])) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(infile[2]!=null && new File(infile[2]).exists()){
            try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(Paths.get(infile[2])));
                 FileOutputStream fos = new FileOutputStream(outputFilePath[2])) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
