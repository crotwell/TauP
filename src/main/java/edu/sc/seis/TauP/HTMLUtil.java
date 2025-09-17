package edu.sc.seis.TauP;

import java.io.*;
import java.net.URL;
import java.util.List;

public class HTMLUtil {

    public static void createHtmlStart(PrintWriter writer, String title, CharSequence css, boolean withSortTable) throws TauPException {
        String htmlStart = "<!DOCTYPE html>\n" +
            "<html lang=\"en-US\">\n" +
            "  <head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width\">\n";
        writer.println(htmlStart);
        writer.println("    <title>" + title + "</title>\n" +
            "    <style>\n"+
                css+"\n\n"
        );
        if (withSortTable) {
            addSortTableCSS(writer);
        }
        //+ css
        writer.println("\n</style>\n" +
            "  </head>\n" +
            "  <body>\n");
        writer.println("  <h3>"+title+"</h3>");
    }

    public static String createHtmlEnding() {
        return "  </body>\n"+
                "</html>";
    }
    public static String createBasicTable(List<String> headers, List<List<String>> values) {
        return createBasicTableMoHeaders(List.of(headers), values);
    }

    public static String createBasicTableMoHeaders(List<List<String>> headers, List<List<String>> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"sortable sticky\">\n");
        sb.append("<thead>\n");
        for (List<String> hLine : headers) {
            sb.append("<tr>\n");
            for (String h : hLine) {
                sb.append("<th>" + h + "</th>\n");
            }
            sb.append("</tr>\n");
        }
        sb.append("</thead>\n");
        for (List<String> rowValues : values) {
            sb.append("<tr>\n");
            for (String v : rowValues) {
                sb.append("<td>"+v+"</td>\n");
            }
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    public static String createTableCSS() {
        return createBaseTableCSS()+"\n"
                +createAlternateRowCSS();
    }
    public static String createBaseTableCSS() {
        StringBuilder sb = new StringBuilder();

        sb.append("table {\n");
        sb.append("    border-collapse: collapse;\n");
        sb.append("    border: 2px solid rgb(200,200,200);\n");
        sb.append("    letter-spacing: 1px;\n");
        sb.append("    font-size: 0.8rem;\n");
        sb.append("}\n");

        sb.append("th {\n");
        sb.append("    background-color: rgb(235,235,235);\n");
        sb.append("}\n");

        sb.append("td {\n");
        sb.append("    text-align: center;\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static String createAlternateRowCSS() {
        StringBuilder sb = new StringBuilder();
        sb.append(" td, th {\n");
        sb.append("    border: 1px solid rgb(190,190,190);\n");
        sb.append("    padding: 10px 20px;\n");
        sb.append("}\n");

        sb.append("tbody tr:nth-child(even) td {\n");
        sb.append("     background: var(--even-row-background, Cornsilk);\n");
        sb.append("}\n");

        sb.append("tbody tr:nth-child(odd) td {\n");
        sb.append("    background: var(--odd-row-background);\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static String createThridRowCSS() {
        StringBuilder sb = new StringBuilder();

        sb.append(" td, th {\n");
        sb.append("    border: 1px solid rgb(190,190,190);\n");
        sb.append("    padding-left: 20px;\n");
        sb.append("    padding-right: 20px;\n");
        sb.append("}\n");

        sb.append("tbody tr:nth-child(3n) td {\n");
        sb.append("    background: var(--odd-row-background);\n");
        sb.append("    border: unset;\n");
        sb.append("    padding-top: 5px;\n");
        sb.append("    border-top: 1px solid lightgray;\n");
        sb.append("}\n");

        sb.append("tbody tr:nth-child(3n+1) td {\n");
        sb.append("     background: var(--even-row-background, Cornsilk);\n");
        sb.append("    padding-top: 0px;\n");
        sb.append("    padding-bottom: 0px;\n");
        sb.append("    border: unset;\n");
        sb.append("}\n");

        sb.append("tbody tr:nth-child(3n+2) td {\n");
        sb.append("    background: var(--odd-row-background);\n");
        sb.append("    padding-bottom: 5px;\n");
        sb.append("    border: unset;\n");
        sb.append("    border-bottom: 1px solid lightgray;\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static void addSortTableCSS(PrintWriter writer) throws TauPException {
        String jsFilename = "js/sortable.css";
        try {
            HTMLUtil.loadResource(jsFilename).transferTo(writer);
        } catch (IOException e) {
            throw new TauPException("Unable to load "+jsFilename, e);
        }
    }

    public static void addSortTableJS(PrintWriter writer) throws TauPException {
        writer.println("  <script type=\"module\">");

        String jsFilename = "js/sortable.js";
        try {
            HTMLUtil.loadResource(jsFilename).transferTo(writer);
        } catch (IOException e) {
            throw new TauPException("Unable to load "+jsFilename, e);
        }
        writer.println("  </script>");
    }

    public static BufferedReader loadResource(String name) {
        String resource = "edu/sc/seis/TauP/html/" + name;
        URL res = HTMLUtil.class.getClassLoader().getResource(resource);
        InputStream inStream = HTMLUtil.class
                .getClassLoader()
                .getResourceAsStream(resource);
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        return in;
    }
}
