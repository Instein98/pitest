package org.pitest.util;

import org.pitest.testapi.Description;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commons {
    /**
     * @author: Jun Yang
     */
    @Deprecated
    public static void addContentToPool(String poolPath, String content) {
        FileWriter fw = null;
        try {
            File f=new File(poolPath);
            fw = new FileWriter(f, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(fw);
        pw.println(content);
        pw.flush();
        try {
            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @author: Jun Yang
     * @param fileName
     * @return
     */
    @Deprecated
    public static String readToString(String fileName) {
        // String encoding = "ISO-8859-1";
        File file = new File(fileName);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // try {
        // return new String(filecontent, encoding);
        return new String(filecontent);
        // } catch (UnsupportedEncodingException e) {
        //   System.err.println("The OS does not support " + encoding);
        //   e.printStackTrace();
        //   return null;
        // }
    }

    /**
     *
     * @author: Jun Yang
     */
    @Deprecated
    public static List<String> readToLinesList(String fileName) {
        List<String> linesList = new ArrayList<>();

        try {
            BufferedReader br;
            br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                linesList.add(line.replace("\n", ""));
            }
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return linesList;
    }

    public static <V> Map<String, V> getStringMapFromDescMap(Map<Description, V> descMap)
    {
        Map<String, V> stringMap = new HashMap<>();
        for (Map.Entry<Description, V>entry : descMap.entrySet())
        {
            stringMap.put(entry.getKey().getFirstTestClass() + "." + entry.getKey().getName(), entry.getValue());
        }

        return stringMap;
    }
}
