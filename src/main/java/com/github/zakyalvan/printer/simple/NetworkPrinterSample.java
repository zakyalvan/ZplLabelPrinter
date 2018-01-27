package com.github.zakyalvan.printer.simple;

import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NetworkPrinterSample {
    private static final String HOSTNAME = "127.0.0.1";
    private static final Integer PORT = 9100;

    public static void main(String[] args) throws Exception {
        Socket socket = null;
        try {
            socket = new Socket(HOSTNAME, PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            byte[] data = "^XA^FX Top section with company logo, name and address.^CF0,60^FO50,50^GB100,100,100^FS^FO75,75^FR^GB100,100,100^FS^FO88,88^GB50,50,50^FS^FO220,50^FDInternational Shipping, Inc.^FS^CF0,40^FO220,100^FD1000 Shipping Lane^FS^FO220,135^FDJakarta 38102^FS^FO220,170^FDIndonesia (IDN)^FS^FO50,250^GB700,1,3^FS^FX Second section with recipient address and permit information.^CFA,30^FO50,300^FDDonal Gurning^FS^FO50,340^FD100 Main Street^FS^FO50,380^FDSpringfield TN 39021^FS^FO50,420^FDUnited States (USA)^FS^CFA,15^FO600,300^GB150,150,3^FS^FO638,340^FDPermit^FS^FO638,390^FD123456^FS^FO50,500^GB700,1,3^FS^FX Third section with barcode.^BY5,2,270^FO175,550^BC^FD1234567890^FS^FX Fourth section (the two boxes on the bottom).^FO50,900^GB700,250,3^FS^FO400,900^GB1,250,3^FS^CF0,40^FO100,960^FDShipping Ctr. X34B-1^FS^FO100,1010^FDREF1 F00B47^FS^FO100,1060^FDREF2 BL4H8^FS^CF0,190^FO485,965^FDCA^FS^XZ"
                    .getBytes(StandardCharsets.UTF_8);
            out.write(data);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        finally {
            if(socket != null) {
                socket.close();
            }
            System.exit(0);
        }
    }
}
