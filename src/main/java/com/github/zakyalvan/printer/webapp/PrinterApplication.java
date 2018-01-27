package com.github.zakyalvan.printer.webapp;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static javax.print.PrintServiceLookup.lookupPrintServices;

@Controller
@RequestMapping("/printers")
@SpringBootApplication
public class PrinterApplication extends WebMvcConfigurerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PrinterApplication.class, args);
    }

    private Resource zplSample = new ClassPathResource("zpl.txt");

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/printers/local");
    }

    @ModelAttribute("serviceNames")
    List<String> serviceNames() {
        LOGGER.info("Populate printer service name to model");
        List<String> names = Arrays.stream(printServices())
                .map(PrintService::getName)
                .collect(toList());

        names.forEach(name -> LOGGER.info("Print service name : {}", name));
        return names;
    }

    @ModelAttribute("localPrintForm")
    LocalPrintForm localFormModel() throws Exception {
        LocalPrintForm form = new LocalPrintForm();
        form.setPrintCommand(readDefaultCommand());
        return form;
    }

    @ModelAttribute("remotePrintForm")
    RemotePrintForm remotePrintForm() throws Exception {
        RemotePrintForm form = new RemotePrintForm();
        form.setHostName("127.0.0.1");
        form.setBoundPort(9100);
        form.setPrintCommand(readDefaultCommand());
        return form;
    }

    String readDefaultCommand() throws Exception {
        BufferedReader sampleReader = new BufferedReader(new InputStreamReader(zplSample.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = sampleReader.readLine()) != null) {
            builder.append(line).append("\r\n");
        }
        return builder.toString();
    }

    @GetMapping("/local")
    String showLocalPrintForm() {
        LOGGER.info("Show local printing form");
        return "local-print-form";
    }

    /**
     * <p>Handle printing</p>
     *
     * @param form
     * @param bindings
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/local")
    String handleLocalPrinting(@Validated @ModelAttribute LocalPrintForm form, BindingResult bindings) throws Exception {
        LOGGER.info("Handle print request {}", form.toString());

        List<PrintService> services = Arrays.stream(printServices())
                .filter(service -> service.getName().equals(form.getServiceName()))
                .collect(toList());

        if(services.size() == 0) {
            bindings.rejectValue("serviceName", "printer.serviceName.invalid");
        }
        else if(services.size() > 1) {
            bindings.rejectValue("serviceName", "printer.serviceName.ambiguous");
        }

        if(bindings.hasErrors()) {
            return "local-print-form";
        }

        PrintService service = services.get(0);
        DocPrintJob printJob = service.createPrintJob();

        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(form.getPrintCommand().getBytes(), flavor, null);

        printJob.print(doc, new HashPrintRequestAttributeSet());

        return "redirect:/printers/local";
    }

    @GetMapping("/remote")
    String showRemotePrintForm() {
        LOGGER.info("Show remote print form");
        return "remote-print-form";
    }

    /**
     * <p>Handle printing</p>
     *
     * @param form
     * @param bindings
     * @return
     * @throws Exception
     */
    @PostMapping("/remote")
    String handleRemotePrinting(@Validated @ModelAttribute RemotePrintForm form, BindingResult bindings) {
        if(bindings.hasErrors()) {
            LOGGER.info("Error on request form");
            return "remote-print-form";
        }

        Socket socket = null;
        try {
            LOGGER.info("Start printing label...");
            socket = new Socket(form.getHostName(), form.getBoundPort());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.write(form.getPrintCommand().getBytes(StandardCharsets.US_ASCII));
            LOGGER.info("Label printed...");
        }
        catch (Exception e) {
            LOGGER.error("Error on handling remote print, see stack trace", e);
        }
        finally {
            if(socket != null) {
                try {
                    socket.close();
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        return "redirect:/printers/remote";
    }

    private PrintService[] printServices() {
        return lookupPrintServices(DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
    }

    @Data
    @NoArgsConstructor
    public static class LocalPrintForm implements Serializable {
        @NotBlank
        private String serviceName;

        @NotBlank
        private String printCommand;
    }

    @Data
    @NoArgsConstructor
    public static class RemotePrintForm implements Serializable {
        @NotBlank
        private String hostName;

        @NotNull
        private Integer boundPort;

        @NotBlank
        private String printCommand;
    }
}
