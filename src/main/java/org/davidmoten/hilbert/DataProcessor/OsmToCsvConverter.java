package org.davidmoten.hilbert.DataProcessor;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OsmToCsvConverter {

    private static final Logger logger = Logger.getLogger(OsmToCsvConverter.class.getName());
    private static final String OSM_FILE_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large.osm";
    private static final String OUTPUT_CSV_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large.csv";

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            OsmHandler handler = new OsmHandler();
            saxParser.parse(OSM_FILE_PATH, handler);
            handler.writeCsv(OUTPUT_CSV_PATH);

            long endTime = System.currentTimeMillis();
            logger.info("转换完成，耗时 " + (endTime - startTime) / 1000.0 + " 秒");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "发生错误", e);
        }
    }
}

class OsmHandler extends DefaultHandler {

    private static final Logger logger = Logger.getLogger(OsmHandler.class.getName());
    private final Map<String, Node> nodes = new HashMap<>();
    private int nodeCount = 0;
    private int progressInterval = 10000;  // 每处理10000个节点打印一次进度

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("node")) {
            String id = attributes.getValue("id");
            String lat = attributes.getValue("lat");
            String lon = attributes.getValue("lon");
            int version = Integer.parseInt(attributes.getValue("version"));

            Node node = nodes.get(id);
            if (node == null || version > node.version) {
                nodes.put(id, new Node(id, lat, lon, version));
            }

            nodeCount++;
            if (nodeCount % progressInterval == 0) {
                logger.info("已处理 " + nodeCount + " 个节点");
            }
        }
    }

    public void writeCsv(String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.append("osm_id,latitude,longitude\n");

            for (Node node : nodes.values()) {
                writer.append(node.id).append(',')
                        .append(node.lat).append(',')
                        .append(node.lon).append('\n');
            }
            logger.info("CSV 文件已保存到 " + outputPath);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "写入 CSV 文件时发生错误", e);
        }
    }
}

class Node {
    String id;
    String lat;
    String lon;
    int version;

    public Node(String id, String lat, String lon, int version) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.version = version;
    }
}
