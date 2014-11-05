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
    String uniqueIdentifier;
    String area;
    String attribute;
    String type;
    String label;
    String opcInterfacePc;
    String opcServerName;
    String opcTag;
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

    public void t() {
        int i = 4;
        while (i > 0) {
            String g = "B";
            g = g + i;
            System.out.println(g);
            i--;
        }
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
        output = new PrintWriter("C:\\APOC_Tool\\OUTPUT\\" + strDate + " devices_txt.csv"); // create .csv for the devices, for txt files
        evac_zone_lookup_location = "C:\\APOC_Tool\\OUTPUT\\" + strDate + " evac_zones_xml.csv";
        output_evac_zone = new PrintWriter(evac_zone_lookup_location); // create .csv for evac zones, used as lookup
        output_devices = new PrintWriter("C:\\APOC_Tool\\OUTPUT\\" + strDate + " devices_xml.csv"); // create .csv for the devices, for xml files

        // add headings to the .csv
        output.println("Unique Identifier,Area,Attribute,Type,Label,OPC Interface PC,OPC Server Name,OPC Tag");
        output_evac_zone.println("filename,id,id_string,label,reception_point_name,site_entry_point_label"); // header of .csv
        output_devices.println("Unique Identifier,Area,id_string,Attribute,Type,Label,evac_zone_id,evac_id_string,evac_label,evac_reception_point_name,evac_site_entry_point_label,channel_type,channel_type_string"); // header of .csv

        path = "C:\\APOC_Tool\\input\\"; // set the static file path
        filepath = path; // set current 

        String[] fileNames = getFileNames(filepath);

        for (String fileName : fileNames) {
            if (fileName.contains(".txt")) {
                // text files
                area = "Unkown";
                readTxtFile(fileName);
                output.flush();
            } else if (fileName.contains(".xml")) {
                // xml files
                area = "Unkown";
                readXmlFile(fileName);
                output.flush();
            } else {
                // folder
                inFolder(fileName);
                filepath = path;
            }
        }

        // modify JFrame to tell them the program has executed allow them to close the JFrame
       /* JButton button = new JButton("click to close");
         button.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
         frame.dispose();
         }
         });
         label.setText("mapping register created C:\\APOC_Tool\\output\\");
         panel.add(button);*/
        frame.dispose();
    }

    /*
     * method 
     */
    public void inFolder(String folderName) {
        filepath = path + folderName + "\\";

        String[] fileNames = getFileNames(filepath);

        for (String fileName : fileNames) {
            if (fileName.contains(".txt")) {
                // text files
                area = folderName;
                readTxtFile(fileName);
                output.flush();
            } else if (fileName.contains(".xml")) {
                // xml files
                area = csvLookup("attribute_xml_area", folderName, 2);
                readXmlFile(fileName);
                output.flush();
            } else {
                // folder
            }
        }
    }

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
                identifierLoop = "0";
                identifierDevice = "000";
                opcTag = identifierArea+"."+"Panel_"+identifierNode+"("+identifierNode+").Node____(1)";
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

                output.println(uniqueIdentifier + ","
                        + area + ","
                        + attribute + ","
                        + type + ","
                        + label + ","
                        + opcInterfacePc + ","
                        + opcServerName + ","
                        + opcTag
                );
            }
            resetVariables();
        } else {
            System.out.println("*** " + parts.length + " *** " + fileLine);
        }
    }

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

    public void readXmlElement(Document d, String fileName, String element) {
        NodeList list = d.getElementsByTagName(element);
        int count = list.getLength(); // count how many

        while (count > 0) {
            d.getDocumentElement().normalize();
            Node node = list.item(count - 1); // use the count to get position of node
            Element e = (Element) node; // get node element
            //String name = e.getAttribute("product_code");

            // uniqueIdentifier calculated at the end
            // area is set with the name of the folder 
            attribute = e.getAttribute("product_code");
            type = csvLookup("attribute_type", attribute, 2);
            label = e.getAttribute("label");
            opcInterfacePc = "";
            opcServerName = "";
            opcTag = "";

            identifierArea = csvLookup("unique_identifier_area", area, 2);
            identifierNode = "" + fileName.charAt(fileName.length() - 7) + fileName.charAt(fileName.length() - 6) + fileName.charAt(fileName.length() - 5);
            identifierLoop = e.getAttribute("loop_number");
            identifierDevice = threeDigit(e.getAttribute("loop_position"));
            identifierType = csvLookup("attribute_type", attribute, 3);
            identifierChannel = "";

            // make changes for special cases
            if (element == "fire_loop_device") {
                // no changes needed for fire loop
            } else if (element == "addressable_speaker") {
                // addressable_speakers identifierLoop to equal identifierLoop + 2
                switch (identifierLoop) {
                    case "1":
                        identifierLoop = "3";
                    case "2":
                        identifierLoop = "4";
                    default:
                }
                identifierDevice = threeDigit(e.getAttribute("address"));
            } else if (element == "microphone") {
                // microphones identifierLoop to equal 5
                identifierLoop = "5";
                identifierChannel = e.getAttribute("annunciation_input_channel");
            } else if (element == "serial_device") {
                // serial_devices identifierLoop to equal 5
                identifierLoop = "5";
            }

            // create identifier tag
            if (identifierChannel.length() == 0) {
                uniqueIdentifier = identifierArea + "-" + identifierNode + "-" + identifierLoop + "-" + identifierDevice + "-" + identifierType;
            } else {
                uniqueIdentifier = identifierArea + "-" + identifierNode + "-" + identifierLoop + "-" + identifierDevice + "-" + identifierType + "-" + identifierChannel;
            }

            // print the fileLine - testing
            // System.out.println(parts[0] + ", " + parts[1] + ", " + parts[2] + ", " + parts[3] + ", " + parts[4] + ", " + parts[5] + ", ");
            System.out.println("*** " + fileName + " *** " + uniqueIdentifier + ", "
                    + area + ", , "
                    + attribute + ", "
                    + type + ", "
                    + label + ", "
                    + ", , , " 
            );
            output.println(uniqueIdentifier + ","
                    + area + ",,"
                    + attribute + ","
                    + type + ","
                    + label + ","
                    + ",,," 
            );
            
            resetVariables();

            count--; // decrement count
            //       System.out.println(name);
        }
    }

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
                device_identifierChannel = device.getAttribute("annunciation_input_channel");
            } else if (element == "serial_device") {
                // serial_devices identifierLoop to equal 5
                device_identifierLoop = "5";
            }

            // create identifier tag     
            String device_uniqueIdentifier = device_identifierArea + "-" + device_identifierNode + "-" + device_identifierLoop + "-" + device_identifierDevice + "-" + device_identifierType;
            if (device_identifierChannel.length() != 0) {
                device_uniqueIdentifier = device_uniqueIdentifier + "-" + device_identifierChannel;
            }

            String evac_zone_id = device.getAttribute("evac_zone_id");
            // lookup evac_zone_id to return id_string, label, reception_point_name, site_entry_point_label
            String evac_zone_details = evacZoneLookup(fileName, evac_zone_id);

            System.out.println("****** " + device_uniqueIdentifier + ", "
                    + area + ", "
                    + device_id_string + ", "
                    + device_product_code + ", "
                    + device_attribute_type + ", "
                    + device_label + ", "
                    + evac_zone_id + ", "
                    + evac_zone_details
                    + ", , " // channel type
            );
            output_devices.println(device_uniqueIdentifier + ","
                    + area + ","
                    + device_id_string + ","
                    + device_product_code + ","
                    + device_attribute_type + ","
                    + device_label + ","
                    + evac_zone_id + ","
                    + evac_zone_details
                    + ",," // channel type
            );

            /*  NodeList interfaceUnitList = device.getElementsByTagName("channel"); // get all elements
             // NodeList channelList = deviceNode.getChildNodes(); // get the child elements
             int interfaceUnitCount = interfaceUnitList.getLength(); // count how many - should always be 1
             Node interfaceUnitNode = interfaceUnitList.item(0); // use the count to get position of node
             Element interfaceUnit = (Element) interfaceUnitNode; // get node element
            
             NodeList channelList = interfaceUnit.getElementsByTagName("*"); // get all elements
             // NodeList channelList = deviceNode.getChildNodes(); // get the child elements
             int channelCount = channelList.getLength(); // count how many
             System.out.println(device_id_string + "  " + channelCount);*/
            NodeList channelList = device.getElementsByTagName("channel"); // get all elements
            // NodeList channelList = deviceNode.getChildNodes(); // get the child elements
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

                   //System.out.println("***values id-" + channel.getAttribute("channel_id") + " label-" + channel.getAttribute("channel_label") + " type-" + channel.getAttribute("channel_type"));
                //d.getDocumentElement().normalize();
                    //Node channelNode = (Node) channelList.item(deviceCount - 1); // use the count to get position of node
                    //Element channel = (Element) channelNode; // get node element
                    //String st = channelNode.getNodeName();
                    //
                    //NodeList channelList = device.getElementsByTagName("*");
                    //int channelCount = channelList.getLength(); // count how many
                    //String evac_fire_loop_device_ref = channel.getAttribute("id_string");
                    System.out.println("** " + device_uniqueIdentifier_channel + ", "
                            + area + ", "
                            + device_id_string + ", "
                            + device_product_code + ", "
                            + device_attribute_type + ", "
                            + device_label + ", "
                            + evac_zone_id + ", "
                            + evac_zone_details + ", "
                            + channel_type + ", "
                            + channel_type_string
                    );
                    output_devices.println(device_uniqueIdentifier_channel + ", "
                            + area + ","
                            + device_id_string + ","
                            + device_product_code + ","
                            + device_attribute_type + ","
                            + device_label + ","
                            + evac_zone_id + ","
                            + evac_zone_details + ","
                            + channel_type + ","
                            + channel_type_string
                    );
                }
                channelCount--; // decrement count

            }

            deviceCount--; // decrement count
        }
        output_devices.flush();
    }

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
                // String filepath = "C:\\Users\\bsearle\\Documents\\APOC\\JavaInput\\";
                //String fileName = "15000 FAS_PAN00055.xml";
                // File file = new File(filepath, fileName);
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
                    //readXmlElement(d, fileName, "fire_loop_device");
                    //readXmlElement(d, fileName, "addressable_speaker");
                    //readXmlElement(d, fileName, "serial_device");
                    //readXmlElement(d, fileName, "microphone");
                }
            } catch (Exception e) {
                System.out.println("Error0: " + e);
                System.out.println("Error0: " + filepath + fileName);
            }
        } else {
            System.out.println("Cannot Find: " + fileName);
        }
    }

    /*
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

                    // return 2 id_string, 3 label, 4 reception_point_name, 5 site_entry_point_label
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

    /*
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

    public void resetVariables() {
        uniqueIdentifier = "";
        //area = "";
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
