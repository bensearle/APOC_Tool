/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apoc;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author bsearle
 */
public class APOC {

    PrintWriter output;
    PrintWriter output_evac_zone;
    PrintWriter output_devices;
    String path; // static path
    String filepath; // location of input files
    // output column headers
    String folderXML;
    String uniqueIdentifier;
    String area;
    String attribute;
    String type;
    String label;
    String opcInterfacePc;
    String opcServerName;
    String opcTag;
    String nodeLabel; // nodes get their label from a different place in txt files.
    boolean nodeDone; // nodes sometimes appear twice, only want data once
    // uniqueIdentifier parts
    String identifierArea;
    String identifierNode;
    String identifierLoop;
    String identifierDevice;
    String identifierType;
    String identifierChannel;
    String evac_zone_lookup_location;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        new APOC().initialize();
    }

    public static String[] getFileNames(String directoryPath) {
        File dir = new File(directoryPath);
        Collection<String> files = new ArrayList<String>();
        if (dir.isDirectory()) {
            File[] listFiles = dir.listFiles();

            for (File file : listFiles) {
                /*if (file.isFile()) {
                 files.add(file.getName());
                 }*/
                files.add(file.getName());
            }
        }
        return files.toArray(new String[]{});
    }

    /*
     * create output file with timestamp
     * get folder/file names
     * 
     */
    public void initialize() throws FileNotFoundException {
        // create JFrame to tell the user to wait
        final JFrame frame = new JFrame();
        frame.setVisible(true);
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        frame.add(panel);
        panel.setLayout(new GridLayout(2, 1));
        JLabel label = new JLabel("please wait whilst mapping register is created");
        label.setHorizontalAlignment(JLabel.CENTER);
        panel.add(label);

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HHmm");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        output = new PrintWriter("C:\\APOC_Tool\\OUTPUT\\" + strDate + " devices_txt.txt"); // create .txt for the devices, for txt files
        evac_zone_lookup_location = "C:\\APOC_Tool\\OUTPUT\\" + strDate + " evac_zones_xml.csv";
        output_evac_zone = new PrintWriter(evac_zone_lookup_location); // create .csv for evac zones, used as lookup
        output_devices = new PrintWriter("C:\\APOC_Tool\\OUTPUT\\" + strDate + " devices_xml.csv"); // create .csv for the devices, for xml files

        // add headings to the .csv and .txt
        output.println("Unique Identifier	Area	Attribute	Type	Label	OPC Interface PC	OPC Server Name	OPC Tag"); // header of tab delimited file 
        output_evac_zone.println("filename,id,id_string,label,reception_point_name,site_entry_point_label"); // header of .csv
        output_devices.println("Unique Identifier,Area,OPC Tag,id_string,Attribute,Type,Label,evac_zone_id,evac_id_string,evac_label,evac_reception_point_name,evac_site_entry_point_label,channel_type,channel_type_string,tag1,tag2,tag3,tag4,fileName"); // header of .csv

        path = "C:\\APOC_Tool\\input\\"; // set the static file path
        filepath = path; // set current 

        String[] fileNames = getFileNames(filepath);

        for (String fileName : fileNames) {
            if (fileName.contains(".txt")) {
                // text files
                area = "Unknown";
                nodeLabel = "";
                nodeDone = false;
                readTxtFile(fileName);
                output.flush();
            } else if (fileName.contains(".xml")) {
                // xml files
                area = "Unknown";
                folderXML = "Unknown";
                nodeLabel = "";
                nodeDone = false;
                readXmlFile(fileName);
                output.flush();
            } else {
                // folder
                inFolder(fileName);
                filepath = path;
            }
        }
        frame.dispose();
    }

    /**
     * go into a folder in the input folder
     * @param folderName 
     */
    public void inFolder(String folderName) {
        filepath = path + folderName + "\\";

        String[] fileNames = getFileNames(filepath);

        for (String fileName : fileNames) {
            if (fileName.contains(".txt")) {
                // text files
                area = folderName;
                nodeLabel = "";
                nodeDone = false;
                readTxtFile(fileName);
                output.flush();
            } else if (fileName.contains(".xml")) {
                // xml files
                area = csvLookup("attribute_xml_area", folderName, 2);
                folderXML = folderName;
                nodeLabel = "";
                nodeDone = false;
                readXmlFile(fileName);
                output.flush();
            } else {
                // folder
            }
        }
    }

    /**
     * add devices from the given line in the text file
     * @param fileName
     * @param fileLine 
     */
    public void readTxtLine(String fileName, String fileLine) {
        // separate fileLine by comma in to a list of strings 
        String[] parts = fileLine.split("\\;");

        if (parts.length == 6) {
            // uniqueIdentifier calculated at the end
            // area is set with the name of the folder 
            attribute = parts[2].trim();
            type = csvLookup("attribute_type", attribute, 2);
            label = parts[3].trim();
            
            
            identifierArea = csvLookup("unique_identifier_area", area, 2);
            
            int loopPos; // position of the loop number from the end
            if (fileName.contains("new")){
                loopPos = 3; // e.g. T1-Node-001-new.txt
            } else { 
                loopPos = 2; // e.g. T1-Node-001.text
            }

            String[] nodeParts = fileName.replaceAll(" ", "-").replaceAll("\\.", "-").split("\\-");
            identifierNode = threeDigit(nodeParts[nodeParts.length - loopPos]);
            if (!parts[4].trim().contains("-")) {
                identifierLoop = parts[4].trim();
            } else {
                identifierLoop = "X";
            }
            if (!parts[5].trim().contains("-")) {
                identifierDevice = threeDigit(parts[5].trim());
            } else {
                identifierDevice = "XXX";
            }
            
            identifierType = csvLookup("attribute_type", attribute, 3);
            
            opcInterfacePc = "";
            opcServerName = "";
            opcTag = (area+"."+"Panel_"+identifierNode+"("+identifierNode+")."+parts[1].trim()+"("+parts[0].trim()+")").replaceAll(" ", "_");
            
            // nodes always have loop and device as 0
            if (type.toLowerCase().contains("node")){
                if (nodeDone){
                    return;
                } else {
                    nodeDone = true;
                    label = nodeLabel;
                    identifierLoop = "0";
                    identifierDevice = "000";
                    //opcTag = identifierArea+"."+"Panel_"+identifierNode+"("+identifierNode+").Node____(1)"; // Tag in format T01
                    opcTag = area.replace(" ", "_")+"."+"Panel_"+identifierNode+"("+identifierNode+").Node____(1)"; // Tag in format Terminal_1

                }
            }
            
            if (parts[1].contains("(In)") || parts[1].contains("(Out)")) {
                // Channel 1 Device 2 Loop 3 (In) - 1:identifierChannel, 2:identifierDevice 3:identifierLoop
                // trim to get rid of tab at the beginning, replace because there is sometimes not a space before (In)
                String[] nameSplit = parts[1].trim().replaceAll("\\(", " ").split(" ");
                // get channel, device and loop - always the next in sequence
                for (int i = 0; i < nameSplit.length; i++) {
                    switch (nameSplit[i]) {
                    case "Channel":
                        identifierChannel = nameSplit[i+1];
                    case "Device":
                        identifierDevice = threeDigit(nameSplit[i+1]);
                    case "Loop":
                        identifierLoop = nameSplit[i+1];
                    default:
                    }          
                }
            } else {
                identifierChannel = ""; // if it is not INI or INO, then there is not channel
            }
                        
            // create identifier tag
            if (identifierChannel.length() == 0) {
                uniqueIdentifier = identifierArea + "-" + identifierNode + "-" + identifierLoop + "-" + identifierDevice + "-" + identifierType;
            } else {
                uniqueIdentifier = identifierArea + "-" + identifierNode + "-" + identifierLoop + "-" + identifierDevice + "-" + identifierType + "-" + identifierChannel;
            }

            if (identifierLoop != "X") {
                // print the fileLine - testing
                // System.out.println(parts[0] + ", " + parts[1] + ", " + parts[2] + ", " + parts[3] + ", " + parts[4] + ", " + parts[5] + ", ");
                System.out.println("*** " + fileName + " *** " + uniqueIdentifier + ", "
                        + area + ", "
                        + attribute + ", "
                        + type + ", "
                        + label + ", "
                        + opcInterfacePc + ", "
                        + opcServerName + ", "
                        + opcTag
                );
                // only output if element has a loop number

                output.println(uniqueIdentifier + "	"
                        + area + "	"
                        + attribute + "	"
                        + type + "	"
                        + label + "	"
                        + opcInterfacePc + "	"
                        + opcServerName + "	"
                        + opcTag
                );
            }
            resetVariables();
        } else if (fileLine.contains("Node Label")){
            parts = fileLine.split(":");
            if (parts.length > 0){
                nodeLabel = parts[1].trim();
            } else {
                nodeLabel = "";
            }
        } else {
            System.out.println("*** " + parts.length + " *** " + fileLine);
        }
    }

    /**
     * read the text file, calling readTxtLine() for every line
     * @param fileName 
     */
    public void readTxtFile(String fileName) {
        BufferedReader br = null;
        File file = new File(filepath, fileName);
        if (file.exists()) {
            //System.out.println("Searching " + filename);
            try {
                String fileLine;
                br = new BufferedReader(new FileReader(file));
                while ((fileLine = br.readLine()) != null) {
                    // Check that the string is not empty
                    if (fileLine.trim().length() != 0) {
                        readTxtLine(fileName, fileLine);
                    }
                }
            } catch (IOException e) {
                //System.out.println(e);
                //             e.printStackTrace();
                System.out.println("Error4: " + e);
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException ex) {
                    //ex.printStackTrace();
                    System.out.println("Error5: " + ex);
                }
            }
        } else {
            System.out.println("Cannot Find: " + fileName);
        }
    }

    public int intLetter(String letter){
        switch (letter){
                case "A":
                    return 1;
                case "B":
                    return 2;
                case "C":
                    return 3;
                case "D":
                    return 4;
                case "E":
                    return 5;
                default: return 0;
        }
        
    }
    
    /**
     * add devices from the given element in the xml file
     * @param d
     * @param fileName
     * @param element 
     */
    public void getXMLDevices(Document d, String fileName, String element) {
        NodeList deviceList = d.getElementsByTagName(element);
        int deviceCount = deviceList.getLength(); // count how many

        while (deviceCount > 0) {
            d.getDocumentElement().normalize();
            Node deviceNode = deviceList.item(deviceCount - 1); // use the count to get position of node
            Element device = (Element) deviceNode; // get node element

            String device_id_string = device.getAttribute("id_string");

            String device_product_code = device.getAttribute("product_code");
            String device_attribute_type = csvLookup("attribute_type", device_product_code, 2);
            String device_label = device.getAttribute("label");

            String device_identifierArea = csvLookup("unique_identifier_area", area, 2);
            String device_identifierNode = "" + fileName.charAt(fileName.length() - 7) + fileName.charAt(fileName.length() - 6) + fileName.charAt(fileName.length() - 5);
            String device_identifierLoop = device.getAttribute("loop_number");
            String device_identifierDevice = threeDigit(device.getAttribute("loop_position"));
            String device_identifierType = csvLookup("attribute_type", device_product_code, 3);
            String device_identifierChannel = "";
            
            if (device_product_code.length() == 0){
                device_attribute_type = "";
                String [] idSplit = device_id_string.split("_");
                device_identifierType = idSplit[idSplit.length-1].substring(0, 3);
            }
            
            // make changes for special cases
            if (element == "fire_loop_device") {
                // no changes needed for fire loop
            } else if (element == "addressable_speaker") {
                // addressable_speakers identifierLoop to equal identifierLoop + 2
                switch (device_identifierLoop) {
                    case "1":
                        device_identifierLoop = "3";
                    case "2":
                        device_identifierLoop = "4";
                    default:
                }
                device_identifierDevice = threeDigit(device.getAttribute("address"));
            } else if (element == "microphone") {
                // microphones identifierLoop to equal 5
                device_identifierLoop = "5";
                device_identifierDevice = "0" + intLetter(device.getAttribute("serial_port")) + device.getAttribute("address");
                //device_identifierChannel = device.getAttribute("annunciation_input_channel");
            } else if (element == "serial_device") {
                // serial_devices identifierLoop to equal 5
                device_identifierLoop = "5";
                device_identifierDevice = "0" + intLetter(device.getAttribute("port_id")) + device.getAttribute("address");                
            }
            
            if (fileName.contains("_PA.xml")){ // if a PA xml file
                device_identifierNode = "" + fileName.charAt(fileName.length() - 10) + fileName.charAt(fileName.length() - 9) + fileName.charAt(fileName.length() - 8);
                
            }
            
            // create identifier tag     
            String device_uniqueIdentifier = device_identifierArea + "-" + device_identifierNode + "-" + device_identifierLoop + "-" + device_identifierDevice + "-" + device_identifierType;
            if (device_identifierChannel.length() != 0) {
                device_uniqueIdentifier = device_uniqueIdentifier + "-" + device_identifierChannel;
            }

            String evac_zone_id = device.getAttribute("evac_zone_id");
            // lookup evac_zone_id to return id_string, label, reception_point_name, site_entry_point_label
            String evac_zone_details = evacZoneLookup(fileName, evac_zone_id);
            String[] evac_zone_details_arary = evac_zone_details.split("\\,"); // array of length 4
            
            String tag1 = device_identifierNode + " :: " + device_id_string; 
            String tag2 = tag1 + " :: " + evac_zone_details_arary[1];
            // tag 3 and 4 are recreated for each channel
            String tag3 = tag1 + " :: " + device_attribute_type + " :: " + device_label; 
            String tag4 = device_uniqueIdentifier + " :: " + device_id_string;
            
            
            
            System.out.println("****** " + device_uniqueIdentifier + ", "
                    + area + ", "
                    + folderXML + ", "
                    + device_id_string + ", "
                    + device_product_code + ", "
                    + device_attribute_type + ", "
                    + device_label + ", "
                    + evac_zone_id + ", "
                    + evac_zone_details + ", "
                    + ", , " // channel type
                    + tag1 + ", " + tag2 + ", " + tag3 + ", " + tag4 + ", "
                    + fileName
            );
            output_devices.println(device_uniqueIdentifier + ","
                    + area + ","
                    + folderXML + ","
                    + device_id_string + ","
                    + device_product_code + ","
                    + device_attribute_type + ","
                    + device_label + ","
                    + evac_zone_id + ","
                    + evac_zone_details + ","
                    + ",," // channel type
                    + tag1 + "," + tag2 + "," + tag3 + "," + tag4 + ", "
                    + fileName
            );


            NodeList channelList = device.getElementsByTagName("channel"); // get all elements
            int channelCount = channelList.getLength(); // count how many
            System.out.println(device_id_string + "  " + channelCount);

            while (channelCount > 0) {
                d.getDocumentElement().normalize();
                Node channelNode = channelList.item(channelCount - 1); // use the count to get position of node
                Element channel = (Element) channelNode; // get node element

                // get element inside the channel that shows whether it is input or output
                NodeList ioChannelList = channel.getElementsByTagName("*"); // get all elements
                // NodeList channelList = deviceNode.getChildNodes(); // get the child elements
                int ioChannelCount = ioChannelList.getLength(); // count how many - should always be 1
                Node ioChannelNode = ioChannelList.item(0); // use the count to get position of node
                Element ioChannel = (Element) ioChannelNode; // get node element
                String ioName = ioChannel.getNodeName();

                if (ioName.contains("input")) {
                    device_identifierChannel = channel.getAttribute("channel_id");
                    String device_uniqueIdentifier_channel = device_identifierArea + "-" + device_identifierNode + "-" + device_identifierLoop + "-" + device_identifierDevice + "-INI-" + device_identifierChannel;

                    device_label = channel.getAttribute("channel_label");
                    String channel_type = channel.getAttribute("channel_type");
                    String channel_type_string = ioName;

                    tag3 = tag1 + " :: " + device_attribute_type + " :: " + device_label; 
                    tag4 = device_uniqueIdentifier_channel + " :: " + device_id_string;
            
                    System.out.println("** " + device_uniqueIdentifier_channel + ", "
                            + area + ", "
                            + folderXML + ", "
                            + device_id_string + ", "
                            + device_product_code + ", "
                            + device_attribute_type + ", "
                            + device_label + ", "
                            + evac_zone_id + ", "
                            + evac_zone_details + ", "
                            + channel_type + ", "
                            + channel_type_string + ", "
                            + tag1 + ", " + tag2 + ", " + tag3 + ", " + tag4 + ", "
                            + fileName
                    );
                    output_devices.println(device_uniqueIdentifier_channel + ", "
                            + area + ","
                            + folderXML + ","
                            + device_id_string + ","
                            + device_product_code + ","
                            + device_attribute_type + ","
                            + device_label + ","
                            + evac_zone_id + ","
                            + evac_zone_details + ","
                            + channel_type + ","
                            + channel_type_string + ","
                            + tag1 + "," + tag2 + "," + tag3 + "," + tag4 + ", "
                            + fileName
                    );
                }
                channelCount--; // decrement count
            }
            deviceCount--; // decrement count
        }
        output_devices.flush();
    }

    /**
     * look at the xml file for "evac_zone" and create and evac zone lookup
     * @param d
     * @param filename 
     */
    public void getXMLEvacZones(Document d, String filename) {
        NodeList evacList = d.getElementsByTagName("evac_zone");
        int evacCount = evacList.getLength(); // count how many

        while (evacCount > 0) {
            d.getDocumentElement().normalize();
            Node evacNode = evacList.item(evacCount - 1); // use the count to get position of node
            Element evacZone = (Element) evacNode; // get node element

            String evac_id = evacZone.getAttribute("id");
            String evac_id_string = evacZone.getAttribute("id_string");
            String evac_label = evacZone.getAttribute("label");
            String evac_reception_point_name = evacZone.getAttribute("reception_point_name");
            String evac_site_entry_point_label = evacZone.getAttribute("site_entry_point_label");

            System.out.println("****** " + filename + ", "
                    + evac_id + ", "
                    + evac_id_string + ", "
                    + evac_label + ", "
                    + evac_reception_point_name + ", "
                    + evac_site_entry_point_label
            );
            output_evac_zone.println(filename + ","
                    + evac_id + ","
                    + evac_id_string + ","
                    + evac_label + ","
                    + evac_reception_point_name + ","
                    + evac_site_entry_point_label
            );
            evacCount--; // decrement count
        }
        output_evac_zone.flush();
    }

    public void readXmlFile(String fileName) {
        BufferedReader br = null;
        File file = new File(filepath, fileName);
        if (file.exists()) {
            try {
                InputStream in;
                in = new FileInputStream(file);
                if (in.available() > 0) { // if the .xml is more than 0 bytes
                    Document d = XMLtoDOM.readXml(in);
                    System.out.println(filepath + fileName);
                    getXMLEvacZones(d, fileName);
                    getXMLDevices(d, fileName, "fire_loop_device");
                    getXMLDevices(d, fileName, "addressable_speaker");
                    getXMLDevices(d, fileName, "serial_device");
                    getXMLDevices(d, fileName, "microphone");
                }
            } catch (Exception e) {
                System.out.println("Error0: " + e);
                System.out.println("Error0: " + filepath + fileName);
            }
        } else {
            System.out.println("Cannot Find: " + fileName);
        }
    }

    /**
     * method to search type.csv for type number and return type name
     */
    public String evacZoneLookup(String filename, String id) {
        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(evac_zone_lookup_location));
            while ((line = br.readLine()) != null) {
                String[] columns = line.split("\\,"); // use comma as separator
                // if the file and evac_zone_id equal
                if (columns[0].trim().equalsIgnoreCase(filename) && columns[1].trim().equalsIgnoreCase(id)) {

                    // return: 2 id_string, 3 label, 4 reception_point_name, 5 site_entry_point_label
                    String returnString = columns[2] + ", " + columns[3] + ", " + columns[4] + ", " + columns[5];

                    return returnString;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error1: " + e);
            //e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error2: " + e);
            //e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println("Error3: " + e);
                    //e.printStackTrace();
                }
            }
        }

        return "na,na,na,na";
    }

    /**
     * method to search type.csv for type number and return type name
     */
    public String csvLookup(String columnZero, String columnOne, int columnLookup) {
        String csvFile = "C:\\APOC_Tool\\lookup\\lookup.csv";
        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                String[] columns = line.split("\\,"); // use comma as separator
                // if the first column equals the parsed parameter, return the second column
                if (columns[0].trim().equalsIgnoreCase(columnZero) && columns[1].trim().equalsIgnoreCase(columnOne)) {
                    switch (columnLookup) {
                        case 2:
                            return columns[2];
                        case 3:
                            return columns[3];
                        default:
                            System.out.println("Error - no column specified in code");
                            break;
                    }
                    return columns[2];
                }
            }
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            System.out.println("Error7: " + e);
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Error8: " + e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                    System.out.println("Error9: " + e);
                }
            }
        }
        // if no value has been return, return an error message
        if (columnZero == "unique_identifier_area") {
            return "XXX";
        } else {
            return "(check lookup.csv for \"" + columnZero + " " + columnOne + "\")";
        }
    }
    
    /**
     * turn input into a 3 digit number: 1 --> 001
     * return XXX if input is more than 3 digits
     * @param number
     * @return threeDigitNo
     */
    public String threeDigit(String number) {
        String threeDigitNo = "XXX";
        int length = String.valueOf(number).length();
        if (length == 1) {
            threeDigitNo = "00" + number;
        } else if (length == 2) {
            threeDigitNo = "0" + number;
        } else if (length == 3) {
            threeDigitNo = "" + number;
        }
        return threeDigitNo;
    }

    /**
     * reset all the variables that are used for creating output from text file
     */
    public void resetVariables() {
        uniqueIdentifier = "";
        //area = ""; // do not reset area as this is linked to the file, not the line
        attribute = "";
        type = "";
        label = "";
        opcInterfacePc = "";
        opcServerName = "";
        opcTag = "";

        identifierArea = "";
        identifierNode = "";
        identifierLoop = "";
        identifierDevice = "";
        identifierType = "";
        identifierChannel = "";
    }
}
