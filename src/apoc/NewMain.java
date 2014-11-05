/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author bsearle
 */
public class NewMain {

    //XMLtoDOM XMLtoDOM;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        new NewMain().start();
    }

    public void start() {
        try {
            String filepath = "C:\\Users\\bsearle\\Documents\\APOC\\JavaInput\\";
            String fileName = "15000 FAS_PAN00055.xml";
            File file = new File(filepath, fileName);
            InputStream in = new FileInputStream(file);
            Document d = XMLtoDOM.readXml(in);

            NodeList list_fire_loop_device = d.getElementsByTagName("fire_loop_device");
            int count_fire_loop_device = list_fire_loop_device.getLength(); // count how many
            while (count_fire_loop_device > 0) {
                d.getDocumentElement().normalize();
                Node node = list_fire_loop_device.item(count_fire_loop_device - 1); // use the count to get position of node
                Element e = (Element) node; // get node element
                String name = e.getAttribute("product_code");

                count_fire_loop_device--; // decrement count
                System.out.println(name);
            }

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }
}
