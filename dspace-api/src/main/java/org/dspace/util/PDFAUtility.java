package org.dspace.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFAUtility {

    private static final Logger log = LoggerFactory.getLogger(PDFAUtility.class);

    public static Map<String, Object> getPDFAndSignatureDetails(InputStream inputStream) {

        Map<String, Object> pdfDetails = new HashMap<>();
        pdfDetails.put(NyayNestConstants.IS_PDFA, false);
        pdfDetails.put(NyayNestConstants.HAS_DIGITAL_SIGNATURES, false);
        pdfDetails.put(NyayNestConstants.SIGNEE, "");
        pdfDetails.put(NyayNestConstants.SIGNED_AT, null);

        PDDocument document = null;
        try {

            // Is PDFA ?
            Path temp = createTempFileFromInputStream(inputStream, "pdf_", ".tmp");
            boolean isPDFA = checkPdfa(temp.toAbsolutePath().toString());
            pdfDetails.put(NyayNestConstants.IS_PDFA, isPDFA);

            // Has Digital Signatures ?
            String filePath = temp.toString();
            document = Loader.loadPDF(new File(filePath));
            List<PDSignature> signatures = document.getSignatureDictionaries();
            if (!signatures.isEmpty()) {
                log.info("PDF is signed.");
                pdfDetails.put(NyayNestConstants.HAS_DIGITAL_SIGNATURES, true);
                // only one person is responsible to sign a pdf document
                PDSignature pdSignature = signatures.get(0);
                // Person who singed the PDF document
                String name = pdSignature.getName();
                if (!Objects.nonNull(name) || name.isEmpty()) {
                    name = getSigneeName(pdSignature);
                }
                pdfDetails.put(NyayNestConstants.SIGNEE, pdSignature.getName());
                if (Objects.nonNull(pdSignature.getSignDate())) {
                    // Time at which signee signed the PDF document
                    pdfDetails.put(NyayNestConstants.SIGNED_AT, LocalDateTime.ofInstant(
                            pdSignature.getSignDate().toInstant(),
                            pdSignature.getSignDate().getTimeZone().toZoneId()
                    ));
                }
            } else {
                log.info("PDF is NOT signed.");
            }
            document.close();
            Files.deleteIfExists(temp);

        } catch (Exception e) {
            log.error("failed to check PDF details");
            e.printStackTrace();
        }
        return pdfDetails;

    }

    private static String getSigneeName(PDSignature signature) {

        String name = "";
        int indexColon = getLastIndex(signature.getReason(), ':');
        if (Integer.valueOf(-1).equals(indexColon)) {
            name = signature.getName();
        } else {
            indexColon += 1;
            int indexComma = getFirstIndex(signature.getReason(), indexColon, ',');
            if (Integer.valueOf(-1).equals(indexComma)) {
                name = signature.getReason().substring(indexColon);
            } else {
                name = signature.getReason().substring(indexColon, indexComma);
            }
            if (name.isEmpty()) {
                name = signature.getName();
            }
        }
        if (!Objects.nonNull(name)) {
            name = "";
        }
        name = name.trim();
        return name;

    }

    private static int getFirstIndex(String str, int start, char ch) {

        int index = -1;
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                index = i;
                break;
            }
        }
        return index;

    }

    private static int getLastIndex(String str, char ch) {

        if (!Objects.nonNull(str) || str.trim().isEmpty()) {
            return -1;
        }
        int lastIndex = -1;
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) == ch) {
                lastIndex = i;
                break;
            }
        }
        return lastIndex;

    }

    public static boolean isPdfa(InputStream inputStream) {

        boolean isPdfa = false;
        try {
            Path temp = createTempFileFromInputStream(inputStream, "pdf_", ".tmp");
            isPdfa = checkPdfa(temp.toAbsolutePath().toString());
            Files.deleteIfExists(temp);
        } catch (Exception e) {
            log.error("failed to check whether PDF conforms to PDFA");
        }
        return isPdfa;

    }

    public static boolean checkPdfa(String filepath) {

        boolean hasPdfaMarkers = false;
        try (PDDocument document = Loader.loadPDF(new File(filepath))) {

            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDMetadata metadata = catalog.getMetadata();
            if (metadata == null) {
                return false;   // no XMP metadata at all
            }

            String xmpRaw = new String(metadata.toByteArray(), StandardCharsets.UTF_8);
            String[] markers = { "pdfaid:part", "pdfaid:conformance", "pdfaSchema", "PDF/A" };
            for (String m : markers) {
                if (xmpRaw.toLowerCase().contains(m.toLowerCase())) {
                    hasPdfaMarkers = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error reading PDF: " + e.getMessage());
        }
        return hasPdfaMarkers;

    }

    public static Path createTempFileFromInputStream(InputStream in, String prefix, String suffix) throws IOException {
        // 1. Create a temporary file
        Path tempFile = Files.createTempFile(prefix, suffix);

        // 2. Copy InputStream content into it
        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);

        return tempFile;
    }

    public static String[] extractPdfaInfo(String xmp) {

        Pattern partPattern = Pattern.compile("<pdfaid:part>(.*?)</pdfaid:part>", Pattern.CASE_INSENSITIVE);
        Pattern confPattern = Pattern.compile("<pdfaid:conformance>(.*?)</pdfaid:conformance>", Pattern.CASE_INSENSITIVE);

        Matcher partMatcher = partPattern.matcher(xmp);
        Matcher confMatcher = confPattern.matcher(xmp);

        String part = partMatcher.find() ? partMatcher.group(1).trim() : null;
        String conf = confMatcher.find() ? confMatcher.group(1).trim() : null;

        return new String[]{ part, conf };
    }

}
