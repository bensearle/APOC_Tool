/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package apoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author bsearle
 */
public class Chris {

    String[] mimicReadToDo;  //  this is the list of mimics that have been found in the system that are used
    String currentMimic;  //  the name of the mimic that is currently being analysed
    String server;  //  the server that is being used

    String cFilepath;  //  the file path where the mimics are stored
    String cFolder;
    String cFile;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //  call the method to start analysing the HEART data
        new Chris().doOrder();
        //new HEART().oneOff();
    }

    /*
     * method used in test phases to call methods that need testing
     */
    public void oneOff() {

    }

    /*
     * method to return a list of all the files in a specific directory
     */
    public static String[] fileNames(String directoryPath) {
        File dir = new File(directoryPath);
        Collection<String> files = new ArrayList<String>();
        if (dir.isDirectory()) {
            File[] listFiles = dir.listFiles();

            for (File file : listFiles) {
                if (file.isFile()) {
                    files.add(file.getName());
                }
            }
        }
        return files.toArray(new String[]{});
    }

    /*
     * method to call all methods in the order that they need to be executed
     */
    public void doOrder() {
        addAllMimcs("LHRVM300110");
        addAllMimcs("LHRVM300112");
        addAllMimcs("LHRVM300113");
        addAllMimcs("LHRVM300114");
        addAllMimcs("LHRVM300115");
        addAllMimcs("LHRVM300116");
        addAllMimcs("LHRVM300118");
        addAllMimcs("LHRVM300120");
        addAllMimcs("LHRVM300122");
        addAllMimcs("LHRVM300124");
        addAllMimcs("LHRVM300126");
        addAllMimcs("LHRVM300128");
        addAllMimcs("LHRVM300146");
        addAllMimcs("LHRVM300148");
        addAllMimcs("LHRVM300158");
        addAllMimcs("LHRVM300182");
        addAllMimcs("LHRVM300183");
    }

    /*
     * method to analyse all mimics, regardless of server
     * only used for testing
     */
    public void addAllMimcs(String f) {
        cFolder = f;
        cFilepath = "G:\\2014-05-13 FACET XML\\2014-05-13 FACET XML\\" + f + "\\";
        String[] fileNameList = fileNames(cFilepath);
        for (String fn : fileNameList) {
            cFile = fn;
            readMimic(fn);
        }
    }

    public void scanMimic(String fileline) {
        Scanner sc = new Scanner(fileline);
        String s;
        while (sc.hasNext()) {
            s = sc.next();
            if (s.toLowerCase().contains("product_code")) {
                Pattern p = Pattern.compile("\"([^\"]*)\"");
                Matcher m = p.matcher(s);
                while (m.find()) {
                    System.out.println(cFolder + "," + cFile.replace(".xml", "") + "," + m.group(1));
                    // run in netbeans, copy from output window to text file, save as .csv, open in excel to remove duplicates
                }
            }
        }
    }

    public void readMimic(String filename) {
        BufferedReader br = null;
        File file = new File(cFilepath, cFile);
        if (file.exists()) {
            //System.out.println("Searching " + filename);
            try {
                String sCurrentLine;
                br = new BufferedReader(new FileReader(cFilepath + cFile));
                while ((sCurrentLine = br.readLine()) != null) {
                    // Check that the string is not empty
                    if (sCurrentLine.trim().length() != 0) {
                        scanMimic(sCurrentLine);
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
                //             e.printStackTrace();
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            //System.out.println("Cannot Find: " + filename);
        }
    }
}
